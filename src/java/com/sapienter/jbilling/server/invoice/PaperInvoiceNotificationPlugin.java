package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.server.pluggableTask.FullCreativePaperInvoiceNotificationTask;
import com.sapienter.jbilling.server.pluggableTask.PaperInvoiceNotificationTask;
import com.sapienter.jbilling.server.pluggableTask.SPCPaperInvoiceNotificationTask;


public enum PaperInvoiceNotificationPlugin {

    CORE_PAPER_INVOICE_NOTIFICATION(PaperInvoiceNotificationTask.class.getName()),
    FULLCREATIVE_PAPER_INVOICE_NOTIFICATION(FullCreativePaperInvoiceNotificationTask.class.getName()),
    SPC_PAPER_INVOICE_NOTIFICATION(SPCPaperInvoiceNotificationTask.class.getName());

    private PaperInvoiceNotificationPlugin(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return this.taskName;
    }

    private String taskName;
}
