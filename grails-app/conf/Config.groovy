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

import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import com.sapienter.jbilling.common.Util


/*
 Load configuration files from the set "JBILLING_HOME" path (provided as either
 an environment variable or a command line system property). External configuration
 files will override default settings.
 */

def appHome = System.getProperty("JBILLING_HOME") ?: System.getenv("JBILLING_HOME")

if (appHome) {
	println "Loading configuration files from JBILLING_HOME = ${appHome}"
	grails.config.locations = [
		"file:${appHome}/${appName}-Config.groovy",
		"file:${appHome}/${appName}-DataSource.groovy"
	]

} else {
	appHome = new File("../${appName}")
	if (appHome.listFiles({ dir, file -> file ==~ /${appName}-.*\.groovy/ } as FilenameFilter)) {
		println "Loading configuration files from ${appHome.canonicalPath}"
		grails.config.locations = [
			"file:${appHome.canonicalPath}/${appName}-Config.groovy",
			"file:${appHome.canonicalPath}/${appName}-DataSource.groovy"
		]

		println "Setting JBILLING_HOME to ${appHome.canonicalPath}"
		System.setProperty("JBILLING_HOME", appHome.canonicalPath)

	} else {
		println "Loading configuration files from classpath"
	}
}
grails.databinding.useSpringBinder = true
grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
	xml: ['text/xml', 'application/xml'],
	text: 'text/plain',
	js: 'text/javascript',
	rss: 'application/rss+xml',
	atom: 'application/atom+xml',
	css: 'text/css',
	csv: 'text/csv',
	all: '*/*',
	json: ['application/json','text/json'],
	form: 'application/x-www-form-urlencoded',
	multipartForm: 'multipart/form-data'
]

grails.app.context = "/"

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']


grails {
	views {
		gsp {
			encoding = 'UTF-8'
			htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
			codecs {
				expression = 'html' // escapes values inside ${}
				scriptlet = 'html' // escapes output from scriptlets in GSPs
				taglib = 'none' // escapes output from taglibs
				staticparts = 'raw' // escapes output from static template parts
			}
		}
		// escapes all not-encoded output at final stage of outputting
		filteringCodecForContentType.'text/html' = 'html'
	}
}

grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// use the jQuery javascript library
grails.views.javascript.library = "jquery"
// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to install the java.util.logging bridge for sl4j. Disable for AppEngine!
grails.logging.jul.usebridge = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = ['com.sapienter.jbilling.server.config', 'com.sapienter.jbilling.saml', 'com.sapienter.jbilling.auth']
// whether to disable processing of multi part requests
grails.web.disable.multipart = false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password', 'creditCard', 'creditCardDTO']

// enable query caching by default
grails.hibernate.cache.queries = false

// set per-environment serverURL stem for creating absolute links
environments {
	production {
		grails.serverURL = System.getenv("JBILLING_SERVER_URL") ?: "http://www.changeme.com"
	}
	// see issue http://jira.grails.org/browse/GRAILS-7598
	development {
		grails.serverURL = System.getenv("JBILLING_SERVER_URL") ?: "http://localhost:8080"
	}
}

/*
 Logging
 2018-01. configuration was externalized.
 production:  logback.xml
 development: logback-test.xml
 ant test:    test/ant-test-logback.xml
 */


/*
 Static web resources
 */
