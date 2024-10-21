package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 * @author amey.pelapkar
 * This interface allows to implement Strong Customer Authentication(SCA)  
 */


public interface ISecurePayment {


	/** Strong Customer Authentication(SCA) - Customer should authenticate transaction against 3D secure authentication.
	 * Transactions that required customer authentication 
	 * 		Process one time payment
	 * 		Store card for future payment
	 * 		Card to be used for recurrence payment 
	 * @param paymentInstrument
	 * @return SecurePaymentWS
	 * @throws PluggableTaskException
	 */
	public default SecurePaymentWS perform3DSecurityCheck(PaymentInformationDTO paymentInstrument, PaymentDTOEx paymentDTOEX)throws PluggableTaskException{
		return null;
	}

}