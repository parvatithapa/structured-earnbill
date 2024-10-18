/**
 * 
 */
package com.sapienter.jbilling.server.movius.integration;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import javax.mail.MessagingException;

/**
 * @author Manish Bansod
 *
 */
public class MoviusTaskUtils {

	private static final Logger LOG = LoggerFactory.getLogger(MoviusTaskUtils.class);
	private static IWebServicesSessionBean sessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
	private static IMethodTransactionalWrapper actionTxWrapper = Context.getBean("methodTransactionalWrapper");

	private MoviusTaskUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Maps the value of Movius Organization object into user object and returns the User Object 
	 * if the user with same name is already present, it returns the existing user with mapped values from organization 
	 * else it creates a new user with mapped values from organization
	 * 
	 * @param org : Movius Parent Organization object
	 * @param entityId : Entity ID of the scheduled task 
	 * @param parentId : Parent ID, or null if the user does not have any parent
	 * @param bl : UserBL object
	 * @param organization : Movius Organization object
	 * 
	 * @return : UserWS object
	 */
	public static UserWS getUserWS(String systemId, Integer entityId,
			Integer parentId,  Organization organization,Integer billingCyclePeriod,Integer billingCycleDay) {
		UserWS userWS = new UserWS();
		boolean isParent = false;
		//if user already exist, its going to update the existing user
		Optional<Integer> userId = getUserByOrgId(organization.getId());
		if(userId.isPresent()) {
		    userWS = sessionBean.getUserWS(userId.get());
		    if(userWS.getIsParent()) {
		    	isParent = true;
		    }
		}
		userWS.setUserName(organization.getName()+"-"+organization.getId());
		userWS.setMainRoleId(Constants.TYPE_CUSTOMER);

		if (organization.hasSubOrgs()) {
			isParent = true;
		}
		if (!userId.isPresent()) {
			userWS.setUseParentPricing(Boolean.TRUE);
		}
		userWS.setIsParent(isParent);
		userWS.setParentId(parentId);
		userWS.setEntityId(entityId);
		userWS.setInvoiceChild(organization.isBillable());
		// userId.isPresent() false
		if (!userId.isPresent()) {
			Integer accountTypeId = getAccountTypeId(entityId);
			userWS.setAccountTypeId(accountTypeId);
			userWS.setInvoiceTemplateId(getInvoiceTemplateId(accountTypeId));
			if(isParent && null == userWS.getParentId() ) {
                               setLanguageAndCurrencyID(userWS, systemId);
				userWS.setMainSubscription(createUserMainSubscription(billingCyclePeriod, billingCycleDay));
			} else {
				if (null != parentId && userWS.getParentId() == parentId) {
                                       UserWS parentWS =sessionBean.getUserWS(parentId);
					if (organization.isBillable()) {
						userWS.setMainSubscription(createUserMainSubscription(billingCyclePeriod,billingCycleDay));
					} else {
						userWS.setMainSubscription(parentWS.getMainSubscription());
						userWS.setNextInvoiceDate(parentWS.getNextInvoiceDate());
					}
                                       setLanguageAndCurrencyFromParent(parentWS, userWS);
				} else {
                                       setLanguageAndCurrencyID(userWS, systemId);
					userWS.setMainSubscription(createUserMainSubscription(billingCyclePeriod, billingCycleDay));
				}
			}
		}

		List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
		if (null != userWS.getMetaFields()) {
			metaFieldValues.addAll(Arrays.asList(userWS.getMetaFields()));
		}

		MetaFieldValueWS orgIdMetaField = new MetaFieldValueWS();
		orgIdMetaField.setFieldName(MoviusConstants.ORG_ID);
		orgIdMetaField.setDataType(DataType.STRING);
		orgIdMetaField.setValue(organization.getId());
		metaFieldValues.add(orgIdMetaField);

		if (null != organization.getBillingPlanId()) {
			MetaFieldValueWS billingPlanMetaField = new MetaFieldValueWS();
			billingPlanMetaField.setFieldName(MoviusConstants.BILLING_PLAN_ID);
			billingPlanMetaField.setDataType(DataType.INTEGER);
			billingPlanMetaField.setValue(organization.getBillingPlanId());
			metaFieldValues.add(billingPlanMetaField);
		}

		if (null != organization.getBillingPlanName()) {
			MetaFieldValueWS billingPlanNameMetaField = new MetaFieldValueWS();
			billingPlanNameMetaField.setFieldName(MoviusConstants.BILLING_PLAN_NAME);
			billingPlanNameMetaField.setDataType(DataType.STRING);
			billingPlanNameMetaField.setValue(organization.getBillingPlanName());
			metaFieldValues.add(billingPlanNameMetaField);
		}

		if (null != organization.getTimezone()) {
			MetaFieldValueWS timeZoneMetaField = new MetaFieldValueWS();
			timeZoneMetaField.setFieldName(MoviusConstants.TIMEZONE);
			timeZoneMetaField.setDataType(DataType.STRING);
			timeZoneMetaField.setValue(organization.getTimezone());
			metaFieldValues.add(timeZoneMetaField);
		}

		userWS.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
		return userWS;
	}

