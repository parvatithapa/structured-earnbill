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

package com.sapienter.jbilling.server.payment.tasks;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.util.Constants;

public class PaymentsGatewayTask extends PaymentTaskWithTimeout implements
        PaymentTask {

    // mandatory parameters
    public static final String PARAMETER_MERCHANT_ID = "merchant_id";
    public static final String PARAMETER_PASSWORD = "password";
    private static final String PARAMETER_HOST = "host"; // "www.paymentsgateway.net";
    private static final String PARAMETER_PORT = "port"; // 6050

    // optional parameters
    public static final String PARAMETER_AVS = "submit_avs";
    public static final String PARAMETER_TEST = "test";
    private static final String PARAMETER_TEST_HOST = "test_host"; // "www.paymentsgateway.net";
    private static final String PARAMETER_TEST_PORT = "test_port"; // 6050

    private static final int CONNECTION_TIME_OUT = 10000; // in millisec
    private static final int REPLY_TIME_OUT = 30000; // in millisec

    // Credit Card Types
    private static final int CC_TYPE_VISA = 2;
    private static final int CC_TYPE_MASTER = 3;
    private static final int CC_TYPE_AMEX = 4;
    private static final int CC_TYPE_DISC = 6;
    private static final int CC_TYPE_DINE = 7;
    // unsupported though
    private static final int CC_TYPE_JCB = 8;

    /*
     * jBilling defs. public static final Integer PAYMENT_METHOD_CHEQUE = new
     * Integer(1); public static final Integer PAYMENT_METHOD_VISA = new
     * Integer(2); public static final Integer PAYMENT_METHOD_MASTERCARD = new
     * Integer(3); public static final Integer PAYMENT_METHOD_AMEX = new
     * Integer(4); public static final Integer PAYMENT_METHOD_ACH = new
     * Integer(5); public static final Integer PAYMENT_METHOD_DISCOVER = new
     * Integer(6); public static final Integer PAYMENT_METHOD_DINERS = new
     * Integer(7); public static final Integer PAYMENT_METHOD_PAYPAL = new
     * Integer(8);
     */

    // Payment Method

    private static final int PAYMENT_METHOD_CC = 1;
    private static final int PAYMENT_METHOD_ACH = 2;
    private static final int PAYMENT_METHOD_CHEQUE = 3;

    // CC Transaction Types
    private static final String CC_SALE = "10";
    private static final String CC_AUTH = "11";
    private static final String CC_CAPT = "12";
    private static final String CC_CRED = "13"; // CC Refunds

    // CC Transaction Types
    private static final String EFT_SALE = "20";
    private static final String EFT_AUTH = "21";
    private static final String EFT_CAPT = "22";
    private static final String EFT_CRED = "23"; // ACH Refund
    private static final String EFT_VERIFY = "26"; // EFT Verify Only - for use
                                                    // with ATMVerify (R)

    // Response Type
    private static final String RESPONSE_CODE_APPROVED = "A"; // Approved
    private static final String RESPONSE_CODE_DECLINED = "D"; // Declined
    private static final String RESPONSE_CODE_ERROR = "E"; // Exception

    private FormatLogger log;
    private String payloadData = "";

    public PaymentsGatewayTask() {
        log = new FormatLogger(Logger.getLogger(PaymentsGatewayTask.class));
    }

    public boolean process(PaymentDTOEx paymentInfo)
            throws PluggableTaskException {

        boolean retValue = false;

        try {

            int method = -1;
            boolean preAuth = false;
            if (paymentInfo.getIsPreauth() != null
                    && paymentInfo.getIsPreauth().intValue() == 1) {
                preAuth = true;
            }
            log.debug("Payment request Received ; Method : "
                    + paymentInfo.getMethod());
            
            PaymentInformationBL piBl = new PaymentInformationBL();
            if (piBl.isCreditCard(paymentInfo.getInstrument())) {
                method = PAYMENT_METHOD_CC;
            } else if (piBl.isCheque(paymentInfo.getInstrument()) && piBl.isACH(paymentInfo.getInstrument())) {
                method = PAYMENT_METHOD_CHEQUE;
            } else if (piBl.isACH(paymentInfo.getInstrument())) {
                method = PAYMENT_METHOD_ACH;
            } else {
                // hmmm problem
                log.error("Can't process without a credit card, ach or cheque");
                throw new PluggableTaskException(
                        "Credit card/ACH/Cheque not present in payment");
            }

//          if (paymentInfo.getIsRefund() == 1
//                  && (paymentInfo.getPayment() == null || paymentInfo
//                          .getPayment().getAuthorization() == null)) {
//              log.error("Can't process refund without a payment with an"
//                      + " authorization record");
//              throw new PluggableTaskException("Refund without previous "
//                      + "authorization");
//          }

            // prepare common data for sending to Gateway
            validateParameters();
            String data = getChargeData(paymentInfo, method, preAuth);

            PaymentAuthorizationDTO response = processPGRequest(data);
            paymentInfo.setAuthorization(response);

            log.debug("Response code " + response.getCode1());
            if (RESPONSE_CODE_APPROVED.equals(response.getCode1())) {
                paymentInfo.setPaymentResult(new PaymentResultDAS()
                        .find(Constants.RESULT_OK));
                log.debug("result is ok");
            } else {
                paymentInfo.setPaymentResult(new PaymentResultDAS()
                        .find(Constants.RESULT_FAIL));
                log.debug("result is fail");
            }

            PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
            bl.create(response, paymentInfo.getId());

        } catch (PluggableTaskException e) {
            log.error("PluggableTaskException", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception", e);
            throw new PluggableTaskException(e);
        }

        log.debug("process returning " + retValue);

        return retValue;

    }

    private String getChargeData(PaymentDTOEx paymentInfo, int method,
            boolean preAuth) throws PluggableTaskException {

        String payloadData = new String("");
        try {

            payloadData += "pg_merchant_id="
                    + ensureGetParameter(PARAMETER_MERCHANT_ID) + "\n";
            payloadData += "pg_password="
                    + ensureGetParameter(PARAMETER_PASSWORD) + "\n";
            payloadData += "pg_total_amount="
                    + (paymentInfo.getAmount().abs()).toString() + "\n";
            payloadData += "pg_transaction_type="
                    + getTransType(paymentInfo, method, preAuth) + "\n";
            // common data
            // pg_consumer_id-assigned by merchant, returned with response
            // ecom_consumerorderid-assigned by merchant, returned with response
            // ecom_walletid-assigned by merchant, returned with response
            // pg_billto_postal_name_company-company name

            Integer userId = paymentInfo.getUserId();
            ContactBL contact = new ContactBL();
            contact.set(userId);
            ContactDTO entity = contact.getEntity();
            payloadData += "Ecom_BillTo_Postal_Name_First="
                    + entity.getFirstName() + "\n";
            payloadData += "Ecom_BillTo_Postal_Name_Last="
                    + entity.getLastName() + "\n";

            if ("true".equals(getOptionalParameter(PARAMETER_AVS, "false"))) {

                payloadData += "ecom_billto_postal_street_line1="
                        + entity.getAddress1() + "\n";
                payloadData += "ecom_billto_postal_street_line2="
                        + entity.getAddress2() + "\n";
                payloadData += "ecom_billto_postal_city=" + entity.getCity()
                        + "\n";
                payloadData += "ecom_billto_postal_stateprov="
                        + entity.getStateProvince() + "\n";
                payloadData += "ecom_billto_postal_postalcode="
                        + entity.getPostalCode() + "\n";
                payloadData += "ecom_billto_postal_countrycode="
                        + entity.getCountryCode() + "\n";
                payloadData += "ecom_billto_telecom_phone_number="
                        + entity.getPhoneNumber() + "\n";
                payloadData += "ecom_billto_online_email=" + entity.getEmail()
                        + "\n";
            }

            // pg_billto_ssn-customer?s social security number
            // pg_billto_dl_number-customer?s driver?s license number
            // pg_billto_dl_state-customer?s driver?s license state of issue
            // pg_billto_date_of_birth-customer?s date of birth (MM/DD/YYYY)
            // pg_entered_by-name or ID of the person entering the data; appears
            // in the Virtual Terminal transaction display window

            // payloadData+="pg_customer_ip_address="+InetAddress.getLocalHost().getHostAddress()+"\n";
            payloadData += "pg_customer_ip_address=1.1.11.1\n";
            payloadData += "pg_software_name=jBilling\n";
            payloadData += "pg_software_version=2.0.0\n";

            // pg_avs_method-specifies which AVS checks to perform on the
            // transaction (if any);
            // makes some optional fields required. See Appendix C for more
            // information on AVS.

            PaymentInformationBL piBl = new PaymentInformationBL();
            if (method == PAYMENT_METHOD_CC) {
            	char[] cardNumber = piBl.getCharMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.PAYMENT_CARD_NUMBER);
                String ccType = getCCType(piBl.getPaymentMethod(cardNumber));
                payloadData += "ecom_payment_card_type=" + ccType + "\n";
                payloadData += "ecom_payment_card_name="
                        + new String(piBl.getCharMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.TITLE)) + "\n";
                payloadData += "ecom_payment_card_number="
                        + cardNumber + "\n";

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(piBl.getDateMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.DATE));

                payloadData += "ecom_payment_card_expdate_month="
                        + calendar.get(Calendar.MONTH) + "\n";
                payloadData += "ecom_payment_card_expdate_year="
                        + calendar.get(Calendar.YEAR) + "\n";

            } else if (method == PAYMENT_METHOD_ACH) {

                /*
                 * "Ecom_Payment_Check_AccounT_Type=S",
                 * "Ecom_Payment_Check_Account= 14730",
                 * "Ecom_Payment_Check_TRN=021000021",
                 */

                String accType = "";
                payloadData += "Ecom_Payment_Check_TRN="
                        + piBl.getCharMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.BANK_ROUTING_NUMBER) + "\n";
                payloadData += "Ecom_Payment_Check_Account="
                        + piBl.getCharMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.BANK_ACCOUNT_NUMBER) + "\n";

                String accountType = piBl.getStringMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.BANK_ACCOUNT_TYPE);
                if (accountType.equalsIgnoreCase(Constants.ACH_CHECKING)) {
                    accType += "C";
                } else if (accountType.equalsIgnoreCase(Constants.ACH_SAVING)) {
                    accType += "S";
                } else {
                    log.error("unknown Account Type : "
                            + accountType);
                    throw new PluggableTaskException("unknown Account Type");
                }

                payloadData += "Ecom_Payment_Check_AccounT_Type=" + accType
                        + "\n";

            } else if (method == PAYMENT_METHOD_CHEQUE) {
                payloadData += "Ecom_Payment_Check_TRN="
                        + piBl.getCharMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.BANK_ROUTING_NUMBER) + "\n";
                payloadData += "Ecom_Payment_Check_Account="
                        + piBl.getCharMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.BANK_ACCOUNT_NUMBER) + "\n";
                payloadData += "Ecom_Payment_Check_Account_Type=C\n";
                payloadData += "ecom_payment_check_checkno=" + 
                		piBl.getStringMetaFieldByType(paymentInfo.getInstrument(), MetaFieldType.CHEQUE_NUMBER) + "\n";
                // payloadData += "PG_Entry_Class_Code=POS\n";
            }
        } catch (PluggableTaskException e) {
            log.error("PluggableTaskException", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception", e);
            throw new PluggableTaskException(e);
        }
        payloadData += "endofdata\n";

        //TODO: check this log and payloadData values
        String maskedCCNumber = payloadData.toString().replaceAll("ecom_payment_card_number=[^\n]*", "ecom_payment_card_number=******");
        log.debug("charge data : " + maskedCCNumber);
        return payloadData;

    }

    /*
     * ecom_payment_card_type-credit card issuer from Table 5-Credit Card Issuer
     * Types below ecom_payment_card_name-cardholder name as it appears on the
     * card ecom_payment_card_number-card account number
     * ecom_payment_card_expdate_month-numeric month of expiration (Jan = 1)
     * ecom_payment_card_expdate_year-four-digit year of expiration
     * ecom_payment_card_verification-CVV2/verification number
     * pg_procurement_card-indicates procurement card transaction, requires
     * pg_sales_tax and pg_customer_acct_code fields
     * pg_customer_acct_code-accounting information for procurement card
     * transactions pg_cc_swipe_data-magstripe data from track one or two
     * pg_mail_or_phone_order-indicates mail order or phone order transaction
     * (as opposed to an Internet-based transaction)
     */

    private void validateParameters() throws PluggableTaskException {
        ensureGetParameter(PARAMETER_MERCHANT_ID);
        ensureGetParameter(PARAMETER_PASSWORD);
    }

    public void failure(Integer userId, Integer retry) {
    }

    /*
     * Credit Card 10 SALE Customer is charged 11 AUTH ONLY Authorization only,
     * CAPTURE transaction required 12 CAPTURE Completes AUTH ONLY transaction
     * 13 CREDIT Customer is credited 14 VOID Cancels non-settled transactions
     * 15 PRE-AUTH Customer charge approved from other source EFT 20 SALE
     * Customer is charged 21 AUTH ONLY Authorization only, CAPTURE transaction
     * required 22 CAPTURE Completes AUTH ONLY transaction 23 CREDIT Customer is
     * credited 24 VOID Cancels non-settled transactions 25 FORCE Customer
     * charged (no validation checks) 26 VERIFY ONLY Verification only, no
     * customer charge
     */

    public String getTransType(PaymentDTOEx paymentInfo, int method,
            boolean preAuth) throws PluggableTaskException {

        String transType = new String();

        if (paymentInfo.getIsRefund() == 1 || 
                paymentInfo.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            if (method == PAYMENT_METHOD_CC) {
                transType += CC_CRED;
            } else if (method == PAYMENT_METHOD_ACH) {
                transType += EFT_CRED;
            } else {
                log.error("Can't process refund for this method: " + method);
                throw new PluggableTaskException(
                        "Can't process refund for this method");
            }
        } else if (paymentInfo.getIsRefund() == 0) {

            switch (method) {
            case PAYMENT_METHOD_CC:
                if (preAuth) {
                    transType += CC_AUTH;
                } else {
                    transType += CC_SALE;
                }
                break;
            case PAYMENT_METHOD_ACH:
                if (preAuth) {
                    transType += EFT_AUTH;
                } else {
                    transType += EFT_SALE;
                }
                break;
            case PAYMENT_METHOD_CHEQUE:
                transType += EFT_VERIFY;
                break;
            default:
                log.error("Unknown payment method : " + method);
                throw new PluggableTaskException(
                        "Unknown payment method : Neither Credit Card, Cheque nor ACH ");
            }
        } else {
            log.error("Unknown transaction type : "
                    + paymentInfo.getIsRefund());
            throw new PluggableTaskException(
                    "Unknown transaction type : Neither Credit Card, Cheque nor ACH");
        }
        return transType;
    }

    public String getCCType(Integer type) {

        log.debug("credit card type: " + type);
        String ccType = "";
        switch (type) {

        case CC_TYPE_VISA:
            ccType += "VISA";
            break;

        case CC_TYPE_MASTER:
            ccType += "MAST";
            break;
        case CC_TYPE_AMEX:
            ccType += "AMER";
            break;

        case CC_TYPE_DISC:
            ccType += "DISC";
            break;

        case CC_TYPE_DINE:
            ccType += "DINE";
            break;

        case CC_TYPE_JCB:
            ccType += "JCB";
            break;

        default:
            log.error("Unknown credit card type: " + type);
            break;
        // throw new PluggableTaskException("Cannot find credit type");
        }
        return ccType;
    }

    private PaymentAuthorizationDTO processPGRequest(String data)
            throws PluggableTaskException {

        PaymentAuthorizationDTO dbRow = new PaymentAuthorizationDTO();

        String negRep = "";
        String autOut = "";
        
        try {
            BufferedReader br = callPG(data);
            String line = br.readLine();
            // log.debug("Response line: "+br);
            while (line != null) {
                // check for end of message
                if (line.equals("endofdata")) {
                    log.debug("ENDOFDATA");
                    break;
                }

                log.debug("Response line: " + line);
                // parse and display name/value pair
                int equalPos = line.indexOf('=');
                String name = line.substring(0, equalPos);
                String value = line.substring(equalPos + 1);
                log.debug(name + "=" + value);
                if (name.equals("pg_response_type")) {
                    dbRow.setCode1(value); // code if 1 it is ok
                }
                if (name.equals("pg_response_code")) {
                    dbRow.setCode2(value);
                }

                if (name.equals("pg_authorization_code")) {
                    dbRow.setApprovalCode(value);
                }
                if (name.equals("pg_response_description")) {
                    dbRow.setResponseMessage(value);
                }
                if (name.equals("pg_trace_number")) {
                    dbRow.setTransactionId(value);
                }
                // preAuth
                if (name.equals("pg_preauth_result")) {
                    dbRow.setCode3(value);
                }
                // Verify
                if(name.equals("pg_preauth_description")) {
                    autOut = value;
                }
                if(name.equals("pg_preauth_neg_report")) {
                    negRep = value;
                }

                // get next line
                line = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            log.error("Error processing payment", e);
        }
        dbRow.setProcessor("PaymentsGateway");

        if (autOut != null && !"".equals(autOut.trim())) {
            dbRow.setResponseMessage(dbRow.getResponseMessage() + " - " + autOut);
        }
        
        if (negRep != null && !"".equals(negRep.trim()) && !negRep.equals(autOut)) {
            dbRow.setResponseMessage(dbRow.getResponseMessage() + " (" + negRep + ")");
        }
        
        return dbRow;
    }

    public boolean preAuth(PaymentDTOEx payment) throws PluggableTaskException {
        log = new FormatLogger(Logger.getLogger(PaymentsGatewayTask.class));
        PaymentInformationBL piBl = new PaymentInformationBL();
        log.error("Prcessing preAuth Reqquest");
        int method = 1; // CC
        boolean preAuth = true;
        if (piBl.isCreditCard(payment.getInstrument())) {
            method = PAYMENT_METHOD_CC;
        } else if (piBl.isACH(payment.getInstrument()) && piBl.isCheque(payment.getInstrument())) {
            method = PAYMENT_METHOD_CHEQUE;
        } else if (piBl.isACH(payment.getInstrument())) {
            method = PAYMENT_METHOD_ACH;
        } else {
            // hmmm problem
            log.error("Can't process without a credit card or ach");
            throw new PluggableTaskException(
                    "Credit card/ACH not present in payment");
        }

        try {
            validateParameters();
            String data = getChargeData(payment, method, preAuth);
            PaymentAuthorizationDTO response = processPGRequest(data);

            PaymentAuthorizationDTO authDtoEx = new PaymentAuthorizationDTO(
                    response);
            PaymentAuthorizationBL bl = new PaymentAuthorizationBL();
            bl.create(authDtoEx, payment.getId());

            payment.setAuthorization(authDtoEx);
            return false;
        } catch (Exception e) {
            log.error("error trying to pre-authorize", e);
            return true;
        }
    }

    /*
     * pay load for confirmPreAuth
     * 
     * Pg_merchant_id N8 M pg_password A20 M pg_transaction_type L M
     * pg_merchant_data_[1-9] A40 O pg_original_trace_number A36 M
     * pg_original_authorization_code A80 C (AC)
     */

    public boolean confirmPreAuth(PaymentAuthorizationDTO auth,
            PaymentDTOEx paymentInfo) throws PluggableTaskException {

        log.error("Processing confirmPreAuth Request");
        boolean retValue = false;

        try {

            if (!RESPONSE_CODE_APPROVED.equals(auth.getCode1())) {
                log.error("Cannot process failed preAuth");
                return retValue;
            }
            payloadData += "pg_merchant_id="
                    + ensureGetParameter(PARAMETER_MERCHANT_ID) + "\n";
            payloadData += "pg_password="
                    + ensureGetParameter(PARAMETER_PASSWORD) + "\n";
            String transType = "";
            PaymentInformationBL piBl = new PaymentInformationBL();
            if (piBl.isCreditCard(paymentInfo.getInstrument())) {
                transType += CC_CAPT;

            } else if (piBl.isACH(paymentInfo.getInstrument())) {
                transType += EFT_CAPT;

            } else {
                // hmmm problem!!! this should not happen
                log.error("Can't process without a credit card or ach");
                throw new PluggableTaskException(
                        "Credit card/ACH not present in payment");
                // return false;
            }

            payloadData += "pg_transaction_type=" + transType + "\n";
            payloadData += "pg_original_trace_number="
                    + auth.getTransactionId() + "\n";
            payloadData += "pg_original_authorization_code="
                    + auth.getApprovalCode() + "\n";
            payloadData += "endofdata\n";

            BufferedReader br = callPG(payloadData);
            String line = br.readLine();

            while (line != null) {
                // check for end of message
                if (line.equals("endofdata")) {
                    log.debug("ENDOFDATA");
                    break;
                }

                log.debug("Response line: " + line);
                // parse and display name/value pair
                int equalPos = line.indexOf('=');
                String name = line.substring(0, equalPos);
                String value = line.substring(equalPos + 1);

                if (name.equals("pg_response_type")) {
                    if (RESPONSE_CODE_APPROVED.equals(value)) {
                        paymentInfo.setPaymentResult(new PaymentResultDAS()
                                .find(Constants.RESULT_OK));
                        log.debug("preAuth result is ok");
                        retValue = false;
                    } else {

                        paymentInfo.setPaymentResult(new PaymentResultDAS()
                                .find(Constants.RESULT_FAIL));
                        log.debug("preAuth result is failed");
                        retValue = true;
                    }

                    auth.setCode1(value);

                }
                if (name.equals("pg_response_code")) {
                    auth.setCode2(value);
                }

                if (name.equals("pg_authorization_code")) {
                    auth.setApprovalCode(value);
                }
                if (name.equals("pg_response_description")) {
                    auth.setResponseMessage(value);
                }
                if (name.equals("pg_trace_number")) {
                    auth.setTransactionId(value);
                }

                // get next line
                line = br.readLine();
            }
            br.close();

        } catch (Exception e) {
            log.error("error trying to confirm pre-authorize", e);
            throw new PluggableTaskException(e);
        }

        return retValue;

    }

    private BufferedReader callPG(String data) throws PluggableTaskException {

        String host = null;
        int port;
        if ("true".equals(getOptionalParameter(PARAMETER_TEST, "false"))) {
            host = super.ensureGetParameter(PARAMETER_TEST_HOST);
            port = Integer.parseInt(super.ensureGetParameter(PARAMETER_TEST_PORT));
            log.debug("Running task in test mode!");
        } else {
            host = super.ensureGetParameter(PARAMETER_HOST);
            port = Integer.parseInt(super.ensureGetParameter(PARAMETER_PORT));
        }

        SocketFactory factory = SSLSocketFactory.getDefault();
        try (SSLSocket s = (SSLSocket) factory.createSocket(host, port);) {

            log.debug("connected to :" + host + "on " + port);
            s.setEnabledCipherSuites(s.getSupportedCipherSuites());
            log.debug("cipher=" + s.getSession().getCipherSuite());

            try (DataOutputStream dos = new DataOutputStream(s.getOutputStream());) {

                // write the content and be sure to flush
                log.debug("Writing data to PG " + data);
                dos.writeBytes(data);
                dos.flush();

                // read the response
                return new BufferedReader(new InputStreamReader(s.getInputStream()));
            }
        } catch (Exception e) {
            log.error("Error processing payment", e);
            return null;
        }
    }
}
