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

##### View Swagger UI
http://localhost:8080/swagger-ui/index.html

##### View OpenAPI schema
http://localhost:8080/v3/api-docs

##### build and run backend tests
```shell
\
docker build -f docker/test/test.Dockerfile -t yaws-tests . && \
docker run --rm \
 --privileged \
 --cap-add=NET_ADMIN \
 --name yaws-tests \
 yaws-tests:latest 
```

## Notes

https://www.cyberciti.biz/faq/how-to-set-up-wireguard-vpn-server-on-alpine-linux/
https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct
https://manpages.debian.org/unstable/wireguard-tools/wg.8.en.html
