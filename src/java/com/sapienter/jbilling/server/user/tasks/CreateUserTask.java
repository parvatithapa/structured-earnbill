package com.sapienter.jbilling.server.user.tasks;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.CreateUserEvent;
import com.sapienter.jbilling.server.user.CreateUserRequestWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBillingPeriod;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class CreateUserTask extends PluggableTask implements IInternalEventsTask {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final ParameterDescription PARAM_DEFAULT_BILLING_CYCLE =
			new ParameterDescription("default billingPeriodId", true, Type.INT);
	private static final ParameterDescription PARAM_NEXT_INVOICE_DAY_OF_PERIOD =
			new ParameterDescription("default nextInvoiceDayOfPeriod", true, Type.INT);
	private static final ParameterDescription PARAM_ACCOUNT_TYPE_ID =
			new ParameterDescription("accountTypeId", true, Type.INT);
	private static final ParameterDescription PARAM_USER_ID_MF_NAME =
			new ParameterDescription("userIdMetaFieldName", true, Type.STR);

	public CreateUserTask() {
		descriptions.add(PARAM_DEFAULT_BILLING_CYCLE);
		descriptions.add(PARAM_NEXT_INVOICE_DAY_OF_PERIOD);
		descriptions.add(PARAM_ACCOUNT_TYPE_ID);
		descriptions.add(PARAM_USER_ID_MF_NAME);
	}

	@SuppressWarnings("unchecked")
	private static final Class<Event>[] events = new Class[] { CreateUserEvent.class };

	private Integer getOrderPeriodId(IWebServicesSessionBean api, UserBillingPeriod billingPeriod)
			throws NumberFormatException, PluggableTaskException {
		Integer defaultBillingPeriod = Integer.valueOf(getMandatoryStringParameter(PARAM_DEFAULT_BILLING_CYCLE.getName()));
		if(null == billingPeriod) {
			return defaultBillingPeriod;
		}
		return Arrays.stream(api.getOrderPeriods())
				.filter(orderPeriod -> orderPeriod.getPeriodUnitId().intValue() == billingPeriod.getPeriodUnti())
				.map(OrderPeriodWS::getId)
				.findFirst()
				.orElse(defaultBillingPeriod);
	}

	@Override
	public void process(Event event) throws PluggableTaskException {
		try {
			CreateUserEvent createUserEvent = (CreateUserEvent) event;
			CreateUserRequestWS createUserRequest = createUserEvent.getCreateUserRequest();
			Integer entityId = event.getEntityId();
			UserDTO dbUser = new UserDAS().findByUserName(createUserRequest.getAccountNumber(), entityId, false);
			if(null!= dbUser) {
				// user already created.
				createUserEvent.setUserId(dbUser.getId());
				return;
			}

			IWebServicesSessionBean api = Context.getBean(Name.WEB_SERVICES_SESSION);
			String userIdMFName = getMandatoryStringParameter(PARAM_USER_ID_MF_NAME.getName());

			MetaField userIdMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER }, userIdMFName);
			if(null == userIdMf) {
				throw new SessionInternalError("validation failed",
						"userIdMetaField not configured for entity "+ entityId,
						HttpStatus.SC_INTERNAL_SERVER_ERROR);
			}

			Integer nextInvoiceDay = createUserRequest!=null ? createUserRequest.getNextInvoiceDayOfPeriod() :
				Integer.valueOf(getMandatoryStringParameter(PARAM_NEXT_INVOICE_DAY_OF_PERIOD.getName()));

			Integer periodId = getOrderPeriodId(api, createUserRequest.getBillingPeriod());
			try (UserWS user = new UserWS()) {
				user.setUserName(createUserRequest.getAccountNumber());
				user.setMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay));
				user.setLanguageId(api.getCallerLanguageId());
				user.setMainRoleId(com.sapienter.jbilling.client.util.Constants.ROLE_CUSTOMER);
				user.setStatusId(UserDTOEx.STATUS_ACTIVE);
				user.setCurrencyId(api.getCallerCurrencyId());
				user.setEntityId(api.getCallerCompanyId());
				Integer accountTypeId = Integer.valueOf(getMandatoryStringParameter(PARAM_ACCOUNT_TYPE_ID.getName()));
				AccountTypeDAS accountTypeDAS = new AccountTypeDAS();
				if(!accountTypeDAS.isIdPersisted(accountTypeId)) {
					throw new SessionInternalError("validation failed",
							"invalid accountTypeId configured",
							HttpStatus.SC_INTERNAL_SERVER_ERROR);
				}
				user.setAccountTypeId(accountTypeId);
				// Adding AccountNumber on customer level metaField
				addCustomerLevelMetaField(user, userIdMf.getName(), createUserRequest.getAccountNumber(), userIdMf.getDataType());
				Integer userId = api.createUser(user);
				logger.debug("user={} created for accountNumber={}", userId, createUserRequest.getAccountNumber());
				createUserEvent.setUserId(userId);
			}
		} catch(SessionInternalError sessionInternalError) {
			throw sessionInternalError;
		} catch (Exception exception) {
			logger.error("createUserTask because of ", exception);
			throw new SessionInternalError("createUserTask failed", exception.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Class<Event>[] getSubscribedEvents() {
		return events;
	}

	private void addCustomerLevelMetaField(UserWS user, String mfName, Object mfValue, DataType dataType) {
		List<MetaFieldValueWS> userMetaFields = new ArrayList<>();
		if (ArrayUtils.isNotEmpty(user.getMetaFields())){
			userMetaFields.addAll(Arrays.asList(user.getMetaFields()));
		}
		MetaFieldValueWS metaField = new MetaFieldValueWS(mfName, null, dataType, true, mfValue);
		userMetaFields.add(metaField);
		logger.debug("adding metafield {} with value {} on user", mfName, mfValue);
		user.setMetaFields(userMetaFields.toArray(new MetaFieldValueWS[userMetaFields.size()]));
		logger.debug("metafield {} added on user {}", metaField, user);
	}
}