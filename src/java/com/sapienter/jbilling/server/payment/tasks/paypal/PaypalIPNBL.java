package com.sapienter.jbilling.server.payment.tasks.paypal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.payment.tasks.paypal.db.PaypalIPNDAS;
import com.sapienter.jbilling.server.payment.tasks.paypal.db.PaypalIPNDTO;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * Created by usman on 8/21/14.
 */
public class PaypalIPNBL {

    private static IWebServicesSessionBean webServicesSessionBean = (IWebServicesSessionBean) Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    private final static String PAYPAL_TRANSACTION_ID = "paypal.transaction.id";
    private final static int PAYPALIPN_PM_ID = 12;

    private final static int VERIFIED_PAYPALIPN=1;

    private final static int CC_PM_ID = 1;

    private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private final static String CC_MF_NUMBER = "cc.number";
    private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private final static String CC_MF_TYPE = "cc.type";

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaypalIPNBL.class));

    private static Integer baseUserId=null;

    public static void save(PaypalIPNWS paypalIPNWS){

        PaypalIPNDTO paypalIPNDTO =new PaypalIPNDTO(paypalIPNWS);

        PaypalIPNDAS paypalIPNDAS= new PaypalIPNDAS();

        PaypalIPNDTO savedIPN = paypalIPNDAS.makePersistent(paypalIPNDTO);
        paypalIPNDAS.flush();

        PaymentDAS paymentDAS=new PaymentDAS();
        Boolean paymentProcessed=paymentDAS.findPaymentProcessed(savedIPN.getTxnId());
        Boolean parentPaymentProcessed=paymentDAS.findPaymentProcessed(savedIPN.getParentTxnId());
        PaymentDTO originalPayment =paymentDAS.findPaymentProcessedDTO(savedIPN.getParentTxnId());

        if (!paymentProcessed && savedIPN.getVerified()==VERIFIED_PAYPALIPN) {

            MetaFieldDAS metaFieldDAS=new MetaFieldDAS();
            Integer metafieldValueId=null;
            if (savedIPN.getPayerEmail()!=null){
                metafieldValueId = metaFieldDAS.getIdByEmail(savedIPN.getPayerEmail());
            }
            if(parentPaymentProcessed){
                baseUserId=originalPayment.getBaseUser().getId();
            }else
            {
            if(metafieldValueId==null){
//              using userId from jbilling.properties file
                baseUserId=Integer.parseInt(Util.getSysProp("anonymous_user_id"));
            }else{
                AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
                baseUserId=accountInformationTypeDAS.getBaseUserIdbyMetaFiedId(metafieldValueId);
            }
            }
            PaymentWS payment = new PaymentWS();

            payment.setId(0);
            payment.setMethodId(null);
            payment.setUserId(baseUserId);

            payment.setIsPreauth(null);
            payment.setPaymentDate(savedIPN.getPaymentDate());
            payment.setDeleted(0);
            payment.setCurrencyId(1);

            if (savedIPN.getPaymentStatus().equals("Refunded")){
                LOG.debug("Setting fields for Refund payment IPN and unlink invoice if any exists.");
                if (originalPayment.getInvoicesMap().size()>0){
                    for (PaymentInvoiceMapDTO linkedInvoice : originalPayment.getInvoicesMap()) {
                        // we need to unlink invoice from the payment before making refund on that payment
                        webServicesSessionBean.removePaymentLink(linkedInvoice.getInvoiceEntity().getId(), originalPayment.getId());
                    }
                }
                payment.setIsRefund(1);
                payment.setPaymentId(originalPayment.getId());
                payment.setAmount(com.sapienter.jbilling.server.util.Util.string2decimal(savedIPN.getMcGross()));
            }else{
                payment.setAmount(savedIPN.getMcGross());
                payment.setIsRefund(0);
                payment.setPaymentId(null);
            }

            PaymentInformationWS paypalIPN = new PaymentInformationWS();
            paypalIPN.setPaymentMethodTypeId(PAYPALIPN_PM_ID);
            paypalIPN.setProcessingOrder(Integer.valueOf(3));

            List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
            addMetaField(metaFields, PAYPAL_TRANSACTION_ID, false, true,
                    DataType.STRING, 1, savedIPN.getTxnId());
            paypalIPN.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
            payment.getPaymentInstruments().add(paypalIPN);

            payment.setId(webServicesSessionBean.applyPayment(payment, null));


        }


    }
    private static void addMetaField(List<MetaFieldValueWS> metaFields,
                                     String fieldName, boolean disabled, boolean mandatory,
                                     DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }
    public static UserWS createUser(String userName,String email,String firstName,String lastName) {

        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName(userName);
        newUser.setPassword("123qwe");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(1);
        newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(email);
        metaField1.setGroupId(1);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue(firstName);
        metaField2.setGroupId(1);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue(lastName);
        metaField3.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[] { metaField1,
                metaField2, metaField3});


        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        PaymentInformationWS cc = createCreditCard("PaypalIPN",
                 "4111111111111152",
                expiry.getTime());

        newUser.getPaymentInstruments().add(cc);


        LOG.debug("Creating user ...");
        newUser.setUserId(webServicesSessionBean.createUser(newUser));

        LOG.debug("User created with id:" + newUser.getUserId());
        return newUser;

    }

    public static PaymentInformationWS createCreditCard(String cardHolderName,
                                                        String cardNumber, Date date) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(CC_PM_ID);
        cc.setProcessingOrder(new Integer(1));
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
                DataType.STRING, 1, cardHolderName);
        addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.STRING, 2,
                cardNumber);
        addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
                DataType.STRING, 3, new SimpleDateFormat(
                Constants.CC_DATE_FORMAT).format(date));
        // have to pass meta field card type for it to be set
        addMetaField(metaFields, CC_MF_TYPE, true, false,
                DataType.INTEGER, 4, new Integer(0));
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    public static String removeChar(String s, char c) {

        String r = "";

        for (int i = 0; i < s.length(); i ++) {
            if (s.charAt(i) != c) r += s.charAt(i);
        }

        return r;
    }
}
