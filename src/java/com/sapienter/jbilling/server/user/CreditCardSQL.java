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

/*
 * Created on Jan 15, 2005
 *
 */
package com.sapienter.jbilling.server.user;

/**
 * @author Emil
 *
 */
public interface CreditCardSQL {
    
    static final String expiring = 
    		"select bu.id, pi.id, to_date(mf.string_value,'MM/YY') " +
	            " from base_user bu inner join user_status st on bu.status_id = st.id " +
	    		" left outer join ageing_entity_step step on st.id = step.status_id " +
	    		" inner join payment_information pi on bu.id = pi.user_id " +
	    		" inner join payment_information_meta_fields_map pimf on pi.id = pimf.payment_information_id " +
	    		" inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " +
	    		" inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'DATE' and  mf.string_value ~ '(?:0[1-9]|1[0-2])/[0-9]{2}') " +
		    		" where bu.deleted = 0 " +
		    		" and (bu.status_id =  " + UserDTOEx.STATUS_ACTIVE + " or step.suspend = 0) " +
		    		" and (pi.processing_order IS NOT NULL) " +
		    		" and to_date(mf.string_value,'MM/YY') <= ? " +
		    		" and pi.id in ( " +
		    			" select p.id from payment_information p  " +
						    " inner join payment_information_meta_fields_map pimf on p.id = pimf.payment_information_id " +
						    " inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " +
						    " inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'PAYMENT_CARD_NUMBER')" +
						    " )";
}
