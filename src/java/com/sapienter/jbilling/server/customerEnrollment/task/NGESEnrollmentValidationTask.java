package com.sapienter.jbilling.server.customerEnrollment.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.customerEnrollment.event.ValidateEnrollmentEvent;
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Created by neeraj on 23/02/15.
 */
public class NGESEnrollmentValidationTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESEnrollmentValidationTask.class));

    private static final Class<Event> events[] = new Class[]{
            ValidateEnrollmentEvent.class
    };

    //initializer for pluggable params
    {
    }


    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    IEDITransactionBean ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
    @Override
    public void process(Event event) throws PluggableTaskException {

        if (!(event instanceof ValidateEnrollmentEvent)) {
            return;
        }
        CustomerEnrollmentDTO enrollmentDTO=((ValidateEnrollmentEvent) event).getEnrollmentDTO();
        if (FileConstants.COMPANY_TYPE_ESCO.equals(enrollmentDTO.getCompany().getType().toString())) {
            LOG.error("Do not allow to create a enrollment for ESCO type company");
            throw new SessionInternalError("Cannot allow to creating an enrollment for ESCO type company", new String[]{"enrollment.not.allow.for.ESCO"});
        }

        MetaFieldValue fieldValue = enrollmentDTO.getMetaField(FileConstants.CUSTOMER_ACCOUNT_KEY);
        if (fieldValue != null) {
            String enrollmentAccountNumber = (String) fieldValue.getValue();
            if (enrollmentAccountNumber != null && enrollmentAccountNumber.length() != enrollmentAccountNumber.trim().length()) {
                fieldValue.setValue(enrollmentAccountNumber.trim());
                String message = "Remove the space from " + FileConstants.CUSTOMER_ACCOUNT_KEY+" is \""+fieldValue.getValue().toString()+"\" instead of \""+enrollmentAccountNumber+"\"";
                LOG.info(message);
                if (StringUtils.isNotEmpty(enrollmentDTO.getMessage())) {
                    enrollmentDTO.setMessage(enrollmentDTO.getMessage().concat(";" + message));
                } else {
                    enrollmentDTO.setMessage(message);
                }
            }
        }

        // validating customer account number
        if(enrollmentDTO.getMetaField(FileConstants.CUSTOMER_ACCOUNT_KEY)!=null && enrollmentDTO.getMetaField(FileConstants.CUSTOMER_ACCOUNT_KEY).getValue()!=null){
            ediTransactionBean.isCustomerExistForAccountNumber(enrollmentDTO);
        }

    }


}
