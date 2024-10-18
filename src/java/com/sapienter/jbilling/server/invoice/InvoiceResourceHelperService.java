package com.sapienter.jbilling.server.invoice;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
public class InvoiceResourceHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String INVOICE_FILE_NAME_PATTERN = "invoice-%s.pdf";
    private static final String APPLICATION_PDF_TYPE = "application/pdf";

    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;


    @Transactional(readOnly = true)
    public Response generatePdfFileForInvoice(Integer invoiceId) {
        try {
            logger.debug("generating pdf file for invoice {}", invoiceId);
            byte[] data = api.getPaperInvoicePDF(invoiceId);
            if(ArrayUtils.isEmpty(data)) {
                return Response.noContent()
                        .build();
            }
            InvoiceDTO invoice = new InvoiceDAS().findNow(invoiceId);
            return Response.ok(data, APPLICATION_PDF_TYPE)
                    .header("Content-Disposition" ,"attachment; filename = "+ String.format(INVOICE_FILE_NAME_PATTERN, invoice.getNumber()))
                    .build();
        } catch(SessionInternalError error) {
            logger.error("error in generatePdfFileForInvoice", error);
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public Response getInvoicesByDateRange(String fromDate, String toDate, Integer offset, Integer limit) {
        InvoiceWS[] invoices = api.getInvoicesByDateRange(fromDate, toDate, offset, limit);
        if(ArrayUtils.isEmpty(invoices)) {
            return Response.noContent().build();
        }
        return Response.ok()
                .entity(invoices)
                .build();
    }
}
