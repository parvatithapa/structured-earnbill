package com.sapienter.jbilling.server.integration.common.utility;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.job.model.MeteredUsageItem;
import com.sapienter.jbilling.server.integration.common.job.model.MeteredUsageStepResult;
import com.sapienter.jbilling.server.integration.common.job.model.PriceModelType;
import com.sapienter.jbilling.server.integration.common.pricing.MeteredUsageCustomUnitBuilder;
import com.sapienter.jbilling.server.integration.common.pricing.MeteredUsageDescriptionBuilder;
import com.sapienter.jbilling.server.integration.common.service.HelperDataAccessService;
import com.sapienter.jbilling.server.integration.common.service.vo.ChargePeriod;
import com.sapienter.jbilling.server.integration.common.service.vo.CompanyInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.CustomerInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineTierInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ProductInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ProductRatingConfigInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ReservedPlanInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ReservedUsageInfo;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;

/**
 * Created by abhishek.yadav
 */
public class UsageItemHelper {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Autowired
	private ReservedInstanceHelper reservedInstanceHelper;

	@Getter
	@Setter
	private HelperDataAccessService dataAccessService;

	public List<MeteredUsageItem> getUsageItemsForOrderLine(Integer entityId, OrderLineInfo orderLineInfo, Integer languageId) {

		List<MeteredUsageItem> usageItems = new ArrayList<>();

		List<ReservedUsageInfo> reservedUsageInfoList = reservedInstanceHelper.getReservedUsageInfo(entityId, orderLineInfo.getOrderLineId(), languageId);
		// Reserved Usage
		if (!reservedUsageInfoList.isEmpty()) {
			usageItems.addAll(getUsageItemsForReservedUsages(entityId, orderLineInfo, reservedUsageInfoList, languageId));
		} else {    //Non-Reserved Usage
			usageItems.addAll(getUsageItemsForNonReservedUsage(entityId, orderLineInfo, languageId));
		}
		return usageItems;
	}

	public List<MeteredUsageItem> getUsageItemsForReservedMonthlyPlan(Integer entityId, OrderInfo orderInfo, OrderLineInfo orderLineInfo) {

		List<MeteredUsageItem> usageItems = new ArrayList<>();

		Integer itemIdOfPlan = dataAccessService.getItemIdForPlan(orderInfo.getPlanId());

		if (!itemIdOfPlan.equals(orderLineInfo.getItemId())) {
			return usageItems;
		}

		//For sending pending adjustment usages
		int mf = new MetaFieldDAS().getFieldByName(entityId,new EntityType[]{EntityType.ORDER} , Constants.ADJUSTMENT).getId();
		MetaFieldValueWS value = dataAccessService.getOrderMetafieldValue(entityId, orderInfo.getOrderId(),mf);

		if(value.getDecimalValueAsDecimal() != null && value.getDecimalValueAsDecimal().compareTo(BigDecimal.ZERO) < 0){
			usageItems.add(getUsageItemForPendingAdjustment(value.getDecimalValueAsDecimal(),orderInfo.getOrderId()));
		}

		Date reportFromDate = getCostReportFromDate(entityId, orderInfo);
		Date reportToDate = getCostReportToDate(orderInfo, entityId);

		Set<ChargePeriod> chargePeriods = DateUtility.divideRangeMonthWise(reportFromDate, reportToDate);
		if (chargePeriods.isEmpty()) {
			return usageItems;
		}
		logger.debug("Sending Reserved Monthly Plan cost from {} to {}", reportFromDate, reportToDate);
		for (ChargePeriod chargePeriod : chargePeriods) {
			usageItems.add(getUsageItemForReservedMonthlyPlan(entityId, orderInfo, orderLineInfo, chargePeriod));
		}

		return usageItems;
	}

	private Date getCostReportFromDate(Integer entityId, OrderInfo orderInfo) {

		Calendar calendarFromDate = Calendar.getInstance();
		MetaField lastRunDateMetafield = dataAccessService.getFieldByName(entityId, EntityType.ORDER, Constants.ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF);
		if (lastRunDateMetafield == null) {
			return null;
		}
		MetaFieldValueWS lastRunDateValue = dataAccessService.getOrderMetafieldValue(entityId, orderInfo.getOrderId(), lastRunDateMetafield.getId());
		if (lastRunDateValue == null || lastRunDateValue.getDateValue() == null) {
			calendarFromDate.setTime(orderInfo.getActiveSince());

		} else {
			calendarFromDate.setTime(lastRunDateValue.getDateValue());
			calendarFromDate.add(Calendar.MONTH, 1);
			calendarFromDate.set(Calendar.DAY_OF_MONTH, 1);

		}

		DateUtility.setTimeToStartOfDay(calendarFromDate);

		return calendarFromDate.getTime();
	}

