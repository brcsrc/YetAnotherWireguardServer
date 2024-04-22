# YetAnotherWireguardServer

## Development

##### Build
```shell
docker build . -t yaws
```

##### Run
```shell
docker run --name yaws -d yaws:latest
```

##### Get shell in container
```shell
docker exec -it yaws bash
```


```shell
\
docker build . -t yaws && \
docker run --name yaws -d yaws:latest && \
docker exec -it yaws bash
```

## Notes

https://www.cyberciti.biz/faq/how-to-set-up-wireguard-vpn-server-on-alpine-linux/


