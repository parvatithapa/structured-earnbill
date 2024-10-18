package jbilling

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.server.user.db.CompanyDAS

class AfterLoginFilters {

    def filters = {
        afterLogin(controller: 'home', action: 'index') {
            before = {
                Integer companyId = session.getAttribute("company_id");
                if (companyId) {
                    session.setAttribute("company_timezone", new CompanyDAS().find(companyId).getTimezone())
                }
                
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication()
                if (authentication.getPrincipal() instanceof CompanyUserDetails ) {
                    CompanyUserDetails details = (CompanyUserDetails) authentication.getPrincipal()
                    LocaleContextHolder.setLocale(details.getLocale())
                }
            }
        }
    }
}
