ARG JBILLING_BASE_IMAGE=override-by-runtime-build-arg-parameter

FROM $JBILLING_BASE_IMAGE

MAINTAINER nilesh.vibhute@sarathisoftech.com

WORKDIR /opt/tomcat
RUN mkdir -p /home/billinghub/jbilling-home

RUN rm -rf ./webapps/ROOT
ADD target/jbilling.war ./webapps/

RUN unzip -qq ./webapps/jbilling.war -d ./webapps/ROOT
RUN rm -rf ./webapps/jbilling.war
ADD resources /home/billinghub/jbilling-home/resources

WORKDIR /home/billinghub

#ENV JBILLING_HOME="/home/billinghub/jbilling-home"

EXPOSE 8080

CMD /opt/tomcat/bin/catalina.sh run
