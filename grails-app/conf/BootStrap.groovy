import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.csrf.ControllerAnnotationHelper
import com.sapienter.jbilling.server.discount.db.DiscountDTO
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.ItemTypeDTO
import com.sapienter.jbilling.server.item.db.PlanDTO
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS
import com.sapienter.jbilling.server.mediation.MediationProcess
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.order.db.OrderDTO
import com.sapienter.jbilling.server.order.db.OrderLineTypeDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.payment.db.PaymentDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDTO
import com.sapienter.jbilling.server.pricing.RateCardBL
import com.sapienter.jbilling.server.pricing.RouteBL
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL
import com.sapienter.jbilling.server.pricing.db.RateCardDTO
import com.sapienter.jbilling.server.pricing.db.RouteDTO
import com.sapienter.jbilling.server.pricing.db.RouteDAS
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO
import com.sapienter.jbilling.server.pricing.db.RateCardDAS
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS
import com.sapienter.jbilling.server.process.db.BillingProcessDTO
import com.sapienter.jbilling.server.report.db.ReportDTO
import com.sapienter.jbilling.server.report.db.ReportTypeDTO
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.db.AbstractDescription
import com.sapienter.jbilling.server.util.db.EnumerationDTO
import com.sapienter.jbilling.server.util.db.JobExecutionHeaderDTO
import com.sapienter.jbilling.server.util.db.NotificationCategoryDTO
import com.sapienter.jbilling.server.util.db.PreferenceTypeDTO
import com.wordnik.swagger.core.SwaggerContext
import grails.converters.JSON
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springmodules.cache.CachingModel
import org.springmodules.cache.provider.CacheProviderFacade
import com.wordnik.swagger.core.SwaggerContext

import java.util.logging.LogManager
import org.slf4j.bridge.SLF4JBridgeHandler


/*
jBilling - The Enterprise Open Source Billing System
Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

This file is part of jbilling.

jbilling is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

jbilling is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
*/

class BootStrap {

    def grailsApplication
    def flowExecutionRepository
    def jobScheduler

