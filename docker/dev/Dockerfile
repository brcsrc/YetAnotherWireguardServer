### stage 1 build web client ###
FROM node:23-alpine AS build-frontend
WORKDIR /app
COPY src/client /app/src/client/
WORKDIR /app/src/client
RUN npm install && npm run build
WORKDIR /app
COPY src/frontend/ /app/src/frontend/
WORKDIR /app/src/frontend
RUN npm install && npm run build

### stage 2 build java artifact ###
FROM gradle:8.9.0-jdk21-alpine AS build-spring
WORKDIR /opt
COPY build.gradle settings.gradle ./
# this stage assumes we have already ran the gradle steps in the README
# i.e. ./gradlew downloadGradleBin && ./gradlew copyDependenciesToLocalRepo
# this is to reduce build time as much as we can
COPY gradle gradle
COPY gradlew gradlew
COPY src/shell/* .
COPY src/backend/ src/backend/
COPY lib/ lib/
COPY --from=build-frontend /app/build/frontend /opt/src/backend/main/resources/static
# skipping tests here as we want to build only and not modify container. also system
# level dependencies like wireguard are not in the environment at this point and tests wont pass here
RUN ./gradlew build -x test --no-daemon;

#### stage 3 build environment ###
FROM alpine:3.23 AS final
WORKDIR /opt
# https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct
RUN wget -O /etc/apk/keys/amazoncorretto.rsa.pub  https://apk.corretto.aws/amazoncorretto.rsa.pub && \
    echo "https://apk.corretto.aws/" >> /etc/apk/repositories && \
    apk update && \
    apk add amazon-corretto-21

RUN apk add --no-cache openrc wireguard-tools iproute2 sqlite iptables
COPY --from=build-spring /opt/build/backend/yaws-0.0.1-SNAPSHOT.jar .
COPY src/shell/* .
COPY docker/prod/entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]
