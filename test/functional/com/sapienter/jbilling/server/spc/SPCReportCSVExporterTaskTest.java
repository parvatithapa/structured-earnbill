package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.junit.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

@Test(groups = "agl", testName = "agl.SPCReportCSVExporterTaskTest")
public class SPCReportCSVExporterTaskTest extends SPCBaseConfiguration {

    private static final String ASSET01                 = "1231231231";
    private static final String ASSET02                 = "1231231444";
    private static final String ASSET03                 = "1231231555";
    private static final String TEST_CUSTOMER_OPTUS     = "Test-SPC";
    private static final String TEST_CUSTOMER_TELSTRA   = "Test-SPC-Telstra";
    private static final String OPTUS_PLAN              = "SPCMO-0111";
    private static final String TELSTRA_PLAN            = "SPCMT-02X";
    private static final int    BILLIING_TYPE_MONTHLY   = 1;
    private static final String MEDIATION_FILE_PREFIX   = "RESELL_";
    private final static String LEVEL_DEBUG = "DEBUG";
    String callerClass = "c.s.j.s.spc.SPCReportCSVExporterTask";
    List<AssetWS> assetWSs = new ArrayList<>();
    UserWS spcOptusUserWS;
    UserWS spcTelstraUserWS;
    Map<Integer, BigDecimal> productQuantityMapOptus = new HashMap<>();
    Map<Integer, BigDecimal> productQuantityMapTelstra = new HashMap<>();
    @Resource(name = "spcJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    
    String msg = "Executing : "+SPCReportCSVExporterTask.class.getName()+"-1 : ";

    @BeforeClass
    public void beforeClass () {

        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        
        testBuilder.given(envBuilder -> {
            logger.debug("SPCReportCSVExporterTaskTest.beforeClass : {} "+testBuilder);
        });
    }

    /**
     * Customer NID - 1st Last month
     * Order Prepaid - Active Since Day - 1st of last Month
     * Generate Invoice - 1st of Last Month
     * Update NID - 1st of current month
     * Upload Mediation - event date - from last month
     * Mediation Order - 1st of last month Onetime
     * Generate Invoice - 1st of current month.
     * System will generate Credit order with the latest invoice
     */
    @Test(priority = 1)
    public void triggerSPCReportCSVExporterTaskMultiple() {

        try {
            testBuilder.given(envBuilder -> {
                
                String companyTimeZone = "Australia/Sydney";
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                configureSPCReportCSVExporterTask(confBuilder);
                confBuilder.build();

                PluggableTaskWS pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(SPCReportCSVExporterTask.class.getName())
                        .getId());

                Map<String, String> spcReportCSVExporterTaskParams = pluggableTask.getParameters();
                String cronExpression = getCronExpression(LocalDateTime.now(ZoneId.of(companyTimeZone)).plusMinutes(1));
                cronExpression = "0/5 * * * * ? *";
                spcReportCSVExporterTaskParams.put("cron_exp", cronExpression);
                spcReportCSVExporterTaskParams.put("spc_report_names_list", "detailed_billing_report");
                spcReportCSVExporterTaskParams.put("spc_csv_export_path", "resources/spc-reports/");
                spcReportCSVExporterTaskParams.put("spc_csv_files_split_limit", "10000");

                updateExistingPlugin(api, pluggableTask.getId(), SPCReportCSVExporterTask.class.getName(), spcReportCSVExporterTaskParams);

                Integer spcCReportCSVExporterTask = pluggableTask.getId();

                confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                configureSPCReportCSVExporterTask(confBuilder);
                confBuilder.build();
                PluggableTaskWS[] pluggableTask1 = api.getPluginsWS(api.getCallerCompanyId(),SPCReportCSVExporterTask.class.getName());                               
                Map<String, String> spcReportCSVExporterTaskParams1 = pluggableTask1[1].getParameters();
                String cronExpression1 = getCronExpression(LocalDateTime.now(ZoneId.of(companyTimeZone)).plusMinutes(1));
                cronExpression1 = "0/6 * * * * ? *";
                spcReportCSVExporterTaskParams1.put("cron_exp", cronExpression1);
                spcReportCSVExporterTaskParams1.put("spc_report_names_list", "active_services_report");
                spcReportCSVExporterTaskParams1.put("spc_csv_export_path", "resources/spc-reports/");
                spcReportCSVExporterTaskParams1.put("spc_csv_files_split_limit", "10000");

                updateExistingPlugin(api, pluggableTask1[1].getId(), SPCReportCSVExporterTask.class.getName(), spcReportCSVExporterTaskParams1);

                Integer spcCReportCSVExporterTask1 = pluggableTask1[1].getId();

            }).validate((testEnv, testEnvBuilder) -> {
                waitFor(9L);
                validateEnhancedLog( LEVEL_DEBUG, callerClass, msg);
                logger.debug("No exception is thrown because the username is not in use.");
            });
        } finally {
            logger.debug("Finally called");
        }
    }


    private void validateEnhancedLog(String priorityLevel, String callerClass, String message) {
          
        int count = getQrtzTriggerrsCount();    
        logger.debug("plugin triggerred after the test case run based on logs {}",count);
        assertEquals("Expected plugin configured in the test cases not matching...!!!", 2,count);
    }
    
    private static Integer getOccurranceCount(String priorityLevel, String callerClass, String message) {
        Integer count=0;
        String path  = null;
        String fileName =null;
        try {
            path = new File(".").getCanonicalPath();
            fileName = path + File.separator + "logs" + File.separator + "jbilling.log";
        } catch (Exception e){ 
            logger.error("exception occurred..."+e.getMessage());
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            String line;
            while ((line = br.readLine()) != null) {
              
                boolean validLine = line.contains(priorityLevel) //valid priority
                        && line.contains(callerClass) //valid class
                        && line.contains(message); //valid message

                if (validLine) {                
                    count++;                
                }
            }

        } catch (IOException e) {
            logger.error("exception occurred..."+e.getMessage());
        }
        
        return count;
    }
    
    

    @AfterClass
    private void teardown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
    }

    private String getCronExpression(LocalDateTime localDateTime) {        

        logger.debug("Cron Expression : {}",toCron(String.valueOf(localDateTime.getSecond()),String.valueOf(localDateTime.getMinute()),String.valueOf(localDateTime.getHour())));

        return toCron(String.valueOf(localDateTime.getSecond()),String.valueOf(localDateTime.getMinute()),String.valueOf(localDateTime.getHour()));
    }

    // 0 0 10 1/1 * ? *
    public static String toCron(final String Secs, final String mins, final String hrs) {
        return String.format("%s %s %s 1/1 * ? *",Secs, mins, hrs);
    }
    
    private int getQrtzTriggerrsCount() {
        String sql = "SELECT Count(*) as count from qrtz_triggers where trigger_name ilike '%SPCReportCSVExporterTask%'";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql);
        return row.next() ? row.getInt("count") : 0;
    }
}
