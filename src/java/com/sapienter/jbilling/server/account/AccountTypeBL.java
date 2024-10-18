package com.sapienter.jbilling.server.account;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentBL;
import com.sapienter.jbilling.server.invoice.InvoiceTemplateDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpStatus;

import java.math.BigDecimal;
import java.util.*;

public class AccountTypeBL {

    private static final Logger logger = LoggerFactory.getLogger(AccountTypeBL.class);

    private AccountTypeDTO accountTypeDTO = null;
    private AccountTypeDAS accountTypeDAS = null;

    private void init() {
        accountTypeDAS = new AccountTypeDAS();
        accountTypeDTO = new AccountTypeDTO();
    }

    public AccountTypeWS getWS(Integer languageId) {
        return getWS(accountTypeDTO);
    }
    
    
    public static final AccountTypeWS getWS(AccountTypeDTO dto) {

       return  getWS(dto.getId(), dto.getCompany().getId(), dto.getInvoiceDesign(), dto.getInvoiceTemplate() != null ? dto.getInvoiceTemplate().getId() : null, dto.getDateCreated(),
                dto.getCreditNotificationLimit1(), dto.getCreditNotificationLimit2(),
                dto.getCreditLimit(), dto.getInvoiceDeliveryMethod(),
                dto.getCurrencyId(), dto.getLanguageId(), dto.getDescription(),
                UserBL.convertMainSubscriptionToWS(dto.getBillingCycle()),
                dto.getInformationTypes(), dto.getPaymentMethodTypes(),dto.getPreferredNotificationAitIds());
    }

    public static final AccountTypeWS getWS(Integer id, Integer entityId, String invoiceDesign, Integer invoiceTemplateId, Date dateCreated, BigDecimal creditNotificationLimit1,
                         BigDecimal creditNotificationLimit2, BigDecimal creditLimit, InvoiceDeliveryMethodDTO invoiceDeliveryMethod,
                         Integer currencyId, Integer languageId, String description, MainSubscriptionWS mainSubscription,
                         Set<AccountInformationTypeDTO> informationTypes, Set<PaymentMethodTypeDTO> paymentMethodTypes, List<Integer> preferedAits) {

		AccountTypeWS ws = new AccountTypeWS();
		ws.setId(id);
		ws.setEntityId(entityId);
		ws.setInvoiceDesign(invoiceDesign);
		ws.setInvoiceTemplateId(invoiceTemplateId);
		ws.setDateCreated(dateCreated);
		ws.setCreditNotificationLimit1(creditNotificationLimit1 != null ? creditNotificationLimit1
				.toString() : null);
		ws.setCreditNotificationLimit2(creditNotificationLimit2 != null ? creditNotificationLimit2
				.toString() : null);
		ws.setCreditLimit(creditLimit != null ? creditLimit.toString() : null);
		ws.setInvoiceDeliveryMethodId(invoiceDeliveryMethod != null ? invoiceDeliveryMethod
				.getId() : null);
		ws.setCurrencyId(currencyId);
		ws.setLanguageId(languageId);
		ws.setMainSubscription(mainSubscription);

		if (description != null) {
			ws.setName(description, Constants.LANGUAGE_ENGLISH_ID);
		}

		if (null != informationTypes && informationTypes.size() > 0) {
			List<Integer> informationTypeIds = new ArrayList<>();
			for (AccountInformationTypeDTO ait : informationTypes) {
				informationTypeIds.add(ait.getId());
			}
			if (!informationTypeIds.isEmpty()) {
                Collections.sort(informationTypeIds);
				ws.setInformationTypeIds(informationTypeIds
						.toArray(new Integer[informationTypeIds.size()]));
			}
		}

		if (null != paymentMethodTypes && paymentMethodTypes.size() > 0) {
			List<Integer> paymentMethodTypeIds = new ArrayList<>(0);
			for (PaymentMethodTypeDTO paymentMethodType : paymentMethodTypes) {
				paymentMethodTypeIds.add(paymentMethodType.getId());
			}
            if (!paymentMethodTypeIds.isEmpty()){
                Collections.sort(paymentMethodTypeIds);
            }
			ws.setPaymentMethodTypeIds(paymentMethodTypeIds
                    .toArray(new Integer[paymentMethodTypeIds.size()]));
		}

		if(null != preferedAits){
		    ws.setPreferedInformationTypeIds( preferedAits.toArray(new Integer[preferedAits.size()]));
		}

		return ws;
    }

