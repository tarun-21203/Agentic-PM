terraform {
  backend "s3" {
    # Bootstrap first (see terraform/bootstrap) to create:
    # - S3 bucket for state
    # - DynamoDB table for state locking
    #
    # Then set these values (backend blocks cannot use variables).
    bucket       = "agentic-pm-tfstate"
    key          = "agentic-pm/terraform.tfstate"
    region       = "us-east-1"
    use_lockfile = true
    encrypt      = true
  }
}

