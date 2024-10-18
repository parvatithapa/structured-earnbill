package com.sapienter.jbilling.server.item;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
public class PriceService {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Resource
	private OrderDAS orderDAS;
	@Resource
	private UserDAS userDAS;
	@Resource(name = "webServicesSession")
	private IWebServicesSessionBean jbillingApi;
	@Resource(name = "jdbcTemplate")
	private JdbcTemplate jdbcTemplate;

	@Transactional(readOnly = true)
	public PriceResponseWS resolvePrice(PriceRequestWS priceRequest) {
		try {
			UserDTO user = userDAS.findNow(priceRequest.getUserId());
			if(null == user) {
				throw new SessionInternalError("user id not found for entity " + jbillingApi.getCallerCompanyId(),
						new String [] { "Please enter valid userId." }, HttpStatus.SC_NOT_FOUND);
			}
			logger.debug("resolving price for userId={} for item={}, plan={} with quantity={} for eventDate={}", priceRequest.getUserId(),
					priceRequest.getItemId(), priceRequest.getPlanId(), priceRequest.getQuantity(), priceRequest.getEventDate());
			ItemBL itemBL = new ItemBL(priceRequest.getItemId());
			List<PricingField> pricingFields = Arrays.asList(PricingField.getPricingFieldsValue(priceRequest.getPricingFields()));
			itemBL.setPricingFields(pricingFields);
			BigDecimal resolvedPrice = itemBL.getPriceByEventDate(priceRequest.getUserId(), user.getCurrencyId(), priceRequest.getQuantity(), priceRequest.getEntityId(), priceRequest.getEventDate());
			logger.debug("price={} resolved for userId={}, for item={} for eventDate={}", resolvedPrice, priceRequest.getUserId(),
					priceRequest.getItemId(), priceRequest.getEventDate());
			PriceResponseWS priceResponse = new PriceResponseWS();
			priceResponse.setEntityId(priceRequest.getEntityId());
			priceResponse.setEventDate(priceRequest.getEventDate());
			priceResponse.setResolvedPrice(resolvedPrice);
			priceResponse.setItemId(priceRequest.getItemId());
			priceResponse.setQuantity(priceRequest.getQuantity());
			priceResponse.setUserId(priceRequest.getUserId());
			priceResponse.setPlanId(priceRequest.getPlanId());
			priceResponse.setOrderId(priceRequest.getOrderId());
			return priceResponse;
		} catch(SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch(Exception exception) {
			logger.error("priceResolution failed for userId={} because of ", priceRequest.getUserId(), exception);
			throw new SessionInternalError("resolvePrice failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private static final String PLAN_SUBSCRIBED_SQL = "SELECT item_id, plan_id FROM plan_item WHERE id IN "
			+ "(SELECT plan_item_id FROM customer_price WHERE user_id = ?)";


	class SubscriptionResult {
		Integer itemId;
		Integer planId;

		public SubscriptionResult(Integer itemId, Integer planId) {
			this.itemId = itemId;
			this.planId = planId;
		}
	}

	private List<SubscriptionResult> findSubscribePlanForUser(Integer userId) {
		SqlRowSet result = jdbcTemplate.queryForRowSet(PLAN_SUBSCRIBED_SQL, userId);
		List<SubscriptionResult> subscribedPlanList = new ArrayList<>();
		while(result.next()) {
			subscribedPlanList.add(new SubscriptionResult(result.getInt("item_id"), result.getInt("plan_id")));
		}
		return subscribedPlanList;
	}

	public void subscribe(UserPlanSubcriptionRequestWS userPlanSubcriptionRequest) {
		try {
			UserDTO user = userDAS.findForUpdate(userPlanSubcriptionRequest.getUserId());
			if(null == user) {
				throw new SessionInternalError("user id not found for entity " + jbillingApi.getCallerCompanyId(),
						new String [] { "Please enter valid userId." }, HttpStatus.SC_NOT_FOUND);
			}
			String planCode = userPlanSubcriptionRequest.getPlanCode();
			List<PlanDTO> plans = new PlanDAS().findPlanByPlanNumber(planCode, jbillingApi.getCallerCompanyId());
			if(CollectionUtils.isEmpty(plans)) {
				throw new SessionInternalError("planCode not found for entity " + jbillingApi.getCallerCompanyId(),
						new String [] { "Please enter valid planCode." }, HttpStatus.SC_NOT_FOUND);
			}
			List<SubscriptionResult> subscribedPlanList = findSubscribePlanForUser(user.getId());
			if(CollectionUtils.isNotEmpty(subscribedPlanList)) {
				if(subscribedPlanList.stream()
						.filter(subscribedPlan -> subscribedPlan.planId.equals(plans.get(0).getId()))
						.findAny().isPresent()) {
					logger.debug("user={}, already subscribed to plan={}", user.getId(), plans.get(0).getId());
					return ;
				}
			}
			PlanBL.subscribe(user.getId(), plans.get(0).getItemId(), userPlanSubcriptionRequest
					.getStartDate(), userPlanSubcriptionRequest.getEndDate());
			logger.debug("user={}, subscribed to plan={}", user.getId(), planCode);
		} catch(SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch (Exception exception) {
			logger.error("planSubscription failed for userId={}, because of ", exception);
			throw new SessionInternalError("subscribe failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}
}