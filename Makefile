SHELL := /bin/bash

YELLOW := \033[0;33m
GREEN  := \033[0;32m
RED    := \033[0;31m
RESET  := \033[0m

define run
	@printf '$(YELLOW)RUNNING $(1)$(RESET)\n'
	@$(2) && printf '$(GREEN)$(1) SUCCESSFUL$(RESET)\n' || { printf '$(RED)$(1) FAILED$(RESET)\n'; exit 1; }
endef

.PHONY: setup \
        build \
        build-backend \
        build-api-spec \
        build-api-client \
        build-frontend \
        test \
        image \
        dev-image \
        run-dev \
        clean

_setup_gradle:
	$(call run,_setup_gradle,./gradlew downloadGradleBin && ./gradlew copyDependenciesToLocalRepo)

_setup_client:
	$(call run,_setup_client,cd src/client && npm install)

_setup_frontend:
	$(call run,_setup_frontend,cd src/frontend && npm install)

setup: _setup_gradle _setup_client _setup_frontend

build-backend:
	$(call run,build-backend,./gradlew build -x test --no-daemon)

build-api-spec:
	$(call run,build-api-spec,./gradlew test --tests com.brcsrc.yaws.OpenApiSpecGeneratorTest --no-daemon)

build-api-client:
	$(call run,build-api-client,cd src/client && npm install && npm run build)

build-frontend:
	$(call run,build-frontend,cd src/frontend && npm install && npm run build)

build: build-backend build-api-spec build-api-client build-frontend

test:
	$(call run,test,./scripts/test-runner.sh run-tests)

image:
	$(call run,image,docker build -f docker/prod/Dockerfile -t yaws:latest .)

dev-image:
	$(call run,dev-image,docker build -f docker/dev/Dockerfile -t yaws-dev:latest .)

run-dev:
	@docker stop yaws-dev >/dev/null 2>&1 || true
	@docker rm yaws-dev >/dev/null 2>&1 || true
	$(call run,run-dev-backend,docker run \
		--privileged \
		--cap-add=NET_ADMIN \
		-e YAWS_DEV="true" \
		-e YAWS_CORS_ALLOWED_ORIGINS="http://localhost:5173" \
		-p 0.0.0.0:51820:51820/udp \
		-p 0.0.0.0:8080:8080/tcp \
		--name yaws-dev \
		-d \
		yaws-dev:latest)
	cd src/frontend && npm run dev

clean:
	$(call run,clean,./gradlew clean && rm -rf build/ src/frontend/node_modules src/client/node_modules gradle/wrapper/*.zip lib/ .gradle/)
