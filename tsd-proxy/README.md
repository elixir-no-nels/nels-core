 
Introduction
===
This component is used to interact with TSD - Services for sensitive data. It is mainly receiving requests from nels portal.

How to build
===
mvn package

Dependencies
===

## NeLS component dependency


* nels.vertx.commons

## Third party dependencies

see the pom.xml

How to deploy
===
On the host where you would like to deploy the master api service
* Create three folders `bin`,`conf` and `logs`
* Put the `tsd.proxy.service.jar` in `bin` folder
* Put the `tsd.service.properties` and `log4j2.xml` in `conf` folder (skeletons provided in this repo). Provide proper configurations
* To start the service 
``` 
cd into the "bin" folder 
java -jar -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory -Dlog4j.configurationFile=../conf/log4j2.xml tsd.proxy.service.jar
```
