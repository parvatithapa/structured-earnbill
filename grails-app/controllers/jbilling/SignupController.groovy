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

import com.sapienter.jbilling.client.EntityDefaults
import com.sapienter.jbilling.common.SystemProperties
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.company.task.SystemAdminCopyTask
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.security.JBCrypto
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.contact.db.ContactMapDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.SubscriberStatusDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserStatusDAS
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS
import com.sapienter.jbilling.server.user.RoleBL
import com.sapienter.jbilling.server.util.LogoType
import com.sapienter.jbilling.server.util.credentials.PasswordService
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.api.validation.EntitySignupValidationGroup
import com.sapienter.jbilling.server.util.db.CurrencyDTO
import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.metafields.*
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.httpclient.HttpStatus

import javax.validation.groups.Default

import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.NumberUtils

import java.util.regex.Pattern

/**
 * SignupController 
 *
 * @author Brian Cowdery
 * @since 10/03/11
 */
class SignupController {
	
	static scope = "singleton"
	
	IWebServicesSessionBean webServicesSession
	
    def webServicesValidationAdvice
    def messageSource
    def viewUtils
    def springSecurityService
    def securityContextLogoutHandler
    def productService
    def PasswordService passwordService;

	static transactional = true
	
    def index () {
		if (0 < CompanyDTO.list().size()) {
			if(!springSecurityService.isLoggedIn()){
				flash.error = 'signup.not.allowed'
				redirect controller: 'login', action: 'auth'
			}
		}
        params.timezone = Constants.DEFAULT_TIMEZONE
    }

	@Secured(["CONFIGURATION_1910"])
	def reseller (){
		render view: 'index'
        params.timezone = Constants.DEFAULT_TIMEZONE
	}

	@RequiresValidFormToken
    def save () {
		CompanyDTO.withTransaction { status ->
			// validate company description to be non empty and more than 5 chars long
			if(params['contact.organizationName'] == null || params['contact.organizationName'].trim().size() < 5) {
				flash.error = 'Company name should be minimum 5 characters long.'
				render view: 'index'
				return
			}

			// validate required fields
			try {
				ContactWS contact = new ContactWS()
				bindData(contact, params, 'contact')

				if(springSecurityService.isLoggedIn()){
					contact.setInvoiceAsReseller(true);
				}

				if (!StringUtils.isEmpty(contact.phoneCountryCode)) {
					if (!NumberUtils.isDigits(contact.phoneCountryCode)) {
						flash.error = 'validation.error.phoneCountryCode.should.be.numeric'
						render view: 'index'
						return
					}
				}

				if (!StringUtils.isEmpty(contact.phoneAreaCode)) {
					if (!NumberUtils.isDigits(contact.phoneAreaCode)) {
						flash.error = 'validation.error.phoneAreaCode.should.be.numeric'
						render view: 'index'
						return
					}
				}
				log.debug contact

				UserWS user = new UserWS()
				bindData(user, params, 'user')

				user.contact = contact
				webServicesValidationAdvice.validateObject(user, Default.class, EntitySignupValidationGroup.class)

			} catch (SessionInternalError e) {
            	viewUtils.resolveException(flash, session?.locale ?: LanguageDTO.DefaultLocale, e)
				render view: 'index'
				return
			}

			if (params['user.password'] != params.verifiedPassword) {
				flash.error = 'passwords.dont.match'
				render view: 'index'
				return
			}

			if(CompanyDTO.findByDescription(params['contact.organizationName'])) {
				// show a error message and return
				flash.error = 'company.already.exists'
				flash.args = [params.contact.organizationName]
				render view: 'index'
				return
			}
			/*
				Create the new entity, root user and basic contact information
			*/

			// create company
			def language = LanguageDTO.get(params.languageId)
			def currency = CurrencyDTO.get(params.currencyId)
			try {
				def company = createCompany(language, currency)
				def companyContact = createCompanyContact(company)

				// create root user and contact information
				def user = createUser(language, currency, company)
				def userContact = createUserContact(user)
				user.setContact(userContact)

				// set all entity defaults
				new EntityDefaults(company, user, language, messageSource).init()

				passwordService.createPassword(user)

				//create  Internal type ProductCategory for new Company
				productService.createInternalTypeCategory(company)

				//create a default customer inspector template
				StringBuilder customerInformationDefault = getCustomerInformationDefault();
				session['configurationFile'] = customerInformationDefault

				//In case of creation of child entity and Invoice as reseller
				if(springSecurityService.isLoggedIn()){
					//Get current logged in user id
					def loggedInUser = UserDTO.get(springSecurityService.principal.userId)
					//set child company's parent company
					company.parent = loggedInUser.company

					boolean rollback = false

					UserWS resellerUser= new UserWS()

					bindResellerUser(resellerUser,params, loggedInUser.company)
					mockMetaFields(resellerUser,params, loggedInUser.company)
					resellerUser.setCreateCredentials(true)

					log.debug resellerUser

					try {
						def resellerId = webServicesSession.createUserWithCompanyId(resellerUser, company.parent.id)
						resellerUser.id = resellerId
						createUserContact(resellerUser)

						// mark company as reseller
						company.invoiceAsReseller = true
						company.reseller = UserDTO.get(resellerId)
						company.save()

						SystemAdminCopyTask sysAdminCopyTask = new SystemAdminCopyTask()
						sysAdminCopyTask.create(company.parent.id, company.id)
					}
					catch (Exception e) {
						// Roll back transaction if any error occurs
						status.setRollbackOnly()
						rollback = true
					}

					// if logged in, delete the remember me cookie and log the user out
					// the user should always be shown the login page after signup
					if (rollback) {
						flash.error = 'customer.account.types.not.available'
						render view:'index'
					} else {
						company.save()
						flash.message = 'signup.successful'
						flash.args = [ companyContact.organizationName, user.company.id, user.userName ]
						//response.deleteCookie(SpringSecurityUtils.securityConfig.rememberMe.cookieName)
						//redirect uri: SpringSecurityUtils.securityConfig.logout.filterProcessesUrl
						redirect controller: 'config', action: 'company'
					}
				} else {
					SystemAdminCopyTask sysAdminCopyTask = new SystemAdminCopyTask()
					sysAdminCopyTask.setLogged(false)
					sysAdminCopyTask.create(company.id, company.id)

					flash.message = 'signup.successful'
					flash.args = [ companyContact.organizationName, user.company.id, user.userName ]

					redirect controller: 'login', action: 'auth', params: [ userName: user.userName, companyId: company.id ]
				}
			} catch (FileNotFoundException fe){
				flash.error = 'signup.logo.filed.not.found'
				render view:'index'
			}
		}
    }

