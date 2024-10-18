package jbilling

import com.sapienter.jbilling.DtOrderPlanWS
import com.sapienter.jbilling.DtReserveInstanceCache
import com.sapienter.jbilling.PaginatedRecordWS
import com.sapienter.jbilling.catalogue.DtPlanWS
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.exception.DtReserveInstanceException
import com.sapienter.jbilling.server.dt.reserve.mapper.DtReserveInstanceWSMapper
import com.sapienter.jbilling.server.dt.reserve.validator.DtReserveInstanceValidator
import com.sapienter.jbilling.server.item.ItemBL
import com.sapienter.jbilling.server.item.PlanBL
import com.sapienter.jbilling.server.item.PlanItemWS
import com.sapienter.jbilling.server.item.PlanWS
import com.sapienter.jbilling.server.item.db.ItemDAS
import com.sapienter.jbilling.server.item.db.ItemDTO
import com.sapienter.jbilling.server.item.db.PlanDAS
import com.sapienter.jbilling.server.item.db.PlanDTO
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.order.*
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO
import com.sapienter.jbilling.server.system.event.EventManager
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.usagePool.event.ReservedUpgradeEvent
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.InternationalDescriptionWS
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import com.sapienter.jbilling.server.util.search.SearchCriteria
import com.sapienter.jbilling.subscribe.DtCancelValidationStatusResponse
import com.sapienter.jbilling.subscribe.DtSubscribeRequestPayload
import grails.util.Holders
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import java.lang.invoke.MethodHandles
import java.text.SimpleDateFormat

@Transactional(propagation = Propagation.REQUIRED)
class DtReserveInstanceService {
    WebServicesSessionSpringBean webServicesSession
    DtReserveInstanceWSMapper dtReserveInstanceWSMapper
    DtReserveInstanceValidator dtReserveInstanceValidator
   // public static String SORT_FIELD_DESCRIPTION = "description"
    public static final String categoryKey = "Categories";
    public static String SORT_FIELD_ENGLISH_PLAN_NAME = "enplanname"
    public static String SORT_FIELD_PRICE = "price"
    public static String SORT_FIELD_ACTIVE_SINCE = "activesince"
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    def messageSource = Holders.getGrailsApplication().getMainContext().getBean("messageSource")


    PaginatedRecordWS<DtPlanWS> listReservePlanCatalogue(SearchCriteria criteria, Locale locale) {

        List<DtPlanWS> reservedPlanList = getPlans();

        reservedPlanList = dtReserveInstanceWSMapper.filterOnCategory(reservedPlanList, criteria.getFilters())
        doSorting(criteria.getSort(), criteria.getDirection(), reservedPlanList)            //Do Sorting
        return new PaginatedRecordWS<DtPlanWS>(criteria.getOffset(), criteria.getMax(), reservedPlanList)
        //Do Pagination
    }

    private void doSorting(String sortField, SearchCriteria.SortDirection sortDirection, List<DtPlanWS> reservedPlanList) {
        if (sortField.equals(SORT_FIELD_ENGLISH_PLAN_NAME)) {
            Collections.sort(reservedPlanList, new Comparator<DtPlanWS>() {
                int compare(DtPlanWS a, DtPlanWS b) {
                    return a.getEnPlanName().compareTo(b.getEnPlanName())
                }
            })
        } else if (sortField.equals(SORT_FIELD_PRICE)) {
            Collections.sort(reservedPlanList, new Comparator<DtPlanWS>() {
                int compare(DtPlanWS a, DtPlanWS b) {
                    return a.getPlanPrice().compareTo(b.getPlanPrice())
                }
            })
        } else if (sortField.equals(SORT_FIELD_ACTIVE_SINCE)) {
            Collections.sort(reservedPlanList, new Comparator<DtPlanWS>() {
                int compare(DtPlanWS a, DtPlanWS b) {
                    return a.getActiveSince().compareTo(b.getActiveSince())
                }
            })
        }
        if (sortDirection == SearchCriteria.SortDirection.DESC) {
            Collections.reverse(reservedPlanList)
        }
    }

