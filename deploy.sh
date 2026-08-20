#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE="${AWS_PROFILE:-wcds}"

BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
TERRAFORM_DIR="$ROOT_DIR/terraform"

TF_BACKEND_REGION="us-east-1"
TF_STATE_BUCKET="agentic-pm-tfstate"
TF_STATE_KEY="agentic-pm/terraform.tfstate"
TF_LOCK_TABLE="agentic-pm-tf-lock"

# NOTE: terraform/backend.tf currently hardcodes the remote state region to us-east-1.
if [[ "$AWS_REGION" != "$TF_BACKEND_REGION" ]]; then
  echo "WARN: terraform backend is pinned to $TF_BACKEND_REGION; overriding AWS_REGION=$AWS_REGION -> $TF_BACKEND_REGION"
  AWS_REGION="$TF_BACKEND_REGION"
fi

FRONTEND_DIST_DIR="$FRONTEND_DIR/dist"
BACKEND_IMAGE_TAG="latest"
# After deploy, ECS desired count (overrides dev.tfvars for this script; export API_ECS_DESIRED_COUNT=0 to skip scaling up)
API_ECS_DESIRED_COUNT="${API_ECS_DESIRED_COUNT:-1}"

echo "==> Verifying AWS credentials..."
export AWS_PROFILE
export AWS_REGION
aws sts get-caller-identity >/dev/null

NAME_PREFIX="$(awk -F= '/^name_prefix[[:space:]]*=/{gsub(/[[:space:]]/,"",$2); print $2}' "$TERRAFORM_DIR/dev.tfvars" 2>/dev/null || true)"
NAME_PREFIX="${NAME_PREFIX:-agentic-pm}"
# dev.tfvars typically contains quotes, e.g. name_prefix = "agentic-pm"
NAME_PREFIX="${NAME_PREFIX%\"}"
NAME_PREFIX="${NAME_PREFIX#\"}"
NAME_PREFIX="$(echo "$NAME_PREFIX" | tr -d '\r' | tr '[:upper:]' '[:lower:]')"

ECR_REPOSITORY_NAME="${NAME_PREFIX}-backend"
ECS_CLUSTER_NAME="${NAME_PREFIX}-cluster"
ECS_SERVICE_NAME="${NAME_PREFIX}-api-svc"

AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ECR_REGISTRY_API="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ECR_REPOSITORY_URL="${ECR_REGISTRY_API}/${ECR_REPOSITORY_NAME}"

echo "==> Ensuring ECR repository exists: $ECR_REPOSITORY_NAME"
if ! aws ecr describe-repositories --repository-name "$ECR_REPOSITORY_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
  aws ecr create-repository --repository-name "$ECR_REPOSITORY_NAME" --region "$AWS_REGION" >/dev/null
fi

echo "==> Building backend API container..."
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: docker is required to build/push the backend image."
  exit 1
fi

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY_API"

cd "$BACKEND_DIR"
docker build -t "$ECR_REPOSITORY_URL:$BACKEND_IMAGE_TAG" .
docker push "$ECR_REPOSITORY_URL:$BACKEND_IMAGE_TAG"

echo "==> Resolving pushed image digest (pins ECS task definition via Terraform)..."
BACKEND_IMAGE_DIGEST="$(
  aws ecr describe-images \
    --repository-name "$ECR_REPOSITORY_NAME" \
    --region "$AWS_REGION" \
    --image-ids "imageTag=$BACKEND_IMAGE_TAG" \
    --query 'imageDetails[0].imageDigest' \
    --output text
)"
if [[ -z "$BACKEND_IMAGE_DIGEST" || "$BACKEND_IMAGE_DIGEST" == "None" ]]; then
  echo "ERROR: Could not read ECR digest for ${ECR_REPOSITORY_NAME}:${BACKEND_IMAGE_TAG}"
  exit 1
fi
echo "    digest: $BACKEND_IMAGE_DIGEST"

echo "==> Bootstrapping Terraform remote state (only if missing)..."
NEED_TF_BOOTSTRAP="false"
if ! aws s3api head-bucket --bucket "$TF_STATE_BUCKET" --region "$AWS_REGION" >/dev/null 2>&1; then
  NEED_TF_BOOTSTRAP="true"
fi
if ! aws dynamodb describe-table --table-name "$TF_LOCK_TABLE" --region "$AWS_REGION" >/dev/null 2>&1; then
  NEED_TF_BOOTSTRAP="true"
fi

if [[ "$NEED_TF_BOOTSTRAP" == "true" ]]; then
  terraform -chdir="$TERRAFORM_DIR/bootstrap" init -input=false
  terraform -chdir="$TERRAFORM_DIR/bootstrap" apply -auto-approve -input=false -var="aws_region=$AWS_REGION"
fi

terraform -chdir="$TERRAFORM_DIR" init -input=false -reconfigure

echo "==> Applying Terraform (updates ECS task definition to this image digest every deploy)..."
terraform -chdir="$TERRAFORM_DIR" apply -auto-approve -input=false \
  -var-file="dev.tfvars" \
  -var="aws_region=$AWS_REGION" \
  -var="backend_image_tag=$BACKEND_IMAGE_TAG" \
  -var="backend_image_digest=$BACKEND_IMAGE_DIGEST" \
  -var="api_ecs_desired_count=$API_ECS_DESIRED_COUNT"

# Read outputs for the frontend build + uploads.
FRONTEND_CF_DOMAIN="$(terraform -chdir="$TERRAFORM_DIR" output -raw frontend_cloudfront_domain)"
API_BASE="https://${FRONTEND_CF_DOMAIN}/api"
COGNITO_POOL_ID="$(terraform -chdir="$TERRAFORM_DIR" output -raw cognito_user_pool_id)"
COGNITO_CLIENT_ID="$(terraform -chdir="$TERRAFORM_DIR" output -raw cognito_user_pool_client_id)"

echo "==> Building frontend (VITE_API_BASE=$API_BASE)..."
cd "$FRONTEND_DIR"
npm install
VITE_API_BASE="$API_BASE" \
  VITE_COGNITO_USER_POOL_ID="$COGNITO_POOL_ID" \
  VITE_COGNITO_CLIENT_ID="$COGNITO_CLIENT_ID" \
  npm run build

if [[ ! -d "$FRONTEND_DIST_DIR" ]]; then
  echo "ERROR: Frontend dist folder not found at: $FRONTEND_DIST_DIR"
  exit 1
fi

CF_DIST_ID="$(terraform -chdir="$TERRAFORM_DIR" output -raw frontend_cloudfront_distribution_id)"
FRONTEND_S3_BUCKET_NAME="$(terraform -chdir="$TERRAFORM_DIR" output -raw frontend_s3_bucket_name)"

echo "==> Uploading frontend build to S3..."
aws s3 sync "$FRONTEND_DIST_DIR/" "s3://$FRONTEND_S3_BUCKET_NAME/" --delete

echo "==> Forcing ECS service rollout (picks up new task definition revision)..."
aws ecs update-service \
  --cluster "$ECS_CLUSTER_NAME" \
  --service "$ECS_SERVICE_NAME" \
  --desired-count "$API_ECS_DESIRED_COUNT" \
  --force-new-deployment >/dev/null

echo "==> Invalidating CloudFront cache..."
aws cloudfront create-invalidation --distribution-id "$CF_DIST_ID" --paths "/*" >/dev/null

FRONTEND_URL="https://${FRONTEND_CF_DOMAIN}"
echo "==> Deploy complete."
echo "Frontend URL: $FRONTEND_URL"
echo "API base wired into frontend build: $API_BASE"

