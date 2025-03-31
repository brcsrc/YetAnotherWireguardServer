#!/usr/bin/env bash

set -eo pipefail

TEST_CONTAINER_NAME="yaws-tests"
TEST_CONTAINER_TAG="latest"

function run_container() {

  #   --privileged                                    # needed to modify iptables
  #   --cap-add=NET_ADMIN                             # needed to modify iptables
  #   --name "$TEST_CONTAINER_NAME"                   # name to address container with
  #   -v "$(pwd)/src:/opt/src"                        # bind mount the src dir so code changes can be immediately tested
  #   -d \                                            # detach container main process from the terminal
  #   "${TEST_CONTAINER_NAME}:${TEST_CONTAINER_TAG}"  # image name and tag to use in container

  docker run \
   --privileged \
   --cap-add=NET_ADMIN \
   --name "$TEST_CONTAINER_NAME" \
   -v "$(pwd)/src:/opt/src" \
   -d \
   "${TEST_CONTAINER_NAME}:${TEST_CONTAINER_TAG}"
}

function run_tests() {
  local test_name="$1"
  local full_rebuild="$2"

  local container_exists=$(docker container inspect -f '{{.State.Running}}' "${TEST_CONTAINER_NAME}")

  if [[ "$full_rebuild" == "true" ]] || [[ "$container_exists" == "false" ]] || [ -z "$container_exists" ]; then
    docker stop "$TEST_CONTAINER_NAME" || true
    docker rm "$TEST_CONTAINER_NAME" || true
    docker build -f docker/test/test.Dockerfile -t "$TEST_CONTAINER_NAME" .
    run_container
  fi

  if [[ -n "$test_name" ]]; then
    echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [${TEST_CONTAINER_NAME}] running tests with ${test_name}"
    docker exec "$TEST_CONTAINER_NAME" /opt/gradlew test --tests "$test_name"
  else
    echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [${TEST_CONTAINER_NAME}] running all tests"
    docker exec "$TEST_CONTAINER_NAME" /opt/gradlew test
  fi

  echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [${TEST_CONTAINER_NAME}] retrieving test reports"

  set +e
  docker exec "$TEST_CONTAINER_NAME" stat /opt/build/reports/jacoco/test/html/index.html > /dev/null 2>&1
  if [ $? == 0 ]; then
    local test_report_loc="/opt/build/reports/jacoco/test/html"
    mkdir -p coverage-report
    docker cp "${TEST_CONTAINER_NAME}:${test_report_loc}" coverage-report/.
    echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [${TEST_CONTAINER_NAME}] view test coverage at $(pwd)/coverage-report/index.html"
  else
    echo "$(date -u +"%Y-%m-%dT%H:%M:%SZ") INFO [${TEST_CONTAINER_NAME}] tests reports could not be found"
  fi
  set -e
}

function main() {
    local TEST_NAME=""
    local FULL_REBUILD="false"

    while true; do
      case "$1" in
        run-tests             ) OPERATION="run-tests"; shift 1;;
        --test-name           ) TEST_NAME="${2:-}"; shift 2;;   # if not present default to ""
        --full-rebuild        ) FULL_REBUILD="true"; shift 1;;
        -- ) shift; break ;;
      * ) break ;;
      esac
    done

    case "$OPERATION" in
      "run-tests")
          run_tests "$TEST_NAME" "$FULL_REBUILD"
        ;;
      *) echo "invalid operation"; exit 1
    esac
}
main "$@"