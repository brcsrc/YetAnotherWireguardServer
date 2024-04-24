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

##### Build for development
```shell
\
docker build . -t yaws && \
docker run --name yaws -d yaws:latest && \
docker exec -it yaws bash
```
if tests fail they should surface in stage 2
```shell
63.95 > Task :test FAILED
63.95 
63.95 ExecutorTests > testWgInstalled() FAILED
63.95     org.opentest4j.AssertionFailedError: expected: <1> but was: <0>
63.95         at app//org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
63.95         at app//org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
63.95         at app//org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
63.95         at app//org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:150)
63.95         at app//org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:145)
63.95         at app//org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:531)
63.95         at app//com.brcsrc.yaws.shell.ExecutorTests.testWgInstalled(ExecutorTests.java:13)
63.95 
63.95 3 tests completed, 1 failed

```

##### Build for development & Skip testing
```shell
\
docker build --build-arg SKIP_TESTS=true . -t yaws && \
docker run --name yaws -d yaws:latest && \
docker exec -it yaws bash
```

##### Run tests outside of run configuration
```shell
./gradlew test --info                         # run all
./gradlew test --tests ExecutorTests --info   # run one
```
## Notes

https://www.cyberciti.biz/faq/how-to-set-up-wireguard-vpn-server-on-alpine-linux/
https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/generic-linux-install.html#alpine-linux-install-instruct


