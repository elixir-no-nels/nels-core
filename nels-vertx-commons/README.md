 
Introduction
===
This component is a base for most of the NeLS micro services. It provides definitions for constants, configuration keys and asynchronous job types. <br/>
 
It also provides a database helper utility in two flavours; one in the form of asynchronous database invocations (based on [Vert.x](https://vertx.io)) and another for synchronous invocations (based on ComboPooledDataSource from [c3po](https://github.com/swaldman/c3p0)).   

How to build
===

mvn clean install

Dependencies
===

## NeLS component dependency

* nels.commons

## Third party dependencies

see the pom.xml