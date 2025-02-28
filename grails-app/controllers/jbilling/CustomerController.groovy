/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.user.UserHelper
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.FlowHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper
import com.sapienter.jbilling.server.metafields.MetaFieldHelper
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.order.OrderPeriodWS
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.payment.PaymentInformationBL
import com.sapienter.jbilling.server.payment.PaymentInformationWS
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS
import com.sapienter.jbilling.server.process.ConfigurationBL
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.CancellationRequestWS
import com.sapienter.jbilling.server.user.CustomerCommissionDefinitionWS;
import com.sapienter.jbilling.server.user.CustomerNoteWS
import com.sapienter.jbilling.server.user.MainSubscriptionWS
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO
import com.sapienter.jbilling.server.user.db.CancellationRequestDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO
import com.sapienter.jbilling.server.user.db.UserCodeDAS
import com.sapienter.jbilling.server.user.db.UserCodeObjectType
import com.sapienter.jbilling.server.user.db.UserDAS
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.db.UserExportableWrapper
import com.sapienter.jbilling.server.user.db.UserStatusDAS
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDefinitionDTO
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.CsvFileGeneratorUtil
import com.sapienter.jbilling.server.util.csv.DynamicExport;
import com.sapienter.jbilling.server.util.csv.Exporter
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import groovy.json.JsonBuilder

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.criterion.Criterion
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.LogicalExpression
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Property
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.type.StandardBasicTypes

@Secured(["MENU_90"])
class CustomerController {
	static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [ max: 25 ]

    static final viewColumnsToFields =
            ['userId': 'id',
            'userName': 'userName',
            'company': 'company.description',
            'status': 'userStatus.id']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    def filterService
    def recentItemService
    def breadcrumbService
    def springSecurityService
    def subAccountService
    def auditBL
    private def modelToPass

    SecurityValidator securityValidator


    @Secured(["hasAnyRole('MENU_90', 'CUSTOMER_15')"])
    def index () {
        list()
    }

