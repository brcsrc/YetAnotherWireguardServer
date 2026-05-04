# YetAnotherWireguardServer
![license badge](https://img.shields.io/badge/License-MIT-blue)
> 🚧 Under Construction 🚧

YetAnotherWireguardServer is a containerized Wireguard server designed to run anywhere you can run Docker.

## Table of Contents
- [Deploying YetAnotherWireguardServer](#deploying-yetanotherwireguardserver)
  - [Quick Start (docker run)](#quick-start-docker-run)
  - [Docker Compose](#docker-compose)
- [Environment Variables](#environment-variables)
- [Links](#links)

---

## Deploying YetAnotherWireguardServer

### Quick Start (docker run)

1. Clone the repository
```shell
git clone https://github.com/brcsrc/YetAnotherWireguardServer
```
2. Build the image
```shell
cd YetAnotherWireguardServer && docker build -f docker/prod/Dockerfile -t yaws .
```
3. Run with the required network capabilities
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

### Docker Compose

When running YAWS in a compose stack, you must explicitly enable IP masquerading on any custom network. Without it, WireGuard clients will connect but have no internet access — `docker run` works out of the box because Docker's default bridge has masquerading enabled, but custom compose networks do not.

WireGuard traffic requires two NAT hops to reach the internet: client IPs masqueraded to the container IP (handled by YAWS), and the container IP masqueraded to the host's public IP (handled by Docker — but only if the network has masquerading enabled).

```yaml
services:
  yaws:
    image: yaws:latest
    container_name: yaws
    privileged: true
    cap_add:
      - NET_ADMIN
    ports:
      - "0.0.0.0:51820:51820/udp"
      - "0.0.0.0:8080:8080/tcp"
    restart: unless-stopped
    networks:
      yaws-net:
        ipv4_address: 172.25.0.20

networks:
  yaws-net:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.enable_ip_masquerade: "true"
    ipam:
      driver: default
      config:
        - subnet: 172.25.0.0/16
```

---

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DEV` | Enables development mode (allows Swagger UI, permits localhost CORS) | `-e DEV="true"` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed CORS origins, usually needed when deployed behind a proxy | `-e CORS_ALLOWED_ORIGINS="https://example.com,https://app.example.com"` |

---

## Links

- [Development](doc/DEVELOPMENT.md)
- [Spring Docs](doc/HELP.md)
- [Entity Relationship Diagram](doc/yaws-erd.drawio)
