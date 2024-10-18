package com.sapienter.jbilling.server.boa.batch;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessingErrorDAS;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessingErrorDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.payment.*;
import com.sapienter.jbilling.server.payment.db.*;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Javier Rivero
 * @since 05/01/16.
 */
public class BOAFileWriter implements ItemWriter<BOAFileRecord> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BOAFileWriter.class));
    IPaymentSessionBean session = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);

    /* A posted Cash Concentration and Disbursement (CCD) ACH credit, with 0 or 1 addenda, or settlement of a received International
     * ACH Transaction (IAT)*/
    private static final Integer PREAUTHORIZED_ACH_CREDIT = 165;
    /* A credit entry to offset ACH debit activity originated through Bank of America Merrill Lynch.*/
    private static final Integer ACH_SETTLEMENT_CREDIT = 166;
    /*A credit entry to offset ACH credit activity originated through Bank of America Merrill Lynch, then returned by
    /* the Receiving Depository Financial Institution*/
    private static final Integer INDIVIDUAL_ACH_RETURN = 257;
    /*A debit entry to offset ACH debit return(s) which were originated through Bank of America Merrill Lynch */
    private static final Integer INDIVIDUAL_ACH_RETURN_ITEM_DEBIT = 557;
    /*An incoming wire received from another account held at Bank of America.*/
    private static final Integer INCOMING_INTERNAL_MONEY_TRANSFER = 191;
    /* An incoming domestic wire */
    private static final Integer INCOMING_MONEY_TRANSFER_CREDIT  = 195;
    /*An incoming international wire.*/
    private static final Integer WIRE_IN_INTERNATIONAL = 208;
    private static final String TRANSACTION_TYPE_ACH = "ACH";
    private static final String TRANSACTION_TYPE_WIRE = "Wire";
    private static final String JBILLING_USER_ID_PATTERN = "(?<=SNDR\\sREF:).+?(?=\\s|$)";
    private UserDAS userDas = new UserDAS();
    private PaymentDAS paymentDas = new PaymentDAS();
    private PaymentMethodDAS paymentMethodDas = new PaymentMethodDAS();
    private Pattern pattern = Pattern.compile(JBILLING_USER_ID_PATTERN);
    private Matcher matcher = pattern.matcher("");
    private JobParameters jobParams;
    MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

    //   937 – Deposit Correction/Non-Cash Credit – Credit customer accounts
    private static final Integer DEPOSIT_CORRECTION_CREDIT = 937;
    //   938 – Deposit Correction Debit – Debit customer accounts
    private static final Integer DEPOSIT_CORRECTION_DEBIT = 938;
    private static final String BOA_TRANSACTION_TIME = "boa.transaction.time";
    private static final String BOA_DEPOSIT_FILE_NAME = "boa.deposit.file.name";
    private static final String BOA_BANK_TRANSACTION_ID = "boa.bank.transaction.id";
    private static final String BOA_FUNDING_ACCOUNT_ID = "boa.funding.account.id";
    private static final String BOA_TRANSACTION_DATE = "boa.transaction.date";
    private static final String BOA_TRANSACTION_TYPE = "boa.transaction.type";


    @Override
    public void write(List<? extends BOAFileRecord> bankRecords) throws Exception {
        boolean isBankPaymentApproved = false;

        for (BOAFileRecord bankRecord : bankRecords) {
            try {
                LOG.info(bankRecord.toString());

                if (bankRecord.getDepositFileDirectory().equals(jobParams.getString(Constants.BOA_JOB_PARAM_READ_FROM_DAILY_FILES_DIRECTORY))){
                    isBankPaymentApproved = true;
                } else if (bankRecord.getDepositFileDirectory().equals(jobParams.getString(Constants.BOA_JOB_PARAM_READ_FROM_INTRADAY_FILES_DIRECTORY))){
                    isBankPaymentApproved = false;
                }

                String customerAccountNumber = bankRecord.getCustReferenceNo();
                Integer userId = customerAccountNumber != null ?
                        metaFieldDAS.getUserIdByCustomerAccountNumber(customerAccountNumber.trim().substring(0, customerAccountNumber.length()-1)) : null;

                if (null == userId) {
                    LOG.warn("No valid jbilling userId found in the record. Default userId will be used");
                    userId = Integer.parseInt(jobParams.getString(Constants.BOA_JOB_PARAM_DEFAULT_USER_ID));
                    bankRecord.setUserId(userId);
                } else {
                    Integer companyId = userDas.getUserCompanyId(userId);
                    if (!userDas.exists(userId, companyId)){
                        LOG.warn("No valid jbilling userId found in the record. Default userId will be used");
                        userId = Integer.parseInt(jobParams.getString(Constants.BOA_JOB_PARAM_DEFAULT_USER_ID));
                    }
                    bankRecord.setUserId(userId);
                }

                PaymentDTO payment = paymentDas.findPaymentByBankReferenceId(userId, bankRecord.getBankReferenceNo());
                PaymentBL paymentBL = new PaymentBL();
                PreferenceBL pref = new PreferenceBL();
                Integer entityId = new UserDAS().find(userId).getCompany().getId();
                pref.set(entityId, Constants.PREFERENCE_BANK_PAYMENT_ALERT_USER_ID);
                Integer prefUserId=pref.getInt();

                String paymentNotes = "";
                if(payment!= null) {
                    if (payment.getPaymentNotes() != null) {
                        paymentNotes = payment.getPaymentNotes();
                    }
                }
                else {
                    LOG.error("No payment found with bankReferenceNo:" + bankRecord.getBankReferenceNo() + " for userId:" + userId);
                }

                StringBuilder buffer = new StringBuilder(paymentNotes);
                buffer.append(bankRecord.getRawData());
                paymentNotes = buffer.toString();

                if (isBankPaymentApproved && payment != null) {
                    payment.setPaymentNotes(paymentNotes);
                    PaymentDTOEx paymentDtoEx = new PaymentDTOEx(payment);
                    paymentDtoEx.setInstrument(paymentDtoEx.getPaymentInstrumentsInfo().get(0).getPaymentInformation());
                    paymentDtoEx.getInstrument().setPaymentMethod(paymentMethodDas.find(Constants.PAYMENT_METHOD_BANK_WIRE));

                    if (Integer.valueOf(payment.getPaymentResult().getId()).equals(Constants.RESULT_ENTERED) &&
                            !bankRecord.getTransactionType().equals(INDIVIDUAL_ACH_RETURN) && !bankRecord.getTransactionType().equals(DEPOSIT_CORRECTION_DEBIT)
                            && !bankRecord.getTransactionType().equals(INDIVIDUAL_ACH_RETURN_ITEM_DEBIT)) {
                        paymentDtoEx.setPaymentResult(new PaymentResultDAS().find(Constants.PAYMENT_RESULT_SUCCESSFUL));
                        session.update(userId, paymentDtoEx);
                    }

                    Integer notificationUserId = userId;
                    if (bankRecord.getTransactionType().equals(INDIVIDUAL_ACH_RETURN) || bankRecord.getTransactionType().equals(DEPOSIT_CORRECTION_DEBIT)){
                        //paymentDtoEx.getBank().setSubType(INDIVIDUAL_ACH_RETURN);
                        paymentDtoEx.setUserId(prefUserId);
                        paymentDtoEx.setPaymentNotes(bankRecord.getRawData());
                        notificationUserId = prefUserId;
                    }
                    paymentBL.sendNotification(paymentDtoEx, new UserDAS().find(notificationUserId).getCompany().getId(),
                            paymentDtoEx.getPaymentResult() != null ? paymentDtoEx.getPaymentResult().getId() : Constants.RESULT_OK.intValue());
                }
                else {
                    if (bankRecord.getTransactionType().equals(PREAUTHORIZED_ACH_CREDIT) || bankRecord.getTransactionType().equals(ACH_SETTLEMENT_CREDIT) ||
                            bankRecord.getTransactionType().equals(INDIVIDUAL_ACH_RETURN_ITEM_DEBIT) || bankRecord.getTransactionType().equals(DEPOSIT_CORRECTION_CREDIT)) {
                        //Create a new payment record for ACH transaction
                        createPaymentRecord(bankRecord, TRANSACTION_TYPE_ACH, isBankPaymentApproved);
                        //} else if (bankRecord.getTransactionType().equals(ACH_SETTLEMENT_CREDIT)) {
                        //Do nothing. Ignore ACH Post transactions
                    } else if (bankRecord.getTransactionType().equals(INDIVIDUAL_ACH_RETURN) || bankRecord.getTransactionType().equals(DEPOSIT_CORRECTION_DEBIT)) {
                        //This is a ACH Return transaction. Find and reverse payment with the same bank-reference number

                        //NOTE: Nested try
                        try(PaymentDTOEx paymentDtoEx = payment != null ? new PaymentDTOEx(payment) : populatePaymentDTOExFromBankRecord(bankRecord, TRANSACTION_TYPE_ACH)) {
                            paymentDtoEx.setPaymentNotes(bankRecord.getRawData());
                            //paymentDtoEx.getBank().setSubType(INDIVIDUAL_ACH_RETURN);
                            paymentDtoEx.setUserId(prefUserId);
                            paymentBL.sendNotification(paymentDtoEx, new UserDAS().find(prefUserId).getCompany().getId(),
                                    paymentDtoEx.getPaymentResult() != null ? paymentDtoEx.getPaymentResult().getId() : Constants.RESULT_OK.intValue());
                        }
                    } else if (bankRecord.getTransactionType().equals(INCOMING_INTERNAL_MONEY_TRANSFER) || bankRecord.getTransactionType().equals(INCOMING_MONEY_TRANSFER_CREDIT)
                            || bankRecord.getTransactionType().equals(WIRE_IN_INTERNATIONAL)) {
                        //Create a new payment record for Wire transaction
                        createPaymentRecord(bankRecord, TRANSACTION_TYPE_WIRE, isBankPaymentApproved);
                    }
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                LOG.debug("Error while processing file: " + bankRecord.getDepositFileName() + ": " + sw.toString());
                BoaBaiProcessingErrorDTO boaBaiProcessingErrors =
                        new BoaBaiProcessingErrorDTO(bankRecord.getDepositFileName(),bankRecord.getRawData(),sw.toString().substring(0,500));
                new BoaBaiProcessingErrorDAS().save(boaBaiProcessingErrors);

            }
        }
    }

    private Integer createPaymentRecord(BOAFileRecord bankRecord, String transactionType, boolean isBankPaymentApproved) {
        try (PaymentDTOEx paymentDTOEx = this.populatePaymentDTOExFromBankRecord(bankRecord, transactionType)) {
            paymentDTOEx.setIsBankPaymentApproved(isBankPaymentApproved);
            paymentDTOEx.setPaymentResult(new PaymentResultDAS().find(isBankPaymentApproved ? Constants.RESULT_OK : Constants.RESULT_ENTERED));

            return session.applyPayment(paymentDTOEx, null, bankRecord.getUserId());
        }catch (Exception exception){
            LOG.debug("Exception: "+exception);
            throw new SessionInternalError(exception);
        }
    }

    private  PaymentDTOEx populatePaymentDTOExFromBankRecord(BOAFileRecord bankRecord, String transactionType) {
        LOG.info("Creating new payment record for userId:" + bankRecord.getUserId() + " and transactionType:" + transactionType);

        PaymentDTOEx paymentDTOEx = new PaymentDTOEx();
        paymentDTOEx.setPaymentInstruments(Collections.singletonList(
                this.createBankWirePayment(bankRecord, new UserDAS().getUserCompanyId(bankRecord.getUserId()))));
        paymentDTOEx.setAmount(bankRecord.getAmount());
        paymentDTOEx.setPaymentMethod(new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_BANK_WIRE));
        paymentDTOEx.setCurrency(new CurrencyDTO(Integer.valueOf("1")));
        paymentDTOEx.setIsRefund(0);
        paymentDTOEx.setPaymentDate(TimezoneHelper.companyCurrentDateByUserId(bankRecord.getUserId()));
        paymentDTOEx.setUserId(bankRecord.getUserId());
        paymentDTOEx.setPaymentNotes(bankRecord.getRawData());

        return paymentDTOEx;
    }

    private static final List<Integer> ACH_TRANSACTION_CODES = Arrays.asList(165, 166, 257, 557);
    private static boolean isAchTransaction(Integer txType) {
        return ACH_TRANSACTION_CODES.contains(txType);
    }

    private static final List<Integer> WIRE_TRANSACTION_CODES = Arrays.asList(191, 195, 208);
    private static boolean isWireTransaction(Integer txType) {
        return WIRE_TRANSACTION_CODES.contains(txType);
    }

    private PaymentInformationDTO createBankWirePayment(BOAFileRecord bankRecord, Integer entityId){
        PaymentInformationDTO bankWire = new PaymentInformationDTO();
        bankWire.setPaymentMethod(new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_BANK_WIRE));
        bankWire.setPaymentMethodType(new PaymentMethodTypeDAS().getPaymentMethodTypeByTemplate("Bank Wire", entityId));
        List<MetaFieldValue> metaFields = new ArrayList<>(6);
        addMetaField(metaFields, BOA_BANK_TRANSACTION_ID, false, true, DataType.STRING, 1, bankRecord.getBankReferenceNo());
        addMetaField(metaFields,BOA_DEPOSIT_FILE_NAME, false, true, DataType.STRING, 2, bankRecord.getDepositFileName());
        addMetaField(metaFields, BOA_FUNDING_ACCOUNT_ID, false, true, DataType.STRING, 3, bankRecord.getFundingAccountId());
        addMetaField(metaFields, BOA_TRANSACTION_DATE, false, true, DataType.DATE, 4, bankRecord.getTransactionDate());
        addMetaField(metaFields, BOA_TRANSACTION_TIME, false, true, DataType.INTEGER, 5, bankRecord.getTransactionTime());
        addMetaField(metaFields, BOA_TRANSACTION_TYPE, false, true, DataType.INTEGER, 6, bankRecord.getTransactionType());
        bankWire.setMetaFields(metaFields);

        return bankWire;
    }

    private static void addMetaField(List<MetaFieldValue> metaFields,
                                     String fieldName, boolean disabled, boolean mandatory,
                                     DataType dataType, Integer displayOrder, Object value) {
        MetaField metaField = new MetaField();

        metaField.setName(fieldName);
        metaField.setDataType(dataType);
        metaField.setDisabled(disabled);
        metaField.setMandatory(mandatory);
        metaField.setDisplayOrder(displayOrder);
        metaField.setEntityType(EntityType.PAYMENT_METHOD_TEMPLATE);
        MetaFieldValue metaFieldValue = metaField.createValue();
        metaField = new MetaFieldDAS().save(metaField);

        metaFieldValue.setField(metaField);
        metaFieldValue.setValue(value);

        metaFieldValue = new MetaFieldValueDAS().save(metaFieldValue);

        metaFields.add(metaFieldValue);
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        jobParams = stepExecution.getJobParameters();
    }
}
