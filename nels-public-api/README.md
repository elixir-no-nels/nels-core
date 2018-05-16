 
Introduction
===
This component is supporting OAuth2, and mainly handling with the requests from nels admin portal and external clients who want to interact with nels.
It is standing in front of nels storage service and sbi service.

How to build
===
mvn package

Dependencies
===

## NeLS component dependency


* nels.commons
* nels.vertx.commons
* nels.client
* nels.extra.client
* sbi.service
* nels.storage.service

## Third party dependencies

see the pom.xml

How to deploy
===
On the host where you would like to deploy the public api service
* Create three folders `bin`,`conf` and `logs`
* Put the `nels.public.api.service.jar` in `bin` folder
* Put the `config.properties` and `log4j2.xml` in `conf` folder (skeletons provided in this repo). Provide proper configurations
* To start the service 
``` 
cd into the "bin" folder 
java -jar -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory -Dlog4j.configurationFile=../conf/log4j2.xml -Dconfig.path=../conf/config.properties nels.public.api.service.jar