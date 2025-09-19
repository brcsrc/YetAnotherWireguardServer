# YetAnotherWireguardServer
![license badge](https://img.shields.io/badge/License-MIT-blue)
> 🚧 Under Construction 🚧

YetAnotherWireguardServer is a containerized Wireguard server designed to run anywhere you can run Docker.

##### Try it yourself
1. Clone the Repository
```shell
git clone https://github.com/brcsrc/YetAnotherWireguardServer
```
2. Build the Image
> Requires Docker
```shell
cd YetAnotherWireguardServer && docker build -f docker/prod/Dockerfile -t yaws .
```
3. Run the Application with necessary network settings
```shell
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -p 0.0.0.0:51820:51820/udp \
 -p 0.0.0.0:8080:8080/tcp \
 --name yaws \
 -d \
 yaws:latest
```


##### *Links*

- [Development](doc/DEVELOPMENT.md)
- [Spring Docs](doc/HELP.md)
- [Entity Relationship Diagram](doc/yaws-erd.drawio)
- [Insomnia Request Model](doc/Insomnia_2025-03-20.json)