package jbilling
import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.pricing.RouteBL
import com.sapienter.jbilling.server.pricing.RouteRecord
import com.sapienter.jbilling.server.pricing.cache.RouteFinder
import com.sapienter.jbilling.server.pricing.db.RouteDAS
import com.sapienter.jbilling.server.pricing.db.RouteDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.plugin.springsecurity.annotation.Secured

import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource

import javax.sql.DataSource

@Secured(["MENU_99"])
class RouteTestController {
    static pagination = [max: 10, offset: 0]

    ViewUtils viewUtils
    DataSource dataSource
    def breadcrumbService
    def routes
    PluginAwareResourceBundleMessageSource messageSource
    SecurityValidator securityValidator

    def index (){
        list()
    }

    def list (){
        def routes = new RouteDAS().getRootRoutes(session['company_id'])
        render view: 'list', model: [routes: routes]
    }

    def testTreeRoute (){

        def fields = params.fields
		RouteDTO rootRoute = null;
        if (fields) {
            try {
                List<PricingField> pricingFields = constructPricingFields(fields)
				
				if(NumberUtils.isDigits(params['rootRoute'])){
						rootRoute = new RouteDAS().getRoute(Integer.parseInt(params.rootRoute))

                    securityValidator.validateCompany(rootRoute?.company?.id, Validator.Type.EDIT)

                } else{
					flash.error = "route.no.company.root.route";
					render template: 'result'
					return
				}

                if (rootRoute && pricingFields) {
                    RouteBL routeBL = new RouteBL(rootRoute)
                    RouteFinder routeFinder = routeBL.getBeanFactory().getFinderInstance();

                    RouteRecord routeRecord = routeFinder.findTreeRoute(rootRoute, pricingFields)

                    render template: 'result', model: [
                            rootRoute: rootRoute,
                            routeRecord: routeRecord,
                            fields: pricingFields
                    ]
                } else {
                    List<String> messages = new ArrayList<String>()

                    if (!rootRoute) {
                        messages.add(messageSource.getMessage("route.no.company.root.route", null, session.locale))
                    }

                    if (!pricingFields) {
                        messages.add(messageSource.getMessage("route.can.not.process.fields", null, session.locale))
                    }

                    flash.errorMessages = messages
                    render template: 'result'
                    return
                }
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                render template: 'result'
                return
            }
        } else {
            List<String> messages = new ArrayList<String>()
            messages.add(messageSource.getMessage("route.can.not.process.fields", null, session.locale))
            flash.errorMessages = messages

            render template: 'result', model: [
                    route: null,
                    routeRecord: null,
                    fields: null
            ]
        }
    }

    private List<PricingField> constructPricingFields (String fields) {

        String input = fields.trim()

        if ((input.length() < 3) || (input.charAt(0) != '[') || (input.charAt(input.length()-1) != ']')) {
            return null
        }

        List<PricingField> pricingFields = new ArrayList<PricingField>()

        for (String field: input.substring(1, input.length()-1).split(";")) {

            if (! field.contains("=")){
                continue
            }
            String[] parts = field.trim().split("=")

            if (parts.length > 2) {
                throw new SessionInternalError("Error in pricing fields string ${fields}", ["bad field: ${field}"] as String[])
            }

            String fieldName = extractQuotedValue(parts[0])
            if (! fieldName) {
                throw new SessionInternalError("Error in pricing fields string: ${fields}", ["bad fieldName in: ${field}"] as String[])
            }

            String fieldStrValue = (parts.length > 1) ? extractQuotedValue(parts[1]) : ''

            PricingField.add(pricingFields, new PricingField(fieldName, fieldStrValue))
        }

        return pricingFields ?: null
    }

    private String extractQuotedValue (String quotedString) {

        String input = quotedString.trim()

        if ((input.length() < 2) || (input.charAt(0) != '\'') || (input.charAt(input.length()-1) != '\'')) {
            return ''
        }
        return input.substring(1, input.length()-1)
    }
}
