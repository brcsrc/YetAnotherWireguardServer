# YAWS Typescript API Client
this package is used to create a library node module to make the automatically generated client code from openapi consumable

##### Store the API Model for YAWS API
start the app with `DEV=true`:

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

Then go to the path `/path/to/your/YetAnotherWireguardServer/

yaws-ts-api-client/` and download the schema
```shell
curl http://localhost:8080/v3/api-docs -o ./openapi.json
```

##### Generate the client code from the model
`openapi-generator-cli` is used as a dev dependency in `/path/to/your/YetAnotherWireguardServer/yaws-ts-api-client/`. from that path run
```shell
npx openapi-generator-cli generate \
  -i ./openapi.json \
  -g typescript-fetch \
  -o ./src
```