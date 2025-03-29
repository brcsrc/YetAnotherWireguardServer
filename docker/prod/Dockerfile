### stage 1 build web client ###
FROM node:18-alpine as build-frontend
WORKDIR /app
COPY yaws-frontend/package.json yaws-frontend/package-lock.json ./
RUN npm install
COPY yaws-frontend/ ./
RUN npm run build

### stage 2 build java artifact ###
FROM gradle:8.9.0-jdk21-alpine as build-spring
WORKDIR /opt
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew gradlew
COPY shell/* .
COPY src/ src/
COPY lib/ lib/

# Copy frontend build files into Spring Boot's static directory
COPY --from=build-frontend /app/dist /opt/src/main/resources/static

RUN ./gradlew build -x test --no-daemon;

#### stage 3 build environment ###
FROM alpine:3.14 as final
WORKDIR /opt
# https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct
RUN wget -O /etc/apk/keys/amazoncorretto.rsa.pub  https://apk.corretto.aws/amazoncorretto.rsa.pub && \
    echo "https://apk.corretto.aws/" >> /etc/apk/repositories && \
    apk update && \
    apk add amazon-corretto-21

RUN apk add --no-cache openrc wireguard-tools iproute2 sqlite
COPY --from=build-spring /opt/build/libs/yaws-0.0.1-SNAPSHOT.jar .
COPY shell/* .
COPY docker/prod/entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]