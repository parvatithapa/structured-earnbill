package jbilling

import com.sapienter.jbilling.server.apiUserDetail.ApiUserDetailWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.util.UrlPathHelper

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.lang.invoke.MethodHandles

class CustomAuthenticationFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    IWebServicesSessionBean webServicesSession

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest= (HttpServletRequest) servletRequest;
        String resourcePath = new UrlPathHelper().getPathWithinApplication(httpRequest)

        if(resourcePath.contains("/api/users/dt")){
            String token = httpRequest.getHeader("appdirect-webhook-token")
            ApiUserDetailWS apiUserDetails = webServicesSession.getUserDetails(token)
            if(apiUserDetails == null){
                logger.error("CustomAuthenticationFilter.doFilter invalid token received")
                ((HttpServletResponse) servletResponse).setStatus(HttpStatus.FORBIDDEN.value())
                return
            }
            logger.info("Token validated successfully")
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }
}
