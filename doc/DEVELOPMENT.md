# Development

## Table of Contents
- [Project Layout](#project-layout)
- [Setup](#setup)
- [Building](#building)
- [Frontend Development](#frontend-development)
- [Running Tests](#running-tests)
- [IntelliJ Setup](#intellij-setup)
- [Database](#database)
- [API Schema and Client Generation](#api-schema-and-client-generation)
- [AWS EC2 Deployment](#aws-ec2-deployment)
- [Production JRE Dependencies](#production-jre-dependencies)
- [Reference Links](#reference-links)

---

## Project Layout

```
src/
  backend/    Java/Spring Boot backend source
  frontend/   React frontend (Vite)
  client/     TypeScript API client (generated from OpenAPI spec)
  shell/      WireGuard shell scripts copied into the container
build/
  backend/    Gradle internals (classes, resources, tmp)
  backend/yaws-0.0.1-SNAPSHOT.jar  Fat JAR
  openapi.json  Generated OpenAPI spec
  frontend/   Built frontend assets
  client/     Compiled TypeScript API client
  coverage/   Jacoco coverage report and test results
docker/
  prod/       Production Dockerfile + entrypoint
  dev/        Development Dockerfile + entrypoint
  test/       Test Dockerfile + entrypoint
scripts/      Deployment and test runner scripts
```

---

## Setup

Run once after cloning. Caches the Gradle binary and JARs locally to speed up image builds, and installs npm dependencies.

```shell
make setup
```

To wipe all build artifacts and setup caches:
```shell
make clean
```

---

## Building

The Makefile orchestrates the full build pipeline. All artifacts land in `build/`.

#### Build everything
```shell
make build
```

#### Build individual steps
```shell
make build-backend    # compiles JAR → build/backend/yaws-0.0.1-SNAPSHOT.jar
make build-api-spec   # generates OpenAPI spec → build/openapi.json
make build-api-client # compiles TS client → build/client/
make build-frontend   # builds React app → build/frontend/
```

#### Build and run the prod image with a persistent database
Bind mount the database to the project filesystem for manual testing. You must have a `yaws.db` available first — run the above command then copy it out with `docker cp yaws:/opt/yaws.db .`
```shell
make image
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -e YAWS_DEV="true" \
 -e YAWS_ADMIN_USERNAME="admin" \
 -e YAWS_ADMIN_PASSWORD="Str0ng!Pass#1" \
 -p 0.0.0.0:51820:51820/udp \
 -p 0.0.0.0:8080:8080/tcp \
 -v $(pwd)/yaws.db:/opt/yaws.db \
 --name yaws \
 -d \
 yaws:latest && \
docker exec -it yaws bash
```

---

## Frontend Development

The Vite dev server proxies `/api/v1` to `localhost:8080`, so you need the backend running locally before starting it.

#### 1. Start the backend
```shell
make dev-image
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -e YAWS_DEV="true" \
 -e YAWS_ADMIN_USERNAME="admin" \
 -e YAWS_ADMIN_PASSWORD="Str0ng!Pass#1" \
 -e YAWS_CORS_ALLOWED_ORIGINS="http://localhost:5173" \
 -p 0.0.0.0:51820:51820/udp \
 -p 0.0.0.0:8080:8080/tcp \
 --name yaws \
 -d \
 yaws-dev:latest
```

#### 2. Start the Vite dev server
```shell
cd src/frontend && npm run dev
```

The app is available at `http://localhost:5173`. API calls are proxied to the backend at `http://localhost:8080`.

---

## Running Tests

#### Run all tests
```shell
make test
```
Coverage report: `build/coverage/index.html`

For granular control, invoke the test runner directly. The container is reused between runs unless `--full-rebuild` is passed.

#### Run all tests with a fresh container
```shell
./scripts/test-runner.sh run-tests --full-rebuild
```

#### Run a specific test suite
```shell
./scripts/test-runner.sh run-tests --test-name "package com.brcsrc.yaws.api.NetworkControllerTests"
```

#### Run a specific test
```shell
./scripts/test-runner.sh run-tests --test-name "package com.brcsrc.yaws.api.NetworkControllerTests.testCreateNetworkClientCreatesClient"
```

#### Run tests matching a pattern
```shell
./scripts/test-runner.sh run-tests --test-name "*testAddClientToNetworkThrowsException*"
```

---

## IntelliJ Setup

1. Open the project by selecting `build.gradle` (not the directory)
2. **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM** → set to Corretto 21
3. Trigger a Gradle sync
4. **File → Invalidate Caches → Invalidate and Restart**

---

## Database

#### Read a table with column names
The default SQLite behavior omits column names from result sets. Use `.headers on` and `.mode column` to include them.
```shell
bash-5.1# sqlite3
SQLite version 3.35.5 2021-04-19 18:32:05
Enter ".help" for usage hints.
Connected to a transient in-memory database.
Use ".open FILENAME" to reopen on a persistent database.
sqlite> .open yaws.db
sqlite> .headers on
sqlite> .mode column
sqlite> SELECT * FROM users;
id  password  user_name
--  --------  ---------
1   changeme  admin
sqlite>
```

---

## API Schema and Client Generation

> the API docs are not exposed unless the app is running and `YAWS_DEV=true` is set in the environment

#### View Swagger UI
http://localhost:8080/swagger-ui/index.html

#### View OpenAPI schema
http://localhost:8080/v3/api-docs

#### Generate client code
The TypeScript API client lives in `src/client/` and is consumed by the frontend via a local package reference. To regenerate it:

```shell
make build-api-spec    # generates build/openapi.json
make build-api-client  # compiles client from spec → build/client/
```

The spec is generated by a Spring test (`OpenApiSpecGeneratorTest`) that loads the full MVC context without binding to a port, so it runs cleanly in CI with no Docker dependency.

---

## AWS EC2 Deployment

YAWS includes a deployment script for quickly spinning up a development environment on AWS EC2.

### Prerequisites
- AWS CLI configured with credentials
- Docker installed locally
- AWS account with permissions to create CloudFormation stacks, ECR repositories, EC2 instances, and IAM roles

### Deploy to EC2

Full deployment (creates infrastructure, builds image, and deploys):
```shell
./scripts/deploy-to-ec2.sh deploy
```

Update app only (skips CloudFormation, rebuilds image and refreshes instances):
```shell
./scripts/deploy-to-ec2.sh deploy --skip-stack
```

Teardown everything:
```shell
./scripts/deploy-to-ec2.sh teardown
```

### What Gets Deployed

The CloudFormation stack (`scripts/ec2-based-infrastructure.yml`) creates:
- **ECR Repository** - Private Docker registry for YAWS images
- **IAM Role** - EC2 instance role with ECR pull and SSM access
- **Security Group** - Allows 443/tcp (restricted to deployer IP) and 51820/udp (open for VPN)
- **Auto Scaling Group** - Single t3.small instance running Amazon Linux 2023
- **Elastic IP** - Consistent public IP address

The EC2 instance userdata automatically:
1. Installs Docker
2. Downloads and configures [JnbRelay](https://github.com/brcsrc/JnbRelay) as a TLS proxy (systemd service)
3. Generates self-signed certificate for HTTPS
4. Pulls YAWS image from ECR
5. Runs YAWS container with CORS configured for the instance's public DNS

### Connecting to the Instance

Via SSM Session Manager (no SSH key required):
```shell
aws ssm start-session \
  --target $(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names yaws-dev-asg \
    --region us-west-2 \
    --query 'AutoScalingGroups[0].Instances[0].InstanceId' \
    --output text) \
  --region us-west-2
```

### Options

- `--stack-name NAME` - Use a custom stack name (default: `yaws-dev`)
- `--image-tag TAG` - Use a custom image tag (default: `latest`)

---

## Production JRE Dependencies

The production image uses a minimal JRE built with `jlink` to reduce image size. The module list in the `build-jre` stage of `docker/prod/Dockerfile` must be kept in sync with the application's actual Java module dependencies. If new libraries are added, re-derive the module list as follows:

1. Build the dev image (`docker/dev/Dockerfile`), which includes the full JDK
2. Run `jdeps` against the exploded fat JAR:

```shell
docker run --rm --entrypoint="" yaws-dev:latest sh -c "
  mkdir -p /tmp/app-exploded && \
  cd /tmp/app-exploded && \
  jar -xf /opt/yaws-0.0.1-SNAPSHOT.jar && \
  jdeps --ignore-missing-deps --multi-release 21 --print-module-deps -R --recursive \
    --class-path 'BOOT-INF/lib/*' \
    /opt/yaws-0.0.1-SNAPSHOT.jar 2>/dev/null
"
```

3. Update the `--add-modules` list in the `build-jre` stage of `docker/prod/Dockerfile` with the output

> The dev image installs the full Amazon Corretto 21 JDK so `jdeps` is always available there. The prod image's final stage only contains the minimal JRE and does not include `jdeps`.

---

## Reference Links

#### WireGuard on Alpine
- https://www.cyberciti.biz/faq/how-to-set-up-wireguard-vpn-server-on-alpine-linux/
- https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct
- https://manpages.debian.org/unstable/wireguard-tools/wg.8.en.html

#### Authentication
- https://www.youtube.com/watch?v=9J-b6OlPy24
- https://www.youtube.com/watch?v=HYBRBkYtpeo

#### Fetching public IP
```shell
# via opendns, requires `bind-utils` on alpine
dig +short myip.opendns.com @resolver1.opendns.com

# via ifconfig.me, requires `curl` on alpine
curl ifconfig.me
```
