package com.sapienter.jbilling.server.sapphire.signupprocess;

import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.MAIN_SUBCRIPTION_ID_PARAM_NAME;
import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.NEXT_INVOICE_DAY;
import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.ORDER_PERIOD_ID_PARAM_NAME;
import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME;
import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.REFERRING_CUSTOMER_ID_META_FIELD_NAME;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder.OrderType;
import com.sapienter.jbilling.server.process.signup.SignupRequestWS;
import com.sapienter.jbilling.server.process.signup.SignupResponseWS;
import com.sapienter.jbilling.server.sapphire.SapphireHelper;
import com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PaymentInformationBackwardCompatibilityHelper;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class UserAndOrderCreationSignupStep extends AbstractSapphireSignupStep {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String FIND_SUBSCRIPTION_ITEM_BY_PLAN_CODE_SQL =
            "SELECT id FROM item WHERE internal_number = ?";

    private static final String FIND_PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME_SQL =
            "SELECT string_value FROM meta_field_value "
                    + " WHERE dtype = 'string' AND meta_field_name_id = "
                    + "(SELECT id FROM meta_field_name WHERE entity_type = 'COMPANY' "
                    + "AND entity_id = ? AND name = ?)";

    private static final String FIND_ACCOUNT_TYPE_ID_BY_PLAN_CODE_AND_ENTITY_ID_SQL =
            "SELECT account_type_id FROM %s WHERE plan_code = ? AND entity_id = ?";

    public UserAndOrderCreationSignupStep(IWebServicesSessionBean service, IMethodTransactionalWrapper txAction, boolean useNewTx, boolean isAsync) {
        super(service, txAction, useNewTx, isAsync);
    }

    private void validateUserAccount(SignupPlaceHolder holder) {
        SignupResponseWS response = holder.getSignUpResponse();
        SignupRequestWS request = holder.getSignUpRequest();
        UserWS user = request.getUser();
        UserBL userBL = new UserBL(user.getUserName(), holder.getEntityId());
        UserDTO savedUser = userBL.getDto();
        if( null == savedUser ) {
            return ;
        } else {
            response.addErrorResponse("Duplicate account - user already exists with supplied name");
        }
    }

    private void createUser(SignupPlaceHolder holder) {
        IWebServicesSessionBean api = getService();
        SignupResponseWS response = holder.getSignUpResponse();
        SignupRequestWS request = holder.getSignUpRequest();
        UserWS user = request.getUser();
        Integer periodId = holder.getPluginIntParamterByName(MAIN_SUBCRIPTION_ID_PARAM_NAME);
        OrderPeriodDTO orderPeriodDTO = new OrderPeriodDAS().findNow(periodId);
        if(null == orderPeriodDTO) {
            response.addErrorResponse("SAPP-ERROR-INVALID-CUSTOMER-BILLING-CYCLE-PERIOD-ID");
            return;
        }
        String paramName = SapphireSignupProcessTask.PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME.getName();
        String provisioningMfName = holder.getPluginParamterByName(paramName);
        if(StringUtils.isEmpty(provisioningMfName)) {
            response.addErrorResponse(paramName + " not configured for plugin "+ SapphireSignupProcessTask.class.getSimpleName());
            logger.error("parameter {} not configured in plugin {} for entity {}", paramName, SapphireSignupProcessTask.class.getSimpleName(), holder.getEntityId());
            return;
        }

        MetaField provisioningMf = MetaFieldBL.getFieldByName(holder.getEntityId(), new EntityType[] {EntityType.CUSTOMER}, provisioningMfName);
        if(null == provisioningMf) {
            response.addErrorResponse(provisioningMfName + " not configured at customer level metafield for entity " + holder.getEntityId());
            logger.error("MetaField {} not defined for entity {} on customer", provisioningMfName, holder.getEntityId());
            return;
        }
        //Validating referring customer account
        if (request.getReferringCustomerId() != null) {
            UserDTO referringCustomer = new UserDAS().findNow(request.getReferringCustomerId());
            if (referringCustomer == null) {
                response.addErrorResponse("SAPP-ERROR-INVALID-REFERRING-CUSTOMER-ID");
                return;
            }
        }
        Integer nextInvoiceDay = holder.getPluginIntParamterByName(NEXT_INVOICE_DAY);
        if(null == nextInvoiceDay) {
            if(SapphireSignupConstants.MONTHLY_PERIOD_UNIT.equals(orderPeriodDTO.getPeriodUnit().getId())) {
                nextInvoiceDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            } else {
                nextInvoiceDay = SapphireSignupConstants.DEFAULT_NEXT_INVOICE_DAY;
            }
        }
        user.setMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay));
        user.setLanguageId(api.getCallerLanguageId());
        user.setMainRoleId(com.sapienter.jbilling.client.util.Constants.ROLE_CUSTOMER);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        user.setCurrencyId(api.getCallerCurrencyId());
        user.setEntityId(api.getCallerCompanyId());
        setAccountTypeIdOnUser(request);
        String aitGroupName = holder.getPluginParamterByName(SapphireSignupProcessTask.PARAM_AIT_GROUP_NAME.getName());
        if(StringUtils.isEmpty(aitGroupName)) {
            response.addErrorResponse(SapphireSignupProcessTask.PARAM_AIT_GROUP_NAME.getName() + " not configured for plugin "+ SapphireSignupProcessTask.class.getSimpleName());
            logger.error("parameter {} not configured in plugin {} for entity {}", paramName, SapphireSignupProcessTask.class.getSimpleName(), holder.getEntityId());
            return;
        }
        setGroupIdOnMetaFields(user, aitGroupName, response);
        if(null!= request.getReferringCustomerId()) {
            addCustomerLevelMetaField(user, REFERRING_CUSTOMER_ID_META_FIELD_NAME, request.getReferringCustomerId(), DataType.INTEGER);
        }
        // Adding pending status on newly created user.
        addCustomerLevelMetaField(user, provisioningMf.getName(),
                UserProvisioninigStatus.PENDING_ACTIVATION.getStatus(), provisioningMf.getDataType());

        List<PaymentInformationWS> userPaymentInstrument = user.getPaymentInstruments();
        if(CollectionUtils.isNotEmpty(userPaymentInstrument)) {
            PaymentInformationBackwardCompatibilityHelper.convertStringMetaFieldsToChar(userPaymentInstrument);
        }
        Integer userId = api.createUser(user);
        user.setId(userId);
        response.setUserId(userId);
        logger.debug("User created {}", userId);
    }

    private Optional<Integer> findAccountTypeIdByCode(String tableName, String code, Integer entityId) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        SqlRowSet accountTypeIdRow = jdbcTemplate.queryForRowSet(String.format(FIND_ACCOUNT_TYPE_ID_BY_PLAN_CODE_AND_ENTITY_ID_SQL, tableName), code, entityId.toString());
        if(accountTypeIdRow.next()) {
            String accountTypeId = accountTypeIdRow.getString("account_type_id");
            if(!StringUtils.isNumeric(accountTypeId)) {
                logger.error("Invalid Account type id {} found from table {}", accountTypeId, tableName);
                throw new SessionInternalError("Invalid Account type id "+ accountTypeId + " found from table "+ tableName);
            }
            return Optional.of(Integer.parseInt(accountTypeId));
        }
        return Optional.empty();
    }

    private void setAccountTypeIdOnUser(SignupRequestWS request) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        Integer entityId = getService().getCallerCompanyId();
        SqlRowSet tableNameRow = jdbcTemplate.queryForRowSet(FIND_PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME_SQL, entityId, PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME);
        if(!tableNameRow.next()) {
            logger.error("Company Level Meta Field {} not found for entity {} ", PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME, entityId);
            throw new SessionInternalError("Company Level Meta Field " + PLAN_CODE_ACCOUNT_TYPE_TABLE_NAME + " not found for entity "+ entityId);
        }

        String tableName = tableNameRow.getString("string_value");
        if(!isTablePresent(jdbcTemplate, tableName)) {
            logger.error("Data Table: {} not found for entity {} ", tableName, entityId);
            throw new SessionInternalError("Data Table:" + tableName + " not found for entity "+ entityId);
        }

        // try to find account type id by plan code
        Optional<Integer> accountTypeId = Optional.empty();
        if(StringUtils.isNotEmpty(request.getPlanCode())) {
            accountTypeId = findAccountTypeIdByCode(tableName, request.getPlanCode(), entityId);
        }

        if(!accountTypeId.isPresent()) {
            // try to find account type id by add on product code
            for(String code : request.getAddonProductCodes()) {
                accountTypeId = findAccountTypeIdByCode(tableName, code, entityId);
                if(accountTypeId.isPresent()) {
                    break;
                }
            }
        }

        if(!accountTypeId.isPresent()) {
            logger.error("No account type id {} found for entity {}", accountTypeId, entityId);
            throw new SessionInternalError("Invalid Account type id "+ accountTypeId + " found from entity "+ entityId);
        }

        if(null == new AccountTypeDAS().findNow(accountTypeId.get())) {
            logger.error("No account type id {} found for entity {}", accountTypeId, entityId);
            throw new SessionInternalError("Invalid Account type id "+ accountTypeId + " found from entity "+ entityId);
        }

        UserWS user = request.getUser();
        logger.debug("Setting Account type id {} on user {}", accountTypeId, user.getUserName());
        user.setAccountTypeId(accountTypeId.get());
    }

    private void setGroupIdOnMetaFields(UserWS user, String aitGroupName, SignupResponseWS response) {
        AccountTypeDAS accoutTypeDAS = new AccountTypeDAS();
        AccountTypeDTO accountType = accoutTypeDAS.find(user.getAccountTypeId());
        if(CollectionUtils.isNotEmpty(accountType.getInformationTypes())
                && ArrayUtils.isNotEmpty(user.getMetaFields())) {
            AccountInformationTypeDTO aitSection = new AccountInformationTypeDAS().findByName(aitGroupName, user.getEntityId(), user.getAccountTypeId());
            if(null == aitSection) {
                String errorMesaage = String.format("AIT group [%s] not found on Account type [%d] for entity [%d]",
                        aitGroupName, accountType.getId(), user.getEntityId());
                response.addErrorResponse(errorMesaage);
                logger.error(errorMesaage);
                throw new SessionInternalError(errorMesaage);
            }
            logger.debug("Setting Group id {}", aitSection.getId());
            for(MetaFieldValueWS metaFieldValue : user.getMetaFields()) {
                metaFieldValue.setGroupId(aitSection.getId());
            }
        }
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

    private boolean isTablePresent(JdbcTemplate jdbcTemplate, String tableName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
                ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null);) {
            return rs.next();
        } catch (SQLException sqlException) {
            throw new SessionInternalError(sqlException);
        }
    }

    private Integer getItemIdFromCode(String productCode) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        return jdbcTemplate.queryForObject(FIND_SUBSCRIPTION_ITEM_BY_PLAN_CODE_SQL, new Object[] { productCode }, Integer.class);
    }

    private void createAddOnProductOrder(SignupPlaceHolder holder) {
        SignupRequestWS request = holder.getSignUpRequest();
        SignupResponseWS response = holder.getSignUpResponse();
        String[] products = request.getAddonProductCodes();
        IWebServicesSessionBean api = getService();
        Integer userId = response.getUserId();
        Integer entityId = holder.getEntityId();
        OrderWS order = new OrderWS();
        order.setActiveSince(TimezoneHelper.companyCurrentDate(entityId));
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(holder.getPluginIntParamterByName(ORDER_PERIOD_ID_PARAM_NAME));
        order.setProrateFlag(false);
        order.setCurrencyId(new UserDAS().find(userId).getCurrencyId());
        SapphireHelper.setOrderStatusAsPending(order, entityId);
        List<OrderLineWS> newLines = createLinesFromProductCodes(products, response, SapphireHelper.getInventoryAllocationProductIds(holder.getParameters()));
        logger.debug("Added new lines {} for products {} ", newLines, products);
        order.setOrderLines(newLines.toArray(new OrderLineWS[0]));
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus());
        Integer orderId = api.createUpdateOrder(order, orderChanges);
        holder.addOrderId(OrderType.MONTHLY, orderId);
        logger.debug("order created {} for user {}", order.getId(), userId);
        response.addAdditionalResponse("Order Id for item codes " + Arrays.toString(products), orderId.toString());
    }

    private List<OrderLineWS> createLinesFromProductCodes(String[] addOnProducts, SignupResponseWS response, List<Integer> inventoryItemIds) {
        List<OrderLineWS> lines = new ArrayList<>();
        IWebServicesSessionBean api = getService();
        for(String addOn : addOnProducts) {
            Integer itemId = getItemIdFromCode(addOn);
            OrderLineWS line = new OrderLineWS();
            line.setItemId(itemId);
            line.setQuantity(BigDecimal.ONE);
            line.setUseItem(true);
            ItemDTOEx item = api.getItem(itemId, null, null);
            line.setTypeId(item.getOrderLineTypeId());
            line.setDescription(item.getDescription());
            if(item.isAssetEnabledItem()) {
                Integer assetId = null;
                if(CollectionUtils.isNotEmpty(inventoryItemIds)
                        && inventoryItemIds.contains(itemId)) {
                    List<AssetDTO> assets = SapphireHelper.findAssetByItemWithLock(api.getCallerCompanyId(), itemId, 1);
                    if(CollectionUtils.isEmpty(assets)) {
                        response.addErrorResponse("SAPP-ERROR-INSUFFICIENT-ASSET-FOR-ITEM[" + itemId + "]");
                        logger.debug("insufficient asset for item {}", itemId);
                        throw new SessionInternalError("insufficient asset for item " + itemId);
                    }
                    assetId = assets.get(0).getId();
                } else {
                    assetId = SapphireHelper.createAsset(itemId);
                }
                line.setAssetIds(new Integer[] {assetId});
            }
            lines.add(line);
        }
        return lines;
    }

    private Integer createSubscriptionOrderWS(SignupPlaceHolder holder) {
        if(StringUtils.isEmpty(holder.getSignUpRequest().getPlanCode())) {
            return null;
        }
        Integer orderPeriodId = holder.getPluginIntParamterByName(ORDER_PERIOD_ID_PARAM_NAME);
        SignupRequestWS request = holder.getSignUpRequest();
        IWebServicesSessionBean api = getService();
        Integer userId = request.getUser().getId();
        OrderWS order = new OrderWS();
        Integer entityId = getService().getCallerCompanyId();
        order.setActiveSince(TimezoneHelper.companyCurrentDate(entityId));
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(orderPeriodId);
        order.setProrateFlag(true);
        order.setCurrencyId(new UserDAS().find(userId).getCurrencyId());
        SapphireHelper.setOrderStatusAsPending(order, entityId);
        Integer itemId = getItemIdFromCode(request.getPlanCode());
        logger.debug("Creating Subscription order for user {} with subscription item {}", userId, itemId);
        OrderLineWS subscriptionLine = new OrderLineWS();
        subscriptionLine.setItemId(itemId);
        subscriptionLine.setQuantity(BigDecimal.ONE);
        subscriptionLine.setUseItem(true);
        ItemDTOEx item = api.getItem(itemId, null, null);
        subscriptionLine.setTypeId(item.getOrderLineTypeId());
        subscriptionLine.setDescription(item.getDescription());
        order.setOrderLines(new OrderLineWS[] { subscriptionLine });
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus());
        List<Integer> inventoryItemIds = SapphireHelper.getInventoryAllocationProductIds(holder.getParameters());
        for (OrderChangeWS orderChange : orderChanges) {
            orderChange.setAppliedManually(1);
            if (orderChange.getItemId().intValue() == itemId) {
                List<OrderChangePlanItemWS> orderChangePlanItems = new ArrayList<>();
                for(PlanItemDTO planItem : SapphireHelper.getAssetEnabledPlanItemsFromSubscriptionItem(itemId)) {
                    OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                    orderChangePlanItem.setItemId(planItem.getItem().getId());
                    orderChangePlanItem.setId(0);
                    orderChangePlanItem.setBundledQuantity(planItem.getBundle().getQuantity().intValue());
                    Integer assetEnabledItemId = planItem.getItem().getId();
                    List<Integer> assetIds = new ArrayList<>();
                    if(CollectionUtils.isNotEmpty(inventoryItemIds)
                            && inventoryItemIds.contains(assetEnabledItemId)) {
                        List<AssetDTO> assets = SapphireHelper.findAssetByItemWithLock(entityId, assetEnabledItemId, orderChangePlanItem.getBundledQuantity());
                        if(assets.size()!= orderChangePlanItem.getBundledQuantity()) {
                            SignupResponseWS response = holder.getSignUpResponse();
                            response.addErrorResponse("SAPP-ERROR-INSUFFICIENT-ASSET-FOR-ITEM[" + assetEnabledItemId + "]");
                            logger.debug("insufficient asset for item {}", assetEnabledItemId);
                            throw new SessionInternalError("insufficient asset for item " + assetEnabledItemId);
                        }
                        assetIds.addAll(assets.stream().map(AssetDTO::getId).collect(Collectors.toList()));
                    } else {
                        for(int i=0; i<orderChangePlanItem.getBundledQuantity(); i++) {
                            assetIds.add(SapphireHelper.createAsset(assetEnabledItemId));
                        }
                    }
                    logger.debug("Creating order with assets {} for user {}", assetIds, userId);
                    orderChangePlanItem.setAssetIds(ArrayUtils.toPrimitive(assetIds.toArray(new Integer[0])));
                    orderChangePlanItems.add(orderChangePlanItem);
                }
                orderChange.setOrderChangePlanItems(orderChangePlanItems.toArray(new OrderChangePlanItemWS[0]));
            }
        }
        order.setId(api.createUpdateOrder(order, orderChanges));
        holder.addOrderId(OrderType.MONTHLY, order.getId());
        holder.addProductCodeAndOrderId(request.getPlanCode(), order.getId());
        logger.debug("subscription order created {} for user {}", order.getId(), userId);
        return order.getId();
    }

    private void createOrders(SignupPlaceHolder holder, boolean isOneTimeCharges) {
        SignupResponseWS response = holder.getSignUpResponse();
        Integer userId = response.getUserId();
        IWebServicesSessionBean api = getService();
        Integer entityId = api.getCallerCompanyId();
        SignupRequestWS request = holder.getSignUpRequest();
        String[] productCodes = isOneTimeCharges ? request.getOneTimeCharges() : request.getAddonProductCodes();
        for(String productCode : productCodes) {
            OrderWS order = new OrderWS();
            order.setActiveSince(TimezoneHelper.companyCurrentDate(entityId));
            order.setUserId(userId);
            order.setBillingTypeId(isOneTimeCharges ? Constants.ORDER_BILLING_POST_PAID : Constants.ORDER_BILLING_PRE_PAID);
            order.setPeriod(isOneTimeCharges ? Constants.ORDER_PERIOD_ONCE :
                holder.getPluginIntParamterByName(ORDER_PERIOD_ID_PARAM_NAME));
            order.setProrateFlag(false);
            order.setCurrencyId(new UserDAS().find(userId).getCurrencyId());
            OrderStatusDAS orderStatusDAS = new OrderStatusDAS();
            order.setStatusId(orderStatusDAS.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId));
            Integer itemId = getItemIdFromCode(productCode);
            logger.debug("Creating order for user {} with subscription item {}", userId, itemId);
            OrderLineWS orderLine = new OrderLineWS();
            orderLine.setItemId(itemId);
            orderLine.setQuantity(BigDecimal.ONE);
            orderLine.setUseItem(true);
            ItemDTOEx item = api.getItem(itemId, null, null);
            orderLine.setDescription(item.getDescription());
            orderLine.setTypeId(item.getOrderLineTypeId());
            order.setOrderLines(new OrderLineWS[] { orderLine });
            OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus());
            for(OrderChangeWS orderChange : orderChanges) {
                orderChange.setAppliedManually(1);
            }
            order.setId(api.createUpdateOrder(order, orderChanges));
            logger.debug("order created {} for user {}", order.getId(), userId);
            response.addAdditionalResponse("Order Id for item code " + productCode, order.getId().toString());
            holder.addOrderId(OrderType.ONE_TIME, order.getId());
        }
    }

    private Integer getOrderChangeApplyStatus() {
        OrderChangeStatusWS[] list = getService().getOrderChangeStatusesForCompany();
        for(OrderChangeStatusWS orderChangeStatus : list) {
            if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return orderChangeStatus.getId();
            }
        }
        throw new SessionInternalError("No order Change status found!");
    }

    private void updateOneTimeOrderStatusToActive(Integer orderId, Integer entityId) {
        OrderDTO order = new OrderDAS().find(orderId);
        Integer statusId = new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId);
        for(OrderDTO childOrder : order.getChildOrders()) {
            if(Constants.ORDER_PERIOD_ONCE.equals(childOrder.getPeriodId())) {
                childOrder.setStatusId(statusId);
            }
        }
    }

    @Override
    public void doExecute(SignupPlaceHolder holder) {
        // execute create user and order.
        SignupResponseWS response = holder.getSignUpResponse();
        SignupRequestWS request = holder.getSignUpRequest();
        try {
            validateUserAccount(holder);
            if(response.hasError()) {
                return;
            }
        } catch(Exception ex) {
            logger.error("duplicate account check failed!", ex);
            response.addErrorResponse("SAPP-ERROR-DUPLICATE-ACCOUNT-CHECK-FAILED");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return;
        }
        try {
            createUser(holder); // creating user.
            if(response.hasError()) {
                return;
            }
        } catch(Exception ex) {
            logger.error("User Creation failed!", ex);
            response.addErrorResponse("SAPP-ERROR-USER-CREATION-FAILED");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return;
        }
        try {
            // creating subscription order for user.
            Integer orderId = createSubscriptionOrderWS(holder);
            if(null!=orderId) {
                response.addAdditionalResponse("Subscription Order Id", orderId.toString());
                updateOneTimeOrderStatusToActive(orderId, holder.getEntityId());
            }
        } catch(Exception ex) {
            logger.error("Subscription order creation failed ", ex);
            response.addErrorResponse("SAPP-ERROR-PLAN-ORDER-CREATION-FAILED");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ;
        }

        try {
            //creating orders for add on products.
            if(ArrayUtils.isNotEmpty(request.getAddonProductCodes())) {
                createAddOnProductOrder(holder);
            }
        } catch(Exception ex) {
            logger.error("Add On Product's order creation failed ", ex);
            response.addErrorResponse("SAPP-ERROR-ADD-ON-SERVICE-ORDER-CREATION-FAILED");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ;
        }

        try {
            // creating one times orders.
            if(ArrayUtils.isNotEmpty(request.getOneTimeCharges())) {
                createOrders(holder, true);
            }
        } catch(Exception ex) {
            logger.error("One time charged order creation failed ", ex);
            response.addErrorResponse("SAPP-ERROR-ONE-TIME-CHARGES-ORDER-CREATION-FAILED");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ;
        }
    }
}
