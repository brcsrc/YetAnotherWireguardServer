# YetAnotherWireguardServer

## Development

##### cache java dependencies locally
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
```shell
\
docker build -f docker/prod/Dockerfile -t yaws . && \
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -p 0.0.0.0:51820:51820/udp \
-p 0.0.0.0:8080:8080/tcp \
 --name yaws \
 -d yaws:latest && \
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
https://www.baeldung.com/spring-boot-sqlite
https://stackoverflow.com/questions/62522444/access-sqlite-db-file-with-jpa
https://github.com/Semo/spring-jpa-sqlite-sample/blob/master/src/main/java/dev/mutiny/semo/config/SQLiteDataTypesConfig.java