	private Date getCostReportToDate(OrderInfo orderInfo, Integer entityId) {

		Calendar calendarLastDayOfMonth = Calendar.getInstance();
		int lastDateInt = calendarLastDayOfMonth.getActualMaximum(Calendar.DATE);
		calendarLastDayOfMonth.set(Calendar.DATE, lastDateInt);
		DateUtility.setTimeToEndOfDay(calendarLastDayOfMonth);
		Date lastDayOfMonth = calendarLastDayOfMonth.getTime();

		Date activeUntil = orderInfo.getActiveUntil();

		if (activeUntil == null) {
			activeUntil = calculateActiveUntilWithDuration(orderInfo, entityId);
			if (activeUntil == null) {
				return null;
			}
		}

		Calendar calendarUntilDate = Calendar.getInstance();
		calendarUntilDate.setTime(activeUntil);
		DateUtility.setTimeToEndOfDay(calendarUntilDate);
		activeUntil = calendarUntilDate.getTime();

		return DateUtility.earliest(lastDayOfMonth, activeUntil);
	}

	private Date calculateActiveUntilWithDuration(OrderInfo orderInfo, Integer entityId) {

		Optional<MetaFieldValueWS> activeUntilValueWS;
		Integer duration;
		Optional<Date> activeUntilDate;
		activeUntilValueWS = reservedInstanceHelper.getMetafieldValueByName(Constants.PLAN_DURATION_MF, entityId, orderInfo.getPlanId(), EntityType.PLAN);

		if (activeUntilValueWS.isPresent()) {
			try {
				duration = Integer.parseInt(activeUntilValueWS.get().getStringValue());
			} catch (NumberFormatException nfe) {
				logger.error("Duration for Plan {} is non-integer, Exception {}", orderInfo.getPlanId(), nfe.getMessage());
				return null;
			}
			activeUntilDate = DateUtility.addMonthsToDate(orderInfo.getActiveSince(), duration);

			if (activeUntilDate.isPresent()) {
				return DateUtility.addDaysToDate(activeUntilDate.get(), -1);
			}
		}
		return null;
	}


	private MeteredUsageItem getUsageItemForReservedMonthlyPlan(Integer entityId, OrderInfo orderInfo, OrderLineInfo orderLineInfo, ChargePeriod chargePeriod) {

		MeteredUsageItem usageItem = new MeteredUsageItem();

		ReservedPlanInfo reservedPlanInfo = reservedInstanceHelper.getReservedPlanInfo(entityId, orderInfo.getPlanId(), orderLineInfo.getPrice());
		BigDecimal quantity = orderLineInfo.getQuantity();
		quantity = reservedInstanceHelper.prorateQuantity(chargePeriod.getFirstDay(), chargePeriod.getLastDay(), quantity);

		Map<String, String> attributes = new HashMap<>();
		attributes.put(Constants.PLAN_PAYMENT_OPTION_MF, reservedPlanInfo.getPaymentOption());
		attributes.put(Constants.PLAN_DURATION_MF, reservedPlanInfo.getDuration());

		usageItem.setPriceModelType(PriceModelType.RESERVED_PURCHASE);
		usageItem.setProductCode(Integer.toString(orderInfo.getPlanId()));
		usageItem.setProductDescription(reservedPlanInfo.getDescription());
		usageItem.setPrice(reservedPlanInfo.getPrice());
		usageItem.setQuantity(quantity);
		usageItem.setPriceModelAttributes(attributes);

		return usageItem;
	}

	private MeteredUsageItem getUsageItemForPendingAdjustment(BigDecimal adjustment, Integer orderId){
		MeteredUsageItem usageItem = new MeteredUsageItem();
		Map<String, String> attributes = new HashMap<>();
		attributes.put(Constants.INITIAL, orderId.toString());
		attributes.put(Constants.FINAL, orderId.toString());
		usageItem.setPriceModelType(PriceModelType.RESERVED_UPGRADE);
		usageItem.setPrice(adjustment);
		usageItem.setQuantity(BigDecimal.valueOf(1));
		usageItem.setPriceModelAttributes(attributes);
		return usageItem;
	}