    PaginatedRecordWS<DtPlanWS> listReserveSubscription(String externalAccountId, SearchCriteria criteria, Locale locale) throws DtReserveInstanceException {
        Integer userId = dtReserveInstanceValidator.validateSubscriptionId(externalAccountId,locale)
        logger.debug("UserId fetched by external accountId is ${userId}")
        String cacheKey = "${DtReserveInstanceCache.RESERVE_CACHE_KEY}${webServicesSession.getCallerCompanyId()}-UserID-${userId}"
        List<DtPlanWS> userReservedPlanList = DtReserveInstanceCache.getReservedInstanceCache(cacheKey)

        if (userReservedPlanList == null) {
            logger.debug("cache not found for cache key ${cacheKey}")

            userReservedPlanList = new ArrayList<>()
            List<OrderWS> orderWSlist = webServicesSession.getUserOrdersPage(userId, null, null)
            ItemDAS itemDAS = new ItemDAS()
            PlanDAS planDAS = new PlanDAS()

            for (OrderWS orderWS : orderWSlist) {
                for (OrderLineWS orderLineWS : orderWS.getOrderLines()) {
                    if (itemDAS.isPlan(orderLineWS.getItemId())) {
                        PlanDTO planDTO = planDAS.findPlanByItemId(orderLineWS.getItemId())
                        PlanWS planWS = new PlanBL(planDTO).getWS()
                        DtOrderPlanWS dtOrderPlanWS = new DtOrderPlanWS()

                        dtOrderPlanWS.setOrderId(orderWS.getId())
                        dtOrderPlanWS.setDuration(getDuration(planWS))
                        dtOrderPlanWS.setPaymentMode(getPaymentOption(planWS))
                        if (dtOrderPlanWS.getDuration() != null && dtOrderPlanWS.getDuration() != 0 &&
                                dtOrderPlanWS.getPaymentMode() != null) {
                            dtOrderPlanWS = (DtOrderPlanWS) dtReserveInstanceWSMapper.mapPlanToDtPlan(planWS, dtOrderPlanWS)

                            if(dtOrderPlanWS != null) {
                                dtOrderPlanWS.setActiveSince(orderWS.getActiveSince() == null ? "" : sdf.format(orderWS.getActiveSince()))
                                dtOrderPlanWS.setActiveUntil(orderWS.getActiveUntil() == null ? "" : sdf.format(orderWS.getActiveUntil()))
                                setOrderStatus(orderWS, dtOrderPlanWS)
                                userReservedPlanList.add(dtOrderPlanWS)
                            }
                        }
                        break;
                    }
                }
            }
            DtReserveInstanceCache.setReservedInstanceCache(cacheKey, userReservedPlanList)
        }

        userReservedPlanList = dtReserveInstanceWSMapper.filterOnCategory(userReservedPlanList, criteria.getFilters())
        doSorting(criteria.getSort(), criteria.getDirection(), userReservedPlanList)        //do sorting
        return new PaginatedRecordWS<DtPlanWS>(criteria.getOffset(), criteria.getMax(), userReservedPlanList)
        //pagination
    }

    @Transactional(propagation = Propagation.REQUIRED)
    DtOrderPlanWS upgradeReservedPlan(String orderId, DtSubscribeRequestPayload upgradeInfo, Locale locale) throws DtReserveInstanceException {
        OrderWS order = dtReserveInstanceValidator.validateOrder(Integer.valueOf(orderId),locale)
        Integer userId = dtReserveInstanceValidator.validateSubscriptionId(upgradeInfo.getSubscriptionId(), locale);

        dtReserveInstanceValidator.validateSubscriptionIdToUpgrade(order.getUserId(), userId, locale)
        dtReserveInstanceValidator.validateUpgradePendingStatus(order, locale)

        Integer existingPlanId = order.getOrderLines()[0].getItemId();
        PlanDTO planDto = new PlanDAS().findPlanByItemId(existingPlanId);
        PlanWS existingPlan = webServicesSession.getPlanWS(planDto.getId());
        PlanWS newPlan = dtReserveInstanceValidator.validatePlanId(Integer.valueOf(upgradeInfo.getPlanId()), locale)
        dtReserveInstanceValidator.upgradeAllowed(existingPlan, newPlan, locale)
        Date until = getActiveUntil(getDuration(newPlan),order.getActiveSince());
        Calendar cal = Calendar.getInstance()

        cal.setTime(dtReserveInstanceValidator.validateActiveSince(upgradeInfo.getActiveSince(), locale));
        dtReserveInstanceValidator.validateUpgradeSince(upgradeInfo.getActiveSince(), order.getActiveSince(), locale)
        cal.add(Calendar.DATE, -1);
        order.setActiveUntil(cal.getTime());

        DtOrderPlanWS newOrder = subscribeReservedPlan(upgradeInfo, until, locale);
        setUpgradedToValue(order, newOrder.getOrderId())
        webServicesSession.createUpdateOrder(order, new OrderChangeWS[0]);

        BigDecimal pendingAdjustment = BigDecimal.ZERO

        for(MetaFieldValueWS metaFieldValueWS : order.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(com.sapienter.jbilling.server.integration.Constants.ADJUSTMENT)){
                pendingAdjustment =  metaFieldValueWS.getDecimalValueAsDecimal()
                break
            }
        }

