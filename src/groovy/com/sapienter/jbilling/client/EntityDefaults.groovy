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

package com.sapienter.jbilling.client

import com.sapienter.jbilling.common.SystemProperties
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.ValidationRule
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType
import com.sapienter.jbilling.server.notification.NotificationMediumType
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageLineDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageSectionDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.order.ApplyToOrder
import com.sapienter.jbilling.server.order.OrderStatusFlag
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO
import com.sapienter.jbilling.server.order.db.OrderStatusDTO
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeDAS
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeDTO
import com.sapienter.jbilling.server.pricing.db.IncrementUnit
import com.sapienter.jbilling.server.pricing.db.PriceUnit
import com.sapienter.jbilling.server.pricing.db.RatingUnitDTO
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.process.db.ProratingType
import com.sapienter.jbilling.server.report.db.ReportDTO
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.db.EnumerationDTO
import com.sapienter.jbilling.server.util.db.EnumerationValueDTO
import com.sapienter.jbilling.server.util.db.InternationalDescription
import com.sapienter.jbilling.server.util.db.InternationalDescriptionId
import com.sapienter.jbilling.server.util.db.JbillingTable
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.util.db.PreferenceDTO
import com.sapienter.jbilling.server.util.db.PreferenceTypeDTO

import org.codehaus.groovy.grails.context.support.ReloadableResourceBundleMessageSource

import java.time.temporal.ChronoUnit

/**
 * EntityDefaults 
 *
 * @author Brian Cowdery
 * @since 10/03/11
 */
class EntityDefaults {

    UserDTO rootUser
    CompanyDTO company
    LanguageDTO language
    JbillingTable entityTable
    Locale locale

    ReloadableResourceBundleMessageSource messageSource

	EntityDefaults() {
	}

    EntityDefaults(CompanyDTO company, UserDTO rootUser, LanguageDTO language, ReloadableResourceBundleMessageSource messageSource) {
        this.company = company
        this.rootUser = rootUser
        this.language = language
        this.entityTable = JbillingTable.findByName(Constants.TABLE_ENTITY)
        this.locale = rootUser.language.asLocale()
        this.messageSource = messageSource
    }

