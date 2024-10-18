package com.sapienter.jbilling.server.payment.tasks;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.tasks.StandardBank.PaymentStandardBankTask;
import com.sapienter.jbilling.server.payment.tasks.absa.ABSAPaymentManager;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;

/**
 * Created by taimoor on 8/7/17.
 */
public class IgnitionResponseManagerTask extends AbstractCronTask {

    private ABSAPaymentManager paymentABSATask;
    private PaymentStandardBankTask paymentStandardBankTask;

    private static final FormatLogger logger = new FormatLogger(IgnitionResponseManagerTask.class);
    private static final ParameterDescription USERNAME =
            new ParameterDescription("username", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PASSWORD =
            new ParameterDescription("password", true, ParameterDescription.Type.STR, true);
    private static final ParameterDescription HOST =
            new ParameterDescription("host", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PORT =
            new ParameterDescription("port", true, ParameterDescription.Type.INT);

    {
        descriptions.add(USERNAME);
        descriptions.add(PASSWORD);
        descriptions.add(HOST);
        descriptions.add(PORT);
    }

    private  String getUserName(JobExecutionContext context) {
        return  context.getJobDetail().getJobDataMap().getString("username");
    }

    private  String getPassword(JobExecutionContext context) {
        return  context.getJobDetail().getJobDataMap().getString("password");
    }

    private  String getHost(JobExecutionContext context) {
        return  context.getJobDetail().getJobDataMap().getString("host");
    }

    private  Integer getPort(JobExecutionContext context) {
        return  Integer.parseInt(context.getJobDetail().getJobDataMap().getString("port"));
    }

    public IgnitionResponseManagerTask() {
        setUseTransaction(true);
    }

    @Override
    public String getTaskName() {
        return "Ignition Payment Response Manager, entity Id: " + getEntityId();
    }

    @Override
    public void doExecute (JobExecutionContext context) throws JobExecutionException {
        try (RunAsUser ctx = new RunAsCompanyAdmin(getEntityId())) {
            logger.debug("Running task using '%s' account", ctx.getUserName());

            _doExecute(context);

            logger.debug("Done. Running task using '%s' account", ctx.getUserName());
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }

    private void _doExecute (JobExecutionContext context) throws Exception {

        paymentABSATask = new ABSAPaymentManager(this.getEntityId(), null, null);
        paymentStandardBankTask = new PaymentStandardBankTask(this.getEntityId(), null, null);

        String host = getHost(context);
        Integer port = getPort(context);
        String username = getUserName(context);
        String password = getPassword(context);

        // Receive Files
        getABSAFiles(host, port, username, password);
        getStandardBankFiles(host, port, username, password);

        // Process Response Files
        processABSAResponseFile();
        processABSAOutputFile();
        processStandardBankResponseFile();
    }

    private void getABSAFiles(String host, int port, String username, String password){
        try {
            paymentABSATask.getResponseFiles(host, port, username, password);
        } catch (Exception e) {
            logger.error("Exception occurred while trying to get ABSA Response files.",e);
        }
    }

    private void getStandardBankFiles(String host, int port, String username, String password){
        try {
            paymentStandardBankTask.transferOutputFileFromServer(host, port, username, password);
        } catch (Exception e) {
            logger.error("Exception occurred while trying to get Standard Bank Response files.",e);
        }
    }

    private void processABSAResponseFile(){
        try {
            paymentABSATask.processReplyFile();
        } catch (Exception e) {
            logger.error("Exception occurred while trying to process ABSA Response file.",e);
        }
    }

    private void processABSAOutputFile(){
        try {
            paymentABSATask.processOutputFile();
        } catch (Exception e) {
            logger.error("Exception occurred while trying to process ABSA Output file.",e);
        }
    }

    private void processStandardBankResponseFile(){
        try {
            paymentStandardBankTask.processStandardBankResponseFile();
        }
        catch (Exception exception){
            logger.error("Exception occurred while trying to process Standard Bank response file.",exception);
        }
    }
}
