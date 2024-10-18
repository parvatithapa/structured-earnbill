package com.sapienter.jbilling.server.payment.ignition;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.StandardBank.PaymentStandardBankTask;
import com.sapienter.jbilling.server.payment.tasks.absa.ABSAPaymentManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Taimoor Choudhary on 3/14/18.
 */
public class IgnitionPaymentManager {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void processManualPayment(int entityId, String upperLevelTrackingDays, String middleLevelTrackingDays,
                                            String lowerLevelTrackingDays, boolean isNAEDOEnabled, List<Date> holidays,
                                            PaymentWS paymentWS, int invoiceId, IWebServicesSessionBean webServicesSessionBean) throws SessionInternalError {

        boolean updatePayment = processPayment(entityId, upperLevelTrackingDays, middleLevelTrackingDays,
                lowerLevelTrackingDays, isNAEDOEnabled, holidays, paymentWS, invoiceId, null);

        if(updatePayment) {

            logger.debug("Updating payment with ID: " + paymentWS.getId());

            webServicesSessionBean.updatePayment(paymentWS);
        }
    }

    public static void processScheduledPayment(int entityId, String upperLevelTrackingDays, String middleLevelTrackingDays,
                                               String lowerLevelTrackingDays, boolean isNAEDOEnabled, List<Date> holidays,
                                               PaymentWS paymentWS, Integer orderId, Map<String, Map<String, ServiceProfile>> allServiceProfiles) throws SessionInternalError {

        processPayment(entityId, upperLevelTrackingDays, middleLevelTrackingDays,
                lowerLevelTrackingDays, isNAEDOEnabled, holidays, paymentWS, orderId, allServiceProfiles);
    }

