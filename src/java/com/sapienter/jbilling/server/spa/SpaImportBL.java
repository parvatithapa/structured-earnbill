package com.sapienter.jbilling.server.spa;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDAS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeStatusBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.event.DistributelNewCustomerEvent;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDTO;
import com.sapienter.jbilling.server.payment.tasks.PaymentPaySafeTask;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeProcessedPaymentEvent;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeResultType;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeStatus;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserHelperDisplayerDistributel;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.SubscriberStatusDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.EnumerationBL;
import com.sapienter.jbilling.server.util.db.EnumerationDTO;
import com.sapienter.jbilling.server.util.db.EnumerationValueDTO;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 * Created by pablo_galera on 10/01/17.
 */
public class SpaImportBL {

    private static final FormatLogger log = new FormatLogger(
            Logger.getLogger(SpaImportBL.class));

    private SpaImportWS spaImportWS;
    private Integer entityId;
    private Integer accountTypeId;
    private Integer accountTypeCurrency;
    private boolean recordPayment = false;
    private boolean sendNotification = false;
    Map<OrderWS, OrderChangeWS[]> orders = new HashMap();

    private static final String PROCESSING_INFORMATION_AIT = "Processing Information";
    public static String ASSET_RESERVED_STATUS = "Reserved";
    private RouteDAS routeDAS;
    private OrderDAS orderDAS;
    private NotificationBL notificationBL;
    private AssetDAS assetDAS;
    private UserDAS userDAS;
    private CustomerDAS customerDAS;
    private MetaFieldDAS metaFieldDAS;
    private PaymentDAS paymentDAS;
    private OrderWS mainOfferOrderForProcessCenter;
    private String phoneNumberAssetIdentifier;
    private String portalUserName;
    private String portalPassword;
    private static final String SEPARATOR = ",";
    
    public SpaImportBL() {
        routeDAS = new RouteDAS();
        orderDAS = new OrderDAS();
        assetDAS = new AssetDAS();
        userDAS = new UserDAS();
        customerDAS = new CustomerDAS();
        notificationBL = new NotificationBL();
        paymentDAS = new PaymentDAS();

    }

    public SpaImportBL(SpaImportWS spaImportWS, Integer entityId) {
        this();
        this.spaImportWS = spaImportWS;
        this.entityId = entityId;
    }

    public void processInitial() {
        log.info("Logging SpaImportWS\n");
        log.info(spaImportWS.toString());
    }


    public static FormatLogger getLog() {
        return log;
    }

    public Integer getAccountTypeId() {
        return accountTypeId;
    }

    public void setAccountTypeId(Integer accountTypeId) {
        this.accountTypeId = accountTypeId;
    }

    public Integer getAccountTypeCurrency() {
        return accountTypeCurrency;
    }

    public void setAccountTypeCurrency(Integer accountTypeCurrency) {
        this.accountTypeCurrency = accountTypeCurrency;
    }

