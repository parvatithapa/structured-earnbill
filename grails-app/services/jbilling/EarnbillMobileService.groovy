
package jbilling


import com.sapienter.jbilling.appdirect.subscription.companydetails.AppdirectCompanyWS
import com.sapienter.jbilling.appdirect.subscription.http.AppdirectCompanyAPIClient
import com.sapienter.jbilling.appdirect.subscription.http.exception.AppdirectCompanyClientException
import com.sapienter.jbilling.appdirect.subscription.oauth.OAuthWS
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO
import com.sapienter.jbilling.paymentUrl.domain.response.PaymentResponse
import com.sapienter.jbilling.server.entity.InvoiceLineDTO
import com.sapienter.jbilling.server.invoice.InvoiceWS
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.payment.PaymentInformationWS
import com.sapienter.jbilling.server.payment.PaymentInvoiceMapWS
import com.sapienter.jbilling.server.payment.PaymentWS
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO
import com.sapienter.jbilling.server.user.CompanyWS
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.CurrencyWS
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS
import org.apache.commons.lang.StringUtils
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.function.Supplier

import static java.util.stream.Collectors.toList

class EarnbillMobileService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    WebServicesSessionSpringBean webServicesSession

    Map getUserDetails(Integer userId) {
        try {
            UserWS userWS = webServicesSession.getUserWS(userId)
            def map = [:] ;
             Integer languageId = userWS.getLanguageId()
            map.put("userId", userWS.getUserId())
            map.put("userName", userWS.getUserName())
            map.put("status", userWS.getStatus())
            map.put("owingBalance", userWS.getOwingBalance())
            map.put("nextInvoiceDate", userWS.getNextInvoiceDate())
            if(null != userWS.getMainSubscription() && null != userWS.getMainSubscription().getPeriodId()) {
                map.put("billingCycle", getInternationalDescription(Constants.TABLE_ORDER_PERIOD, languageId, userWS.getMainSubscription().getPeriodId()))
            }
            map.put("companyName", userWS.getCompanyName())
            map.put("email", getMetafieldValue("EMAIL", userWS.getMetaFields()))
            map.put("phoneNumber", getMetafieldValue("PHONE_NUMBER", userWS.getMetaFields()))
            map.put("address1", getMetafieldValue("ADDRESS1", userWS.getMetaFields()))
            map.put("address2", getMetafieldValue("ADDRESS2", userWS.getMetaFields()))
            map.put("city", getMetafieldValue("CITY", userWS.getMetaFields()))
            map.put("state", getMetafieldValue("STATE_PROVINCE", userWS.getMetaFields()))
            map.put("postalCode", getMetafieldValue("POSTAL_CODE", userWS.getMetaFields()))
            def paymentInstruments = new ArrayList(userWS.getPaymentInstruments().size())
            for (PaymentInformationWS informationWS: userWS.getPaymentInstruments()){
                def paymentInstrument = [:]
                paymentInstrument.put("paymentInstrumentId",informationWS.getId())
                paymentInstrument.put("paymentMethodId",informationWS.getPaymentMethodId())
                PaymentMethodTypeDTO paymentDTO =
                        new PaymentMethodTypeDAS().findByPaymentMethodTypeId(userWS.getEntityId(), informationWS.getPaymentMethodTypeId());
                if(null != paymentDTO) {
                    paymentInstrument.put("paymentMethodDes", paymentDTO.getMethodName())
                }
                paymentInstrument.put("processingOrder",informationWS.getProcessingOrder())
                paymentInstrument.put("cardHolderName", getMetafieldValue('TITLE', informationWS.getMetaFields()))
                paymentInstrument.put("cardNumber", getMetafieldValue('PAYMENT_CARD_NUMBER', informationWS.getMetaFields()))
                paymentInstrument.put("autoPaymentLimit", getMetafieldValue('AUTO_PAYMENT_LIMIT', informationWS.getMetaFields()))
                paymentInstrument.put("autoPaymentAuthorization", getMetafieldValue('AUTO_PAYMENT_AUTHORIZATION', informationWS.getMetaFields()))
                paymentInstruments.add(paymentInstrument)
            }
            map.put("paymentInstrument", paymentInstruments)
            return map
        } catch (Exception e) {
            logger.error("Error get user details, moving to error table", e)
            throw e
        }
    }

    Map getUnpaidInvoices(Integer userId) {
        try {
            Integer[] invoiceIds = webServicesSession.getUnpaidInvoices(userId)
            def map = [:]
            def unpaidInvoice = new ArrayList(invoiceIds.length)
            for (Integer invoiceId : invoiceIds){
                def invoice = [:]
                InvoiceWS ws = webServicesSession.getInvoiceWS(invoiceId)
                if (ws.getStatusDescr().equals("Unpaid")) {
                    invoice.put("invoiceId", ws.getId())
                    invoice.put("dueBalance", ws.getBalance())
                    invoice.put("dueDate", ws.getDueDate())
                    invoice.put("invoiceStatus", ws.getStatusDescr())
                    unpaidInvoice.add(invoice)
                }
            }
            map.put("unpaidInvoices", unpaidInvoice)
            return map
        } catch (Exception e) {
            logger.error("Error getting unpaid invoice list, moving to error table", e)
            throw e
        }
    }

    Map getInvoicesByUserId(Integer userId) {
        try {
            InvoiceWS[] invoiceWS = webServicesSession.getAllInvoicesForUser(userId)
            def map = [:]
            def list = new ArrayList()
            for (InvoiceWS ws : invoiceWS){
                def invoice = [:]
                invoice.put("invoiceId", ws.getId())
                invoice.put("amount", ws.getTotal())
                invoice.put("dueDate", ws.getDueDate())
                invoice.put("status", ws.getStatusId())
                invoice.put("balance", ws.getBalance())
                invoice.put("invoiceStatus", ws.getStatusDescr())
                list.add(invoice)
            }
            map.put("invoices", list)
            return map
        } catch (Exception e) {
            logger.error("Error getting invoices list , moving to error table", e)
            throw e
        }
    }

    Map getInvoiceById(Integer invoiceId) {
        try {
            InvoiceWS invoiceWS = webServicesSession.getInvoiceWS(invoiceId)
            def map = [:]
            map.put("invoiceId", invoiceWS.getId())
            map.put("status", invoiceWS.getStatusId())
            map.put("invoiceStatus", invoiceWS.getStatusDescr())
            map.put("invoiceDate", invoiceWS.getCreateDatetime())
            map.put("dueDate", invoiceWS.getDueDate())
            map.put("generatedDate", invoiceWS.getCreateDatetime())
            map.put("amount", invoiceWS.getTotal())
            map.put("balance", invoiceWS.getBalance())
            map.put("carriedBalance", invoiceWS.getCarriedBalance())
            map.put("paymentAttempts", invoiceWS.getPaymentAttempts())
            def list = new ArrayList()
            for (InvoiceLineDTO ws : invoiceWS.getInvoiceLines()){
                def invoiceLine = [:]
                invoiceLine.put("description", ws.getDescription())
                invoiceLine.put("quantity", ws.getQuantity())
                invoiceLine.put("price", ws.getPrice())
                invoiceLine.put("tax", ws.getTaxAmount())
                invoiceLine.put("amount", ws.getAmount())
                list.add(invoiceLine)
            }
            map.put("invoiceLines", list)
            return map
        } catch (Exception e) {
            logger.error("Error get invoice by invoiceId , moving to error table", e)
            throw e
        }
    }

    Map getOrdersByUserId(Integer userId) {
        try {
            OrderWS[] orderWS ;
            orderWS = webServicesSession.getUsersAllSubscriptions(userId);
            orderWS = webServicesSession.getOrderMetaFieldMap(orderWS);
            def map = [:]
            def list = new ArrayList()
            for (OrderWS ws : orderWS){
                def order = [:]
                order.put("orderId", ws.getId())
                order.put("activeSince", ws.getActiveSince())
                order.put("activeUntil", ws.getActiveUntil())
                order.put("amount", ws.getTotal())
                list.add(order)
            }
            map.put("orders", list)
            return map
        } catch (Exception e) {
            logger.error("Error getting order list, moving to error table", e)
            throw e
        }
    }

    Map getOrderById(Integer orderId) {
        try {
            OrderWS orderWS = webServicesSession.getOrder(orderId)
            def map = [:]
            Integer languageId = getLanguageIdByUserId(orderWS.getUserId())
            map.put("orderId", orderWS.getId())
            map.put("createDate", orderWS.getCreateDate())
            map.put("activeSince", orderWS.getActiveSince())
            map.put("activeUntil", orderWS.getActiveUntil())
            map.put("period",orderWS.getPeriodStr())
            map.put("status", orderWS.getStatusStr())
            def list = new ArrayList()
            for (OrderLineWS ws : orderWS.getOrderLines()){
                def orderLine = [:]
                orderLine.put("description", ws.getDescription())
                orderLine.put("quantity", ws.getQuantity())
                orderLine.put("price", ws.getPrice())
                orderLine.put("total", ws.getAmount())
                list.add(orderLine)
            }
            map.put("orderLines", list)
            map.put("notes", orderWS.getNotes())
            def invoices = new ArrayList()
            for (InvoiceWS ws : orderWS.getGeneratedInvoices()){
                def invoice = [:]
                invoice.put("invoiceId", ws.getId())
                invoice.put("invoiceDate", ws.getCreateDatetime())
                invoice.put("amount", ws.getTotal())
                invoices.add(invoice)
            }
            map.put("generatedInvoice", invoices)
            return map
        } catch (Exception e) {
            logger.error("Error get order by orderId, moving to error table", e)
            throw e
        }
    }

    Map getPaymentsByUserId(Integer userId) {
        try {
            PaymentWS[] paymentWS = webServicesSession.findPaymentsForUser(userId,0,0)
            def map = [:]
            def list = new ArrayList()
            for (PaymentWS ws : paymentWS){
                Integer languageId = getLanguageIdByUserId(ws.getUserId())
                def payment = [:]
                payment.put("paymentId", ws.getId())
                payment.put("paymentDate", ws.getPaymentDate())
                payment.put("paymentAmount", ws.getAmount())
                if(null != ws.getResultId()) {
                    payment.put("paymentResult",getInternationalDescription(Constants.TABLE_PAYMENT_RESULT,languageId, ws.getResultId()))
                }
                list.add(payment)
            }
            map.put("payments", list)
            return map
        } catch (Exception e) {
            logger.error("Error getting payment list, moving to error table", e)
            throw e
        }
    }

    Map getPaymentById(Integer paymentId) {
        try {
            PaymentWS paymentWS = webServicesSession.getPayment(paymentId)
            if (null == paymentWS){
               return paymentWS
            }
            Integer languageId = getLanguageIdByUserId(paymentWS.getUserId())
            def map = [:]
            map.put("paymentId", paymentWS.getId())
            map.put("paymentDate", paymentWS.getPaymentDate())
            map.put("paymentAmount", paymentWS.getAmount())
            if(null != paymentWS.getResultId()) {
                map.put("paymentResult",getInternationalDescription(Constants.TABLE_PAYMENT_RESULT,languageId, paymentWS.getResultId()))
            }
            if(null != paymentWS.getAuthorizationId()) {
                map.put("transactionId", paymentWS.getAuthorizationId().getTransactionId())
                map.put("responseMessage", paymentWS.getAuthorizationId().getResponseMessage())
            }
            map.put("paymentMethod", paymentWS.getMethod())
            def list = new ArrayList()
            for (PaymentInvoiceMapWS ws : paymentWS.getPaymentInvoiceMap()){
                def paymentInvoiceMap = [:]
                paymentInvoiceMap.put("lastInvoice", ws.getInvoiceId())
                paymentInvoiceMap.put("amountCredited", ws.getAmount())
                paymentInvoiceMap.put("date", ws.getCreateDatetime())
                list.add(paymentInvoiceMap)
            }
            map.put("paymentInvoiceMap", list)
            return map
        } catch (Exception e) {
            logger.error("Error get payment by paymentId, moving to error table", e)
            throw e
        }
    }

    Map getCurrencyById(Integer currencyId)
    {
        def map = [:]
           CurrencyWS[] currencies = webServicesSession.getCurrencies()
        for (CurrencyWS currency : currencies){
            if(currency.getId() == currencyId){
                map.put("currencySymbol", currency.getSymbol())
                map.put("currencyCode",currency.getCode())
                map.put("currencyId",currency.getId())
            }
        }
        return map;
    }

    /* internal methods */
    private Object getMetafieldValue(String feildUsage, MetaFieldValueWS[] metaFields) {

        if (null != metaFields) {
            for (MetaFieldValueWS metaField : metaFields) {
                def metaFieldWS = metaField.getMetaField()
                if(feildUsage.equalsIgnoreCase(metaFieldWS.getFieldUsage().toString())) {
                    switch (metaFieldWS.getDataType()) {
                        case "STRING":
                            return metaField.getStringValue();
                        case "INTEGER":
                            return metaField.getIntegerValue();
                        case "DECIMAL":
                            return metaField.getDecimalValue();
                        case "BOOLEAN":
                            return metaField.getBooleanValue();
                        case "DATE":
                            return metaField.getDateValue();
                        case "CHAR":
                            return metaField.getCharValue();
                        default:
                            return metaField.getValue();
                    }
                }
            }
        }
        return null
    }

    private String getCompanyNameFromMarketplace(String customerUuid, OAuthWS oAuthWS) {
        AppdirectCompanyWS appdirectCompany
        try {
            AppdirectCompanyAPIClient apiClient = Context.getBean(Context.Name.APPDIRECT_COMPANY_API_CLIENT)
            appdirectCompany = apiClient.getCompanyDetails(customerUuid,
                    oAuthWS.baseApiUrl,
                    oAuthWS.consumerKey,
                    oAuthWS.consumerSecret)

        } catch (AppdirectCompanyClientException e) {
            logger.error("Failed request", e)
            String fallback = getFallbackCompanyName(customerUuid)
            logger.warn("Un-categorized error, falling back to generated company name ${fallback}")
            return fallback
        }

        if (null == appdirectCompany) {
            logger.error("Company not found in marketplace (404)")
            throw new SessionInternalError("Company not found", HttpStatus.SC_INTERNAL_SERVER_ERROR)
        }
        return appdirectCompany.getName()
    }

    private MetaFieldValueWS[] getMetaFieldValues(Map<Integer, List<MetaField>> aitMetaFieldsMap,
                                                  String customerUuid, String appDirectSubscriptionId,
                                                  Integer productId) {

        List<MetaFieldValueWS> aitMetaFieldValueWSList = new ArrayList<>()
        for(Map.Entry<Integer, List<MetaField>> entry: aitMetaFieldsMap.entrySet()) {
            List<MetaFieldWS> aitMetaFieldWSList = entry.getValue().stream()
                    .map({ dto -> MetaFieldBL.getWS(dto) })
                    .collect(toList())

            for(MetaFieldWS metaFieldWS: aitMetaFieldWSList) {
                if(metaFieldWS.isMandatory()) {
                    MetaFieldValueWS metaFieldValueWS = createMetaFieldValueWS(metaFieldWS,
                            customerUuid, appDirectSubscriptionId, productId)
                    metaFieldValueWS.setGroupId(entry.getKey())
                    if(null != metaFieldValueWS.getValue()) {
                        aitMetaFieldValueWSList.add(metaFieldValueWS)
                    }
                }
            }
        }
        return aitMetaFieldValueWSList.toArray(new MetaFieldValueWS[aitMetaFieldValueWSList.size()])
    }

    private MetaFieldValueWS[] getMetaFieldValues(List<MetaFieldWS> metaFieldWSList, String customerUuid,
                                                  String appDirectSubscriptionId, Integer productId) {

        MetaFieldValueWS[] metaFieldValueWSArray = new MetaFieldValueWS[metaFieldWSList.size()]
        int i = 0
        metaFieldWSList.each {
            MetaFieldWS metaFieldWS ->
                MetaFieldValueWS metaFieldValueWS = createMetaFieldValueWS(metaFieldWS, customerUuid,
                        appDirectSubscriptionId, productId)
                metaFieldValueWSArray[i++] = metaFieldValueWS
        }
        return metaFieldValueWSArray
    }

    String getInternationalDescription(String tableName, Integer languageId, Integer foreignId){
        String content = new InternationalDescriptionDAS().getDescriptionForForeignId(foreignId, languageId, tableName)
        return content
    }

    Integer getLanguageIdByUserId(Integer userId){
        UserWS ws = webServicesSession.getUserWS(userId)
        return ws.getLanguageId();
    }

    Map getCompanyInfo(Integer entityId){

        try {
            CompanyWS company = webServicesSession.getCompanyByEntityId(entityId)
            def map = [:]
            map.put("entityId", company.getId())
            map.put("companyDescription", company.getDescription())
            def hsMobileNumber = company.getMetaFieldByName('Help&SupportMobile')
            def hsEmail = company.getMetaFieldByName('Help&SupportEmail')
            map.put("mobileNo", null != hsMobileNumber ? hsMobileNumber.getValue() : null)
            map.put("email", null != hsEmail ? hsEmail.getValue() : null)
            String address = company.getContact().getAddress1()+" "+company.getContact().getAddress2()+company.getContact().getCity()+" "+company.getContact().getStateProvince()+" "+company.getContact().getPostalCode();
            map.put("address", address)

            return map;
        } catch (Exception e) {
            logger.error("Error get companyInfo by entityId, moving to error table", e)
            throw e
        }
    }

    Map getAllProducts() {
        try {
            ItemDTOEx[] items = webServicesSession.getAllItems()
            def map = [:]
            def list = new ArrayList()
            for (ItemDTOEx ws : items) {
                def item = [:]
                item.put("id", ws.getId())
                item.put("productCode", ws.getNumber())
                item.put("description", ws.getDescription())
                item.put("rate", ws.getPrice())
                list.add(item)
            }
            map.put("items", list)
            return map
        }catch (Exception e){
            logger.error("Error in getAllProducts: {}", e.message, e)
            throw new SessionInternalError("An error occurred while fetching products.", e)
        }
    }



    String getPaymentUrlValue(Integer paymentLogUrlLogId) {
        PaymentUrlLogDTO dto = new PaymentUrlLogDAS().find(paymentLogUrlLogId)
        return dto.getPaymentUrl();
    }

    Map createPaymentUrl(Map map) {
        map.putIfAbsent("paymentUrlType", "upi")
        PaymentUrlLogDTO dto = webServicesSession.createPaymentUrl(map);
        def response = [:]
        response.put('paymentUrlLogId', dto.getId())
        response.put('paymentResponse', dto.getPaymentUrlResponse())
        if (null != dto) {
            String paymentUrl = getPaymentUrlValue(dto.getId());
            if (StringUtils.isNotBlank(paymentUrl)) {
                response.put('intentUrl', paymentUrl)
                if (paymentUrl.contains("pa=")) {
                    String[] urlParts = paymentUrl.split("pa=")
                    if(urlParts.length > 1) {
                        String[] protocolParts = urlParts[1].split("&")
                        String upiProtocol = protocolParts[0]
                        response.put("UPI", upiProtocol)
                    }
                }
            }
        }
        return response
    }

    Map checkPaymentStatus(Integer paymentLogUrlLogId) {
        def map = [:]
        Optional optionalResponse = webServicesSession.executePaymentTask(paymentLogUrlLogId, null, "checkPaymentUrlStatus")
        PaymentResponse response = optionalResponse.orElseThrow({
            logger.error("No payment information was found for the specified transaction ID: {}", paymentLogUrlLogId)
            new SessionInternalError("No payment information was found for the specified transaction ID: " + paymentLogUrlLogId, HttpStatus.SC_NOT_FOUND)
        } as Supplier)

        String status = response.getResponseCode()
        map.put("status", status)

        return map
    }
}