    private static boolean processPayment(int entityId, String upperLevelTrackingDays, String middleLevelTrackingDays,
                                          String lowerLevelTrackingDays, boolean isNAEDOEnabled, List<Date> holidays,
                                          PaymentWS paymentWS, Integer orderId, Map<String, Map<String, ServiceProfile>> allServiceProfiles) throws SessionInternalError {
        try {

            boolean updatePayment = false;
            String userReference = null;
            ServiceProfile serviceProfile;
            Integer[] orderIds = new Integer[1];
            Map<String, ServiceProfile> serviceProfilesForBrand;

            UserBL userBL = new UserBL(paymentWS.getUserId());
            UserWS userWs = userBL.getUserWS();

            if(paymentWS.getPaymentId() != null){

                String serviceProfileName = null;
                String bankName = null;
                PaymentBL bl = new PaymentBL(paymentWS.getPaymentId());
                PaymentWS refund =  PaymentBL.getWS(bl.getDTOEx(userWs.getLanguageId()));

                for(MetaFieldValueWS metaFieldValueWS : refund.getMetaFields()){
                    if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_USER_REFERENCE)){
                        userReference = metaFieldValueWS.getStringValue();

                        if(userReference!=null){
                            userReference = userReference.substring(10);
                        }
                    }
                    if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_SENT_ON)){
                        serviceProfileName = metaFieldValueWS.getStringValue();
                    }
                }

                for(MetaFieldValueWS metaFieldValueWS : refund.getPaymentInstruments().get(0).getMetaFields()){
                    if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.PAYMENT_BANK_NAME)){
                        bankName = metaFieldValueWS.getStringValue();
                    }
                }

                if(serviceProfileName == null){
                    logger.error("Unable to find service profile from refund payment: " + paymentWS.getPaymentId());
                    throw new PluggableTaskException("Unable to find service profile from refund payment: " + paymentWS.getPaymentId());
                }

                // Get Service Profiles for Brand
                serviceProfilesForBrand = IgnitionUtility.getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME,
                        ServiceProfile.Names.SERVICE_PROFILE, serviceProfileName, entityId);

                // Get Service Profile to use
                serviceProfile = serviceProfilesForBrand.get(bankName);
            }else {
                orderIds[0] = orderId;

                // Get Brand and Service Provider name
                Pair<String, String> brandAndServiceProvider = IgnitionUtility.getBrandAndServiceProviderFromOrder(entityId, userWs, orderIds[0]);

                if(!MapUtils.isEmpty(allServiceProfiles)) {
                    // Get Service Profile for given Brand & Service Provider
                    serviceProfile = IgnitionUtility.getServiceProfileForBrand(brandAndServiceProvider, allServiceProfiles);
                    // Get all Service Profiles for given Brand
                    serviceProfilesForBrand = allServiceProfiles.get(brandAndServiceProvider.getLeft());
                }else{
                    // Get Service Profiles for Brand
                    serviceProfilesForBrand = IgnitionUtility.getServiceProfilesForGivenColumn(IgnitionConstants.DATA_TABLE_NAME,
                            ServiceProfile.Names.BRAND_NAME, brandAndServiceProvider.getLeft(), entityId);
                    // Get Service Profile to use as per the Service Provide (Bank)
                    serviceProfile = serviceProfilesForBrand.get(brandAndServiceProvider.getRight());
                }
            }

            if (isNAEDOEnabled && isNaedoPaymentRequired(userWs)) {

                // Get NAEDO Service Profile
                serviceProfile = serviceProfilesForBrand.get(IgnitionConstants.SERVICE_PROVIDER_ABSA_NAEDO);

                if (serviceProfile == null) {
                    logger.error("Unable to find service profile for NAEDO payment for User: " + userWs.getId());
                    throw new SessionInternalError("Can't process payment Request, Unable to find service profile for NAEDO payment for User: " + userWs.getId());
                }

                IgnitionConstants.NAEDOWorkflowType naedoType = IgnitionConstants.NAEDOWorkflowType.NONE;

                // Get NAEDO Workflow type from UserWS
                for (MetaFieldValueWS metaFieldValueWS : userWs.getMetaFields()) {
                    if (metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_NAEDO_TYPE)) {
                        naedoType = IgnitionConstants.NAEDOWorkflowType.getNAEDOWorkflowType(metaFieldValueWS.getStringValue());
                    }
                }

                if (naedoType.equalsName(IgnitionConstants.NAEDOWorkflowType.NONE.toString())) {
                    logger.error("No NAEDO Workflow type set for a User in NAEDO Status: " + userWs.getId());
                    throw new SessionInternalError("Cannot process event: No NAEDO Workflow type set for a User in NAEDO Status: " + userWs.getId());
                }

                String selectedTrackingDay = IgnitionConstants.NAEDOWorkflowType.UPPER.equalsName(naedoType.toString()) ? upperLevelTrackingDays :
                        IgnitionConstants.NAEDOWorkflowType.MIDDLE.equalsName(naedoType.toString()) ? middleLevelTrackingDays : lowerLevelTrackingDays;

                IgnitionConstants.NAEDOPaymentTracking naedoPaymentTracking = IgnitionConstants.NAEDOPaymentTracking.getNAEDOPaymentTracking(selectedTrackingDay);

                if (naedoPaymentTracking == null) {
                    logger.error("NAEDO payment Tracking value: %s, for incoming NAEDO Type: %s is not supported.", naedoPaymentTracking, selectedTrackingDay);
                    throw new SessionInternalError(String.format("Cannot process event: NAEDO payment Tracking value: %s, for incoming NAEDO Type: %s is not supported", naedoPaymentTracking, selectedTrackingDay));
                }

                logger.debug("Sending NAEDO Payment Request on ABSA for payment ID: " + paymentWS.getId());

                ABSAPaymentManager paymentABSATask = new ABSAPaymentManager(entityId, holidays, serviceProfile);
                paymentABSATask.requestNAEDOPayment(paymentWS, orderIds, userWs, userReference, naedoPaymentTracking, naedoType);

                logger.debug("Payment NAEDO Request sent for ID: " + paymentWS.getId());

                updatePayment = true;
            } else {

                if (serviceProfile == null) {
                    logger.debug("Service Profile not found for User %s for given brand", userWs.getId());

                    if (serviceProfilesForBrand.isEmpty()) {
                        logger.error("Cannot process payment: No Service Profiles found for the given brand");
                        throw new SessionInternalError("Cannot process payment: No Service Profiles found for the given brand");
                    }

                    for (Map.Entry<String, ServiceProfile> serviceProfileEntrySet : serviceProfilesForBrand.entrySet()) {
                        serviceProfile = serviceProfileEntrySet.getValue();
                        break;
                    }

                    logger.debug("New selected profile is for Bank: " + serviceProfile.getServiceProvider());
                }

                if (!serviceProfile.getServiceProvider().equals(IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK)) {

                    logger.debug("Sending Payment Request on ABSA for payment ID: " + paymentWS.getId());

                    ABSAPaymentManager paymentABSATask = new ABSAPaymentManager(entityId, holidays, serviceProfile);
                    paymentABSATask.requestPayment(paymentWS, orderIds, userWs, userReference);

                    logger.debug("Payment Request sent for ID: " + paymentWS.getId());

                    updatePayment = true;
                } else {

                    logger.debug("Sending payment call to Standard Bank Task for Payment Id : " + paymentWS.getId());

                    PaymentStandardBankTask paymentStandardBankTask = new PaymentStandardBankTask(entityId, holidays, serviceProfile);
                    paymentStandardBankTask.requestPayment(paymentWS, orderIds, userWs, userReference);

                    logger.debug("Payment Request sent for ID: " + paymentWS.getId());

                    updatePayment = true;
                }
            }

            return updatePayment;
        }catch(Exception exception){
            throw new SessionInternalError(exception);
        }
    }

    private static boolean isNaedoPaymentRequired(UserWS userWS){

        boolean isNaedo = false;

        for(MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(IgnitionConstants.USER_IN_NAEDO)){
                isNaedo = (metaFieldValueWS.getBooleanValue() == null ? false : metaFieldValueWS.getBooleanValue());
                break;
            }
        }

        return isNaedo;
    }
}