        ReservedUpgradeEvent event = new ReservedUpgradeEvent()
        event.setEntityId(new UserBL(userId).getEntityId())
        event.setUserId(userId)
        event.setExistingOrder(order)
        event.setNewOrderId(newOrder.getOrderId())
        event.setInitialPriceReported(getRate(existingPlan))
        event.setPendingAdjustment(pendingAdjustment)
        EventManager.process(event)

        return newOrder;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    DtOrderPlanWS subscribeReservedPlan(DtSubscribeRequestPayload upgradeInfo, Date activeUntil, Locale locale) throws DtReserveInstanceException {
        Integer userId = dtReserveInstanceValidator.validateSubscriptionId(upgradeInfo.getSubscriptionId(), locale);
        logger.debug("UserId fetched by external accountId is ${userId}")
        Calendar activeSince = Calendar.getInstance();
        activeSince.setTime(dtReserveInstanceValidator.validateActiveSince(upgradeInfo.getActiveSince(), locale));
        PlanWS planToOrder = dtReserveInstanceValidator.validatePlanId(Integer.valueOf(upgradeInfo.getPlanId()), locale)
        Integer duration = getDuration(planToOrder);
        if (activeUntil == null) {
            activeUntil = getActiveUntil(duration, activeSince.getTime());
        }
        OrderWS newOrder = getUserSubscriptionToPlan(activeSince.getTime(), activeUntil, userId, Constants.ORDER_BILLING_PRE_PAID,
                getOrCreateMonthlyOrderPeriod(), Integer.valueOf(upgradeInfo.getPlanId()), new Integer(1));
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(newOrder, getOrCreateOrderChangeStatusApply());
        orderChanges[0].setStartDate(activeSince.getTime());
        orderChanges[0].setAppliedManually(new Integer(1))
        Integer orderId = webServicesSession.createOrder(newOrder, orderChanges)
        OrderWS orderWS = webServicesSession.getOrder(orderId)
            MetaFieldValueWS[] metafields = orderWS.getMetaFields();
            for (MetaFieldValueWS metafield : metafields) {
                if (metafield.getFieldName().equals(com.sapienter.jbilling.server.integration.Constants.ADJUSTMENT)) {
                    metafield.setBigDecimalValue(BigDecimal.ZERO);
                    break;
                }
            }
        webServicesSession.createUpdateOrder(orderWS, new OrderChangeWS[0])
        DtOrderPlanWS dtOrderPlanWS = new DtOrderPlanWS();

        dtOrderPlanWS.setOrderId(orderWS.getId())
        dtOrderPlanWS.setDuration(duration)
        dtOrderPlanWS.setPaymentMode(getPaymentOption(planToOrder))
        dtOrderPlanWS = (DtOrderPlanWS) dtReserveInstanceWSMapper.mapPlanToDtPlan(planToOrder, dtOrderPlanWS)

        dtOrderPlanWS.setActiveSince(orderWS.getActiveSince() == null ? "" : sdf.format(orderWS.getActiveSince()))
        dtOrderPlanWS.setActiveUntil(orderWS.getActiveUntil() == null ? "" : sdf.format(orderWS.getActiveUntil()))
        setOrderStatus(orderWS, dtOrderPlanWS)

        return dtOrderPlanWS;
    }