	private Map<String, String> getPriceModelAttributesForReserved(Integer entityId, Integer planId) {

		Map<String, String> priceModelAttribute = new HashMap<>();

		MetaField metafieldPaymentOption = dataAccessService.getFieldByName(entityId, EntityType.PLAN, Constants.PLAN_PAYMENT_OPTION_MF);
		MetaField metafieldDuration = dataAccessService.getFieldByName(entityId, EntityType.PLAN, Constants.PLAN_DURATION_MF);

		MetaFieldValueWS paymentOption = dataAccessService.getPlanMetafieldValue(entityId, planId, metafieldPaymentOption.getId());
		MetaFieldValueWS duration = dataAccessService.getPlanMetafieldValue(entityId, planId, metafieldDuration.getId());

		priceModelAttribute.put(metafieldPaymentOption.getName(), paymentOption.getStringValue());
		priceModelAttribute.put(metafieldDuration.getName(), duration.getStringValue());

		return priceModelAttribute;
	}

	private Map<String, String> getPriceModelAttributesForTiered(Locale locale, OrderLineTierInfo tierInfo) {

		Map<String, String> priceModelAttribute = new HashMap<>();
		priceModelAttribute.put(Constants.TIERED_FROM, getValueInString(locale, tierInfo.getTierFrom()));
		priceModelAttribute.put(Constants.TIERED_TO, getValueInString(locale, tierInfo.getTierTo()));

		return priceModelAttribute;
	}

	private String getValueInString(Locale locale, BigDecimal value) {

		if (value == null) {
			return null;
		}

		return NumberFormat.getInstance(locale)
				.format(value.setScale(Constants.TIER_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP));
	}

	private void setProductFields(Integer entityId, MeteredUsageItem usageItem, OrderLineInfo orderLineInfo, Integer languageId) {
		int productId = orderLineInfo.getItemId();

		usageItem.setItemId(productId);
		Optional<ProductInfo> productInfo = dataAccessService.getProductInfo(entityId, productId, languageId);


		if (productInfo.isPresent()) {
			usageItem.setProductDescription(productInfo.get().getDescription());
			usageItem.setProductBillingUnit(getProductBillingUnit(productInfo.get(), languageId, orderLineInfo.getCreateDateTime()));
			usageItem.setProductCode(productInfo.get().getProductCode());
		} else {
			//TODO MISSING PRODUCT in JBILLING, default to using order level description and empty billing units
			usageItem.setProductDescription(orderLineInfo.getDescription());
			usageItem.setProductBillingUnit("");
			usageItem.setProductCode(orderLineInfo.getItemId().toString());
		}
	}

	private List<MeteredUsageItem> getUsageItemsForNonReservedUsage(Integer entityId, OrderLineInfo orderLineInfo, Integer languageId) {

		List<MeteredUsageItem> usageItems = new ArrayList<>();

		List<OrderLineTierInfo> tiers = dataAccessService.getOrderLineTiers(orderLineInfo.getOrderLineId());

		if (!tiers.isEmpty()) {
			usageItems.addAll(getUsageItemsForTiers(entityId, tiers, orderLineInfo, languageId));

		} else {
			usageItems.add(getUsageItemForSimpleModelType(entityId, orderLineInfo, languageId));
		}
		return usageItems;
	}

	private List<MeteredUsageItem> getUsageItemsForTiers(Integer entityId, List<OrderLineTierInfo> tiers, OrderLineInfo orderLine, Integer languageId) {

		List<MeteredUsageItem> usageItems = new ArrayList<>();
		if (!tiers.isEmpty()) {

			Optional<CompanyInfo> companyInfo = dataAccessService.getCompanyInfo(entityId);
			Locale locale = companyInfo
					.orElse(CompanyInfo.builder().locale(new Locale("en")).build())
					.getLocale();

			for (OrderLineTierInfo tier : tiers) {
				MeteredUsageItem usageItem = new MeteredUsageItem();
				setProductFields(entityId, usageItem, orderLine, languageId);
				usageItem.setPriceModelType(PriceModelType.TIERED);
				usageItem.setPriceModelAttributes(getPriceModelAttributesForTiered(locale, tier));
				usageItem.setQuantity(tier.getQuantity());
				usageItem.setPrice(tier.getPrice());
				usageItems.add(usageItem);
			}
		}
		return usageItems;
	}