grails.resources.modules = {
	'core' {
		defaultBundle 'core-ui'

		resource url: '/css/all.css', attrs: [media: 'screen']
		resource url: '/css/lt7.css', attrs: [media: 'screen'],
		wrapper: { s -> "<!--[if lt IE 8]>$s<![endif]-->" }
	}

	'ui' {
		dependsOn 'jquery'
		defaultBundle 'core-ui'

		resource url: '/js/main.js', disposition: 'head'
		resource url: '/js/datatable.js', disposition: 'head'
		resource url: '/js/slideBlock.js', disposition: 'head'
	}

	'input' {
		dependsOn 'jquery'
		defaultBundle "input"

		resource url: '/js/form.js', disposition: 'head'
		resource url: '/js/checkbox.js', disposition: 'head'
		resource url: '/js/clearinput.js', disposition: 'head'
	}

	'disjointlistbox' {
		defaultBundle "disjointlistbox"

		resource url: '/js/disjointlistbox.js', disposition: 'head'
	}

	'panels' {
		dependsOn 'jquery'
		defaultBundle 'panels'

		resource url: '/js/panels.js', disposition: 'head'
	}

	'jquery-validate' {
		dependsOn 'jquery'
		defaultBundle "jquery-validate"

		resource url: '/js/jquery-validate/jquery.validate.min.js', disposition: 'head'
		resource url: '/js/jquery-validate/additional-methods.min.js', disposition: 'head'
		resource url: '/js/jquery-migrate-1.2.1.js'
	}

	jquery {
		defaultBundle "jquery"

		resource url: '/js/jquery-1.11.3.min.js', disposition: 'head'
	}

	'errors' {
		defaultBundle "errors"

		resource url: '/js/errors.js', disposition: 'head'
	}

	'showtab' {
		defaultBundle: "showtab"
		resource url: '/js/showtab.js', disposition: 'head'
	}
	overrides {
		'jquery-theme' {
			resource id: 'theme', url: '/jquery-ui/themes/jbilling/jquery-ui-1.10.4.custom.css'
		}
	}

	itg {
		resource(url: 'css/ict.css')
		resource(url: 'css/alpaca.min.css')
		resource(url: 'css/alpaca-jqueryui.min.css')
		resource(url: 'css/evol.colorpicker.css')
		resource(url: 'css/jstree/invoice-template-tree.css')

		resource(url: 'js/json/invoiceTemplate-schema.js', disposition: 'head')
	}

	wizard {
		resource url: '/js/enrollment-wizard.js?2', disposition: "defer"
		resource url: '/js/jquery-validate/jquery.validate.min.js', disposition: "defer"
	}

	rest {
		resource url: '/vendor/swagger-ui/2.0.14/css/highlight.default.css', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/css/screen.css', disposition: 'head'

		resource url: '/vendor/swagger-ui/2.0.14/images/explorer_icons.png'
		resource url: '/vendor/swagger-ui/2.0.14/images/logo_small.png'
		resource url: '/vendor/swagger-ui/2.0.14/images/pet_store_api.png'
		resource url: '/vendor/swagger-ui/2.0.14/images/throbber.gif'
		resource url: '/vendor/swagger-ui/2.0.14/images/wordnik_api.png'

		resource url: '/vendor/swagger-ui/2.0.14/lib/shred.bundle.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/jquery-1.8.0.min.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/jquery.slideto.min.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/jquery.wiggle.min.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/jquery.ba-bbq.min.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/handlebars-1.0.0.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/underscore-min.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/backbone-min.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/swagger.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/swagger-ui.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/highlight.7.3.pack.js', disposition: 'head'
		resource url: '/vendor/swagger-ui/2.0.14/lib/swagger-oauth.js', disposition: 'head'


		resource url: '/vendor/swagger-ui/2.0.14/lib/shred/content.js'
		resource url: '/vendor/swagger-ui/2.0.14/swagger-ui.min.js'
	}

}


/*
 Documentation
 */
grails.doc.authors = "Emiliano Conde, Brian Cowdery, Emir Calabuch, Lucas Pickstone, Vikas Bodani, Crystal Bourque"
grails.doc.license = "AGPL v3"
grails.doc.images = new File("src/docs/images")
grails.doc.api.org.springframework = "http://static.springsource.org/spring/docs/3.0.x/javadoc-api/"
grails.doc.api.org.hibernate = "http://docs.jboss.org/hibernate/stable/core/javadocs/"
grails.doc.api.java = "http://docs.oracle.com/javase/6/docs/api/"

//gdoc aliases
grails.doc.alias.userGuide = "1. jBilling User Guide"
grails.doc.alias.integrationGuide = "2. jBilling Integration Guide"


/*
 Spring Security
 */
// require authentication on all URL's
grails.plugin.springsecurity.rejectIfNoRule = false
grails.plugin.springsecurity.fii.rejectPublicInvocations = false

// failure url
grails.plugin.springsecurity.failureHandler.useForward = false
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = '/login/authfail?login_error=1'
grails.plugin.springsecurity.failureHandler.ajaxAuthFailUrl = '/login/authfail?login_error=1'

//success handler
grails.plugin.springsecurity.successHandler.alwaysUseDefault = true

