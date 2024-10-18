/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package jbilling

import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.user.IUserSessionBean
import com.sapienter.jbilling.server.user.db.CompanyDTO

import java.beans.PropertyChangeEvent;
import com.sapienter.jbilling.server.util.SecurityValidator

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO
import com.sapienter.jbilling.server.payment.IPaymentSessionBean
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.user.UserBL

import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional;

import com.sapienter.jbilling.server.payment.blacklist.CsvProcessor

import org.hibernate.criterion.*;

@Secured(["CUSTOMER_14"])
class BlacklistController {
	
	static scope = "singleton"
	
    SecurityValidator securityValidator

    def index () {
        flash.invalidToken = flash.invalidToken
        redirect action: 'list', params: params
    }

    def getFilteredList(params) {

        CompanyDTO companyDTO = CompanyDTO.get(session['company_id'])
        List<Integer> companyIds = []
        if (!companyDTO.getParent()) {
            companyIds = companyDTO.createCriteria().listDistinct() {
                projections {
                    property("id")
                }
                eq('parent.id', companyDTO.getId())
            }
        }
        companyIds.add(companyDTO.getId())

        def blacklist= BlacklistDTO.createCriteria().listDistinct() {
	        createAlias("company", "company", CriteriaSpecification.INNER_JOIN)
			createAlias("user", "user", CriteriaSpecification.LEFT_JOIN)
            createAlias("contact", "contact", CriteriaSpecification.LEFT_JOIN)
			
			if (params.filterBy && params.filterBy != message(code: 'blacklist.filter.by.default')) {
				 createAlias("creditCard.metaFields", "mf", CriteriaSpecification.LEFT_JOIN)
				 createAlias("mf.field","fieldName", CriteriaSpecification.LEFT_JOIN)
				 
                 or {
                     eq('user.id', params.int('filterBy'))
					 ilike('user.userName', "${params.filterBy}%")
                     ilike("contact.firstName", "${params.filterBy}%")
                     ilike("contact.lastName", "${params.filterBy}%")
                     ilike("contact.organizationName", "${params.filterBy}%")

					 def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
					 .setProjection(Projections.property('id'))
					 .add(Restrictions.like('stringValue.value', "%${params.filterBy}%").ignoreCase())
					 and{
						 addToCriteria(Property.forName("mf.id").in(subCriteria))
						 eq('fieldName.name',CommonConstants.METAFIELD_NAME_CC_NUMBER)
                     }
                 }
             }
            'in'('company.id', companyIds)
            or {
                isNull('user')
                eq('user.deleted', 0)
            }
            order('id', 'asc')
        }
    }

    def list () {
        def blacklist = getFilteredList(params)
        def selected = params.id ? BlacklistDTO.get(params.int('id')) : null
        if(selected) {
            securityValidator.validateCompany(selected?.company?.id, Validator.Type.VIEW)
        }
        render view: 'list', model: [ blacklist: blacklist, selected: selected ]
    }

	def filter () {
		 def blacklist = getFilteredList(params)
		 render template: 'entryList', model: [blacklist: blacklist]
	}

    def show () {
        def entry = BlacklistDTO.get(params.int('id'))
        securityValidator.validateCompany(entry?.company?.id, Validator.Type.VIEW)

        render template: 'show', model: [selected: entry]
    }

    @RequiresValidFormToken
    def save () {
        def replace = params.csvUpload == 'modify'
        def file = request.getFile('csv');
        if (!params.csv.getContentType().toString().contains('text/csv')) {
            flash.error = "csv.error.found"
            redirect action: 'list'
            return
        } else if (!file.empty) {
            def csvFile = File.createTempFile("blacklist", ".csv")
            file.transferTo(csvFile)

            IPaymentSessionBean paymentSession = Context.getBean(Context.Name.PAYMENT_SESSION)
            def added
            try {
                added = paymentSession.processCsvBlacklist(csvFile.getAbsolutePath(), replace, (Integer) session['company_id'])
                flash.message = replace ? 'blacklist.updated' : 'blacklist.added'
                flash.args = [added]
                redirect view: 'list'
            } catch (CsvProcessor.ParseException e) {
                log.debug "Invalid format for the Blacklsit CSV file"
                flash.error = "Invalid format for the Blacklist CSV file"
                redirect action: 'list'
            }
        }

        redirect action: 'list'
    }

	@Transactional(readOnly = false)
    def user () {
        if (params.id) {
            Integer userId = params.int('id')
            Integer executorId = (Integer) session['user_id']
            def bl = new UserBL(userId)
            securityValidator.validateUserAndCompany(bl.getUserWS(), Validator.Type.EDIT)
            IUserSessionBean iUserSessionBean = (IUserSessionBean) Context.getBean(Context.Name.USER_SESSION)
            iUserSessionBean.setUserBlacklisted(executorId, userId, true)

            flash.message = 'user.blacklisted'
            flash.args = [params.id as String]
        }

        redirect controller: 'customerInspector', action: 'inspect', id: params.id
    }

}