	private List<MeteredUsageItem> getUsageItemsForReservedUsages(Integer entityId, OrderLineInfo orderLineWS, List<ReservedUsageInfo> reservedUsageInfoList, Integer languageId) {

		List<MeteredUsageItem> usageItems = new ArrayList<>();

		BigDecimal excessQuantity = orderLineWS.getQuantity();

		for (ReservedUsageInfo reservedUsageInfo : reservedUsageInfoList) {
			MeteredUsageItem reservedUsageItem = new MeteredUsageItem();
			setProductFields(entityId, reservedUsageItem, orderLineWS, languageId);
			reservedUsageItem.setPriceModelType(PriceModelType.RESERVED_USAGE);
			reservedUsageItem.setPriceModelAttributes(getPriceModelAttributesForReserved(entityId, reservedUsageInfo.getPlanId()));
			reservedUsageItem.setPrice(BigDecimal.ZERO);
			reservedUsageItem.setQuantity(reservedUsageInfo.getQuantity());
			usageItems.add(reservedUsageItem);

			excessQuantity = excessQuantity.subtract(reservedUsageInfo.getQuantity());
		}

		if (excessQuantity.compareTo(BigDecimal.ZERO) > 0) {
			orderLineWS.setQuantity(excessQuantity);
			usageItems.addAll(getUsageItemsForNonReservedUsage(entityId, orderLineWS, languageId));
		}

		return usageItems;
	}

	private MeteredUsageItem getUsageItemForSimpleModelType(Integer entityId, OrderLineInfo orderLineInfo, Integer languageId) {

		MeteredUsageItem usageItem = new MeteredUsageItem();
		setProductFields(entityId, usageItem, orderLineInfo, languageId);

		usageItem.setQuantity(orderLineInfo.getQuantity());
		usageItem.setPrice(getPricePerUnit(orderLineInfo));
		usageItem.setPriceModelType(PriceModelType.SIMPLE);
		usageItem.setPriceModelAttributes(null);
		return usageItem;
	}

	// This is a re-adjustment done to recalculate price from amount and quantity.
	// It's negates the bug on the Mediation Order Line processing which is a historical issue
	private BigDecimal getPricePerUnit(OrderLineInfo orderLineInfo) {
		BigDecimal price = orderLineInfo.getPrice();
		if (BigDecimal.ZERO.compareTo(orderLineInfo.getQuantity()) != 0) {
			price = orderLineInfo.getAmount()
				.divide(orderLineInfo.getQuantity(), 6, RoundingMode.HALF_UP);
		}
		return price;
	}

	public List<MeteredUsageStepResult> getUsageStepResultForReservedPlanPurchase(Integer entityId, int userId, BigDecimal quantity,
																																								ReservedPlanInfo reservedPlanInfo) {
		List<MeteredUsageStepResult> results = new ArrayList<>();

		Optional<CustomerInfo> customerInfo = dataAccessService.getCustomerInfo(entityId, userId, Constants.CUSTOMER_EXTERNAL_ACCOUNT_IDENTIFIER_MF);
		if (!customerInfo.isPresent() || StringUtils.isEmpty(customerInfo.get().getExternalAccountIdentifier())) {
			logger.error("CustomerInfo with userId={} not found, skip sending it", userId);
			return results;
		}

		String plainId = Integer.toString(reservedPlanInfo.getPlanId());
		int languageId = customerInfo.get().getLanguageId();

		Map<String, String> attributes = new HashMap<>();
		attributes.put(Constants.PLAN_PAYMENT_OPTION_MF, reservedPlanInfo.getPaymentOption());
		attributes.put(Constants.PLAN_DURATION_MF, reservedPlanInfo.getDuration());

		MeteredUsageStepResult stepResult = new MeteredUsageStepResult();
		stepResult.setAccountIdenfitier(customerInfo.get().getExternalAccountIdentifier());
		results.add(stepResult);

		MeteredUsageItem item = new MeteredUsageItem();
		item.setPriceModelType(PriceModelType.RESERVED_PURCHASE);
		item.setCustomUnit(MeteredUsageCustomUnitBuilder.RESERVED_PURCHASE.getCustomUnit(languageId, plainId, "", attributes));
		item.setFormattedDescription(MeteredUsageDescriptionBuilder.RESERVED_PURCHASE.getDescription(languageId, reservedPlanInfo.getDescription(), "", attributes));
		item.setPrice(reservedPlanInfo.getPrice());
		item.setQuantity(quantity);
		item.setPriceModelAttributes(attributes);

		List<MeteredUsageItem> items = new ArrayList<>();
		items.add(item);
		stepResult.setItems(items);
		return results;
	}

