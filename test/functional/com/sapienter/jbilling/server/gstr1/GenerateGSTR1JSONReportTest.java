package com.sapienter.jbilling.server.gstr1;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static net.sf.json.test.JSONAssert.assertNotNull;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;
import static org.testng.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@Test(groups = {"test-earnbill", "earnbill"}, testName = "GenerateGSTR1JSONReportTest")
public class GenerateGSTR1JSONReportTest {

    private static final Logger logger = LoggerFactory.getLogger(GenerateGSTR1JSONReportTest.class);
    private static JbillingAPI api = null;
    private static int ORDER_CHANGE_STATUS_APPLY_ID;
    private static int ORDER_PERIOD_MONTHLY_ID = 1;
    private TestBuilder testBuilder;
    private EnvironmentHelper envHelper;
    private static final String GST_ZEN_EINVOICE_PROVIDER_PLUGIN = "com.sapienter.jbilling.einvoice.plugin.GstZenEInvoiceProviderPlugin";
    private AccountTypeWS refAccountType;
    private Integer userId;
    private static final String TEST_CUSTOMER_CODE = "Jack" + System.currentTimeMillis();

    @BeforeClass
    public void setupClass() throws Exception {

        testBuilder = getTestEnvironment();
        api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api).intValue();
        refAccountType = api.getAccountType(1);

