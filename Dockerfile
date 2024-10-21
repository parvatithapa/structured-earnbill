FROM tomcat:9.0.95-jre8-temurin-jammy

LABEL MAINTAINER="amartya.sinha@sarathisoftech.com"

RUN echo '#!/bin/sh' > /usr/local/tomcat/bin/setenv.sh && echo 'export UMASK=022' >> /usr/local/tomcat/bin/setenv.sh && chmod +x /usr/local/tomcat/bin/setenv.sh

RUN set -eux; apt-get update; apt-get install -y --no-install-recommends libfreetype-dev fonts-dejavu fonts-hosny-amiri; rm -rf /var/lib/apt/lists/*

WORKDIR /usr/local/tomcat

RUN rm -rf ./webapps/ROOT
ADD target/jbilling.war ./webapps/app.war

RUN mkdir -p /home/earnbill

WORKDIR /home/earnbill
ADD resources /home/earnbill/resources

EXPOSE 8080

CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]
