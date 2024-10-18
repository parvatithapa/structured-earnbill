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

package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.process.BusinessDays;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import java.io.File;
import java.util.Date;

/**
 * BusinessDayAgeingTask
 *
 * @author Brian Cowdery
 * @since 29/04/11
 */
public class BusinessDayAgeingTask extends BasicAgeingTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BusinessDayAgeingTask.class));

    private static final String PARAM_HOLIDAY_FILE = "holiday_file";
    private static final String PARAM_DATE_FORMAT = "date_format";

    private BusinessDays businessDays;

    private BusinessDays getBusinessDaysHelper() {
        if (businessDays == null) {
            String dateFormat = getParameter(PARAM_DATE_FORMAT, "yyyy-MM-dd");
            String holidayFile = getParameter(PARAM_HOLIDAY_FILE, (String) null);

            if (holidayFile != null) {
                holidayFile = Util.getSysProp("base_dir") + File.separator + holidayFile;
            }

            businessDays = new BusinessDays(new File(holidayFile), DateTimeFormat.forPattern(dateFormat));
        }

        return businessDays;
    }

    @Override
    public boolean isAgeingRequired(UserDTO user, InvoiceDTO overdueInvoice, Integer stepDays, Date today) {

        Date invoiceDueDate = Util.truncateDate(overdueInvoice.getDueDate());
        Date expiryDate = getBusinessDaysHelper().addBusinessDays(invoiceDueDate, stepDays);

        // last status change + step days as week days
        if (expiryDate.equals(today) || expiryDate.before(today)) {
            LOG.debug("User status has expired (last change " + invoiceDueDate + " + "
                      + stepDays + " days is before today " + today + ")");
            return true;
        }

        LOG.debug("User does not need to be aged (last change " + invoiceDueDate + " + "
                  + stepDays + " days is after today " + today + ")");
        return false;
    }
}
