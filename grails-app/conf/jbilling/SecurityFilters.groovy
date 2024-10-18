package jbilling

import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.common.XSSChecker
import com.sapienter.jbilling.csrf.ControllerAnnotationHelper
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

/**
* Security filter implemented for Salesforce branch issue #5687 Review and respond to Portswigger Security Scan
* This analyzes the common requests in the system and verifies that the data being supplied agrees with the
* expected format of data to parseout any scripts. This is to prevent Reflective XSS attacks
*/
class SecurityFilters {

    private static final String JAX_RS_CONTROLLER = "jaxrs"

    boolean noCache = true

    def filters = {
    	//All contoller list methods. Returning false from here prevents the request from being processed
        allList(controller:'*', action:'list') {
        
            before = {

            	if(params?.order!=null && params?.order!='null' && params?.order!='' && params?.order!='asc' && params?.order!='desc' ){
            		System.out.println("Security filter reject: Invalid order param value")
					return false;
            	} else if(params?.sort!=null && params?.sort!='null' && (params?.sort?.indexOf('\'') >= 0 || params?.sort?.indexOf('"') >= 0)){
            		System.out.println("Security filter reject: Invalid sort param value")
            		log.debug("Security filter reject: Invalid sort param value")
            		return false;
            	} else if(params?.contactFieldTypes!=null && params?.contactFieldTypes!='null' && !(params['contactFieldTypes']).isNumber()){
            		System.out.println("Security filter reject: Invalid contactFieldTypes param value")
            		return false;
            	}
            }
            after = { Map model ->

            }
            afterView = { Exception e ->

            }
        }

        all(controller:'*', action:'*') {

            before = {
                if (controllerName.equalsIgnoreCase(JAX_RS_CONTROLLER)){
                    return true
                }
            	def value = request?.getHeader('Referer');
                if(new XSSChecker().hasScript(value)){
                	System.out.println("Security filter reject: Invalid Referer header value")
                	return false
                } else if(params?.template!=null && params?.template!='null' && !params?.template?.matches("[a-zA-Z/]*")){
                	System.out.println("Security filter reject: Invalid template name")
                	return false
                }
                def validRefererPrefix = "^${grailsApplication.config.grails.serverURL}".replace("http", "https?")
                if (request.post) {
                    return value && value =~ validRefererPrefix
                }
            }
            after = { Map model ->
                if(noCache) {
                    response.addHeader('Cache-control', 'no-store')
                    response.addHeader('Pragma', 'no-store')
                }
            }
            afterView = { Exception e ->

            }
        }

        validFormTokenRequired (controller: '*', action:'*') {
            before = {
                boolean valid = true
                if (controllerName.equalsIgnoreCase(JAX_RS_CONTROLLER)){
                    return true
                }
                if (ControllerAnnotationHelper.requiresValidFormToken(controllerName, actionName)) {
                    //the action requires valid form token and the call must be POST
                    if (!request.post) {
                        log.info("Action requires POST call. URL: ${request.requestURL}, query: ${request.queryString}")
                        flash.invalidToken = "flash.requires.post.call"
                        valid = false
                    } else {
                        //it is POST
                        if (!hasValidFormToken(request)) {
                            log.info("Form Token is invalid. URL: ${request.requestURL}, query: ${request.queryString}")
                            flash.invalidToken = "flash.invalid.token.error"
                            valid = false
                        } else {
                            //all good reset the token
                            resetToken(request)
                        }
                    }
                }
                if (!valid) {
                    redirect(controller: controllerName)
                    return false
                }
                return true
            }
        }
    }

    @PostConstruct
    public void init() {
        if(["true","1"].contains(Util.getSysProp('http.cache'))) {
            noCache = false
        }
    }


    /* Taken from WithFormMethod.groovy */
    private boolean hasValidFormToken (HttpServletRequest request) {
        SynchronizerTokensHolder tokensHolderInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
        if (!tokensHolderInSession) return false

        String[] tokenInRequests = request.parameterMap[SynchronizerTokensHolder.TOKEN_KEY]
        if (!tokenInRequests) return false

        String[] urlInRequests = request.parameterMap[SynchronizerTokensHolder.TOKEN_URI]
        if (!urlInRequests) return false

        try {

            String tokenInRequest = tokenInRequests[0]
            String urlInRequest = urlInRequests[0]

            return tokensHolderInSession.isValid(urlInRequest, tokenInRequest)
        }
         catch(IllegalArgumentException e) {
            log.debug("Caught Exception while trying to validate Token. " + e.getMessage())
            return false
        }
    }

    /* Taken from WithFormMethod.groovy */
    private void resetToken(HttpServletRequest request) {
        SynchronizerTokensHolder tokensHolderInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)

        String[] urlInRequest = request.parameterMap[SynchronizerTokensHolder.TOKEN_URI]
        String[] tokenInRequest = request.parameterMap[SynchronizerTokensHolder.TOKEN_KEY]

        if (urlInRequest && tokenInRequest) {
            tokensHolderInSession.resetToken(urlInRequest[0], tokenInRequest[0])
        }
        if (tokensHolderInSession.isEmpty()) request.getSession(false)?.removeAttribute(SynchronizerTokensHolder.HOLDER)
    }
    
}