    DtCancelValidationStatusResponse validateCancellation(String subscriptionId, Locale locale) throws DtReserveInstanceException{
        SearchCriteria criteria = new SearchCriteria()
        String reason = messageSource.getMessage('validation.cancellation.allowed', null, messageSource.getMessage(
                'validation.cancellation.allowed', null, 'Subscription cancellation can be performed.', locale), locale)

        criteria.setMax(Integer.MAX_VALUE)
        DtCancelValidationStatusResponse dtCancelStatus = new DtCancelValidationStatusResponse()
        dtCancelStatus.setCancellationAllowed(true)
        dtCancelStatus.setReason(reason)
        PaginatedRecordWS<DtOrderPlanWS> subscriptionPlanlist =  listReserveSubscription(subscriptionId, criteria, locale)
        Date initialDate = new Date(0L) //to track max active until date of a order
        Date activeUntil = new Date(0L)

        for (DtOrderPlanWS dtOrderPlanWS : subscriptionPlanlist.getRecords()) {
            if ( dtOrderPlanWS.getOrderStatus().equals(Constants.ORDER_STATUS_PENDING) || dtOrderPlanWS.getOrderStatus().equals("Active")) {
                dtCancelStatus.setCancellationAllowed(false)

                if( !dtOrderPlanWS.getActiveUntil().isEmpty() && !(dtOrderPlanWS.getActiveUntil() == null) &&
                        sdf.parse(dtOrderPlanWS.getActiveUntil()).after(activeUntil)){
                    activeUntil = sdf.parse(dtOrderPlanWS.getActiveUntil())
                    reason = messageSource.getMessage('validation.error.upgrade.pending', [dtOrderPlanWS.getActiveUntil()]  as String[], messageSource.getMessage(
                            'validation.error.upgrade.pending', [dtOrderPlanWS.getActiveUntil()] as String[],
                            'Active reserve instance is available for this subscription so it Cannot be cancelled until ${dtOrderPlanWS.getActiveUntil()}',
                            locale), locale)

                    dtCancelStatus.setReason(reason)
                }

                if(!activeUntil.after(initialDate)) {
                    reason = messageSource.getMessage('validation.error.cancellation.not.allowed', null, messageSource.getMessage(
                            'validation.error.cancellation.not.allowed', null,
                            'Subscription Cannot be cancelled as there are active reserve instance subscription(s)', locale), locale)

                    dtCancelStatus.setReason(reason)
                }
            }
        }

        return dtCancelStatus
    }

    Map<String,Set<String>> getCategories(){
        List<DtPlanWS> reservedPlanList = getPlans();
        Set<String> categories = new HashSet<>();
        for(DtPlanWS plan : reservedPlanList){
            categories.add(plan.getProductCategory());
        }
        Map<String,Set<String>> categoryMap = new HashMap<>()
        categoryMap.put(categoryKey,categories);
        return categoryMap
    }

    private void setOrderStatus(OrderWS orderWS, DtOrderPlanWS dtOrderPlanWS) {
        if (orderWS.getActiveSince() != null && orderWS.getActiveSince().
                after(TimezoneHelper.currentDateForTimezone(webServicesSession.getCompany().getTimezone()))) {
            dtOrderPlanWS.setOrderStatus(Constants.ORDER_STATUS_PENDING)
        } else {
            dtOrderPlanWS.setOrderStatus(orderWS.getOrderStatusWS().getDescription())
        }
    }

