 
Introduction
===
This service exposes a RESTful API on top of the NeLS storage file system. It supports the following features:
* move files/folders
* copy files/folders

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
On the host where you would like to deploy the storage api service
* Create three folders `bin`,`conf` and `logs`
* Put the `storage.extra.service.jar` in `bin` folder
* Put the `config.properties` and `log4j2.xml` in `conf` folder (skeletons provided in this repo). Provide proper configurations
* To start the service 
``` 
cd into the "bin" folder 
java -jar -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory -Dlog4j.configurationFile=../conf/log4j2.xml -Dconfig.path=../conf/config.properties storage.extra.service.jar
