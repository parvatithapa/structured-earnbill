package com.sapienter.jbilling.server.dt.reserve.validator

import com.sapienter.jbilling.exception.DtReserveInstanceException
import com.sapienter.jbilling.server.integration.common.utility.DateUtility
import com.sapienter.jbilling.server.item.PlanWS
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.util.DTOFactory
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import com.sapienter.jbilling.server.util.search.BasicFilter
import com.sapienter.jbilling.server.util.search.SearchCriteria
import grails.util.Holders
import jbilling.DtReserveInstanceService

import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

import static com.sapienter.jbilling.server.util.Constants.DeutscheTelekom.EXTERNAL_ACCOUNT_IDENTIFIER;

class DtReserveInstanceValidator {
    WebServicesSessionSpringBean webServicesSession;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    def messageSource = Holders.getGrailsApplication().getMainContext().getBean("messageSource")

     Integer validateSubscriptionId(String externalAccountId, Locale locale) throws DtReserveInstanceException {
            List<CustomerDTO> customerList  = new UserBL().getUserByCustomerMetaField(externalAccountId,
                    EXTERNAL_ACCOUNT_IDENTIFIER, webServicesSession.getCallerCompanyId());

            if (null == customerList || customerList.size() > 1) {
                String errorMesssage = messageSource.getMessage('validation.error.invalid.subscriptionId', null, messageSource.getMessage(
                        'validation.error.invalid.subscriptionId', null, 'Invalid subscription id  received.', locale), locale)
                throw new DtReserveInstanceException(404, errorMesssage)

            }

            return UserBL.getWS(DTOFactory.getUserDTOEx(customerList.get(0).getBaseUser())).getUserId();
    }

    Locale validateLocale(String localeStr){
        if(localeStr != null && localeStr.equalsIgnoreCase("de")){
            return Locale.GERMAN
        }else{
            return Locale.ENGLISH
        }
    }

    PlanWS validatePlanId(Integer planId, Locale locale) throws DtReserveInstanceException{
        PlanWS plan =  webServicesSession.getPlanWS(Integer.valueOf(planId));
        if (null == plan) {
            String errorMesssage = messageSource.getMessage('validation.error.invalid.planId', null, messageSource.getMessage(
                    'validation.error.invalid.planId', null, 'Invalid plan id  received.', locale), locale)
            throw new DtReserveInstanceException(404, errorMesssage)
        }
        return plan;
    }

    SearchCriteria validateSearchCriteria(Integer pageSize, Integer pageNumber, String sortField, String sortOrder,String filter, Locale locale, String os, String ram, String cpu) throws DtReserveInstanceException{
        String errorMesssage = messageSource.getMessage('validation.error.invalid.parameter', null, messageSource.getMessage(
                'validation.error.invalid.parameter', null, 'Invalid parameter received in request.', locale), locale)

        try {
            SearchCriteria criteria = new SearchCriteria()
            BasicFilter[] filters = new BasicFilter[4];
            for(int i = 0; i<4; i++){
                filters[i] = new BasicFilter();
            }
            filters[0].setStringValue(filter)
            filters[1].setStringValue(os)
            filters[2].setStringValue(ram)
            filters[3].setStringValue(cpu)
            criteria.setFilters(filters)
            criteria.setMax(pageSize != null ? pageSize : 10)
            criteria.setOffset(pageNumber != null ? pageNumber : 1)
            criteria.setSort(sortField != null ? sortField.toLowerCase() : DtReserveInstanceService.SORT_FIELD_ENGLISH_PLAN_NAME)
            criteria.setDirection(sortOrder != null ? SearchCriteria.SortDirection.valueOf(sortOrder.toUpperCase()) : SearchCriteria.SortDirection.ASC)

            if ((pageSize !=null && pageSize > 20 && pageSize > 0 )||
                    (pageNumber != null && pageNumber < 1) ||
                    (!criteria.getDirection().equals(SearchCriteria.SortDirection.ASC) &&
                    !criteria.getDirection().equals(SearchCriteria.SortDirection.DESC)) ||
                    (!criteria.getSort().equals(DtReserveInstanceService.SORT_FIELD_ENGLISH_PLAN_NAME) &&
                            !criteria.getSort().equals(DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE) &&
                            !criteria.getSort().equals(DtReserveInstanceService.SORT_FIELD_PRICE))) {
                throw new DtReserveInstanceException(400, errorMesssage)
            }
            return criteria
        }
        catch (Exception e){
            throw new DtReserveInstanceException(400, errorMesssage)
        }
    }


    void validateSubscriptionIdToUpgrade(Integer orderUserId, Integer userId, Locale locale) throws DtReserveInstanceException{
        if (!userId.equals(orderUserId)) {
            String errorMesssage = messageSource.getMessage('validation.error.invalid.orderId', null, messageSource.getMessage(
                    'validation.error.invalid.orderId', null, 'The order ID does not exist', locale), locale)

            throw new DtReserveInstanceException(404, errorMesssage)
        }
    }

