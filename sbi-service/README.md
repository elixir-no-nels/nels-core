 
Introduction
===
This component is a RESTful service that manages data in StoreBioInfo storage in NeLS e-infrastructure. 

How to build
===
mvn clean package

Dependencies
===

## NeLS component dependency


* nels.vertx.commons

## Third party dependencies

see the pom.xml

How to deploy
===
1. Create three folders `bin`, `conf` and `logs`
2. Put `sbi.service.jar` into `bin` folder
3. Put `sbi.service.properties` into `conf` folder
4. Run command `java -jar -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4jLogDelegateFactory sbi.service.jar` to start the service