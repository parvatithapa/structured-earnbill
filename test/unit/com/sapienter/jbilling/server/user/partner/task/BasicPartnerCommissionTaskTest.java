package com.sapienter.jbilling.server.user.partner.task;

import static org.easymock.EasyMock.*;

import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDAS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.DateMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.partner.CommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.user.partner.db.*;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import junit.framework.TestCase;
import org.easymock.IAnswer;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class BasicPartnerCommissionTaskTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(BasicPartnerCommissionTaskTest.class);
    private static String CUST_ACC_NR = "123";
    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test usage fee as defined by levels on partner
     * @throws Exception
     */
    @Test
    public void testUsageFee() throws Exception {
        BasicPartnerCommissionTask task = new BasicPartnerCommissionTask();
        CompanyDTO  company = new CompanyDTO(1);
        Date nextRunDate = parseDate("2016-02-01");
        PartnerDTO partnerDTO = new PartnerDTO(10);
        partnerDTO.setBaseUser(new UserDTO());
        partnerDTO.setType(PartnerType.STANDARD);
        partnerDTO.setCommissionType(PartnerCommissionType.CONSUMPTION);
        partnerDTO.setCommissionValues(Arrays.asList(
                commissionValue(0, new BigDecimal(2)),
                commissionValue(30, new BigDecimal(1)),
                commissionValue(60, new BigDecimal(0))
        ));

        CustomerDTO customer = new CustomerDTO(1);
        MetaField mf = new MetaField();
        mf.setName(FileConstants.UTILITY_CUST_ACCT_NR);
        StringMetaFieldValue mfValue = new StringMetaFieldValue(mf);
        mfValue.setValue(CUST_ACC_NR);
        CustomerAccountInfoTypeMetaField aitMap = new CustomerAccountInfoTypeMetaField();
        aitMap.setMetaFieldValue(mfValue);
        customer.getCustomerAccountInfoTypeMetaFields().add(aitMap);

        UserDTO user = new UserDTO(2);
        user.setCustomer(customer);
        ItemDTO electricity = new ItemDTO(1);
        mf = new MetaField();
        mf.setName(FileConstants.COMMODITY);
        mfValue = new StringMetaFieldValue(mf);
        mfValue.setValue("E");
        electricity.getMetaFields().add(mfValue);

        InvoiceDTO invoice1 = buildInvoice(1, user, electricity, "2015-11-20");
        InvoiceDTO invoice2 = buildInvoice(2, user, electricity, "2015-12-20");
        InvoiceDTO invoice3 = buildInvoice(3, user, electricity, "2016-01-20");

        CompanyDAS companyDAS = createNiceMock(CompanyDAS.class);
        task.setCompanyDAS(companyDAS);
        expect(companyDAS.find(1)).andStubReturn(company);

        CommissionProcessConfigurationDAS configurationDAS = createNiceMock(CommissionProcessConfigurationDAS.class);
        task.setConfigurationDAS(configurationDAS);
        expect(configurationDAS.findByEntity(anyObject())).andStubReturn(
                new CommissionProcessConfigurationDTO.Builder()
                        .id(1)
                        .company(company)
                        .nextRunDate(nextRunDate)
                        .periodUnit(new PeriodUnitDTO(Constants.PERIOD_UNIT_MONTH))
                        .periodValue(1)
                        .build());


        PartnerDAS partnerDAS = createNiceMock(PartnerDAS.class);
        task.setPartnerDAS(partnerDAS);
        expect(partnerDAS.findPartnersByCompany(anyObject())).andStubReturn(
                print("partnerDAS.findPartnersByCompany - {}", Arrays.asList(partnerDTO))
        );
        expect(partnerDAS.find(anyObject())).andStubReturn(partnerDTO);

        CustomerEnrollmentDAS customerEnrollmentDAS = createNiceMock(CustomerEnrollmentDAS.class);
        task.setCustomerEnrollmentDAS(customerEnrollmentDAS);
        expect(customerEnrollmentDAS.findCustomerEnrollmentDate(company.getId(), CUST_ACC_NR)).andStubReturn(
                parseDate("2015-11-10")
        );

        //we will return 2 invoices for the customer
        InvoiceDAS invoiceDAS = createNiceMock(InvoiceDAS.class);
        task.setInvoiceDAS(invoiceDAS);
        expect(invoiceDAS.findForPartnerCommissions(eq(partnerDTO.getId()), anyObject(Date.class))).andStubReturn(
                Arrays.asList(invoice1.getId(), invoice2.getId(), invoice3.getId())
        );
        expect(invoiceDAS.find(anyObject())).andStubAnswer(new IAnswer<InvoiceDTO>() {
            @Override
            public InvoiceDTO answer() throws Throwable {
                Integer id = (Integer)getCurrentArguments()[0];
                if(id.equals(invoice1.getId())) return invoice1;
                if(id.equals(invoice2.getId())) return invoice2;
                if(id.equals(invoice3.getId())) return invoice3;
                fail("Unknown invoice id " + id);
                return null;
            }
        });

        //store all saved lines so we can return it later.
        List<PartnerCommissionLineDTO> partnerCommissionLineDTOs = new ArrayList<>();

        //for the first invoice we will return an empty list - no commission received.
        //on the second call we will return a value to indicate the first one was safe
        PartnerCommissionDAS partnerCommissionDAS = createMock(PartnerCommissionDAS.class);
        task.setPartnerCommissionDAS(partnerCommissionDAS);
//        expect(partnerCommissionDAS.findCustomerCommission(anyObject()))
//                .andReturn(Arrays.asList());
        expect(partnerCommissionDAS.save(anyObject(InvoiceCommissionDTO.class))).andStubAnswer(new IAnswer<PartnerCommissionLineDTO>() {
            @Override
            public PartnerCommissionLineDTO answer() throws Throwable {
                InvoiceCommissionDTO commission = (InvoiceCommissionDTO) getCurrentArguments()[0];
                assertNotNull(commission.getCommissionProcessRun());
                assertEquals(partnerDTO.getId(), commission.getPartner().getId());
                if (commission.getInvoice().getId() == 1) { //invoice1
                    //2 units * 2 per unit
                    assertEquals(new BigDecimal(4), commission.getStandardAmount());
                } else if (commission.getInvoice().getId() == 2) { //invoice2
                    //2 units * 1 per unit
                    assertEquals(new BigDecimal(2), commission.getStandardAmount());
                } else if (commission.getInvoice().getId() == 3) { //invoice3
                    //2 units * 0 per unit
                    assertEquals(new BigDecimal(0), commission.getStandardAmount());
                } else {
                    fail("Unknown invoice id " + commission.getInvoice());
                }
                partnerCommissionLineDTOs.add(commission);
                return commission;
            }
        });
        partnerCommissionDAS.reattach(anyObject());
        expectLastCall().anyTimes();
//        expect(partnerCommissionDAS.findCustomerCommission(user)).andReturn(Arrays.asList(new CustomerCommissionDTO()));

        PaymentCommissionDAS paymentCommissionDAS = createNiceMock(PaymentCommissionDAS.class);
        task.setPaymentCommissionDAS(paymentCommissionDAS);

        PartnerReferralCommissionDAS partnerReferralCommissionDAS = createNiceMock(PartnerReferralCommissionDAS.class);
        task.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        expect(partnerReferralCommissionDAS.findAllForCompany(anyObject())).andStubReturn(new ArrayList<>(0));
        expect(partnerCommissionDAS.findByPartnerAndProcessRun(anyObject(), anyObject())).andStubReturn(partnerCommissionLineDTOs);

        CommissionDAS commissionDAS = createMock(CommissionDAS.class);
        task.setCommissionDAS(commissionDAS);
        expect(commissionDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionDTO>() {
            @Override
            public CommissionDTO answer() throws Throwable {
                CommissionDTO commission = (CommissionDTO) getCurrentArguments()[0];
                assert commission.getAmount().equals(new BigDecimal(6));
                assertEquals(CommissionType.DEFAULT_STANDARD_COMMISSION, commission.getType());
                return commission;
            }
        });

        CommissionProcessRunDAS commissionProcessRunDAS = createNiceMock(CommissionProcessRunDAS.class);
        task.setProcessRunDAS(commissionProcessRunDAS);
        expect(commissionProcessRunDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionProcessRunDTO>() {
            @Override
            public CommissionProcessRunDTO answer() throws Throwable {
                return (CommissionProcessRunDTO) getCurrentArguments()[0];
            }
        });

        EventLogger eventLogger = createNiceMock(EventLogger.class);

        PartnerBL partnerBL = new PartnerBL(false);
        partnerBL.setPartnerDAS(partnerDAS);
        partnerBL.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        partnerBL.setPartnerCommissionDAS(partnerCommissionDAS);
        partnerBL.setCommissionDAS(commissionDAS);
        partnerBL.setELogger(eventLogger);
        task.setPartnerBL(partnerBL);

        PlatformTransactionManager txMgr = createNiceMock(PlatformTransactionManager.class);
        task.setTransactionManager(txMgr);

        Object[] mocks = new Object[] {commissionDAS, partnerCommissionDAS, partnerReferralCommissionDAS, paymentCommissionDAS,
                invoiceDAS, customerEnrollmentDAS, configurationDAS, partnerDAS, companyDAS, commissionProcessRunDAS, eventLogger, txMgr};
    }

    /**
     * The partner has the same commission as the test above, but the there are user specific commition values which
     * should override these
     * @throws Exception
     */
    @Test
    public void testCustomerSpecificUsageFee() throws Exception {
        BasicPartnerCommissionTask task = new BasicPartnerCommissionTask();
        CompanyDTO  company = new CompanyDTO(1);
        Date nextRunDate = parseDate("2016-02-01");
        PartnerDTO partnerDTO = new PartnerDTO(10);
        partnerDTO.setBaseUser(new UserDTO());
        partnerDTO.setType(PartnerType.STANDARD);
        partnerDTO.setCommissionType(PartnerCommissionType.CONSUMPTION);
        partnerDTO.setCommissionValues(Arrays.asList(
                commissionValue(0, new BigDecimal(2)),
                commissionValue(30, new BigDecimal(1)),
                commissionValue(60, new BigDecimal(0))
        ));

        CustomerDTO customer = new CustomerDTO(1);
        MetaField mf = new MetaField();
        mf.setName(FileConstants.UTILITY_CUST_ACCT_NR);
        StringMetaFieldValue mfValue = new StringMetaFieldValue(mf);
        mfValue.setValue(CUST_ACC_NR);
        CustomerAccountInfoTypeMetaField aitMap = new CustomerAccountInfoTypeMetaField();
        aitMap.setMetaFieldValue(mfValue);
        customer.getCustomerAccountInfoTypeMetaFields().add(aitMap);

        UserDTO user = new UserDTO(2);
        user.setCustomer(customer);
        customer.setBaseUser(user);
        //create customer specific commission for user
        CustomerCommissionDefinitionDTO commDef = new CustomerCommissionDefinitionDTO(
                new CustomerCommissionDefinitionPK(partnerDTO, customer.getBaseUser()), new BigDecimal("5")
        );
        customer.getBaseUser().getCommissionDefinitions().add(commDef);

        ItemDTO electricity = new ItemDTO(1);
        mf = new MetaField();
        mf.setName(FileConstants.COMMODITY);
        mfValue = new StringMetaFieldValue(mf);
        mfValue.setValue("E");
        electricity.getMetaFields().add(mfValue);

        InvoiceDTO invoice1 = buildInvoice(1, user, electricity, "2015-11-20");
        InvoiceDTO invoice2 = buildInvoice(2, user, electricity, "2015-12-20");
        InvoiceDTO invoice3 = buildInvoice(3, user, electricity, "2016-01-20");

        CompanyDAS companyDAS = createNiceMock(CompanyDAS.class);
        task.setCompanyDAS(companyDAS);
        expect(companyDAS.find(1)).andStubReturn(company);

        CommissionProcessConfigurationDAS configurationDAS = createNiceMock(CommissionProcessConfigurationDAS.class);
        task.setConfigurationDAS(configurationDAS);
        expect(configurationDAS.findByEntity(anyObject())).andStubReturn(
                new CommissionProcessConfigurationDTO.Builder()
                        .id(1)
                        .company(company)
                        .nextRunDate(nextRunDate)
                        .periodUnit(new PeriodUnitDTO(Constants.PERIOD_UNIT_MONTH))
                        .periodValue(1)
                        .build());


        PartnerDAS partnerDAS = createNiceMock(PartnerDAS.class);
        task.setPartnerDAS(partnerDAS);
        expect(partnerDAS.findPartnersByCompany(anyObject())).andStubReturn(
                print("partnerDAS.findPartnersByCompany - {}", Arrays.asList(partnerDTO))
        );
        expect(partnerDAS.find(anyObject())).andStubReturn(partnerDTO);

        CustomerEnrollmentDAS customerEnrollmentDAS = createNiceMock(CustomerEnrollmentDAS.class);
        task.setCustomerEnrollmentDAS(customerEnrollmentDAS);
        expect(customerEnrollmentDAS.findCustomerEnrollmentDate(company.getId(), CUST_ACC_NR)).andStubReturn(
                parseDate("2015-11-10")
        );

        //we will return 2 invoices for the customer
        InvoiceDAS invoiceDAS = createNiceMock(InvoiceDAS.class);
        task.setInvoiceDAS(invoiceDAS);
        expect(invoiceDAS.findForPartnerCommissions(eq(partnerDTO.getId()), anyObject(Date.class))).andStubReturn(
                Arrays.asList(invoice1.getId(), invoice2.getId(), invoice3.getId())
        );
        expect(invoiceDAS.find(anyObject())).andStubAnswer(new IAnswer<InvoiceDTO>() {
            @Override
            public InvoiceDTO answer() throws Throwable {
                Integer id = (Integer)getCurrentArguments()[0];
                if(id.equals(invoice1.getId())) return invoice1;
                if(id.equals(invoice2.getId())) return invoice2;
                if(id.equals(invoice3.getId())) return invoice3;
                fail("Unknown invoice id " + id);
                return null;
            }
        });

        //store all saved lines so we can return it later.
        List<PartnerCommissionLineDTO> partnerCommissionLineDTOs = new ArrayList<>();

        //for the first invoice we will return an empty list - no commission received.
        //on the second call we will return a value to indicate the first one was safe
        PartnerCommissionDAS partnerCommissionDAS = createMock(PartnerCommissionDAS.class);
        task.setPartnerCommissionDAS(partnerCommissionDAS);
//        expect(partnerCommissionDAS.findCustomerCommission(anyObject()))
//                .andReturn(Arrays.asList());
        expect(partnerCommissionDAS.save(anyObject(InvoiceCommissionDTO.class))).andStubAnswer(new IAnswer<PartnerCommissionLineDTO>() {
            @Override
            public PartnerCommissionLineDTO answer() throws Throwable {
                InvoiceCommissionDTO commission = (InvoiceCommissionDTO) getCurrentArguments()[0];
                assertNotNull(commission.getCommissionProcessRun());
                assertEquals(partnerDTO.getId(), commission.getPartner().getId());
                if (commission.getInvoice().getId() == 1) { //invoice1
                    //2 units * 5 per unit
                    assertEquals(new BigDecimal(10), commission.getStandardAmount());
                } else if (commission.getInvoice().getId() == 2) { //invoice2
                    //2 units * 5 per unit
                    assertEquals(new BigDecimal(10), commission.getStandardAmount());
                } else if (commission.getInvoice().getId() == 3) { //invoice3
                    //2 units * 5 per unit
                    assertEquals(new BigDecimal(10), commission.getStandardAmount());
                } else {
                    fail("Unknown invoice id " + commission.getInvoice());
                }
                partnerCommissionLineDTOs.add(commission);
                return commission;
            }
        });
        partnerCommissionDAS.reattach(anyObject());
        expectLastCall().anyTimes();
//        expect(partnerCommissionDAS.findCustomerCommission(user)).andReturn(Arrays.asList(new CustomerCommissionDTO()));

        PaymentCommissionDAS paymentCommissionDAS = createNiceMock(PaymentCommissionDAS.class);
        task.setPaymentCommissionDAS(paymentCommissionDAS);

        PartnerReferralCommissionDAS partnerReferralCommissionDAS = createNiceMock(PartnerReferralCommissionDAS.class);
        task.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        expect(partnerReferralCommissionDAS.findAllForCompany(anyObject())).andStubReturn(new ArrayList<>(0));
        expect(partnerCommissionDAS.findByPartnerAndProcessRun(anyObject(), anyObject())).andStubReturn(partnerCommissionLineDTOs);

        CommissionDAS commissionDAS = createMock(CommissionDAS.class);
        task.setCommissionDAS(commissionDAS);
        expect(commissionDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionDTO>() {
            @Override
            public CommissionDTO answer() throws Throwable {
                CommissionDTO commission = (CommissionDTO) getCurrentArguments()[0];
                assert commission.getAmount().equals(new BigDecimal(30));
                assertEquals(CommissionType.DEFAULT_STANDARD_COMMISSION, commission.getType());
                return commission;
            }
        });

        CommissionProcessRunDAS commissionProcessRunDAS = createNiceMock(CommissionProcessRunDAS.class);
        task.setProcessRunDAS(commissionProcessRunDAS);
        expect(commissionProcessRunDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionProcessRunDTO>() {
            @Override
            public CommissionProcessRunDTO answer() throws Throwable {
                return (CommissionProcessRunDTO) getCurrentArguments()[0];
            }
        });

        EventLogger eventLogger = createNiceMock(EventLogger.class);

        PartnerBL partnerBL = new PartnerBL(false);
        partnerBL.setPartnerDAS(partnerDAS);
        partnerBL.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        partnerBL.setPartnerCommissionDAS(partnerCommissionDAS);
        partnerBL.setCommissionDAS(commissionDAS);
        partnerBL.setELogger(eventLogger);
        task.setPartnerBL(partnerBL);

        PlatformTransactionManager txMgr = createNiceMock(PlatformTransactionManager.class);
        task.setTransactionManager(txMgr);

        Object[] mocks = new Object[] {commissionDAS, partnerCommissionDAS, partnerReferralCommissionDAS, paymentCommissionDAS,
                invoiceDAS, customerEnrollmentDAS, configurationDAS, partnerDAS, companyDAS, commissionProcessRunDAS, eventLogger, txMgr};
    }

    private InvoiceDTO buildInvoice(int id, UserDTO user, ItemDTO electricity, String date) {
        InvoiceDTO invoice1 = new InvoiceDTO();
        invoice1.setId(id);
        invoice1.setCreateDatetime(parseDate(date));
        invoice1.setBaseUser(user);
        InvoiceLineDTO line = new InvoiceLineDTO();
        line.setItem(electricity);
        line.setQuantity(new BigDecimal(2));
        line.setAmount(new BigDecimal(100));
        line.setPrice(new BigDecimal(50));
        invoice1.getInvoiceLines().add(line);
        return invoice1;
    }

    @Test
    public void testConsumerFee() throws Exception {
        BasicPartnerCommissionTask task = new BasicPartnerCommissionTask();
        CompanyDTO  company = new CompanyDTO(1);
        Date nextRunDate = parseDate("2016-02-01");
        PartnerDTO partnerDTO = new PartnerDTO(10);
        partnerDTO.setBaseUser(new UserDTO());
        partnerDTO.setCommissionType(PartnerCommissionType.CUSTOMER);
        partnerDTO.setCommissionValues(Arrays.asList(
               commissionValue(0, BigDecimal.TEN)
        ));

        CustomerDTO customer = new CustomerDTO(1);
        MetaField mf = new MetaField();
        mf.setName(FileConstants.UTILITY_CUST_ACCT_NR);
        StringMetaFieldValue mfValue = new StringMetaFieldValue(mf);
        mfValue.setValue(CUST_ACC_NR);
        CustomerAccountInfoTypeMetaField aitMap = new CustomerAccountInfoTypeMetaField();
        aitMap.setMetaFieldValue(mfValue);
        customer.getCustomerAccountInfoTypeMetaFields().add(aitMap);

        UserDTO user = new UserDTO(2);
        user.setCustomer(customer);
        ItemDTO electricity = new ItemDTO(1);

        InvoiceDTO invoice1 = new InvoiceDTO();
        invoice1.setId(1);
        invoice1.setCreateDatetime(parseDate("2016-01-20"));
        invoice1.setBaseUser(user);
        InvoiceDTO invoice2 = new InvoiceDTO();
        invoice2.setId(2);
        invoice2.setCreateDatetime(parseDate("2016-01-20"));
        invoice2.setBaseUser(user);

        CompanyDAS companyDAS = createNiceMock(CompanyDAS.class);
        task.setCompanyDAS(companyDAS);
        expect(companyDAS.find(1)).andStubReturn(company);

        CommissionProcessConfigurationDAS configurationDAS = createNiceMock(CommissionProcessConfigurationDAS.class);
        task.setConfigurationDAS(configurationDAS);
        expect(configurationDAS.findByEntity(anyObject())).andStubReturn(
                new CommissionProcessConfigurationDTO.Builder()
                        .id(1)
                        .company(company)
                        .nextRunDate(nextRunDate)
                        .periodUnit(new PeriodUnitDTO(Constants.PERIOD_UNIT_MONTH))
                        .periodValue(1)
                        .build());


        PartnerDAS partnerDAS = createNiceMock(PartnerDAS.class);
        task.setPartnerDAS(partnerDAS);
        expect(partnerDAS.find(anyObject())).andStubReturn(partnerDTO);
        expect(partnerDAS.findPartnersByCompany(anyObject())).andStubReturn(
                print("partnerDAS.findPartnersByCompany - {}", Arrays.asList(partnerDTO))
        );

        CustomerEnrollmentDAS customerEnrollmentDAS = createNiceMock(CustomerEnrollmentDAS.class);
        task.setCustomerEnrollmentDAS(customerEnrollmentDAS);
        expect(customerEnrollmentDAS.findCustomerEnrollmentDate(company.getId(), CUST_ACC_NR)).andStubReturn(
           parseDate("2016-01-15")
        );

        //we will return 2 invoices for the customer
        InvoiceDAS invoiceDAS = createNiceMock(InvoiceDAS.class);
        task.setInvoiceDAS(invoiceDAS);
        expect(invoiceDAS.findForPartnerCommissions(eq(partnerDTO.getId()), anyObject(Date.class))).andStubReturn(
                Arrays.asList(invoice1.getId(), invoice2.getId())
        );
        expect(invoiceDAS.find(anyObject())).andStubAnswer(new IAnswer<InvoiceDTO>() {
            @Override
            public InvoiceDTO answer() throws Throwable {
                Integer id = (Integer)getCurrentArguments()[0];
                if(id.equals(invoice1.getId())) return invoice1;
                if(id.equals(invoice2.getId())) return invoice2;
                fail("Unknown invoice id " + id);
                return null;
            }
        });

        //store all saved lines so we can return it later.
        List < PartnerCommissionLineDTO > partnerCommissionLineDTOs = new ArrayList<>();

        //for the first invoice we will return an empty list - no commission received.
        //on the second call we will return a value to indicate the first one was safe
        PartnerCommissionDAS partnerCommissionDAS = createMock(PartnerCommissionDAS.class);
        task.setPartnerCommissionDAS(partnerCommissionDAS);
        expect(partnerCommissionDAS.findCustomerCommission(anyObject(), anyObject()))
                .andReturn(Arrays.asList());
        expect(partnerCommissionDAS.save(anyObject(CustomerCommissionDTO.class))).andStubAnswer(new IAnswer<PartnerCommissionLineDTO>() {
            @Override
            public PartnerCommissionLineDTO answer() throws Throwable {
                CustomerCommissionDTO commission = (CustomerCommissionDTO) getCurrentArguments()[0];
                assert commission.getAmount().equals(BigDecimal.TEN);
                assert commission.getUser().getId() == user.getId();
                assert commission.getPartner().getId() == partnerDTO.getId();
                assert commission.getCommissionProcessRun() != null;
                partnerCommissionLineDTOs.add(commission);
                return commission;
            }
        });
        partnerCommissionDAS.reattach(anyObject());
        expectLastCall().anyTimes();
//        expect(partnerCommissionDAS.findCustomerCommission(user)).andReturn(Arrays.asList(new CustomerCommissionDTO()));

        PaymentCommissionDAS paymentCommissionDAS = createNiceMock(PaymentCommissionDAS.class);
        task.setPaymentCommissionDAS(paymentCommissionDAS);

        PartnerReferralCommissionDAS partnerReferralCommissionDAS = createNiceMock(PartnerReferralCommissionDAS.class);
        task.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        expect(partnerReferralCommissionDAS.findAllForCompany(anyObject())).andStubReturn(new ArrayList<>(0));
        expect(partnerCommissionDAS.findByPartnerAndProcessRun(anyObject(), anyObject())).andStubReturn(partnerCommissionLineDTOs);

        CommissionDAS commissionDAS = createMock(CommissionDAS.class);
        task.setCommissionDAS(commissionDAS);
        expect(commissionDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionDTO>() {
            @Override
            public CommissionDTO answer() throws Throwable {
                CommissionDTO commission = (CommissionDTO) getCurrentArguments()[0];
                assert commission.getAmount().equals(BigDecimal.TEN);
                assertEquals(CommissionType.CUSTOMER_COMMISSION, commission.getType());
                return commission;
            }
        });

        CommissionProcessRunDAS commissionProcessRunDAS = createNiceMock(CommissionProcessRunDAS.class);
        task.setProcessRunDAS(commissionProcessRunDAS);
        expect(commissionProcessRunDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionProcessRunDTO>() {
            @Override
            public CommissionProcessRunDTO answer() throws Throwable {
                return (CommissionProcessRunDTO)getCurrentArguments()[0];
            }
        });

        EventLogger eventLogger = createNiceMock(EventLogger.class);

        PartnerBL partnerBL = new PartnerBL(false);
        partnerBL.setPartnerDAS(partnerDAS);
        partnerBL.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        partnerBL.setPartnerCommissionDAS(partnerCommissionDAS);
        partnerBL.setCommissionDAS(commissionDAS);
        partnerBL.setELogger(eventLogger);
        task.setPartnerBL(partnerBL);

        PlatformTransactionManager txMgr = createNiceMock(PlatformTransactionManager.class);
        task.setTransactionManager(txMgr);

        Object[] mocks = new Object[] {commissionDAS, partnerCommissionDAS, partnerReferralCommissionDAS, paymentCommissionDAS,
                invoiceDAS, customerEnrollmentDAS, configurationDAS, partnerDAS, companyDAS, commissionProcessRunDAS, eventLogger, txMgr};

        replay(mocks);
        task.calculateCommissions(1);
        verify(mocks);
    }

    /**
     * There are commissions defined for the partner, but there are user specific commissions defined which
     * should override the partner ones.
     * @throws Exception
     */
    @Test
    public void testCustomerSpecificCommission() throws Exception {
        BasicPartnerCommissionTask task = new BasicPartnerCommissionTask();
        CompanyDTO  company = new CompanyDTO(1);
        Date nextRunDate = parseDate("2016-02-01");
        PartnerDTO partnerDTO = new PartnerDTO(10);
        partnerDTO.setBaseUser(new UserDTO());
        partnerDTO.setCommissionType(PartnerCommissionType.CUSTOMER);
        partnerDTO.setCommissionValues(Arrays.asList(
                commissionValue(0, BigDecimal.TEN)
        ));

        CustomerDTO customer = new CustomerDTO(1);
        MetaField mf = new MetaField();
        mf.setName(FileConstants.UTILITY_CUST_ACCT_NR);
        StringMetaFieldValue mfValue = new StringMetaFieldValue(mf);
        mfValue.setValue(CUST_ACC_NR);
        CustomerAccountInfoTypeMetaField aitMap = new CustomerAccountInfoTypeMetaField();
        aitMap.setMetaFieldValue(mfValue);
        customer.getCustomerAccountInfoTypeMetaFields().add(aitMap);

        UserDTO user = new UserDTO(2);
        user.setCustomer(customer);
        customer.setBaseUser(user);

        //create customer specific commission for user
        CustomerCommissionDefinitionDTO commDef = new CustomerCommissionDefinitionDTO(
                new CustomerCommissionDefinitionPK(partnerDTO, customer.getBaseUser()), new BigDecimal("5")
        );
        customer.getBaseUser().getCommissionDefinitions().add(commDef);

        ItemDTO electricity = new ItemDTO(1);

        InvoiceDTO invoice1 = new InvoiceDTO();
        invoice1.setId(1);
        invoice1.setCreateDatetime(parseDate("2016-01-20"));
        invoice1.setBaseUser(user);
        InvoiceDTO invoice2 = new InvoiceDTO();
        invoice2.setId(2);
        invoice2.setCreateDatetime(parseDate("2016-01-20"));
        invoice2.setBaseUser(user);

        CompanyDAS companyDAS = createNiceMock(CompanyDAS.class);
        task.setCompanyDAS(companyDAS);
        expect(companyDAS.find(1)).andStubReturn(company);

        CommissionProcessConfigurationDAS configurationDAS = createNiceMock(CommissionProcessConfigurationDAS.class);
        task.setConfigurationDAS(configurationDAS);
        expect(configurationDAS.findByEntity(anyObject())).andStubReturn(
                new CommissionProcessConfigurationDTO.Builder()
                        .id(1)
                        .company(company)
                        .nextRunDate(nextRunDate)
                        .periodUnit(new PeriodUnitDTO(Constants.PERIOD_UNIT_MONTH))
                        .periodValue(1)
                        .build());


        PartnerDAS partnerDAS = createNiceMock(PartnerDAS.class);
        task.setPartnerDAS(partnerDAS);
        expect(partnerDAS.find(anyObject())).andStubReturn(partnerDTO);
        expect(partnerDAS.findPartnersByCompany(anyObject())).andStubReturn(
                print("partnerDAS.findPartnersByCompany - {}", Arrays.asList(partnerDTO))
        );

        CustomerEnrollmentDAS customerEnrollmentDAS = createNiceMock(CustomerEnrollmentDAS.class);
        task.setCustomerEnrollmentDAS(customerEnrollmentDAS);
        expect(customerEnrollmentDAS.findCustomerEnrollmentDate(company.getId(), CUST_ACC_NR)).andStubReturn(
                parseDate("2016-01-15")
        );

        //we will return 2 invoices for the customer
        InvoiceDAS invoiceDAS = createNiceMock(InvoiceDAS.class);
        task.setInvoiceDAS(invoiceDAS);
        expect(invoiceDAS.findForPartnerCommissions(eq(partnerDTO.getId()), anyObject(Date.class))).andStubReturn(
                Arrays.asList(invoice1.getId(), invoice2.getId())
        );
        expect(invoiceDAS.find(anyObject())).andStubAnswer(new IAnswer<InvoiceDTO>() {
            @Override
            public InvoiceDTO answer() throws Throwable {
                Integer id = (Integer)getCurrentArguments()[0];
                if(id.equals(invoice1.getId())) return invoice1;
                if(id.equals(invoice2.getId())) return invoice2;
                fail("Unknown invoice id " + id);
                return null;
            }
        });

        //store all saved lines so we can return it later.
        List < PartnerCommissionLineDTO > partnerCommissionLineDTOs = new ArrayList<>();

        //for the first invoice we will return an empty list - no commission received.
        //on the second call we will return a value to indicate the first one was saved
        PartnerCommissionDAS partnerCommissionDAS = createMock(PartnerCommissionDAS.class);
        task.setPartnerCommissionDAS(partnerCommissionDAS);
        expect(partnerCommissionDAS.findCustomerCommission(anyObject(), anyObject()))
                .andReturn(Arrays.asList());
        expect(partnerCommissionDAS.save(anyObject(CustomerCommissionDTO.class))).andStubAnswer(new IAnswer<PartnerCommissionLineDTO>() {
            @Override
            public PartnerCommissionLineDTO answer() throws Throwable {
                CustomerCommissionDTO commission = (CustomerCommissionDTO) getCurrentArguments()[0];
                assertEquals(new BigDecimal("5"), commission.getAmount());
                assert commission.getUser().getId() == user.getId();
                assert commission.getPartner().getId() == partnerDTO.getId();
                assert commission.getCommissionProcessRun() != null;
                partnerCommissionLineDTOs.add(commission);
                return commission;
            }
        });
        partnerCommissionDAS.reattach(anyObject());
        expectLastCall().anyTimes();
//        expect(partnerCommissionDAS.findCustomerCommission(user)).andReturn(Arrays.asList(new CustomerCommissionDTO()));

        PaymentCommissionDAS paymentCommissionDAS = createNiceMock(PaymentCommissionDAS.class);
        task.setPaymentCommissionDAS(paymentCommissionDAS);

        PartnerReferralCommissionDAS partnerReferralCommissionDAS = createNiceMock(PartnerReferralCommissionDAS.class);
        task.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        expect(partnerReferralCommissionDAS.findAllForCompany(anyObject())).andStubReturn(new ArrayList<>(0));
        expect(partnerCommissionDAS.findByPartnerAndProcessRun(anyObject(), anyObject())).andStubReturn(partnerCommissionLineDTOs);

        CommissionDAS commissionDAS = createMock(CommissionDAS.class);
        task.setCommissionDAS(commissionDAS);
        expect(commissionDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionDTO>() {
            @Override
            public CommissionDTO answer() throws Throwable {
                CommissionDTO commission = (CommissionDTO) getCurrentArguments()[0];
                assert commission.getAmount().equals(new BigDecimal("5"));
                assertEquals(CommissionType.CUSTOMER_COMMISSION, commission.getType());
                return commission;
            }
        });

        CommissionProcessRunDAS commissionProcessRunDAS = createNiceMock(CommissionProcessRunDAS.class);
        task.setProcessRunDAS(commissionProcessRunDAS);
        expect(commissionProcessRunDAS.save(anyObject())).andStubAnswer(new IAnswer<CommissionProcessRunDTO>() {
            @Override
            public CommissionProcessRunDTO answer() throws Throwable {
                return (CommissionProcessRunDTO)getCurrentArguments()[0];
            }
        });

        EventLogger eventLogger = createNiceMock(EventLogger.class);

        PartnerBL partnerBL = new PartnerBL(false);
        partnerBL.setPartnerDAS(partnerDAS);
        partnerBL.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
        partnerBL.setPartnerCommissionDAS(partnerCommissionDAS);
        partnerBL.setCommissionDAS(commissionDAS);
        partnerBL.setELogger(eventLogger);
        task.setPartnerBL(partnerBL);

        PlatformTransactionManager txMgr = createNiceMock(PlatformTransactionManager.class);
        task.setTransactionManager(txMgr);

        Object[] mocks = new Object[] {commissionDAS, partnerCommissionDAS, partnerReferralCommissionDAS, paymentCommissionDAS,
                invoiceDAS, customerEnrollmentDAS, configurationDAS, partnerDAS, companyDAS, commissionProcessRunDAS, eventLogger, txMgr};

        replay(mocks);
        task.calculateCommissions(1);
        verify(mocks);
    }

    private <T> T print(String msg, T obj) {
        logger.debug(msg, obj);
        return obj;
    }
    private static PartnerCommissionValueDTO commissionValue(int days, BigDecimal rate) {
        PartnerCommissionValueDTO valueDTO = new PartnerCommissionValueDTO();
        valueDTO.setDays(days);
        valueDTO.setRate(rate);
        return valueDTO;
    }

    private static Date parseDate(String date) {
        return Date.from(LocalDate.parse(date, dateFormat).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }
}