    public static final  AccountTypeDTO getDTO(AccountTypeWS ws,Integer entityId) {

		AccountTypeDTO accountTypeDTO = new AccountTypeDTO();
		if (ws.getId() != null && ws.getId() > 0) {
			accountTypeDTO.setId(ws.getId());
		}

		accountTypeDTO.setCompany(new CompanyDTO(entityId));

		accountTypeDTO.setCreditLimit(ws.getCreditLimitAsDecimal());
		accountTypeDTO.setCreditNotificationLimit1(ws
				.getCreditNotificationLimit1AsDecimal());
		accountTypeDTO.setCreditNotificationLimit2(ws
				.getCreditNotificationLimit2AsDecimal());
		accountTypeDTO.setInvoiceDesign(ws.getInvoiceDesign());
		accountTypeDTO.setInvoiceTemplate(new InvoiceTemplateDAS().find(ws
				.getInvoiceTemplateId()));
		accountTypeDTO.setBillingCycle(UserBL.convertMainSubscriptionFromWS(
				ws.getMainSubscription(), entityId));
		accountTypeDTO.setLanguage(new LanguageDAS().find(ws.getLanguageId()));
		accountTypeDTO.setCurrency(new CurrencyDAS().find(ws.getCurrencyId()));
		accountTypeDTO.setInvoiceDeliveryMethod(new InvoiceDeliveryMethodDTO(ws
				.getInvoiceDeliveryMethodId()));
		// set payment method types
		if (ws.getPaymentMethodTypeIds() != null) {
			Set<PaymentMethodTypeDTO> paymentMethodTypes = new HashSet<>(
					0);
			PaymentMethodTypeDAS das = new PaymentMethodTypeDAS();

			for (Integer paymentMethodTypeId : ws.getPaymentMethodTypeIds()) {
				paymentMethodTypes.add(das.find(paymentMethodTypeId));
			}
			accountTypeDTO.setPaymentMethodTypes(paymentMethodTypes);
		}
		List<PaymentMethodTypeDTO> globalPaymentMethods = new PaymentMethodTypeDAS()
				.findByAllAccountType(entityId);
		for (PaymentMethodTypeDTO globalPaymentMethod : globalPaymentMethods) {
			accountTypeDTO.getPaymentMethodTypes().add(globalPaymentMethod);
		}
		return accountTypeDTO;
    }
    
    public AccountTypeBL() {
        init();
    }

    public AccountTypeBL(Integer accountTypeId) {
        init();
        setAccountType(accountTypeId);
    }

    public void setAccountType(Integer accountTypeId) {
        accountTypeDTO = accountTypeDAS.find(accountTypeId);
    }

    public AccountTypeDTO getAccountType() {
        return accountTypeDTO;
    }

    public boolean delete() {

        if (accountTypeDAS.countCustomerByAccountType(accountTypeDTO.getId()) > 0) {
            throw new SessionInternalError("Could not delete Account Type",
                    new String[]{"cannot.delete.account.type.used.by.customers"});
        }

        if(new CustomerEnrollmentBL().countByAccountType(accountTypeDTO.getId()) >0){
            throw new SessionInternalError("Could not delete Account Type",
                    new String[]{"cannot.delete.account.type.enrollment.using"});
        }

        Iterator<AccountInformationTypeDTO> itr = accountTypeDTO.getInformationTypes().iterator();
        while (itr.hasNext()) {
            AccountInformationTypeDTO ait = itr.next();
            itr.remove();
            new AccountInformationTypeBL(ait.getId()).delete();
        }
        accountTypeDAS.delete(accountTypeDTO);
        return true;
    }

    public AccountTypeDTO create(AccountTypeDTO accountTypeDTO) {

        accountTypeDTO.setDateCreated(TimezoneHelper.serverCurrentDate());
        accountTypeDTO = accountTypeDAS.save(accountTypeDTO);

        accountTypeDAS.flush();
        accountTypeDAS.clear();
        return accountTypeDTO;
    }

    public void update(AccountTypeDTO accountType) {

        AccountTypeDTO accountTypeDTO = accountTypeDAS.find(accountType.getId());

        accountTypeDTO.setCreditLimit(accountType.getCreditLimit());
        accountTypeDTO.setCreditNotificationLimit1(accountType.getCreditNotificationLimit1());
        accountTypeDTO.setCreditNotificationLimit2(accountType.getCreditNotificationLimit2());
        accountTypeDTO.setInvoiceDesign(accountType.getInvoiceDesign());
        accountTypeDTO.setInvoiceTemplate(accountType.getInvoiceTemplate());
        accountTypeDTO.setBillingCycle(accountType.getBillingCycle());
        accountTypeDTO.setLanguage(new LanguageDAS().find(accountType.getLanguageId()));
        accountTypeDTO.setCurrency(new CurrencyDAS().find(accountType.getCurrencyId()));
        accountTypeDTO.setInvoiceDeliveryMethod(new InvoiceDeliveryMethodDAS().find(accountType.getInvoiceDeliveryMethod().getId()));
        accountTypeDTO.setPaymentMethodTypes(accountType.getPaymentMethodTypes());
        accountTypeDAS.save(accountTypeDTO);

        accountTypeDAS.flush();
        accountTypeDAS.clear();
    }

    public boolean isAccountTypeUnique(Integer entityId,String name, boolean isNew){

        List<AccountTypeDTO> accountTypeDTOList=new AccountTypeDAS().findAll(entityId);
        List<String> descriptionList=new ArrayList<>();
        for(AccountTypeDTO accountType1 :accountTypeDTOList) {

            descriptionList.add(accountType1.getDescription());
        }
        
        if(isNew) {
        	return  !descriptionList.contains(name);
        } else {
        	return Collections.frequency(descriptionList, name) < 2;
        }
    }
}
