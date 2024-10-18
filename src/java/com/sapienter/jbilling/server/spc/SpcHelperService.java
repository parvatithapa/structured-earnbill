package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.IntegerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchange;
import com.sapienter.jbilling.server.integration.db.OutBoundInterchangeDAS;
import com.sapienter.jbilling.server.integration.db.Status;
import com.sapienter.jbilling.server.item.AssetAssignmentDAS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoBL;
import com.sapienter.jbilling.server.notification.db.InvoiceEmailProcessInfoDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.spc.billing.SPCUserFilterTask;
import com.sapienter.jbilling.server.spc.wookie.crm.SendSms;
import com.sapienter.jbilling.server.spc.wookie.crm.WookieNotification;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionNotificationEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.tasks.EventBasedCustomNotificationTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;

@Transactional
public class SpcHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private EventLogger eLogger = EventLogger.getInstance();

    private static final String CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER =
            " Custom notification id: {} does not exist for the user id {} ";
    private static final Integer DEFAULT_TIME_OUT = 1000;
    @Resource(name = "namedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Resource
    private INotificationSessionBean notificationSession;
    @Resource
    private SessionFactory sessionFactory;
    @Resource
    private OutBoundInterchangeDAS outBoundInterchangeDAS;
    @Resource
    private CustomerUsagePoolDAS customerUsagePoolDAS;
    @Resource
    private OrderDAS orderDAS;
    private Cache<PriceCacheKey, BigDecimal> priceCache;

    @PostConstruct
    void init() {
        priceCache = CacheBuilder.newBuilder()
                .concurrencyLevel(10)
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final Integer PLUGGABLE_TASK_BILLABLE_USER_FILTER = 34;

    private static final String FETCH_TARIFF_CODE_AND_CONSUMPTION_PERCENTAGE_SQL =
            "SELECT id, plan_id, tariff_codes_note, consumption_percentages, free_amount, credit_item_id, credit_pool_name FROM %s WHERE plan_id = ?";

    private static final String PLAN_BASED_FREE_CALL_INFO_TABLE_NAME_MF = "plan based free call info table name";
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Integer WEB_SERVICE_TIMEOUT = 10000;
    private static final String CREDIT_POOL = "Credit Pool";
    private static final String OPTUS_MUR = "Optus Mur";
    private static final String USAGE_POOL = "Usage Pool";
    private static final String RECORDS_PROCESSED_SQL =
            "SELECT records_processed "
            + "FROM jbilling_mediation_process "
            + "WHERE file_name =:fileName "
            + "ORDER BY star_date DESC "
            + "LIMIT 1 ";
    private static final String INSERT_MEDIATION_RECON_DATA = "INSERT into mediation_reconciliation_history (processed_dir,processed_archive,processed_file,is_verified,retry_count) VALUES (?,?,?,?,?) ";
    private static final String DIFFERENCE_FROM_RECONCILIATION_HISTORY_SQL = "SELECT * FROM mediation_reconciliation_history WHERE processed_archive =:fileName ";
    private static final String UPDATE_RECONCILIATION_HISTORY_SQL = "UPDATE mediation_reconciliation_history SET is_verified = ?, retry_count = ? WHERE processed_archive = ? ";

    /**
     * fetches {@link SpcCreditPoolInfo} for given plan id.
     * @param planId
     * @return
     * @throws PluggableTaskException
     */
    public List<SpcCreditPoolInfo> getCreditPoolsForPlan(Integer planId, String tableName) {
        String query = String.format(FETCH_TARIFF_CODE_AND_CONSUMPTION_PERCENTAGE_SQL, tableName);
        return jdbcTemplate.query(query, new Object[] { planId.toString() }, (rs, rowNum) ->  new SpcCreditPoolInfo(rs.getInt(1), rs.getInt(2),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)));
    }

    /**
     * fetches {@link Plan rate} for given product name.
     * @param productName
     * @return
     * @throws PluggableTaskException
     */
    public BigDecimal getRateForPlanItem(String productCode, String tableName) {
        String query = "SELECT charge FROM "
                + tableName
                + " WHERE name like (:productCode) "
                + " ORDER BY id DESC LIMIT 1";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("productCode", productCode);
        return namedParameterJdbcTemplate.queryForObject(query, parameters, BigDecimal.class);
    }

    private static final String GET_CREDIT_ORDER_BY_SUBSCRIPTION_ORDER_SQL =
            "SELECT subOrder.order_id "
                    + "FROM   (SELECT po.id             AS order_id, "
                    + "               mfv.integer_value AS value "
                    + "        FROM   purchase_order po "
                    + "               INNER JOIN order_meta_field_map omfm   ON po.id = omfm.order_id "
                    + "               INNER JOIN meta_field_value mfv        ON mfv.id = omfm.meta_field_value_id "
                    + "               INNER JOIN meta_field_name mfn         ON mfn.id = mfv.meta_field_name_id "
                    + "               INNER JOIN order_status os             ON os.id = po.status_id"
                    + "        WHERE  po.user_id = ? "
                    + "           AND os.order_status_flag = 0 "
                    + "           AND po.deleted = 0"
                    + "           AND mfn.NAME = ?) AS subOrder, "
                    + "       (SELECT po.id            AS order_id, "
                    + "               mfv.string_value AS value "
                    + "        FROM   purchase_order po "
                    + "               INNER JOIN order_meta_field_map omfm  ON po.id = omfm.order_id "
                    + "               INNER JOIN meta_field_value mfv       ON mfv.id = omfm.meta_field_value_id "
                    + "               INNER JOIN meta_field_name mfn        ON mfn.id = mfv.meta_field_name_id "
                    + "               INNER JOIN order_status os             ON os.id = po.status_id"
                    + "        WHERE  po.user_id = ? "
                    + "           AND os.order_status_flag = 0 "
                    + "           AND po.deleted = 0 "
                    + "           AND mfn.NAME = ?) AS creditPoolName "
                    + " WHERE  subOrder.order_id = creditPoolName.order_id "
                    + "   AND subOrder.value = ? "
                    + "   AND creditPoolName.value = ?";

    public Set<Integer> getCreditOrderForSubscriptionOrder(Integer userId, String orderIdMfName, String tariffCodeMfName, 
    		Integer subscriptionOrderId, String creditPoolName) {
        long queryStartTime = System.currentTimeMillis();
        SqlRowSet row = jdbcTemplate.queryForRowSet(GET_CREDIT_ORDER_BY_SUBSCRIPTION_ORDER_SQL, userId, orderIdMfName, userId,
                tariffCodeMfName, subscriptionOrderId, creditPoolName);
        logger.debug("Time taken to fetch credit orders: {} miliseconds for subscriptionOrderId {}",
                (System.currentTimeMillis() - queryStartTime), subscriptionOrderId);
        Set<Integer> set = new HashSet<>();
        while(row.next()) {
            set.add(row.getInt(1));
        }
        return set;
    }

    private static final String FETCH_USAGE_FOR_CALL_IDENTIFIER_TARIFF_CODE_JMR_SQL =
            " SELECT COALESCE(SUM(jmr.RATED_PRICE_WITH_TAX), 0) "
                    +" FROM purchase_order po "
                    +" INNER JOIN order_line ol ON po.id = ol.order_id "
                    +" INNER JOIN order_line_itemized_usage oliu ON ol.id = oliu.order_line_id "
                    +" INNER JOIN JBILLING_MEDIATION_RECORD jmr ON JMR.ORDER_LINE_ID=OL.ID "
                    +" WHERE po.is_mediated = 't' "
                    +" AND po.deleted = 0 "
                    +" AND po.user_id = :userId "
                    +" AND po.id = :orderId "
                    +" AND ol.deleted = 0 "
                    +" AND po.status_id in (SELECT id "
                    +" FROM order_status "
                    +" WHERE order_status_flag = 0) "
                    +" AND po.active_since < (:nextInvoiceDate) "
                    +" AND ol.call_identifier IN (:callIdentifiers) "
                    +" AND oliu.separator IN (:tariffCodes) "
                    +" AND jmr.status = 'PROCESSED' "
                    +" AND decodeTariffCode(jmr.pricing_fields)=oliu.separator";

    private static final String FETCH_USAGE_FOR_CALL_IDENTIFIER_TARIFF_CODE_OLIU_SQL_CURRENT_PERIOD =
            "SELECT ol.item_id,COALESCE(SUM(oliu.amount), 0) as usage "
                    + "FROM purchase_order po "
                    + "INNER JOIN order_line ol ON po.id = ol.order_id "
                    + "INNER JOIN order_line_itemized_usage oliu ON ol.id = oliu.order_line_id "
                    + "WHERE po.user_id =:userId "
                    + "AND po.is_mediated = 't' "
                    + "AND po.deleted = 0 "
                    + "AND ol.deleted = 0 "
                    + "AND po.status_id in "
                    + "(SELECT id "
                    + "FROM order_status "
                    + "WHERE order_status_flag = 0) "
                    + "AND po.active_since < (:nextInvoiceDate) "
                    + "AND ol.call_identifier IN (:callIdentifiers) "
                    + "AND oliu.separator IN (:tariffCodes) group by ol.item_id";
    
    private static final String FETCH_USAGE_FOR_CALL_IDENTIFIER_TARIFF_CODE_OLIU_SQL_NEXT_PERIOD =
            "SELECT ol.item_id,COALESCE(SUM(oliu.amount), 0) as usage "
                    + "FROM purchase_order po "
                    + "INNER JOIN order_line ol ON po.id = ol.order_id "
                    + "INNER JOIN order_line_itemized_usage oliu ON ol.id = oliu.order_line_id "
                    + "WHERE po.user_id =:userId "
                    + "AND po.is_mediated = 't' "
                    + "AND po.deleted = 0 "
                    + "AND ol.deleted = 0 "
                    + "AND po.status_id in "
                    + "(SELECT id "
                    + "FROM order_status "
                    + "WHERE order_status_flag = 0) "
                    + "AND po.active_since >= (:nextInvoiceDate) "
                    + "AND ol.call_identifier IN (:callIdentifiers) "
                    + "AND oliu.separator IN (:tariffCodes) group by ol.item_id";

    public BigDecimal getUsageForAssetsAndTariffCodesFromJMR(List<String> callIdentifiers,Integer orderId, List<String> tariffCodes, Integer userId, Date nextInvoiceDate) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("callIdentifiers", callIdentifiers);
        parameters.addValue("orderId", orderId);
        parameters.addValue("tariffCodes", tariffCodes);
        parameters.addValue("userId", userId);
        parameters.addValue("nextInvoiceDate", nextInvoiceDate);
        
        return namedParameterJdbcTemplate.queryForObject(FETCH_USAGE_FOR_CALL_IDENTIFIER_TARIFF_CODE_JMR_SQL, parameters, BigDecimal.class);
    }

    public List<Map<String, Object>> getUsageForAssetsAndTariffCodes(List<String> callIdentifiers, List<String> tariffCodes, Date nextInvoiceDate, Date eventDate, Integer userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("callIdentifiers", callIdentifiers);
        parameters.addValue("tariffCodes", tariffCodes);
        parameters.addValue("nextInvoiceDate", nextInvoiceDate);
        parameters.addValue("userId", userId);

        return namedParameterJdbcTemplate.queryForList(
                ((eventDate.before(nextInvoiceDate)) ? FETCH_USAGE_FOR_CALL_IDENTIFIER_TARIFF_CODE_OLIU_SQL_CURRENT_PERIOD : 
                    FETCH_USAGE_FOR_CALL_IDENTIFIER_TARIFF_CODE_OLIU_SQL_NEXT_PERIOD), parameters);
    }
    /**
     * checks table present in data base and return true
     * if present else throws exception.
     * @param tableName
     * @return
     */
    public boolean isTablePresent(String tableName) {
        Assert.notNull(tableName, "provide tableName");
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
                if(rs.next()) {
                    return Boolean.TRUE;
                }
                throw new SessionInternalError("table " + tableName + " not found!");
            }
        } catch (SQLException sqlException) {
            throw new SessionInternalError(sqlException);
        }
    }

    /**
     * validates and return {@link MetaField} for given MetaField Name.
     * @param entityId
     * @param mfName
     * @param entityType
     * @return
     */
    public MetaField validateAndGetMetaField(Integer entityId, String mfName, EntityType entityType, DataType dataType) {
        Assert.notNull(mfName, "provide mfName");
        Assert.notNull(entityType, "provide entityType");
        MetaField metaField = MetaFieldBL.getFieldByName(entityId, new EntityType[] { entityType } , mfName);
        if(null == metaField) {
            throw new SessionInternalError("MetaField "+ mfName + " not found on "
                    + ""+ entityType.name() + " for entity "+ entityId);
        }
        if(!metaField.getDataType().equals(dataType)) {
            throw new SessionInternalError("MetaField "+ mfName + " incorrect DataType " + metaField.getDataType()
                    + " found expected data type is "+ dataType.name() + " for entity "+ entityId);
        }
        return metaField;
    }

    @Async("asyncTaskExecutor")
    public void notifyToUser(Integer entityId, Event event, Map<String, String> params) {
        logger.debug("Notification Processing for Event {}",event.getName());
        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<String, String> notificationsParams = getNotificationParameters(event);
        if(event instanceof SpcCreditPoolNotificationEvent) {
            sendCreditPoolNotificationToUser((SpcCreditPoolNotificationEvent) event, params,notificationsParams);
        } else if(event instanceof OptusMurNotificationEvent) {
            sendOptusMurNotificationToUser((OptusMurNotificationEvent) event, params,notificationsParams);
        } else if(event instanceof UsagePoolConsumptionNotificationEvent) {
            sendSpcNotificationToUser((UsagePoolConsumptionNotificationEvent) event, params, notificationsParams);
        }
        logger.debug("Total Time taken for event {} is {} MILLISECONDS",event.getName(),stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    private static final String FETCH_PLAN_BASED_FREE_CALL_INFO_SQL =
            "SELECT plan_code, free_call_count, item_codes FROM %s WHERE plan_code = ?";

    /**
     * fetches {@link PlanBasedFreeCallInfo} for given asset.
     * @param order
     * @return
     */
    public List<PlanBasedFreeCallInfo> getPlanBasedFreeCallInfoForAsset(OrderDTO order) {
        PlanDTO plan = order.getPlanFromOrder();
        if(null == plan) {
            logger.debug("no plan found for order {}", order.getId());
            return Collections.emptyList();
        }
        CompanyDTO entity = order.getBaseUserByUserId().getCompany();
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> planBasedFreeCallInfoTableName = entity.getMetaField(PLAN_BASED_FREE_CALL_INFO_TABLE_NAME_MF);
        if(null == planBasedFreeCallInfoTableName || planBasedFreeCallInfoTableName.isEmpty()) {
            logger.debug("{} not set on company level meta field for entity {}",
                    PLAN_BASED_FREE_CALL_INFO_TABLE_NAME_MF, entity.getId());
            return Collections.emptyList();
        }
        String tableName = planBasedFreeCallInfoTableName.getValue();
        isTablePresent(tableName);
        String query = String.format(FETCH_PLAN_BASED_FREE_CALL_INFO_SQL, tableName);
        return jdbcTemplate.query(query, new Object[] { plan.getItem().getInternalNumber() }, (rs, rowNum) -> new PlanBasedFreeCallInfo(rs.getString(1), Long.parseLong(rs.getString(2)),
                Arrays.stream(rs.getString(3).split(",")).collect(Collectors.toList()), entity.getId()));
    }

    /**
     * Creates {@link EventBasedCustomNotificationTask} instance with given entityId and Params.
     * @param entityId
     * @param params
     * @return
     */
    private EventBasedCustomNotificationTask createEventBasedCustomNotificationTaskInstance(Integer entityId, Map<String, String> params) {
        Assert.notNull(entityId, "entityId required!");
        Assert.notEmpty(params, "parameter required!");
        EventBasedCustomNotificationTask eventBasedCustomNotificationTask = new EventBasedCustomNotificationTask();
        eventBasedCustomNotificationTask.setParameters(params);
        eventBasedCustomNotificationTask.setEntityId(entityId);
        return eventBasedCustomNotificationTask;
    }

    /**
     * Sends credit pool notification to user.
     * @param spcCreditPoolNotificationEvent
     * @param params
     */
    @SuppressWarnings({ "unchecked"})
    private void sendCreditPoolNotificationToUser(SpcCreditPoolNotificationEvent
            spcCreditPoolNotificationEvent, Map<String, String> params,Map<String, String> notificationsParams) {
        Integer userId = spcCreditPoolNotificationEvent.getJmr().getUserId();
        for (BigDecimal utilizedPercentage : spcCreditPoolNotificationEvent.getUtilizedPercentages()) {
            if (params.containsKey(String.valueOf(utilizedPercentage))) {
                Integer notificationMessageTypeId = Integer.parseInt(params
                        .get(String.valueOf(utilizedPercentage)));
                if(null == notificationMessageTypeId) {
                    logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
                    return;
                }
                logger.debug("Notifying user: {} for a credit pool consumption notification event", userId);
                notify(notificationMessageTypeId, userId, message -> {
                    message.addParameter(SPCConstants.PERCENTAGE_CONSUMPTION_PARAM, utilizedPercentage);
                    message.getParameters().putAll(notificationsParams);
                }
                , params,spcCreditPoolNotificationEvent);
            }
        }
    }

    /**
     * Sends notification to user.
     * @param notificationTypeId
     * @param userId
     * @param applyParameters
     */
    public void notify(Integer notificationTypeId, Integer userId, Consumer<MessageDTO> applyParameters, Map<String, String> params) {
        try {
            UserBL userBL = new UserBL(userId);
            MessageDTO message = new NotificationBL().getCustomNotificationMessage(
                    notificationTypeId,
                    userBL.getEntityId(userId), userId,
                    userBL.getLanguage());
            // add additional parameters.
            if(null != applyParameters){
                applyParameters.accept(message);
            }

            if(StringUtils.isNotEmpty(params.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName()))){
                sendNotificationExternalApi(userId,userBL.getEntityId(userId), message, params);
            } else{
                notificationSession.notify(userId, message);
            }
        } catch(NotificationNotFoundException notificationNotFound) {
            logger.error(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationTypeId, userId);
        }
    }

   
    
    private WookieNotification wookieSmsNotificationRequest(Map parameter) {
        WookieNotification wookieSmsNotification = new WookieNotification();
        String crmPrefixes = (String) parameter.get("crmPrefixes");

        if (null != crmPrefixes) {
            List<String> crmPrefixList = Splitter.on(",").trimResults().splitToList(crmPrefixes);
            wookieSmsNotification.setCrmAccountRecordId(crmPrefixList.get(0) + parameter.get(SPCConstants.CRM_ACCOUNT_ID));
            wookieSmsNotification.setCrmOrderRecordId(crmPrefixList.get(1) + parameter.get(SPCConstants.CRM_ORDER_ID));
            wookieSmsNotification.setCrmServiceRecordId(crmPrefixList.get(2) + parameter.get(SPCConstants.SERVICE_IDS_PARAM));
        }else {
            wookieSmsNotification.setCrmAccountRecordId(String.valueOf(parameter.get(SPCConstants.CRM_ACCOUNT_ID)));
            wookieSmsNotification.setCrmOrderRecordId(String.valueOf(parameter.get(SPCConstants.CRM_ORDER_ID)));
            wookieSmsNotification.setCrmServiceRecordId(String.valueOf(parameter.get(SPCConstants.SERVICE_IDS_PARAM)));
        }
        wookieSmsNotification.setVendorReference(String.valueOf(parameter.get(SPCConstants.VENDER_REFEREBCE)));
        SendSms sendSms = new SendSms();
        sendSms.setAutoAddComment(true);
        NotificationBL notificationBL = new NotificationBL();
        String dataBoost;
        String message;
        BigDecimal percentageConsumption = new BigDecimal(String.valueOf(parameter.get(SPCConstants.PERCENTAGE_CONSUMPTION_PARAM)));
        if (!String.valueOf(parameter.get(SPCConstants.DATA_BOOST_PARAM)).equals("null") && percentageConsumption.compareTo(ONE_HUNDRED) == 0) {
            dataBoost = getDataBoost((String) parameter.get(SPCConstants.DATA_BOOST_PARAM));
            message = notificationBL.getNotificationAlertMessage(String.valueOf(parameter.get(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM)),
                    String.valueOf(parameter.get(SPCConstants.PLAN_CODE_PARAM)), dataBoost);
        } else {
            message = notificationBL.getNotificationAlertMessage(String.valueOf(parameter.get(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM)),
                    String.valueOf(parameter.get(SPCConstants.PLAN_CODE_PARAM)), String.valueOf(parameter.get(SPCConstants.PERCENTAGE_CONSUMPTION_PARAM)));
        }
        if (message == null) {
            return null;
        }
        logger.debug("SMS body is {}", message);
        sendSms.setBodyText(message);
        wookieSmsNotification.setSendSms(sendSms);
        return wookieSmsNotification;
    }

    private String getDataBoost(String dataBoost) {
        String dataBoostNumber = dataBoost.replaceAll("[^0-9]", "");
        RuleBasedNumberFormat ordinal = new RuleBasedNumberFormat(Locale.UK, RuleBasedNumberFormat.SPELLOUT);
        return "100% + " + StringUtils.capitalize(ordinal.format(Integer.parseInt(dataBoostNumber), "%spellout-ordinal"))
                + " Boost";
    }

    private void sendNotificationExternalApi(Integer userId,Integer entityId, MessageDTO message, Map<String, String> params) {
        //Code for calling external API service
        OutBoundInterchange interchange = new OutBoundInterchange();
        try {
            String methodName = params.get(SpcNotificationTask.PARAM_SEND_NOTIFICATION_METHOD_NAME.getName());
            if (methodName != null && !methodName.isEmpty()) {
                interchange.setMethodName(methodName);
            } else {
                interchange.setMethodName("sendNotificationExternalApi");
            }
            interchange.setHttpMethod(HttpMethod.POST);
            interchange.setUserId(userId);
            interchange.setCompany(new CompanyDTO(entityId));
            HashMap parameters = message.getParameters();
            if(!Strings.isNullOrEmpty(params.get(SpcNotificationTask.PARAMETER_CRM_PAYLOAD_PREFIX.getName()))){
                parameters.put("crmPrefixes",params.get(SpcNotificationTask.PARAMETER_CRM_PAYLOAD_PREFIX.getName()));
            }
            WookieNotification notificationRequest = wookieSmsNotificationRequest(parameters);
            if (null==notificationRequest){
                logger.error("SMS body not found for Plan No {} in Data Table {} for Order Id {} of User Id {}",
                        parameters.get(SPCConstants.PLAN_CODE_PARAM),
                        parameters.get("orderId"),
                        parameters.get(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM),userId);
                return;
            }
            interchange.setRequest(OBJECT_MAPPER.writeValueAsString(notificationRequest));
            logger.debug("posting request {} for entity {}", interchange, userId);
            String url = params.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName());
            validateParameter(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName(), url);

            HttpEntity<String> request = new HttpEntity<>(interchange.getRequest(), getHeader(params));

            ResponseEntity<String> response = restTemplate(getTimeOut(params))
                    .exchange(url, interchange.getHttpMethod(),request, String.class);
            interchange.setResponse(response.getBody());
            interchange.setStatus(Status.SENT);
            logger.debug("Response {}", interchange.getResponse());
        } catch (HttpClientErrorException | HttpServerErrorException error) {
            interchange.setResponse(error.getResponseBodyAsString());
            interchange.setStatus(Status.SEND_FAILED);
            logger.error("Response from Wookie is {}", interchange.getResponse());
        } catch (Exception e) {
            String errorResponse = ExceptionUtils.getMessage(e);
            interchange.setResponse(errorResponse);
            interchange.setStatus(Status.SEND_FAILED);
            logger.error("Send notification failed to Wookie ", e);
        }
        outBoundInterchangeDAS.save(interchange);
    }

    private String getVendorReferenceId(UserDTO userDTO){
        Stopwatch stopwatch = Stopwatch.createStarted();
        String vendorId;
        String query;
        MapSqlParameterSource parameters;
        query = "SELECT MAX(id) FROM jbilling_mediation_record WHERE  user_id=:user_id AND status='PROCESSED'";

        parameters = new MapSqlParameterSource();
        parameters.addValue("user_id", userDTO.getId());
        vendorId = namedParameterJdbcTemplate.queryForObject(query, parameters, String.class);
        logger.debug("Time taken to fetch vendorReferenceId is {} MILLISECONDS",stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return vendorId;
    }

    private Map<String,String> getNotificationParameters(Event event) {
        Map<String,String> params = new HashMap<>();
        logger.debug("Extracting Notification parameter for {}",event.getName());
        if (event instanceof UsagePoolConsumptionNotificationEvent ) {
            UsagePoolConsumptionNotificationEvent usagePoolConsumptionNotificationEvent = (UsagePoolConsumptionNotificationEvent) event;
            CustomerUsagePoolDTO customerUsagePool = customerUsagePoolDAS.find(usagePoolConsumptionNotificationEvent.getCustomerUsagePoolId());
            UserDTO user = customerUsagePool.getCustomer().getBaseUser();
            OrderDTO planOrder = customerUsagePool.getOrder();
            params.putAll(getPlanDetailsParameters(planOrder.getId(),user.getId()));
            params.put(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM, SPCConstants.WOOKIE_USAGE_POOL_SMS_TABLE);
            params.put(SPCConstants.DATA_BOOST_PARAM, String.valueOf(usagePoolConsumptionNotificationEvent.getCurrentDataBoost()));
            params.put(SPCConstants.VENDER_REFEREBCE, getVendorReferenceId(user));
        }
        if (event instanceof OptusMurNotificationEvent) {
            OptusMurNotificationEvent optusMurNotificationEvent = (OptusMurNotificationEvent) event;
            params.put(SPCConstants.DATA_BOOST_PARAM, String.valueOf(optusMurNotificationEvent.getCurrentUsagePoolName()));
            params.putAll(getPlanDetailsParameters(optusMurNotificationEvent.getSubscriptionOrderId(),optusMurNotificationEvent.getUserId()));
            params.put(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM,SPCConstants.WOOKIE_OPTUS_MUR_SMS_TABLE);
            params.put(SPCConstants.VENDER_REFEREBCE, String.valueOf(optusMurNotificationEvent.getJmr().getId()));
        }
        if (event instanceof SpcCreditPoolNotificationEvent) {
            SpcCreditPoolNotificationEvent spcCreditPoolNotificationEvent = (SpcCreditPoolNotificationEvent) event;
            SpcCreditPoolInfo creditPool = spcCreditPoolNotificationEvent.getCreditPoolToUse();
            params.putAll(getPlanDetailsParameters(spcCreditPoolNotificationEvent.getPlanOrderId(),spcCreditPoolNotificationEvent.getJmr().getUserId()));
            params.put(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM,SPCConstants.WOOKIE_CREDIT_POOL_SMS_TABLE);
            params.put(SPCConstants.VENDER_REFEREBCE, String.valueOf(spcCreditPoolNotificationEvent.getJmr().getId()));
        }
        return params;
    }


    private String getServiceIdsForAssetList(OrderDTO planOrder) {
    	List<AssetDTO> assetDTOs = planOrder.getAssets();
    	logger.debug("Fetching order level asset ID ");
        if (CollectionUtils.isEmpty(assetDTOs)) {
        	AssetDTO assetDTO = new AssetAssignmentDAS().getAssetsFromAssignmentsForOrder(planOrder.getId());
        	if (null != assetDTO) {
        		assetDTOs.add(assetDTO);
        	}
        	assetDTOs.stream()
	        .forEach(asset -> logger.debug("Asset id: {}, identifier: {}, from asset assignment", 
	        		asset.getId(), asset.getIdentifier()));
            if (CollectionUtils.isEmpty(assetDTOs)) {
            	return StringUtils.EMPTY;
            }
        }
        StringJoiner joiner = new StringJoiner(",");
        for (AssetDTO assetDTO : assetDTOs) {
            for (MetaFieldValue<?> fieldValue : assetDTO.getMetaFields()) {
                if (null != fieldValue && null != fieldValue.getValue() && fieldValue.getFieldName().equals(SPCConstants.SERVICE_ID)) {
                    joiner.add(String.valueOf(fieldValue.getValue()));
                }
            }
        }
        return joiner.toString();
    }

    private Map<String,String> getPlanDetailsParameters(Integer planOrderId,Integer userId) {
        logger.debug("Fetching plan and crm level fields");
        Map<String,String> params = new HashMap<>();
        OrderDTO planOrder = new OrderBL(planOrderId).getEntity();
        UserDTO user = new UserBL(userId).getEntity();
        CustomerDTO customer = user.getCustomer();
        params.put(SPCConstants.PLAN_CODE_PARAM, (null != planOrder.getPlanFromOrder() ? planOrder.getPlanFromOrder().getItem().getInternalNumber() : StringUtils.EMPTY));

        params.put(SPCConstants.SERVICE_IDS_PARAM, planOrder != null ? getServiceIdsForAssetList(planOrder) : StringUtils.EMPTY);
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> crmAccountId = customer.getMetaField(SPCConstants.CRM_ACCOUNT_ID);
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> crmOrderId = planOrder.getMetaField(SPCConstants.CRM_ORDER_ID);
        params.put(SPCConstants.CRM_ACCOUNT_ID, crmAccountId != null ? crmAccountId.getValue() : null);
        params.put(SPCConstants.CRM_ORDER_ID, crmOrderId != null ? crmOrderId.getValue() : null);
        params.put("orderId", String.valueOf(planOrder.getId()));
        return params;
    }

    /**
     * Creates Auth Header for given credential
     *
     * @return
     * @param params
     */
    private HttpHeaders getHeader(Map<String, String> params) {
        //check access_token endpoint has to use or not
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (Boolean.parseBoolean(params.get(SpcNotificationTask.PARAMETER_ENABLE_ACCESS_TOKEN.getName()))){
            String username = params.get(SpcNotificationTask.PARAMETER_USERNAME.getName());
            validateParameter(SpcNotificationTask.PARAMETER_USERNAME.getName(), username);
            String password = params.get(SpcNotificationTask.PARAMETER_PASSWORD.getName());
            validateParameter(SpcNotificationTask.PARAMETER_PASSWORD.getName(), password);
            String accessTokenUrl = params.get(SpcNotificationTask.PARAMETER_ACCESS_TOKEN_URL.getName());
            validateParameter(SpcNotificationTask.PARAMETER_ACCESS_TOKEN_URL.getName(), accessTokenUrl);
            try {
                // get token from token cache.
                String token = SpcNotificationTask.tokenCache().get(TokenKey.of(
                        username, password, accessTokenUrl)).getAccessToken();
                headers.set("Authorization", "Bearer " + token);
            } catch (ExecutionException ex) {
                throw new SessionInternalError("error in getHeader", ex);
            }
        }
        return HttpHeaders.readOnlyHttpHeaders(headers);
    }

    private static ClientHttpRequestFactory getClientHttpRequestFactory(final int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        return new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().setDefaultRequestConfig(config).build());
    }

    private static RestTemplate restTemplate(int timeout) {
        return new RestTemplate(getClientHttpRequestFactory(timeout));
    }

    private Integer getTimeOut(Map<String, String> parameters) {
        String value = parameters.get(SpcNotificationTask.PARAM_TIME_OUT.getName());
        Integer timeout;
        if (StringUtils.isEmpty(value) || !NumberUtils.isDigits(value)) {
            timeout = DEFAULT_TIME_OUT;
        } else {
            timeout = Integer.parseInt(value)*1000;
            if(timeout < DEFAULT_TIME_OUT) {
                timeout = DEFAULT_TIME_OUT;
            }
        }
        logger.debug("CRM Sms Notification timeout {}", timeout);
        return timeout;
    }

    private void validateParameter(String paramName, Object paramValue) {
        Assert.notNull(paramValue, String.format("Parameter [%s] is null", paramName));
    }

    /**
     * Sends Optus Mur notification to user.
     * @param optusMurNotificationEvent
     */
    @SuppressWarnings({ "unchecked"})
    private void sendOptusMurNotificationToUser(OptusMurNotificationEvent optusMurNotificationEvent, Map<String, String> params,Map<String, String> notificationsParams) {
        Map<String, String> parameters = optusMurNotificationEvent.getNotificationParams();
        if(MapUtils.isEmpty(parameters)) {
            logger.debug("Parameters not found for fireOptusMurConsumptionNotification");
            return;
        }
        List<BigDecimal> utilizedPercentages = optusMurNotificationEvent.getUtilizedPercentages();
        Integer userId = optusMurNotificationEvent.getJmr().getUserId();
        for (BigDecimal utilizedPercentage : utilizedPercentages) {
            if (parameters.containsKey(String.valueOf(utilizedPercentage))) {
                Integer notificationMessageTypeId = Integer.parseInt(parameters
                        .get(String.valueOf(utilizedPercentage)));
                if (null == notificationMessageTypeId) {
                    logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
                    return;
                }
                logger.debug("Notifying user: {} for a Optus Mur consumption notification event", userId);
                notify(notificationMessageTypeId, userId, message -> {
                    message.addParameter(SPCConstants.PERCENTAGE_CONSUMPTION_PARAM, utilizedPercentage);
                    if(utilizedPercentage.compareTo(ONE_HUNDRED) == 0) {
                        String nextUsagePoolName = optusMurNotificationEvent.getCurrentUsagePoolName();
                        String emailContent;
                        if(StringUtils.isNotEmpty(nextUsagePoolName)) {
                            emailContent = " and have received your "+ nextUsagePoolName;
                        } else {
                            emailContent = ", pro-rate charge will get apply";
                        }
                        message.addParameter("emailContent", emailContent);
                    }
                    message.getParameters().putAll(notificationsParams);
                }, params, optusMurNotificationEvent);
            }
        }
    }

    private static final String FIND_CALL_COUNTER_SUM_FOR_USER_SQL =
            "SELECT COALESCE(SUM(ol.call_counter), 0) FROM order_line ol "
                    + "INNER JOIN purchase_order po ON ol.order_id = po.id "
                    + "WHERE po.is_mediated = 't' AND ol.deleted = 0 AND po.deleted = 0 "
                    + "AND ol.item_id IN (:itemIds) AND po.user_id = :userId "
                    + "AND po.status_id in (SELECT id  FROM order_status WHERE order_status_flag = 0)";

    public Long getCallCountersForItemsForActiveMediatedOrder(Integer userId, List<Integer> items) {
        BigDecimal callCountSum = (BigDecimal) sessionFactory.getCurrentSession()
                .createSQLQuery(FIND_CALL_COUNTER_SUM_FOR_USER_SQL)
                .setParameterList("itemIds", items)
                .setParameter("userId", userId)
                .uniqueResult();
        return null!= callCountSum ? callCountSum.longValue() : 0L;
    }

    @SuppressWarnings({ "unchecked"})
    private void sendSpcNotificationToUser(UsagePoolConsumptionNotificationEvent event, Map<String, String> params, Map<String, String> notificationsParams) {
        CustomerUsagePoolDTO customerUsagePool = new CustomerUsagePoolDAS().find(event.getCustomerUsagePoolId());
        Integer userId = customerUsagePool.getCustomer().getBaseUser().getUserId();
        logger.debug("Firing notification event for usage pool consumption to external API for user {}", userId);
        Integer percentageConsumption = event.getAction().getPercentage();
        notify(event.getAction().getNotificationId(), userId, msg -> {
            msg.getParameters().putAll(notificationsParams);
            msg.addParameter(SPCConstants.PERCENTAGE_CONSUMPTION_PARAM, percentageConsumption);
        }, params,event);
    }

    public List<OrderDTO> findActiveDataBoostFeeOrAdjustmentOrders(Integer subscriptionOrderId, List<Integer> itemList, Date nextInvoiceDate, boolean isNewPeriod) {
        Criteria criteria = sessionFactory.getCurrentSession().createCriteria(OrderDTO.class)
                .createAlias("lines", "ol")
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .createAlias("orderPeriod", "op")
                .createAlias("orderStatus", "os")
                .createAlias("ol.item", "item")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("ol.deleted", 0))
                .add(Restrictions.in("item.id", itemList))
                .add(Restrictions.eq("metaField.name", "Subscription Order Id"))
                .add(Restrictions.eq("op.id", Constants.ORDER_PERIOD_ONCE))
                .add(Restrictions.eq("os.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.sqlRestriction("integer_value =  ?", subscriptionOrderId, IntegerType.INSTANCE));
        if(isNewPeriod) {
            criteria.add(Restrictions.ge("activeSince", nextInvoiceDate));
        } else {
            criteria.add(Restrictions.lt("activeSince", nextInvoiceDate));
        }
        return criteria.list();
    }

    private static final String DELETE_BATCH_STEP_EXECUTION_CONTEXT_SQL = "DELETE FROM batch_step_execution_context WHERE "
            + "step_execution_id IN (SELECT step_execution_id "
            + "FROM batch_step_execution WHERE job_execution_id IN "
            + "(SELECT job_execution_id FROM batch_job_execution "
            + "WHERE job_instance_id IN (SELECT job_instance_id "
            + "FROM batch_job_instance WHERE job_name in "
            + "('optusMurMediationJob', 'spcMediationJobLauncher')) "
            + "AND date(start_time) < date(now()) AND end_time IS NOT NULL))";

    private static final String DELETE_BATCH_STEP_EXECUTION_SQL = "DELETE FROM batch_step_execution "
            + "WHERE job_execution_id IN (SELECT job_execution_id "
            + "FROM batch_job_execution WHERE job_instance_id IN "
            + "(SELECT job_instance_id FROM batch_job_instance WHERE "
            + "job_name in ('optusMurMediationJob', 'spcMediationJobLauncher')) "
            + "AND date(start_time) < date(now()) AND end_time IS NOT NULL)";

    private static final String DELETE_BATCH_JOB_EXECUTION_PARAMS_SQL = "DELETE FROM batch_job_execution_params "
            + "WHERE job_execution_id  IN (SELECT job_execution_id "
            + "FROM batch_job_execution WHERE job_instance_id IN (SELECT job_instance_id "
            + "FROM batch_job_instance  WHERE job_name in "
            + "('optusMurMediationJob', 'spcMediationJobLauncher')) AND date(start_time) < date(now()) AND end_time IS NOT NULL)";

    private static final String DELETE_BATCH_JOB_EXECUTION_CONTEXT_SQL = "DELETE FROM batch_job_execution_context WHERE "
            + "job_execution_id IN (SELECT job_execution_id FROM batch_job_execution "
            + "WHERE job_instance_id IN (SELECT job_instance_id FROM batch_job_instance "
            + "WHERE job_name in ('optusMurMediationJob', 'spcMediationJobLauncher')) "
            + "AND date(start_time) < date(now()) AND end_time IS NOT NULL)";

    private static final String DELETE_BATCH_JOB_EXECUTION_SQL = "DELETE FROM batch_job_execution WHERE job_execution_id "
            + "IN (SELECT job_execution_id FROM batch_job_execution WHERE "
            + "job_instance_id IN (SELECT job_instance_id FROM batch_job_instance "
            + "WHERE job_name in ('optusMurMediationJob', 'spcMediationJobLauncher')) "
            + "AND date(start_time) < date(now()) AND end_time IS NOT NULL)";

    private static final String DELETE_BATCH_JOB_INSTANCE_SQL = "DELETE FROM batch_job_instance WHERE job_name in "
            + "('optusMurMediationJob', 'spcMediationJobLauncher') AND "
            + "job_instance_id NOT IN (SELECT job_instance_id FROM batch_job_execution)";

    private static final String DELETE_OPTUS_MUR_RECORDS_SQL =
            "DELETE FROM jbilling_mediation_record WHERE event_date <= ? "
                    + "AND jbilling_entity_id = ? AND chargeable = 'f'";

    private static final String DELETE_OPTUS_MUR_USAGE_RECORDS_SQL =
            "DELETE FROM optus_mur_usage_map WHERE user_id IN "
                    + "(SELECT id FROM base_user WHERE entity_id = ? ) "
                    + "AND create_date <= ?";

    public void deleteMediationJobMetaDataAndMurMediationRecords(Integer entityId, Integer days) {
        Assert.notNull(entityId, "entityId required!");
        int rowCount;
        // delete batch step execution context up to yesterday.
        rowCount = jdbcTemplate.update(DELETE_BATCH_STEP_EXECUTION_CONTEXT_SQL);

        // delete batch step execution up to yesterday.
        rowCount+= jdbcTemplate.update(DELETE_BATCH_STEP_EXECUTION_SQL);

        // delete batch job execution params up to yesterday.
        rowCount+= jdbcTemplate.update(DELETE_BATCH_JOB_EXECUTION_PARAMS_SQL);

        // delete batch job execution context up to yesterday.
        rowCount+= jdbcTemplate.update(DELETE_BATCH_JOB_EXECUTION_CONTEXT_SQL);

        // delete batch job execution up to yesterday.
        rowCount+= jdbcTemplate.update(DELETE_BATCH_JOB_EXECUTION_SQL);

        // delete batch job instance up to yesterday.
        rowCount+= jdbcTemplate.update(DELETE_BATCH_JOB_INSTANCE_SQL);
        logger.debug("total row deleted from batch job meta data table {}", rowCount);

        if(null!= days && days.intValue() > 0) {
            // deleting optus mur mediation data.
            Date date = DateUtils.addDays(new Date(), (days * -1));
            logger.debug("deleting optus mur mediation records before {}", date);
            int deletedRecordCount = jdbcTemplate.update(DELETE_OPTUS_MUR_RECORDS_SQL, date, entityId);
            logger.debug("{} records deleted for entity {} for date {}", deletedRecordCount, entityId, date);
            date = OptusMurNotificationTask.formateDate(date);
            // deeting optus mur usage map for entity.
            deletedRecordCount = jdbcTemplate.update(DELETE_OPTUS_MUR_USAGE_RECORDS_SQL, entityId, date);
            logger.debug("{} records deleted for entity {} for date {} from optus mur usage map", deletedRecordCount, entityId, date);
        }
    }

    @EqualsAndHashCode
    @ToString
    static class PriceCacheKey {
        Integer userId;
        Integer itemId;
        String eventDate;
        String assetNumber;
        String codeString;

        PriceCacheKey(Integer userId, Integer itemId, String eventDate, String assetNumber, String codeString) {
            this.userId = userId;
            this.itemId = itemId;
            this.eventDate = eventDate;
            this.assetNumber = assetNumber;
            this.codeString = codeString;
        }

        static PriceCacheKey of(Integer userId, Integer itemId, String eventDate, String assetNumber, String codeString) {
            return new PriceCacheKey(userId, itemId, eventDate, assetNumber, codeString);
        }
    }

    /**
     * Resolves price for {@link JbillingMediationRecord} for given eventDate.
     * @param order
     * @param jmr
     * @param eventDate
     * @return
     * @throws ExecutionException
     */
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public BigDecimal resolvePriceForJMR(OrderDTO order, JbillingMediationRecord jmr, Date eventDate) throws ExecutionException {
        String codeString = jmr.getPricingFieldValueByName(SPCConstants.CODE_STRING);
        PriceCacheKey priceCacheKey = PriceCacheKey.of(jmr.getUserId(), jmr.getItemId(),
                new SimpleDateFormat("yyyyMMdd").format(eventDate), jmr.getSource(), codeString);
        return priceCache.get(priceCacheKey, () -> {
            long itemLoadStartTime = System.currentTimeMillis();
            ItemBL itemBl = new ItemBL(jmr.getItemId());
            logger.debug("load item took {} miliseconds for user {}", (System.currentTimeMillis() - itemLoadStartTime), jmr.getUserId());
            itemBl.setPricingFields(Arrays.asList(PricingField.getPricingFieldsValue(jmr.getPricingFields())));
            long startQueryTime = System.currentTimeMillis();
            BigDecimal resolvedPrice = itemBl.getPrice(jmr.getUserId(), jmr.getCurrencyId(), jmr.getQuantity(),
                    jmr.getjBillingCompanyId(), order, null, true, eventDate);
            logger.debug("price resolved {} for jmr {} ", resolvedPrice, jmr.getRecordKey());
            logger.debug("time taken {} miliseconds to resolve price for single jmr for user {} for code string {}", (System.currentTimeMillis() - startQueryTime),
                    jmr.getUserId(), codeString);
            return resolvedPrice;
        });
    }

    /**
     * Removes all price entries from cache.
     */
    public void invalidatePriceCache() {
        priceCache.invalidateAll();
    }

    private static final String FIND_CUSTOMER_SQL = "SELECT COUNT(*) FROM customer WHERE user_id = ? ";
    private static final String FIND_CUSTOMER_INVOICE_DESIGN_SQL = "SELECT TRIM(invoice_design) FROM customer WHERE user_id = ? ";
    private static final String UPDATE_CUSTOMER_INVOICE_DESIGN_SQL = "UPDATE customer SET invoice_design = ? WHERE user_id = ? ";

    public String getCustomerInvoiceDesign(Integer userId, String invoiceDesign) {
        int count = jdbcTemplate.queryForObject(FIND_CUSTOMER_SQL, Integer.class, userId);
        if(count <= 0) {
            logger.debug("Customer object is still not persisted in DB, hence returning invoice design ({}) from user.customer object for userId {}", invoiceDesign, userId);
            return invoiceDesign;
        }
        return getCustomerInvoiceDesign(userId);
    }

    public String getCustomerInvoiceDesign(Integer userId) {
        String customerInvoiceDesign = jdbcTemplate.queryForObject(FIND_CUSTOMER_INVOICE_DESIGN_SQL, String.class, userId);
        logger.debug("Returning invoice design ({}) from DB for userId {}", customerInvoiceDesign, userId);
        return customerInvoiceDesign;
    }

    public void updateCustomerInvoiceDesign(Integer userId, String invoiceDesign, Integer executorId) {
        String oldInvoiceDesign = jdbcTemplate.queryForObject(FIND_CUSTOMER_INVOICE_DESIGN_SQL, String.class, userId);
        int status = jdbcTemplate.update(UPDATE_CUSTOMER_INVOICE_DESIGN_SQL, invoiceDesign.trim(), userId);
        if(status != 0) {
            logger.debug("Customer Invoice Design successfully updated for user id : {} from old value : {} to new value : {}",
                    userId, oldInvoiceDesign, invoiceDesign);
        } else {
            logger.debug("Customer Invoice Design update failed for user id : {} from old value : {} to new value : {}",
                    userId, oldInvoiceDesign, invoiceDesign);
        }
        eLogger.audit(executorId,
                userId,
                Constants.TABLE_CUSTOMER, userId,
                EventLogger.MODULE_USER_MAINTENANCE,
                EventLogger.INVOICE_DESIGN_CHANGE,
                null, oldInvoiceDesign, null);
    }

    public boolean isAGL(Integer userId, String invoiceDesign){
        return Objects.equals(SPCConstants.AGL_INVOICE, getCustomerInvoiceDesign(userId, invoiceDesign));
    }

    /**
     * Validate customer level invoice design.
     * @param userId
     * @param invoiceDesign
     * @return
     */
    public boolean validateCustomerInvoiceDesign(Integer userId, String invoiceDesign) {
        try {
            String customerLevelinvoiceDesign = getCustomerInvoiceDesign(userId);
            if (null == customerLevelinvoiceDesign && SPCConstants.SPC_INVOICE.equalsIgnoreCase(invoiceDesign)) {
                return true;
            }
            return invoiceDesign.equalsIgnoreCase(customerLevelinvoiceDesign);
        } catch(Exception ex) {
            logger.error("Failed to reterived customer level invoice design for customer {}", userId, ex);
            return true;
        }
    }
    
    private void setOutboundRequest(Integer userId, Integer entityId,MessageDTO message, Map<String, String> params, Event event) {
    	// Code for setting outbound message to outbound interchange
    	OutBoundInterchange interchange = new OutBoundInterchange();
    	try {
    		String methodName = params.get(SpcNotificationTask.PARAM_SEND_NOTIFICATION_METHOD_NAME.getName());
    		if (null != methodName && !methodName.isEmpty()) {
    			interchange.setMethodName(methodName);
    		} else if(event instanceof SpcCreditPoolNotificationEvent) {
    			interchange.setMethodName("Credit Pool SMS Alert");
    		} else if(event instanceof OptusMurNotificationEvent) {
    			interchange.setMethodName("Optus MUR SMS Alert");
    		} else if(event instanceof UsagePoolConsumptionNotificationEvent) {
    			interchange.setMethodName("Usage Pool SMS Alert");
    		}
    		interchange.setHttpMethod(HttpMethod.POST);
    		interchange.setUserId(userId);
    		interchange.setCompany(new CompanyDTO(entityId));
    		HashMap parameters = message.getParameters();
    		if (!Strings.isNullOrEmpty(params.get(SpcNotificationTask.PARAMETER_CRM_PAYLOAD_PREFIX.getName()))) {
    			parameters.put("crmPrefixes", params.get(SpcNotificationTask.PARAMETER_CRM_PAYLOAD_PREFIX.getName()));
    		}
    		WookieNotification notificationRequest = wookieSmsNotificationRequest(parameters);
    		if (null == notificationRequest) {
    			logger.error("SMS body not for Plan No {} in Data Table {} for Order Id {} of User Id {}",
    					parameters.get(SPCConstants.PLAN_CODE_PARAM),parameters.get("orderId"), parameters
    					.get(SPCConstants.WOOKIE_SMS_TABLE_NAME_PARAM),userId);
    			return;
    		}
    		interchange.setRequest(OBJECT_MAPPER.writeValueAsString(notificationRequest));
    		interchange.setResponse("");
    		interchange.setStatus(Status.UNPROCESSED);
    	} catch (Exception e) {
    		interchange.setResponse(ExceptionUtils.getMessage(e));
    	}
    	outBoundInterchangeDAS.save(interchange);
    }
    
    /**
     * Sends notification to user.
     * @param notificationTypeId
     * @param userId
     * @param applyParameters
     */
    public void notify(Integer notificationTypeId, Integer userId,Consumer<MessageDTO> applyParameters, Map<String, String> params, Event event) {
    	try {
    		UserBL userBL = new UserBL(userId);
    		MessageDTO message = new NotificationBL().getCustomNotificationMessage(notificationTypeId,userBL.getEntityId(userId), userId,
    				userBL.getLanguage());
    		// add additional parameters.
    		if (null != applyParameters) {
    			applyParameters.accept(message);
    		}
    		if (StringUtils.isNotEmpty(params.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName()))) {
    			sendNotificationExternalApi(userId, userBL.getEntityId(userId),message, params);
    		} else {
    			setOutboundRequest(userId, userBL.getEntityId(userId), message, params, event);
    		}
    	} catch (NotificationNotFoundException notificationNotFound) {
    		logger.error(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationTypeId, userId);
    	}
    }

    public Integer getFileProcessedRecordCount(String fileName) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("fileName", fileName);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(RECORDS_PROCESSED_SQL, parameters);
        return row.next() ? row.getInt("records_processed") : 0;
    }

    public void createOrUpdateMediationReconciliationData(Set<MediationReconciliationHistory> reconHistorySet) {
        for (MediationReconciliationHistory reconHistoryRecord : reconHistorySet) {
            String processedArchive = reconHistoryRecord.getProcessedArchive();
            MediationReconciliationHistory oldReconHistoryRecord = getMediationReconciliationHistory(processedArchive);
            if (null != oldReconHistoryRecord) {
                int retryCount = oldReconHistoryRecord.getRetryCount();
                jdbcTemplate.update(UPDATE_RECONCILIATION_HISTORY_SQL, reconHistoryRecord.isVerified(),
                        retryCount + 1, processedArchive);
            } else {
                jdbcTemplate.update(INSERT_MEDIATION_RECON_DATA, reconHistoryRecord.getProcessedDir(),
                        reconHistoryRecord.getProcessedArchive(), reconHistoryRecord.getProcessedFile(), reconHistoryRecord.isVerified(),
                        reconHistoryRecord.getRetryCount());
            }
        }
    }

    public MediationReconciliationHistory getMediationReconciliationHistory(String fileName) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("fileName", fileName);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(DIFFERENCE_FROM_RECONCILIATION_HISTORY_SQL, parameters);
        boolean isVerified = false;
        if (row.next()) {
            MediationReconciliationHistory reconciliationHistory = new MediationReconciliationHistory();
            reconciliationHistory.setProcessedDir(row.getString("processed_dir"));
            reconciliationHistory.setProcessedArchive(row.getString("processed_archive"));
            reconciliationHistory.setProcessedFile(row.getString("processed_file"));
            isVerified = row.getBoolean("is_verified");
            reconciliationHistory.setVerified(isVerified);
            reconciliationHistory.setRetryCount(row.getInt("retry_count"));
            return reconciliationHistory;
        }
        logger.debug("File {} processed : {} ", fileName, isVerified);
        return null;
    }
    
    public InvoiceEmailProcessInfoDTO createOrUpdateInvoiceEmailProcessInfo(InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO) {
        InvoiceEmailProcessInfoBL invoiceEmailProcessInfoBL = new InvoiceEmailProcessInfoBL();
        return invoiceEmailProcessInfoBL.saveInvoiceEmailProcessInfo(invoiceEmailProcessInfoDTO);    
    }   

    public Date calculateBillingUntilWithDelayDays(CustomerDTO customer, Date billingUntil, Integer entityId) {
    	try {

	    	PluggableTaskDTO pluggableTask = new PluggableTaskBL<>()
	    				.getByClassAndCategoryAndEntity(
	    						SPCUserFilterTask.class.getName(), 
	    						Constants.PLUGGABLE_TASK_BILLABLE_USER_FILTER, 
	    						entityId);
    		if(pluggableTask == null) {
    			return billingUntil;
    		}
	        int daysToDelay = getNumberOfDaysDelayForCustomerByPlugin(customer, pluggableTask);
	        if(daysToDelay != 0) {
	            billingUntil = DateUtils.addDays(billingUntil, daysToDelay);
	            logger.debug("Modified billingUntil date: {} for customer", billingUntil);
	        }
    	} catch(Exception ex) {
            logger.error("Failed to calculate billingUntil date for Post-Paid customer");
            throw new SessionInternalError(ex);
        }
    	return billingUntil;
    }

    public Integer getNumberOfDaysDelayForCustomer(CustomerDTO customerDTO, Integer entityId) {
        // Plugin loaded to get 3days delay parameters
        PluggableTaskDTO pluggableTask = new PluggableTaskBL<>().getByClassAndCategoryAndEntity(SPCUserFilterTask.class.getName(),
                Constants.PLUGGABLE_TASK_BILLABLE_USER_FILTER, entityId);
        return null == pluggableTask ? 0 : getNumberOfDaysDelayForCustomerByPlugin(customerDTO, pluggableTask);
    }

    private int getNumberOfDaysDelayForCustomerByPlugin(CustomerDTO customer, PluggableTaskDTO pluggableTask) {
    	Map<String, String> params = loadParaMetersFormPlugin(pluggableTask.getId());
    	return daysToDelay(customer, params); 
    }

    private Map<String, String> loadParaMetersFormPlugin(Integer pluginId) {
        PluggableTaskDTO plugin = new PluggableTaskBL<>(pluginId).getDTO();
        Map<String, String> params = new HashMap<>();
        for(PluggableTaskParameterDTO parameter : plugin.getParameters()) {
            params.put(parameter.getName(), parameter.getValue());
        }
        return params;
    }

    private int daysToDelay(CustomerDTO customer, Map<String, String> params) {
    	if (params.isEmpty()) {
    		logger.error("Parameter map is empty");
    		return 0;
    	}
        String customerTypeMetaFieldName = params.get(SPCUserFilterTask.PARAM_CUSTOMER_TYPE_MF_NAME.getName());
        logger.debug("Customer Type MetaField Name: {}", customerTypeMetaFieldName);
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> customerTypeMetaFieldValue = customer.getMetaField(customerTypeMetaFieldName);
        if(null == customerTypeMetaFieldValue || customerTypeMetaFieldValue.isEmpty()) {
            logger.debug("customerType not found on user {}", customer.getBaseUser().getId());
            return 0;
        }
        String customerType  = params.get(SPCUserFilterTask.PARAM_CUSTOMER_TYPE.getName());
        logger.debug("Customer Type: {}", customerType);

        int daysToDelay = Integer.parseInt(params.get(SPCUserFilterTask.PARAM_DAYS_TO_DELAY_BILLING.getName()));
        logger.debug("Parameter daysToDelay: {}", daysToDelay);

        if(daysToDelay > 0  && null != customerType && customerTypeMetaFieldValue.getValue().equals(customerType)) {
        	return daysToDelay;
        }
        return 0;
    }
}