    /**
     * Create a new company for the given language and currency.
     *
     * @param language
     * @param currency
     * @return created company
     */
    def createCompany(language, currency) {
        def company = new CompanyDTO(
                description: StringUtils.left(params['contact.organizationName'], 100),
                createDatetime: TimezoneHelper.serverCurrentDate(),
                language: language,
                currency: currency,
                deleted: 0,
                timezone: params['timezone']
        ).save()

        return company
    }

    /**
     * Create new root user for the given company, currency, and language.
     *
     * @param language
     * @param currency
     * @param company
     * @return created root user
     */
    def createUser(language, currency, company) {
		UserStatusDAS userStatusDAS = new UserStatusDAS()
        def user = new UserDTO()
        bindData(user, params, 'user')
        def methodId = JBCrypto.getPasswordEncoderId(null);
        user.encryptionScheme = methodId
        user.deleted = 0
        user.userStatus = userStatusDAS.find(UserDTOEx.STATUS_ACTIVE)
        user.subscriberStatus = new SubscriberStatusDAS().find(UserDTOEx.SUBSCRIBER_ACTIVE)
        user.language = language
        user.currency = currency
        user.company = company
        log.debug("Company @@@@@@@@@@@@@@@@: "+company)
        user.createDatetime = TimezoneHelper.serverCurrentDate()

		createDefaultRoles(language, company)

		// get root role		
        def rootRole = new RoleDAS().findByRoleTypeIdAndCompanyId(
			Constants.TYPE_ROOT, company.id)

        user.roles.add(rootRole);
        user = user.save(flush:true)

        return user
    }
	
