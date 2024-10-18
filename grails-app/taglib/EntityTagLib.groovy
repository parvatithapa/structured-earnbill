/**
 * EntityTagLib
 *
 * @author Khobab Chaudhary
 */
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.LogoType
import jbilling.CompanyService
import org.codehaus.groovy.grails.web.context.ServletContextHolder

class EntityTagLib {

    def CompanyService companyService

	/**
	 * Shows body only if logged in company is root
	 */
	def isRoot = { attrs, body ->
		def company = CompanyDTO.get(session['company_id'])
		def childEntities = CompanyDTO.findAllByParent(company)
		def root = CompanyDTO.get(session['company_id'])?.parent == null || (childEntities != null && childEntities.size() > 0)
		if (root) {
			out << body()
		}
	}
	
	/**
	 * Shows body only if logged in company is not root
	 */
	def isNotRoot = { attrs, body ->
		def company = CompanyDTO.get(session['company_id'])
		def childEntities = CompanyDTO.findAllByParent(company)
		def root = company?.parent == null || (childEntities != null && childEntities.size() > 0)
		if (!root) {
			out << body()
		}
	}

	/**
	 * Shows body only if logged in company is root and has at least one child entities
	 */
	def isGlobal = { attrs, body ->
	    def company = CompanyDTO.get(session['company_id'])
		def childEntities = CompanyDTO.findAllByParent(company)
		def isGlobal = (childEntities != null && childEntities.size() > 0)
		if (isGlobal) {
		   out << body()
		 }
	}

    /**
     * Create link for logo image
     */
    def logoLink = { attrs ->
        if(session['company_id']) {
            def params = [:]
            def parmName = attrs['favicon'] ? 'favicon' : 'i'

            if(attrs['favicon']) params[parmName] = companyService.entityLogoVersion()+"_"+session['company_id']

            out << createLink(controller: 'config', action: 'entityLogo', params: params)
        } else {
			def image = attrs.favicon ? LogoType.FAVICON.getShortName() : LogoType.NAVIGATION.getShortName()
			out << resource(dir: 'images', file: image)
        }
    }

    /**
     * Add a company and children to the page scope.
     */
    def company = { attrs ->
        def result = companyService.company(attrs['children'])
        result.each { pageScope[it.key] = it.value}
    }
}
