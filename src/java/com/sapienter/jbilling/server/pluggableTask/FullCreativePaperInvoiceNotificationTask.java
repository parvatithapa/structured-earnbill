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


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * This plugin handles the Full creative old invoice design 
 * template on the basis of invoice revision date parameter
 * If invoice date is before invoice revision date then use 
 * old invoice template - invoice_design_fc because for old 
 * invoices invoice summary was not populated
 *  
 * @author Ashok Kale
 * @since  11-Jan-2016
 */
public class FullCreativePaperInvoiceNotificationTask
        extends PaperInvoiceNotificationTask {
	
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaperInvoiceNotificationTask.class));
    
    public static final ParameterDescription PARAMETER_INVOICE_REVISION_DATE = 
        	new ParameterDescription("invoice_revision_date", true, ParameterDescription.Type.DATE);
    public static final ParameterDescription PARAMETER_BACKWARD_COMPATIBLE_DESIGN = 
        	new ParameterDescription("backwards-compatible-design", true, ParameterDescription.Type.STR);
    private static final String PAPER_INVOICE_EXCEPTION = "Exception generating paper invoice";
    
    //initializer for pluggable params
    { 
    	descriptions.add(PARAMETER_INVOICE_REVISION_DATE);
    	descriptions.add(PARAMETER_BACKWARD_COMPATIBLE_DESIGN);
    }
    
	private Date invoiceRevisionDate;
	private String invoiceDesign;
	
	@Override
    public void init(UserDTO user, MessageDTO message) throws TaskException {
    	// TODO Auto-generated method stub
    	super.init(user, message);
    	try {
			invoiceRevisionDate = getParameter(PARAMETER_INVOICE_REVISION_DATE.getName());
			invoiceDesign = parameters.get(PARAMETER_BACKWARD_COMPATIBLE_DESIGN.getName());
			if (null != invoiceDesign && getInvoice().getCreateDatetime().before(invoiceRevisionDate)) {
	    		setDesign(invoiceDesign);
	    	}
		} catch (ParseException e) {
			LOG.error(PAPER_INVOICE_EXCEPTION, e);
		}
    }

	public Date getParameter(String key) throws ParseException {
        String value = (String) parameters.get(key);
        if (value == null || value.trim().equals(""))
            return null;
        Date date = new SimpleDateFormat("yyyyMMdd").parse(value);
        LOG.info("In getParameter with key=" + key + " and value=" + value);
        return date;
    }

	/**
     * For Full Creative, need to remove the last blank PDF page.
     * @return boolean
     */
	@Override
	protected boolean removeBlankPage() {
		return true;
    }
}
