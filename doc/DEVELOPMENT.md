# Development

## Gradle Build Helpful Commands
### generate gradle wrapper
generates a gradle wrapper script locally
```shell
gradle wrapper
```
### cache gradle jars and zip locally (sppeds up image build)
the gradle binary is cached in `gradle/` to remove the need to download when starting the container
```shell
./gradlew downloadGradleBin
```
##### cache java dependencies locally (speeds up image build)
the jars from gradle dependencies are cached in `lib/` to remove the need to download them when starting the container
```shell
./gradlew copyDependenciesToLocalRepo
```

##### Build just the spring application without tests
```shell
./gradlew build -x test
```

##### Clean dependencies and rebuild
```shell
./gradlew clean && ./gradlew build -x test
```

##### Build for development
this will start the container locally for behavior testing
```shell
\
docker build -f docker/dev/Dockerfile -t yaws . && \
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -e DEV="true" \
 -p 0.0.0.0:51820:51820/udp \
 -p 0.0.0.0:8080:8080/tcp \
 --name yaws \
 -d \
 yaws:latest && \
docker exec -it yaws bash
```

##### Build for development and pass the sqlite db through to project filesystem
> you have to already have the DB available. you can run the above command and then copy it with `docker cp yaws:/opt/yaws.db .`

this will bind mount the database to a container on run for manually testing 
```shell
\
docker build -f docker/prod/Dockerfile -t yaws . && \
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -p 0.0.0.0:51820:51820/udp \
 -p 0.0.0.0:8080:8080/tcp \
 -v $(pwd)/yaws.db:/opt/yaws.db \
 --name yaws \
 -d \
 yaws:latest && \
docker exec -it yaws bash
```

##### build and run backend tests

run all tests. if a test container exists it will be reused, if it does not one will be created prior to tests
```shell
./scripts/test-runner.sh run-tests
```

run all tests with a new container regardless if one exists or not

```shell
./scripts/test-runner.sh run-tests --full-rebuild
```

run all tests of a specific test suite

```shell
./scripts/test-runner.sh run-tests --test-name "package com.brcsrc.yaws.api.NetworkControllerTests"
```

run a specific test in a test suite
```shell
./scripts/test-runner.sh run-tests --test-name "package com.brcsrc.yaws.api.NetworkControllerTests.testCreateNetworkClientCreatesClient"
```

run any test matching the pattern
```shell
./scripts/test-runner.sh run-tests --test-name "*testAddClientToNetworkThrowsException*"
```

## YAWS Database manipulation
### read table with column names as well as values 
the default behavior does not respond with result sets with column names, only column values
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

## API Schema and client generation
##### View Swagger UI
http://localhost:8080/swagger-ui/index.html

##### View OpenAPI schema
http://localhost:8080/v3/api-docs

##### generating client code

[see the API client README](../yaws-frontend/api-client/README.md)




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

## Notes

##### wireguard on alpine
https://www.cyberciti.biz/faq/how-to-set-up-wireguard-vpn-server-on-alpine-linux/
https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct
https://manpages.debian.org/unstable/wireguard-tools/wg.8.en.html

##### authentication
https://www.youtube.com/watch?v=9J-b6OlPy24
https://www.youtube.com/watch?v=HYBRBkYtpeo

##### fetching public ip
```shell
# via opendns, requires `bind-utils` on alpine
dig +short myip.opendns.com @resolver1.opendns.com

# via ifconfig.me, requires `curl` on alpine
curl ifconfig.me
```
