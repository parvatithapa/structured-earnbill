package jbilling

import com.sapienter.jbilling.server.payment.tasks.paypal.PaypalIPNBL
import com.sapienter.jbilling.server.payment.tasks.paypal.PaypalIPNWS

import java.text.SimpleDateFormat

public class PaypalIPNController
{
    static allowedMethods = [process: 'POST']
    private static SimpleDateFormat formatDate = new SimpleDateFormat("HH:mm:ss dd MMM yyyy zzz");
    private final static int VERIFIED_PAYPALIPN=1;
    private final static int UNVERIFIED_PAYPALIPN=0;
    private final static char negative ="-";

    def webServicesSession

        def process = {
            log.debug "Received IPN notification from PayPal Server ${params}"
            def server = "https://www.sandbox.paypal.com/cgi-bin/webscr"
            def login = "paypal-facilitator@answerconnect.com"
            if (!server || !login) throw new IllegalStateException("Paypal misconfigured! You need to specify the Paypal server URL and/or account email. Refer to documentation.")

            params.cmd = "_notify-validate"
            def queryString = params.toQueryString()[1..-1]

            log.debug "Sending back query $queryString to PayPal server $server"
            def url = new URL(server)
            def conn = url.openConnection()
            conn.doOutput = true
            def writer = new OutputStreamWriter(conn.getOutputStream())
            writer.write queryString
            writer.flush()

            def result = conn.inputStream.text?.trim()

            log.debug "Got response from PayPal IPN $result"

            if (result == 'VERIFIED') {
                PaypalIPNBL.save(bindPaypalIPNParams(params,VERIFIED_PAYPALIPN))
                if (params.receiver_email != login) {
                    log.warn """WARNING: receiver_email parameter received from PayPal does not match configured e-mail. This request is possibly fraudulent!
                    REQUEST INFO: ${params}
				"""
                }
            }
            else {
                PaypalIPNBL.save(bindPaypalIPNParams(params,UNVERIFIED_PAYPALIPN))
                log.debug "Error with PayPal IPN response: [$result] "
            }

            render "OK" // Paypal needs a response, otherwise it will send the notification several times!

    }
    def bindPaypalIPNParams(params,verified){

        PaypalIPNWS paypalIPNWS= new PaypalIPNWS();

        String dateInString = params.payment_date;
        Date paymentDate = formatDate.parse(dateInString);

        paypalIPNWS.itemNumber =params.item_number
        paypalIPNWS.verifySign = params.verify_sign
        paypalIPNWS.business= params.business
        paypalIPNWS.payerStatus= params.payer_status
        paypalIPNWS.transactionSubject= params.transaction_subject
        paypalIPNWS.protectionEligibilty= params.protection_eligibilty
        paypalIPNWS.firstName= params.first_name
        paypalIPNWS.payerId= params.payer_id
        paypalIPNWS.payerEmail= params.payer_email
        paypalIPNWS.mcFee= params.mc_fee
        paypalIPNWS.txnId= params.txn_id
        paypalIPNWS.parentTxnId=params.parent_txn_id
        paypalIPNWS.quantity= params.quantity
        paypalIPNWS.recieverEmail= params.receiver_email
        paypalIPNWS.notifyVersion= params.notify_version
        paypalIPNWS.paymentStatus= params.payment_status
        String mcGross= params.mc_gross
        paypalIPNWS.mcGross=  mcGross != null ? PaypalIPNBL.removeChar(mcGross,negative) : null
        paypalIPNWS.paymentDate= paymentDate
        String paymentGross=params.payment_gross
        paypalIPNWS.paymentGross=paymentGross !=null ? PaypalIPNBL.removeChar(paymentGross,negative) : null
        paypalIPNWS.ipnTrackId= params.ipn_track_id
        paypalIPNWS.receiptId= params.receipt_id
        paypalIPNWS.lastName= params.last_name
        paypalIPNWS.paymentType= params.payment_type
        paypalIPNWS.verified=verified

        return paypalIPNWS

    }

}