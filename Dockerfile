# multistage build
# stage 1 build web client

# stage 2 build java artifact
FROM gradle:8.7.0-jdk21 as build-backend
WORKDIR /opt
COPY . .
RUN ./gradlew build

# stage 3 build environment
FROM alpine:3.14 as final
WORKDIR /opt
# https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct
RUN wget -O /etc/apk/keys/amazoncorretto.rsa.pub  https://apk.corretto.aws/amazoncorretto.rsa.pub && \
    echo "https://apk.corretto.aws/" >> /etc/apk/repositories && \
    apk update && \
    apk add --no-cache wireguard-tools iproute2 amazon-corretto-21
COPY --from=build-backend /opt/build/libs/yaws-0.0.1-SNAPSHOT.jar .

CMD ["tail", "-f", "/dev/null"]