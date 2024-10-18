package com.sapienter.jbilling.rest

import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import org.grails.jaxrs.itest.IntegrationTestSpec

/**
 * @author Vojislav Stanojevikj
 * @since 31-Oct-2016.
 */
class RestBaseSpec extends IntegrationTestSpec{

    protected String BASE_URL = 'api'

    protected def webServicesSessionMock

    protected void init(restResource, String restEndPointName) {
        grailsApplication.config.org.grails.jaxrs.doreader.disable = true
        grailsApplication.config.org.grails.jaxrs.dowriter.disable = true
        webServicesSessionMock = Mock(WebServicesSessionSpringBean)
        restResource.webServicesSession = webServicesSessionMock
        BASE_URL = "${BASE_URL}/${restEndPointName}"
    }

    @Override
    boolean isAutoDetectJaxrsClasses() {
        return false
    }

}