    void validateUpgradePendingStatus(OrderWS order, Locale locale) throws DtReserveInstanceException{
        if(getUpgradedToValue(order) != null) {
            Date newActiveSince = new Date(order.getActiveUntil().getTime() + TimeUnit.DAYS.toMillis( 1 ));
            String activeSinceStr = [sdf.format(newActiveSince)]
            String errorMesssage = messageSource.getMessage('validation.error.upgrade.pending', [activeSinceStr] as String[],
                    messageSource.getMessage('validation.error.upgrade.pending', [activeSinceStr] as String[],
                            'Request rejected as an Upgrade is pending from ${sdf.format(newActiveSince)}', locale), locale)

            throw new DtReserveInstanceException(412, errorMesssage)
        }
    }

    private Integer getUpgradedToValue(OrderWS orderWS){
        for(MetaFieldValueWS metaFieldValueWS : orderWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals("Upgraded to")){
                return metaFieldValueWS.getIntegerValue()
            }
        }

        return null
    }

    OrderWS validateOrder(Integer orderId, Locale locale) throws DtReserveInstanceException{

        OrderWS order = webServicesSession.getOrder(orderId)
        if(order==null){
            String errorMesssage = messageSource.getMessage('validation.error.invalid.orderId', null, messageSource.getMessage(
                    'validation.error.invalid.orderId', null, 'The order ID does not exist', locale), locale)
            throw new DtReserveInstanceException(404, errorMesssage)
        }
        return order

    }

    Date validateActiveSince(Date date, Locale locale) throws DtReserveInstanceException {

        if(date == null){
            return new Date()
        }
        Calendar cal = Calendar.getInstance()
        Calendar limit = Calendar.getInstance()
        cal.setTime(date)
        limit.setTime(new Date())
        limit.add(Calendar.MONTH,1)
        limit.add(Calendar.DATE,-1)
        if(cal.after(limit)){
            String errorMesssage = messageSource.getMessage('validation.error.reserve.purchase.effective.date', null, messageSource.getMessage(
                    'validation.error.reserve.purchase.effective.date', null, 'Effective date later than one month', locale), locale)
            throw new DtReserveInstanceException(400, errorMesssage);
        }
        return date
    }

    Date validateUpgradeSince(Date newDate, Date existingDate, Locale locale) throws DtReserveInstanceException {

        if(newDate == null){
            newDate = new Date()
        }
        Calendar cal = Calendar.getInstance()
        cal.setTime(newDate)
        DateUtility.setTimeToStartOfDay(cal)

        Calendar cal2 = Calendar.getInstance()
        cal2.setTime(existingDate)
        DateUtility.setTimeToStartOfDay(cal2)
        cal2.add(Calendar.DATE,1)

        if(cal.compareTo(cal2)<=0){
           String errorMesssage = messageSource.getMessage('validation.error.upgrade.date.before.intital.date', null, messageSource.getMessage(
                'validation.error.upgrade.date.before.intital.date', null, 'Upgrade date on or before initial date', locale), locale)

            throw new DtReserveInstanceException(400, errorMesssage);
        }
    }

    public void upgradeAllowed(PlanWS from, PlanWS to,Locale locale) {
        DtReserveInstanceService dtr = new DtReserveInstanceService();
        String fromPaymentOption = dtr.getPaymentOption(from);
        String toPaymentOption = dtr.getPaymentOption(to);
        Integer fromDuration = dtr.getDuration(from);
        Integer toDuration = dtr.getDuration(to);
        if(fromPaymentOption == "UPFRONT" || toPaymentOption == "UPFRONT"){
            String errorMesssage = messageSource.getMessage('validation.error.upgrade.upfront', null, messageSource.getMessage(
                    'validation.error.upgrade.upfront', null, 'Upfront payment order cannot be upgraded.', locale), locale)

            throw new DtReserveInstanceException(400, errorMesssage);
        }
        if(fromDuration>toDuration){
            String errorMesssage = messageSource.getMessage('validation.error.upgrade.Lesser.period', null, messageSource.getMessage(
                    'validation.error.upgrade.Lesser.period', null,
                    'Cannot be upgraded to a lesser period plan.', locale), locale)

            throw new DtReserveInstanceException(400, errorMesssage)
        }
        if(from.getId()==to.getId()){
            String errorMesssage = messageSource.getMessage('validation.error.upgrade.same.planId', null,
                    messageSource.getMessage('validation.error.upgrade.same.planId', null,
                            'Upgrade plan ID is same as the existing plan ID', locale), locale)


            throw new DtReserveInstanceException(400, errorMesssage);
        }
    }

}