    def getList(filters, statuses, params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        params.alias = params?.alias ?: pagination.alias
        params.fetch = null;

        List<String> contactFilters = ['contact.firstName','contact.lastName','contact.email','contact.phoneNumber',
                                       'contact.postalCode','contact.organizationName']
		def user_id = session['user_id']
        return UserDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            createAlias("customer", "customer")
            createAlias("company", "company")
            def aitFilters = []
            def customerMetaFieldFilters = []
            and {
                if(!Collections.disjoint(contactFilters,filters.findAll{it.value}*.field)) {
                    createAlias("customer.accountType", "accountType")
                    setFetchMode("accountType", FetchMode.JOIN)
                    createAlias("customer.customerAccountInfoTypeMetaFields", "aitMetaFields")
                    createAlias("aitMetaFields.accountInfoType", "accountInfoType")
                    createAlias("aitMetaFields.metaFieldValue", "mfv")
                    setFetchMode("mfv", FetchMode.JOIN)
                    eqProperty("accountType.preferredNotificationAitId", "accountInfoType.id")
                    createAlias("mfv.field", "metaField")
                    or{
                        filters.each { filter ->
                            if (filter.value && contactFilters.contains(filter.field)) {
                                DetachedCriteria metaFieldTypeSubCrit = DetachedCriteria.forClass(MetaField.class,"metaFieldType")
                                        .setProjection(Projections.property('id'))
                                        .add(Restrictions.sqlRestriction("exists (select * from meta_field_name where field_usage= ? and id = {alias}.id)",
                                        filterService.getMetaFieldTypeForFilter(filter.field)?.toString(),
                                        StandardBasicTypes.STRING))
                                Criterion metaFieldTypeCrit =Property.forName("metaField.id").in(metaFieldTypeSubCrit)
                                def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringMFValue")
                                        .setProjection(Projections.property('id'))
                                if(filter.field == 'contact.postalCode'){
                                    subCriteria.add(Restrictions.eq('stringMFValue.value', filter.stringValue))
                                }else {
                                    subCriteria.add(Restrictions.ilike('stringMFValue.value', filter.stringValue, MatchMode.ANYWHERE))
                                }
                                Criterion aitMfv = Property.forName("mfv.id").in(subCriteria)
                                LogicalExpression aitMfvAndType = Restrictions.and(aitMfv,metaFieldTypeCrit)
                                addToCriteria(aitMfvAndType)
                            }
                        }
                    }
                }

                filters.each { filter ->
                    if (filter.value) {
						if(filter.value != null && filter.field == 'as.identifier'){
							createAlias("orders", "or")
							createAlias("or.lines", "ol")
							createAlias("ol.assets", "as")
							addToCriteria( Restrictions.ilike("as.identifier",  filter.stringValue, MatchMode.ANYWHERE) )
						}
                        // handle user status separately from the other constraints
                        // we need to find the UserStatusDTO to compare to
                        if (filter.constraintType == FilterConstraint.STATUS) {
                            eq("userStatus", statuses.find{ it.id == filter.integerValue })

                        } else if (filter.field == 'contact.fields') {
                            if(null != filter.fieldKeyData)
                                customerMetaFieldFilters.add(filter)
                        } else if (filter.field == 'accountTypeFields') {
                            if(null != filter.fieldKeyData)
                                aitFilters.add(filter)
                        } else if(filter.field == 'u.company.description') {
							addToCriteria( Restrictions.ilike("company.description",  filter.stringValue, MatchMode.ANYWHERE) );
                        } else if(filter.field == 'userCodes.userCode.identifier') {
                            createAlias("customer.userCodeLinks", "userCodes")
                            createAlias("userCodes.userCode", "userCode")
                            addToCriteria( Restrictions.eq("userCode.identifier",  filter.stringValue) )
                        } else if(filter.field == 'customer.partner.id'){
                            createAlias("customer.partners", "_partner")
                            addToCriteria( Restrictions.eq("_partner.id",  filter.value) )
                        } else if(filter.field == 'invoices'){
							createAlias("invoices", "inv")
							eq("inv.deleted", 0)
						}else if (!contactFilters.contains(filter.field)){
                            addToCriteria(filter.getRestrictions());
                        }
                    } else if (filter.value != null && filter.field == 'deleted') {
						addToCriteria(filter.getRestrictions());
					}
                }

                if(params.userId) {
                    eq('id', params.int('userId'))
                }

                if(params.company) {
                    addToCriteria( Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.userName) {
                    Criterion USER_NAME = Restrictions.ilike("userName",  params.userName, MatchMode.ANYWHERE);
                    addToCriteria(USER_NAME)
                }
                if (params.status) {
                    eq("userStatus", statuses.find{ it.name == params.status })

                }

                //check that the user is a customer
                isNotNull('customer')
				'in'('company', retrieveCompanies())

                if (SpringSecurityUtils.ifNotGranted("CUSTOMER_17")) {
                    UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
                    boolean partnerAliasCreated = false

                    if (loggedInUser.getPartner() != null) {
                        // #7043 - Agents && Commissions - A logged in Partner should only see its customers and the ones of his children.
                        // A child Partner should only see its customers.
                        def partnerIds = []
                        if (loggedInUser.getPartner() != null) {
                            partnerIds << loggedInUser.getPartner().getId()
                            if (loggedInUser.getPartner().getChildren()) {
                                partnerIds += loggedInUser.getPartner().getChildren().id
                            }
                        }
                        createAlias("customer.partners", "partner")
                        'in'('partner.id', partnerIds)

                        partnerAliasCreated = true
                    } else if (SpringSecurityUtils.ifAnyGranted("CUSTOMER_18")) {
                        // restrict query to sub-account user-ids
                        'in'('id', subAccountService.getSubAccountUserIds())
                    }

                    //not granted to see all customer, restrict by user role
                    UserDTO callerUser= UserDTO.get(user_id as int)
            		if ( callerUser.getRoles().find { it.roleTypeId == Constants.TYPE_PARTNER } ) {
                        if(!partnerAliasCreated) {
                            createAlias("customer.partners", "partner")
                        }
            			eq('partner.id', callerUser.partnersForUserId.id)
            		} else if ( callerUser.getRoles().find { it.roleTypeId == Constants.TYPE_CUSTOMER } ) {
						// limit list to only this customer
						eq('id', user_id)
            		}
                }
            }


            if (aitFilters.size() > 0) {
                aitFilters.each { filter ->
                    def detach = DetachedCriteria.forClass(CustomerDTO.class, "customer1")
                            .setProjection(Projections.property('customer1.id'))
                            .createAlias("customer1.customerAccountInfoTypeMetaFields", "timeLine")
                            .createAlias("timeLine.metaFieldValue", "fieldValue")
                            .createAlias("fieldValue.field", "field")
                            .setFetchMode("fieldValue", FetchMode.JOIN)
                    String typeId = filter.fieldKeyData
                    String ccfValue = filter.stringValue
                    log.debug "Account Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                    if (typeId && ccfValue) {
                        def type = findMetaFieldsByName(typeId.toInteger())
                        try {
                            def subCriteria = FilterService.metaFieldFilterCriteria(type, ccfValue)
                            if (type && subCriteria) {
                                detach.add(Restrictions.eq("field.name", type.name))
                                //Adding the sub criteria according to the datatype passed
                                detach.add(Property.forName("fieldValue.id").in(subCriteria))
                                addToCriteria(Property.forName("customer.id").in(detach))
                            }
                        } catch (SessionInternalError e) {
                            log.error("Invalid value in the custom field " + e.getMessage().toString())
                            throw new SessionInternalError(e.getMessage());
                        }
                    }
                }
            }

            if (customerMetaFieldFilters.size() > 0) {
                customerMetaFieldFilters.each { filter ->
                    def detach = DetachedCriteria.forClass(CustomerDTO.class, "customer2")
                            .setProjection(Projections.property('customer2.id'))
                            .createAlias("customer2.metaFields", "metaFieldValue")
                            .createAlias("metaFieldValue.field", "metaField")
                            .setFetchMode("metaField", FetchMode.JOIN)
                    String typeId = filter.fieldKeyData
                    String ccfValue = filter.stringValue
                    log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                    if (typeId && ccfValue) {
                        def type1 = findMetaFieldsByName(typeId.toInteger())
                        try{
                            def subCriteria = FilterService.metaFieldFilterCriteria(type1, ccfValue);
                            if (type1 && subCriteria) {
                                detach.add(Restrictions.eq("metaField.name", type1.name))
                                //Adding the sub criteria according to the datatype passed
                                detach.add(Property.forName("metaFieldValue.id").in(subCriteria))
                                addToCriteria(Property.forName("customer.id").in(detach))
                            }
                        } catch (SessionInternalError e) {
                            log.error("Invalid value in the custom field " + e.getMessage())
                            throw new SessionInternalError(e.getMessage());
                        }
                    }
                }
            }

            resultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            // apply sorting
            SortableCriteria.buildSortNoAlias(params, delegate)
        }
    }

    @Secured
    def findPartners() {
        def term = params.term

        List<PartnerDTO> partnerList = PartnerDTO.createCriteria().list(
                max:    15,
        ) {
            and {
                createAlias('baseUser', 'baseUser')
                createAlias("baseUser.contact", "contact")

                eq('baseUser.company', CompanyDTO.get(session['company_id']))

                or {
                    if(term.isInteger()) {
                        eq('id', params.int('term'))
                    }
                    addToCriteria(Restrictions.ilike("brokerId", term, MatchMode.START))
                    addToCriteria(Restrictions.ilike("baseUser.userName", term, MatchMode.START))
                    addToCriteria(Restrictions.ilike("contact.firstName", term, MatchMode.ANYWHERE))
                    addToCriteria(Restrictions.ilike("contact.lastName", term, MatchMode.ANYWHERE))
                }
            }
        }

        render(contentType: "application/json") {
            def partners = array {
                for (partnerDTO in partnerList) {
                    partner( [label: partnerDTO.baseUser.contact.firstName ? (partnerDTO.baseUser.contact.firstName + ' ' + partnerDTO.baseUser.contact.lastName) : partnerDTO.baseUser.userName, value: partnerDTO.id] )
                }
            }
        }
    }

	def getFilteredUserIds(filters, statuses, params) {
		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order
		params.alias = SortableCriteria.NO_ALIAS
		List<String> contactFilters = ['contact.firstName','contact.lastName','contact.email','contact.phoneNumber',
									   'contact.postalCode','contact.organizationName']
		def user_id = session['user_id']
		return UserDTO.createCriteria().list(
				max:    params.max,
				offset: params.offset
		) {
            def aitFilters = []
            def customerMetaFieldFilters = []
			property('id')
			createAlias("customer", "customer")

			and {
				if(!Collections.disjoint(contactFilters,filters.findAll{it.value}*.field)) {
					createAlias("customer.accountType", "accountType")
					setFetchMode("accountType", FetchMode.JOIN)
					createAlias("customer.customerAccountInfoTypeMetaFields", "aitMetaFields")
					createAlias("aitMetaFields.accountInfoType", "accountInfoType")
					createAlias("aitMetaFields.metaFieldValue", "mfv")
					setFetchMode("mfv", FetchMode.JOIN)
					eqProperty("accountType.preferredNotificationAitId", "accountInfoType.id")
					createAlias("mfv.field", "metaField")
					or{
						filters.each { filter ->
							if (filter.value && contactFilters.contains(filter.field)) {
								DetachedCriteria metaFieldTypeSubCrit = DetachedCriteria.forClass(MetaField.class,"metaFieldType")
										.setProjection(Projections.property('id'))
										.add(Restrictions.sqlRestriction("exists (select * from meta_field_name where field_usage= ? and id = {alias}.id)",
										filterService.getMetaFieldTypeForFilter(filter.field)?.toString(),
										StandardBasicTypes.STRING))
								Criterion metaFieldTypeCrit =Property.forName("metaField.id").in(metaFieldTypeSubCrit)
								def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringMFValue")
										.setProjection(Projections.property('id'))
								if(filter.field == 'contact.postalCode'){
									subCriteria.add(Restrictions.eq('stringMFValue.value', filter.stringValue))
								}else {
									subCriteria.add(Restrictions.ilike('stringMFValue.value', filter.stringValue, MatchMode.ANYWHERE))
								}
								Criterion aitMfv = Property.forName("mfv.id").in(subCriteria)
								LogicalExpression aitMfvAndType = Restrictions.and(aitMfv,metaFieldTypeCrit)
								addToCriteria(aitMfvAndType)
							}
						}
					}
				}

				filters.each { filter ->
					if (filter.value != null) {
						if(filter.field == 'as.identifier'){
							createAlias("orders", "or")
							createAlias("or.lines", "ol")
							createAlias("ol.assets", "as")
							addToCriteria( Restrictions.ilike("as.identifier",  filter.stringValue, MatchMode.ANYWHERE) )
						}
						// handle user status separately from the other constraints
						// we need to find the UserStatusDTO to compare to
						if (filter.constraintType == FilterConstraint.STATUS) {
							eq("userStatus", statuses.find{ it.id == filter.integerValue })

						} else if (filter.field == 'contact.fields') {
                            if (null != filter.fieldKeyData)
                                customerMetaFieldFilters.add(filter)
						} else if (filter.field == 'accountTypeFields') {
							String typeId = params['accountTypeFields.fieldKeyData']?params['accountTypeFields.fieldKeyData']:filter.fieldKeyData
							String ccfValue = filter.stringValue
							log.debug "Account Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                            if (null != filter.fieldKeyData) {
                                aitFilters.add(filter)
                            }
						} else if(filter.field == 'u.company.description') {
							eq('company', CompanyDTO.findByDescriptionIlike('%' + filter.stringValue + '%'))
						} else if(filter.field == 'userCodes.userCode.identifier') {
							createAlias("customer.userCodeLinks", "userCodes")
							createAlias("userCodes.userCode", "userCode")
							addToCriteria( Restrictions.eq("userCode.identifier",  filter.stringValue) )
						} else if(filter.field == 'customer.partner.id'){
							createAlias("customer.partners", "_partner")
							addToCriteria( Restrictions.eq("_partner.id",  filter.value) )
						} else if(filter.field == 'invoices'){
							createAlias("invoices", "inv")
							eq("inv.deleted", 0)
                        } else if (filter.field == 'deleted') {
                            addToCriteria(filter.getRestrictions());
                        } else if (!contactFilters.contains(filter.field) && filter.getRestrictions()){
                            addToCriteria(filter.getRestrictions());
                        }
                    }
				}

				if(params.userId) {
					eq('id', params.int('userId'))
				}

				if(params.company) {
					addToCriteria( Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
				}
				if(params.userName) {
					Criterion USER_NAME = Restrictions.ilike("userName",  params.userName, MatchMode.ANYWHERE);
					addToCriteria(USER_NAME)
				}
				if (params.status) {
					eq("userStatus", statuses.find{ it.name == params.status })

				}

				//check that the user is a customer
				isNotNull('customer')
				'in'('company', retrieveCompanies())

				if (SpringSecurityUtils.ifNotGranted("CUSTOMER_17")) {
					UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
					boolean partnerAliasCreated = false

					if (loggedInUser.getPartner() != null) {
						// #7043 - Agents && Commissions - A logged in Partner should only see its customers and the ones of his children.
						// A child Partner should only see its customers.
						def partnerIds = []
						partnerIds << loggedInUser.getPartner().getId()
						if (loggedInUser.getPartner().getChildren()) {
							partnerIds += loggedInUser.getPartner().getChildren().id
						}
						createAlias("customer.partners", "partner")
						'in'('partner.id', partnerIds)

						partnerAliasCreated = true
					} else if (SpringSecurityUtils.ifAnyGranted("CUSTOMER_18")) {
						// restrict query to sub-account user-ids
						'in'('id', subAccountService.getSubAccountUserIds())
					}

					//not granted to see all customer, restrict by user role
					UserDTO callerUser= UserDTO.get(user_id as int)
					if ( callerUser.getRoles().find { it.roleTypeId == Constants.TYPE_PARTNER } ) {
						if(!partnerAliasCreated) {
							createAlias("customer.partners", "partner")
						}
						eq('partner.id', callerUser.partnersForUserId.id)
					} else if ( callerUser.getRoles().find { it.roleTypeId == Constants.TYPE_CUSTOMER } ) {
						// limit list to only this customer
						eq('id', user_id)
					}
				}
			}

            if (aitFilters.size() > 0) {
                aitFilters.each { filter ->
                    def detach = DetachedCriteria.forClass(CustomerDTO.class, "customer1")
                            .setProjection(Projections.property('customer1.id'))
                            .createAlias("customer1.customerAccountInfoTypeMetaFields", "timeLine")
                            .createAlias("timeLine.metaFieldValue", "fieldValue")
                            .createAlias("fieldValue.field", "field")
                            .setFetchMode("fieldValue", FetchMode.JOIN)
                    String typeId = filter.fieldKeyData
                    String ccfValue = filter.stringValue
                    log.debug "Account Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                    if (typeId && ccfValue) {
                        def type = findMetaFieldsByName(typeId.toInteger())
                        try {
                            def subCriteria = FilterService.metaFieldFilterCriteria(type, ccfValue)
                            if (type && subCriteria) {
                                detach.add(Restrictions.eq("field.name", type.name))
                                //Adding the sub criteria according to the datatype passed
                                detach.add(Property.forName("fieldValue.id").in(subCriteria))
                                addToCriteria(Property.forName("customer.id").in(detach))
                            }
                        } catch (SessionInternalError e) {
                            log.error("Invalid value in the custom field " + e.getMessage().toString())
                            throw new SessionInternalError(e.getMessage());
                        }
                    }
                }
            }

            if (customerMetaFieldFilters.size() > 0) {
                customerMetaFieldFilters.each { filter ->
                    def detach = DetachedCriteria.forClass(CustomerDTO.class, "customer2")
                            .setProjection(Projections.property('customer2.id'))
                            .createAlias("customer2.metaFields", "metaFieldValue")
                            .createAlias("metaFieldValue.field", "metaField")
                            .setFetchMode("metaField", FetchMode.JOIN)
                    String typeId = filter.fieldKeyData
                    String ccfValue = filter.stringValue
                    log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"
                    if (typeId && ccfValue) {
                        def type1 = findMetaFieldsByName(typeId.toInteger())
                        try{
                            def subCriteria = FilterService.metaFieldFilterCriteria(type1, ccfValue);
                            if (type1 && subCriteria) {
                                detach.add(Restrictions.eq("metaField.name", type1.name))
                                //Adding the sub criteria according to the datatype passed
                                detach.add(Property.forName("metaFieldValue.id").in(subCriteria))
                                addToCriteria(Property.forName("customer.id").in(detach))
                            }
                        } catch (SessionInternalError e) {
                            log.error("Invalid value in the custom field " + e.getMessage())
                            throw new SessionInternalError(e.getMessage());
                        }
                    }
                }
            }
            
			resultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
			// apply sorting
			SortableCriteria.sort(params, delegate)
		}
	}

    /**
     * Get a list of users and render the list page. If the "applyFilters" parameter is given, the
     * partial "_users.gsp" template will be rendered instead of the complete user list.
     */
    @Secured(["hasAnyRole('MENU_90', 'CUSTOMER_15')"])
    def findCustomers (){

        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findByEntityId(session['company_id'])
        def users = []

        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if(params.id?.isInteger() && selected == null){
            flash.info = message(code: 'flash.customer.not.found')
        }

        // if logged in as a customer, you can only view yourself
        try {
            if (SpringSecurityUtils.ifNotGranted("MENU_90")) {
                users << UserDTO.get(springSecurityService.principal.id)
            } else {
                users = getList(filters, statuses, params)
            }
            def jsonData = getCustomersJsonData(users, params)
            render jsonData as JSON
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

    }

    private def getChildren(GrailsParameterMap params, boolean withPagination) {
        def parent = UserDTO.get(params.int('id'))
        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(parent)), Validator.Type.VIEW)

        def paginationValues = [max: params.max]
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        if (withPagination){
            paginationValues.offset = params.offset
        }

        def children = UserDTO.createCriteria().list(paginationValues) {
            and {
                createAlias("company", "company")
                createAlias("customer", "customer")
                createAlias("customer.parent", "parent")
                createAlias("parent.baseUser", "baseUser")

                eq('deleted', 0)
                eq('baseUser.id', params.int('id'))
                order("id", "desc")

                if(params.userId) {
                    eq('customer.id', params.int('userId'))
                }

                if(params.company) {
                    addToCriteria(Restrictions.ilike("company.description",  params.company, MatchMode.ANYWHERE) );
                }
                if(params.userName) {
                    addToCriteria(Restrictions.ilike("userName",  params.userName, MatchMode.ANYWHERE))
                }
            }
            SortableCriteria.sort(params, delegate)
        }
        children
    }

    /**
     * Get a list of users and render the list page. If the "applyFilters" parameter is given, the
     * partial "_users.gsp" template will be rendered instead of the complete user list.
     */
    @Secured(["hasAnyRole('MENU_90', 'CUSTOMER_15')"])
    def list (){
        def filters = filterService.getFilters(FilterType.CUSTOMER, params)
        def statuses = new UserStatusDAS().findByEntityId(session['company_id'])
        def users = []

        UserDTO selected = null

        if(params.id) {
            def paramsClone = params.clone()
            paramsClone.clear()
            paramsClone['userId'] = params.int('id')

            def custTmp = getList([], statuses, paramsClone)
            if(custTmp.size() > 0) {
                selected = custTmp[0]
            }
        }

        def userData = [:]

        //validate if this user belongs to one of the given companies then show its details
        //and if the user is a customer
        selected = retrieveCompaniesIds().contains(selected?.company?.id) && selected?.getCustomer() ? selected : null

        //validate if current logged user is an agent and if he is able to edit the 'selected' customer
        def userIsAllowed = true
        if(SpringSecurityUtils.ifNotGranted("CUSTOMER_17")) {
            userIsAllowed = securityValidator.validateCustomerAgentRelationship(session['user_id'], selected, Validator.Type.VIEW)
        }
        selected = userIsAllowed ? selected : null

        if (selected) {
            //collect user related data, e.g. contact, latestPayment, revenue etc..
			userData = retrieveUserData(selected)
            def userBl = new UserBL(selected.id)
            selected.accountLocked = userBl.isAccountLocked()
            selected.accountExpired = userBl.validateAccountExpired(selected.accountDisabledDate)

            if(SpringSecurityUtils.ifNotGranted("CUSTOMER_15")) {
                selected = null
                userIsAllowed = false
            }
        }

        def crumbDescription = selected ? UserHelper.getDisplayName(selected, userData.contact) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id, crumbDescription)

        def contactFieldTypes = params['contactFieldTypes']

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'customersTemplate', model: [statuses: statuses, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])] + userData
            }else {
                render view: 'list', model: [statuses: statuses, filters: filters, customerNotes: retrieveCustomerNotes(), displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])] + userData
            }
            return
        }
        try {
            // if logged in as a customer, you can only view yourself
            if (SpringSecurityUtils.ifNotGranted("MENU_90")) {
                users << UserDTO.get(springSecurityService.principal.id)
            } else {
                users = getList(filters, statuses, params)
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }

        // Show error message if no customer found.
        if (params.int("id") && !selected && userIsAllowed) {
            flash.error = "flash.customer.not.found"
        }

        if (params.applyFilter || params.partial) {
            render template: 'customersTemplate', model: [
                    selected: selected,
                    users: users,
                    statuses: statuses,
                    filters: filters,
                    contactFieldTypes: contactFieldTypes,
                    displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']) ] + userData
        } else {
            render view: 'list', model: [selected: selected, users: users, statuses: statuses, filters: filters, customerNotes: retrieveCustomerNotes(), displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])] + userData
        }
    }

    def getListWithSelected(statuses, selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        try{
            getList([idFilter], statuses, params)
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
    }

	/**
     * Applies the set filters to the user list, and exports it as a CSV for generate.
     */
	@Secured(["CUSTOMER_16"])
	def csv () {
		try {
			def filters = filterService.getFilters(FilterType.CUSTOMER, params)
			def statuses = new UserStatusDAS().findByEntityId(session['company_id'])

			// For when the csv is exported on JQGrid
			params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
			params.order  = (params.sord != null ? params.sord : params.order)
			params.max = CsvExporter.MAX_RESULTS

            def filterdUserIds = getFilteredUserIds(filters, statuses, params);
			if(PreferenceBL.getPreferenceValueAsIntegerOrZero(session['company_id'], Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != 0) {
				def users = getUsers(filterdUserIds, DynamicExport.YES)
				CsvFileGeneratorUtil.generateCSV(com.sapienter.jbilling.client.util.Constants.USER_CSV,users, session['company_id'],session['user_id']);
				render "success"
			} else {
                def users = getUsers(filterdUserIds, DynamicExport.NO);
				renderUserCsvFor(users)
			}
		} catch (SessionInternalError e) {
			log.error e.getMessage()
			viewUtils.resolveException(flash, session.locale, e);
		}
	}

	List<UserExportableWrapper> getUsers(def userIds, def dynamicExport) {
		userIds.remove((Object) 108100)
		List<UserExportableWrapper> users = new ArrayList<>()
		for(Integer userId: userIds) {
			users.add(new UserExportableWrapper(userId, dynamicExport))
		}
		return users;
	}


    /**
     * Applies the set filters to the user list, and exports it as a CSV for download.
     */
    @Secured(["CUSTOMER_16"])
    def subaccountsCsv () {
        // For when the csv is exported on JQGrid
        params.sort = viewColumnsToFields[params.sidx] != null ? viewColumnsToFields[params.sidx] : params.sort
        params.order  = (params.sord != null ? params.sord : params.order)
        params.max = CsvExporter.MAX_RESULTS

        def users = getChildren(params, false)

        renderCsvFor(users)
    }

    def renderCsvFor(users) {
        if (users.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list'
        } else {
            DownloadHelper.setResponseHeader(response, "users.csv")
            Exporter<UserDTO> exporter = CsvExporter.createExporter(UserDTO.class);
            render text: exporter.export(users), contentType: "text/csv"
        }
    }

	def renderUserCsvFor(userExportables) {
		if (userExportables.size() > CsvExporter.MAX_RESULTS) {
			flash.error = message(code: 'error.export.exceeds.maximum')
			flash.args = [ CsvExporter.MAX_RESULTS ]
			redirect action: 'list'
		} else {
			DownloadHelper.setResponseHeader(response, "users.csv")
			Exporter<UserExportableWrapper> exporter = CsvExporter.createExporter(UserExportableWrapper.class);
			render text: exporter.export(userExportables), contentType: "text/csv"
		}
	}
	/**
     * Show details of the selected user. By default, this action renders the "_show.gsp" template.
     * When rendering for an AJAX request the template defined by the "template" parameter will be rendered.
     */
    @Secured(["CUSTOMER_15"])
    def show () {
        def user = UserDTO.get(params.int('id'))
        if (!user) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(user)), Validator.Type.VIEW)
		//collect user related data, e.g. contact, latestPayment, revenue etc..
		def userData = retrieveUserData(user)
        def userBl = new UserBL(user.id)
		CancellationRequestWS[] cancellationRequests = webServicesSession.getCancellationRequestsByUserId(user.id)
		def isCancelled = false;
		isCancelled = cancellationRequests ? true : false
        user.accountLocked = userBl.isAccountLocked()
        user.accountExpired = userBl.validateAccountExpired(user.accountDisabledDate)

        if(!flash.isChainModel) {
            recentItemService.addRecentItem(user.userId, RecentItemType.CUSTOMER)
            breadcrumbService.addBreadcrumb(controllerName, 'list', params.template ?: null, user.userId, UserHelper.getDisplayName(user, userData.contact))
        }

        CustomerEnrollmentDTO customerEnrollment = CustomerEnrollmentDTO.findByUser(user)

        if(flash.isChainModel){
            chain controller: 'myAccount', action: 'index', params: [message: flash.message, args: flash.args],
                       model: [selected: user, customerEnrollment:customerEnrollment, customerNotes: retrieveCustomerNotes(), displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])] + userData
        } else {
            FlowHelper.display(this, true, [template: params.template ?: 'show',
                                            model   : [partial: true, selected: user, customerEnrollment:customerEnrollment, customerNotes: retrieveCustomerNotes(), displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id']),  isCancelled: isCancelled] + userData])
        }
    }

    /**
     * Fetches a list of sub-accounts for the given user id and renders the user list "_table.gsp" template.
     */
    @Secured(["CUSTOMER_18"])
    def subaccounts () {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def parent = UserDTO.get(params.int('id'))

        securityValidator.validateUserAndCompany(UserBL.getWS(new UserDTOEx(parent)), Validator.Type.VIEW)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            render template: 'customersTemplate', model:[parent: parent]
            return
        }

        def children = getChildren(params, true)

        render template: 'customersTemplate',
                  model: [     users: children,
                              parent: parent,
                           displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
    }

    /**
     * JQGrid will call this method to get the list as JSon data
     */
    @Secured(["CUSTOMER_18"])
    def findSubaccounts () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def children = getChildren(params, true)

        try {
            def jsonData = getCustomersJsonData(children, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Users to JSon
     */
    private def Object getCustomersJsonData(users, GrailsParameterMap params) {
        def jsonCells = users
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def numberOfPages = Math.ceil(users.totalCount / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: users.totalCount, total: numberOfPages]

        jsonData
    }

    /**
     * Shows all customers of the given partner id
     */
    def partner () {
        def filter = new Filter(type: FilterType.CUSTOMER, constraintType: FilterConstraint.EQ, field: 'customer.partner.id', template: 'id', visible: true, integerValue: params.id)
        filterService.setFilter(FilterType.CUSTOMER, filter)

        redirect action: 'list'
    }

    /**
     *  Add new note from customer screen
     */
    @Secured(["CUSTOMER_11"])
    @RequiresValidFormToken
     def saveCustomerNotes () {
        try {
            CustomerNoteWS customerNoteWS=new CustomerNoteWS();
            bindData(customerNoteWS,params.notes);
            webServicesSession.createCustomerNote(customerNoteWS);
            render "success"
        } catch (SessionInternalError e) {
            log.error("Could not save the customer's notes", e)
            viewUtils.resolveException(flash, session.locale, e)
        }
    }

	 /**
	  * Helper method to get the response in JSON format
	  *
	  * @param status
	  * @param errorMsg
	  * @return Response as JSON String
	  */
	 private String getJsonResponse(String status, String errorMsg){
		 def json = new JsonBuilder()
		 def root = json status: status, errorMessage: errorMsg
		 return json.toString();
	 }

	 /**
	  * Helper method to get the error messages code from properties file depending upon the type of error
	  *
	  * @param errorMsg
	  * @return Error Message from Properties file
	  */
	 private String getErrorMessages(String errorMsg){
		 def errorMessage = "";
		 if(errorMsg.contains("Pending State")){
			 errorMessage = message(code: 'cancellation.already.pending.state')
		 }
		 else if(errorMsg.contains("Last Invoice Date")){
			 errorMessage = message(code: 'cancellation.date.higher.than.last.invoice.date')
		 }
		 else if(errorMsg.contains("No Active")){
			 errorMessage = message(code: 'cancellation.no.active.subscription.order')
		 }
		 else if(errorMsg.contains("active since date")){
			 errorMessage = message(code: 'cancellation.date.lower.than.active.date')
		 }
		 else {
			 errorMessage = "Exception Occurred";
		 }
		 return errorMessage;
	 }


	 /**
	  * Get the Cancellation Request using the Cancellation ID
	  * @return cancellation object
	  */
	 @Secured(["CUSTOMER_1906"])
	 def getCustomerCancellation () {
		 try {
			 CancellationRequestWS cancellationWs = webServicesSession.getCancellationRequestById(params.int("cancellationId"));
			 render new JsonBuilder(cancellationWs).toPrettyString()
		 } catch (SessionInternalError e) {
			 log.error("Could not get the customer's cancellation", e)
			 flash.error = message(code: 'customer.cancellation.not.found')
			 viewUtils.resolveException(flash, session.locale, e)
		 }
	 }

	/**
	 *  Add new cancellation request from customer screen
	 */
	@Secured(["CUSTOMER_1906"])
	def saveCustomerCancellation () {
		try {
			CancellationRequestWS cancellationws = new CancellationRequestWS();
			bindData(cancellationws,params.cancellation);
			cancellationws.setCustomerId(params.int("cancellation_customerId"));
			webServicesSession.createCancellationRequest(cancellationws);
			render getJsonResponse("success","");
		} catch (SessionInternalError e) {
			log.error("Could not save the customer's cancellation request", e)
			render getJsonResponse("error",getErrorMessages(e.getMessage()));
		}
	}

	/**
	 * Edit the existing cancellation request from edit screen
	 * @return
	 */
	@Secured(["CUSTOMER_1906"])
	def editCustomerCancellation () {
		try {
			CancellationRequestWS cancellationWs = webServicesSession.getCancellationRequestById(params.int("cancellationId_edit"));

			// get the the date formatter for the user locale
			DateFormat format = new SimpleDateFormat(message(code: 'date.format'), session.locale);

			//set the data retrieved from the fields
			cancellationWs.setReasonText(params.get("reasonText"));
			cancellationWs.setCancellationDate(format.parse(params.get("cancellationDate_edit")));
			webServicesSession.updateCancellationRequest(cancellationWs);
			render getJsonResponse("success","");
		} catch (SessionInternalError e) {
			log.error("Could not update the customer's cancellation request", e)
			render getJsonResponse("error",getErrorMessages(e.getMessage()));
		}
	}

	/**
	 * Delete the existing cancellation request from delete screen
	 * @return
	 */
	@Secured(["CUSTOMER_1906"])
	def deleteCustomerCancellation () {
		try {
			webServicesSession.deleteCancellationRequest(params.int("cancellationId_delete"));
			render getJsonResponse("success","");
		} catch (SessionInternalError e) {
			log.error("Could not delete the customer's cancellation request", e)
			render getJsonResponse("error",getErrorMessages(e.getMessage()));
		}
	}

    /**
     * Delete the given user id.
     */
    @Secured(["CUSTOMER_12"])
    def delete () {
        if (params.id) {
            def partnerId = params.int('id')
            securityValidator.validateUserAndCompany(webServicesSession.getUserWS(partnerId), Validator.Type.EDIT)
			try {
				webServicesSession.deleteUser(partnerId)
				flash.message = 'customer.deleted'
				flash.args = [ params.id ]
				log.debug("Deleted user ${params.id}.")
			} catch (SessionInternalError e) {
				log.error("Could not delete user", e)
				viewUtils.resolveException(flash, session.locale, e)
			}

            // remove the id from the list in session.
            subAccountService.removeSubAccountUserId(params.int('id'))
        }

        // render the partial user list
        params.partial = true
        redirect action: 'list'
    }

    /**
     * Start the terminate process for user id.
     */
    @Secured(["CUSTOMER_1100"])
    def terminate () {
        def success = true
        if (params.id) {

            try {
                def dateParser = new java.text.SimpleDateFormat(message(code: 'date.format'))
                dateParser.lenient = false

                webServicesSession.initiateTermination(params.int('id'), params['reason'], dateParser.parse(params.effectiveDate))
                flash.message = 'customer.terminate'
                flash.args = [ params.id ]
                log.debug("Termination process started for ${params.id}.")
            } catch (java.text.ParseException e) {
                log.debug("Unparseable date", e);
                flash.error = message(code: 'termination.invalid.date.format')
                success = false
            } catch (SessionInternalError e) {
                log.error("Could not start termination for user", e)
                viewUtils.resolveException(flash, session.locale, e)
                success = false
            }
        }

        if(success) {
            // render the partial user list
            params.partial = true
            redirect action: 'list'
        } else {
            redirect action: 'list', id: params.id
        }
    }

    /**
     * Get the user to be edited and show the "edit.gsp" view. If no ID is given this view
     * will allow creation of a new user.
     */
    @Secured(["hasAnyRole('CUSTOMER_10', 'CUSTOMER_11', 'MY_ACCOUNT_162')"])
    def edit () {
        def accountTypes
        def accountTypeId
		def companyId
        def defaultIdp

		log.debug("params.accountTypeId:########## "+session["company_id"])
        accountTypeId = params.accountTypeId && params.accountTypeId?.isInteger() ?
            params.int('accountTypeId') : null

		companyId = params.int('user.entityId') == null ||
					params.int('user.entityId') == 'null' ?
						session['company_id'] : params.int('user.entityId')

        if(!accountTypeId && !params.id){
            accountTypes = AccountTypeDTO.createCriteria().list() {
                eq('company.id', companyId)
                order('id', 'asc')
            };

            //if this is request for new customer creation then check if there
            //are available account types. If not abort customer creation
            if (!params.id && (!accountTypes || accountTypes.size == 0)) {
                flash.error = message(code: 'customer.account.types.not.available')
                redirect controller: 'customer', action: 'list'
                return
            }
        }

        def user
        def parent

        try {
            user = params.id ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
            if (params.id?.isInteger()) {
                securityValidator.validateUserAndCompany(user, Validator.Type.EDIT, true)

                if(user?.deleted==1) {
                    log.error("Customer not found or deleted, redirect to list.")
                    customerNotFoundErrorRedirect(params.id)
                    return
                }
            }

            if(user.id > 0){
                MetaFieldValueWS[] metaFieldValueWS = user.getMetaFields();
                for (int i = 0; i < metaFieldValueWS.length; i++) {
                    if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_IDP_ID_CUSTOMER)) {
                        defaultIdp = metaFieldValueWS[i].getIntegerValue()
                        break;
                    }
                }
            }

            parent = params.parentId ? webServicesSession.getUserWS(params.int('parentId')) : null
        } catch (SessionInternalError e) {
            log.error("Could not fetch WS object", e)
			customerNotFoundErrorRedirect(params.id)
            return
        }

        def crumbName = params.id ? 'update' : 'create'
        def crumbDescription = params.id ? UserHelper.getDisplayName(user, user.contact) : null
        if(!flash.isChainModel){
            breadcrumbService.addBreadcrumb(controllerName, actionName, crumbName, params.int('id'), crumbDescription)
        }

        if(user.userId || accountTypeId) {

            //if existing user then set the account type of that user
            accountTypeId = user.userId ? user.accountTypeId : accountTypeId
			companyId = user.userId ? (user.entityId ? user.entityId : UserDTO.get(user.userId)?.company?.id) : companyId
            def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) :
                                accountTypes?.size() > 0 ? accountTypes.get(0) : null

            def periodUnits = PeriodUnitDTO.list()
            def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', CompanyDTO.get(companyId)) }
			def templateName = Constants.TEMPLATE_MONTHLY

            if(!user.userId || 0 == user.userId){
                initUserDefaultData(user, accountType)
            }

            def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }

			// set dates map, effective dates map
			Map<Integer, ArrayList<Date>> dates;
			Map<Integer, Date> effectiveDatesMap;
			Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = new HashMap<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>>();

			if(user.userId) {
				dates = user.timelineDatesMap;
				effectiveDatesMap = user.effectiveDateMap;
				accountInfoTypeFieldsMap = user.accountInfoTypeFieldsMap;

				// if a new info type was added after creating a user it should be
				// added to maps
				for(def accountInfoType : infoTypes) {
					if(!dates.containsKey(accountInfoType.id)) {
						ArrayList<Date> date = new ArrayList<Date>()
						date.add(CommonConstants.EPOCH_DATE)

						dates.put(accountInfoType.id, date)
						effectiveDatesMap.put(accountInfoType.id, CommonConstants.EPOCH_DATE)
					}
				}
			} else {
				dates = new HashMap<Integer, ArrayList<Date>>()
				effectiveDatesMap = new HashMap<Integer, Date>()
				for(def accountInfoType : infoTypes) {
					ArrayList<Date> date = new ArrayList<Date>()
					date.add(CommonConstants.EPOCH_DATE)
					dates.put(accountInfoType.id, date)

					effectiveDatesMap.put(accountInfoType.id, CommonConstants.EPOCH_DATE)
				}
			}

            // #7043 - Agents && Commissions - If Partner or Sub-Partner creates a customer it has to be automatically linked to the
            // Partner or Parent Partner.
            UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)
            Integer partnerId
            def partnerIds = []
            if (loggedInUser.getPartner() != null) {
                partnerId = loggedInUser.partner.id
                partnerIds << loggedInUser.partner.user.id
                if (loggedInUser.partner.children) {
                    partnerIds += loggedInUser.partner.children.user.id
                }
            }

            def userCompany = CompanyDTO.get(companyId)
            def companyInfoTypes = userCompany.getCompanyInformationTypes();

            // show only recurring payment methods
            def List<PaymentMethodTypeDTO> paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)

            //get all the logged in user's user codes. If the user is a Partner then we have to show the User Codes for the Partner and Sub-Partners if it corresponds.
            def userCodes = []
            if (partnerIds) {
                userCodes = new UserCodeDAS().findActiveForPartner(partnerIds).collect { it.identifier }
            } else {
                userCodes = new UserCodeDAS().findActiveForUser(session['user_id'] as int).collect { it.identifier }
            }

			/*
			 * This code use to render the period value template(i.e monthly) as per user main sunscription period unit in case of edit.
			 */
			if (null != user) {
				OrderPeriodWS orderPeriodWs = webServicesSession.getOrderPeriodWS(user.mainSubscription.periodId);
				if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
					templateName = Constants.TEMPLATE_WEEKLY
					user.mainSubscription.weekDaysMap = MainSubscriptionWS.weekDaysMap;
				} else if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_DAY) == 0) {
					templateName = Constants.TEMPLATE_DAILY
				} else if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
					templateName = Constants.TEMPLATE_YEARLY
					user.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
					user.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
					GregorianCalendar calendarInstance = new GregorianCalendar();
					calendarInstance.set(Calendar.DAY_OF_YEAR, user.mainSubscription.nextInvoiceDayOfPeriod);
					calendarInstance.getTime();
					user.mainSubscription.nextInvoiceDayOfPeriod = (calendarInstance.get(Calendar.MONTH) + 1)
					user.mainSubscription.nextInvoiceDayOfPeriodOfYear = (calendarInstance.get(Calendar.DAY_OF_MONTH))
				} else if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
					templateName = Constants.TEMPLATE_SEMI_MONTHLY
					user.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
				} else {
					user.mainSubscription.monthDays = MainSubscriptionWS.monthDays;
				}
			}

			/*
			 * When user want to use billing cycle period that time populate on hidden feild at gsp level
			 * set Main Subscription period as per Billing Configuration Next Run date and billing period
			 * in orderPeriodSubscriptionUnit hidden field.
			 */
			BillingProcessConfigurationWS billingProcessConfiguration = webServicesSession.getBillingProcessConfiguration();
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(billingProcessConfiguration.nextRunDate);
            def orderPeriodSubscriptionUnit = orderPeriods.findResult { (it.periodUnit.id == billingProcessConfiguration.periodUnitId
                                                                         && it.value == 1) ? it : null }?.id

            def commissionDefinitions = bindCommissions(user.commissionDefinitions)
            MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
			if (null != parent && parent.isParent) {
				mainSubscription = parent.getMainSubscription();
                user.mainSubscription.periodId = mainSubscription.periodId
				OrderPeriodWS orderPeriodWs = webServicesSession.getOrderPeriodWS(parent.mainSubscription.periodId);
				if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
					templateName = Constants.TEMPLATE_WEEKLY
				} else if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_DAY) == 0) {
					templateName = Constants.TEMPLATE_DAILY
				} else if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
					templateName = Constants.TEMPLATE_YEARLY
					GregorianCalendar calendarInstance = new GregorianCalendar();
				} else if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
					templateName = Constants.TEMPLATE_SEMI_MONTHLY
				}
			} else {
				mainSubscription.periodId = Constants.PERIOD_UNIT_MONTH
				mainSubscription.nextInvoiceDayOfPeriod = 1;
				mainSubscription.monthDays = MainSubscriptionWS.monthDays;
			}
			setFraction(user);
            def ssoActive = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_SSO) as int
            modelToPass = [ user: user,
                            parent: parent,
                            company: userCompany,
                            currencies: retrieveCurrenciesByCompanyId(companyId),
                            periodUnits: periodUnits,
                            orderPeriods: orderPeriods,
                            availableFields: retrieveAvailableMetaFieldsByCompanyId(companyId),
                            mainSubscription: mainSubscription,
                            orderPeriodSubscriptionUnit: orderPeriodSubscriptionUnit,
                            periodUnitCompany : periodUnits.find { it.id == billingProcessConfiguration.periodUnitId }?.getDescription(session['language_id']),
                            templateName: templateName,
                            accountType: accountType,
                            accountInformationTypes: infoTypes,
                            pricingDates : dates,
                            effectiveDates : effectiveDatesMap,
                            datesXml : map2xml(dates),
                            effectiveDatesXml : map2xml(effectiveDatesMap),
                            infoFieldsMapXml : map2xml(accountInfoTypeFieldsMap),
                            removedDatesXml : map2xml(new HashMap<Integer, List<Date>>()),
                            customerNotes:retrieveCustomerNotes(),
                            userCodes: userCodes,
                            paymentMethods : paymentMethods,
                            partnerId: partnerId,
                            loggedInUser: loggedInUser,
                            commissionDefinitions:commissionDefinitions,
                            companyInfoTypes:companyInfoTypes,
                            ssoActive: ssoActive,
                            defaultIdp:defaultIdp,
                            commissionDefinitions:commissionDefinitions,
                            displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]

            if(flash.isChainModel){
                chain controller: 'myAccount', action: 'edit', model: modelToPass
            } else {
                render view: "edit", model: modelToPass
            }
        } else {
            render view: 'list', model: [
                    accountTypes: accountTypes,
                    parentId: parent?.id,
					companies: retrieveCompanies()
            ]
        }
    }

	private void setFraction(UserWS user){
		PaymentInformationDTO converted = null
		PaymentInformationBL piBl = new PaymentInformationBL();
		for(PaymentInformationWS info: user.paymentInstruments){
			converted = new PaymentInformationDTO(info, session['company_id'])
			if(piBl.isCreditCard(converted)|| piBl.isACH(converted)){
				def metaName = null;
				for(MetaFieldValue value: converted.metaFields){
					if(MetaFieldHelper.isValueOfType(value, MetaFieldType.AUTO_PAYMENT_LIMIT)){
						metaName = value.getField().getName();
						break;
					}
				}
				if(metaName!=null){
					MetaFieldValueWS[] temp = new MetaFieldValueWS[info.metaFields.size()];
					int i =0;
					for(MetaFieldValueWS ws: info.metaFields){
						if(metaName.equals(ws.getFieldName()) && null != ws.getDecimalValue()){
							String substr = ws.getDecimalValue().substring(0, ws.getDecimalValue().length()-8);
							ws.setDecimalValue(substr);
						}
						temp[i++] = ws;
					}
					info.setMetaFields(temp);
				}
			}
		}
	}
	/**
	 * gets account types for the given company
	 */
	def getAccountTypes (){
		UserWS user = new UserWS()
		UserHelper.bindUser(user, params)
		def accountTypes = AccountTypeDTO.createCriteria().list() {
			eq('company.id', user.entityId)
			order('id', 'asc')
		};

		render template : 'accountTypeDropDown',
			   model : [accountTypes : accountTypes]
	}

	private void customerNotFoundErrorRedirect(customerId) {
		flash.error = 'customer.not.found'
		flash.args = [ customerId as String ]
		redirect controller: 'customer', action: 'list'
	}

    /**
     * Validate and save a user.
     */
    @Secured(["hasAnyRole('CUSTOMER_10', 'CUSTOMER_11', 'MY_ACCOUNT_162')"])
    @RequiresValidFormToken
    def save () {
        forward action: 'saveCustomer', params: params
    }

    @Secured(["hasAnyRole('CUSTOMER_10', 'CUSTOMER_11', 'MY_ACCOUNT_162')"])
    def saveCustomer (){
        UserWS user = new UserWS()
        def defaultIdp;
        def templateName = Constants.TEMPLATE_MONTHLY
        def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', retrieveCompany()) }
        /*
         * Calculate nextInvoiceDayOfPeriod in case of yearly period unit.
         */

        OrderPeriodWS orderPeriodWs = webServicesSession.getOrderPeriodWS(params['mainSubscription.periodId']?.toInteger());
        if (orderPeriodWs.periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
            Integer nextInvoiceDayOfPeriod = 1;
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.set(Calendar.MONTH, params.mainSubscription.nextInvoiceDayOfPeriod.toInteger() - 1);
            calendar.set(Calendar.DAY_OF_MONTH, params.mainSubscription.nextInvoiceDayOfPeriodOfYear.toInteger());
            calendar.getTime();
            nextInvoiceDayOfPeriod = calendar.get(Calendar.DAY_OF_YEAR);
            params.mainSubscription.nextInvoiceDayOfPeriod = nextInvoiceDayOfPeriod;
        }

        OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.mainSubscription.periodId.toInteger());
        if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_MONTH) == 0) {
            templateName = Constants.TEMPLATE_MONTHLY
            params.mainSubscription.monthDays = MainSubscriptionWS.monthDays;
        } else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
            templateName = Constants.TEMPLATE_WEEKLY
            params.mainSubscription.weekDaysMap = MainSubscriptionWS.weekDaysMap;
        } else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_DAY) == 0) {
            templateName = Constants.TEMPLATE_DAILY
            params.mainSubscription.nextInvoiceDayOfPeriod = new Integer(1);
        } else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
            templateName = Constants.TEMPLATE_YEARLY
            params.mainSubscription.yearMonthsMap = MainSubscriptionWS.yearMonthsMap;
            params.mainSubscription.yearMonthDays = MainSubscriptionWS.yearMonthDays;
        } else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
            templateName = Constants.TEMPLATE_SEMI_MONTHLY
            params.mainSubscription.semiMonthlyDaysMap = MainSubscriptionWS.semiMonthlyDaysMap;
        }

        /*
          * when user 'use company billing cycle' if any validation error occur set
          * Main Subscription period as per Billing Configuration
          * Next Run date and billing period in orderPeriodSubscriptionUnit hidden field.
          */
        BillingProcessConfigurationWS billingProcessConfiguration = webServicesSession.getBillingProcessConfiguration();
        GregorianCalendar cal = new GregorianCalendar();
        def orderPeriodSubscriptionUnit = null;
        cal.setTime(billingProcessConfiguration.nextRunDate);
        for (OrderPeriodDTO orderPeriodDto : orderPeriods) {
            if (orderPeriodDto.periodUnit.id == billingProcessConfiguration.periodUnitId) {
                orderPeriodSubscriptionUnit = orderPeriodDto.id
                break;
            }
        }

        try {
            def existingId = params.existingId as Integer
            UserHelper.bindUser(user, params)
            user.setId(existingId)
        } catch (NumberFormatException e) {
            flash.error = 'validation.error.invalid.agentid'
            flash.errorMessages = null
        } catch (SessionInternalError sie) {
            flash.error = 'validation.error.unexist.agentid'
            flash.errorMessages = null
        }

        UserHelper.bindMetaFields(user, retrieveAvailableMetaFieldsByCompanyId(user.entityId), params)
        UserHelper.bindMetaFields(user, retrieveAvailableAitMetaFields(user.accountTypeId), params)

        try {
            UserHelper.bindPaymentInformations(user, params.int("modelIndex"), params)
        } catch (SessionInternalError sie) {
            viewUtils.resolveException(flash, session.locale, sie)
            flash.error = flash.errorMessages[0]
            flash.errorMessages = null
        }

        List<CustomerNoteWS> customerNotesWS = []
        if (params.newNotesTotal) {
            for (int i = 0; i < params.newNotesTotal.toInteger(); i++) {
                customerNotesWS.add(bindData(new CustomerNoteWS(), params.notes."${i}") as CustomerNoteWS)
            }
        }
        user.setCustomerNotes(customerNotesWS.toArray(new CustomerNoteWS[customerNotesWS.size()]))

        // convert xml to maps
        def timelineDates = params.datesXml
        def effectiveDates = params.effectiveDatesXml
        def removedDates = params.removedDatesXml
        def infoFieldsXml = params.infoFieldsMapXml

        user.timelineDatesMap = xml2map(timelineDates)
        user.effectiveDateMap = xml2map(effectiveDates)
        user.removedDatesMap = xml2map(removedDates)

        def oldUser = (user.userId && user.userId != 0) ? webServicesSession.getUserWS(user.userId) : null

        if (oldUser) {
            securityValidator.validateUserAndCompany(oldUser, Validator.Type.VIEW)

            MetaFieldValueWS[] metaFieldValueWS = user.getMetaFields();
            for (int i = 0; i < metaFieldValueWS.length; i++) {
                if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_IDP_ID_CUSTOMER)) {
                    defaultIdp = metaFieldValueWS[i].getIntegerValue()
                    break;
                }
            }
        }
        UserHelper.bindPassword(user, oldUser, params, flash)

        def periodUnits = PeriodUnitDTO.list()

        def accountTypeId = user?.accountTypeId
        def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) : null;

        // if child company was selected assign that company else assing parent company
        def companyId = user.entityId
        def company = CompanyDTO.get(companyId)
        def companyInfoTypes = company.getCompanyInformationTypes();
        def ssoActive = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_SSO) as int
        def commissionDefinitions = bindCommissions(user.commissionDefinitions)

        def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }
        modelToPass = [                        user : user,
                                            company : retrieveCompany(),
                                    availableFields : retrieveAvailableMetaFields(),
                                        periodUnits : periodUnits,
                                       orderPeriods : orderPeriods,
                                         currencies : retrieveCurrencies(),
                              commissionDefinitions : commissionDefinitions,
                                       templateName : templateName,
                        orderPeriodSubscriptionUnit : orderPeriodSubscriptionUnit,
                                       pricingDates : xml2map(timelineDates),
                                     effectiveDates : xml2map(effectiveDates),
                                           datesXml : timelineDates,
                                  effectiveDatesXml : effectiveDates,
                                   infoFieldsMapXml : infoFieldsXml,
                                    removedDatesXml : removedDates,
                                     paymentMethods : getRecurringPaymentMethods(accountType?.paymentMethodTypes),
                                   companyInfoTypes : companyInfoTypes,
                                          ssoActive : ssoActive,
                                         defaultIdp : defaultIdp]

        MetaFieldValueWS[] metaFieldValueWS = user.getMetaFields();
        boolean ssoEnabled = false;
        for (int i = 0; i < metaFieldValueWS.length; i++) {
            if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_ENABLED_CUSTOMER)) {
                ssoEnabled = metaFieldValueWS[i].getBooleanValue()
                break;
            }
        }

        if(ssoEnabled){
            user.setCreateCredentials(false);
        }

        if(!params['idpConfigurationIds']?.toString()?.trim()?.isEmpty() && ssoEnabled) {
            for (int i = 0; i < metaFieldValueWS.length; i++) {
                if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(Constants.SSO_IDP_ID_CUSTOMER)) {
                    int metaValue = params['idpConfigurationIds'] as Integer
                    metaFieldValueWS[i].setIntegerValue(metaValue)
                    break;
                }
            }
            user.setMetaFields(metaFieldValueWS)
        }

        if ((params['idpConfigurationIds']?.toString()?.trim()?.isEmpty() || companyInfoTypes.size() == 0) && ssoEnabled) {
            log.error("No default Idp is configured for company : " + companyId)
            flash.error = message(code: 'default.idp.error')
        }

        if (flash.error) {
            if(flash.isChainModel) {
                chain controller: 'myAccount', action: 'edit', model: modelToPass + loadModelForSaveError(user, user.entityId)
            } else {
                render view: 'edit', model: modelToPass + loadModelForSaveError(user, user.entityId)
            }

            return
        }

        try {

            // save or update
            if (!oldUser) {
                if (SpringSecurityUtils.ifAllGranted("CUSTOMER_10") || SpringSecurityUtils.ifAllGranted("MY_ACCOUNT_162")) {
                    if (user?.userName.trim() && Pattern.compile("^[a-zA-Z0-9.+_-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$").matcher(user?.userName).matches()) {
                        user = webServicesSession.createUserWithCIMProfileValidation(user);
                        /*if(accountType?.paymentMethodTypes?.size() == 0) {
                            flash.info= "customer.accountType.paymentMethod.associated.info"
                        }*/
                        //						def lastPayment = webServicesSession.getLatestPayment(user.userId)
                        if (user?.cimProfileError == CommonConstants.CIM_PROFILE_ERROR) {
                            flash.message = 'customer.created.error.in.cim.profile'
                        } else if (user?.cimProfileError == CommonConstants.CIM_PROFILE_BILLING_INFO_ERROR) {
                            flash.message = 'customer.created.error.in.cim.billing.information'
                        } else {
                            flash.message = 'customer.created'
                        }
                        flash.args = [user.userId as String]

                        // add the id to the list in session.
                        subAccountService.addSubAccountUserId(user)
                    } else {
                        if (StringUtils.isBlank(user?.userName)) {
                            flash.error = message(code: 'customer.error.name.blank')
                        } else {
                            flash.error = message(code: 'loginName.email.validation')
                        }
                        user.userName = ''
                        if(flash.isChainModel) {
                            chain controller: 'myAccount', action: 'edit', model: modelToPass + loadModelForSaveError(user, companyId)
                        } else {
                            render view: "edit", model: modelToPass + loadModelForSaveError(user, companyId)
                        }
                        return
                    }
                } else {
                    render view: '/login/denied'
                    return
                }

            } else {
                if (SpringSecurityUtils.ifAllGranted("CUSTOMER_11") || SpringSecurityUtils.ifAllGranted("MY_ACCOUNT_162")) {
                    user = webServicesSession.updateUserWithCIMProfileValidation(user)
                    if (user?.cimProfileError == CommonConstants.CIM_PROFILE_ERROR) {
                        flash.message = 'customer.updated.error.in.cim.profile'
                    } else if (user?.cimProfileError == CommonConstants.CIM_PROFILE_BILLING_INFO_ERROR) {
                        flash.message = 'customer.updated.error.in.cim.billing.information'
                    } else {
                        flash.message = 'customer.updated'
                    }
                    flash.args = [user.userId as String]
                } else {
                    render view: '/login/denied'
                    return
                }
            }

        } catch (SessionInternalError e) {
            flash.isChainModel ?: flash.clear()
            viewUtils.resolveException(flash, session.locale, e)

            if(flash.isChainModel) {
                chain controller: 'myAccount', action: 'edit', model: modelToPass + loadModelForSaveError(user, companyId)
            } else {
                render view: "edit", model: modelToPass + loadModelForSaveError(user, companyId)
            }
            return
        } finally {
             /* commented this method call as it's causing credit card details removed from the customer account.
              * This issue is occurring rarely for 1 or 2 records in a day during save and update operation */

            // cleanPaymentsInformation(user?.getPaymentInstruments());
        }

        if(flash.isChainModel) {
            chain action: 'show', id: user.userId
        } else {
            chain action: 'list', params: [id: user.userId, customerNotes: retrieveCustomerNotes()]
        }
    }

    private Map loadModelForSaveError(user, companyId) {
        def periodUnits = PeriodUnitDTO.list()
        def orderPeriods = OrderPeriodDTO.createCriteria().list(){eq('company', CompanyDTO.get(user.entityId))}

        UserDTO loggedInUser = UserDTO.get(springSecurityService.principal.id)

        def accountTypeId = user?.accountTypeId
        def accountType = accountTypeId ? AccountTypeDTO.get(accountTypeId) : null;

        def infoTypes = accountType?.informationTypes?.sort { it.displayOrder }

        //get all the logged in user's user codes.
        def userCodes = new UserCodeDAS().findActiveForUser(session['user_id'] as int).collect { it.identifier }

        [
                periodUnits: periodUnits,
                orderPeriods: orderPeriods,
                accountType: accountType,
                accountInformationTypes: infoTypes,
                company: CompanyDTO.get(companyId),
                currencies: retrieveCurrenciesByCompanyId(companyId),
                availableFields: retrieveAvailableMetaFieldsByCompanyId(companyId),
                loggedInUser: loggedInUser,
                userCodes : userCodes
        ]
    }

	def addDate (){
		Integer aitId = params.aitId as Integer

		Map<Integer, ArrayList<Date>> dates = xml2map(params.dates)

		def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.date)
		ArrayList<Date> datesForGivenAit = dates.get(aitId)
		if(!datesForGivenAit.contains(startDate)) {
			datesForGivenAit.add(startDate)
		}
		datesForGivenAit.sort()
		dates.put(aitId, datesForGivenAit)

		render template: '/customer/timeline', model: [startDate : startDate , pricingDates : dates, aitVal : aitId, isNew : params.isNew]
	}

	def editDate (){
		Date startDate
		try {
			startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)
		} catch(Exception e) {
			Map<Integer, ArrayList<Date>> datesMap = xml2map(params.dates)
			ArrayList<Date> aitDates = datesMap.get(params.aitId as Integer)

			startDate = findEffectiveDate(aitDates)
		}

		def accountInfoType = AccountInformationTypeDTO.get(params.aitId)
		Map<Integer, Map<Date, ArrayList<MetaFieldValueWS>>> accountInfoTypeFieldsMap = xml2map(params.values)
		Map<Date, ArrayList<MetaFieldValueWS>> valuesByDate = accountInfoTypeFieldsMap.get(accountInfoType.id)
		ArrayList<MetaFieldValueWS> values
		if(valuesByDate != null) {
			if(valuesByDate.containsKey(startDate)) {
				// if valeus are present for the date then paint those values
				values = valuesByDate.get(startDate)
			} else {
				// if values are not present then paint the latest ones
				for(Map.Entry<Date, ArrayList<MetaFieldValueWS>> entry : valuesByDate.entrySet()) {
					values = entry.getValue()
				}
			}
		}

        render template: '/customer/aITMetaFields', model: [ait : accountInfoType, values : values, aitVal : accountInfoType.id]
	}

	def updateDatesXml (){
		Integer aitId = params.aitId as Integer
		def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.date)
		Map<Integer, ArrayList<Date>> datesMap = xml2map(params.dates)

		ArrayList<Date> dates = datesMap.get(aitId)
		String converted
		if(!dates.contains(startDate)) {
			dates.add(startDate)
			dates.sort()
			datesMap.put(aitId, dates)
			converted = map2xml(datesMap)
		} else {
			converted = params.dates
		}
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
        return
	}

	def refreshTimeLine (){
		Integer aitId = params.aitId as Integer
		Map<Integer, ArrayList<Date>> datesMap = xml2map(params.values)
		ArrayList<Date> aitDates = datesMap.get(aitId)

		def startDate
		try {
			startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)
		} catch(Exception e) {
			startDate = findEffectiveDate(aitDates)
		}

		render template: '/customer/timeline', model: [startDate : startDate , pricingDates : datesMap, aitVal : aitId, isNew : params.isNew]

	}

	def updateEffectiveDateXml (){
		Map<Integer, Date> dates = xml2map(params.values)

		try {
			dates.put(params.aitId as Integer, TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate))
		} catch(Exception e) {
			Map<Integer, ArrayList<Date>> timelineDates = xml2map(params.dates)
			ArrayList<Date> aitDates = timelineDates.get(params.aitId as Integer)

			dates.put(params.aitId as Integer, findEffectiveDate(aitDates))
		}

		String converted = map2xml(dates)
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
        return
	}

	def updateTimeLineDatesXml (){
		Map<Integer, ArrayList<Date>> dates = xml2map(params.dates)
		dates.get(params.aitId as Integer).remove(TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate))
		String converted = map2xml(dates)
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
        return
	}

	def updateRemovedDatesXml (){
		Integer aitId = params.aitId as Integer
		def startDate = TimezoneHelper.currentDateForTimezone(session['company_timezone']).parse(message(code: 'date.format'), params.startDate)
		Map<Integer, ArrayList<Date>> dates = xml2map(params.removedDates)
		if(dates.containsKey(aitId)) {
			dates.get(aitId).add(startDate)
		} else {
			ArrayList<Date> date = new ArrayList<Date>()
			date.add(startDate)

			dates.put(aitId, date)
		}
		String converted = map2xml(dates)
		render (text: converted, contentType: "text/xml", encoding: "UTF-8")
        return
	}

	def addPaymentInstrument (){
		UserWS user = new UserWS()
		UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)

		def accountType = AccountTypeDTO.get(params.int("accountTypeId"))
		// show only recurring payment methods
		def List<PaymentMethodTypeDTO> paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)
		// add a new payment instrument
		PaymentInformationWS paymentInstrument = new PaymentInformationWS()
            paymentInstrument.setPaymentMethodTypeId(paymentMethods?.iterator().next().id)

            user.paymentInstruments.add(paymentInstrument)

		render template: '/customer/paymentMethods', model: [paymentMethods : paymentMethods , paymentInstruments : user.paymentInstruments , accountTypeId : accountType?.id]
	}

    def refreshPaymentInstrument() {
        int currentIndex = params.int("currentIndex")

        def user = params.int("id") && params.int("id")>0 ? webServicesSession.getUserWS(params.int('id')) : new UserWS()
        bindData(params, user.paymentInstruments)
        List<PaymentInformationWS> paymentInstruments = user.paymentInstruments
        UserHelper.bindPaymentInformations(user, params.int("modelIndex"), params)

        def accountType = AccountTypeDTO.get(params.int("accountTypeId"))
        def paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)

		if (user.paymentInstruments.size() > 0) {
			paymentInstruments.eachWithIndex { PaymentInformationWS paymentInformationWS, int index ->
                if (paymentInformationWS.paymentMethodTypeId.equals(user.paymentInstruments.get(index).paymentMethodTypeId)) {
                    user.paymentInstruments.set(index, paymentInformationWS)
                }
			}
		}
        render template: '/customer/paymentMethods', model: [paymentMethods: paymentMethods, paymentInstruments: user.paymentInstruments, accountTypeId: accountType?.id, user: user]
    }

	def removePaymentInstrument (){
		def currentIndex = params.int("currentIndex")

		UserWS user = new UserWS()
		UserHelper.bindPaymentInformations(user ,params.int("modelIndex"), params)

		def accountType = AccountTypeDTO.get(params.int("accountTypeId"))
		def paymentMethods = getRecurringPaymentMethods(accountType?.paymentMethodTypes)

		PaymentInformationWS removed = user.paymentInstruments.remove(currentIndex)
		log.debug("user instrument is: " + user.paymentInstruments)
		// if this was saved in database then we need to remove it from database as well
		if(removed.id != null && removed.id != 0) {
			boolean isRemoved = webServicesSession.removePaymentInstrument(removed.id)
		}
        removed.close()

		render template: '/customer/paymentMethods', model: [paymentMethods : paymentMethods , paymentInstruments : user.paymentInstruments , accountTypeId : accountType?.id]
	}

    def retrieveCurrencies() {
        def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), session['company_id'].toInteger())
        return currencies.findAll { it.inUse }
    }

	def retrieveCompany() {
		CompanyDTO.get(session['company_id'])
	}

	def retrieveCurrenciesByCompanyId(Integer entityId) {
		def currencies = new CurrencyBL().getCurrencies(session['language_id'].toInteger(), entityId)
		return currencies.findAll { it.inUse }
	}

	def retrieveCompanies(){
		def parentCompany = CompanyDTO.get(session['company_id'])
		def childs = CompanyDTO.findAllByParent(parentCompany)
		childs.add(parentCompany)
		return childs;
	}

	def retrieveCompaniesIds() {
		def ids = new ArrayList<Integer>(0);

		for(def child : retrieveCompanies()) {
			ids.add(child.id)
		}
		return ids
	}

    def retrieveAvailableMetaFields() {
        return MetaFieldBL.getAvailableFieldsList(session["company_id"], EntityType.CUSTOMER);
    }

	def retrieveAvailableMetaFieldsByCompanyId(Integer entityId) {
		return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.CUSTOMER);
	}

	def retrieveAvailableAccounTypeMetaFields() {
		return MetaFieldBL.getAllAvailableFieldsList(session["company_id"], EntityType.ACCOUNT_TYPE);
	}

    def retrieveAvailableAitMetaFields(Integer accountType){
        return MetaFieldExternalHelper.getAvailableAccountTypeFieldsMap(accountType)
    }

    def findMetaFieldsByName(Integer metaFieldId) {
        MetaField searchField = MetaField.get(metaFieldId)
        if(searchField!=null) return searchField;
        return null;
    }

    @Secured(["CUSTOMER_15"])
    def history (){
        def user = UserDTO.get(params.int('id'))

        def currentUser = auditBL.getColumnValues(user)
        def userVersions = auditBL.get(UserDTO.class, user.getAuditKey(user.userId), versions.max)

        def customer = user.customer
        def currentCustomer = auditBL.getColumnValues(customer)
        def customerVersions = auditBL.get(CustomerDTO.class, customer.getAuditKey(customer.id), versions.max);

        def records = [
                [ name: 'user', id: user.userId, current: currentUser, versions:  userVersions ],
                [ name: 'customer', id: customer?.id, current: currentCustomer, versions:  customerVersions ]
        ]

        render view: '/audit/history', model: [ records: records, historyid: user.userId ]
    }

    def restore (){
        switch (params.record) {
            case "user":
                def user = UserDTO.get(params.int('id'));
                auditBL.restore(user, user.userId, params.long('timestamp'))
                break;

            case "contact":
                def contact = ContactDTO.get(params.int('id'));
                auditBL.restore(contact, contact.id, params.long('timestamp'))
                break;

            case "customer":
                def customer = CustomerDTO.get(params.int('id'));
                auditBL.restore(customer, customer.id, params.long('timestamp'))
                break;
        }

        chain action: 'history', params: [id: params.historyid ]
    }

	def retrieveUserData(def user) {

        if (null == user) return [:]

        def revenue = webServicesSession.getTotalRevenueByUser(user.userId)
        def latestOrder = webServicesSession.getLatestOrder(user.userId)
        def latestPayment = webServicesSession.getLatestPayment(user.userId)
        def latestInvoice = webServicesSession.getLatestInvoice(user.userId)
        def customerAssets = webServicesSession.getAssetsByUserId(user.userId)
        def cancellationRequests = webServicesSession.getCancellationRequestsByUserId(user.userId)


        def enableTotalOwnedPayment = false
        def isCurrentCompanyOwning = user.company?.id?.equals(session['company_id']) ? true : false

        ConfigurationBL config = new ConfigurationBL(user.getCompany()?.getId());
        if (config?.getEntity()?.getAutoPaymentApplication() == 1 && UserBL.getBalance(user.userId) > 0 && user.deleted == 0) {
            enableTotalOwnedPayment = true
        }

        // get all meta fileds, standard + ait timeline
        List<MetaFieldValue> values = new ArrayList<MetaFieldValue>()
        if (user?.customer?.metaFields) {
            values.addAll(user?.customer?.metaFields)
        }

		new UserBL().getCustomerEffectiveAitMetaFieldValues(values, user?.customer?.getAitTimelineMetaFieldsMap(), user.getCompany()?.id)

        //find all the user codes linked to this customer
        def userCodes = user?.customer? new UserCodeDAS().findLinkedIdentifiers(UserCodeObjectType.CUSTOMER, user.customer.id) : null

		return [ revenue:revenue,
                latestOrder:latestOrder,
                latestPayment:latestPayment,
                latestInvoice:latestInvoice,
				enableTotalOwnedPayment:enableTotalOwnedPayment,
                isCurrentCompanyOwning:isCurrentCompanyOwning,
				metaFields : values,
                userCodes : userCodes,
				customerAssets: customerAssets,
				cancellationRequests : cancellationRequests
        ]
    }

    def initUserDefaultData(def user, def accountType){

        //default data from account type
        user.mainSubscription = UserBL.convertMainSubscriptionToWS(accountType?.billingCycle)
        user.invoiceDesign = accountType?.invoiceDesign
        user.invoiceTemplateId = accountType?.invoiceTemplate?.id
        user.creditLimitAsDecimal = accountType?.creditLimit
        user.currencyId = accountType?.currencyId
        user.languageId = accountType?.languageId
        user.invoiceDeliveryMethodId = accountType?.invoiceDeliveryMethod?.id
    }

	def map2xml(map) {
		XStream converter = new XStream(new DomDriver())
		return converter.toXML(map)
	}

	/**
	 * Convert an xml to map
	 *
	 * @param xmlValue	:	map in form of xml
	 * @return			: 	xml in form of map
	 */
	def xml2map(String xmlValue) {
		XStream converter = new XStream(new DomDriver())
		return converter.fromXML(xmlValue)
	}

	/**
	 * Gets a list of dates and return currently effective date
	 *
	 * @param dates	:	list of dates
	 * @return		:	currently effective date
	 */
	def findEffectiveDate(dates) {
		Date date = TimezoneHelper.currentDateForTimezone(session['company_timezone']);
		Date forDate = null;
		for (Date start : dates) {
			if (start != null && start.after(date))
				break;

			forDate = start;
		}
		return forDate;
	}

	/**
	 * Return only those payment method types that are recurring.
	 *
	 * @param paymentMethodTypes	list of payment method types
	 * @return	recurring payment methods
	 */
	def getRecurringPaymentMethods(paymentMethodTypes) {
		def List<PaymentMethodTypeDTO> paymentMethods = new ArrayList<PaymentMethodTypeDTO>();
		for(PaymentMethodTypeDTO dto : paymentMethodTypes) {
			if(dto.isRecurring) {
				paymentMethods.add(dto)
			}
		}
		return paymentMethods
	}


	/*
	 * Ajax function to render main subscription value template as per selected Main Subscription unit.
	 */
	def updateSubscription () {
		OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.mainSubscription.periodId.toInteger());
		params.mainSubscription.nextInvoiceDayOfPeriod = null;
		def templateName = Constants.TEMPLATE_MONTHLY;
		if (orderPeriod.periodUnitId .compareTo(Constants.PERIOD_UNIT_MONTH) == 0) {
			templateName = Constants.TEMPLATE_MONTHLY
		  	params.mainSubscription.monthDays= MainSubscriptionWS.monthDays;
		} else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
			templateName = Constants.TEMPLATE_WEEKLY
			params.mainSubscription.weekDaysMap= MainSubscriptionWS.weekDaysMap;
		} else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_DAY) == 0) {
			templateName = Constants.TEMPLATE_DAILY
		  	params.mainSubscription.nextInvoiceDayOfPeriod = new Integer(1);
		} else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
			templateName = Constants.TEMPLATE_YEARLY
			params.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
			params.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
		} else if (orderPeriod.periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
			templateName = Constants.TEMPLATE_SEMI_MONTHLY
		  	params.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
		}

			render template: '/customer/subscription/' + templateName, model: [ mainSubscription: params.mainSubscription]
	}

	/*
	 * When user click on 'Use Company Billin Cycle' Ajax request call this function fetch the billing configuration and
	 * set the period unit and period value as per Billing Configuration Next run date and billing period and
	 * render the template as per period unit.
	 */
	def updateSubscriptionOnBillingCycle () {

		BillingProcessConfigurationWS billingProcessConfiguration = webServicesSession.getBillingProcessConfiguration();
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(billingProcessConfiguration.nextRunDate)
		MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
		def orderPeriods = OrderPeriodDTO.createCriteria().list() { eq('company', retrieveCompany()) }
		OrderPeriodWS orderPeriod = webServicesSession.getOrderPeriodWS(params.mainSubscription.periodId.toInteger());
		def templateName = Constants.TEMPLATE_MONTHLY
		if (billingProcessConfiguration.periodUnitId.compareTo(Constants.PERIOD_UNIT_MONTH) == 0) {
			templateName = Constants.TEMPLATE_MONTHLY
			params.mainSubscription.nextInvoiceDayOfPeriod = cal.get(Calendar.DAY_OF_MONTH);
		  	params.mainSubscription.monthDays= MainSubscriptionWS.monthDays;
		} else if (billingProcessConfiguration.periodUnitId.compareTo(Constants.PERIOD_UNIT_WEEK) == 0) {
			templateName = Constants.TEMPLATE_WEEKLY
			params.mainSubscription.nextInvoiceDayOfPeriod = cal.get(Calendar.DAY_OF_WEEK);
			params.mainSubscription.weekDaysMap= MainSubscriptionWS.weekDaysMap;
		} else if (billingProcessConfiguration.periodUnitId .compareTo(Constants.PERIOD_UNIT_DAY) == 0) {
			templateName = Constants.TEMPLATE_DAILY
		  	params.mainSubscription.nextInvoiceDayOfPeriod = new Integer(1);
		} else if (billingProcessConfiguration.periodUnitId.compareTo(Constants.PERIOD_UNIT_YEAR) == 0) {
			templateName = Constants.TEMPLATE_YEARLY
			params.mainSubscription.nextInvoiceDayOfPeriod = (cal.get(Calendar.MONTH) + 1)
			params.mainSubscription.nextInvoiceDayOfPeriodOfYear = (cal.get(Calendar.DAY_OF_MONTH))
			params.mainSubscription.yearMonthsMap= MainSubscriptionWS.yearMonthsMap;
			params.mainSubscription.yearMonthDays= MainSubscriptionWS.yearMonthDays;
		} else if (billingProcessConfiguration.periodUnitId.compareTo(Constants.PERIOD_UNIT_SEMI_MONTHLY) == 0) {
			templateName = Constants.TEMPLATE_SEMI_MONTHLY
			Integer dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
			if (cal.getActualMaximum(Calendar.DAY_OF_MONTH) == dayOfMonth) {
				dayOfMonth = new Integer(15);
			} else if(cal.get(Calendar.DAY_OF_MONTH) > new Integer(15)) {
				dayOfMonth = (cal.get(Calendar.DAY_OF_MONTH) - new Integer(15))
			}
			params.mainSubscription.nextInvoiceDayOfPeriod = dayOfMonth;
		  	params.mainSubscription.semiMonthlyDaysMap= MainSubscriptionWS.semiMonthlyDaysMap;
		}
			render template: '/customer/subscription/' + templateName, model: [ mainSubscription: params.mainSubscription]
	}

    def retrieveCustomerNotes() {
        if (UserDTO.get(params.int('id'))) {
            def customerNotes = CustomerNoteDTO.createCriteria().list(max: 5, offset: 0) {
                and {
                    eq('customer.id', UserBL.getUserEntity(params.int('id'))?.getCustomer()?.getId())
                    order("creationTime", "desc")
                }
            }
        }
    }

	/**
	 * Display an cancellation requests. request ID is required.
	 */
	def showCancellationRequest () {
		if (!params.id) {
			flash.error = 'product.asset.not.selected'
			flash.args = [ params.id  as String]
			redirect controller: 'product', action: 'list'
			return
		}
		def cancellationRequest = CancellationRequestDTO.get(params.int('id'))
		render view: 'showCancellationRequest', model: [cancellationRequest:cancellationRequest]
	}

    private def cleanPaymentsInformation(List<PaymentInformationWS> paymentInformations) {
        if (paymentInformations != null) {
            for (PaymentInformationWS ws : paymentInformations) {
                ws.close();
            }
        }
    }

    private def bindCommissions(CustomerCommissionDefinitionWS[] commissions){
        def commissionDefinitions = []

        commissions?.each{
            PartnerDTO partnerDTO = PartnerDTO.get(it.partnerId)
            commissionDefinitions += [  partnerId : it.partnerId,
                                      partnerName : partnerDTO.baseUser.contact.firstName ? (partnerDTO.baseUser.contact.firstName + ' ' + partnerDTO.baseUser.contact.lastName) : partnerDTO.baseUser.userName,
                                             rate : it.rate
            ]
        }

        return commissionDefinitions
    }

    def findMetaFieldType(Integer metaFieldId) {
        return retrieveAvailableMetaFields().findResult { it.id == metaFieldId ? it : null}
    }

}