// allow user switching
grails.plugin.springsecurity.useSwitchUserFilter = true
grails.plugin.springsecurity.switchUser.targetUrl = '/user/reload'
grails.plugin.springsecurity.switchUser.switchFailureUrl = '/user/failToSwitch'

/*
 Spring Batch
 */
//Task Executor Settings
springbatch.executor.core.pool.size = 6
springbatch.executor.max.pool.size = 10
springbatch.executor.queueCapacity = 20

//Billing Process Grid Size
springbatch.billing.process.grid.size = 6

//Ageing Process Grid Size
springbatch.ageing.process.grid.size = 6

//Mediation Process
springbatch.mediation.process.grid.size=6
springbatch.mediation.process.partition.count=101
springbatch.mediation.jmr.writer.batchSize=20
springbatch.mediation.jmr.sendMetrics=false

// static security rules
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
	'/services/**'                  : ['IS_AUTHENTICATED_FULLY', 'API_120'],
	'/hessian/**'                   : ['IS_AUTHENTICATED_FULLY', 'API_120'],
	'/jaxrs/**'                     : ['IS_AUTHENTICATED_FULLY', 'API_120', 'API_2100'],
	'/httpinvoker/**'               : ['IS_AUTHENTICATED_FULLY', 'API_120'],
	'/j_spring_security_switch_user': ["hasAnyRole('CONFIGURATION_1908')", 'IS_AUTHENTICATED_FULLY']
]

// IP address restrictions to limit access to known systems (always use with web-services in production environments!)
//grails.plugin.springsecurity.ipRestrictions = [
//        '/services/**': ['127.0.0.1'],
//        '/hessian/**': ['127.0.0.1'],
//        '/httpinvoker/**': ['127.0.0.1']
//]

// configure which URL's require HTTP and which require HTTPS
/*
 portMapper.httpPort = 8080
 portMapper.httpsPort = 8443
 grails.plugin.springsecurity.secureChannel.definition = [
 '/services/**': 'REQUIRES_SECURE_CHANNEL',
 '/hessian/**': 'REQUIRES_SECURE_CHANNEL',
 '/httpinvoker/**': 'REQUIRES_SECURE_CHANNEL',
 '/version': 'REQUIRES_INSECURE_CHANNEL',
 '/css/**': 'ANY_CHANNEL',
 '/images/**': 'ANY_CHANNEL'
 ]
 */

// basic HTTP authentication filter for web-services
grails.plugin.springsecurity.useBasicAuth = true
grails.plugin.springsecurity.basic.realmName = "jBilling Web Services"

