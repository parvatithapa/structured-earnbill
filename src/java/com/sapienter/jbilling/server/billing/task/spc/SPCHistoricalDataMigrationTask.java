package com.sapienter.jbilling.server.billing.task.spc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SessionFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.migration.InvoiceMigrationHelper;
import com.sapienter.jbilling.server.invoice.migration.OrderCreationHelper;
import com.sapienter.jbilling.server.invoice.migration.SPCInvoice;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * @author Harhsad Pathan
 * @since 08-03-2020 This schedule task is for generating historical invoice 
 */
public class SPCHistoricalDataMigrationTask extends AbstractCronTask  {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Integer 	UNPAID 					= 27;
    public 	static final Integer 	AUS_DOLLAR 				= 11;
    private static final boolean 	printOnConsole 			= true;

    private static final Integer 	THREE_MONTHS_BACK 		= -3;
    private static final Integer 	TWO_MONTHS_BACK 		= -2;
    private static final Integer 	ONE_MONTH_BACK 			= -1;
    private static final Integer 	CURRENT_MONTH 			= 0;
    private static final String 	C1_MID_MNTH 			= "C1=mid mth";
    private static final String 	C2_CALENDAR_MNTH 		= "C2=calendar mth" ;
    private static final String 	AUSBBS_CALENDAR_MNT 	= "AusBBS=calendar mth";
    private static final String 	PARALLEL_BILL_RUN_TWO 	= "Parallel Bill Run -2";
    private static final String 	PARALLEL_BILL_RUN_ONE 	= "Parallel Bill Run -1";
    private static final String 	PARALLEL_BILL_RUN   	= "Parallel Bill Run";
    private static final String 	CSV_EXTN 				= ".csv";
    private static final String 	INDEX_FILE_HEADER 		= SPCInvoice.getHeaders();
    private static final Integer    ITEM_ONE_TIME			= 6;
    public static  		 Integer       MONTH_OF_NEXT_RUN_DATE  = Integer.valueOf(0);
    public static  		 Integer       YEAR_OF_NEXT_RUN_DATE   = Integer.valueOf(0);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static Map<Integer, SPCInvoice> SPC_CUSTOMER_MAP_ONE = new ConcurrentHashMap<>();
    private static Map<Integer, SPCInvoice> SPC_CUSTOMER_MAP_TWO = new ConcurrentHashMap<>();
    private static Map<Integer, SPCInvoice> SPC_CUSTOMER_MAP_THREE = new ConcurrentHashMap<>();
    private static Map<Integer, SPCInvoice> FAILED_USERS 		 = new ConcurrentHashMap<>();
    private static Set<Integer>	successfulUserIds 				 = new HashSet<>();

    private static Integer 		ORDER_CHANGE_STATUS_APPLY_ID;
    private String OUT_PUT_FILE = "invoice-migration-failed-user";
    public  static String description ;
    private Set<Integer> 		USERS;
    private List<SPCInvoice> listOfFailedUser = new ArrayList<SPCInvoice>();

