Introduction
===
The NeLS Portal plays a central role in the overall NeLS architecture. It has the following features / purposes:

1. It provides login through multiple identity providers (i.e. it exposes a service provider SAML profile towards its registered identity providers using [Spring Security](https://projects.spring.io/spring-security/))
2. It provides a web based remote-file-system experience to end users leveraging the NeLS storage service
3. It provides administrative functions for the Norwegian Bioinformatics Helpdesk 
4. It provides a tailored navigation and operation experience when interacting with NeLS import/export enabled Galaxy instances
5. It provides an easy to use, web based, experience to consume the Storebioinfo and TSD services
6. It provides a proxy authentication layer for the NeLS OAuth2 service

How to build
===
mvn clean package -P [TEST, PROD]

Dependencies
===

## NeLS component dependency
* nels.commons
* nels.eup.core
* nels.idp.core
* nels.extra.client

## Third party dependencies
see the pom.xml

How to deploy
===
The NeLS portal is a java war application that should be deployed in an application container. Apache Tomcat is advised for deploying the NeLS portal.<br/>
Note that for the NeLS portal to be functional, it needs proper configurations towards other components in the NeLS architecture