    public UserWS getCustomer() {
        String missingInformation = validateNonMandatoryAndMissingInformation();
        AccountTypeDTO accountType = new AccountTypeDAS().findAccountTypeByName(entityId, SpaConstants.ACCOUNT_TYPE_RESIDENTIAL);
        AccountInformationTypeDTO contactInformationAIT = accountType.getInformationTypes().stream().filter(at -> at.getName().equals(SpaConstants.CONTACT_INFORMATION_AIT)).findFirst().get();
        Integer contactInformationAITid = contactInformationAIT.getId();
        accountTypeId = accountType.getId();
        accountTypeCurrency = accountType.getCurrencyId();
        UserWS user = new UserWS();
        StringBuffer name = new StringBuffer(SpaConstants.CUSTOMER_NAME_PREFIX).append("_").append(System.currentTimeMillis()); //will be updated right away after creation 
        user.setUserName(name.toString());
        user.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        user.setAccountTypeId(accountTypeId);
        user.setLanguageId(SpaImportHelper.getLanguageId(spaImportWS.getLanguage()));
        user.setMainRoleId(Constants.TYPE_CUSTOMER);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        user.setCurrencyId(accountTypeCurrency);
        user.setSubscriberStatusId(new SubscriberStatusDAS().find(UserDTOEx.SUBSCRIBER_ACTIVE).getId());
        user.setInvoiceTemplateId(getInvoiceTemplateId());
        setMainSubscription(user);
        boolean spaEnrollmentIncomplete = false;
        if (!StringUtils.isEmpty(spaImportWS.getRequiredAdjustmentDetails())) {
            missingInformation += "\nAdjustment required: " + spaImportWS.getRequiredAdjustmentDetails();
        }
        if (!StringUtils.isEmpty(missingInformation)) {
            spaEnrollmentIncomplete = true;
        }
        // Set payment information
        PaymentInformationWS paymentInformationWS = this.getPaymentInformationWS();
        user.getPaymentInstruments().add(paymentInformationWS);

        List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();

        setCustomerMetafield(metaFieldValues, spaEnrollmentIncomplete, missingInformation);

        setContactInformationAITMetaFields(contactInformationAITid, metaFieldValues);

        setOtherAddressesAITMetaFields(metaFieldValues);

        setProcessingInformationAITMetaFields(metaFieldValues);

        user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[metaFieldValues.size()]));

        return user;
    }

    public Integer getInvoiceTemplateId(Integer entityId, String language ) {

        CompanyDTO company = new CompanyDAS().find(entityId);

        String invoiceTemplateName = SpaConstants.MF_ENGLISH_INVOICE_TEMPLATE_NAME;
        if (SpaConstants.FRENCH_LANGUAGE.equals(language)) {
            invoiceTemplateName = SpaConstants.MF_FRENCH_INVOICE_TEMPLATE_NAME;
        }

        MetaFieldValue invoiceTemplateNameMF = company.getMetaField(invoiceTemplateName);
        if (invoiceTemplateNameMF != null) {
            return new InvoiceTemplateDAS().getDefaultTemplateId((String) invoiceTemplateNameMF.getValue(), entityId);
        }
        return null;
    }

    public void updateAddresses(UserDTO userDTO) {
        Date startDate = spaImportWS.getProductsOrdered().get(0).getStartDate();
        if (!DateConvertUtils.asLocalDate(startDate).isAfter(TimezoneHelper.companyCurrentLDT(userDTO.getCompany().getId()).toLocalDate())) {
            // logic for when is NOT a future order 
            updateCustomerAddresses(userDTO, SpaConstants.CONTACT_INFORMATION_AIT, AddressType.BILLING);
            updateCustomerAddresses(userDTO, SpaConstants.SERVICE_ADDRESS_AIT, AddressType.SERVICE);
            updateCustomerAddresses(userDTO, SpaConstants.SHIPPING_ADDRESS_AIT, AddressType.SHIPPING);
            updateCustomerAddresses(userDTO, SpaConstants.PORTING_ADDRESS_AIT, AddressType.PORTING);
        } else {
            // logic for when is a future order 
            addCustomerAddress(userDTO, startDate, SpaConstants.CONTACT_INFORMATION_AIT, AddressType.BILLING);
            addCustomerAddress(userDTO, startDate, SpaConstants.SERVICE_ADDRESS_AIT, AddressType.SERVICE);
            addCustomerAddress(userDTO, startDate, SpaConstants.SHIPPING_ADDRESS_AIT, AddressType.SHIPPING);
            addCustomerAddress(userDTO, startDate, SpaConstants.PORTING_ADDRESS_AIT, AddressType.PORTING);
        }        
    }
    
    public void updateCustomerAddresses(UserDTO userDTO, String accountInformationTypeName, AddressType addressType) {
        AccountInformationTypeDTO addressAIT = new AccountInformationTypeDAS().findByName(accountInformationTypeName, entityId, accountTypeId);
        SpaAddressWS address = spaImportWS.getAddress(addressType);
        
        if (address != null) {
            CustomerDTO customer = userDTO.getCustomer();
            boolean needUpdate = populateCustomerAccountInfoTypeMetaField(SpaConstants.POSTAL_CODE, address.getPostalCode(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.MF_STREET_NUMBER_SUFFIX, address.getStreetNumberSufix(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.MF_STREET_NUMBER, address.getStreetNumber(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.MF_STREET_NAME, address.getStreetName(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.MF_STREET_TYPE, address.getStreetType(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.MF_STREET_DIRECTION, address.getStreetDirecton(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.MF_APT_SUITE, address.getStreetAptSuite(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.CITY, address.getCity(), customer, addressAIT) |
                                 populateCustomerAccountInfoTypeMetaField(SpaConstants.PROVINCE, address.getProvince(), customer, addressAIT);

            if(AddressType.BILLING.equals(addressType)){
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String emailverified = dateFormat.format(spaImportWS.getEmailVerified());
                needUpdate = populateCustomerAccountInfoTypeMetaField(SpaConstants.CUSTOMER_NAME, spaImportWS.getCustomerName(), customer, addressAIT) |
                        populateCustomerAccountInfoTypeMetaField(SpaConstants.CUSTOMER_COMPANY, spaImportWS.getCustomerCompany(), customer, addressAIT) |
                        populateCustomerAccountInfoTypeMetaField(SpaConstants.PHONE_NUMBER_1,spaImportWS.getPhoneNumber1(), customer, addressAIT) |
                        populateCustomerAccountInfoTypeMetaField(SpaConstants.PHONE_NUMBER_2,spaImportWS.getPhoneNumber2(), customer, addressAIT) |
                        populateCustomerAccountInfoTypeMetaField(SpaConstants.EMAIL_VERIFIED,emailverified, customer, addressAIT) |
                        populateCustomerAccountInfoTypeMetaField(SpaConstants.EMAIL_ADDRESS,spaImportWS.getEmailAddress(), customer, addressAIT);
            }
            
            if (needUpdate) {
                for (Map.Entry<Integer, List<MetaFieldValue>> entry : customer.getAitMetaFieldMap().entrySet()) {
                    if (addressAIT.getId() == entry.getKey()) {
                        for (MetaFieldValue value : entry.getValue()) {
                            customer.insertCustomerAccountInfoTypeMetaField(value, addressAIT, CommonConstants.EPOCH_DATE);
                        }
                    }
                }
            }
            customerDAS.save(customer);
        }
    }

    /**
     * Function to create a new MetaFieldValue when customerAccountInfoTypeMetaField does not exist for a specific metafield name and group id.
     * If that exists, it is updated with the new value.
     * The method returns a boolean to know if some metafield value was created and it is necessary insert it to the customerAccountInfoTypeMetaFields set. 
     * @param metaFieldName metafield name
     * @param newValue new value
     * @param customer customer
     * @param addressAIT address account info type
     * @return boolean
     */
    private boolean populateCustomerAccountInfoTypeMetaField(String metaFieldName, String newValue, CustomerDTO customer, AccountInformationTypeDTO addressAIT) {
        CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = customer.getCustomerAccountInfoTypeMetaFields().stream()
                .filter(customerAccount -> customerAccount.getAccountInfoType().getId() == addressAIT.getId() &&
                        metaFieldName.equals(customerAccount.getMetaFieldValue().getField().getName()))
                .findFirst()
                .orElse(null);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        if (customerAccountInfoTypeMetaField == null) {
            if (StringUtils.isEmpty(newValue)) {
                return false;
            }
            if (SpaConstants.EMAIL_VERIFIED.equals(metaFieldName)){
                try {
                     customer.setAitMetaField(entityId, addressAIT.getId(), metaFieldName, dateFormat.parse(newValue));
                     return true;
                } catch (ParseException e) {
                     log.error("error while updating Email Verified Time Stamp metafield {} ", e.getMessage());
                }
            }
            customer.setAitMetaField(entityId, addressAIT.getId(), metaFieldName, newValue);
            return true;
        }
        if (SpaConstants.EMAIL_VERIFIED.equals(metaFieldName)){
	        try {
                 customerAccountInfoTypeMetaField.getMetaFieldValue().setValue(dateFormat.parse(newValue));
				 return false;
			} catch (ParseException e) {
				log.error("error while updating Email Verified Time Stamp metafield {} ", e.getMessage());
			}
        }
        customerAccountInfoTypeMetaField.getMetaFieldValue().setValue(newValue);
        return false;
    }

    @SuppressWarnings("rawtypes")
	public void addCustomerAddress(UserDTO userDTO, Date effectiveDate, String aitName, AddressType addressType) {
    	AccountInformationTypeDTO ait = new AccountInformationTypeDAS().findByName(aitName, entityId, accountTypeId);
    	if(ait != null) {
        SpaAddressWS spaAddress = spaImportWS.getAddress(addressType);
        CustomerDTO customer = userDTO.getCustomer();
        Integer groupId = ait.getId();
        if (spaAddress != null) {
        	setAdditionalAddressesAITMetaFields(customer,groupId);
            customer.setAitMetaField(entityId, groupId, SpaConstants.POSTAL_CODE, spaAddress.getPostalCode());
            customer.setAitMetaField(entityId, groupId, SpaConstants.STREET_NUMBER_SUFFIX, spaAddress.getStreetNumberSufix());
            customer.setAitMetaField(entityId, groupId, SpaConstants.STREET_NUMBER, spaAddress.getStreetNumber());
            customer.setAitMetaField(entityId, groupId, SpaConstants.STREET_NAME, spaAddress.getStreetName());
            customer.setAitMetaField(entityId, groupId, SpaConstants.STREET_TYPE, spaAddress.getStreetType());
            customer.setAitMetaField(entityId, groupId, SpaConstants.STREET_DIRECTION, spaAddress.getStreetDirecton());
            customer.setAitMetaField(entityId, groupId, SpaConstants.MF_APT_SUITE, spaAddress.getStreetAptSuite());
            customer.setAitMetaField(entityId, groupId, SpaConstants.PROVINCE, spaAddress.getProvince());
            customer.setAitMetaField(entityId, groupId, SpaConstants.CITY, spaAddress.getCity());
  
            if (AddressType.EMERGENCY.equals(addressType)) {
                customer.setAitMetaField(entityId, groupId, SpaConstants.MF_PROVIDED, true);
                customer.setAitMetaField(entityId, groupId, SpaConstants.MF_REQUIRED, true);
            }

            if(AddressType.BILLING.equals(addressType)){
                customer.setAitMetaField(entityId, groupId,SpaConstants.CUSTOMER_NAME, spaImportWS.getCustomerName());
                customer.setAitMetaField(entityId, groupId,SpaConstants.CUSTOMER_COMPANY, spaImportWS.getCustomerCompany());
                customer.setAitMetaField(entityId, groupId,SpaConstants.PHONE_NUMBER_1,spaImportWS.getPhoneNumber1());
                customer.setAitMetaField(entityId, groupId,SpaConstants.PHONE_NUMBER_2,spaImportWS.getPhoneNumber2());
                customer.setAitMetaField(entityId, groupId,SpaConstants.EMAIL_ADDRESS,spaImportWS.getEmailAddress());
                customer.setAitMetaField(entityId, groupId,SpaConstants.EMAIL_VERIFIED,spaImportWS.getEmailVerified());
            }

            for (Map.Entry<Integer, List<MetaFieldValue>> entry : customer.getAitMetaFieldMap().entrySet()) {
                if (groupId.equals(entry.getKey())) {
                    for (MetaFieldValue value : entry.getValue()) {
                        customer.addCustomerAccountInfoTypeMetaField(value, ait, effectiveDate);
                    }
                }
            }
            customerDAS.save(customer);
		} else {
			if (!AddressType.EMERGENCY.equals(addressType)) {
				setAdditionalAddressesAITMetaFields(customer, groupId);
				for (Map.Entry<Integer, List<MetaFieldValue>> entry : customer.getAitMetaFieldMap().entrySet()) {
					if (groupId.equals(entry.getKey())) {
						for (MetaFieldValue value : entry.getValue()) {
							if (value.getFieldName().equals(SpaConstants.SAME_AS_CUSTOMER_INFORMATION)) {
								customer.addCustomerAccountInfoTypeMetaField(value, ait, effectiveDate);
							}
						}
					}
				}
				customerDAS.save(customer);
			}
		}
    	} else {
    	   log.debug("AccountInformationType is not found for address type %s ", aitName);
       }
    }


    private Integer getInvoiceTemplateId() {
        return this.getInvoiceTemplateId(entityId, spaImportWS.getLanguage());
    }

    private void setOtherAddressesAITMetaFields(List<MetaFieldValueWS> metaFieldValues) {
        AccountInformationTypeDTO shippingAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SHIPPING_ADDRESS_AIT, entityId, accountTypeId);
        SpaAddressWS address;

        if (shippingAddressAIT != null) {
            address = spaImportWS.getAddress(AddressType.SHIPPING);
            if(address==null) {
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, shippingAddressAIT.getId(), DataType.BOOLEAN, false, true));
            }else{
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, shippingAddressAIT.getId(), DataType.BOOLEAN, false, false));
                addMetaFieldAddressInformation(metaFieldValues,shippingAddressAIT.getId(),address);
            }
        }
        AccountInformationTypeDTO serviceAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SERVICE_ADDRESS_AIT, entityId, accountTypeId);

        if (serviceAddressAIT != null) {
            address = spaImportWS.getAddress(AddressType.SERVICE);
            if(address==null){
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, serviceAddressAIT.getId(), DataType.BOOLEAN, false, true));
            }else {
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, serviceAddressAIT.getId(), DataType.BOOLEAN, false, false));
                addMetaFieldAddressInformation(metaFieldValues,serviceAddressAIT.getId(),address);
            }
        }
        AccountInformationTypeDTO emergencyAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.EMERGENCY_ADDRESS_AIT, entityId, accountTypeId);

        if (emergencyAddressAIT != null) {
            boolean provided = false;
            address = spaImportWS.getAddress(AddressType.EMERGENCY);
            if (address != null) {
                addMetaFieldAddressInformation(metaFieldValues, emergencyAddressAIT.getId(), address);
            }
            if (isVOIPService()) {
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_REQUIRED, emergencyAddressAIT.getId(), DataType.BOOLEAN, false, true));
                if (address != null) {
                    provided = true;
                }
            } else {
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_REQUIRED, emergencyAddressAIT.getId(), DataType.BOOLEAN, false, false));
            }
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_PROVIDED, emergencyAddressAIT.getId(), DataType.BOOLEAN, false, provided));
        }
        AccountInformationTypeDTO portingAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.PORTING_ADDRESS_AIT, entityId, accountTypeId);

        if (portingAddressAIT != null) {
            address = spaImportWS.getAddress(AddressType.PORTING);
            if(address==null){
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, portingAddressAIT.getId(), DataType.BOOLEAN, false, true));
            }else {
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, portingAddressAIT.getId(), DataType.BOOLEAN, false, false));
                addMetaFieldAddressInformation(metaFieldValues,portingAddressAIT.getId(),address);
            }
        }
    }

    private boolean isVOIPService() {
        for (SpaProductsOrderedWS productsOrdered : spaImportWS.getProductsOrdered()) {
            if (ServiceType.VOIP.name().equals(productsOrdered.getServiceType())) {
                return true;
            }
        }
        return false;
    }

    private void addMetaFieldAddressInformation(List<MetaFieldValueWS> metaFieldValues, Integer groupId, SpaAddressWS address){
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.POSTAL_CODE, groupId, DataType.STRING, false, address.getPostalCode()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CITY, groupId, DataType.STRING, false, address.getCity()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_NUMBER_SUFFIX, groupId, DataType.STRING, false, address.getStreetNumberSufix()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_NUMBER, groupId, DataType.STRING, false, address.getStreetNumber()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_NAME, groupId, DataType.STRING, false, address.getStreetName()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_TYPE, groupId, DataType.STRING, false, address.getStreetType()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_DIRECTION, groupId, DataType.STRING, false, address.getStreetDirecton()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_APT_SUITE, groupId, DataType.STRING, false, address.getStreetAptSuite()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PROVINCE, groupId, DataType.STRING, false, address.getProvince()));
    }

    private void setProcessingInformationAITMetaFields(List<MetaFieldValueWS> metaFieldValues) {
        AccountInformationTypeDTO processingInformationAIT = new AccountInformationTypeDAS().findByName(PROCESSING_INFORMATION_AIT, entityId, accountTypeId);
        if (processingInformationAIT != null) {
            if (!StringUtils.isEmpty(spaImportWS.getConfirmationNumber())) {
                metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CONFIRMATION_NUMBER, processingInformationAIT.getId(), DataType.STRING, false, spaImportWS.getConfirmationNumber()));
            }
            for (SpaProductsOrderedWS spaProductsOrdered : spaImportWS.getProductsOrdered()) {
                if (spaProductsOrdered.isManualProcess()) {
                    metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PROCESS_CENTER, processingInformationAIT.getId(), DataType.STRING, false, spaProductsOrdered.getProcessCenter()));
                    metaFieldValues.add(new MetaFieldValueWS(SpaConstants.HEX_MESSAGE, processingInformationAIT.getId(), DataType.STRING, false, decodeHexMessage(spaProductsOrdered.getHexencodedMessage())));
                }
            }
        }
    }

    public void dispatchNewProcessSpaImportEvent(Integer userId) {
        portalUserName = getPortalUserName();
        portalPassword = getPortalPassword();
        UserDTO user = userDAS.find(userId);
        DistributelNewCustomerEvent distributelNewCustomerEvent = new DistributelNewCustomerEvent(user.getCompany().getId(), user);
        setNewCustomerEventSpaProperties(distributelNewCustomerEvent);

        for (SpaProductsOrderedWS spaProductsOrdered : spaImportWS.getProductsOrdered()) {
            distributelNewCustomerEvent.getParameters().put(ServiceType.class.getName(), spaProductsOrdered.getServiceType());
            EventManager.process(distributelNewCustomerEvent);

            if (spaProductsOrdered.isManualProcess()) {
                sendMessageToProccescenter(user, spaProductsOrdered);
            }

            // Create Request to Update 911 Emergency Address
            if(!StringUtils.isEmpty(spaProductsOrdered.getPhoneNumber())){
                Distributel911AddressUpdateEvent addressUpdateEvent = Distributel911AddressUpdateEvent.
                        createEventForAddingNewPhoneNumber(user.getCompany().getId(), user.getId(), spaProductsOrdered.getPhoneNumber());
                EventManager.process(addressUpdateEvent);
            }
        }
    }

    private void setNewCustomerEventSpaProperties(DistributelNewCustomerEvent newCustomerEvent) {
        newCustomerEvent.getParameters().put(SpaConstants.MSGDTO_PARAMETER_PORTAL_USERNAME, portalUserName);
        newCustomerEvent.getParameters().put(SpaConstants.MSGDTO_PARAMETER_PORTAL_PASSWORD, portalPassword);
        for (SpaProductsOrderedWS spaProductsOrdered : spaImportWS.getProductsOrdered()) {
            if (spaProductsOrdered.isPPPOE()) {
                newCustomerEvent.getParameters().put(SpaConstants.PPPOEUSERNAME, spaProductsOrdered.getPppoeUsername());
                newCustomerEvent.getParameters().put(SpaConstants.PPPOEPASSWORD, spaProductsOrdered.getPppoePassword());
            }
        }
    }

    private void setCustomerMetafield(List<MetaFieldValueWS> metaFieldValues, boolean spaEnrollmentIncomplete, String missingInformation) {

        setMetafieldValueWS(SpaConstants.SPA_ENROLLMENT_INCOMPLETE, spaEnrollmentIncomplete, metaFieldValues);
        if (spaEnrollmentIncomplete) {
            setMetafieldValueWS(SpaConstants.SPA_ENROLLMENT_NOTES, missingInformation, metaFieldValues);
        }
        setMetafieldValueWS(SpaConstants.TAX_EXEMPT, spaImportWS.getTaxExempt() != null ? Boolean.valueOf(spaImportWS.getTaxExempt()) : null, metaFieldValues);
        setMetafieldValueWS(SpaConstants.TAX_EXEMPTION_NUMBER, spaImportWS.getTaxExemptionCode(), metaFieldValues);

    }

    private void setContactInformationAITMetaFields(Integer contactInformationAITid, List<MetaFieldValueWS> metaFieldValues) {
        if (null != spaImportWS.getCustomerName()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CUSTOMER_NAME, contactInformationAITid, DataType.STRING, false, spaImportWS.getCustomerName()));
        }

        if (null != spaImportWS.getCustomerCompany()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CUSTOMER_COMPANY, contactInformationAITid, DataType.STRING, false, spaImportWS.getCustomerCompany()));
        }
        SpaAddressWS address = spaImportWS.getAddress(AddressType.BILLING);
        if (null != address.getProvince()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PROVINCE, contactInformationAITid, DataType.STRING, false, address.getProvince()));
        }

        if (null != spaImportWS.getPhoneNumber1()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PHONE_NUMBER_1, contactInformationAITid, DataType.STRING, false, spaImportWS.getPhoneNumber1()));
        }

        if (null != spaImportWS.getPhoneNumber2()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PHONE_NUMBER_2, contactInformationAITid, DataType.STRING, false, spaImportWS.getPhoneNumber2()));
        }

        if (null != spaImportWS.getEmailAddress()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.EMAIL_ADDRESS, contactInformationAITid, DataType.STRING, false, spaImportWS.getEmailAddress()));
        }

        if (null != spaImportWS.getEmailVerified()) {
            metaFieldValues.add(new MetaFieldValueWS(SpaConstants.EMAIL_VERIFIED, contactInformationAITid, DataType.DATE, false, spaImportWS.getEmailVerified()));
        }
        addMetaFieldAddressInformation(metaFieldValues, contactInformationAITid,address);
    }

    private void setMainSubscription(UserWS user) {
        for (OrderPeriodDTO orderPeriod : new OrderPeriodDAS().getOrderPeriods(entityId)) {
            if (Integer.valueOf(1).equals(orderPeriod.getValue()) &&
                    PeriodUnitDTO.DAY == orderPeriod.getPeriodUnit().getId()) {
                user.setMainSubscription(new MainSubscriptionWS(orderPeriod.getId(), 1));
            }
        }
    }

    public boolean generateOrders(Integer userId) {

        for (SpaProductsOrderedWS spaProductsOrderedWS : spaImportWS.getProductsOrdered()) {
            createOrderForPlan(spaProductsOrderedWS.getPlanId(), spaProductsOrderedWS, userId, true);
            if (spaProductsOrderedWS.getModemId() != null) {
                createOrderForPlan(spaProductsOrderedWS.getModemId(), spaProductsOrderedWS, userId, false);
            }
            if (spaProductsOrderedWS.getServicesIds() != null) {
                for (Integer optionalPlanId : spaProductsOrderedWS.getServicesIds()) {
                    createOrderForPlan(optionalPlanId, spaProductsOrderedWS, userId, false);
                }
            }
        }

        return true;
    }

    private void createOrderForPlan(Integer planId, SpaProductsOrderedWS spaProductsOrderedWS, Integer userId, boolean isMainOffering) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setCurrencyId(accountTypeCurrency);
        order.setActiveSince(spaProductsOrderedWS.getStartDate());
        order.setCreateDate(TimezoneHelper.serverCurrentDate());
        List<MetaFieldValueWS> mfValues = new ArrayList<>();
        if (spaProductsOrderedWS.getPlanId().equals(planId) && StringUtils.isNotEmpty(spaProductsOrderedWS.getInstallationTime())) {
            setMetafieldValueWS(SpaConstants.INSTALLATION_TIME, spaProductsOrderedWS.getInstallationTime(), mfValues);
        }
        setMetafieldValueWS(SpaConstants.MF_STAFF_IDENTIFIER, spaImportWS.getStaffIdentifier() != null ?
                spaImportWS.getStaffIdentifier() : SpaConstants.EMPTY_STAFF_IDENTIFIER, mfValues);
        setMetafieldValueWS(SpaConstants.MF_ENROLLMENT_TYPE, !spaImportWS.isUpdateCustomer() ?
                SpaConstants.ENROLLMENT_TYPE_NEW_CUSTOMER : SpaConstants.ENROLLMENT_TYPE_NEW_SERVICES, mfValues);
        
        if (isMainOffering) {
            mainOfferOrderForProcessCenter = order;
        }
        order.setMetaFields(mfValues.toArray(new MetaFieldValueWS[mfValues.size()]));
        PlanDTO plan = new PlanDAS().findPlanById(planId);
        order.setPeriod(plan.getPeriod().getId());
        order.setPeriodStr(plan.getPeriod().getDescription());
        createOrderLinesAndOrderChanges(order, plan, spaProductsOrderedWS);
    }

    private void setMetafieldValueWS(String fieldName, Object value, List<MetaFieldValueWS> mfValues) {
        if (value != null) {
            MetaFieldValueWS metafiledValue = new MetaFieldValueWS();
            metafiledValue.setFieldName(fieldName);
            metafiledValue.setValue(value);
            mfValues.add(metafiledValue);
        }
    }

    public OrderWS getMainOfferOrderForProcessCenter() {
        return mainOfferOrderForProcessCenter;
    }

    private void createOrderLinesAndOrderChanges(OrderWS order, PlanDTO plan,  SpaProductsOrderedWS spaProductsOrderedWS) {
        OrderLineWS line = getOrderLineByPlanDTO(plan, order.getCreateDate());
        order.setOrderLines(new OrderLineWS[]{line});
        OrderChangeWS orderChangeWS = buildOrderChangeByOrderLine(order, line, plan, spaProductsOrderedWS);
        orders.put(order, new OrderChangeWS[]{orderChangeWS});
    }

    private OrderLineWS getOrderLineByPlanDTO(PlanDTO plan, Date createDate) {
        OrderLineWS line = new OrderLineWS();
        line.setQuantity(1);
        line.setPrice(plan.getItem().getPrice(new Date()).getRate());
        line.setAmount(plan.getItem().getPrice(new Date()).getRate());
        line.setDescription(plan.getItem().getDescription());
        line.setCreateDatetime(createDate);
        line.setDeleted(0);
        line.setEditable(Boolean.TRUE);
        line.setUseItem(Boolean.TRUE);
        line.setProductCode(plan.getItem().getInternalNumber());
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(plan.getItemId());
        line.setProvisioningStatusId(Constants.PROVISIONING_STATUS_ACTIVE);
        return line;
    }

    private OrderChangeWS buildOrderChangeByOrderLine(OrderWS order, OrderLineWS line, PlanDTO plan, SpaProductsOrderedWS spaProductsOrderedWS) {
        OrderChangeWS orderChange = new OrderChangeWS();
        orderChange.setItemId(line.getItemId());
        orderChange.setQuantity(line.getQuantity());
        orderChange.setPrice(line.getPrice());
        orderChange.setDescription(line.getDescription());
        orderChange.setUseItem(line.getUseItem() ? 1 : 0);
        orderChange.setStartDate(spaProductsOrderedWS.getStartDate());
        orderChange.setAppliedManually(1);
        orderChange.setApplicationDate(TimezoneHelper.companyCurrentDate(entityId));

        OrderChangeStatusDTO orderChangeStatus = new OrderChangeStatusDAS().findApplyStatus(plan.getItem().getEntityId());
        if (orderChangeStatus == null) {
            orderChangeStatus = new OrderChangeStatusDTO();
            orderChangeStatus.setApplyToOrder(ApplyToOrder.YES);
            orderChangeStatus.setCompany(plan.getItem().getEntity());
            OrderChangeStatusBL.createOrderChangeStatus(null, plan.getItem().getEntityId());
        }
        orderChange.setUserAssignedStatusId(orderChangeStatus.getId());
        orderChange.setUserAssignedStatus(orderChangeStatus.getDescription());
        orderChange.setOptLock(BigDecimal.ONE.intValue());
        orderChange.setOrderChangeTypeId(Constants.ORDER_CHANGE_TYPE_DEFAULT);
        orderChange.setOrderWS(order);

        List<OrderChangePlanItemWS> orderChangePlanItem = new ArrayList();
        for (PlanItemDTO planItem : plan.getPlanItems()) {
            AssetDTO asset = getAssetByPlanItem(planItem, spaProductsOrderedWS, order.getUserId());
            orderChangePlanItem.add(buildOrderChangeItem(planItem.getItem().getId(), planItem.getItem().getDescription(SpaImportHelper.getLanguageId(spaImportWS.getLanguage())), asset != null ? asset.getId() : null));
        }
        orderChange.setOrderChangePlanItems(orderChangePlanItem.toArray(new OrderChangePlanItemWS[orderChangePlanItem.size()]));

        List<OrderLineWS> orderLine = getPlanBundleItems(plan);
        order.setPlanBundledItems(orderLine.toArray(new OrderLineWS[orderLine.size()]));
        return orderChange;
    }

    private AssetDTO getAssetByPlanItem(PlanItemDTO planItem, SpaProductsOrderedWS spaProductsOrderedWS, Integer userId) {
        if (Integer.valueOf(1).equals(planItem.getItem().getAssetManagementEnabled())) {
            AssetDTO asset = new AssetDTO();
            asset.setAssetStatus(getDefaultAssetStatus(planItem));
            asset.setItem(planItem.getItem());
            asset.setIdentifier(getAssetIdentifier(planItem, spaProductsOrderedWS));
            asset.setEntity(planItem.getItem().getEntity());
            asset.setCreateDatetime(TimezoneHelper.serverCurrentDate());
            for (MetaField metafield : planItem.getItem().findItemTypeWithAssetManagement().getAssetMetaFields()) {
                switch (metafield.getName()) {
                    case SpaConstants.DOMAIN_ID: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getBanffAccountId());
                        MetaField configurationPortalMF = planItem.getItem().findItemTypeWithAssetManagement().getAssetMetaFields().
                                stream().filter(mf -> SpaConstants.CONFIGURATION_PORTAL.equals(mf.getName())).findFirst().orElse(null);
                        if (configurationPortalMF != null) {
                            asset.setMetaField(configurationPortalMF, SpaConstants.CONFIGURATION_PORTAL_BANFF);
                        }
                        break;
                    }
                    case SpaConstants.TRACKING_NUMBER: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getTrackingNumber());
                        break;
                    }
                    case SpaConstants.MF_PPPOE_USER: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getPppoeUsername());
                        break;
                    }
                    case SpaConstants.MF_PPPOE_PASSWORD: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getPppoePassword());
                        break;
                    }
                    case SpaConstants.MF_MAC_ADRESS: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getMacAddress());
                        break;
                    }
                    case SpaConstants.MF_MODEL: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getModel());
                        break;
                    }
                    case SpaConstants.MF_SERIAL_NUMBER: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getSerialNumber());
                        break;
                    }
                    case SpaConstants.MF_PHONE_NUMBER: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getPhoneNumber());
                        break;
                    }
                    case SpaConstants.COURIER: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getCourier());
                        break;
                    }
                    case SpaConstants.ASSET_STATE: {
                        asset.setMetaField(metafield, SpaConstants.ACTIVE_ASSET_STATE);
                        break;
                    }
                    case SpaConstants.HOST: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getHost());
                        break;
                    }
                    case SpaConstants.SIPPASSWORD: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getSipPassword());
                        break;
                    }
                    case SpaConstants.CONFIGURATION_PORTAL: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getConfigurationPortal());
                        break;
                    }
                    case SpaConstants.ALLOWG729: {
                        asset.setMetaField(metafield, Boolean.valueOf(spaProductsOrderedWS.getAllowg729()));
                        break;
                    }
                    case SpaConstants.CUSTOMER_TYPE: {
                        asset.setMetaField(metafield, spaProductsOrderedWS.getCustomerType());
                        break;
                    }
                    case SpaConstants.VOICE_MAIL_PASSWORD: {                        
                        asset.setMetaField(metafield, userId.toString());                        
                        break;
                    }
                    default: {
                        if (!SpaConstants.CONFIGURATION_PORTAL.equals(metafield.getName())) {
                            asset.setMetaField(metafield, null);
                        }
                    }
                }
            }
            asset = new AssetDAS().save(asset);
            return asset;
        }
        return null;
    }

    private AssetStatusDTO getDefaultAssetStatus(PlanItemDTO planItem) {
        for (ItemTypeDTO itemType : planItem.getItem().getItemTypes()) {
            if (itemType.findDefaultAssetStatus()!=null) {
                return itemType.findDefaultAssetStatus();
            }
        }
        return null;
    }

    private String getAssetIdentifier(PlanItemDTO planItem, SpaProductsOrderedWS spaProductsOrderedWS) {
        if(StringUtils.isNotBlank(spaProductsOrderedWS.getModemAssetIdentifier()) 
                && planItem.getPlan().getId().equals(spaProductsOrderedWS.getModemId()) ){
            if(!CollectionUtils.isEmpty(new AssetDAS().findAssetsByIdentifier(spaProductsOrderedWS.getModemAssetIdentifier()))){
                return getIdentifire(planItem,spaProductsOrderedWS);
            }
            return spaProductsOrderedWS.getModemAssetIdentifier();
        }else if(StringUtils.isNotBlank(spaProductsOrderedWS.getServiceAssetIdentifier())
                && !planItem.getPlan().getId().equals(spaProductsOrderedWS.getModemId()) ){
            if(!CollectionUtils.isEmpty(new AssetDAS().findAssetsByIdentifier(spaProductsOrderedWS.getServiceAssetIdentifier()))){
                return getIdentifire(planItem,spaProductsOrderedWS);
            }
            return spaProductsOrderedWS.getServiceAssetIdentifier();
        }
        return getIdentifire(planItem,spaProductsOrderedWS);
    }

    private OrderChangePlanItemWS buildOrderChangeItem(Integer itemId, String description, Integer assetId) {
        OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
        orderChangePlanItem.setItemId(itemId);
        orderChangePlanItem.setDescription(description);
        orderChangePlanItem.setBundledQuantity(BigDecimal.ONE.intValue());
        if (assetId != null) {
            orderChangePlanItem.setAssetIds(new int[]{assetId});
        }
        orderChangePlanItem.setOptlock(BigDecimal.ZERO.intValue());
        return orderChangePlanItem;
    }

    public List<OrderLineWS> getPlanBundleItems(PlanDTO plan) {
        OrderLineWS line;
        List<OrderLineWS> lines = new ArrayList();
        for (PlanItemDTO item : plan.getPlanItems()) {
            line = new OrderLineWS();
            line.setPrice(item.getPrice(new Date()).getRate());
            line.setItemId(item.getId());
            lines.add(line);
        }
        return lines;
    }

    public Map<OrderWS, OrderChangeWS[]> getOrders() {
        return orders;
    }

    public OrderChangeWS[] getOrderChanges() {
        return null;
    }

    public PaymentWS getPayment(Integer customerId) {
        PaymentWS payment = new PaymentWS();
        payment.setUserId(customerId);
        Date companyDate = TimezoneHelper.companyCurrentDate(entityId);
        payment.setPaymentDate(companyDate);
        payment.setCurrencyId(accountTypeCurrency);
        payment.setAmount(spaImportWS.getPaymentResult().getAmount().toString());
        payment.setIsRefund(0);
        payment.setSendNotification(false);
        PaymentInformationWS cc = getPaymentInformationWS();
        payment.getPaymentInstruments().add(cc);
        PaymentResultDTO paymentResult = new PaymentResultDAS().findPaymentResultByName(spaImportWS.getPaymentResult().getResult(), 
                                                                                        SpaImportHelper.getLanguageId(spaImportWS.getLanguage()));
        List<MetaFieldValueWS> metaFields = new ArrayList<>();
        addMetaField(metaFields, PaymentPaySafeTask.PAYSAFE_PROFILE_ID, false, false, DataType.STRING, 5, spaImportWS.getPaymentCredential().getPaymentProfileId());
        payment.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
        if (paymentResult != null) {
            payment.setResultId(paymentResult.getId());
        }
        return payment;
    }

    private PaymentInformationWS getPaymentInformationWS() {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(new PaymentMethodTypeDAS().findByMethodName(SpaConstants.PAYMENT_METHOD_NAME_CREDIT_CARD, entityId).get(0).getId());
        cc.setPaymentMethodId(CommonConstants.PAYMENT_METHOD_CREDIT);
        cc.setProcessingOrder(1);
        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(3);
        addStringToCharMetaField(metaFields, SpaConstants.CC_CARDHOLDER_NAME, false, false,
                DataType.CHAR, 1, spaImportWS.getPaymentCredential().getCcname());
        addStringToCharMetaField(metaFields, SpaConstants.CC_NUMBER, false, false,
                DataType.CHAR, 2, SpaConstants.CC_MASK + spaImportWS.getPaymentCredential().getCcnumber());
        addStringToCharMetaField(metaFields, SpaConstants.CC_EXPIRYDATE, false, false,
                DataType.CHAR, 3, spaImportWS.getPaymentCredential().getCcmonth() + "/" + spaImportWS.getPaymentCredential().getCcyear());
        addStringToCharMetaField(metaFields, SpaConstants.CUSTOMER_TOKEN, false, false,
                DataType.CHAR, 4, spaImportWS.getPaymentCredential().getCustomerToken());
        addMetaField(metaFields, SpaConstants.AUTOPAYMENT_AUTHORIZATION, false, false,
                DataType.BOOLEAN, 5, true);
        addMetaField(metaFields, PaymentPaySafeTask.PAYMENT_STATUS, false, false,
                DataType.STRING, 6, PaySafeStatus.ACTIVE.getName());
        addMetaField(metaFields, PaymentPaySafeTask.PAYMENT_ATTEMPT_COUNT, false, false,
                DataType.INTEGER, 7, Integer.valueOf(0));
        addMetaField(metaFields, PaymentPaySafeTask.PAYSAFE_PROFILE_ID, true, false, DataType.STRING, 8, spaImportWS.getPaymentCredential().getPaymentProfileId());

        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
        return cc;
    }

    private void addMetaField(List<MetaFieldValueWS> metaFields,
                              String fieldName, boolean disabled, boolean mandatory,
                              DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);
        ws.getMetaField().setEntityId(entityId);
        metaFields.add(ws);
    }

    private void addStringToCharMetaField(List<MetaFieldValueWS> metaFields,
                                          String fieldName, boolean disabled, boolean mandatory,
                                          DataType dataType, Integer displayOrder, String value) {
        addMetaField(metaFields, fieldName, disabled, mandatory, dataType, displayOrder, value != null ? value.toCharArray() : null);
    }

    public InvoiceWS getInvoice() {
        return null;
    }

    public boolean validateMandatory() {
        return SpaValidator.validateMandatory(spaImportWS);
    }

    public boolean getRecordPayment() {
        return recordPayment;
    }

    public void setRecordPayment(boolean recordPayment) {
        this.recordPayment = recordPayment;
    }

    public boolean getSendNotification() {
        return sendNotification;
    }

    private String validateNonMandatoryAndMissingInformation() {
        this.recordPayment = SpaValidator.hasToRecordPayment(spaImportWS);
        this.sendNotification = SpaValidator.hasToSendNotification(spaImportWS);
        return SpaValidator.validateNonMandatoryAndMissingInformation(spaImportWS);
    }

    public void updatePaymentStatus(Integer paymentId,int paymentStatus){
        PaymentDTO payment= paymentDAS.find(paymentId);
        payment.setPaymentResult(new PaymentResultDAS().find(paymentStatus));
        paymentDAS.save(payment);
    }


    public void createPaymentAuthorization(Integer paymentId) {
        PaymentAuthorizationDTO paymentAuthorization = new PaymentAuthorizationDTO();
        paymentAuthorization.setPayment(paymentDAS.find(paymentId));
        paymentAuthorization.setProcessor(SpaConstants.PROCESSOR_PAYSAFE);
        paymentAuthorization.setCode1(SpaConstants.PROCESSOR_PAYSAFE);
        paymentAuthorization.setTransactionId(spaImportWS.getPaymentResult().getTransactionToken());
        paymentAuthorization.setCreateDate(TimezoneHelper.companyCurrentDate(entityId));
        new PaymentAuthorizationDAS().save(paymentAuthorization);
    }

    public boolean setFurtherOrderAndAssetInformation(String banffAccountId, String trackingNumber, String courier, String serialNumber, String macAddress, String model, String serviceAssetIdentifier) {
        if (StringUtils.isBlank(banffAccountId) && StringUtils.isBlank(serviceAssetIdentifier)) {
            return false;
        }

        List<AssetDTO> assets = null;
        if(StringUtils.isNotBlank(serviceAssetIdentifier)){
            assets = new AssetDAS().findAssetsByIdentifier(serviceAssetIdentifier);
        }else {
            assets = new AssetDAS().findAssetByMetaFieldValue(entityId, SpaConstants.DOMAIN_ID, banffAccountId);
        }

        if (CollectionUtils.isEmpty(assets)) {
            return false;
        }

        assets.stream().forEach(
                asset -> {
                    asset.setMetaField(entityId, null, SpaConstants.TRACKING_NUMBER, trackingNumber);
                    asset.setMetaField(entityId, null, SpaConstants.MF_SERIAL_NUMBER, serialNumber);
                    asset.setMetaField(entityId, null, SpaConstants.MF_MAC_ADRESS, macAddress);
                    asset.setMetaField(entityId, null, SpaConstants.MF_MODEL, model);
                    asset.setMetaField(entityId, null, SpaConstants.COURIER, courier);
                    assetDAS.save(asset);
                    assetDAS.flush();
                }
        );
        return true;
    }


    public String getMCFFileName(Integer invoiceID) {
        InvoiceDTO invoice = new InvoiceDAS().find(invoiceID);
        for (InvoiceLineDTO invoiceLine : invoice.getInvoiceLines()) {
            if (invoiceLine.getOrder() != null) {
                MetaFieldValue detailFileNamesMF = MetaFieldHelper.getMetaField(invoiceLine.getOrder(), SpaConstants.DETAIL_FILE_NAMES);
                if (detailFileNamesMF != null) {
                    return detailFileNamesMF.getValue().toString();
                }
            }
        }
        return null;
    }

    public byte[] getMCFFile(Integer invoiceID) {
        byte[] mcfFile = null;
        try {
            String initialPath = com.sapienter.jbilling.common.Util.getSysProp("base_dir") +
                    new InvoiceDAS().findNow(invoiceID).getBaseUser().getCompany().getMetaField(Constants.MF_DETAIL_FILE_FOLDER).getValue() + File.separator;
            String fileTmpNameAndPath = initialPath + File.separator + getMCFFileName(invoiceID);
            mcfFile = Files.readAllBytes(new File(fileTmpNameAndPath).toPath());
        } catch (IOException e) {
            log.error("error getting MCF file: " + e.getMessage());
        }
        return mcfFile;
    }

    public static boolean isDistributel(Integer companyId) {
        if (companyId == null) {
            return false;
        }
        List<MetaFieldValue> metafields = new CompanyDAS().find(companyId).getMetaFields();
        if (metafields == null || metafields.isEmpty()) {
            return false;
        }
        return metafields.stream().anyMatch(metafield -> metafield.getField().getName().equals(SpaConstants.BUSINESS_UNIT));
    }


    public boolean sendMessageToProccescenter(UserDTO user, SpaProductsOrderedWS spaProductsOrdered) {
        RouteDTO route = routeDAS.getRoute(entityId, SpaConstants.DT_PROCESS_CENTERS);
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        String processCenter = spaProductsOrdered.getProcessCenter();
        if (processCenter != null) {
            criteria.setFilters(new BasicFilter[]{
                    new BasicFilter(SpaConstants.DT_COL_PROCESS_CENTER_ID, Filter.FilterConstraint.EQ, processCenter),
            });
        }
        SearchResultString queryResult = routeDAS.searchDataTable(criteria, route);
        List<List<String>> rows = queryResult.getRows();
        if (rows.size() > 0) {
            String email = rows.get(0).get(2);
            Integer notificationId = Integer.valueOf(rows.get(0).get(3));
            try {
                MessageDTO message = notificationBL.getCustomNotificationMessage(notificationId, entityId, user.getUserId(), SpaImportHelper.getLanguageId(SpaConstants.ENGLISH_LANGUAGE));
                message.getParameters().put(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS, email);
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_NAME, UserHelperDisplayerDistributel.getInstance().getDisplayName(user));
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_EMAIL, spaImportWS.getEmailAddress());
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_ID, user.getId());
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_ACCOUNT_ID, mainOfferOrderForProcessCenter.getId());
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_CONFIRMATION_NUMBER, spaImportWS.getConfirmationNumber() != null ? spaImportWS.getConfirmationNumber() : " ");
                SpaAddressWS address = spaImportWS.getAddress(AddressType.SHIPPING);
                StringBuilder completeAddress = new StringBuilder();
                completeAddress.append(StringUtils.isNotEmpty(address.getStreetName()) ? address.getStreetName() : "");
                completeAddress.append(" ");
                completeAddress.append(address.getStreetNumber() != null ? address.getStreetNumber() : "");
                completeAddress.append("\n");
                completeAddress.append(address.getCity());
                completeAddress.append(" - ");
                completeAddress.append(address.getProvince());
                completeAddress.append("\t");
                completeAddress.append(address.getPostalCode());
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_ADDRESS, completeAddress);
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(TimezoneHelper.convertToTimezoneByEntityId(user.getCreateDatetime(), entityId)));
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_PORTAL_USERNAME, portalUserName);
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_PORTAL_PASSWORD, portalPassword);
                OrderLineWS[] orderLineWS = mainOfferOrderForProcessCenter.getOrderLines();
                String packageName = "";
                if (ArrayUtils.isNotEmpty(orderLineWS)) {
                    packageName = orderLineWS[0].getDescription();
                }
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_PACKAGE_NAME, packageName);
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_INSTALL_DATE, new SimpleDateFormat("yyyy-MM-dd").format(spaProductsOrdered.getStartDate()));
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_HEXENCODEDMESSAGE, formatExpresionToHTML(decodeHexMessage(spaProductsOrdered.getHexencodedMessage())));
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_PROCESS_CENTER, (spaProductsOrdered.getProcessCenter()));
                message.getParameters().put(SpaConstants.MSGDTO_PARAMETER_CONFIRMATION_NUMBER, (spaImportWS.getConfirmationNumber()));
                if(StringUtils.containsIgnoreCase(phoneNumberAssetIdentifier,SpaConstants.NEW_PHONE_NUMBER)){
                    message.getContent()[0].setContent(SpaConstants.SUBJECT_MAIL_PROCESS_CENTER_NEW_NUMBER);
                }

                INotificationSessionBean notificationSess = (INotificationSessionBean) Context.getBean(Context.Name.NOTIFICATION_SESSION);
                notificationSess.notify(user.getUserId(), message);
                return true;
            } catch (NotificationNotFoundException e) {
                e.printStackTrace();
                log.error("error sending message to process center " + e.getMessage());
            }
        }
        return false;
    }

    private String formatExpresionToHTML(String text) {
        StringBuilder formatedString = new StringBuilder("");
        for (String token : text.split("\n")) {
            formatedString.append(StringEscapeUtils.escapeHtml(token)).append("<br>");
        }
        return formatedString.toString();
    }

    private String getRandomCharSequence() {
        String allowedChars = SpaConstants.ALLOWED_CHARS_PORTAL;
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(SpaConstants.RANDOM_CHAR_SEQUENCE_LENGTH);
        for (int i = 0; i < SpaConstants.RANDOM_CHAR_SEQUENCE_LENGTH; i++) {
            sb.append(allowedChars.charAt(rnd.nextInt(allowedChars.length())));
        }
        return sb.toString();
    }

    private String getPortalUserName() {
        return getRandomCharSequence();
    }

    private String getPortalPassword() {
        return getRandomCharSequence();
    }

    private String decodeHexMessage(String hexencodedMessage) {
        try {
            if (hexencodedMessage != null) {
                byte[] bytes = Hex.decodeHex(hexencodedMessage.toCharArray());
                return  org.apache.commons.codec.binary.StringUtils.newStringIso8859_1(bytes);
            }
        } catch (DecoderException e) {
            log.error("decoding hexMessage", e);
        }
        return null;
    }

    public boolean isValidToGenerateInvoice(){
        return spaImportWS.getProductsOrdered().get(0).getStartDate().before(Calendar.getInstance().getTime());
    }

    public PlanWS[] getPlans(String province, String userType, Integer entityId) {
        List<PlanDTO> plans = new PlanDAS().findAllActiveAvailable(entityId);

        if (!StringUtils.isEmpty(province)) {
            plans = getPlansFilteringByProvince(plans, province);
        }

        if (!StringUtils.isEmpty(userType)) {
            plans = getPlansFilteringByUserType(plans, userType, entityId);
        }

        List<PlanWS> ws = PlanBL.getWS(plans);
        return ws.toArray(new PlanWS[ws.size()]);
    }

    private List<PlanDTO> getPlansFilteringByProvince(List<PlanDTO> plans, String province) {
        List<PlanDTO> filteredPlans = new ArrayList<>();
        for (PlanDTO plan : plans) {
            MetaFieldValue provinceMF = plan.getMetaField(SpaConstants.PROVINCE);
            if (provinceMF == null) {
                filteredPlans.add(plan);
            } else {
                if (province.equals((String)provinceMF.getValue())) {
                    filteredPlans.add(plan);
                }
            }
        }
        return filteredPlans;
    }

    private List<PlanDTO> getPlansFilteringByUserType(List<PlanDTO> plans, String userType, Integer entityId) {
        List<PlanDTO> filteredPlans = new ArrayList<>();
        EnumerationDTO userTypeEnumeration = new EnumerationBL().getEnumerationByName(SpaConstants.USER_TYPE, entityId);
        Integer maxLevel = null;
        if (userTypeEnumeration == null) {
            return plans;
        } else {
            for (EnumerationValueDTO enumValue : userTypeEnumeration.getValues()) {
                String stEnumValue = enumValue.getValue();
                if (stEnumValue.contains(userType)) {
                    maxLevel = Integer.valueOf(stEnumValue.substring(0, stEnumValue.indexOf(":")));
                    break;
                }
            }
        }
        if (maxLevel == null) {
            return plans;
        }
        for (PlanDTO plan : plans) {
            MetaFieldValue userTypeMetaField = plan.getMetaField(SpaConstants.USER_TYPE);
            if (userTypeMetaField == null) {
                filteredPlans.add(plan);
            } else {
                String assignedUserType = (String) plan.getMetaField(SpaConstants.USER_TYPE).getValue();
                Integer planLevel = Integer.valueOf(assignedUserType.substring(0, assignedUserType.indexOf(":")));
                if (maxLevel.compareTo(planLevel) >= 0) {
                    filteredPlans.add(plan);
                }
            }
        }
        return filteredPlans;
    }

    public List<AssetWS> assetOrderUpdateStatus(Integer orderId, String assetDescription){
        List<OrderChangeDTO> orderChanges= new OrderChangeDAS().findByOrder(orderId);
        List<AssetDTO> listAsset = new ArrayList();
        orderChanges.stream().forEach(orderChange -> orderChange.getOrderChangePlanItems()
                .forEach(orderChangePlanItem -> {
                    if (orderChangePlanItem.getAssets() != null && !orderChangePlanItem.getAssets().isEmpty()) {
                        AssetStatusDTO assetStatusDescription = getAssetStatusDescription(orderChangePlanItem.getItem(), assetDescription);
                        if (assetStatusDescription == null) {
                            throw new IllegalArgumentException(String.format("The asset status with description %s does not exits", assetDescription));
                        }
                        orderChangePlanItem.getAssets().forEach(asset -> {
                            if(asset.getAssetStatus().getIsAvailable() == 1){
                                asset.setAssetStatus(assetStatusDescription);
                                listAsset.add(asset);
                            }
                        });
                    }
                })
        );
        List<AssetWS> assetWSList = new ArrayList();
        listAsset.forEach(asset -> assetWSList.add(AssetBL.getWS(asset)));
        return assetWSList;
    }

    private AssetStatusDTO getAssetStatusDescription(ItemDTO itemDTO, String assetDescription) {
        AssetStatusDAS assetStatusDAS = new AssetStatusDAS();
        List<AssetStatusDTO> assetStatus = assetStatusDAS.getStatuses(itemDTO.getItemTypes().stream().filter(it -> Integer.valueOf(1).equals(it.getAllowAssetManagement())).findFirst().get().getId(), true);
        return assetStatus.stream().filter(asset -> asset.getDescription().equals(assetDescription)).findFirst().get();
    }

    public boolean validateVOIPPhoneNumber() {
        return SpaValidator.validateVOIPPhoneNumber(spaImportWS, entityId);
    }

    public static void validateDistributelAsset(AssetWS asset) {
        if (isDistributel(asset.getEntityId())) {
            SpaValidator.validateDistributelAsset(asset);
        }
    }

    public static void emailPayment(Integer entityId, Integer paymentId) {
        PaymentBL paymentBL = new PaymentBL(paymentId);
        PaymentDTO paymentDTO = paymentBL.getDTO();
        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(paymentDTO.getBaseUser().getLanguage().getId());
        if (Constants.RESULT_ENTERED.equals(paymentDTO.getResultId())) {
            MessageDTO message = null;
            Integer notificationId = paymentDTO.getIsRefund() == 0 ? MessageDTO.TYPE_PAYMENT_ENTERED : MessageDTO.TYPE_PAYMENT_REFUND;
            try {
                NotificationBL notificationBL = new NotificationBL();
                notificationBL.setForceNotification(true);
                message = notificationBL.getPaymentMessage(entityId, paymentDTOEx,
                        paymentDTOEx.getPaymentResult().getId(), Integer.valueOf(notificationId));

            } catch (NotificationNotFoundException e) {
                log.debug("Notification id: %s does not exist for the user id %s ",
                        notificationId,
                        paymentDTOEx.getUserId(), e);
            }
            if (message == null) {
                return;
            }
            log.debug("Notifying user: %s, with result %s and is a %s",
                    paymentDTOEx.getUserId(),
                    paymentDTOEx.getResultId(),
                    paymentDTOEx.getIsRefund() == 1 ? "Refund" : "Payment");
            INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSession.notify(paymentDTOEx.getUserId(), message);

        } else {
            PaySafeResultType resultType = PaySafeResultType.SUCESSFUL;
            if (!CommonConstants.RESULT_OK.equals(paymentDTO.getResultId())) {
                MetaFieldValue metaFieldValue = paymentDTO.getMetaField(PaymentPaySafeTask.PAYMENT_STATUS);
                String status = (null != metaFieldValue &&
                        null != metaFieldValue.getValue() ? metaFieldValue.getValue().toString() : StringUtils.EMPTY);
                if (status.isEmpty() && (CommonConstants.RESULT_FAIL.equals(paymentDTO.getResultId()) ||
                        CommonConstants.RESULT_UNAVAILABLE.equals(paymentDTO.getResultId()))) {
                    status = PaySafeStatus.ACTIVE.getName();
                }
                PaySafeStatus paySafeStatus = PaySafeStatus.getByName(status);
                if (null != paySafeStatus) {
                    switch (paySafeStatus) {
                    case ACTIVE:
                        resultType = PaySafeResultType.FAILURE;
                        break;
                    case CANCELLED:
                        resultType = PaySafeResultType.CANCELLED;
                        break;
                    case DISABLED:
                        resultType = PaySafeResultType.DISABLED;
                        break;
                    }
                }
            }
            PaySafeProcessedPaymentEvent paySafeProcessedPaymentEvent = new PaySafeProcessedPaymentEvent(entityId, resultType, paymentDTOEx, false);
            EventManager.process(paySafeProcessedPaymentEvent);
        }
    }

    public static CustomerEmergency911AddressWS getCustomerEmergency911AddressWS(UserDTO user,Integer groupId, String addressType) {

        CustomerEmergency911AddressWS addressWS = new CustomerEmergency911AddressWS();
        addressWS.setReturnCode(1);
        CustomerDTO customerDTO = user.getCustomer();

        Date effectiveDate = new Date();
        try {
            effectiveDate = customerDTO.getCurrentEffectiveDateByGroupId(groupId);
            if(null == effectiveDate || CommonConstants.EPOCH_DATE.compareTo(effectiveDate) == 0) {
                addressWS.setEffectiveDate(parseDate(user.getCreateDatetime()));
            } else {
                addressWS.setEffectiveDate(parseDate(effectiveDate));
            }
        } catch (Exception e) {
            log.debug("Error getting effective date,{}", e);
        }

        if(addressType.equals(SpaConstants.EMERGENCY_ADDRESS_AIT)) {
            addressWS.setAddressType("911");
        } else {
            addressWS.setAddressType("Contact Address");
        }

        StringBuilder address = new StringBuilder();
        String[] metaFieldArray = { SpaConstants.MF_APT_SUITE,SpaConstants.STREET_NUMBER,SpaConstants.STREET_NAME,SpaConstants.STREET_TYPE
                ,SpaConstants.STREET_DIRECTION,SpaConstants.CITY,SpaConstants.PROVINCE,SpaConstants.POSTAL_CODE };

        CustomerAccountInfoTypeMetaField[] metaFieldArrayValue = new CustomerAccountInfoTypeMetaField[metaFieldArray.length];

        for (int i =0;i < metaFieldArray.length;i++) {
            metaFieldArrayValue[i] = customerDTO.getCustomerAccountInfoTypeMetaField(metaFieldArray[i], groupId,effectiveDate);
        }

        if(user.getLanguage().getId() == 4) {
            CustomerAccountInfoTypeMetaField temp = metaFieldArrayValue[2];
            metaFieldArrayValue[2] = metaFieldArrayValue[3];
            metaFieldArrayValue[3] = temp;
        }

        for (int i =0;i < metaFieldArrayValue.length;i++) {
            if (null != metaFieldArrayValue[i]) {
                address.append((String)metaFieldArrayValue[i].getMetaFieldValue().getValue()).append(SEPARATOR);
            }
        }

        if (!StringUtils.isEmpty(address.toString())) {
            addressWS.setAddress(address.toString().substring(0, address.toString().length() - 1));
        }

        return addressWS;
    }

    private static String parseDate(Date date) {
        if(date == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    public boolean validateServiceAssetIdentifier() {
        return SpaValidator.validateServiceAssetIdentifier(spaImportWS);
    }

    public boolean validateModemAssetIdentifier() {
        return SpaValidator.validateModemAssetIdentifier(spaImportWS);
    }

    private String getIdentifire(PlanItemDTO planItem, SpaProductsOrderedWS spaProductsOrderedWS){
        StringBuilder assetItentifier = new StringBuilder(spaProductsOrderedWS.getServiceType());
        phoneNumberAssetIdentifier= spaProductsOrderedWS.getPhoneNumber();
        if (!StringUtils.isEmpty(spaProductsOrderedWS.getPhoneNumber())) {
            assetItentifier.append(" ");
            if (planItem.getPlan().getId().equals(spaProductsOrderedWS.getModemId())) {
                assetItentifier.append(SpaConstants.VOIP_ATA);
            } else {
                assetItentifier.append(SpaConstants.VOIP_PHONE);
            }
        }
        assetItentifier.append(" - " + System.currentTimeMillis());
        return assetItentifier.toString();
    }
    
	private void setAdditionalAddressesAITMetaFields(CustomerDTO customer, Integer groupId) {
	    SpaAddressWS address;

	    AccountInformationTypeDTO shippingAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SHIPPING_ADDRESS_AIT, entityId, accountTypeId);
	    if (shippingAddressAIT != null && groupId == shippingAddressAIT.getId()) {
	        address = spaImportWS.getAddress(AddressType.SHIPPING);
	        customer.setAitMetaField(entityId, shippingAddressAIT.getId(), SpaConstants.SAME_AS_CUSTOMER_INFORMATION, address == null ? true : false);
	    }

	    AccountInformationTypeDTO serviceAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SERVICE_ADDRESS_AIT, entityId, accountTypeId);
	    if (serviceAddressAIT != null && groupId == serviceAddressAIT.getId()) {
	        address = spaImportWS.getAddress(AddressType.SERVICE);
	        customer.setAitMetaField(entityId, serviceAddressAIT.getId(), SpaConstants.SAME_AS_CUSTOMER_INFORMATION, address == null ? true : false);       
	    }
	    
	    AccountInformationTypeDTO portingAddressAIT = new AccountInformationTypeDAS().findByName(SpaConstants.PORTING_ADDRESS_AIT, entityId, accountTypeId);
	    if (portingAddressAIT != null && groupId == portingAddressAIT.getId()) {
	        address = spaImportWS.getAddress(AddressType.PORTING);
	        customer.setAitMetaField(entityId, portingAddressAIT.getId(), SpaConstants.SAME_AS_CUSTOMER_INFORMATION, address == null ? true : false);
	    }
	}
}
