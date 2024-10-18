package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.event.BulkDownloadEvent;
import com.sapienter.jbilling.server.item.event.BulkUploadEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.api.JbillingDeutscheTelecomAPI;
import grails.plugin.springsecurity.SpringSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;

/**
 * Created by wajeeha on 5/12/18.
 */
@Transactional(propagation = Propagation.REQUIRED)
public class DeutscheTelecomWebServicesSessionSpringBean implements JbillingDeutscheTelecomAPI {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SpringSecurityService springSecurityService;
    private IWebServicesSessionBean webServicesSessionBean;

    public SpringSecurityService getSpringSecurityService() {
        if (springSecurityService == null)
            this.springSecurityService = Context.getBean(Context.Name.SPRING_SECURITY_SERVICE);
        return springSecurityService;
    }

    public void setSpringSecurityService(SpringSecurityService springSecurityService) {
        this.springSecurityService = springSecurityService;
    }

    public void setWebServicesSessionBean(IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long uploadDefaultPrices( String sourceFilePath,
                                     String errorFilePath) throws SessionInternalError {

        try {
            BulkUploadEvent bulkUploadEvent = new BulkUploadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkUploadEvent.UploadType.DEFAULT_PRODUCT);
            EventManager.process(bulkUploadEvent);

            return bulkUploadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start default price upload job", e);
            throw new SessionInternalError("Unable to start default price upload job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long uploadAccountTypePrices( String sourceFilePath,
                                         String errorFilePath) throws SessionInternalError {

        try {
            BulkUploadEvent bulkUploadEvent = new BulkUploadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkUploadEvent.UploadType.ACCOUNT_LEVEL_PRICE);
            EventManager.process(bulkUploadEvent);

            return bulkUploadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start account price upload job", e);
            throw new SessionInternalError("Unable to start account price upload job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long uploadCustomerLevelPrices( String sourceFilePath,
                                           String errorFilePath) throws SessionInternalError {

        try {
            BulkUploadEvent bulkUploadEvent = new BulkUploadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkUploadEvent.UploadType.CUSTOMER_PRICE);
            EventManager.process(bulkUploadEvent);

            return bulkUploadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start customer level price upload job", e);
            throw new SessionInternalError("Unable to start customer level price upload job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long uploadPlanPrices( String sourceFilePath,
                                  String errorFilePath) throws SessionInternalError {

        try {
            BulkUploadEvent bulkUploadEvent = new BulkUploadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkUploadEvent.UploadType.PLAN_PRICE);
            EventManager.process(bulkUploadEvent);

            return bulkUploadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start plan price upload job", e);
            throw new SessionInternalError("Unable to start plan price upload job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadDefaultPrices( String sourceFilePath,
                                     String errorFilePath) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.DEFAULT_PRODUCT);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start default price download job", e);
            throw new SessionInternalError("Unable to start default price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadAccountLevelPrices( String sourceFilePath,
                                       String errorFilePath) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.ACCOUNT_LEVEL_PRICE);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start default price download job", e);
            throw new SessionInternalError("Unable to start default price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadCustomerLevelPrices( String sourceFilePath,
                                            String errorFilePath) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.CUSTOMER_PRICE);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start default price download job", e);
            throw new SessionInternalError("Unable to start default price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadPlans( String sourceFilePath,
                                        String errorFilePath) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.PLANS);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start default price download job", e);
            throw new SessionInternalError("Unable to start default price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadIndividualDefaultPrice( String sourceFilePath,
                                        String errorFilePath, String productCode) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.DEFAULT_PRODUCT, productCode);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start individual default price download job", e);
            throw new SessionInternalError("Unable to start individual default price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadIndividualAccountLevelPrice( String sourceFilePath,
                                            String errorFilePath, String accountId) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.ACCOUNT_LEVEL_PRICE, accountId);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start individual account level price download job", e);
            throw new SessionInternalError("Unable to start individual account level price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadIndividualCustomerLevelPrice( String sourceFilePath,
                                             String errorFilePath, String accountIdentificationCode) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.CUSTOMER_PRICE, accountIdentificationCode);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start individual customer level price download job", e);
            throw new SessionInternalError("Unable to start individual customer level price download job", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Long downloadIndividualPlan( String sourceFilePath,
                               String errorFilePath, String planNumber) throws SessionInternalError {

        try {
            BulkDownloadEvent bulkDownloadEvent = new BulkDownloadEvent(sourceFilePath, errorFilePath, webServicesSessionBean.getCallerCompanyId(),
                    webServicesSessionBean.getCallerId(), BulkDownloadEvent.DownloadType.PLANS, planNumber);
            EventManager.process(bulkDownloadEvent);

            return bulkDownloadEvent.getExecutionId();
        } catch (Exception e) {
            logger.error("Unable to start individual plan price download job", e);
            throw new SessionInternalError("Unable to start individual plan price download job", e);
        }
    }

    public Integer getCustomerByMetaField(String customerIdentificationCode, String metaFieldName){
        Integer customerBaseUserId = new UserDAS().findUserByMetaFieldNameAndValue(metaFieldName,
                customerIdentificationCode, webServicesSessionBean.getCallerCompanyId());
        return customerBaseUserId;
    }
}