package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.tasks.StandardBank.PaymentStandardBankTask;
import com.sapienter.jbilling.server.payment.tasks.absa.ABSAPaymentManager;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by Taimoor Choudhary on 9/20/17.
 */
public class IgnitionPaymentInputFilesManagerTask  extends AbstractCronTask {

    private static final FormatLogger logger = new FormatLogger(IgnitionPaymentInputFilesManagerTask.class);

    private ABSAPaymentManager paymentABSATask;
    private PaymentStandardBankTask paymentStandardBankTask;

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
    @Override
    public String getTaskName() {
        return "Ignition Payment Input Files Manager, entity Id: " + getEntityId();
    }

    public IgnitionPaymentInputFilesManagerTask() {
        setUseTransaction(true);
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

        // Send Files
        sendABSAFiles(host, port, username, password);
        sendStandardBankFiles(host, port, username, password);
    }

    private void sendABSAFiles(String host, int port, String username, String password){
        try {
            paymentABSATask.sendInputFile(host, port, username, password);
        } catch (Exception e) {
            logger.error("Exception occurred while trying to send ABSA INPUT file.",e);
        }
    }

    private void sendStandardBankFiles(String host, int port, String username, String password){
        try {
            paymentStandardBankTask.transferInputFileToServer(host, port, username, password);
        } catch (Exception e) {
            logger.error("Exception occurred while trying to send Standard Bank INPUT file.",e);
        }
    }
}