    private static final ParameterDescription LOCAL_PATH = new ParameterDescription("local path", true, ParameterDescription.Type.STR);
    private static final ParameterDescription INVOICE_METAFIELD = new ParameterDescription("invoice metafield", true, ParameterDescription.Type.STR);
    private static final ParameterDescription ADJUSTMENT_ITEM_ID = new ParameterDescription("adjustment item Id", true, ParameterDescription.Type.STR);
    private static final ParameterDescription TARGATE_TEST_ITEM_ID = new ParameterDescription("migration item Id", true, ParameterDescription.Type.STR);
    private static final ParameterDescription INPUT_FILE_NAME = new ParameterDescription("input file name with no extention", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PAYMENT_METHOD_ID = new ParameterDescription("payment method Id", true, ParameterDescription.Type.STR);

    private static final String XLSX_EXTN = ".xlsx";
    private String inputFileName = StringUtils.EMPTY;

    private static Date billingDate = null;
    private ExecutorService service;
    private static final int THREAD_POOL_SIZE = 5;
    private IWebServicesSessionBean spcTargetApi;
    private IMethodTransactionalWrapper actionTxWrapper = Context.getBean("methodTransactionalWrapper");

    public SPCHistoricalDataMigrationTask() {
        super.setUseTransaction(true);
        descriptions.add(LOCAL_PATH);
        descriptions.add(INVOICE_METAFIELD);
        descriptions.add(ADJUSTMENT_ITEM_ID);
        descriptions.add(TARGATE_TEST_ITEM_ID);
        descriptions.add(INPUT_FILE_NAME);
        descriptions.add(PAYMENT_METHOD_ID);
        service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        spcTargetApi = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        _init(context);
        IBillingProcessSessionBean billing = Context.getBean(Context.Name.BILLING_PROCESS_SESSION);
        inputFileName = parameters.get(LOCAL_PATH.getName()) + parameters.get(INPUT_FILE_NAME.getName()) + XLSX_EXTN;

        try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())) {

            List<Map<Integer, SPCInvoice>> spcInvoiceMaplist = InvoiceMigrationHelper.readXlsxSheet(inputFileName);
            Calendar nextRunDate = Calendar.getInstance();
            nextRunDate.setTime(billing.getConfigurationDto(70).getNextRunDate());

            MONTH_OF_NEXT_RUN_DATE = nextRunDate.get(Calendar.MONTH);
            YEAR_OF_NEXT_RUN_DATE = nextRunDate.get(Calendar.YEAR);

            description = spcTargetApi.getItem(Integer.parseInt(parameters.get(TARGATE_TEST_ITEM_ID.getName())), null, null).getDescription();

            SPC_CUSTOMER_MAP_ONE   = spcInvoiceMaplist.get(0);
            SPC_CUSTOMER_MAP_TWO   = spcInvoiceMaplist.get(1);
            SPC_CUSTOMER_MAP_THREE = spcInvoiceMaplist.get(2);

            long startTime = System.nanoTime();
            List<Integer> userList = new ArrayList<>();
            for (Integer id : SPC_CUSTOMER_MAP_ONE.keySet()) {
                 logger.debug("inserting user {}",id);
                userList.add(id);
            }
            logger.info("total users count: : {} ",userList.size());

            List<List<Integer>> partUserIds = InvoiceMigrationHelper.partition(userList, 10);

            for (List<Integer> users : partUserIds) {
                executeBatchExportInNewThread(users);
            }

            service.shutdown();
            while (!service.isTerminated()) {
            }
            long endTime = System.nanoTime();
            long totalTime = endTime - startTime;
            logger.info("Total time to execute : {}  nano seconds",totalTime);
            successfulUserIds.stream().forEach(userid -> {
            logger.info("User successful  : {} ",userid);
            });
            
            // Get New Pool Of Threads for verification.
            service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            for (List<Integer> users : partUserIds) {
                verifyUserInNewThread(users);   
            }
            service.shutdown();
            while (!service.isTerminated()) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                List<SPCInvoice> userList = new ArrayList<>();
                for (Integer key : FAILED_USERS.keySet()) {
                    userList.add(FAILED_USERS.get(key));
                }
                writeToCSV(userList, OUT_PUT_FILE);
            } catch (IOException e) {
                  logger.info("Execption while writing failed user file {}",e.getMessage());
            }
            if (!listOfFailedUser.isEmpty()) { 
                try {
                    logger.info("Collecting failed users");
                    writeToCSV(listOfFailedUser, "reconciliation-failed");
                    logger.info("finished Executing ");
                } catch (IOException e) {
                    logger.info("Execption while writing failed reconcilation file {}",e.getMessage());
                } 
            } else { 
                logger.info("######################## NO FAILED USERS in Collecting failed users ################"); 
            }
        }

    }

    public void createInvoicesOne(List<Integer> users) {

        int generated = 0;

        for (Integer userId : users) {
            if (generated % 100 == 0) {
                SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
                sf.getCurrentSession().clear();
            }
            generated++;

            SPCInvoice spcInvoice = SPC_CUSTOMER_MAP_ONE.get(userId);

            if (printOnConsole)
            	logger.debug(successfulUserIds.size()+1 + "," + spcInvoice.toString());
            try {
                PaymentWS paymentWS = null;
                spcInvoice.setBillingCycle(PARALLEL_BILL_RUN_ONE);

                BigDecimal openingBalance = spcInvoice.getOpeningBalance();

                //When opening balance is positive.
                if (null != openingBalance && openingBalance.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal balance = openingBalance;
                    createPreviousInvoice(spcInvoice,THREE_MONTHS_BACK,17, balance);
                }

                // create credit note when opening balance is less than Zero, here opening balance is used as -ve amount
                if (null != openingBalance && openingBalance.compareTo(BigDecimal.ZERO) < 0){
                    createPreviousAdjustmentOrderAndInvoice(spcInvoice,THREE_MONTHS_BACK,17,true);
                }

                // create credit note when adjustment is provided
                if (null != spcInvoice.getAdjustments() && spcInvoice.getAdjustments().compareTo(BigDecimal.ZERO) != 0) {
                    createPreviousAdjustmentOrderAndInvoice(spcInvoice,THREE_MONTHS_BACK,17,false);
                }

              //get payments
                paymentWS = getPaymentWS(spcInvoice, THREE_MONTHS_BACK, 17);
                if (null != paymentWS) {
                    Integer saveLegacyPayment = spcTargetApi.saveLegacyPayment(paymentWS);
                    logger.debug("userId : {} , saveLegacyPayment : {} ", spcInvoice.getUserId(), saveLegacyPayment);
                }
                // Create invoice when new charges is positive. 
                if (null != spcInvoice.getNewCharges() && spcInvoice.getNewCharges().compareTo(BigDecimal.ZERO) > 0) {
                    InvoiceWS createLegacyInvoiceOnly = createLegacyInvoiceOnly(spcInvoice, TWO_MONTHS_BACK, 17);
                    createLegacyInvoiceOnly.setTotal(openingBalance.compareTo(BigDecimal.ZERO) < 0 ? spcInvoice.getNewCharges() 
                            : openingBalance.add(spcInvoice.getNewCharges()));

                    // Set carried balance only if opening balance is positive
                    if (null != spcInvoice.getPayments() && null != openingBalance && spcInvoice.getPayments().compareTo(openingBalance) != 0) {
                        createLegacyInvoiceOnly.setCarriedBalance(openingBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : openingBalance);
                    }

                    Integer cycleOneInvoiceId = spcTargetApi.saveLegacyInvoice(createLegacyInvoiceOnly);
                    logger.debug("userId : {} , cycleOneInvoiceId : {} ", spcInvoice.getUserId(), cycleOneInvoiceId);

                    // Create invoice when new charges is negative
                } else if (null != spcInvoice.getNewCharges() && spcInvoice.getNewCharges().compareTo(BigDecimal.ZERO) < 0) {

                    boolean useOpeningBalance = openingBalance.compareTo(BigDecimal.ZERO) < 0 || 
                            spcInvoice.getNewCharges().compareTo(BigDecimal.ZERO) < 0 ? Boolean.FALSE: Boolean.TRUE;

                    createCreditNoteAtNegativeNewCharges(spcInvoice, TWO_MONTHS_BACK, 17, useOpeningBalance);
                }

                spcTargetApi.applyExistingCreditNotesToUnpaidInvoices(userId);

            } catch (Exception e) {
                logger.debug("Exception while processing user {}, with Exception {} " ,userId,e.getMessage());
                spcInvoice.setException(e.getMessage());
                FAILED_USERS.put(userId, spcInvoice);
            }
        }
    }

    public void createInvoicesTwo(List<Integer> users) {

        int generated = 0;
        InvoiceBL invoiceBl = new InvoiceBL();
        for (Integer userId : users) {

            if (generated % 100 == 0) {
                SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
                sf.getCurrentSession().clear();
            }
            generated++;
            SPCInvoice spcInvoice = SPC_CUSTOMER_MAP_TWO.get(userId);
            logger.debug("Thread : "+Thread.currentThread().toString());
            if (printOnConsole)
            	logger.debug(successfulUserIds.size()+1 + "," + spcInvoice.toString());
            try  {

                spcInvoice.setBillingCycle(PARALLEL_BILL_RUN_TWO);
                BigDecimal openingBalance = spcInvoice.getOpeningBalance();

                Calendar calendar = getCalendarInstance(TWO_MONTHS_BACK, 17);
                if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
                    calendar.set(Calendar.DATE, 1);
                }

                // This specifies that we dont have records for parallel run 2
                if (ArrayUtils.isEmpty(invoiceBl.getAllInvoices(userId,1))) {

                    BigDecimal openingBalance2 = spcInvoice.getOpeningBalance();

                    //When opening balance is positive.
                    if (null != openingBalance2 && openingBalance2.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal balance = openingBalance2;
                        createPreviousInvoice(spcInvoice,TWO_MONTHS_BACK,17, balance);
                    }

                    // create credit note when opening balance is less than Zero, here opening balance is used as -ve amount
                    if (null != openingBalance2 && openingBalance2.compareTo(BigDecimal.ZERO) < 0){
                        createPreviousAdjustmentOrderAndInvoice(spcInvoice,TWO_MONTHS_BACK,17,true);
                    }
                };


                if (null != spcInvoice.getAdjustments() && spcInvoice.getAdjustments().compareTo(BigDecimal.ZERO) != 0) {
                    createPreviousAdjustmentOrderAndInvoice(spcInvoice,ONE_MONTH_BACK,17,false);
                }

                if (null != spcInvoice.getNewCharges() && spcInvoice.getNewCharges().compareTo(BigDecimal.ZERO) > 0) {
                    InvoiceWS createLegacyInvoiceOnly = createLegacyInvoiceOnly(spcInvoice, ONE_MONTH_BACK, 17);
                    createLegacyInvoiceOnly.setCarriedBalance(openingBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : openingBalance);
                    createLegacyInvoiceOnly.setTotal(openingBalance.compareTo(BigDecimal.ZERO) < 0 ? spcInvoice.getNewCharges() 
                            : openingBalance.add(spcInvoice.getNewCharges()));
                    Integer cycleTwoLegacyInvoiceId = spcTargetApi.saveLegacyInvoice(createLegacyInvoiceOnly);
                    logger.debug("userId : {} , cycleTwoLegacyInvoiceId : {} ", spcInvoice.getUserId(), cycleTwoLegacyInvoiceId);

                    // Create invoice when new charges is negative
                } if (null != spcInvoice.getNewCharges() && spcInvoice.getNewCharges().compareTo(BigDecimal.ZERO) < 0) {
                    boolean useOpeningBalance = spcInvoice.getOpeningBalance().compareTo(BigDecimal.ZERO) < 0 || 
                            spcInvoice.getNewCharges().compareTo(BigDecimal.ZERO) < 0 ? Boolean.FALSE: Boolean.TRUE;
                    createCreditNoteAtNegativeNewCharges(spcInvoice, ONE_MONTH_BACK, 17, useOpeningBalance);
                }

                spcTargetApi.applyExistingCreditNotesToUnpaidInvoices(userId);

                if (null != spcInvoice.getPayments() && spcInvoice.getPayments().compareTo(BigDecimal.ZERO) != 0) {

                    PaymentWS paymentForInvoice = InvoiceMigrationHelper.getPaymentForInvoice(userId, spcInvoice, Integer.parseInt(parameters.get(PAYMENT_METHOD_ID.getName())));
                    paymentForInvoice.setPaymentDate(sdf.parse(sdf.format(calendar.getTime())));
                    Integer cycleTwoLegacyPayment = spcTargetApi.saveLegacyPayment(paymentForInvoice);
                    logger.debug("userId : {} , cycleTwoLegacyPayment : {} ", spcInvoice.getUserId(), cycleTwoLegacyPayment);
                }

            } catch (Exception e) {
                logger.debug("Exception while processing user {}, with Exception {} " ,userId,e.getMessage());
                spcInvoice.setException(e.getMessage());
                FAILED_USERS.put(userId, spcInvoice);
            }
        }
    }

    public void createInvoicesThree(List<Integer> users) {

        int generated = 0;
        InvoiceBL invoiceBl = new InvoiceBL();
        for (Integer userId : users) {

		if (generated % 100 == 0) {
			SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
			sf.getCurrentSession().clear();
		}
		generated++;

		SPCInvoice spcInvoice = SPC_CUSTOMER_MAP_THREE.get(userId);
		logger.debug("Thread : "+Thread.currentThread().toString());
		if (printOnConsole)
			logger.debug(successfulUserIds.size()+1 + "," + spcInvoice.toString());
		try  {

			spcInvoice.setBillingCycle(PARALLEL_BILL_RUN);

			Calendar calendar = getCalendarInstance(ONE_MONTH_BACK, 17);
			if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
				calendar.set(Calendar.DATE, 1);
			}

			// This specifies that we dont have records for parallel run 2
			if (ArrayUtils.isEmpty(invoiceBl.getAllInvoices(userId,1))) {

				BigDecimal openingBalance2 = spcInvoice.getOpeningBalance();

				//When opening balance is positive.
				if (null != openingBalance2 && openingBalance2.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal balance = openingBalance2;
					createPreviousInvoice(spcInvoice,ONE_MONTH_BACK,17, balance);
				}

				// create credit note when opening balance is less than Zero, here opening balance is used as -ve amount
				if (null != openingBalance2 && openingBalance2.compareTo(BigDecimal.ZERO) < 0){
					createPreviousAdjustmentOrderAndInvoice(spcInvoice,ONE_MONTH_BACK,17,true);
				}
			};

			if (null != spcInvoice.getAdjustments() && spcInvoice.getAdjustments().compareTo(BigDecimal.ZERO) != 0) {
				createPreviousAdjustmentOrderAndInvoice(spcInvoice,CURRENT_MONTH,17,false);
			}

			spcTargetApi.applyExistingCreditNotesToUnpaidInvoices(userId);

			if (null != spcInvoice.getPayments() && spcInvoice.getPayments().compareTo(BigDecimal.ZERO) != 0) {

				PaymentWS paymentForInvoice = InvoiceMigrationHelper.getPaymentForInvoice(userId, spcInvoice,Integer.parseInt(parameters.get(PAYMENT_METHOD_ID.getName())));
				paymentForInvoice.setPaymentDate(sdf.parse(sdf.format(calendar.getTime())));
				Integer cycleTwoLegacyPayment = spcTargetApi.saveLegacyPayment(paymentForInvoice);
				logger.debug("userId : {} , cycleTwoLegacyPayment : {} ", spcInvoice.getUserId(), cycleTwoLegacyPayment);
			}

		} catch (Exception e) {
			logger.debug("Exception while processing user {}, with Exception {} " ,userId,e.getMessage());
			spcInvoice.setException(e.getMessage());
			FAILED_USERS.put(userId, spcInvoice);
		}
        }
    }

    private void createPreviousAdjustmentOrderAndInvoice(SPCInvoice spcInvoice, Integer month, Integer date , boolean useOpeningBalance) throws ParseException {

        Calendar calendar  = Calendar.getInstance();
        calendar.set(Calendar.MONTH, MONTH_OF_NEXT_RUN_DATE);
        calendar.set(Calendar.YEAR, YEAR_OF_NEXT_RUN_DATE);
        calendar.set(Calendar.DAY_OF_MONTH, date);
        calendar.add(Calendar.MONTH, month);

        if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
            calendar.set(Calendar.DATE, 1);
        }
        try {
            ORDER_CHANGE_STATUS_APPLY_ID = OrderCreationHelper.getOrCreateOrderChangeStatusApply(spcTargetApi);
            Date activeSince = sdf.parse(sdf.format(calendar.getTime()));
            OrderWS orderWS = OrderCreationHelper.getOrderForAdjustments(spcInvoice,activeSince, Integer.parseInt(parameters.get(ADJUSTMENT_ITEM_ID.getName())),spcTargetApi,useOpeningBalance);
            OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(orderWS, ORDER_CHANGE_STATUS_APPLY_ID);
            for (OrderChangeWS change : changes)
            {
                change.setStartDate(orderWS.getActiveSince());
            }
            Integer adjustmentOrderId = spcTargetApi.createUpdateOrder(orderWS, changes);
            logger.debug("userId : {} , adjustmentOrderId : {} ", spcInvoice.getUserId(), adjustmentOrderId);

            Integer adjustmentInvoiceId = null;

            adjustmentInvoiceId = spcTargetApi.createInvoiceFromOrder(adjustmentOrderId, null);
            logger.debug("userId : {} , adjustmentInvoiceId : {} ", spcInvoice.getUserId(), adjustmentInvoiceId);

        } catch (Exception e) {
            logger.debug("Exception while creating invoice for order of user  {},  with Exception {} " ,spcInvoice.getUserId(),e.getMessage());
            spcInvoice.setException(e.getMessage());
            FAILED_USERS.put(spcInvoice.getUserId(), spcInvoice);
        }
    }

    private void createCreditNoteAtNegativeNewCharges(SPCInvoice spcInvoice, Integer month, Integer date, boolean useOpeningBalance) throws ParseException {

        Calendar calendar  = Calendar.getInstance();
        calendar.set(Calendar.MONTH, MONTH_OF_NEXT_RUN_DATE);
        calendar.set(Calendar.YEAR, YEAR_OF_NEXT_RUN_DATE);
        calendar.set(Calendar.DAY_OF_MONTH, date);
        calendar.add(Calendar.MONTH, month);

        OrderWS orderWS;
        try {
            if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
                calendar.set(Calendar.DATE, 1);
            }
            ORDER_CHANGE_STATUS_APPLY_ID = OrderCreationHelper.getOrCreateOrderChangeStatusApply(spcTargetApi);
            Date activeSince = sdf.parse(sdf.format(calendar.getTime()));
            orderWS = OrderCreationHelper.getOrderForNigativeNewCharges(spcInvoice,activeSince, Integer.parseInt(parameters.get(ADJUSTMENT_ITEM_ID.getName())),spcTargetApi,useOpeningBalance);
            OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(orderWS, ORDER_CHANGE_STATUS_APPLY_ID);
            for (OrderChangeWS change : changes)
            {
                change.setStartDate(orderWS.getActiveSince());
            }
            Integer adjustmentOrderId = spcTargetApi.createUpdateOrder(orderWS, changes);
            logger.debug("userId : {} , adjustmentOrderId : {} ", spcInvoice.getUserId(), adjustmentOrderId);

            Integer adjustmentInvoiceId = null;

            adjustmentInvoiceId = spcTargetApi.createInvoiceFromOrder(adjustmentOrderId, null);
            logger.debug("userId : {} , adjustmentInvoiceId : {} ", spcInvoice.getUserId(), adjustmentInvoiceId);

        } catch (Exception e) {
            logger.debug("Exception while creating invoice for order of user  {}  with Exception {} " ,spcInvoice.getUserId(),e.getMessage());
            spcInvoice.setException(e.getMessage());
            FAILED_USERS.put(spcInvoice.getUserId(), spcInvoice);
        }
    }

    public Integer createPreviousInvoice(SPCInvoice spcInvoice, Integer month, Integer date, BigDecimal balance)  {

        Calendar calendar = getCalendarInstance(month, date);
        InvoiceWS invoiceWS = new InvoiceWS();
        invoiceWS.setStatusId(UNPAID);
        invoiceWS.setUserId(spcInvoice.getUserId());
        invoiceWS.setCurrencyId(AUS_DOLLAR);
        invoiceWS.setBalance(balance);
        logger.debug("spcInvoice.getBillRunTiming() : {}", spcInvoice.getBillRunTiming());
        try {
            if (C1_MID_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
                calendar.set(Calendar.DATE, 17);
                invoiceWS.setCreateDatetime(sdf.parse(sdf.format(calendar.getTime())));
            } else if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
                calendar.set(Calendar.DATE, 1);
                invoiceWS.setCreateDatetime(sdf.parse(sdf.format(calendar.getTime())));
            }
            // ((previous balance - (payment)) + new charges + adjustment)
            invoiceWS.setTotal(spcInvoice.getOpeningBalance());
            invoiceWS.setDueDate(sdf.parse(sdf.format(calendar.getTime())));
            invoiceWS.setCarriedBalance("0");

            InvoiceLineDTO invoiceLineDTO = InvoiceMigrationHelper.buildInvoiceLine(spcInvoice.getUserId(),Integer.parseInt(parameters.get(TARGATE_TEST_ITEM_ID.getName())));
            invoiceLineDTO.setAmountAsDecimal(spcInvoice.getOpeningBalance());
            invoiceLineDTO.setPriceAsDecimal(spcInvoice.getOpeningBalance());
            invoiceLineDTO.setDescription(description);
            invoiceLineDTO.setTypeId(ITEM_ONE_TIME);

            invoiceWS.setInvoiceLines(new InvoiceLineDTO[]{invoiceLineDTO});
            MetaFieldValueWS metaField = new MetaFieldValueWS();
            metaField.setFieldName(parameters.get(INVOICE_METAFIELD.getName()));
            metaField.setValue(spcInvoice.getOpeningBalance());
            invoiceWS.setMetaFields(new MetaFieldValueWS[]{metaField});

            Integer saveLegacyInvoiceId = spcTargetApi.saveLegacyInvoice(invoiceWS);
            logger.debug("userId : {} , saveLegacyInvoiceId : {} ", spcInvoice.getUserId(), saveLegacyInvoiceId);
            return saveLegacyInvoiceId;
        } catch (Exception e) {
            logger.debug("Exception while creating invoice for order of user  {}  with Exception {} " ,spcInvoice.getUserId(),e.getMessage());
            spcInvoice.setException("createPreviousInvoice : "+e.getMessage());
            FAILED_USERS.put(spcInvoice.getUserId(), spcInvoice);
        }
        return null;

    }

    public PaymentWS getPaymentWS(SPCInvoice spcInvoice, Integer month, Integer date){

        Calendar calendar = getCalendarInstance(month, date);
        if (C1_MID_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
            calendar.set(Calendar.DATE, 17);
        } else if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
            calendar.set(Calendar.DATE, 1);
        }

        PaymentWS paymentWS = null;
        try {
            if ( spcInvoice.getPayments().compareTo(BigDecimal.ZERO) != 0 ) {
                paymentWS = InvoiceMigrationHelper.getPaymentForInvoice(spcInvoice.getUserId(), spcInvoice,Integer.parseInt(parameters.get(PAYMENT_METHOD_ID.getName())));
                paymentWS.setPaymentDate(DateUtils.addDays(sdf.parse(sdf.format(calendar.getTime())),1));
            }
        } catch (Exception e) {
            logger.debug("exeption while creating payment {}",e);
            spcInvoice.setException(e.getMessage());
            FAILED_USERS.put(spcInvoice.getUserId(), spcInvoice);
        }

        return paymentWS;
    }

    public InvoiceWS createLegacyInvoiceOnly(SPCInvoice spcInvoice, Integer month, Integer date) throws ParseException {

        Calendar calendar = getCalendarInstance(month, date);
        InvoiceWS invoiceWS = new InvoiceWS();
        invoiceWS.setStatusId(UNPAID);
        invoiceWS.setUserId(spcInvoice.getUserId());
        invoiceWS.setBalance(spcInvoice.getNewCharges());
        invoiceWS.setCurrencyId(AUS_DOLLAR);
        logger.debug("spcInvoice.getBillRunTiming() : {}", spcInvoice.getBillRunTiming());

        if (C1_MID_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming())) {
            calendar.set(Calendar.DATE, 17);
            invoiceWS.setCreateDatetime(sdf.parse(sdf.format(calendar.getTime())));
        } else if(C2_CALENDAR_MNTH.equalsIgnoreCase(spcInvoice.getBillRunTiming()) || AUSBBS_CALENDAR_MNT.equalsIgnoreCase(spcInvoice.getBillRunTiming()) ) {
            calendar.set(Calendar.DATE, 1);
            invoiceWS.setCreateDatetime(sdf.parse(sdf.format(calendar.getTime())));
        }

        // ((previous balance - (payment)) + new charges + adjustment)
        invoiceWS.setTotal(spcInvoice.getNewCharges());
        invoiceWS.setDueDate(sdf.parse(sdf.format(calendar.getTime())));
        invoiceWS.setCarriedBalance("0");

        InvoiceLineDTO invoiceLineDTO = InvoiceMigrationHelper.buildInvoiceLine(spcInvoice.getUserId(),Integer.parseInt(parameters.get(TARGATE_TEST_ITEM_ID.getName())));
        invoiceLineDTO.setAmountAsDecimal(spcInvoice.getNewCharges());
        invoiceLineDTO.setPriceAsDecimal(spcInvoice.getNewCharges());
        invoiceLineDTO.setDescription(description);
        invoiceLineDTO.setTypeId(ITEM_ONE_TIME);

        invoiceWS.setInvoiceLines(new InvoiceLineDTO[]{invoiceLineDTO});
        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.setFieldName(parameters.get(INVOICE_METAFIELD.getName()));
        metaField.setValue(spcInvoice.getNewCharges());
        invoiceWS.setMetaFields(new MetaFieldValueWS[]{metaField});

        return invoiceWS;
    }

    private Calendar getCalendarInstance(Integer month,Integer date) {
        Calendar calendar  = Calendar.getInstance();
        calendar.set(Calendar.YEAR, YEAR_OF_NEXT_RUN_DATE);
        calendar.set(Calendar.MONTH, MONTH_OF_NEXT_RUN_DATE);
        calendar.add(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, date);
        return calendar;
    }

    public String writeToCSV(List<SPCInvoice> message, String fileName) throws IOException {
        try(FileWriter writer = new FileWriter(new File(parameters.get(LOCAL_PATH.getName()) +fileName+CSV_EXTN));){
            writer.write(INDEX_FILE_HEADER);
            message.forEach(messages -> {
                try {
                    writer.write("\n");
                    writer.write(messages.toString());
                } catch (IOException e) {
                    throw new SessionInternalError("Exception in csv creation", e);
                }
            });
        }

        return fileName;
    }
    public  void verifyBalanceByUserApi(List<Integer> users) {

        for (Integer userId : users) {
            BigDecimal balance ;
            logger.debug("reconciling user  : {}",userId);
            SPCInvoice parallelRunMinusTwo = SPC_CUSTOMER_MAP_ONE.get(userId);
            SPCInvoice parallelRunMinusOne = SPC_CUSTOMER_MAP_TWO.get(userId);
            SPCInvoice parallelRun         = SPC_CUSTOMER_MAP_THREE.get(userId);

			BigDecimal checkBalance = parallelRunMinusOne.getCheckBalance();
			if (BigDecimal.ZERO.compareTo(checkBalance) == 0) {
				checkBalance = parallelRunMinusTwo.getCheckBalance()
				        .add(parallelRunMinusOne.getPayments())
				        .add(parallelRunMinusOne.getAdjustments())
				        .add(parallelRunMinusOne.getNewCharges());
			}

			BigDecimal parallelRunPayment = parallelRun.getPayments();
			BigDecimal parallelRunAdjustment = parallelRun.getAdjustments();
			BigDecimal parallelBalance = checkBalance.add(parallelRunPayment.add(parallelRunAdjustment));

			balance = spcTargetApi.getUserWS(userId).getOwingBalanceAsDecimal();

			BigDecimal balanceFromSheet = parallelBalance.setScale(2, BigDecimal.ROUND_HALF_UP);

            if (balance.compareTo(balanceFromSheet) == 0) {
            	logger.debug("user reconciled : {}",true);
            }
            else {
                logger.debug("Issues with user : {}", parallelRunMinusOne.getUserId());
                parallelRunMinusOne.setException("final balance from jbilling: " + balance.stripTrailingZeros().toPlainString() + "  balance from the sheet : " + parallelRunMinusOne.getCheckBalance().toPlainString());
                listOfFailedUser.add(parallelRunMinusOne);
           }
        }
    }
    private void executeBatchExportInNewThread(final List<Integer> userBatche) {
        service.execute(() -> actionTxWrapper.execute(() -> {
            try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())){
                createInvoicesOne(userBatche);
                createInvoicesTwo(userBatche);
                createInvoicesThree(userBatche);
            }
            catch(Exception ex) {
                logger.error("Record got Skipped: ", ex);
            }
        }));
    }
    private void verifyUserInNewThread(final List<Integer> userBatche) {
        service.execute(() -> actionTxWrapper.execute(() -> {
            try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())){
                verifyBalanceByUserApi(userBatche);
            }
            catch(Exception ex) {
                logger.error("Record got Skipped: ", ex);
            }
        }));
    }
}