        testBuilder.given(envBuilder -> {
            Hashtable<String, String> parameters = new Hashtable<>();

            parameters.put("company_state", "UTTARAKHAND");
            parameters.put("gst_zen_base_url", "https://my.gstzen.in/~gstzen/a/post-einvoice-data/einvoice-json/");
            parameters.put("gstin", "05AADCG4992P1ZZ");
            parameters.put("gst_state_code", "05");
            parameters.put("company_name", "GSTZEN DEMO PRIVATE LIMITED");
            parameters.put("company_address", "UTTARAKHAND");
            parameters.put("postal_code", "263642");
            parameters.put("auth_token", "de3a3a01-273a-4a81-8b75-13fe37f14dc6");
            parameters.put("send_einvoice_on_successful_payment", "false");
            parameters.put("timeout", "1000");
            parameters.put("dispatcher_name", "Maharashtra storage");
            parameters.put("dispatcher_location", "Pune");
            parameters.put("dispatcher_address_1", "Karve nagar lane no.6");
            parameters.put("dispatcher_pin_code", "411030");
            parameters.put("dispatcher_state_code", "27");

            PluggableTaskWS plugins[] = api.getPluginsWS(api.getCallerCompanyId(), GST_ZEN_EINVOICE_PROVIDER_PLUGIN);
            if (null == plugins || plugins.length == 0) {
                Integer pluginId = buildAndPersistPlugIn(envBuilder, api, GST_ZEN_EINVOICE_PROVIDER_PLUGIN, parameters);
                System.out.println("pluginId : " + pluginId);
            }
            userId = createCustomer(envBuilder, refAccountType.getId(), new Date());
        }).test((testEnv, testEnvBuilder) -> {
        });
    }

    public static Integer buildAndPersistPlugIn(TestEnvironmentBuilder envBuilder, JbillingAPI api, String pluginClassName, Hashtable<String, String> parameters) {
        envBuilder.configurationBuilder(api)
                .addPluginWithParameters(pluginClassName, parameters)
                .build();
        return envBuilder.idForCode(pluginClassName);
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }

    @Test
    public void test01GenerateGSTR1JSONReport() throws Exception {

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
        item.setId(api.createItem(item));

        // price for the monthly order.
        BigDecimal price = new BigDecimal("82.81");

        // setup orders
        OrderWS order = setupOrder(userId, item, price, ORDER_PERIOD_MONTHLY_ID);

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        String startDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String endtDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String jSON = api.generateGstR1JSONFileReport(startDate, endtDate);
        assertNotNull("GSTR-1 data in JSON format is ready.", jSON);
        assertTrue("String should not be empty", !jSON.isEmpty());
        logger.debug("GSTR-1 data in JSON format is ready.",jSON);
    }

    @Test
    public void test02DateValidationFileNotFound() throws Exception {

        String startDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String endtDate = LocalDate.now().plusDays(6).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        assertThrows(SessionInternalError.class, () -> {
            api.generateGstR1JSONFileReport(startDate, endtDate);
        });
        try{
            api.generateGstR1JSONFileReport(startDate, endtDate);
        }catch(SessionInternalError e){
            logger.debug("Error message : ",e.getErrorMessages());
            String actual = e.getErrorMessages()[0].toString().trim();
            String ecpected = "Invoice not found between these period of time.";
            assertEquals("Exception is not same",ecpected,actual);
        }
    }

    @Test
    public void test03ValidateDatePeriod90Days() throws Exception {

        String startDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String endtDate = LocalDate.now().plusDays(91).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        assertThrows(SessionInternalError.class, () -> {
            api.generateGstR1JSONFileReport(startDate, endtDate);
        });
        try{
            api.generateGstR1JSONFileReport(startDate, endtDate);
        }catch(SessionInternalError e){
            logger.debug("Error message : ",e.getMessage());
            String actual = e.getErrorMessages()[0].toString().trim();
            String ecpected = "Maximum Period Exceeded.The maximum period for generating GSTR-1 is 90 days." +
                    "Please choose a date range within the allowed 90 days period.You cannot select dates beyond this limit.";
            assertEquals("Exception is not same",ecpected,actual);
        }
    }

    private OrderWS setupOrder(Integer userId, ItemDTOEx item, BigDecimal price, int period) {
        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(period);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(price);
        line.setAmount(price);

        order.setOrderLines(new OrderLineWS[]{line});
        return order;
    }

    private ItemTypeWS buildItemType() {
        ItemTypeWS type = new ItemTypeWS();
        type.setDescription("Invoice, Item Type:" + System.currentTimeMillis());
        type.setOrderLineTypeId(1);//items
        type.setAllowAssetManagement(0);//does not manage assets
        type.setOnePerCustomer(false);
        type.setOnePerOrder(false);
        return type;
    }

    private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId) {
        ItemDTOEx item = new ItemDTOEx();
        long millis = System.currentTimeMillis();
        String name = String.valueOf(millis) + new Random().nextInt(10000);
        item.setDescription("Invoice, Product:" + name);
        item.setPriceModelCompanyId(priceModelCompanyId);
        item.setPrice(new BigDecimal("10"));
        item.setNumber("INV-PRD-" + name);
        item.setAssetManagementEnabled(0);
        Integer typeIds[] = new Integer[]{itemTypeId};
        item.setTypes(typeIds);
        return item;
    }

    private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
        OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
        for (OrderChangeStatusWS status : statuses) {
            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status.getId();
            }
        }
        //there is no APPLY status in db so create one
        OrderChangeStatusWS apply = new OrderChangeStatusWS();
        String status1Name = "APPLY: " + System.currentTimeMillis();
        OrderChangeStatusWS status1 = new OrderChangeStatusWS();
        status1.setApplyToOrder(ApplyToOrder.YES);
        status1.setDeleted(0);
        status1.setOrder(1);
        status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
        return api.createOrderChangeStatus(apply);
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder, Integer accountTypeId, Date nid) {
        logger.debug("creating the customer {}", TEST_CUSTOMER_CODE);
        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                .withUsername(TEST_CUSTOMER_CODE).withAccountTypeId(accountTypeId)
                .withMainSubscription(new MainSubscriptionWS(envHelper.getOrderPeriodMonth(api), getDay(nid)));

        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(nid);
        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
        return user.getId();
    }
    private Integer getDay(Date inputDate) {
        logger.debug("getting the day {}", inputDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }

}