	public List<MeteredUsageStepResult> getAdjustmentForPlanUpgrade(Integer entityId, int userId, BigDecimal adjustment, Integer initialOrderId, Integer newOrderId){

		List<MeteredUsageStepResult> results = new ArrayList<>();

		Optional<CustomerInfo> customerInfo = dataAccessService.getCustomerInfo(entityId, userId, Constants.CUSTOMER_EXTERNAL_ACCOUNT_IDENTIFIER_MF);
		if (!customerInfo.isPresent() || StringUtils.isEmpty(customerInfo.get().getExternalAccountIdentifier())) {
			logger.error("CustomerInfo with userId={} not found, skip sending it", userId);
			return results;
		}

		int languageId = customerInfo.get().getLanguageId();

		Map<String, String> attributes = new HashMap<>();
		attributes.put(Constants.INITIAL, initialOrderId.toString());
		attributes.put(Constants.FINAL, newOrderId.toString());

		MeteredUsageStepResult stepResult = new MeteredUsageStepResult();
		stepResult.setAccountIdenfitier(customerInfo.get().getExternalAccountIdentifier());
		results.add(stepResult);

		MeteredUsageItem item = new MeteredUsageItem();
		item.setPriceModelType(PriceModelType.RESERVED_UPGRADE);
		item.setCustomUnit(MeteredUsageCustomUnitBuilder.RESERVED_UPGRADE.getCustomUnit(languageId, "", "", attributes));
		item.setFormattedDescription(MeteredUsageDescriptionBuilder.RESERVED_UPGRADE.getDescription(languageId, "", "", attributes));
		item.setPrice(adjustment);
		item.setQuantity(BigDecimal.valueOf(1));
		item.setPriceModelAttributes(attributes);

		List<MeteredUsageItem> items = new ArrayList<>();
		items.add(item);
		stepResult.setItems(items);
		return results;
	}

	private Optional<List<ProductRatingConfigInfo>> getPricingUnitForDate(ProductInfo productInfo, Date date) {
		SortedMap<Date, List<ProductRatingConfigInfo>> ratingConfig = productInfo.getRatingConfig();

		if (ratingConfig == null || ratingConfig.isEmpty()) {
			logger.debug("rating config null or empty.");
			return Optional.empty();
		}

		if (date == null) {
			logger.debug("returning rating config for epoch");
			return Optional.of(ratingConfig.get(ratingConfig.firstKey()));
		}

		Date forDate = CommonConstants.EPOCH_DATE;
		if (ratingConfig.firstKey().before(CommonConstants.EPOCH_DATE)) {
			//Additional, Epoch Date is irrelavent in the this case
			forDate = ratingConfig.firstKey();
		}
		logger.debug("First key {}, Rating Config required for {}", ratingConfig.firstKey(), forDate);

		for (Date start : ratingConfig.keySet()) {
			if (start != null && start.after(date)) {
				logger.debug("{} is after expected rating config date of {}", start, date);
				break;
			}
			forDate = start;
		}
		logger.debug("For date is set to {}, returning: {}", forDate, (forDate != null ? ratingConfig.get(forDate) : ratingConfig.get(ratingConfig.firstKey())));
		return Optional.of(forDate != null ? ratingConfig.get(forDate) : ratingConfig.get(ratingConfig.firstKey()));
	}

	private String getProductBillingUnit(ProductInfo productInfo, int languageId, Date dateTime) {
		Optional<List<ProductRatingConfigInfo>> configInfo = getPricingUnitForDate(productInfo, dateTime);

		if (!configInfo.isPresent()) {
			logger.warn("Rating Configuration missing for product {}. Billing Unit will be set to Default i.e {}", productInfo.getProductId(), Constants.DEFAULT_BILLING_UNIT);
			return Constants.DEFAULT_BILLING_UNIT;
		}

		Optional<ProductRatingConfigInfo> prConfigInfo = configInfo.get().stream()
			.filter(prInfo -> prInfo.getLanguageId() == languageId)
			.findFirst();

		return prConfigInfo
			.orElseGet(() -> configInfo.get().stream()
				.filter(prInfo -> prInfo.getLanguageId() == languageId)
				.findFirst()
				.orElse(ProductRatingConfigInfo.builder().pricingUnit("").build()))
			.getPricingUnit();
	}
}
