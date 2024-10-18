/**
 * 
 */
package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;

/**
 * @author mazhar
 *
 */
public interface IExternalACHStorage {
	
	public String storeACH(ContactDTO contact, PaymentInformationDTO instrument, boolean updateKey);

	public String deleteACH(ContactDTO contact, PaymentInformationDTO instrument);
}
