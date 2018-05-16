Introduction
===
This repository consists of core components that are used for building the [NeLS](https://nels.bioinfo.no) infrastructure.
The NeLS system builds upon multiple external services/applications by implementing a set of micro-services and portals.<br/>
Each of the code packages found in this repository have basic README files that explain their purpose, features and basic how-tos.

Components Categorization
===
NeLS components can be categorized as follows:

1. Basic model definition, operations and utility packages
    * nels.commons (java)
    * nels.vertx.commons (java)
    * nels.eup.core (java)
    * nels.idp.core (java)
2. Portals
    * NeLS user portal (java)
    * NeLS administration portal (angularjs)
3. NeLS micro-services
    * master api service (java)
    * public api service (java)
    * NeLS storage service (java)
    * storebioinfo storage service (java)
    * tsd proxy service (java)
    * oauth2 service (python)
    * storage proxy service (python)
4. Command lines
    * nels.storage.commandlines (python)
    * nels.storage.statistics (python)
    * nels.test.integration (python)



External Applications
===
* Apache web server - hosts the NeLS admin portal application, proxies the central NeLS portal as well as the different micro services
* Apache tomcat - hosts the NeLS Portal central web application
* Postgresql database - hosts the NeLS users databases, identity databases and storebioinfo metadata databases
* Rabbit MQ service - for facilitating messages between components
* iRods service - used for data warehousing by the storebioinfo service 
* Memcached
* Galaxy