    /**
     * Initialize the entity, creating the necessary preferences, plugins and other defaults.
     */
    def init() {
         /*
            Order Status
         */
		
		def invoiceOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.INVOICE, entity:company).save()
		invoiceOS.setDescription("Active", language.id)
		def finishedOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.FINISHED, entity:company).save()
		finishedOS.setDescription("Finished", language.id)
		def notInvoiceOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.NOT_INVOICE, entity:company).save()
		notInvoiceOS.setDescription("Suspended", language.id)
		def suspAgeingOS = new OrderStatusDTO(orderStatusFlag: OrderStatusFlag.SUSPENDED_AGEING, entity:company).save()
		suspAgeingOS.setDescription("Suspended ageing(auto)", language.id)
		log.debug("company.id ********: "+company?.id)

        // add company currency to the entity currency map
        // it's annoying that we need to build this association manually, it would be better mapped through CompanyDTO
        company.currencies << company.currency

        /*
            Order periods
         */
        def monthly = new OrderPeriodDTO(company: company, value: 1, periodUnit: new PeriodUnitDTO(Constants.PERIOD_UNIT_MONTH)).save()
		monthly.setDescription("Monthly", language.id)

		def maxStatusId= OrderChangeStatusDTO.createCriteria().get {
		    projections {
		        max "id"
		    }
		} as Integer
        //default order change status 'Default' apply
        def orderChangeStatusApply = new OrderChangeStatusDTO(company: company, applyToOrder: ApplyToOrder.YES, 
											deleted: 0, order: 1, id: ++maxStatusId).save()
        orderChangeStatusApply.setDescription(getMessage("order.change.status.default.apply"), Constants.LANGUAGE_ENGLISH_ID)
		orderChangeStatusApply.setDescription(getMessage("order.change.status.default.apply"), language.id)
        orderChangeStatusApply.save()

        //default account type
        def oneMonthly= new MainSubscriptionDTO(monthly, Integer.valueOf(1))
        def defaultAccountType = new AccountTypeDTO(company: company, billingCycle: oneMonthly, currency: company.currency, language : language).save()
        defaultAccountType.setDescription('description', language.id, getMessage("default.account.type.name"))

        /*
            Rating Unit
         */
        new RatingUnitDTO(company: company, canBeDeleted: false, name: "Time",
                priceUnit: new PriceUnit(name: "Minute"), incrementUnit: new IncrementUnit(name: "Seconds", quantity: 60)).save()

        /*
            Invoice delivery methods
         */
        InvoiceDeliveryMethodDAS invoiceDeliveryMethodDAS = new InvoiceDeliveryMethodDAS()
        List<InvoiceDeliveryMethodDTO> invoiceDeliveryMethodDtos = invoiceDeliveryMethodDAS.findAll()
        for (InvoiceDeliveryMethodDTO dto : invoiceDeliveryMethodDtos) {
            dto.entities << company
        }

        /*
            Reports
         */
        ReportDTO.list().each { report ->
            company.reports << report
        }


        /*
            Billing process configuration
         */
        new BillingProcessConfigurationDTO(
                entity: company,
				periodUnit: new PeriodUnitDTO(Constants.PERIOD_UNIT_MONTH),
                nextRunDate: TimezoneHelper.companyCurrentDatePlusTemporalUnit(company.id,1,ChronoUnit.MONTHS),
                generateReport: 1,
                retries: 0,
                daysForRetry: 1,
                daysForReport: 3,
                reviewStatus: 1,
				dueDateUnitId: 1,
                dueDateValue: 1,
                onlyRecurring: 1,
                invoiceDateProcess: 0,
                maximumPeriods: 99,
                autoPaymentApplication: 1,
				autoCreditNoteApplication: 1,
				applyCreditNotesBeforePayments: 1,
				lastDayOfMonth: false,
				proratingType: ProratingType.PRORATING_AUTO_OFF
        ).save()


        /*
            Pluggable tasks
         */

        // PaymentFakeTask
        def paymentTask = new PluggableTaskDTO(entityId: company.id, type: new PluggableTaskTypeDTO(21), processingOrder: 1).save()
        new PluggableTaskParameterDTO(task: paymentTask, name: 'all', strValue: 'yes').save()
		
        // BasicEmailNotificationTask
        def emailTask = new PluggableTaskDTO(entityId: company.id, type: new PluggableTaskTypeDTO(9), processingOrder: 1).save()

        updateParametersForTask(emailTask)

        // PaperInvoiceNotificationTask
        def notificationTask = new PluggableTaskDTO(entityId: company.id, type: new PluggableTaskTypeDTO(12), processingOrder: 2, parameters: [
            new PluggableTaskParameterDTO(name: 'design', strValue: 'invoice_design'),
            new PluggableTaskParameterDTO(name: 'sql_query', strValue: 'true')
        ]).save()

        updateParametersForTask(notificationTask)

		//The IDs may not be hard coded. They may be different under different circumstances.
		PluggableTaskTypeDAS pluggableTaskTypeDAS= new PluggableTaskTypeDAS();
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicLineTotalTask'), processingOrder: 1).save()    // BasicLineTotalTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.CalculateDueDate'), processingOrder: 1).save()    // CalculateDueDate
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.OrderChangeBasedCompositionTask'), processingOrder: 2).save()    // OrderChangeBasedCompositionTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicOrderFilterTask'), processingOrder: 1).save()    // BasicOrderFilterTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicInvoiceFilterTask'), processingOrder: 1).save()    // BasicInvoiceFilterTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicOrderPeriodTask'), processingOrder: 1).save()    // BasicOrderPeriodTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pluggableTask.BasicPaymentInfoTask'), processingOrder: 1).save()   // BasicPaymentInfoTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.payment.tasks.NoAsyncParameters'), processingOrder: 1).save()   // NoAsyncParameters
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.pricing.tasks.PriceModelPricingTask'), processingOrder: 1).save()   // PriceModelPricingTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.item.tasks.BasicItemManager'), processingOrder: 1).save()   // BasicItemManager
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.billing.task.BillingProcessTask'), processingOrder: 1).save()   // BillingProcessTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.process.task.AgeingProcessTask'), processingOrder: 2).save()   // AgeingProcessTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolEvaluationTask'), processingOrder: 3).save()  // CustomerUsagePoolEvaluationTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.process.task.BasicAgeingTask'), processingOrder: 1).save()   // BasicAgeingTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask'), processingOrder: 1).save()   // BasicBillingProcessFilterTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.order.task.CreateOrderForResellerTask'), processingOrder: 2).save()  // CreateOrderForResellerTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.invoice.task.DeleteResellerOrderTask'), processingOrder: 3).save()  // DeleteResellerOrderTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.usagePool.task.CustomerPlanSubscriptionProcessingTask'), processingOrder: 4).save()  // CustomerPlanSubscriptionProcessingTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolUpdateTask'), processingOrder: 5).save()  // CustomerUsagePoolUpdateTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.usagePool.task.CustomerPlanUnsubscriptionProcessingTask'), processingOrder: 6).save()  // CustomerPlanUnsubscriptionProcessingTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.order.task.OrderChangeApplyOrderStatusTask'), processingOrder: 7).save()  // CustomerPlanUnsubscriptionProcessingTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.invoice.task.ApplyNegativeInvoiceToCreditNoteTask'), processingOrder: 8).save()  // ApplyNegativeInvoiceToCreditNoteTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.invoice.task.ApplyNegativeInvoiceToPaymentTask'), processingOrder: 9).save()  // ApplyNegativeInvoiceToPaymentTask
		new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.usagePool.task.ProrateCustomerUsagePoolTask'), processingOrder: 10).save()  // ProrateCustomerUsagePoolTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.item.tasks.RemoveAssetFromFinishedOrderTask'), processingOrder: 11).save()  // RemoveAssetFromFinishedOrderTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.item.tasks.AssetUpdatedTask'), processingOrder: 12).save()  // AssetUpdatedTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.order.task.CreateCyclesTask'), processingOrder: 13).save()  // CreateCyclesTask
        new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.item.tasks.SetAssetToOrderFinishedStatusTask'), processingOrder: 14).save()  // SetAssetToOrderFinishedStatusTask
		
		//if ( MediationVersion.MEDIATION_VERSION_2_0.getVersion().equals( Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)) ) {
		//	new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.mediation.task.SimpleMediationTask'), processingOrder: 1).save()   // SimpleMediationTask
		//}
		
        //This is for testing the initial email sent when the company is created
        //Otherwise, admin users WILL NOT have passwords configured
        //And the SMTP server will need to be configured
        def testNotificationTask = new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.notification.task.TestNotificationTask'), processingOrder: 3, parameters: [
                new PluggableTaskParameterDTO(name: 'from', strValue: 'admin@jbilling.com')
        ]).save()

        updateParametersForTask(testNotificationTask)

        //This is for the sysadmin receive the crendencials when execute the copy company
        def adminCredentialNotificationTask = new PluggableTaskDTO(entityId: company.id, type: pluggableTaskTypeDAS.findByClassName('com.sapienter.jbilling.server.company.task.AdminCredentialNotificationTask'), processingOrder: 8, parameters: [
                new PluggableTaskParameterDTO(name: 'notification_id', intValue: 21)
        ]).save()

        updateParametersForTask(adminCredentialNotificationTask)

        /*
            Preferences
         */
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(Constants.PREFERENCE_SHOW_NOTE_IN_INVOICE), value: 1).save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(Constants.PREFERENCE_INVOICE_PREFIX), value: "").save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(Constants.PREFERENCE_INVOICE_NUMBER), value: 1).save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(Constants.PREFERENCE_FAILED_LOGINS_LOCKOUT), value: 3).save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(Constants.PREFERENCE_ACCOUNT_LOCKOUT_TIME), value: 60).save()
        new PreferenceDTO(jbillingTable: entityTable, foreignId: company.id, preferenceType: new PreferenceTypeDTO(Constants.PREFERENCE_ONE_ORDER_PER_MEDIATION_RUN), value: '0').save()

        /*
            Notification messages
         */
        createNotificationMessage(Constants.NOTIFICATION_TYPE_INVOICE_EMAIL, 'signup.notification.email.title', 'signup.notification.email')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_USER_REACTIVATED, 'signup.notification.user.reactivated.title', 'signup.notification.user.reactivated')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_USER_OVERDUE, 'signup.notification.overdue.title', 'signup.notification.overdue')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_ORDER_EXPIRE_1, 'signup.notification.order.expire.1.title', 'signup.notification.order.expire.1')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_PAYMENT_SUCCESS, 'signup.notification.payment.success.title', 'signup.notification.payment.success')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_PAYMENT_FAILED, 'signup.notification.payment.failed.title', 'signup.notification.payment.failed')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_INVOICE_REMINDER, 'signup.notification.invoice.reminder.title', 'signup.notification.invoice.reminder')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_CREDIT_CARD_UPDATE, 'signup.notification.credit.card.update.title', 'signup.notification.credit.card.update')
		createNotificationMessage(Constants.NOTIFICATION_TYPE_LOST_PASSWORD, 'signup.notification.lost.password.update.title', 'signup.notification.lost.password.update')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_INITIAL_CREDENTIALS, 'signup.notification.initial.credentials.update.title', 'signup.notification.initial.credentials.update')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_USAGE_POOL_CONSUMPTION, 'signup.notification.usage.pool.consumption.percentage.email.title', 'signup.notification.usage.pool.consumption.percentage.email')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_SSO_ENABLED, 'signup.notification.sso.enabled.email.title', 'signup.notification.sso.enabled.email')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_SCHEDULED_JOB_STARTED, 'signup.notitication.scheduled.task.started.title', 'signup.notitication.scheduled.task.started.body')
        createNotificationMessage(Constants.NOTIFICATION_TYPE_SCHEDULED_JOB_COMPLETED, 'signup.notitication.scheduled.task.completed.title', 'signup.notitication.scheduled.task.completed.body')
        createNotificationMessage(Constants.NOTIFY_PASSWORD_CHANGE, 'signup.password.notification.notify.title', 'signup.notification.notify.user')


        /*
         * Create payment method template meta fields for entity
         */
		// Credit card meta file
		MetaFieldType usage;
		ValidationRule rule;
		InternationalDescriptionId desId
		
		int valRulId = JbillingTable.findByName(Constants.TABLE_VALIDATION_RULE).getId();
	
		PaymentMethodTemplateDTO template = PaymentMethodTemplateDTO.findByTemplateName(Constants.PAYMENT_CARD)
		usage = MetaFieldType.TITLE
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cc.cardholder.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : false, mandatory : true, primary : true, displayOrder : 1, fieldUsage : usage).save()
		
		usage =MetaFieldType.PAYMENT_CARD_NUMBER
		rule = new ValidationRule(ruleType : ValidationRuleType.PAYMENT_CARD, enabled : true).save()
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cc.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : false, mandatory : true, primary : true, displayOrder : 2, validationRule : rule, fieldUsage : usage).save()
		desId = new InternationalDescriptionId(valRulId, rule.getId(), "errorMessage", language.id)
		new InternationalDescription(id : desId, content : getMessage("validation.payment.card.number.invalid")).save()
		
		usage = MetaFieldType.DATE
		SortedMap<String, String> attributes = new TreeMap<String, String>()
		attributes.put('regularExpression', '(?:0[1-9]|1[0-2])/[0-9]{4}')
		rule = new ValidationRule(ruleType : ValidationRuleType.REGEX, enabled : true, ruleAttributes : attributes).save()
		rule.ruleAttributes = attributes
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cc.expiry.date", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : false, mandatory : true, primary : true, displayOrder : 3, validationRule : rule, fieldUsage : usage).save()
		desId = new InternationalDescriptionId(valRulId, rule.getId(), "errorMessage", language.id)
		new InternationalDescription(id : desId, content : getMessage("validation.payment.card.expiry.date.invalid")).save()
		
		usage = MetaFieldType.GATEWAY_KEY
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cc.gateway.key", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : true, mandatory : false, primary : true, displayOrder : 4, fieldUsage : usage).save()
		
		usage = MetaFieldType.CC_TYPE
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cc.type", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : true, mandatory : false, primary : true, displayOrder : 5, fieldUsage : usage).save()
		
		usage = MetaFieldType.AUTO_PAYMENT_LIMIT
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "autopayment.limit", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.DECIMAL, disabled : false, mandatory : false, primary : true, displayOrder : 6, fieldUsage : usage).save()

		usage = MetaFieldType.AUTO_PAYMENT_AUTHORIZATION
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "autopayment.authorization", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.BOOLEAN, disabled : false, mandatory : false, primary : true, displayOrder : 7, fieldUsage : usage).save()

		// Ach template meta fields 
		template = PaymentMethodTemplateDTO.findByTemplateName(Constants.ACH);
		usage = MetaFieldType.BANK_ROUTING_NUMBER
		attributes = new TreeMap<String, String>()
		attributes.put('regularExpression', '(?<=\\s|^)\\d+(?=\\s|$)')
		rule = new ValidationRule(ruleType : ValidationRuleType.REGEX, enabled : true, ruleAttributes : attributes).save()
		rule.ruleAttributes = attributes
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "ach.routing.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : false, mandatory : true, primary : true, displayOrder : 1, validationRule : rule, fieldUsage : usage).save()
		desId = new InternationalDescriptionId(valRulId, rule.getId(), "errorMessage", language.id)
		new InternationalDescription(id : desId, content : getMessage("validation.ach.aba.routing.number.invalid")).save()
		
		usage = MetaFieldType.TITLE
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "ach.customer.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 2, fieldUsage : usage).save()
		
		usage = MetaFieldType.BANK_ACCOUNT_NUMBER
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "ach.account.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : false, mandatory : true, primary : true, displayOrder : 3, fieldUsage : usage).save()
		
		usage = MetaFieldType.BANK_NAME
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "ach.bank.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 4, fieldUsage : usage).save()
		
		usage = MetaFieldType.BANK_ACCOUNT_TYPE
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "ach.account.type", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.ENUMERATION, disabled : false, mandatory : true, primary : true, displayOrder : 5, fieldUsage : usage).save()
		
		EnumerationDTO enumeration = new EnumerationDTO(name : "ach.account.type", entityId : company.id).save()
		new EnumerationValueDTO(value : 'CHECKING', enumeration : enumeration).save()
		new EnumerationValueDTO(value : 'SAVINGS', enumeration : enumeration).save()
		
		usage = MetaFieldType.GATEWAY_KEY
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "ach.gateway.key", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.CHAR, disabled : true, mandatory : false, primary : true, displayOrder : 6, fieldUsage : usage).save()
		
		usage = MetaFieldType.AUTO_PAYMENT_LIMIT
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "autopayment.limit", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : false, primary : true, displayOrder : 7, fieldUsage : usage).save()

		usage = MetaFieldType.AUTO_PAYMENT_AUTHORIZATION
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "autopayment.authorization", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.BOOLEAN, disabled : false, mandatory : false, primary : true, displayOrder : 8, fieldUsage : usage).save()

		// cheque template meta fields
		template = PaymentMethodTemplateDTO.findByTemplateName(Constants.CHEQUE)
		
		usage = MetaFieldType.BANK_NAME
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cheque.bank.name", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 1, fieldUsage : usage).save()
		
		usage = MetaFieldType.CHEQUE_NUMBER
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cheque.number", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.STRING, disabled : false, mandatory : true, primary : true, displayOrder : 2, fieldUsage : usage).save()
		
		usage = MetaFieldType.DATE
		template.paymentTemplateMetaFields << new MetaField(entityId : company.id, name : "cheque.date", entityType : EntityType.PAYMENT_METHOD_TEMPLATE, dataType : DataType.DATE, disabled : false, mandatory : true, primary : true, displayOrder : 3, fieldUsage : usage).save()

        //Add Subscriber URI metafield
        new MetaField(entityId : company.id, name : "Subscriber URI", entityType : EntityType.CUSTOMER, dataType: DataType.STRING, disabled : false, mandatory : false, displayOrder: 1, optlock: 0, primary: true).save()
    }

    /**
     * When we construct the pluggable task the way we do it now, we enable the pluggable task
     * to have parameters immediately but we need to still reference their parameters
     * to the task they are in
     * @param taskDTO
     */
    def updateParametersForTask(PluggableTaskDTO taskDTO) {
        for (PluggableTaskParameterDTO parameter : taskDTO.getParameters()) {
            parameter.task = taskDTO
            parameter.save()
        }
    }

    /**
     * Create a new, 2 section notification message for the given type id and messages. Messages are
     * resolved from the grails 'messages.properties' bundle.
     *
     * @param typeId notification type id
     * @param titleCode message code for the notification message title
     * @param bodyCode message code for the notification message body
     */
    def createNotificationMessage(Integer typeId, String titleCode, String bodyCode) {
        def message = new NotificationMessageDTO(
                entity: company,
                language: language,
                useFlag: 1,
                notificationMessageType: new NotificationMessageTypeDTO(id: typeId)
        )
		
		def mediumTypes = new ArrayList<NotificationMediumType>(Arrays.asList(NotificationMediumType.values()));
		message.mediumTypes = mediumTypes

        message.save()

        def titleSection = new NotificationMessageSectionDTO(notificationMessage: message, section: 1)
        titleSection.notificationMessageLines << new NotificationMessageLineDTO(notificationMessageSection: titleSection, content: getMessage(titleCode))
        message.notificationMessageSections << titleSection
        titleSection.save()

        def bodySection = new NotificationMessageSectionDTO(notificationMessage: message, section: 2)
        def bodyContent = getMessage(bodyCode);
        if(SystemProperties.isBrandingJBilling()) {
            bodyContent = bodyContent.replaceAll("AppBilling", "jBilling")
        }
        bodySection.notificationMessageLines << new NotificationMessageLineDTO(notificationMessageSection: bodySection, content: bodyContent)
        message.notificationMessageSections << bodySection
        bodySection.save()
    }

    def String getMessage(String code) {
        return messageSource.getMessage(code, new Object[0], code, locale)
    }

    def String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, code, locale)
    }
}