	/**
	 * 	Creates default roles taken from another company
	 * 
	 * @param language
	 * @param currency
	 * @param company
	 * @return
	 */
	def createDefaultRoles(language, company) {

		def defaultRoleList = [ Constants.TYPE_ROOT, Constants.TYPE_CLERK, Constants.TYPE_CUSTOMER, Constants.TYPE_PARTNER, Constants.TYPE_SYSTEM_ADMIN]

		def roleService = new RoleBL()

		defaultRoleList.each() {

			def role = new RoleDAS().findByRoleTypeIdAndCompanyId(
					it as Integer, null)

			// check the initial role ( companyId = null )
			if (!role) {
				// if not initial role set use the latest company role settings available
				def defaultCompanyId = CompanyDTO.createCriteria().get {
					projections {
						min("id")
					}
				}
				role = new RoleDAS().findByRoleTypeIdAndCompanyId(
						it as Integer, defaultCompanyId as Integer)
			}
			
			if (!role) {
				return
			}

			def newRole = new RoleDTO()
			newRole.permissions.addAll(role.permissions)
			newRole.company = company
			newRole.roleTypeId = it
            newRole.requiredToModify = role.requiredToModify
            newRole.requiredToCreateUser = role.requiredToCreateUser
            newRole.final = role.final
			newRole.expirePassword = false
			newRole.passwordExpireDays = 0

			roleService.create(newRole)
            roleService.setDescription(language.id, role.getDescription(language.id)?:role.getDescription())
            roleService.setTitle(language.id, role.getTitle(language.id)?:role.getTitle(1))

		}
	}
	
    /**
     * Create a new primary contact for the given user.
     *
     * @param user
     * @return created user contact
     */
    def createUserContact(user) {
        def userContact = new ContactDTO()
        bindData(userContact, params, 'contact')
        userContact.deleted = 0
        userContact.createDate = TimezoneHelper.serverCurrentDate()
        userContact.userId = user.id
        userContact.include = 1
        userContact.save()

        // map contact to the user table
        // map contact to the primary contact type
        new ContactMapDTO(
                jbillingTable: JbillingTable.findByName(Constants.TABLE_BASE_USER),
                contact: userContact,
                foreignId: user.id
        ).save(flush:true)

        return userContact
    }

    /**
     * Create a new contact for the company.
     *
     * @param company
     * @return created company contact
     */
    def createCompanyContact(company) {
        def entityContact = new ContactDTO()
        bindData(entityContact, params, 'contact')
        entityContact.deleted = 0
        entityContact.createDate = TimezoneHelper.serverCurrentDate()
        entityContact.save()

        // map contact to the entity table
        // map contact to the base entity contact type
        new ContactMapDTO(
                jbillingTable: JbillingTable.findByName(Constants.TABLE_ENTITY),
                contact: entityContact,
                foreignId: company.id
        ).save()

        return entityContact
    }
	