// authentication filter configuration
grails.plugin.springsecurity.filterChain.chainMap = [
	'/services/distributelApi**'   : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/hessian/distributelApi**'    : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/httpinvoker/distributelApi**': 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/api/users/dt/**'             : 'none',
	'/api/authentication/authenticate'       : 'none',
	'/api/authentication/verifyToken'        : 'none',
	'/api/paymentWebHook/**'       : 'none',
	'/api/**'                      : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/services/deutscheTelecomApi**'   : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/hessian/deutscheTelecomApi**'    : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/httpinvoker/deutscheTelecomApi**': 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/services/**'                 : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/hessian/**'                  : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	'/api-docs/**'                 : 'JOINED_FILTERS,-exceptionTranslationFilter, -securityContextPersistenceFilter',
	//      '/httpinvoker/**'              : 'statelessSecurityContextPersistenceFilter,staticAuthenticationProcessingFilter,securityContextHolderAwareRequestFilter,basicExceptionTranslationFilter,filterInvocationInterceptor',
	'/**'                          : 'JOINED_FILTERS,-basicAuthenticationFilter,-basicExceptionTranslationFilter, -statelessSecurityContextPersistenceFilter'
]

// voter configuration
grails.plugin.springsecurity.voterNames = ['authenticatedVoter', 'roleVoter', 'permissionVoter', 'webExpressionVoter']
// Valid Company Invoice Logo Image Type
validImageExtensions = ['image/png', 'image/jpeg', 'image/gif']

grails.plugin.springsecurity.useSecurityEventListener = true

//events published by the provider manager
grails.plugin.springsecurity.onInteractiveAuthenticationSuccessEvent = { e, appCtx ->
}

grails.plugin.springsecurity.onAuthenticationSuccessEvent = { e, appCtx ->
	def request = RequestContextHolder?.currentRequestAttributes()
	if (request?.params?.get('interactive_login')) {
		appCtx.getBean("appAuthResultHandler").loginSuccess(e)
	}
}


grails.plugin.springsecurity.onInteractiveAuthenticationSuccessEvent = { e, appCtx ->
	appCtx.getBean("tabConfigurationService").load()
}

grails.plugin.springsecurity.onAbstractAuthenticationFailureEvent = { e, appCtx ->
	appCtx.getBean("appAuthResultHandler").loginFailure(e)
	def request = RequestContextHolder?.currentRequestAttributes()
	def client_id = request?.params?.get('j_client_id')
	request.setAttribute("login_company", client_id, RequestAttributes.SCOPE_SESSION);
	RequestContextHolder.setRequestAttributes(request);
}

// Valid Company Invoice Logo Image Type
validImageExtensions = ['image/png', 'image/jpeg', 'image/gif']

// Disable the new ChainedTransactionManager (from Grails 2.3.7)
// for now. It is known to conflict with the BE1PC config for JMS
grails.transaction.chainedTransactionManagerPostProcessor.blacklistPattern = '.*'

'swagger4jaxrs' {
	resourcePackage = "com.sapienter.jbilling.resources/"
	basePath = "${grailsApplication.config.grails.serverURL}/api" ?: ''
	version = '0.1' // Default "1".
	title = 'AppBilling' // Default: App Name.
	description = 'Testing documentation auto-generate'
	scan = true
}

org.grails.jaxrs.resource.scope = 'singleton'
org.grails.jaxrs.doreader.disable = true
org.grails.jaxrs.dowriter.disable = true

// Added by the Api Toolkit plugin:
apitoolkit.apiName = 'api'
apitoolkit.apichain.limit = 3
apitoolkit.rest.postcrement = false
apitoolkit.attempts = 5
apitoolkit.chaining.enabled = true
apitoolkit.batching.enabled = true
apitoolkit.user.roles = ['ROLE_USER']
apitoolkit.admin.roles = ['ROLE_ROOT', 'ROLE_ADMIN']

// Added by the Recaptcha plugin:
recaptcha {
	// These keys are generated by the ReCaptcha service
	//The commented 2 below can be used for testing and will always return a valid captcha
	//publicKey = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"
	//privateKey = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe"
	//keys were generated with Lexie with a jbilling admin account at https://www.google.com/recaptcha
	publicKey  = Util.getSysProp('recaptcha.public.key')
	privateKey = Util.getSysProp('recaptcha.private.key')

	// Include the noscript tags in the generated captcha
	includeNoScript = true

	// Include the required script tag with the generated captcha
	includeScript = false

	// Set to false to disable the display of captcha
	enabled = true

	/*
	 proxy {
	 server = ""   // IP or hostname of proxy server
	 port = ""     // Proxy server port, defaults to 80
	 username = "" // Optional username if proxy requires authentication
	 password = "" // Optional password if proxy requires authentication
	 }
	 */
}

cors.url.pattern = '/api/*'
cors.enable.logging = true

grails.plugin.springsecurity.providerNames = [
	'samlAuthenticationProvider',
	'daoAuthenticationProvider',
	'anonymousAuthenticationProvider',
	'rememberMeAuthenticationProvider']

environments {
	production {
		oauthConsumerKey = "greg-jbilling-product-178741"
		oauthConsumerSecret = "nGCTFK4Y8BymOml0"
		legacyMarketplaceBaseUrl = "https://testmarketplace.appdirect.com"
	}
	development {
		oauthConsumerKey = "jbilling-sample-saml-182605"
		oauthConsumerSecret = "G15vb4t0SiamzQkR"
		legacyMarketplaceBaseUrl = "https://testmarketplace.appdirect.com"
	}
}

grails.plugin.springsecurity.useHttpSessionEventPublisher = true


org.grails.jaxrs.provider.init.parameters = [ 'com.sun.jersey.config.property.packages' :'com.wordnik.swagger.sample.resource;com.wordnik.swagger.jaxrs.listing;com.sapienter.jbilling.server.util.restexceptionhandler']

useUniqueLoginName=true