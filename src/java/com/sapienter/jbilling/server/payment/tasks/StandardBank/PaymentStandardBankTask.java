package com.sapienter.jbilling.server.payment.tasks.StandardBank;

import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.UserWS;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by Wajeeha Ahmed on 7/9/17.
 */
public class PaymentStandardBankTask {

    private static final Logger LOG = Logger.getLogger(PaymentStandardBankTask.class);
    private final List<Date> holidays;
    private ServiceProfile serviceProfile;
    private Integer entityId;

    public PaymentStandardBankTask(Integer entityId, List<Date> holidays, ServiceProfile serviceProfile){
        this.entityId = entityId;
        this.holidays = holidays;
        this.serviceProfile = serviceProfile;
    }

    public boolean requestPayment(PaymentWS payment, Integer[] orderIds, UserWS userWS, String userReference) throws PluggableTaskException {
        LOG.debug("Sending request to Standard Bank Payment Manger");
        StandardBankPaymentManager standardBankPaymentManager = new StandardBankPaymentManager(serviceProfile,this.entityId,holidays);
        Boolean status = standardBankPaymentManager.processPayment(payment,orderIds, userWS, userReference);
        return status;
    }

    public void processStandardBankResponseFile() throws IOException {
        StandardBankPaymentManager standardBankPaymentManager = new StandardBankPaymentManager(null,this.entityId,holidays);
        standardBankPaymentManager.processStandardBankResponseFile();
    }

    public void transferInputFileToServer(String host, int port, String username, String password){
        StandardBankPaymentManager standardBankPaymentManager = new StandardBankPaymentManager(serviceProfile,entityId,holidays);
        standardBankPaymentManager.sendInputFile(host,port,username,password);
    }

    public void transferOutputFileFromServer(String host, int port, String username, String password)throws Exception{
        StandardBankPaymentManager standardBankPaymentManager = new StandardBankPaymentManager(serviceProfile,entityId,holidays);
        standardBankPaymentManager.getResponseFiles(host,port,username,password);
    }
}
