# YetAnotherWireguardServer

## Development

##### cache java dependencies locally
```shell
./gradlew copyDependenciesToLocalRepo
```

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
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -p 0.0.0.0:51820:51820/udp \
 --name yaws \
 -d yaws:latest && \
docker exec -it yaws bash
```

##### build and run tests
```shell
docker build --build-arg TEST_ONLY=true . -t yaws
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
docker run \
 --privileged \
 --cap-add=NET_ADMIN \
 -p 0.0.0.0:51820:51820/udp \
 --name yaws \
 -d yaws:latest && \
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

##### write conf to file
```java
import java.io.FileWriter;
import java.io.IOException;

public class WriteToFileExample {
    public static void main(String[] args) {
        String filePath = "/etc/wireguard/wg0.conf";
        String content = "[Interface]\n" +
                         "ListenPort = 51820\n" +
                         "PrivateKey = foo=\n" +
                         "[Peer]";
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }
}

```

## Setting up Wireguard
data is conventionally stored here
```shell
cd /etc/wireguard
```
generate server key pair
```shell
umask 077; wg genkey | tee privatekey | wg pubkey > publickey
```

setup wg conf
```shell
echo '
[Interface]
Address = 10.100.0.1/24
ListenPort = 51820
PrivateKey = <server priv key>
[Peer]
PublicKey = <client pub key>
AllowedIPs = 10.100.0.2/32
' > /etc/wireguard/wg0.conf
```

create ip interface
```shell
echo '
auto wg0
iface wg0 inet static
	address 10.100.0.1
	netmask 255.255.255.0
	pre-up ip link add dev wg0 type wireguard
	pre-up wg setconf wg0 /etc/wireguard/wg0.conf
	post-up ip route add 10.100.0.1/24 dev wg0
	post-down ip link delete wg0
' > /etc/network.d/wg0.conf
```

edit iptable config
```shell
echo '
# /etc/conf.d/iptables
IPTABLES_SAVE="/etc/iptables/rules-save"
SAVE_RESTORE_OPTIONS="-c"
SAVE_ON_STOP="yes"
IPFORWARD="yes"
' > /etc/conf.d/iptables
```

enable packet forwarding
```shell
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf 

sysctl -w net.ipv4.ip_forward=1
```

create client key
```shell
umask 077; wg genkey | tee client_privatekey | wg pubkey > client_publickey
```

create client config
```shell
echo '
[Interface]
PrivateKey = <client priv key>
Address = 10.100.0.2/24
[Peer]
PublicKey = <serverpublic key>
Endpoint = 127.0.0.1:51820 
AllowedIPs = 0.0.0.0/0 
' > client.conf
```
\