    /**
     * Helper method to find and set Language Id and Currency Id
     * 
     * @param userWS
     * @param systemId
     */
    private static void setLanguageAndCurrencyID(UserWS userWS, String systemId) {
    	CompanyDTO companyDTO = new CompanyDAS().findEntityByName(systemId);
		if(null == companyDTO) {
		      LOG.debug("No company found with this system id: {}", systemId);
		      throw new SessionInternalError("No company found with this system id: "+systemId);
		}
		userWS.setLanguageId(companyDTO.getLanguageId());
		userWS.setCurrencyId(companyDTO.getCurrencyId());
    }
    
    /**
     * Helper method to find account type ID at company level metafiled
     * 
     * @param entityId
     */
    private static Integer getAccountTypeId(Integer entityId) {
    	Integer accountTypeId = Integer.parseInt(new MetaFieldDAS().
				getComapanyLevelMetaFieldValue(MoviusConstants.ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY, entityId));
    	if (null == accountTypeId) {
			LOG.debug("No account type ID setup at company level metafiled in entityId: {}", entityId);
    		throw new SessionInternalError("No account type ID setup at company level metafiled in entityId: "+entityId);
    	}
    	return accountTypeId;
    }
    
    private static Integer getInvoiceTemplateId(Integer accountTypeId) {
		AccountTypeWS accountType = sessionBean.getAccountType(accountTypeId);
		if (null == accountType.getInvoiceTemplateId()) {
			LOG.debug("No Inoice Template set on account type level {}", accountType.getDescription(accountType.getLanguageId()));
			throw new SessionInternalError("No Inoice Template set on account type level : "
							+ accountType.getDescription(accountType.getLanguageId()));
		}
		return accountType.getInvoiceTemplateId();
    }

	public static ItemDTOEx getItem(Integer entityId) throws SessionInternalError {
		return actionTxWrapper.<ItemDTOEx>execute(() -> {
			Integer itemId = Integer.parseInt(new MetaFieldDAS().
					getComapanyLevelMetaFieldValue(MoviusConstants.ITEM_ID_FOR_ORG_HIERARCHY, entityId));
			ItemDTO item = new ItemDAS().find(itemId);
			return new ItemBL().getWS(item);
		});
	}

	public static OrderWS getOrderWS(UserWS user, Integer itemId, String quantity, String itemDescription, String price){
		OrderLineWS []orderLineWS = new OrderLineWS[1];
		orderLineWS[0] = buildOrderLine(itemId, quantity, itemDescription, price);
		OrderWS orderWS = new OrderWS();
		orderWS.setActiveSince(new Date());
		orderWS.setOrderLines(orderLineWS);
		orderWS.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		orderWS.setPeriod(getMonthlyOrderPeriod(user.getEntityId()));
		orderWS.setCurrencyId(user.getCurrencyId());
		orderWS.setUserId(user.getId());
		orderWS.setProrateFlag(true);
		return orderWS;
	}

	public static OrderLineWS buildOrderLine(Integer itemId, String quantity, String description, String price) {
		OrderLineWS line = new OrderLineWS();
		line.setQuantity(quantity);
		line.setDescription(description);
		line.setItemId(itemId);
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setPrice(price);
		line.setAmount(price);
		return line;
	}

	public static Integer getMonthlyOrderPeriod(Integer entityId) {
		return actionTxWrapper.<Integer>execute(() -> {
			List<OrderPeriodDTO> periods = new OrderPeriodDAS().getOrderPeriods(entityId);
			for(OrderPeriodDTO period : periods){
				if(1 == period.getValue() && PeriodUnitDTO.MONTH == period.getUnitId()){
					return period.getId();
				}
			}
			return null;
		});
	}

