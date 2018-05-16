 
Introduction
===
This component is a RESTful api micro-service built using [vert.x](https://vertx.io/). It supports the following features/purposes:

1. Provide RESTful access to NeLS core features
2. Orchestra asynchronous jobs management by interfacing with [Rabbit MQ](https://www.rabbitmq.com/) 

How to build
===
mvn clean package 

Dependencies
===

## NeLS component dependency

* nels.vertx.commons
* nels.commons
* nels.client

## Third party dependencies

see the pom.xml

How to deploy
===
On the host where you would like to deploy the master api service
* Create three folders `bin`,`conf` and `logs`
* Put the `nels.master.api.jar` in `bin` folder
* Put the `config.properties` and `log4j2.xml` in `conf` folder (skeletons provided in this repo). Provide proper configurations
* To start the service 
``` 
cd into the "bin" folder 
java -jar -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory -Dlog4j.configurationFile=../conf/log4j2.xml -Dconfig.path=../conf/config.properties nels.master.api.jar
```
