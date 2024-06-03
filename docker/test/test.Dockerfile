FROM gradle:8.7.0-jdk21-alpine
WORKDIR /opt
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew gradlew
COPY shell/* .
COPY src/ src/
COPY lib/ lib/
RUN apk update && \
    apk add openrc wireguard-tools iproute2 iptables
COPY docker/test/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]