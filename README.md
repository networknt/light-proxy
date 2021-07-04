A fast and light-weight reverse proxy with embedded gateway to wrap third party APIs and bring them to the ecosystem of light-4j frameworks

[Stack Overflow](https://stackoverflow.com/questions/tagged/light-4j) |
[Google Group](https://groups.google.com/forum/#!forum/light-4j) |
[Gitter Chat](https://gitter.im/networknt/light-proxy) |
[Subreddit](https://www.reddit.com/r/lightapi/) |
[Youtube Channel](https://www.youtube.com/channel/UCHCRMWJVXw8iB7zKxF55Byw) |
[Documentation](https://doc.networknt.com/service/proxy/) |
[Contribution Guide](https://doc.networknt.com/contribute/) |

[![Build Status](https://travis-ci.org/networknt/light-proxy.svg?branch=master)](https://travis-ci.org/networknt/light-proxy)

## Why Reverse Proxy

All the services developed on top of light-4j frameworks support [client side service discovery](http://microservices.io/patterns/client-side-discovery.html), 
load balance and cluster natively. So there is no need to put a reverse proxy instance in front of our
services like other API frameworks that support only [server side service discovery](http://microservices.io/patterns/server-side-discovery.html).

Also, light services embed a distributed gateway to address all the cross-cutting concerns in the 
request/response chain and work with the ecosystem that consists:

* [light-oauth2](https://doc.networknt.com/service/oauth/) for security
* [light-portal](https://github.com/networknt/light-portal) for API management and market place
* [light-config-server](https://github.com/networknt/light-config-server) for centralized configuration management
* [light-eventuate-4j](https://doc.networknt.com/style/light-eventuate-4j/) for eventual consistency based on event sourcing, CQRS and Kafka
* [ELK](https://www.elastic.co/webinars/introduction-elk-stack) for centralized logging with traceabilityId and correlationId
* [InfluxDB](https://github.com/influxdata/influxdb) and [Grafana](https://github.com/grafana/grafana) for metrics
* [Consul](https://github.com/hashicorp/consul) or [Zookeeper](http://zookeeper.apache.org/) for service registry
* [Kubernetes](https://kubernetes.io/) for container orchestration

Currently, we only support Java language; however, we are planning to support Nodejs and Go in the future
if there are enough customer demands. For some of our customers, they have some existing RESTful APIs that
built on top of other Java frameworks or other languages. We've been asked frequently on how to interact
with these services to/from light services and how to enable security, metrics, logging, discovery, 
validation, sanitization etc. on the existing services. 

Our answer is to deploy a reverse proxy built on top of light-4j framework that wraps the existing service. 

The reverse proxy has the following features:

* High throughput, low latency and small footprint. 
* Integrate light-oauth2 to protect un-secured services
* Built-in load balancer
* Can be started with Docker or standalone
* Support HTTP 2.0 protocol on both in/out connections
* TLS termination
* Support REST, GraphQL and RPC style of APIs
* Centralized logging with ELK, traceabilityId and CorrelationId
* Collect client and service metrics into InfluxDB and view the dashboard on Grafana
* Service registry and discovery with Consul or Zookeeper
* Manage configuration with light-config-server

To learn how to use this proxy, pleases refer to 

* [Getting Started](https://doc.networknt.com/getting-started/light-proxy/) to learn core concepts
* [Tutorial](https://doc.networknt.com/tutorial/proxy/) with step by step guide for RESTful proxy
* [Configuration](https://doc.networknt.com/service/proxy/configuration/) for different configurations based on your situations
* [Artifact](https://doc.networknt.com/service/proxy/artifact/) to guide customer to choose the right artifact to deploy light-proxy.


### Sample on local environment

- Start a sample NodeJs API:

```

 cd ~/networknt
 git clone git@github.com:networknt/light-proxy.git

```

Follow the [steps](nodeapp/start.md) to start Nodejs books store restful API. The Nodejs api will start on local port: 8080 

We can verify the Nodejs restful API directly with curl command:

```
Get:

curl --location --request GET 'http://localhost:8080/api/books/' \
--header 'Content-Type: application/json' \
--data-raw '{"name":"mybook"}'

Post:

curl --location --request POST 'http://localhost:8080/api/books/' \
--header 'Content-Type: application/json' \
--data-raw '{"title":"Newbook"}'

Put:

curl --location --request POST 'http://localhost:8080/api/books/' \
--header 'Content-Type: application/json' \
--data-raw '{"title":"Newbook"}'

Delete:

curl --location --request DELETE 'http://localhost:8080/api/books/4' \
--header 'Content-Type: application/json' \
```

Now let's use light-proxy to leverage the light platform cross-cutting concerns:

- Build and start Proxy server:

```

cd light-proxy
mvn clean install
java -jar -Dlight-4j-config-dir=config/local  target/light-proxy.jar

```

The light-proxy will start based on the config on /config/local

Since we are trying to verify the light-proxy concept on local environment only. The sample will only verify schema validation handler for cross-cutting concerns.

The openapi specification for the book store API located at [here](config/local/openapi.yaml)

Access bookstore nodejs API through light-proxy:  
 
GET:
 
```

https://localhost:9445/api/books/
```

POST:
 
```
https://localhost:9445/api/books/

request body:

{"title":"New Book"}

```

Create a book with schema validation error: book title is required field

POST

```
https://localhost:9445/api/books/

request body:

{"author":"Steve Jobs"}

```

Response:

```
{
    "statusCode": 400,
    "code": "ERR11004",
    "message": "VALIDATOR_SCHEMA",
    "description": "Schema Validation Error - requestBody.title: is missing but it is required",
    "severity": "ERROR"
}

```