	public static Integer getOrderChangeApplyStatus(Integer entityId) {
		return actionTxWrapper.execute(() -> {
			List<OrderChangeStatusDTO>statuses = new OrderChangeStatusDAS().findOrderChangeStatuses((entityId));
			Integer statusId = null;
			for(OrderChangeStatusDTO orderChangeStatus : statuses){
				if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
					statusId = orderChangeStatus.getId();
					break;
				}
			}
			return statusId;
		});
	}

	public static void updateOrderChangeEndDate(OrderChangeWS[] orderChangeWSArray, Integer itemId){
		actionTxWrapper.execute(() -> {
			OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
			for(OrderChangeWS orderChangeWS : orderChangeWSArray){
				if(null != orderChangeWS.getId() && orderChangeWS.getItemId().equals(itemId) && null == orderChangeWS.getEndDate()) {
					OrderChangeDTO persistedOrderChange = new OrderChangeDAS().find(orderChangeWS.getId());
			        if(null != persistedOrderChange){
			            persistedOrderChange.setEndDate(new Date());
			            orderChangeDAS.save(persistedOrderChange);
			        }
				}
			}
		});
	}

    public static boolean isUserExist(String orgId) {
        return getUserByOrgId(orgId).isPresent();
    }

    public static Optional<Integer> getUserByOrgId(String orgId) {
        return actionTxWrapper.<Optional<Integer>>execute(() -> {
            UserBL userBL = new UserBL();
            List<CustomerDTO> results = userBL.getUserByCustomerMetaField(orgId, MoviusConstants.ORG_ID, sessionBean
                    .getCallerCompanyId());
            if(CollectionUtils.isNotEmpty(results)) {
                return Optional.of(results.get(0).getBaseUser().getId());
            }
            return Optional.empty();
        });
    }
    
    public static boolean isPriceOrQuantityUpdated(OrderChangeWS[] oldOrderChanges, UserWS userWS, String price, String quantity) {
		boolean isUpdated = false;
		BigDecimal receivedQuantity = new BigDecimal(quantity);
		BigDecimal receivedPrice = new BigDecimal(price);
		BigDecimal originalQuantity = BigDecimal.ZERO;
		BigDecimal originalPrice = BigDecimal.ZERO;

		for (OrderChangeWS change : oldOrderChanges) {
			if(change.getItemId().equals(getItem(userWS.getEntityId()).getId())
					&& null == change.getEndDate()) {
				originalQuantity = originalQuantity.add(change.getQuantityAsDecimal());
				originalPrice = originalPrice.add(change.getPriceAsDecimal());
			}
		}

		if((originalPrice.compareTo(receivedPrice) != 0)
				|| (originalQuantity.compareTo(receivedQuantity) != 0))
			isUpdated = true;

		return isUpdated;
	}

    public static OrderWS getLatestMonthlyActiveOrder(Integer userId, Integer languageId, Integer orderPeriodId) {
		return actionTxWrapper.execute(() -> {
			OrderDAS orderDAS = new OrderDAS();
		    List<OrderDTO> orderDTOList = orderDAS.findOrdersByUserAndPeriod(userId, orderPeriodId);
		    if(orderDTOList.isEmpty()) {
		        return null;
		    }
		    return new OrderBL(orderDTOList.get(0)).getWS(languageId);
		});
    }

    public static OrderLineWS getCurrentOrderLine(OrderLineWS[] lines, UserWS userWS){
        for(OrderLineWS orderLineWS : lines){
            if(orderLineWS.getItemId().equals(getItem(userWS.getEntityId()).getId())) {
                return orderLineWS;
            }
        }
        return null;
    }

    private static Integer getMonthlyOrderPeriod(){
        OrderPeriodWS[] periods = sessionBean.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                return period.getId();
            }
        }
        return null;
    }
    
	private static MainSubscriptionWS createUserMainSubscription(Integer billingCyclePeriod, Integer billingCycleDay) {
    	MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(null != billingCyclePeriod ? billingCyclePeriod:getMonthlyOrderPeriod()); //monthly, if parameter is null
    	mainSubscription.setNextInvoiceDayOfPeriod(null != billingCycleDay ? billingCycleDay : 1); // 1st of the month, if parameter is null 
    	return mainSubscription;
    }

	public static String buildErrorsParameter(List<Errors> notificationErrorList){
		try {
			StringBuilder builder = new StringBuilder();
			for (Errors errors : notificationErrorList) {
				String fileName = errors.getFilename();
				builder = builder.append("File name: ").append(fileName).append("%n");
				int count = 1;
				for (String error : errors.getErrorList()) {
					builder.append("\t")
							.append(count)
							.append(". ")
							.append("\t")
							.append(error)
							.append("%n");
					count++;
				}

			}
			return String.format(builder.toString());
		} catch (Exception e){
			return "Error report creation failed";
		}
	}

	public static String getMetaFieldValueByName(String metaFieldName, Integer entityId){
		return actionTxWrapper.execute(() -> {
			CompanyDTO companyDTO = new CompanyDAS().find(entityId);
			List<MetaFieldValue> metaFieldValueList = companyDTO.getMetaFields();
			for(MetaFieldValue metaFieldValue : metaFieldValueList){
				if(metaFieldValue.getField().getName().equals(metaFieldName) && null != metaFieldValue.getValue()){
					return metaFieldValue.getValue().toString();
				}
			}
			return null;
		});
	}

	/**
	 * Sends the sendMoviusCustomerNotFoundEmailMessage Notification
	 *
	 * @param entityId
	 * @param billingAdminEmail
	 * @param messageKey
	 * @param params
	 * @throws SessionInternalError
	 * @throws NotificationNotFoundException when no message row or message row is not activated for the specified entity
	 */
	public static void sendMoviusErrorReportMessage(Integer entityId, String billingAdminEmail, String messageKey, String[] params) {
		try {
			if(null!=billingAdminEmail) {
				NotificationBL.sendSapienterEmail(billingAdminEmail, entityId, messageKey, null, params);
			} else {
				LOG.debug(MoviusConstants.ORIGINATION_ERROR_COMPANY_ADMIN_EMAIL_NOT_FOUND,entityId);
			}
		} catch (MessagingException | IOException e) {
			LOG.error("Exception while sending notification : " + e.getMessage(), e);
		}
	}

	public static class Errors{
		private String filename;
		private List<String> errorList;

		public Errors(){
			filename = MoviusConstants.EMPTY;
			errorList = new ArrayList<>();
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public List<String> getErrorList() {
			return errorList;
		}

		public void setErrorList(List<String> errorList) {
			this.errorList = errorList;
		}
	}

	/**
	 * Check if origination order is already available for the received org
	 * in this case, the quantity for subscription order line should be sum up the origination lines
	 * @param lines
	 * @param userWS
	 * @param receivedQuanity
	 */
	public static String getOriginationOrderLineQuantity(OrderLineWS[] lines, UserWS userWS, String receivedQuanity) {
		Boolean isOriginiationLinePresent = false;
		BigDecimal originiationQuantity = BigDecimal.ZERO;

		for(OrderLineWS orderLineWS : lines) {
			if(!orderLineWS.getItemId().equals(getItem(userWS.getEntityId()).getId())) {
				originiationQuantity = originiationQuantity.add(orderLineWS.getQuantityAsDecimal());
				isOriginiationLinePresent = true;
			}
		}
		return isOriginiationLinePresent ? originiationQuantity.toString() : receivedQuanity;
	}
	
	public static boolean isOrderLineExists(OrderWS orderWS, Integer itemId) {
	    return Arrays.stream(orderWS.getOrderLines()).anyMatch(orderLineWS -> 0 == orderLineWS.getItemId().compareTo(itemId));
	}

	public static Integer getSubscriptionItemIdByEntity(Integer entityId) {
        String item = MoviusTaskUtils.getMetaFieldValueByName(MoviusConstants.ITEM_ID_FOR_ORG_HIERARCHY, entityId);
        return Integer.parseInt(item);
    }

	public static String findOrgIdbyUserId(Integer userId) {
		return actionTxWrapper.execute(() -> {
			UserBL bl = new UserBL(userId);
	        CustomerDTO customer = bl.getEntity().getCustomer();
	        MetaFieldValue<?> orgId = customer.getMetaField(MoviusConstants.ORG_ID);
	        return Objects.nonNull(orgId) ? (String) orgId.getValue() : null;
		});
    }

    private static void setLanguageAndCurrencyFromParent(UserWS parentUser, UserWS childUser) {
      if(null != parentUser){
          childUser.setLanguageId(parentUser.getLanguageId());
          childUser.setCurrencyId(parentUser.getCurrencyId());
      }
    }
}
