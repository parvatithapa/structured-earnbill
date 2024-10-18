package com.sapienter.jbilling.server.customerInspector;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.ResourceBundle;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.customerInspector.domain.MetaFieldTypeField;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.Util;

/**
 * Created by Pablo Galera
 */
public class CustomerInformationHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd");
    
    private IWebServicesSessionBean webServicesSessionSpringBean;
    private Integer userId;
    private String name;

    private IWebServicesSessionBean getApi() {
        return this.webServicesSessionSpringBean;
    }

    public CustomerInformationHelper(Integer userId, String name, IWebServicesSessionBean webServicesSessionSpringBean) {
        this.userId = userId;
        this.webServicesSessionSpringBean = webServicesSessionSpringBean;
        this.name = name;
    }

    public String getLanguage() {
        return getUserWS(userId).getLanguage();
    }

    public Integer getId() {
        return getUserWS(userId).getId();
    }

    private UserWS getUserWS(Integer userId) {
        return this.getApi().getUserWS(userId);
    }

    public Object getValue() {
        Object objectField = null;
        try {
            boolean foundMember = false;
            try {
                Method method = this.getClass().getDeclaredMethod(name);
                method.setAccessible(true);
                objectField = method.invoke(this);
                foundMember = true;
            } catch (NoSuchMethodException ex) {
            } catch (Exception ex) {
                throw ex;
            }

            if (!foundMember) {
                UserWS user = this.getApi().getUserWS(userId);
                Field field = UserWS.class.getDeclaredField(name);
                field.setAccessible(true);
                objectField = field.get(user);
            }

            if (objectField instanceof Number || (objectField instanceof String && NumberUtils.isNumber((String) objectField))) {
                objectField = formatNumber(objectField);
            }
        } catch (Exception e) {
            logger.info("Cannot retrieve the required property field: {}. {}", this.name, e);
        }

        return objectField;
    }

    private Object formatNumber(Object o) {
        String number = "";
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(2);
        if (o instanceof Number) {
            if (o instanceof Double || o instanceof Float || o instanceof BigDecimal) {
                number = nf.format(o);
            } else {
                number = o.toString();
            }
        } else if (o instanceof String) {
            try {
                number = nf.format(nf.parseObject((String) o));
            } catch (Exception e) {
                logger.info("Cannot parse the string object: {}", o);
            }
        }
        return number;
    }

    private String getStringFromBundleByKey(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", new UserBL(userId).getLocale());
        return bundle.getString(key);
    }

    private String getStringFromBundleByKeyWithParams(String key, Object... params) {
        return MessageFormat.format(getStringFromBundleByKey(key), params);
    }

    private String accountType() {
        return new UserBL(userId).getAccountType().getDescription();
    }

    private String invoiceDeliveryMethod() {
        Integer invoiceDeliveryMethodId = new UserBL(userId).getUserWS().getInvoiceDeliveryMethodId();

        if (invoiceDeliveryMethodId != null) {
            return getStringFromBundleByKey("customer.invoice.delivery.method." + invoiceDeliveryMethodId);
        }
        return null;
    }

    private String billingCycle() {
        UserWS user = this.getApi().getUserWS(userId);
        String unit = this.getApi().getOrderPeriodWS(user.getMainSubscription().getPeriodId()).getDescription(this.getApi().getCallerLanguageId()).getContent();
        String day;
        if(PeriodUnitDTO.YEAR == new OrderPeriodDAS().find(user.getMainSubscription().getPeriodId()).getPeriodUnit().getId()){
            day = LocalDate.ofYearDay(Year.now().getValue(), user.getMainSubscription().getNextInvoiceDayOfPeriod()).format(dateFormatter);
            return unit + ", " + day;
        }else{
            day = user.getMainSubscription().getNextInvoiceDayOfPeriod().toString();
            return day + " " + unit;
        }
    }

    private String childInvoicing() {
        UserWS user = this.getApi().getUserWS(userId);
        CustomerBL customerBL = new CustomerBL(user.getCustomerId());
        if (customerBL.getEntity().getParent() == null) {
            return null;
        }
        if (user.getInvoiceChild()) {
            return getStringFromBundleByKey("customer.invoice.if.child.true");
        }

        Integer invoiceableParentUserId = new CustomerBL(user.getCustomerId()).getInvoicableParent().getBaseUser().getId();
        return getStringFromBundleByKeyWithParams("customer.invoice.if.child.false", invoiceableParentUserId);
    }

    private List<String> subAccounts() {
        UserWS user = this.getApi().getUserWS(userId);
        CustomerBL customerBL = new CustomerBL(user.getCustomerId());
        final List<String> subAccountList = new ArrayList<>();
        customerBL.getEntity().getChildren().forEach(n -> subAccountList.add(n.getBaseUser().getId() + "  " + n.getBaseUser().getUserName()));
        return subAccountList;
    }

    private List<String> agentIds() {
        UserWS userWS = getApi().getUserWS(userId);
        CustomerDTO customerDTO = new CustomerBL(userWS.getCustomerId()).getEntity();
        ArrayList<String> partners = new ArrayList<>();
        customerDTO.getPartners().forEach(n -> partners.add(n.getId() + "  " + n.getType().name()));
        return partners;
    }

    private String lifeTimeRevenue() {
        UserBL userBL = new UserBL(userId);
        return Util.formatSymbolMoney(userBL.getDto().getCurrency().getSymbol(), false) + formatNumber(getApi().getTotalRevenueByUser(userId));
    }

    private String owingBalance() {
        UserBL userBL = new UserBL(userId);
        return Util.formatSymbolMoney(userBL.getDto().getCurrency().getSymbol(), false) + formatNumber(UserBL.getBalance(userId));
    }


    private Boolean excludeAgeing() {
        UserWS userWS = getApi().getUserWS(userId);
        CustomerDTO customerDTO = new CustomerBL(userWS.getCustomerId()).getEntity();
        return customerDTO.getExcludeAging() == 1;
    }

    private String currency() {
        return new CurrencyBL(getApi().getUserWS(userId).getCurrencyId()).getEntity().getDescription();
    }

    private BigDecimal creditLimitAsDecimal() {
        return getApi().getUserWS(userId).getCreditLimitAsDecimal();
    }

    private BigDecimal dynamicBalanceAsDecimal() {
        return getApi().getUserWS(userId).getDynamicBalanceAsDecimal();
    }

    private BigDecimal autoRechargeAsDecimal() {
        return getApi().getUserWS(userId).getAutoRechargeAsDecimal();
    }

    private Date dueDatePeriod() {
        return getApi().getLatestInvoice(userId).getDueDate();
    }

    private List<StringBuilder> phoneNumbers() {
        List<Object> phoneNumbers = MetaFieldTypeField.getMetaFieldValueByFieldUsage(MetaFieldType.PHONE_NUMBER.name(), userId);
        ListIterator<Object> phoneCountryCode = MetaFieldTypeField.getMetaFieldValueByFieldUsage(MetaFieldType.PHONE_COUNTRY_CODE.name(), userId).listIterator();
        ListIterator<Object> phoneAreaCode = MetaFieldTypeField.getMetaFieldValueByFieldUsage(MetaFieldType.PHONE_AREA_CODE.name(), userId).listIterator();
        List<StringBuilder> phoneNumbersCustomer = new ArrayList<>();
        for (Object phone : phoneNumbers) {
            StringBuilder builder = new StringBuilder();
            builder.append(phoneCountryCode.hasNext() ? phoneCountryCode.next() + "-" : "")
                    .append(phoneAreaCode.hasNext() ? phoneAreaCode.next() + "-" : "").append(phone);
            phoneNumbersCustomer.add(builder);
        }
        return phoneNumbersCustomer;
    }

    private String dueDateValue(){
        UserWS user = getApi().getUserWS(userId);
        CustomerDTO customerDTO = new CustomerBL(user.getCustomerId()).getEntity();
        PeriodUnitDTO period = new PeriodUnitDAS().findAll().stream()
                                                            .filter(p -> customerDTO.getDueDateUnitId() != null &&
                                                                         customerDTO.getDueDateUnitId().equals(p.getId()))
                                                            .findFirst()
                                                            .orElse(new PeriodUnitDAS().find(Constants.PERIOD_UNIT_DAY));

        StringBuilder text = new StringBuilder();
        text.append(customerDTO.getDueDateValue() != null ? customerDTO.getDueDateValue().toString() : "");
        text.append(" ");
        text.append(period.getDescription(getApi().getCallerLanguageId()));

        return text.toString();
    }

    private String userStatus() {
        UserWS user = getApi().getUserWS(userId);
        if (user.getDeleted() == 1) {
            return getStringFromBundleByKey("user.userstatus.deleted");
        } else if(user.getStatus() != null){
            return new UserStatusDAS().find(user.getStatusId()).getDescription(user.getLanguageId());
        } else {
            return null;
        }
    }
}
