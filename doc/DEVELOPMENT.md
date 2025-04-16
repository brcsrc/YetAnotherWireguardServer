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
docker build -f docker/prod/Dockerfile -t yaws . && \
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
