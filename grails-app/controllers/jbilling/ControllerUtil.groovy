package jbilling

import com.sapienter.jbilling.server.user.EntityBL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class ControllerUtil {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    def static fetchCompanyIdFromDomain(String serverName) {
        def domainName;
        def entityId
        def split = serverName.split("\\.");
        if (split.length > 0) {
            domainName = split[0];
        }
        if (domainName) {
            // Check if company id for that domain name exists
            entityId = EntityBL.getEntityIdByDomainName(domainName);
            if (!entityId) {
                log.error("No Company found for the domain : {}", domainName)
            }
        }
        return entityId
    }

    def static fetchCompanyIdFromUserName(String userName) {
        def entityId
        if (userName) {
            entityId = EntityBL.getEntityIdByUserName(userName);
            if (!entityId) {
                log.debug("No Company found for the userName : {}", userName)
            }
        }
        return entityId
    }

}
