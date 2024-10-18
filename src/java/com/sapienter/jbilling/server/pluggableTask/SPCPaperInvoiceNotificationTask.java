/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pluggableTask;


import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin handles the old invoice design 
 * template on the basis of invoice revision date parameter
 * If invoice date is before invoice revision date then use 
 * old invoice template because for old 
 * invoices service summary was not populated
 *  
 * @author Mahesh Shivarkar
 * @since  03-July-2019
 */
public class SPCPaperInvoiceNotificationTask
        extends PaperInvoiceNotificationTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PAPER_INVOICE_EXCEPTION = "Exception generating paper invoice";

    @Override
    public void init(UserDTO user, MessageDTO message) throws TaskException {
        super.init(user, message);
        setDesign(getInvoiceDesignByDate(getInvoice().getCreateDatetime()));
    }

    private String getInvoiceDesignByDate(Date invoiceDate) {
        Map<String, String> params = getParameters();
        Map<Date, String> paramsDateMap = new TreeMap<>();
        params.keySet().stream().sorted().forEach(key ->
                getDateMap(key, paramsDateMap)
        );
        String design = params.get("design");
        for (Entry<Date, String> entry : paramsDateMap.entrySet()) {
            if (entry.getKey().after(invoiceDate)) {
                design = params.get(entry.getValue());
                break;
            }
        }
        logger.debug("SPC Invoice Design :: {} for date :: {}",design, invoiceDate);
        return design;
    }
    private void getDateMap(String date, Map<Date, String> paramsDateMap) {
        if (StringUtils.isNumeric(date)) {
            try {
                SimpleDateFormat dateFromat = new SimpleDateFormat("yyyyMMdd");
                paramsDateMap.put(dateFromat.parse(date), date);
            } catch (ParseException e) {
                logger.error(PAPER_INVOICE_EXCEPTION, e);
            }
        }
    }
}
