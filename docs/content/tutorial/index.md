---
date: 2017-09-15T18:24:39-04:00
title: Tutorial
---

In this tutorial, we are going to use RESTful APIs as an example to show how to
deployed the light-proxy and enable all sorts of middleware handlers through
configurations. We also explore standalone deployment and Docker deployment. 

With specification handler, security handler and validation handler change, you
can easily update the configuration to make the same instance of light-proxy
server work with GraphQL backend. The default configuration is for RESTful backend
only as light-rest-4j middleware handlers are wired in the reqeust/response chain.

To make it simple, we will build a backend service with light-rest-4j with a
swagger specification file defined in [model-config](https://github.com/networknt/model-config) 
repo. In real production environment, the backend service can be built with other 
Java framework or other languages like Nodejs, Go etc.

The backend service will have too endpoints: one get and one post to demo different
scenarios in term of proxy functionality.

## Backend service specification and light-codegen config.json

You can find the swagger spec. and config.json [here](https://github.com/networknt/model-config/tree/master/rest/reverse-proxy)

Regarding to how to create Swagger Specification(Named OpenAPI Specification now),
please refer to [this document](https://networknt.github.io/light-rest-4j/tool/swagger-editor/)

## Generate the backend serivce

Given the swagger specification and config.json, we are going to use light-codegen
to generate the project and update the service with meaningful response based on the
specification.

```
cd ~/networknt
rm -rf light-example-4j/rest/light-proxy-backend
java -jar light-codegen/codegen-cli/target/codegen-cli.jar -f light-rest-4j -o light-example-4j/rest/light-proxy-backend -m model-config/rest/reverse-proxy/swagger.json -c model-config/rest/reverse-proxy/config.json
```

The newly updated handlers can be found [here](https://github.com/networknt/light-example-4j/tree/master/rest/light-proxy-backend/src/main/java/com/networknt/backend/handler)
and these files along with test cases are the only ones changed after light-codegen.

## Start backend services

If you have multiple computers you can start multiple instance that listening on the same
port on each computer. Otherwise, start multiple instances that listening to different ports
on one computer. Here we are going to start three instances on the same computer and let
them listening to 8081, 8082 and 8083 on http. Let's disable the https for now.

If you generate the light-proxy-backend service you should have the project on your local
light-example-4j/rest folder already. If not, let's clone the repo and compile it locally.

```
cd ~/networknt
git clone git@github.com:networknt/light-example-4j.git
```
In order to start three instances with different http ports, we need to update server.yml which
is located at light-proxy-backend/src/main/resources/config folder before each start. The following 
is the default one generated based on the config.json file. As you can see, http is disabled 
and https is enabled and listen to 8443 port. Let's enable http and disable https and change 
http port from 8080 to 8081.  

```yaml

# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort:  8080

# Enable HTTP should be false on official environment.
enableHttp: false

# Https port if enableHttps is true.
httpsPort:  8443

# Enable HTTPS should be true on official environment.
enableHttps: true

# Http/2 is enabled by default.
enableHttp2: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: com.networknt.backend-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```

Here is the server.yml after the change.

```yaml

# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort:  8081

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort:  8443

# Enable HTTPS should be true on official environment.
enableHttps: false

# Http/2 is enabled by default.
enableHttp2: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: com.networknt.backend-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```

Now let's start the first instance.

```
cd ~/networknt/light-example-4j/rest/light-proxy-backend
mvn clean install exec:exec
```

Now from another terminal, you can issue a curl command to ensure server is running
and listening on 8081 on http.

```
curl http://localhost:8081/v1/getData
```

Result:

```json
{"enableHttp2":true,"httpPort":8081,"enableHttps":false,"value":"value1","httpsPort":8443,"key":"key1"}
```

You can also test the post request with the following command line.

```
curl -X POST   http://localhost:8081/v1/postData   -H 'content-type: application/json'   -d '{"key":"key1", "value": "value1"}'
```

And the request should be something like this.

```json
{"enableHttps":false,"value":"value1","httpsPort":8443,"key":"key1","enableHttp2":true,"httpPort":8081}
```

Now let's start another service in another terminal after updating the port number to
8082 in server.yml with the same command lines above.

Here is the updated server.yml

```yaml

# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort:  8082

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort:  8443

# Enable HTTPS should be true on official environment.
enableHttps: false

# Http/2 is enabled by default.
enableHttp2: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: com.networknt.backend-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

``` 

Test the second instance with command:

```
curl http://localhost:8082/v1/getData
```

Now let's start the third instance with port number 8083 in server.yml file. Here is the
updated server.yml

```yaml

# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort:  8083

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort:  8443

# Enable HTTPS should be true on official environment.
enableHttps: false

# Http/2 is enabled by default.
enableHttp2: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: com.networknt.backend-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```

Test the third instance with command:

```
curl http://localhost:8083/v1/getData
```

At this moment, we have three backend service instances running and they are listening
to 8081, 8082 and 8083 on http protocol.

## light-proxy configuration

With the backend services ready, let's clone the light-proxy and make some configuration
changes. 

```
cd ~/networknt
git clone git@github.com:networknt/light-proxy.git
```

Now you can add a proxy.yml in light-proxy/src/main/resources/config folder as following.

```yaml
# Reverse Proxy Handler Configuration

# If HTTP 2.0 protocol will be used to connect to target servers
http2Enabled: false

# If TLS is enabled when connecting to the target servers
httpsEnabled: false

# Target URIs
hosts: http://localhost:8081,http://localhost:8082,http://localhost:8083

# Connections per thread to the target servers
connectionsPerThread: 20

# Max request time in milliseconds before timeout
maxRequestTime: 10000

``` 

As we are going to enable Security and Validator based on swagger.json from backend server,
let's copy the swagger.json from light-example-4j/rest/light-proxy-backend/src/main/resources/config folder.

```
cd ~/networknt
cp light-example-4j/rest/light-proxy-backend/src/main/resources/config/swagger.json light-proxy/src/main/resources/config
```

## start light-proxy and test

```
cd ~/networknt/light-proxy
mvn clean install -DskipTests
mvn exec:exec
```

Given the light-proxy server.yml only have https enabled and listening to port 8080, let's
use curl to test it.

```
curl -k https://localhost:8080/v1/getData
```

And we get result:

```
curl -k https://localhost:8080/v1/getData
{"enableHttp2":true,"httpPort":8082,"enableHttps":false,"value":"value1","httpsPort":8443,"key":"key1"}
curl -k https://localhost:8080/v1/getData
{"enableHttp2":true,"httpPort":8083,"enableHttps":false,"value":"value1","httpsPort":8443,"key":"key1"}
curl -k https://localhost:8080/v1/getData
{"enableHttp2":true,"httpPort":8081,"enableHttps":false,"value":"value1","httpsPort":8443,"key":"key1"}
curl -k https://localhost:8080/v1/getData
{"enableHttp2":true,"httpPort":8082,"enableHttps":false,"value":"value1","httpsPort":8443,"key":"key1"}
```

As you can see the proxy server is working on http connection to the backend services. And 
the response is from different instances for each request due to load balance. You can stop 
one of the three instances and the proxy will automatically failover to working instances.

Also, you can test the post request with the following command line.

```
curl -k -X POST \
  https://localhost:8080/v1/postData \
  -H 'content-type: application/json' \
  -d '{"key":"key1", "value": "value1"}'
```

And we hit the error in Undertow.

```
14:04:03.357 [XNIO-1 I/O-1]   ERROR io.undertow.proxy handleFailure - UT005028: Proxy request to /v1/postData failed
io.undertow.server.TruncatedResponseException: null
	at io.undertow.client.http.HttpRequestConduit.truncateWrites(HttpRequestConduit.java:711)
	at io.undertow.conduits.AbstractFixedLengthStreamSinkConduit.terminateWrites(AbstractFixedLengthStreamSinkConduit.java:256)
	at org.xnio.conduits.ConduitStreamSinkChannel.shutdownWrites(ConduitStreamSinkChannel.java:178)
	at io.undertow.channels.DetachableStreamSinkChannel.shutdownWrites(DetachableStreamSinkChannel.java:79)
	at io.undertow.server.handlers.proxy.ProxyHandler$HTTPTrailerChannelListener.handleEvent(ProxyHandler.java:736)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction$1.completed(ProxyHandler.java:628)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction$1.completed(ProxyHandler.java:543)
	at io.undertow.client.http.HttpClientExchange.invokeReadReadyCallback(HttpClientExchange.java:212)
	at io.undertow.client.http.HttpClientConnection.initiateRequest(HttpClientConnection.java:418)
	at io.undertow.client.http.HttpClientConnection.sendRequest(HttpClientConnection.java:350)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction.run(ProxyHandler.java:543)
	at io.undertow.util.SameThreadExecutor.execute(SameThreadExecutor.java:35)
	at io.undertow.server.HttpServerExchange.dispatch(HttpServerExchange.java:797)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyClientHandler.completed(ProxyHandler.java:298)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyClientHandler.completed(ProxyHandler.java:272)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool.connectionReady(ProxyConnectionPool.java:338)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool.access$900(ProxyConnectionPool.java:61)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool$2.completed(ProxyConnectionPool.java:286)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool$2.completed(ProxyConnectionPool.java:273)
	at io.undertow.client.http.HttpClientProvider.handleConnected(HttpClientProvider.java:156)
	at io.undertow.client.http.HttpClientProvider.access$000(HttpClientProvider.java:51)
	at io.undertow.client.http.HttpClientProvider$2.handleEvent(HttpClientProvider.java:127)
	at io.undertow.client.http.HttpClientProvider$2.handleEvent(HttpClientProvider.java:124)
	at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
	at org.xnio.nio.WorkerThread$ConnectHandle.handleReady(WorkerThread.java:326)
	at org.xnio.nio.WorkerThread.run(WorkerThread.java:561)
14:05:03.462 [XNIO-1 I/O-1]   ERROR io.undertow.proxy handleFailure - UT005028: Proxy request to /v1/postData failed
java.io.IOException: UT001000: Connection closed
	at io.undertow.client.http.HttpClientConnection$ClientReadListener.handleEvent(HttpClientConnection.java:573)
	at io.undertow.client.http.HttpClientConnection$ClientReadListener.handleEvent(HttpClientConnection.java:511)
	at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
	at org.xnio.conduits.ReadReadyHandler$ChannelListenerHandler.readReady(ReadReadyHandler.java:66)
	at org.xnio.nio.NioSocketConduit.handleReady(NioSocketConduit.java:88)
	at org.xnio.nio.WorkerThread.run(WorkerThread.java:561)
14:05:37.434 [XNIO-1 I/O-5]   ERROR io.undertow.proxy handleFailure - UT005028: Proxy request to /v1/postData failed
io.undertow.server.TruncatedResponseException: null
	at io.undertow.client.http.HttpRequestConduit.truncateWrites(HttpRequestConduit.java:711)
	at io.undertow.conduits.AbstractFixedLengthStreamSinkConduit.terminateWrites(AbstractFixedLengthStreamSinkConduit.java:256)
	at org.xnio.conduits.ConduitStreamSinkChannel.shutdownWrites(ConduitStreamSinkChannel.java:178)
	at io.undertow.channels.DetachableStreamSinkChannel.shutdownWrites(DetachableStreamSinkChannel.java:79)
	at io.undertow.server.handlers.proxy.ProxyHandler$HTTPTrailerChannelListener.handleEvent(ProxyHandler.java:736)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction$1.completed(ProxyHandler.java:628)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction$1.completed(ProxyHandler.java:543)
	at io.undertow.client.http.HttpClientExchange.invokeReadReadyCallback(HttpClientExchange.java:212)
	at io.undertow.client.http.HttpClientConnection.initiateRequest(HttpClientConnection.java:418)
	at io.undertow.client.http.HttpClientConnection.sendRequest(HttpClientConnection.java:350)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction.run(ProxyHandler.java:543)
	at io.undertow.util.SameThreadExecutor.execute(SameThreadExecutor.java:35)
	at io.undertow.server.HttpServerExchange.dispatch(HttpServerExchange.java:797)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyClientHandler.completed(ProxyHandler.java:298)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyClientHandler.completed(ProxyHandler.java:272)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool.connectionReady(ProxyConnectionPool.java:338)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool.access$900(ProxyConnectionPool.java:61)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool$2.completed(ProxyConnectionPool.java:286)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool$2.completed(ProxyConnectionPool.java:273)
	at io.undertow.client.http.HttpClientProvider.handleConnected(HttpClientProvider.java:156)
	at io.undertow.client.http.HttpClientProvider.access$000(HttpClientProvider.java:51)
	at io.undertow.client.http.HttpClientProvider$2.handleEvent(HttpClientProvider.java:127)
	at io.undertow.client.http.HttpClientProvider$2.handleEvent(HttpClientProvider.java:124)
	at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
	at org.xnio.nio.WorkerThread$ConnectHandle.handleReady(WorkerThread.java:326)
	at org.xnio.nio.WorkerThread.run(WorkerThread.java:561)
14:05:50.670 [XNIO-1 I/O-1]   ERROR io.undertow.proxy handleFailure - UT005028: Proxy request to /v1/postData failed
io.undertow.server.TruncatedResponseException: null
	at io.undertow.client.http.HttpRequestConduit.truncateWrites(HttpRequestConduit.java:711)
	at io.undertow.conduits.AbstractFixedLengthStreamSinkConduit.terminateWrites(AbstractFixedLengthStreamSinkConduit.java:256)
	at org.xnio.conduits.ConduitStreamSinkChannel.shutdownWrites(ConduitStreamSinkChannel.java:178)
	at io.undertow.channels.DetachableStreamSinkChannel.shutdownWrites(DetachableStreamSinkChannel.java:79)
	at io.undertow.server.handlers.proxy.ProxyHandler$HTTPTrailerChannelListener.handleEvent(ProxyHandler.java:736)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction$1.completed(ProxyHandler.java:628)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction$1.completed(ProxyHandler.java:543)
	at io.undertow.client.http.HttpClientExchange.invokeReadReadyCallback(HttpClientExchange.java:212)
	at io.undertow.client.http.HttpClientConnection.initiateRequest(HttpClientConnection.java:418)
	at io.undertow.client.http.HttpClientConnection.sendRequest(HttpClientConnection.java:350)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyAction.run(ProxyHandler.java:543)
	at io.undertow.util.SameThreadExecutor.execute(SameThreadExecutor.java:35)
	at io.undertow.server.HttpServerExchange.dispatch(HttpServerExchange.java:797)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyClientHandler.completed(ProxyHandler.java:298)
	at io.undertow.server.handlers.proxy.ProxyHandler$ProxyClientHandler.completed(ProxyHandler.java:272)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool.connectionReady(ProxyConnectionPool.java:338)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool.access$900(ProxyConnectionPool.java:61)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool$2.completed(ProxyConnectionPool.java:286)
	at io.undertow.server.handlers.proxy.ProxyConnectionPool$2.completed(ProxyConnectionPool.java:273)
	at io.undertow.client.http.HttpClientProvider.handleConnected(HttpClientProvider.java:156)
	at io.undertow.client.http.HttpClientProvider.access$000(HttpClientProvider.java:51)
	at io.undertow.client.http.HttpClientProvider$2.handleEvent(HttpClientProvider.java:127)
	at io.undertow.client.http.HttpClientProvider$2.handleEvent(HttpClientProvider.java:124)
	at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
	at org.xnio.nio.WorkerThread$ConnectHandle.handleReady(WorkerThread.java:326)
	at org.xnio.nio.WorkerThread.run(WorkerThread.java:561)
14:06:37.544 [XNIO-1 I/O-5]   ERROR io.undertow.proxy handleFailure - UT005028: Proxy request to /v1/postData failed
java.io.IOException: UT001000: Connection closed
	at io.undertow.client.http.HttpClientConnection$ClientReadListener.handleEvent(HttpClientConnection.java:573)
	at io.undertow.client.http.HttpClientConnection$ClientReadListener.handleEvent(HttpClientConnection.java:511)
	at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
	at org.xnio.conduits.ReadReadyHandler$ChannelListenerHandler.readReady(ReadReadyHandler.java:66)
	at org.xnio.nio.NioSocketConduit.handleReady(NioSocketConduit.java:88)
	at org.xnio.nio.WorkerThread.run(WorkerThread.java:561)
14:06:50.779 [XNIO-1 I/O-1]   ERROR io.undertow.proxy handleFailure - UT005028: Proxy request to /v1/postData failed
java.io.IOException: UT001000: Connection closed
	at io.undertow.client.http.HttpClientConnection$ClientReadListener.handleEvent(HttpClientConnection.java:573)
	at io.undertow.client.http.HttpClientConnection$ClientReadListener.handleEvent(HttpClientConnection.java:511)
	at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
	at org.xnio.conduits.ReadReadyHandler$ChannelListenerHandler.readReady(ReadReadyHandler.java:66)
	at org.xnio.nio.NioSocketConduit.handleReady(NioSocketConduit.java:88)
	at org.xnio.nio.WorkerThread.run(WorkerThread.java:561)

```