    private OrderWS getUserSubscriptionToPlan(Date since, Date until, Integer userId,
                                              Integer billingType, Integer orderPeriodID,
                                              Integer plansItemId, Integer planQuantity) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingType);
        order.setPeriod(orderPeriodID);
        PlanWS plan = webServicesSession.getPlanWS(plansItemId);
        PlanItemWS[] planItems = plan.getPlanItems();
        Integer currencyId = planItems[0].getModel().getCurrencyId();
        order.setCurrencyId(currencyId);
        order.setActiveSince(since);
        if (until != null) {
            order.setActiveUntil(until);
        }
        order.setProrateFlag(Boolean.TRUE);
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(planQuantity);
        line.setDescription("Order line for plan subscription");
        line.setItemId(plan.getItemId());
        line.setUseItem(true);
        line.setPrice(webServicesSession.getPlanWS(plansItemId).getPlanItems().get(0).getModel().getRateAsDecimal());
        OrderLineWS[] lines = new OrderLineWS[1];
        lines[0] = line;
        order.setOrderLines(lines);
        return order;
    }

    private Integer getOrCreateMonthlyOrderPeriod() {
        OrderPeriodWS[] periods = webServicesSession.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()) {
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(webServicesSession.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "MONTHLY")));
        return webServicesSession.createOrderPeriod(monthly);
    }

    private Integer getOrCreateOrderChangeStatusApply() {
        OrderChangeStatusWS[] statuses = webServicesSession.getOrderChangeStatusesForCompany();
        for (OrderChangeStatusWS status : statuses) {
            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status.getId();
            }
        }
        //there is no APPLY status in db so create one
        OrderChangeStatusWS apply = new OrderChangeStatusWS();
        String statusName = "APPLY SUBSCRIPTION";
        OrderChangeStatusWS status = new OrderChangeStatusWS();
        status.setApplyToOrder(ApplyToOrder.YES);
        status.setDeleted(0);
        status.setOrder(1);
        status.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, statusName));
        return webServicesSession.createOrderChangeStatus(apply);
    }

     Integer getDuration(PlanWS plan) {
        MetaFieldValueWS[] planMetaFields = plan.getMetaFields()
        for (MetaFieldValueWS metaField : planMetaFields) {
            if (metaField.getFieldName().equals("Duration") && metaField.getStringValue() != null) {
                return Integer.valueOf(metaField.getStringValue())
            }
        }
    }

     String getPaymentOption(PlanWS plan) {
        MetaFieldValueWS[] planMetaFields = plan.getMetaFields();
        for (MetaFieldValueWS metaField : planMetaFields) {
            if (metaField.getFieldName().equals("Payment Option")) {
                return metaField.getStringValue()
            }
        }
    }

     Date getActiveUntil(Integer duration, Date activeSince) throws SessionInternalError {
        if (duration != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(activeSince);
            cal.add(Calendar.MONTH, duration);
            cal.add(Calendar.DATE, -1)
            return cal.getTime();
        } else return null;
    }


    public BigDecimal getRate(PlanWS plan) throws SessionInternalError{
        ItemDTO planItemDTO = new ItemBL(plan.getItemId()).getEntity();
        PriceModelDTO planPriceModel = planItemDTO.getPrice(TimezoneHelper.companyCurrentDate(planItemDTO.getPriceModelCompanyId()),
                planItemDTO.getPriceModelCompanyId());
        return planPriceModel.getRate();
    }

    private boolean setUpgradedToValue(OrderWS orderWS, Integer upgraedOrder){
        for(MetaFieldValueWS metaFieldValueWS : orderWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals("Upgraded to")){
                   metaFieldValueWS.setIntegerValue(upgraedOrder)
                    break
            }
        }
    }

    private List<DtPlanWS> getPlans(){
        String cacheKey = "${DtReserveInstanceCache.RESERVE_CACHE_KEY}${webServicesSession.getCallerCompanyId()}"
        List<DtPlanWS> reservedPlanList = DtReserveInstanceCache.getReservedInstanceCache(cacheKey)
        logger.debug("catalogue cache key ${cacheKey}")

        if (reservedPlanList == null) {
            logger.debug("cache key not found. Retrieving list from DB")

            reservedPlanList = new ArrayList<>()
            List<PlanWS> rawPlanList = webServicesSession.getAllPlans()

            for (PlanWS planWS : rawPlanList) {
                DtPlanWS dtPlanWS = new DtPlanWS()
                dtPlanWS.setDuration(getDuration(planWS))
                dtPlanWS.setPaymentMode(getPaymentOption(planWS))

                if (dtPlanWS.getDuration() != null && dtPlanWS.getDuration() != 0 &&
                        dtPlanWS.getPaymentMode() != null) {
                    dtPlanWS = dtReserveInstanceWSMapper.mapPlanToDtPlan(planWS, dtPlanWS)

                    if(dtPlanWS != null) {
                        reservedPlanList.add(dtReserveInstanceWSMapper.mapPlanToDtPlan(planWS, dtPlanWS))
                    }
                }
            }
            DtReserveInstanceCache.setReservedInstanceCache(cacheKey, reservedPlanList)
        }

        return reservedPlanList;

    }
}

