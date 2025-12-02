#!/usr/bin/env bash
#
# Deploy YAWS to AWS EC2 development environment
#
# Usage:
#   ./scripts/deploy-to-ec2.sh deploy                - Full deployment (CloudFormation + Docker build/push + ASG refresh)
#   ./scripts/deploy-to-ec2.sh deploy --skip-stack   - Skip CloudFormation, just build/push Docker image and refresh ASG
#   ./scripts/deploy-to-ec2.sh teardown              - Delete stack and clean up all resources
#
# Options:
#   --stack-name NAME    Override default stack name (default: yaws-dev)
#   --image-tag TAG      Override default image tag (default: latest)
#

set -eo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

AWS_REGION="us-west-2"
STACK_NAME="yaws-dev"
TEMPLATE_FILE="${SCRIPT_DIR}/ec2-based-infrastructure.yml"
DOCKERFILE="${PROJECT_ROOT}/docker/prod/Dockerfile"
IMAGE_TAG="latest"

function log_info() {
  echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [deploy-to-ec2] $1"
}

function log_error() {
  echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") ERROR [deploy-to-ec2] $1" >&2
}

function check_aws_cli() {
  if ! command -v aws &> /dev/null; then
    log_error "AWS CLI is not installed. Please install it first."
    exit 1
  fi

  log_info "checking AWS credentials"
  if ! aws sts get-caller-identity --region "${AWS_REGION}" &> /dev/null; then
    log_error "AWS credentials not configured or invalid"
    exit 1
  fi
}

function check_docker() {
  if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed. Please install it first."
    exit 1
  fi

  if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running"
    exit 1
  fi
}

function get_ecr_repository_uri() {
  local stack_name="$1"

  aws cloudformation describe-stacks \
    --stack-name "${stack_name}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs[?OutputKey==`ECRRepositoryUri`].OutputValue' \
    --output text 2>/dev/null || echo ""
}

function get_stack_status() {
  local stack_name="$1"

  aws cloudformation describe-stacks \
    --stack-name "${stack_name}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].StackStatus' \
    --output text 2>/dev/null || echo "DOES_NOT_EXIST"
}

function deploy_cloudformation_stack() {
  local stack_name="$1"
  local template_file="$2"
  local allowed_ip="$3"

  local stack_status=$(get_stack_status "${stack_name}")

  local parameters=""
  if [[ -n "${allowed_ip}" ]]; then
    parameters="--parameters ParameterKey=AllowedIP,ParameterValue=${allowed_ip}"
  fi

  if [[ "${stack_status}" == "DOES_NOT_EXIST" ]]; then
    log_info "creating CloudFormation stack: ${stack_name}"
    aws cloudformation create-stack \
      --stack-name "${stack_name}" \
      --template-body "file://${template_file}" \
      --capabilities CAPABILITY_NAMED_IAM \
      --region "${AWS_REGION}" \
      ${parameters}

    log_info "waiting for stack creation to complete (this may take several minutes)"
    aws cloudformation wait stack-create-complete \
      --stack-name "${stack_name}" \
      --region "${AWS_REGION}"

    log_info "stack created successfully"
  else
    log_info "updating CloudFormation stack: ${stack_name} (current status: ${stack_status})"

    set +e
    aws cloudformation update-stack \
      --stack-name "${stack_name}" \
      --template-body "file://${template_file}" \
      --capabilities CAPABILITY_NAMED_IAM \
      --region "${AWS_REGION}" \
      ${parameters} 2>&1 | tee /tmp/update-stack-output.txt

    local update_exit_code=$?
    set -e

    if [[ $update_exit_code -eq 0 ]]; then
      log_info "waiting for stack update to complete"
      aws cloudformation wait stack-update-complete \
        --stack-name "${stack_name}" \
        --region "${AWS_REGION}"
      log_info "stack updated successfully"
    elif grep -q "No updates are to be performed" /tmp/update-stack-output.txt; then
      log_info "no stack updates required"
    else
      log_error "stack update failed"
      exit 1
    fi
  fi
}

function build_and_push_image() {
  local ecr_uri="$1"
  local image_tag="$2"

  log_info "building Docker image from ${DOCKERFILE}"
  docker build \
    -f "${DOCKERFILE}" \
    -t "${ecr_uri}:${image_tag}" \
    "${PROJECT_ROOT}"

  log_info "logging in to ECR"
  aws ecr get-login-password --region "${AWS_REGION}" | \
    docker login --username AWS --password-stdin "${ecr_uri}"

  log_info "pushing image to ECR: ${ecr_uri}:${image_tag}"
  docker push "${ecr_uri}:${image_tag}"

  log_info "image pushed successfully"
}