	/**
	 * binds param fields to reseller user
	 * 
	 * @param user	: reseller userws
	 * @return
	 */
	def bindResellerUser(user,params, company) {
		user.setCompanyName(company.description)
		user.setUserName(params['contact.organizationName'])
		user.setPassword(Constants.RESELLER_PASSWORD);
		user.setMainRoleId(Constants.TYPE_CUSTOMER);
		user.setLanguageId(company.language.id)
		def accountTypes= com.sapienter.jbilling.server.user.db.AccountTypeDTO.findAllByCompany(company,[sort: "id"]);
		//use first one if exists
		if (accountTypes?.size > 0 ) {
			user.setAccountTypeId(accountTypes.get(0)?.id);
		}
		user.setStatusId(UserDTOEx.STATUS_ACTIVE)
		user.setCurrencyId(company.currency.id)
	}
	/**
	 * Mocks meta field values for the reseller user
	 * 
	 * @param user	:	reseller userws
	 */
	def mockMetaFields(user, params, company) {
		def metaFields = [] 
		
		log.debug "Company ID " + company.id
		log.debug "Account Type ID " + user.accountTypeId
		
		def metaFieldsDefined= []
		//here we use addAll expect << because we need to add the elements in metaFieldsDefined not the list of elements.
		metaFieldsDefined.addAll(MetaFieldBL.getAvailableFieldsList(company.id as Integer, EntityType.CUSTOMER as EntityType[])?.findAll {
			it.isMandatory()
		});
		Map<Integer, List<MetaField>> allAITMFsByID= MetaFieldExternalHelper.getAvailableAccountTypeFieldsMap(user.accountTypeId as Integer)
		allAITMFsByID.each { k, v ->
			for (m in v) {
				if (m.isMandatory()) {
					metaFieldsDefined.add(m);
				}
			}
		}
		
		//use first one if exists
		for (MetaField mf in metaFieldsDefined) {
			if(mf.isMandatory()) {
				def metaField1 = new MetaFieldValueWS();
				metaField1.setFieldName(mf.getName()); //same as the meta field name
				//here we need to set group id because we have the same name meta-field in a different AIT group.
				metaField1.setGroupId(MetaFieldBL.getGroupId(mf));//same as account info type meta field group id
				if (mf.getDefaultValue()) {
					metaField1.setValue(mf.getDefaultValue())
				} else {

					switch(mf.getDataType()) {
						case DataType.STRING:
						case DataType.STATIC_TEXT:
						case DataType.TEXT_AREA:
						case DataType.ENUMERATION:
							metaField1.setValue("-");
							break;
						case DataType.JSON_OBJECT:
							metaField1.setValue("{}");
							break;
						case DataType.INTEGER:
							metaField1.setValue(Integer.valueOf(0));
							break;
						case DataType.DECIMAL:
							metaField1.setValue(BigDecimal.ZERO);
							break;
						case DataType.BOOLEAN:
							metaField1.setValue(false);
							break;
						case DataType.DATE:
							metaField1.setValue(TimezoneHelper.currentDateForTimezone(session['company_timezone']))
							break;
						case DataType.LIST:
							metaField1.setValue([])
							break;
					}

					def emailType = mf.getFieldUsage()
					if (emailType == MetaFieldType.EMAIL ) {
						log.debug params['contact.email']
						log.debug mf.name
						//metaField1.setStringValue(String.valueOf(params['contact.email']))
						metaField1.setValue(String.valueOf(params['contact.email']))
					}

					def orgName = mf.getFieldUsage()
					if (orgName == MetaFieldType.ORGANIZATION ) {
						log.debug company.description
						//metaField1.setStringValue(company.description)
						metaField1.setValue(company.description)
					}
				}
				log.debug "Mandatory Meta field name ${metaField1.getFieldName()}, value: ${metaField1.getValue()}"
				metaFields << metaField1
			}
		}
		
		user.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]))
	}

    def copyCompany() {

		def map = [:]
        Integer entityId = params.int("id");
		String childCompanyTemplateName = params.childCompany
		boolean isCompanyChild = params.boolean("isCompanyChild");
		boolean copyProducts = params.boolean("copyProducts");
		boolean copyPlans = params.boolean("copyPlans");
        List<String> importEntities = params.entities instanceof String ? params.entities as List : params.entities
		if (entityId) {
			UserWS userWS = null
			try {
				//For EarnBill-Saas instance
				if (grailsApplication.config.useUniqueLoginName) {
					if (StringUtils.isNotBlank(params?.systemAdminLoginName) && Pattern
							.compile("^[a-zA-Z0-9.+_-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
							.matcher(params?.systemAdminLoginName).matches()) {
						userWS = webServicesSession.copyCompanyInSaas(
								childCompanyTemplateName,
								entityId,
								importEntities,
								isCompanyChild,
								copyProducts,
								copyPlans,
								params.systemAdminEmail,
								params.systemAdminLoginName
						)

					} else {
						map.error = StringUtils.isBlank(params?.systemAdminLoginName) ?
								message(code: 'user.error.name.blank') :
								message(code: 'loginName.email.validation')
						render(status: HttpStatus.SC_OK, text: map as JSON)
						flash.errorMessages = null
						return
					}
				} else {
					//For EarnBill instance
					userWS = webServicesSession.copyCompany(
							childCompanyTemplateName,
							entityId,
							importEntities,
							isCompanyChild,
							copyProducts,
							copyPlans,
							params.adminEmail
					)
				}
				map.message = g.message(code: 'copy.company.create.label', args:
						[userWS.userName, userWS.password, CompanyDTO.get(userWS.entityId).description, userWS.entityId])

			} catch (SessionInternalError e) {
				viewUtils.resolveException(flash, session?.locale ?: LanguageDTO.DefaultLocale, e)
				map.error = flash.errorMessages
				render(status: HttpStatus.SC_OK, text: map as JSON)

				flash.errorMessages = null
				return
			} finally {
				if (userWS != null)
					userWS.close()
			}
		}

		render(status: HttpStatus.SC_OK, text: map as JSON)
		return
    }

    /**
     * Default customer inspector template

         <?xml version="1.0" encoding="UTF-8"?>
            <customerInformation>
               <row>
                  <column>
                     <basic label="Login Name: " entity="CUSTOMER" name="userName" style="font-weight:bold ;color: green"/>
                  </column>
                  <column>
                     <basic label="User ID" entity="CUSTOMER" name="id" style="font-weight:bold ;color: green"/>
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Last Login" entity="CUSTOMER" name="lastLogin" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Type" entity="CUSTOMER" name="role" style="font-weight: bold" />
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Status" entity="CUSTOMER" name="status" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Account Type" entity="CUSTOMER" name="accountTypeId" style="font-weight: bold" />
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Subscriber Status" entity="CUSTOMER" name="subscriberStatusId" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Agent ID" entity="CUSTOMER" name="partnerId" style="font-weight: bold" />
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Language: " entity="CUSTOMER" name="language" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Credit Limit" entity="CUSTOMER" name="creditLimit" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Currency" entity="CUSTOMER" name="currencyId" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Auto-Recharge Amount" entity="CUSTOMER" name="autoRecharge" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Exclude from Collections" entity="CUSTOMER" name="excludeAgeing" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Dynamic Balance" entity="CUSTOMER" name="dynamicBalance" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                     <basic label="Invoice Design" entity="CUSTOMER" name="invoiceDesign" style="font-weight: bold"/>
                  </column>
                  <column>
                     <basic label="Invoice Delivery Method" entity="CUSTOMER" name="invoiceDeliveryMethodId" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                <static label= "" value=""/>
                  </column>
                  <column>
                     <basic label="Due Date" entity="CUSTOMER" name="dueDateValue" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                <static label= "" value=""/>
                  </column>
                  <column>
                     <basic label="Next Invoice Date" entity="CUSTOMER" name="nextInvoiceDate" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                <static label= "" value=""/>
                  </column>
                  <column>
                     <basic label="Lifetime revenue" entity="CUSTOMER" name="" style="font-weight: bold"/>
                  </column>
               </row>
               <row>
                  <column>
                <static label= "" value=""/>
                  </column>
                  <column>
                     <basic label="Total Owed" entity="CUSTOMER" name="" style="font-weight: bold"/>
                  </column>
               </row>
            </customerInformation>

     */
    def StringBuilder getCustomerInformationDefault() {
        String columnOpen = "<column>";
        String columnClose = "</column>";
        String rowOpen = "<row>";
        String rowClose = "</row>";
        StringBuilder xml = new StringBuilder()
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        xml.append("<customerInformation>")

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Login Name: \" entity=\"CUSTOMER\" name=\"userName\" style=\"font-weight:bold ;color: green\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"User ID\" entity=\"CUSTOMER\" name=\"id\" style=\"font-weight:bold ;color: green\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Last Login\" entity=\"CUSTOMER\" name=\"lastLogin\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Type\" entity=\"CUSTOMER\" name=\"role\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Status\" entity=\"CUSTOMER\" name=\"status\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Account Type\" entity=\"CUSTOMER\" name=\"accountTypeId\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Subscriber Status\" entity=\"CUSTOMER\" name=\"subscriberStatusId\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Agent ID\" entity=\"CUSTOMER\" name=\"partnerId\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Language: \" entity=\"CUSTOMER\" name=\"language\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Credit Limit\" entity=\"CUSTOMER\" name=\"creditLimit\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Currency\" entity=\"CUSTOMER\" name=\"currencyId\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Auto-Recharge Amount\" entity=\"CUSTOMER\" name=\"autoRecharge\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Exclude from Collections\" entity=\"CUSTOMER\" name=\"excludeAgeing\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Dynamic Balance\" entity=\"CUSTOMER\" name=\"dynamicBalance\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<basic label=\"Invoice Design\" entity=\"CUSTOMER\" name=\"invoiceDesign\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Invoice Delivery Method\" entity=\"CUSTOMER\" name=\"invoiceDeliveryMethodId\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<static label=\"\" value=\"\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Due Date\" entity=\"CUSTOMER\" name=\"dueDateValue\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<static label=\"\" value=\"\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Next Invoice Date\" entity=\"CUSTOMER\" name=\"nextInvoiceDate\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<static label=\"\" value=\"\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Lifetime revenue\" entity=\"CUSTOMER\" name=\"\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append(rowOpen)
        xml.append(columnOpen)
        xml.append("<static label=\"\" value=\"\" />");
        xml.append(columnClose)
        xml.append(columnOpen)
        xml.append("<basic label=\"Total Owed\" entity=\"CUSTOMER\" name=\"\" style=\"font-weight: bold\" />");
        xml.append(columnClose)
        xml.append(rowClose)

        xml.append("</customerInformation>")
    }

}
