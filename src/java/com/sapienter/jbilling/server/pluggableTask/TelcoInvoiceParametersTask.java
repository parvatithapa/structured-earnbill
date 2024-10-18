package com.sapienter.jbilling.server.pluggableTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;

import static com.sapienter.jbilling.common.Util.getSysProp;

import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.integration.common.utility.DateUtility;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDAS;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDTO;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDAS;
import com.sapienter.jbilling.server.process.event.CustomInvoiceFieldsEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Util;
/**
 * This plugin handles the Telco invoice template fields and parameters
 * to pass invoice summary specific parameters
 *  
 * @author Ashok Kale
 * @since  03-Jan-2016
 */

public class TelcoInvoiceParametersTask extends PluggableTask
implements IInternalEventsTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(TelcoInvoiceParametersTask.class));
	private static final String FEES_PRODUCT_CATEGORY_ID = "Fees Product Category ID";
    private static final String ADJUSTMENTS_PRODUCT_CATEGORY_ID = "Adjustments Product Category ID";
    private static final String INVOICE_TEMPLATE_PAYMENT_BUTTON_NAME = "Invoice Template Payment Button Name";
    private static final String INVOICE_TEMPLATE_PAYMENT_URL = "Invoice Template Payment URL";
    private static final String INVOICE_TEMPLATE_COLOR_CODE = "Invoice Template Color Code";
    private static final MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
    private static final String BASE_DIR = getSysProp("base_dir");

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
    	CustomInvoiceFieldsEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
	
	public void process(Event event) throws PluggableTaskException {
		
		if (!(event instanceof CustomInvoiceFieldsEvent)) {
            throw new PluggableTaskException("Cannot process event " + event);
        }
		
		LOG.debug("Processing " + event);
		
		CustomInvoiceFieldsEvent customInvoiceFieldsEvent = (CustomInvoiceFieldsEvent) event;
        
        try {
        	Integer userId = customInvoiceFieldsEvent.getUserId();
        	Integer entityId = customInvoiceFieldsEvent.getEntityId();
        	Map<String, Object> parameters = customInvoiceFieldsEvent.getParameters();
        	InvoiceDTO invoice = customInvoiceFieldsEvent.getInvoice();
        	ContactDTOEx to = customInvoiceFieldsEvent.getTo();
        	
            generatePaperInvoiceReport(entityId, userId, parameters, to, invoice);
        } catch (Exception e){
        	LOG.error("Exception while generating paper invoice in process method", e);
        	throw new PluggableTaskException("Message or parameters may be null");
        }
	}

    private static void generatePaperInvoiceReport(Integer entityId, Integer userId, Map<String, Object> parameters, 
    		ContactDTOEx to, InvoiceDTO invoice) throws FileNotFoundException, SessionInternalError {
    	
    	UserDTO user = new UserDAS().find(userId);
    	Integer customerId = user.getCustomer().getId();
    	setAllContanctDetails(parameters, customerId, to, entityId, user.getCustomer().getAccountType().getId());
    	try{
    		// symbol of the currency
            String symbol = evaluateCurrencySymbol(invoice.getCurrency().getId());
            parameters.put("core.param.currency_symbol",symbol);
    		parameters.put("core.param.userId", userId);
    		parameters.put("core.param.dueDate", Util.formatDate(invoice.getDueDate(), userId));
    		parameters.put("core.param.serviceNumber", getServiceNumber(entityId));
    		
    		// Logo for invoice template
    		parameters.put("core.param.companyLogo", getCompanyLogo(entityId));
    		// There are conditions dependent on entityId parameter.
    		parameters.put("entityId", entityId);
    		parameters.put("core.param.entityId", entityId);
    		parameters.put("core.param.companyName", new EntityBL(entityId).getEntity().getDescription());
    		
    		// Company address detail parameters
    		ContactDTO companyDetails = new ContactDAS().findEntityContact(entityId);
    		parameters.put("core.param.companyAddress1", companyDetails.getAddress1());
    		parameters.put("core.param.companyAddress2", companyDetails.getAddress2());
    		parameters.put("core.param.companyCity", companyDetails.getCity());
    		parameters.put("core.param.companyPostalCode", companyDetails.getPostalCode());
    		parameters.put("core.param.companyStateCode", companyDetails.getStateProvince());
    		parameters.put("core.param.lateFeePercentage", getLateFeePercentage(entityId));
    		parameters.put("core.param.accountNumber", getPrimaryAccountNumber(userId, entityId)); 
    		parameters.put("core.param.billing_cycle", getCustomerBillingCycle(customerId));
    		
    		// Population of Invoice summary parameters
    		InvoiceSummaryDTO invoiceSummary = new InvoiceSummaryDAS().findInvoiceSummaryByInvoice(invoice.getId());
    		if (null != invoiceSummary) {
        		parameters.put("core.param.total_due", invoiceSummary.getTotalDue().setScale(2, BigDecimal.ROUND_HALF_UP));
        		parameters.put("core.param.amount_of_last_statement", invoiceSummary.getAmountOfLastStatement().setScale(2, BigDecimal.ROUND_HALF_UP));
        		parameters.put("core.param.payment_received", invoiceSummary.getPaymentReceived().setScale(2, BigDecimal.ROUND_HALF_UP));
        		parameters.put("core.param.new_charges", invoiceSummary.getNewCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        		parameters.put("core.param.monthly_charges", invoiceSummary.getMonthlyCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        		parameters.put("core.param.usage_charges", invoiceSummary.getUsageCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
        		BigDecimal taxesAndFees = invoiceSummary.getFees().add(invoiceSummary.getTaxes()).setScale(2, BigDecimal.ROUND_HALF_UP);
        		parameters.put("core.param.taxes_and_fees", taxesAndFees);
        		parameters.put("core.param.adjustment_charges", invoiceSummary.getAdjustmentCharges().setScale(2, BigDecimal.ROUND_HALF_UP));
                parameters.put("core.param.last_invoice_date",(null != invoiceSummary.getLastInvoiceDate() ? invoiceSummary.getLastInvoiceDate() : Util.getEpochDate()));
        		parameters.put("core.param.creation_invoice_id", invoiceSummary.getCreationInvoiceId());
                parameters.put("core.param.late.fee", getLateFee(invoiceSummary.getTotalDue(), getLateFeePercentage(entityId)));
    		}
            parameters.put("core.param.invoice_date", invoice.getCreateDatetime());
            String feesProductCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(FEES_PRODUCT_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(feesProductCategoryId)) {
                parameters.put("core.param.feesProductCategoryId", Integer.valueOf(feesProductCategoryId));
            }
            String adjustmentsProductCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(ADJUSTMENTS_PRODUCT_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(adjustmentsProductCategoryId)) {
                parameters.put("core.param.adjustmentsProductCategoryId", Integer.valueOf(adjustmentsProductCategoryId));
            }
            String invoiceTemplatePaymentButtonName = metaFieldDAS.getComapanyLevelMetaFieldValue(INVOICE_TEMPLATE_PAYMENT_BUTTON_NAME, entityId);
            parameters.put("core.param.invoice_template_payment_button_name", null != invoiceTemplatePaymentButtonName && !invoiceTemplatePaymentButtonName.isEmpty() ? invoiceTemplatePaymentButtonName : "Click Here to Pay Online");
            String paymentURL = metaFieldDAS.getComapanyLevelMetaFieldValue(INVOICE_TEMPLATE_PAYMENT_URL, entityId);
            if (StringUtils.isNotEmpty(paymentURL)) {
                parameters.put("core.param.invoice_template_payment_url", paymentURL);
            }
            String colorCode = metaFieldDAS.getComapanyLevelMetaFieldValue(INVOICE_TEMPLATE_COLOR_CODE, entityId);
            if (StringUtils.isNotEmpty(colorCode)) {
                parameters.put("core.param.invoice_template_color_code", colorCode);
            }
            parameters.put("BASE_DIR", BASE_DIR);

            Optional<OrderProcessDTO> startDate = invoice.getOrderProcesses().stream()
                    .filter(process -> null != process.getPeriodStart())
                    .min(Comparator.comparing(OrderProcessDTO :: getPeriodStart));
            Date invoicePeriodStartDate = startDate.isPresent() ? startDate.get().getPeriodStart() : null; 

            Optional<OrderProcessDTO> endDate = invoice.getOrderProcesses().stream()
                    .filter(process -> null != process.getPeriodEnd())
                    .max(Comparator.comparing(OrderProcessDTO :: getPeriodEnd));
            Date invoicePeriodEndDate = endDate.isPresent() ? endDate.get().getPeriodEnd() : null;

            parameters.put("core.param.invoice_period.start_date", invoicePeriodStartDate);
            parameters.put("core.param.invoice_period.end_date", DateUtility.addDaysToDate(invoicePeriodEndDate,-1));
            boolean isPrepaid = false;
            List<Date> activeSinceList = new ArrayList<>();
            for (OrderProcessDTO orderProcess : invoice.getOrderProcesses()) {
                OrderDTO orderDTO = orderProcess.getPurchaseOrder();
                if (orderDTO.getOrderPeriod().getDescription(invoice.getBaseUser().getLanguage().getId()).equals("Monthly") && 
                        orderDTO.getOrderBillingType().getDescription(invoice.getBaseUser().getLanguage().getId()).equals("pre paid")) {
                    isPrepaid = true;
                }
                if (orderDTO.getIsMediated()) {
                    activeSinceList.add(orderDTO.getActiveSince());
                }
            }
            if (isPrepaid) {
                if (CollectionUtils.isNotEmpty(activeSinceList)) {
                    Collections.sort(activeSinceList);
                    parameters.put("core.param.usage_period.start_date", activeSinceList.get(0));
                }
                parameters.put("core.param.usage_period.end_date", DateUtility.addDaysToDate(invoice.getCreateDatetime(), -1));
            } else {
                parameters.put("core.param.usage_period.start_date", invoicePeriodStartDate);
                parameters.put("core.param.usage_period.end_date", DateUtility.addDaysToDate(invoicePeriodEndDate,-1));
            }
		} catch (Exception e) {
			LOG.error("Exception while generating paper invoice in setting invoice summary details", e);
			throw new SessionInternalError("Error getting invoice", e);
    	}
	}

	private static String getPrimaryAccountNumberByCustomerIdAndEntityId(Integer userId, Integer entityId) {
    	if(null == userId || null == entityId) {
    		return null;
    	}
    	return new CustomerDAS().getPrimaryAccountNumberByUserAndEntityId(userId, entityId);
    }
	
    private static String evaluateCurrencySymbol(Integer currencyId) {
        CurrencyBL currency = new CurrencyBL(currencyId);
        String symbol = currency.getEntity().getSymbol();
        if (symbol.length() >= 4 && symbol.charAt(0) == '&' &&
                symbol.charAt(1) == '#') {
            // this is an html symbol
            // remove the first two digits
            symbol = symbol.substring(2);
            // remove the last digit (;)
            symbol = symbol.substring(0, symbol.length() - 1);
            // convert to a single char
            Character ch = new Character((char)
                    Integer.valueOf(symbol).intValue());
            symbol = ch.toString();
        }
        return symbol;
    }
    
    private static File getCompanyLogo(Integer entityId) {
        String LOGO_JPG_LOCATION_PATH = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "logos/entity-%d.jpg";
        String LOGO_PNG_LOCATION_PATH = com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "logos/entity-%d.png";
        
        File jpgLogo = new File(String.format(LOGO_JPG_LOCATION_PATH, entityId));
        File pngLogo = new File(String.format(LOGO_PNG_LOCATION_PATH, entityId));
        
        if (jpgLogo.exists()) {
			return jpgLogo;
		} else {
			return pngLogo;
		}
    }
    
    private static String getServiceNumber(Integer entityId) {
		String serviceNumber = "";
		ContactDTO adminDetails = new ContactDAS().findByEntityAndUserName("admin", entityId);
		if (null != adminDetails && null != adminDetails.getPhoneCountryCode() && 
	    	null != adminDetails.getPhoneAreaCode() && !adminDetails.getPhoneNumber().isEmpty()) {
    		serviceNumber =	adminDetails.getPhoneCountryCode() +"-"+
    						adminDetails.getPhoneAreaCode() +"-"+
    						adminDetails.getPhoneNumber();
    	}
		return serviceNumber;
    	
    }
    
    /**
     * Get late fee percentage by entityId and plugin parameter
     * @param entityId
     * @return
     */
    private static BigDecimal getLateFeePercentage(Integer entityId) {
    	BigDecimal lateFeePercentage = BigDecimal.ZERO;
		String penaltyItemId = new PluggableTaskParameterDAS().getPenaltyItemId(entityId);
		if (null != penaltyItemId) {
    		// Retrieve late fee percentage
			ItemDTO item = new ItemDAS().find(Integer.valueOf(penaltyItemId));
			lateFeePercentage = item.getPrice(new Date(),entityId).getRate();
    		lateFeePercentage = null != lateFeePercentage ? lateFeePercentage : BigDecimal.ZERO;
    	}
		LOG.debug("Late Fee Percentage "+lateFeePercentage);
		return lateFeePercentage;
    }
    
    private static BigDecimal getLateFee(BigDecimal totalDue, BigDecimal lateFeePercentage) {
		BigDecimal lateFee = BigDecimal.ZERO;
		lateFee = totalDue.multiply(lateFeePercentage).divide(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
		return (null != lateFee ? lateFee : BigDecimal.ZERO);
    }

    /**
     * Get customer primary account number using userId and entityId
     * @param userId
     * @param entityId
     * @return String
     */
    private static String getPrimaryAccountNumber(Integer userId, Integer entityId) {
    	String primaryAccountNumber = getPrimaryAccountNumberByCustomerIdAndEntityId(userId, entityId);
        return ((null != primaryAccountNumber && !primaryAccountNumber.isEmpty()) ? primaryAccountNumber : "");
    }
    
    /**
     * Get customer billing cycle
     * @param customerId
     * @return String
     */
    @SuppressWarnings("deprecation")
    private static String getCustomerBillingCycle(Integer customerId) {
    	CustomerBL customer = new CustomerBL(customerId);
        return (null != customer.getEntity() ? customer.getEntity().getMainSubscription().getSubscriptionPeriod().getDescription() :null);
        
    }

    /**
     * Set all contact related parameters
     * @param parameters
     * @param user
     * @param to
     * @param entityId
     */
    private static void setAllContanctDetails(Map<String, Object> parameters, Integer customerId, ContactDTOEx to, Integer entityId, Integer accountTypeId) {
        try {
            String mailingGroupNameValue = new MetaFieldDAS().getComapanyLevelMetaFieldValue(MetaFieldName.NOTIFICATION_MAILING_ADDRESS.getMetaFieldName(), entityId);
            MetaFieldGroup metaFieldGroup = new AccountInformationTypeDAS().getGroupByNameAndEntityId(entityId, EntityType.ACCOUNT_TYPE, mailingGroupNameValue, accountTypeId);
            if(null == metaFieldGroup || null == to) {
                return ;
            }
            
            MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
            MetaFieldValueDAS metaFieldValueDAS = new MetaFieldValueDAS();

            Date effectiveDate = TimezoneHelper.companyCurrentDate(entityId);

            List<MetaFieldValue<?>> organizationFieldValues = new ArrayList<>();

            for(Integer valueFieldId : metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.ORGANIZATION, metaFieldGroup.getId(), effectiveDate)) {
                organizationFieldValues.add(metaFieldValueDAS.find(valueFieldId));
            }

            Collections.sort(organizationFieldValues, (v1 , v2) -> v1.getField().getDisplayOrder().compareTo(v2.getField().getDisplayOrder()));

            StringBuilder firstNameLastName = new StringBuilder();

            for(MetaFieldValue<?> value: organizationFieldValues) {
                Object strValue = value.getValue();
                firstNameLastName.append(Objects.nonNull(strValue) ? strValue.toString() + " " : "");
            }

            Map<String, String> aitMetaFieldsBYUsage = metaFieldDAS
                    .getCustomerAITMetaFieldValueMapByMetaFieldType(customerId, metaFieldGroup.getId(), effectiveDate);

            LOG.debug(" %s meta fields fetched for customer %s for effective date %s ", aitMetaFieldsBYUsage, customerId, effectiveDate);

            parameters.put("customer.name", firstNameLastName.toString());
            parameters.put("core.param.customer.organization", Objects.nonNull(to.getOrganizationName()) ? to.getOrganizationName() : "");

            parameters.put("core.param.title", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.TITLE.toString(), ""));

            String firstName = aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.FIRST_NAME.toString(), "");
            String lastName = aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.LAST_NAME.toString(), "");

            parameters.put("core.param.first.name", firstName.trim());
            parameters.put("core.param.last.name", lastName.trim());

            parameters.put("core.param.customer.address",  aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.ADDRESS1.toString(), ""));
            parameters.put("core.param.customer.address2", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.ADDRESS2.toString(), ""));
            parameters.put("core.param.customer.city", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.CITY.toString(), ""));
            parameters.put("core.param.customer.province", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.STATE_PROVINCE.toString(), ""));
            parameters.put("core.param.customer.postalCode", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.POSTAL_CODE.toString(), ""));

        } catch (Exception e) {
            LOG.error("Exception while generating paper invoice in setting contanct details", e);
        }
    }

}