    private void setupLogging() {
        LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(".level=INFO".bytes));
        SLF4JBridgeHandler.install();
    }

    def init = { servletContext ->

        setupLogging()

		SpringSecurityUtils.clientRegisterFilter 'samlMetadataGeneratorFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 1
        SpringSecurityUtils.clientRegisterFilter 'samlEntryPoint', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 2
		SpringSecurityUtils.clientRegisterFilter 'samlMetadataDisplayFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 3
        SpringSecurityUtils.clientRegisterFilter 'samlProcessingFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 4
        SpringSecurityUtils.clientRegisterFilter 'samlLogoutFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 5
        SpringSecurityUtils.clientRegisterFilter 'samlLogoutProcessingFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 6
        SpringSecurityUtils.clientRegisterFilter 'concurrentSessionFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 7
        SpringSecurityUtils.clientRegisterFilter 'customAuthenticationFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 8
		// register jwt auth filter.
		SpringSecurityUtils.clientRegisterFilter 'jwtAuthenticationFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 9
		// microservice to billinghub auth filter.
		SpringSecurityUtils.clientRegisterFilter 'internalServiceAuthenticationFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 10
        // internal mcirservice auth filter.
        SpringSecurityUtils.clientRegisterFilter 'billingHubMicroServiceAuthenticationFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 11
		
        // register rate card beans on startup
        for (RateCardDTO rateCard : new RateCardDAS().findAll()) {
            new RateCardBL(rateCard).registerSpringBeans();
        }
        for (RouteDTO routeDTO : new RouteDAS().findAll()) {
            new RouteBL(routeDTO).registerSpringBeans();
        }
        for (RouteRateCardDTO routeRateCardDTO : new RouteRateCardDAS().findAll()) {
            new RouteBasedRateCardBL(routeRateCardDTO).registerSpringBeans();
        }

        // Setting web-flow default snapshot size to 100. The default size of the web-flow snapshot is 15 which was the cause of the issue at the time of create order. Issue #8712
        flowExecutionRepository.maxSnapshots = 100

        // start the quartz scheduler and schedule jbilling background processes only on batch servers
		if (!Util.getSysPropBooleanTrue(Constants.PROPERTY_RUN_API_ONLY_BUT_NO_BATCH)) {
            jobScheduler.startScheduler();
		}

        try {
            registerUserDTOMarshaller()
            registerOrderDTOMarshaller()
            registerJobExecutionDTOMarshaller()
            registerPartnerDTOMarshaller()
            registerContactDTOMarshaller()
            registerInvoiceDTOMarshaller()
            registerPaymentDTOMarshaller()
            registerBillingProcessDTOMarshaller()
            registerMediationProcessMarshaller()
            registerDiscountDTOMarshaller()
            registerItemTypeDTOMarshaller()
            registerItemDTOMarshaller()
            registerPreferenceTypeDTOMarshaller()
            registerRoleDTOMarshaller()
            registerEnumerationDTOMarshaller()
            registerNotificationCategoryDTOMarshaller()
            registerNotificationMessageTypeDTOMarshaller()
            registerAccountTypeDTOMarshaller()
            registerUsagePoolDTOMarshaller()
            registerRateCardDTOMarshaller()
            registerOrderPeriodDTOMarshaller()
            registerPaymentMethodTypeDTOMarshaller()
            registerOrderStatusDTOMarshaller()
            registerPluggableTaskTypeCategoryDTOMarshaller()
            registerPluggableTaskDTOMarshaller()
            registerPlanDTOMarshaller()
            registerReportTypeDTOMarshaller()
            registerReportDTOMarshaller()

            registerMediationConfigurationWSMarshaller()
            registerEntityTypeMarshaller()
            registerMetaFieldMarshaller()
            registerMetaFieldGroupMarshaller()
            registerDescriptionMarshaller()
            registerAssetDTOMarshaller()
        } catch (Exception e) {
            log.error "Marshaller Exception occurred: ${e?.message}", e
        }
//        Initialize annotation map for RequiresValidFormToken annotation.
        ControllerAnnotationHelper.init(grailsApplication)
        SwaggerContext.registerClassLoader(this.getClass().getClassLoader())
    }

    def destroy = {
    }

    private registerUserDTOMarshaller() {
        JSON.registerObjectMarshaller(UserDTO) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def status = ''
            if (it.userStatus.id > 1 && !it.userStatus.isSuspended()) {
                status = 'overdue'
            } else if (it.userStatus.id > 1 && it.userStatus.isSuspended()) {
                status = 'suspended'
            }

            def balance = UserBL.getBalance(it.id) as BigDecimal
            def currencySymbol = it.currency.symbol
            def customer = it.customer
            def contact = ContactDTO.findByUserId(it.id)
            def type = it.roles.asList().first()?.getTitle(session['language_id'])
            def hierarchy = [:]
            if (customer?.children?.size() > 0) {
                hierarchy.parent = true
                hierarchy.children = customer.children.findAll { it.baseUser.deleted == 0 }.size()
            }

            if (customer?.isParent == 1 && !customer?.parent) {
                hierarchy.parent = true
                hierarchy.children = customer.children.findAll { it.baseUser.deleted == 0 }.size()

            }
            if (customer?.parent) {
                hierarchy.child = true
            }

            [cell: [userId        : it.id,
                    userName      : it.userName,
                    company       : it.company.description,
                    status        : status,
                    balance       : balance,
                    contact       : contact,
                    currencySymbol: currencySymbol,
                    hierarchy     : hierarchy,
                    type          : type
            ],
             id  : it.id]
        }
        log.debug("UserDTO Marshaller registered")
    }

    private registerJobExecutionDTOMarshaller() {
        JSON.registerObjectMarshaller(JobExecutionHeaderDTO) {
            [cell: [id          : it.id,
                    startDate   : it.startDate,
                    endDate     : it.endDate,
                    status      : it.status?.name(),
                    jobType     : it.jobType
            ],
                    id  : it.id]
        }
        log.debug("JobExecutionHeaderDTO Marshaller registered")
    }

    private registerOrderDTOMarshaller() {
        JSON.registerObjectMarshaller(OrderDTO) {
            def total = it.total
            def currencySymbol = it.currency.symbol
            def date = it.createDate
            def customer = it.baseUserByUserId

            def hierarchy = [:]
            if (it.childOrders?.size() > 0) {
                hierarchy.parent = true
                hierarchy.children = it.childOrders.findAll { it.deleted == 0 }.size()
            }
            if (it.parentOrder) {
                hierarchy.child = true
            }

            [cell: [orderid       : it.id,
                    customer      : customer.userName,
                    company       : customer.company.description,
                    date          : date,
                    amount        : total,
                    currencySymbol: currencySymbol,
                    hierarchy     : hierarchy
            ],
             id  : it.id]
        }
        log.debug("OrderDTO Marshaller registered")
    }

    private registerPartnerDTOMarshaller() {
        JSON.registerObjectMarshaller(PartnerDTO) {
            def customer = it.baseUserByUserId
            def status = ''
            if (customer.userStatus?.id > 1 && !customer.userStatus?.isSuspended()) {
                status = 'overdue'
            } else if (customer.userStatus?.id > 1 && customer.userStatus?.isSuspended()) {
                status = 'suspended'
            }
            def contact = ContactDTO.findByUserId(customer.id)

            def hierarchy = [:]
            if (it.children?.size() > 0) {
                hierarchy.parent = true
                hierarchy.children = it.children.findAll { it.baseUser.deleted == 0 }.size()
            }
            if (it.parent) {
                hierarchy.child = true
            }

            [cell: [userid   : it.id,
                    username : customer.userName,
                    company  : customer.company.description,
                    status   : status,
                    contact  : contact,
                    hierarchy: hierarchy
            ],
             id  : it.id]
        }
        log.debug("PartnerDTO Marshaller registered")
    }

    private registerContactDTOMarshaller() {
        JSON.registerObjectMarshaller(ContactDTO) {
            [firstName   : it.firstName,
             lastName    : it.lastName,
             organization: it.organizationName]
        }
        log.debug("ContactDTO Marshaller registered")
    }

    private registerInvoiceDTOMarshaller() {
        JSON.registerObjectMarshaller(InvoiceDTO) {
            def currencySymbol = it.currencyDTO.symbol
            def dueDate = it.dueDate
            def customer = it.baseUser
            def status = it.invoiceStatus

            [cell: [invoiceId     : it.id,
                    invoiceNumber : it.publicNumber,
                    userName      : customer.userName,
                    company       : customer.company.description,
                    dueDate       : dueDate,
                    status        : status,
                    amount        : it.total,
                    balance       : it.balance,
                    currencySymbol: currencySymbol
            ],
             id  : it.id]

        }
        log.debug("InvoiceDTO Marshaller registered")
    }

    private registerPaymentDTOMarshaller() {
        JSON.registerObjectMarshaller(PaymentDTO) {
            def currencySymbol = it.currencyDTO.symbol
            def date = it.paymentDate
            def customer = it.baseUser
            def paymentOrRefund = it.isRefund ? 'R' : 'P'

            [cell: [paymentId      : it.id,
                    userName       : customer.userName,
                    company        : customer.company.description,
                    date           : date,
                    paymentOrRefund: paymentOrRefund,
                    amount         : it.amount,
                    currencySymbol : currencySymbol,
                    method         : it.paymentMethod,
                    result         : it.paymentResult
            ],
             id  : it.id]

        }
        log.debug("PaymentDTO Marshaller registered")
    }

    private registerBillingProcessDTOMarshaller() {
        JSON.registerObjectMarshaller(BillingProcessDTO) {
            def orderCount = it.orderProcesses?.size()
            def invoiceCount = it.invoices?.size()
            def invoiced = [:]
            def invoiceCarried = [:]
            it.invoices?.each { invoice ->
                invoiced[invoice.currency] = invoiced.get(invoice.currency, BigDecimal.ZERO).add(invoice.total.subtract(invoice.carriedBalance))
                invoiceCarried[invoice.currency] = invoiceCarried.get(invoice.currency, BigDecimal.ZERO).add(invoice.carriedBalance)
            }
            def multiCurrency = invoiced.keySet().size() == 1 ? Boolean.FALSE : Boolean.TRUE
            def currencySymbol
            def totalInvoiced
            def totalCarried
            if (multiCurrency) {
                currencySymbol = ''
                totalInvoiced = ''
                totalCarried = ''
            } else {
                invoiced.entrySet().each { total ->
                    totalInvoiced = total.value
                    currencySymbol = total.key.symbol
                }
                invoiceCarried.entrySet().each { total ->
                    totalCarried = total.value
                }
            }
            [cell: [billingId     : it.id,
                    date          : it.billingDate,
                    orderCount    : orderCount,
                    invoiceCount  : invoiceCount,
                    multiCurrency : multiCurrency,
                    currencySymbol: currencySymbol,
                    totalInvoiced : totalInvoiced,
                    totalCarried  : totalCarried,
                    isReview      : it.isReview
            ],
             id  : it.id]
        }
        log.debug("BillingProcessDTO Marshaller registered")
    }

    private registerMediationProcessMarshaller() {
        JSON.registerObjectMarshaller(MediationProcess) {
            [cell: [processId   : it.id.toString(),
                    startDate   : it.startDate,
                    endDate     : it.endDate,
                    totalRecords: it.recordsProcessed,
                    orders      : it.doneAndBillable
            ],
             id  : it.id.toString()]

        }
        log.debug("MediationProcess Marshaller registered")
	}
	
    private registerDiscountDTOMarshaller() {
        JSON.registerObjectMarshaller(DiscountDTO, 2) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [discountId : it.id,
                    code       : it.code,
                    description: it.getDescription(session['language_id']),
                    type       : it.type
            ],
             id  : it.id]

        }
        log.debug("DiscountDTO Marshaller registered")
    }

    private registerItemTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(ItemTypeDTO, 3) {
            def lineType = new OrderLineTypeDTO(it.orderLineTypeId, 0)
            def entities = it.entities?.toArray()
            def company = ''
            if (it.entity == null && entities?.size() > 0) {
                company = entities[0]?.description
            } else {
                company = it.entity.description
            }
            [cell: [categoryId: it.id,
                    company   : company,
                    global    : it.global,
                    multiple  : it.entities?.size() > 1,
                    lineType  : lineType,
                    name      : it.description
            ],
             id  : it.id]

        }
        log.debug("ItemTypeDTO Marshaller registered")
    }

    private registerItemDTOMarshaller() {
        JSON.registerObjectMarshaller(ItemDTO, 4) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def name = it?.getDescription(session['language_id']).encodeAsHTML()
            def entities = it.entities?.toArray()
            def company = ''
            if (entities?.size() > 0) {
                company = entities[0]?.description
            }
            [cell: [productId: it.id,
                    company  : company,
                    global   : it.global,
                    multiple : it.entities?.size() > 1,
                    name     : name,
                    number   : it.internalNumber
            ],
             id  : it.id]

        }
        log.debug("ItemDTO Marshaller registered")
    }

    private registerPreferenceTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(PreferenceTypeDTO, 5) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def preference = it.preferences.find {
                it.jbillingTable.name == Constants.TABLE_ENTITY &&
                        it.foreignId == session['company_id']
            }
            def value = preference ? preference.value : it.defaultValue
            [cell: [preferenceId: it.id,
                    description : it.getDescription(session['language_id']) ?: '',
                    value       : value
            ],
             id  : it.id]

        }
        log.debug("PreferenceTypeDTO Marshaller registered")
    }

    private registerRoleDTOMarshaller() {
        JSON.registerObjectMarshaller(RoleDTO, 6) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [roleId: it.id,
                    title : it.getDescription(session['language_id'], 'title') ?: ''
            ],
             id  : it.id]
        }
        log.debug("RoleDTO Marshaller registered")
    }

    private registerEnumerationDTOMarshaller() {
        JSON.registerObjectMarshaller(EnumerationDTO) {
            [cell: [enumId: it.id,
                    name  : it.getName() ?: ''
            ],
             id  : it.id]
        }
        log.debug("EnumerationDTO Marshaller registered")
    }

    private registerNotificationCategoryDTOMarshaller() {
        JSON.registerObjectMarshaller(NotificationCategoryDTO, 7) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [categoryId : it.id,
                    description: it?.getDescription(session['language_id']) ?: ''
            ],
             id  : it.id]
        }
        log.debug("NotificationCategoryDTO Marshaller registered")
    }

    private registerNotificationMessageTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(NotificationMessageTypeDTO, 8) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def languageId = session['language_id'] as Integer
            def entityId = session['company_id'] as Integer
            def active = true
            it.getNotificationMessages().each {
                if (languageId == it.language.id && entityId == it.entity.id && it.useFlag > 0) {
                    active = false
                }
            }

            [cell: [notificationId: it.id,
                    description   : it?.getDescription(languageId) ?: '',
                    active        : !active
            ],
             id  : it.id]
        }
        log.debug("NotificationMessageTypeDTO Marshaller registered")
    }

    private registerAccountTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(AccountTypeDTO, 9) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [typeId     : it.id,
                    description: it.getDescription(session['language_id']) ?: ''
            ],
             id  : it.id]
        }
        log.debug("AccountTypeDTO Marshaller registered")
    }

    private registerUsagePoolDTOMarshaller() {
        JSON.registerObjectMarshaller(UsagePoolDTO, 10) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [poolId          : it.id,
                    name            : it.getDescription(session['language_id'], 'name') ?: '',
                    quantity        : it.quantity,
                    cyclePeriodValue: it.cyclePeriodValue,
                    cyclePeriodUnit : it.cyclePeriodUnit
            ],
             id  : it.id]
        }
        log.debug("UsagePoolDTO Marshaller registered")
    }

    private registerRateCardDTOMarshaller() {
        JSON.registerObjectMarshaller(RateCardDTO) {
            def entities = it.childCompanies?.toArray()
            def company = ''
            if (it.company == null && entities?.size() > 0) {
                company = entities[0]?.description
            } else {
                company = it.company.description
            }
            def multiple = entities?.size() > 1 || (entities?.size() == 1 && it.company != null)
            [cell: [rateCardId: it.id,
                    company   : company,
                    global    : it.global,
                    multiple  : multiple,
                    name      : it.name,
                    tableName : it.tableName
            ],
             id  : it.id]

        }
        log.debug("RateCardDTO Marshaller registered")
    }

    private registerMediationConfigurationWSMarshaller() {
        JSON.registerObjectMarshaller(MediationConfigurationWS) {
            def cache = (CacheProviderFacade) Context.getBean(Context.Name.CACHE);
            def cacheModel = (CachingModel) Context.getBean(Context.Name.CACHE_MODEL_READONLY);
            def pluggableTaskDAS = new PluggableTaskDAS(cache: cache, cacheModel: cacheModel)
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def readers = new ArrayList<PluggableTaskDTO>()
            if (session.company_id) {
                readers = pluggableTaskDAS.findByEntityCategory(session.company_id as Integer,
                        Constants.PLUGGABLE_TASK_MEDIATION_READER);
            }
            def pluggableTaskId = it.pluggableTaskId
            def configReader = readers.find { it.id == pluggableTaskId }
            [cell: [mediationConfigId: it.id,
                    name             : it.name,
                    order            : it.orderValue,
                    readerId         : configReader?.id,
                    readerDescription: configReader?.type?.getDescription(session.language_id)
            ],
             id  : it.id]

        }
        log.debug("MediationConfigurationWS Marshaller registered")
    }

    private registerOrderPeriodDTOMarshaller() {
        JSON.registerObjectMarshaller(OrderPeriodDTO, 11) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [periodId   : it.id,
                    description: it.getDescription(session['language_id']) ?: '',
                    unit       : it.getPeriodUnit()?.getDescription(session['language_id']) ?: '',
                    value      : it.value
            ],
             id  : it.id]
        }
        log.debug("OrderPeriodDTO Marshaller registered")
    }

    private registerPaymentMethodTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(PaymentMethodTypeDTO) {
            [cell: [paymentMethodId: it.id,
                    name           : it.methodName ?: ''
            ],
             id  : it.id]
        }
        log.debug("PaymentMethodTypeDTO Marshaller registered")
    }

    private registerOrderStatusDTOMarshaller() {
        JSON.registerObjectMarshaller(OrderStatusDTO, 12) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [orderStatusId: it.id,
                    flag         : it.orderStatusFlag as String,
                    description  : it.getDescription(session['language_id']) ?: ''
            ],
             id  : it.id]
        }
        log.debug("OrderStatusDTO Marshaller registered")
    }

    private registerPluggableTaskTypeCategoryDTOMarshaller() {
        JSON.registerObjectMarshaller(PluggableTaskTypeCategoryDTO, 13) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [categoryId   : it.id,
                    interfaceName: it.interfaceName,
                    description  : it.getDescription(session['language_id']) ?: ''
            ],
             id  : it.id]
        }
        log.debug("PluggableTaskTypeCategoryDTO Marshaller registered")
    }

    private registerPluggableTaskDTOMarshaller() {
        JSON.registerObjectMarshaller(PluggableTaskDTO) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            def type = it.type
            [cell: [pluginId     : it.id,
                    typeClassName: type.className,
                    typeTitle    : type.getDescription(session['language_id'], 'title') ?: '',
                    order        : it.processingOrder
            ],
             id  : it.id]
        }
        log.debug("PluggableTaskDTO Marshaller registered")
    }

    private registerPlanDTOMarshaller() {
        JSON.registerObjectMarshaller(PlanDTO) {
            [cell: [planId  : it.id,
                    products: it.planItems?.size(),
                    item    : it.item
            ],
             id  : it.id]
        }
        log.debug("PlanDTO Marshaller registered")
    }

    private registerReportTypeDTOMarshaller() {
        JSON.registerObjectMarshaller(ReportTypeDTO, 14) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [typeId     : it.id,
                    reports    : it.reports?.size(),
                    description: it.getDescription(session['language_id']) ?: ''
            ],
             id  : it.id]
        }
        log.debug("ReportTypeDTO Marshaller registered")
    }

    private registerReportDTOMarshaller() {
        JSON.registerObjectMarshaller(ReportDTO, 15) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [reportId: it.id,
                    fileName: it.fileName,
                    name    : it.name
            ],
             id  : it.id]
        }
        log.debug("ReportDTO Marshaller registered")
    }

    private registerEntityTypeMarshaller() {
        JSON.registerObjectMarshaller(EntityType) {
            [cell: [name: it as String
            ],
             id  : it as String]
        }
        log.debug("EntityType Marshaller registered")
    }

    private registerMetaFieldMarshaller() {
        JSON.registerObjectMarshaller(MetaField, 14) {
            [cell: [metaFieldId: it.id,
                    name       : it.name
            ],
             id  : it.id]
        }
        log.debug("MetaField Marshaller registered")
    }

    private registerMetaFieldGroupMarshaller() {
        JSON.registerObjectMarshaller(MetaFieldGroup, 15) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [cell: [groupId    : it.id,
                    description: it?.getDescription(session['language_id'])
            ],
             id  : it.id]
        }
        log.debug("MetaFieldGroup Marshaller registered")
    }

    private registerDescriptionMarshaller() {
        JSON.registerObjectMarshaller(AbstractDescription, 1) {
            def session = RequestContextHolder.currentRequestAttributes().getSession()
            [description: it?.getDescription(session['language_id'])]
        }
        log.debug("Description Marshaller registered")
    }

    private registerAssetDTOMarshaller() {
        JSON.registerObjectMarshaller(AssetDTO, 17) {
            def entities = it.entities?.toArray()
            def company = ''
            if (entities?.size() > 0) {
                company = entities[0]?.description
            }
            [cell: [assetId       : it.id,
                    identifier    : it.identifier,
                    company       : company,
                    global        : it.global,
                    multiple      : it.entities?.size() > 1,
                    createDatetime: it.createDatetime,
                    status        : it.assetStatus.description

            ],
             id  : it.id]
        }
        log.debug("AssetDTO Marshaller registered")
    }
}