function refresh_asg() {
  local stack_name="$1"

  local asg_name=$(aws cloudformation describe-stacks \
    --stack-name "${stack_name}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs[?OutputKey==`AutoScalingGroupName`].OutputValue' \
    --output text 2>/dev/null)

  if [[ -z "${asg_name}" ]]; then
    log_error "could not retrieve Auto Scaling Group name from stack outputs"
    return 1
  fi

  log_info "starting instance refresh for ASG: ${asg_name}"

  local refresh_id=$(aws autoscaling start-instance-refresh \
    --auto-scaling-group-name "${asg_name}" \
    --region "${AWS_REGION}" \
    --query 'InstanceRefreshId' \
    --output text)

  log_info "instance refresh initiated (ID: ${refresh_id})"
  log_info "waiting for instance refresh to complete (this may take several minutes)"

  while true; do
    local refresh_status=$(aws autoscaling describe-instance-refreshes \
      --auto-scaling-group-name "${asg_name}" \
      --instance-refresh-ids "${refresh_id}" \
      --region "${AWS_REGION}" \
      --query 'InstanceRefreshes[0].Status' \
      --output text)

    if [[ "${refresh_status}" == "Successful" ]]; then
      log_info "instance refresh completed successfully"
      break
    elif [[ "${refresh_status}" == "Failed" || "${refresh_status}" == "Cancelled" ]]; then
      log_error "instance refresh ${refresh_status}"
      return 1
    else
      log_info "instance refresh status: ${refresh_status}"
      sleep 10
    fi
  done
}

function get_instance_public_dns() {
  local stack_name="$1"

  local asg_name=$(aws cloudformation describe-stacks \
    --stack-name "${stack_name}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs[?OutputKey==`AutoScalingGroupName`].OutputValue' \
    --output text)

  local instance_id=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "${asg_name}" \
    --region "${AWS_REGION}" \
    --query 'AutoScalingGroups[0].Instances[0].InstanceId' \
    --output text)

  if [[ -z "${instance_id}" || "${instance_id}" == "None" ]]; then
    log_error "no running instance found in ASG"
    return 1
  fi

  local public_dns=$(aws ec2 describe-instances \
    --instance-ids "${instance_id}" \
    --region "${AWS_REGION}" \
    --query 'Reservations[0].Instances[0].PublicDnsName' \
    --output text)

  echo "${public_dns}"
}

function print_deployment_info() {
  local stack_name="$1"

  log_info "deployment complete!"
  log_info "stack outputs:"

  aws cloudformation describe-stacks \
    --stack-name "${stack_name}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' \
    --output table

  local public_dns=$(get_instance_public_dns "${stack_name}")

  if [[ -n "${public_dns}" && "${public_dns}" != "None" ]]; then
    log_info "instance public DNS: ${public_dns}"
    log_info "connect via SSM: aws ssm start-session --target \$(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-names ${stack_name}-asg --region ${AWS_REGION} --query 'AutoScalingGroups[0].Instances[0].InstanceId' --output text) --region ${AWS_REGION}"
  else
    log_info "instance is still launching, public DNS not yet available"
  fi
}

function deploy() {
  local skip_stack="$1"

  check_aws_cli
  check_docker

  log_info "deploying YAWS to AWS EC2"
  log_info "region: ${AWS_REGION}"
  log_info "stack: ${STACK_NAME}"

  if [[ "${skip_stack}" != "true" ]]; then
    local allowed_ip="$(curl -s ifconfig.me)/32"
    log_info "restricting access to: ${allowed_ip} (443/tcp), 51820/udp open to all"
    deploy_cloudformation_stack "${STACK_NAME}" "${TEMPLATE_FILE}" "${allowed_ip}"
  else
    log_info "skipping CloudFormation stack deployment"
  fi

  local ecr_uri=$(get_ecr_repository_uri "${STACK_NAME}")

  if [[ -z "${ecr_uri}" ]]; then
    log_error "could not retrieve ECR repository URI from stack outputs"
    exit 1
  fi

  build_and_push_image "${ecr_uri}" "${IMAGE_TAG}"
  refresh_asg "${STACK_NAME}"

  print_deployment_info "${STACK_NAME}"
}

function teardown() {
  check_aws_cli

  log_info "tearing down YAWS EC2 environment"
  log_info "stack: ${STACK_NAME}"

  local stack_status=$(get_stack_status "${STACK_NAME}")

  if [[ "${stack_status}" == "DOES_NOT_EXIST" ]]; then
    log_info "stack does not exist, nothing to tear down"
    return 0
  fi

  # Empty ECR repository before deletion
  local ecr_repo_name=$(aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${AWS_REGION}" \
    --query 'Stacks[0].Outputs[?OutputKey==`ECRRepositoryName`].OutputValue' \
    --output text 2>/dev/null)

  if [[ -n "${ecr_repo_name}" ]]; then
    log_info "deleting all images from ECR repository: ${ecr_repo_name}"

    set +e
    local image_ids=$(aws ecr list-images \
      --repository-name "${ecr_repo_name}" \
      --region "${AWS_REGION}" \
      --query 'imageIds[*]' \
      --output json 2>/dev/null)
    set -e

    if [[ -n "${image_ids}" && "${image_ids}" != "[]" ]]; then
      aws ecr batch-delete-image \
        --repository-name "${ecr_repo_name}" \
        --region "${AWS_REGION}" \
        --image-ids "${image_ids}" &>/dev/null || true
    fi
  fi

  log_info "deleting CloudFormation stack: ${STACK_NAME}"
  aws cloudformation delete-stack \
    --stack-name "${STACK_NAME}" \
    --region "${AWS_REGION}"

  log_info "waiting for stack deletion to complete"
  aws cloudformation wait stack-delete-complete \
    --stack-name "${STACK_NAME}" \
    --region "${AWS_REGION}"

  log_info "stack deleted successfully"
}

function main() {
  local OPERATION=""
  local SKIP_STACK="false"

  while true; do
    case "$1" in
      deploy                ) OPERATION="deploy"; shift 1;;
      teardown              ) OPERATION="teardown"; shift 1;;
      --skip-stack          ) SKIP_STACK="true"; shift 1;;
      --stack-name          ) STACK_NAME="${2:-}"; shift 2;;
      --image-tag           ) IMAGE_TAG="${2:-}"; shift 2;;
      -- ) shift; break ;;
      * ) break ;;
    esac
  done

  case "$OPERATION" in
    "deploy")
      deploy "$SKIP_STACK"
      ;;
    "teardown")
      teardown
      ;;
    *)
      log_error "invalid operation. Usage: $0 {deploy|teardown} [--skip-stack] [--stack-name NAME] [--image-tag TAG]"
      exit 1
      ;;
  esac
}

main "$@"
