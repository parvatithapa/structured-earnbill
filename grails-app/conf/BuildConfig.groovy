/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde
 This file is part of jbilling.
 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.
 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

grails.work.dir = "${userHome}/.grails/${grailsVersion}"
grails.project.work.dir = "${grails.work.dir}/projects/${appName}-${appVersion}-jaxrs"

grails.servlet.version             = "3.0"
grails.project.class.dir           = "target/classes"
grails.project.test.class.dir      = "target/test-classes"
grails.project.test.reports.dir    = "target/test-results"
grails.project.target.level        = 1.8
grails.project.source.level        = 1.8
grails.project.war.file            = "target/${appName}.war"
grails.project.dependency.resolver = "maven" // or ivy

grails.war.resources = { stagingDir, args ->
	println "copy for war ..."
	copy(verbose: true, todir: "${stagingDir}/WEB-INF/classes/spring") {
		fileset(dir: "grails-app/conf/spring") {
			include(name: "*Beans.groovy")
		}
	}
}

grails.project.dependency.resolution = {
	// inherit Grails' default dependencies
	inherits("global") {
		excludes "com.google.guava"
		excludes "log4j", "grails-plugin-log4j"
	}

	// log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
	log "debug"

	// repositories for dependency resolution
	repositories {
		inherits false       // Don't inherit repositories from plugins as they have http URLs, while we need https

		mavenLocal()         // local maven cache. At first position in list to be used as preferred location for jars
		grailsPlugins()      // local Grails plugins installed in directory ~/.grails/2.4.3/projects/PROJECT_NAME/plugins/
		grailsHome()         // local GRAILS_HOME/lib/

		mavenRepo "https://repo1.maven.org/maven2/"          // mavenCentral
		mavenRepo "https://repo.grails.org/grails/plugins/"  // grailsCentral

		// our repository. it is configured to be a caching proxy for several other repositories
		mavenRepo "http://maven.jbilling.com/nexus/content/groups/jbilling/"
	}

	dependencies {

		// 'build' phase dependencies

		build 'org.eclipse.jdt:core:3.3.0-v_771' // for drools-compile

		build ('com.lowagie:itext:2.1.7') {
			transitive = false
		}
		build 'org.eclipse.jdt.core.compiler:ecj:4.4.2'  // for tomcat8 plugin and jasperreports

		def slf4jVersion = '1.7.25'
		build ("org.slf4j:log4j-over-slf4j:${slf4jVersion}",
				"org.slf4j:jcl-over-slf4j:${slf4jVersion}",
				"org.slf4j:jul-to-slf4j:${slf4jVersion}",
				"org.slf4j:slf4j-api:${slf4jVersion}" )


		// 'compile' phase dependencies

		compile("org.slf4j:log4j-over-slf4j:${slf4jVersion}",
				"org.slf4j:jcl-over-slf4j:${slf4jVersion}",
				"org.slf4j:jul-to-slf4j:${slf4jVersion}",
				"org.slf4j:slf4j-api:${slf4jVersion}" )

		compile ("ch.qos.logback:logback-core:1.2.3",
				"ch.qos.logback:logback-classic:1.2.3")
		test    ("org.codehaus.janino:janino:3.0.8")

		compile('org.springmodules:spring-modules-cache:0.8') {
			transitive = false
		}
		compile('org.osgi:org.osgi.core:4.1.0')
		compile('org.apache.xbean:xbean-spring:3.5') {
			excludes 'commons-logging'
		}

		compile('org.apache.xmlrpc:xmlrpc-client:3.1') {
			excludes 'junit', 'xml-apis'
		}

		compile('org.apache.geronimo.javamail:geronimo-javamail_1.4_mail:1.8.4')
		compile('org.apache.geronimo.javamail:geronimo-javamail_1.4_provider:1.8.4')
		compile('org.apache.geronimo.specs:geronimo-javamail_1.4_spec:1.7.1')

		def droolsVersion = "5.0.1"
		compile ("org.drools:drools-core:${droolsVersion}",
				"org.drools:drools-decisiontables:${droolsVersion}",
				"org.drools:drools-templates:${droolsVersion}") {
					excludes 'joda-time'
				}
		compile ("org.drools:drools-ant:${droolsVersion}") {
			excludes 'joda-time', "ant", "ant-nodeps"
		}
		compile ("org.drools:drools-compiler:${droolsVersion}") {
			excludes 'joda-time', 'core'
		}

		compile('org.quartz-scheduler:quartz:2.2.2') {
			excludes "c3p0"
		}
		compile 'joda-time:joda-time:2.9.4'

		compile('net.sf.opencsv:opencsv:2.3') {
			excludes 'junit'
		}

		compile 'com.github.tomakehurst:wiremock-standalone:2.17.0'
		compile('org.mockito:mockito-all:1.10.19')

		compile('commons-httpclient:commons-httpclient:3.0.1') {
			excludes 'junit', 'commons-logging'
		}
		compile 'commons-net:commons-net:3.5'
		compile 'commons-codec:commons-codec:1.10'

		compile('commons-beanutils:commons-beanutils:1.9.2',
				'commons-configuration:commons-configuration:1.10'){
					excludes 'commons-logging'
					transitive = false
				}

		compile 'org.hibernate:hibernate-validator:5.1.2.Final'

		def jacksonVersion = "2.9.3"
		compile "com.fasterxml.jackson.core:jackson-core:${jacksonVersion}"
		compile "com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}"
		compile "com.fasterxml.jackson.core:jackson-annotations:${jacksonVersion}"
		compile "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}"

		compile 'org.apache.velocity:velocity:1.7'
		compile('org.apache.velocity:velocity-tools:2.0') {
			excludes 'struts-core', 'struts-taglib', 'struts-tiles', 'commons-logging'
		}

		compile('net.sf.jasperreports:jasperreports:6.4.3') {
			excludes 'jaxen', 'xalan', 'xml-apis', 'jdtcore', 'itext', 'commons-logging', 'stax-api'
		}

		compile "org.bouncycastle:bcprov-jdk16:1.46"

		compile 'net.sf.jasperreports:jasperreports-fonts:6.0.0'
		compile('org.apache.poi:poi:3.15') {
			excludes 'commons-logging'
		}

		compile('net.sf.barcode4j:barcode4j:2.1') {
			excludes "ant", 'commons-logging'
		}

		compile 'org.liquibase:liquibase-core:3.4.2'
		compile 'com.mchange:c3p0:0.9.5.2'

		compile('org.codehaus.groovy.modules.http-builder:http-builder:0.5.2') {
			excludes "groovy", 'commons-logging', 'xercesImpl'
		}

		compile "com.thoughtworks.xstream:xstream:1.4.7"

		// lombok dependency for boiler plate, getter,setter, toString and hashCode method
		compile 'org.projectlombok:lombok:1.16.20'

		// [igor.poteryaev@jbilling.com 2016-05-06]
		// Updated (spring-core, spring-jms, spring-messaging) to fix: https://jira.spring.io/browse/SPR-11841
		// (indefinite wait on jbilling instance shutdown due to race condition in spring-jms module)
		// [igor.poteryaev@jbilling.com 2016-06-17]
		// Updated all to 4.3.0.RELEASE version to fix problem with not started spring batch integration jms listeners
		// [igor.poteryaev@appdirect.com 2018-02-27]
		// Updated all to 4.3.14.RELEASE version to get Spring fixes for period (Jun, 2016) - (Jan, 2018)
		def springVersion = "4.3.14.RELEASE"
		compile (
				"org.springframework:spring-aop:${springVersion}",
				"org.springframework:spring-beans:${springVersion}",
				"org.springframework:spring-context:${springVersion}",
				"org.springframework:spring-context-support:${springVersion}",
				"org.springframework:spring-expression:${springVersion}",
				"org.springframework:spring-jdbc:${springVersion}",
				"org.springframework:spring-jms:${springVersion}",
				"org.springframework:spring-messaging:${springVersion}",
				"org.springframework:spring-orm:${springVersion}",
				"org.springframework:spring-tx:${springVersion}",
				"org.springframework:spring-web:${springVersion}",
				"org.springframework:spring-webmvc:${springVersion}"
				)
		compile("org.springframework:spring-core:${springVersion}") {
			excludes 'commons-logging'
		}

		// [igor.poteryaev@appdirect.com 2018-02-27]
		// Updated all to 4.3.14.RELEASE version to get Spring fixes for period (Jun, 2016) - (Jan, 2018)
		def springIntegrationVersion = "4.3.14.RELEASE"
		compile "org.springframework.integration:spring-integration-core:${springIntegrationVersion}"
		compile "org.springframework.integration:spring-integration-ftp:${springIntegrationVersion}"
		compile "org.springframework.integration:spring-integration-sftp:${springIntegrationVersion}"
		compile "org.springframework.integration:spring-integration-file:${springIntegrationVersion}"
		compile "org.springframework.integration:spring-integration-jdbc:${springIntegrationVersion}"
		compile "org.springframework.integration:spring-integration-jms:${springIntegrationVersion}"

		def springBatchVersion = "3.0.8.RELEASE"
		compile "org.springframework.batch:spring-batch-core:${springBatchVersion}"
		compile "org.springframework.batch:spring-batch-integration:${springBatchVersion}"

		compile 'org.springframework.amqp:spring-amqp:2.0.2.RELEASE'
		compile 'org.springframework.amqp:spring-rabbit:2.0.2.RELEASE'

		compile 'org.grails:grails-datastore-core:3.1.2.RELEASE'

		compile('org.springframework.data:spring-data-jpa:1.10.2.RELEASE') {
			excludes "aspectjrt" // included by grails.plugins.rest
		}

		compile('org.apache.httpcomponents:httpclient:4.5.5') {
			excludes 'commons-logging'
		}

		compile 'eu.infomas:annotation-detector:3.0.5'

		compile('net.sf.ehcache:ehcache-jmsreplication:0.5') {
			excludes 'ehcache-core'
		}

		compile "com.googlecode.json-simple:json-simple:1.1.1"

		compile "com.google.guava:guava:24.0-jre"

		/* SAML Dependencies start */

		compile('org.springframework.security.extensions:spring-security-saml2-core:1.0.2.RELEASE') {
			excludes 'spring-security-core'
			excludes 'spring-security-web'
			excludes 'bcprov-jdk15'
			excludes 'xml-apis'
			excludes 'bcprov-jdk15'
		}

		/* SAML Dependencies End */

		/* OAuth Dependencies Start */

		compile ('oauth.signpost:signpost-core:1.2.1.2') {
			transitive = false
		}
		compile ('org.apache.cxf:cxf-rt-rs-client:3.0.4') {
			transitive = false
		}

		compile ('org.springframework.boot:spring-boot-starter-hateoas:1.5.2.RELEASE') {
			excludes 'log4j-over-slf4j', 'logback-classic', 'spring-boot-starter-tomcat'
		}

		compile 'com.googlecode.libphonenumber:libphonenumber:8.9.12'

		compile 'com.googlecode.libphonenumber:geocoder:2.101'

		compile 'com.microsoft.azure:azure-storage:5.4.0'
		/* Azure Webhooks Dependencies End */

		// io.vavr dependency
		compile 'io.vavr:vavr:0.9.1'

		compile 'com.googlecode.libphonenumber:carrier:1.93'

		// apache commons-compress dependency
		compile 'org.apache.commons:commons-compress:1.18'
		// apache tika-core dependency
		compile 'org.apache.tika:tika-core:1.20'

		// 'runtime' phase dependencies

		runtime 'org.hibernate:hibernate-entitymanager:4.3.11.Final'

		runtime 'javax.activation:activation:1.1.1'

		compile('net.sf.ehcache:ehcache-jmsreplication:0.5') {
			exclude 'ehcache-core'
		}

		// jwt dependencies
		def jsonwebtokenVersion = "0.11.2"

		compile ("io.jsonwebtoken:jjwt-api:${jsonwebtokenVersion}")

		runtime ("io.jsonwebtoken:jjwt-impl:${jsonwebtokenVersion}")

		runtime ("io.jsonwebtoken:jjwt-jackson:${jsonwebtokenVersion}")

		def activemqVersion = "5.14.4"

		runtime "org.apache.activemq:activemq-kahadb-store:${activemqVersion}"

		runtime "org.apache.activemq:activemq-broker:${activemqVersion}"

		runtime "org.apache.activemq:activemq-client:${activemqVersion}"

		runtime("org.apache.activemq:activemq-pool:${activemqVersion}") {
			excludes 'junit', 'commons-logging', 'log4j'
		}

		runtime 'org.eclipse.jdt.core.compiler:ecj:4.4.2'  // for tomcat8 plugin and jasperreports

		//needed by the jDiameter library
		runtime 'org.picocontainer:picocontainer:2.13.5'
		runtime 'commons-pool:commons-pool:1.6'

		runtime 'xerces:xercesImpl:2.11.0'  // for paypal payment

		runtime 'org.postgresql:postgresql:42.3.1'
		runtime 'mysql:mysql-connector-java:5.1.26'
		runtime 'org.hsqldb:hsqldb:2.3.2'

		runtime ('com.lowagie:itext:2.1.7') {
			transitive = false
		}

		runtime 'com.googlecode.jcsv:jcsv:1.4.0'
		runtime 'org.apache.tika:tika-core:1.17'

		runtime (
				"org.springframework:spring-aspects:${springVersion}"
				)

		// ehcache versions starting from 2.8.3 will require Hibernate also to be upgraded from 4.x to 5.x
		runtime 'net.sf.ehcache:ehcache:2.8.2'
		runtime 'org.apache.cxf:cxf-rt-ws-security:3.0.4'

		// 'provided' dependencies

		provided 'javax.jms:jms-api:1.1-rev-1'


		// Test dependencies

		// override junit bundled with grails
		build('junit:junit:4.12') {
			transitive = false
		}
		test ('junit:junit:4.12') {
			transitive = false // excludes "hamcrest-core"
		}
		test    'org.hamcrest:hamcrest-all:1.3'

		test    ('org.testng:testng:6.9.10') {
			excludes "junit", "snakeyaml", "bsh"
		}
		test    'org.easymock:easymockclassextension:3.2'

		// minimal viable selenium dependencies
		// without transitive = false more than 25 dependent libraries will be used
		def seleniumVersion = "2.53.1"
		test    ("org.seleniumhq.selenium:selenium-java:${seleniumVersion}",
				"org.seleniumhq.selenium:selenium-api:${seleniumVersion}",
				"org.seleniumhq.selenium:selenium-support:${seleniumVersion}",
				"org.seleniumhq.selenium:selenium-remote-driver:${seleniumVersion}",
				"org.seleniumhq.selenium:selenium-firefox-driver:${seleniumVersion}",
				"org.seleniumhq.selenium:selenium-chrome-driver:${seleniumVersion}",
				"org.seleniumhq.selenium:selenium-ie-driver:${seleniumVersion}") {
					transitive = false
				}
		test 'org.apache.commons:commons-exec:1.3'

		test "org.glassfish.web:el-impl:2.2"

		test "org.jacoco:org.jacoco.ant:0.8.12"

		test (
				"org.springframework:spring-test:${springVersion}"
				)

		test 'net.javacrumbs.json-unit:json-unit:1.15.0'


		// ui-testing-automation module dependencies
		test    ("com.googlecode.json-simple:json-simple:1.1.1") {
			transitive = false
		}
		test    ("ru.stqa.selenium:webdriver-factory:3.0",
				"ru.yandex.qatools.htmlelements:htmlelements-java:1.17",
				"ru.yandex.qatools.htmlelements:htmlelements-matchers:1.17",
				"com.google.code.findbugs:jsr305:3.0.1",
				"net.java.dev.jna:jna-platform:4.2.2"
				)
		test    ('com.lowagie:itext:2.1.7') {
			transitive = false
		}

		test 'com.github.tomakehurst:wiremock:2.17.0'
		test('org.mockito:mockito-all:1.10.19')
		test 'javax.servlet:javax.servlet-api:3.1.0'

		// jbilling modules

		compile ('com.sapienter.jbilling:audit-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:customer-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:database-configurations:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:item-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:jbilling-common-impl:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:filter-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:jbilling-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:mediation-process-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:mediation-process-service-impl:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:mediation-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:mediation-service-impl:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:order-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:event-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:usage-pool-service:1.0.0') {
			transitive = false
		}
		compile ("com.wordnik:swagger-jaxrs_2.11:1.3.12"){
			// included by jaxrs:0.11 plugin
			excludes "javax.ws.rs:jsr311-api:1.1.1"
		}
		compile ('com.sapienter.jbilling:mediation-full-creative-impl:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:mediation-deutsche-telekom-impl:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:saml-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:metered-usage-service:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:metered-usage-service-impl:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:appdirect-client-library:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:dt-customer:1.0.0') {
			transitive = false
		}
		compile ('com.sapienter.jbilling:dt-reserve-instance:1.0.0') {
			transitive = false
		}
		test    ('com.sapienter.jbilling:ui-testing-automation:1.0.0') {
			transitive = false
		}
		// ui-testing-automation module dependencies
		test    ("com.googlecode.json-simple:json-simple:1.1.1",
				"org.uncommons:reportng:1.1.4"
				) {
					transitive = false
				}
		compile ('com.sapienter.jbilling:mediation-movius-impl:1.0.0') {
			transitive = false
		}
		compile ("org.grails:grails-web:2.4.3") {
			excludes "org.aspectj:aspectjrt"
		}
		runtime ("org.grails:grails-core:2.4.3") {
			excludes "org.aspectj:aspectjrt"
		}

		compile ('com.sapienter.jbilling:quantity-rating:1.0.0') {
			transitive = false
		}
		compile ('com.google.code.gson:gson:2.10.1')
		compile ('io.github.resilience4j:resilience4j-retry:0.11.0')
		compile('org.springframework.security.oauth:spring-security-oauth:2.1.0.RELEASE')
		compile 'com.cashfree.pg.java:cashfree_pg:4.1.2'
		compile 'com.cashfree.verification.java:cashfree_verification:1.0.3'
		compile 'org.apache.commons:commons-lang3:3.7'

		compile ('com.sapienter.jbilling:mediation-sapphire-impl:1.0.0') {
			transitive = false
		}

		compile ('com.sapienter.jbilling:mediation-spc-impl:1.0.0') {
			transitive = false
		}

		def stripeIntegrationVersion = "20.56.0"
		compile "com.stripe:stripe-java:${stripeIntegrationVersion}"
		compile 'pl.allegro.finance:tradukisto:2.0.0'
		compile 'com.google.zxing:core:3.5.2'
		compile 'com.google.zxing:javase:3.5.2'
	}

	plugins {
		build ":tomcat:8.0.30"

		compile ":jquery-ui:1.10.4"
		compile ':webflow:2.1.0'
		compile ":cookie:0.51"
		compile (":cxf:2.1.1") {
			excludes "cxf-rt-frontend-jaxrs"
		}
		compile ":remote-pagination:0.4.8"
		compile ":remoting:1.3"
		compile ":spring-security-core:2.0-RC4"
		compile (':jaxrs:0.11') {
			excludes "jsr311-api"
		}
		compile (":swagger4jaxrs:0.2"){
			excludes "com.wordnik:swagger-jaxrs_2.10:1.3.2"
			excludes ":jaxrs:0.8"
			excludes "javax.ws.rs:jsr311-api:1.1.1"
			excludes "com.fasterxml.jackson.core:jackson-core:2.1.0"
			excludes "com.fasterxml.jackson.core:jackson-databind:2.1.0"
		}

		runtime(":hibernate4:4.3.5.5") {
			excludes "hibernate-validator", "ehcache-core"
		}
		runtime ":jquery:1.11.1"
		runtime ":resources:1.2.8"
		runtime ":webxml:1.4.1"
		compile ':recaptcha:1.5.0'
		compile(":rest-client-builder:2.1.1") {
			export = false
		}

		runtime ":cors:1.3.0"
	}
}

grails.war.resources = { stagingDir, args ->
	delete(file: "${stagingDir}/WEB-INF/lib/liquibase*.jar")
	delete(file: "${stagingDir}/WEB-INF/classes/hibernate-debug.cfg.xml")
	delete(file: "${stagingDir}/WEB-INF/classes/logback-test.xml")
}
