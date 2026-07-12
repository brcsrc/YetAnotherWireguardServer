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
COPY src/shell/* .
COPY src/backend/ src/backend/
COPY --from=build-frontend /app/build/frontend /opt/src/backend/main/resources/static
RUN gradle wrapper
RUN ./gradlew build -x test --no-daemon;

### stage 3 build minimal JRE with jlink ###
FROM gradle:8.9.0-jdk21-alpine AS build-jre
# modules derived from jdeps analysis of the fat JAR (see DEVELOPMENT.md)
# jdk.crypto.ec and jdk.crypto.cryptoki are not reported by jdeps but are required
# at runtime for outbound TLS/SSL handshakes (ECDHE cipher suites)
RUN jlink \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql.rowset,jdk.jfr,jdk.unsupported \
    --add-modules jdk.crypto.ec,jdk.crypto.cryptoki \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /opt/jre
RUN cp /opt/java/openjdk/lib/security/cacerts /opt/jre/lib/security/cacerts

### stage 4 build environment ###
FROM alpine:3.23 AS final
WORKDIR /opt
COPY --from=build-jre /opt/jre /opt/jre
ENV PATH="/opt/jre/bin:$PATH"
RUN apk add --no-cache openrc wireguard-tools iproute2 sqlite iptables
COPY --from=build-spring /opt/build/backend/yaws-0.0.1-SNAPSHOT.jar .
COPY src/shell/* .
COPY docker/prod/entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["entrypoint.sh"]
