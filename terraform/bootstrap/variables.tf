variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "tags" {
  description = "Tags applied to all resources via the AWS provider"
  type        = map(string)
  default = {
    Project = "agentic-pm"
  }
}

variable "state_bucket_name" {
  description = "S3 bucket for Terraform remote state"
  type        = string
  default     = "agentic-pm-tfstate"
}

variable "lock_table_name" {
  description = "DynamoDB table for Terraform state locking"
  type        = string
  default     = "agentic-pm-tf-lock"
}

