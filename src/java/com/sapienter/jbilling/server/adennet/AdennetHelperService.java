/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.adennet.config.ExternalConfig;
import com.sapienter.jbilling.server.adennet.dto.PlanRowMapper;
import com.sapienter.jbilling.server.adennet.dto.SubscriptionAndPlanRowMapper;
import com.sapienter.jbilling.server.adennet.dto.UserInfoRowMapper;
import com.sapienter.jbilling.server.adennet.dto.UserRowMapper;
import com.sapienter.jbilling.server.adennet.ws.AddOnProductWS;
import com.sapienter.jbilling.server.adennet.ws.AdennetBankPlanWS;
import com.sapienter.jbilling.server.adennet.ws.AdennetCustomerContactInformation;
import com.sapienter.jbilling.server.adennet.ws.AdennetPlanWS;
import com.sapienter.jbilling.server.adennet.ws.AdvanceRechargeResponseWS;
import com.sapienter.jbilling.server.adennet.ws.BalanceResponseWS;
import com.sapienter.jbilling.server.adennet.ws.ConsumptionUsageDetailsWS;
import com.sapienter.jbilling.server.adennet.ws.ConsumptionUsageMapResponseWS;
import com.sapienter.jbilling.server.adennet.ws.FeeWS;
import com.sapienter.jbilling.server.adennet.ws.PlanDescriptionWS;
import com.sapienter.jbilling.server.adennet.ws.PrimaryPlanWS;
import com.sapienter.jbilling.server.adennet.ws.RadiusSessionResponseWS;
import com.sapienter.jbilling.server.adennet.ws.ReceiptWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeResponseWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeWS;
import com.sapienter.jbilling.server.adennet.ws.RefundRequestWS;
import com.sapienter.jbilling.server.adennet.ws.SimReissueRequestWS;
import com.sapienter.jbilling.server.adennet.ws.SubscriptionWS;
import com.sapienter.jbilling.server.adennet.ws.UserAndAssetAssociationResponseWS;
import com.sapienter.jbilling.server.adennet.ws.WalletTransactionResponseWS;
import com.sapienter.jbilling.server.adennet.ws.WalletTransactionWS;
import com.sapienter.jbilling.server.adennet.ws.bhmr.BHMRResponseWS;
import com.sapienter.jbilling.server.adennet.ws.recharge_history.CancelRechargeTransactionResponseWS;
import com.sapienter.jbilling.server.adennet.ws.recharge_history.CancelRechargeTransactionWS;
import com.sapienter.jbilling.server.adennet.ws.recharge_history.RechargeTransactionResponseWS;
import com.sapienter.jbilling.server.adennet.ws.ums.ErrorResponse;
import com.sapienter.jbilling.server.adennet.ws.ums.TransactionResponseWS;
import com.sapienter.jbilling.server.item.AssetAssignmentDAS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderStatusDTO;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import jbilling.Filter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.http.HttpStatus;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sapienter.jbilling.common.CommonConstants.ADENNET_SUBSCRIBER_NUMBER_PATTERN;
import static com.sapienter.jbilling.common.CommonConstants.TAX_DATE_FORMAT;
import static com.sapienter.jbilling.common.CommonConstants.TAX_TABLE_NAME;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.ACTIVE_CONSUMPTION_USAGE_MAP;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.ADDRESS_LINE_1;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.ADENNET_DATE_TIME_FORMAT;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.ASSET_STATUS_RELEASED;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.CONTACT_NUMBER;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.DATE_TIME_FORMATTER_TRANSACTION;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.EMAIL_ID;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.FIRST_NAME;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.META_FIELD_CUSTOMER_TYPE;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.META_FIELD_GOVERNORATE;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.SOURCE_POS;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.ADD_ON_PKG_CATEGORY_ID;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.ADD_ON_PRODUCT_ID;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.ASSET_ENABLE_PRODUCT_ID;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.DOWNGRADE_FEE_ID;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.HSS_SSH_IP;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.HSS_SSH_PASSWORD;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.HSS_SSH_USERNAME;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.IMAGE_SERVER_ADDRESS;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.IMAGE_SERVER_FOLDER_LOCATION;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.IMAGE_SERVER_PASSWORD;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.IMAGE_SERVER_USERNAME;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.IS_PROD_ENV;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.MAXIMUM_RECHARGE_AND_REFUND_LIMIT;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.MEDIATION_SERVICE_PASSWORD;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.MEDIATION_SERVICE_URL;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.RADIUS_SERVER_SERVICE_URL;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.REISSUE_COUNT_DURATION_IN_MONTH;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.REISSUE_COUNT_LIMIT;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.SIM_PRICE_ID;
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.USAGE_MANAGEMENT_SERVICE_URL;

@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
@Slf4j
public class AdennetHelperService {

	public static final String RETCODE_0_OPERATION_IS_SUCCESSFUL = "RETCODE = 0 Operation is successful";
	public static final String RETCODE_0_SUCCESS_0001_OPERATION_IS_SUCCESSFUL = "RETCODE = 0 SUCCESS0001:Operation is successful";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final String UMS_CALL_FAILED_FOR_USER_ID = "UMS call failed for userId=%s  exception = %s";
	public static final String UMS_CALL_FAILED_FOR_USER_ID_EXCEPTION = "UMS call failed for userId={}, exception={}";
	public static final String RECHARGE_FAILED_FOR_USER_ID_EXCEPTION = "Recharge failed for userId={} due to error={}";
	public static final String UNABLE_TO_FETCH_BHMR_RECORDS = "Unable to fetch BHMR records";
	public static final String SIM_REISSUE_END_POINT = "/wallet/debit";
	public static final String SIM_REISSUE_SUCCESSFUL = "Sim reissue successful for subscriber number {}";
	public static final String REISSUE_OPERATION_HAS_FAILED = "The sim reissue operation has failed.";
	public static final String CALL_UMS_TO_REISSUE = "Calling UMS to reissue subscriber number {}";
	private static final String LIMIT = "&limit=";
	private static final String OFF_SET = "?offSet=";
	private static final String END_POINT_WALLET = "/wallet/";
	private static final String END_POINT_WALLET_REFUND = "/wallet/refund";
	private static final String END_POINT_BALANCE = "/balance";
	private static final String END_POINT_RECHARGE = "/recharge";
	private static final String END_POINT_USAGE = "/usage/";
	private static final String END_POINT_REQUEST = "/request";
	private static final String VALIDATION_FAILED = "validation failed";
	private static final String RECHARGE_REQUESTS_FAILED = "failed to post recharge request";
	private static final FileSystemOptions fileSystemOptions = new FileSystemOptions();
	private static final String END_POINT_RECHARGE_TRANSACTION = "/recharge/transactions";
	private static final String END_POINT_RECHARGE_TRANSACTION_BY_ENTITY_ID = "/recharge/%d/transactions";
	private static final String END_POINT_REFUND_RECHARGE_TRANSACTION = "/recharge/transaction/refund";
	public static final String END_POINT_SESSION = "/session/";
	public static final String RE_ISSUE_AUDIT_LOG_MSG = "ICCID was changed from '%s' to '%s'.";

	private static final String FIND_PLAN_ID_BY_IDENTIFIER = "WITH active_order   " +
			"       AS (SELECT po.id      AS order_id,   " +
			"                  po.user_id AS user_id   " +
			"           FROM   purchase_order po   " +
			"                  JOIN order_meta_field_map omfm   " +
			"                    ON po.id = omfm.order_id   " +
			"                  JOIN meta_field_value mfv   " +
			"                    ON omfm.meta_field_value_id = mfv.id   " +
			"           WHERE mfv.integer_value = (SELECT po.id   " +
			"                                           FROM   purchase_order po   " +
			"                                                  JOIN order_line ol   " +
			"                                                    ON po.id = ol.order_id   " +
			"                                                  JOIN asset a   " +
			"                                                    ON ol.id = a.order_line_id   " +
			"                                           WHERE a.entity_id = ? AND  " +
			"                      a.subscriber_number = ?)   " +
			"                  AND po.user_id = ? order by create_datetime desc limit 1) " +
			"  SELECT p.id AS plan_id   " +
			"  FROM   item i   " +
			"         INNER JOIN PLAN p   " +
			"                 ON p.item_id = i.id   " +
			"         INNER JOIN order_line ol   " +
			"                 ON ol.item_id = i.id   " +
			"  WHERE  ol.order_id = (SELECT order_id   " +
			"                        FROM active_order)";

	private static final String FIND_ACTIVE_PLAN_ID_BY_IDENTIFIER = "WITH asset_info AS ( " +
			"    SELECT identifier, order_line_id , subscriber_number " +
			"    FROM asset  " +
			"    WHERE subscriber_number = ?  " +
			"    AND deleted = 0 " +
			"), " +
			"active_order AS ( " +
			"    SELECT po.id AS order_id, po.user_id as user_id " +
			"    FROM purchase_order po " +
			"    JOIN order_meta_field_map omfm ON po.id = omfm.order_id " +
			"    JOIN meta_field_value mfv ON omfm.meta_field_value_id = mfv.id " +
			"    WHERE po.status_id = (SELECT id FROM order_status WHERE order_status_flag = 0 AND entity_id = ?) " +
			"      AND mfv.integer_value = ( " +
			"          SELECT order_id " +
			"          FROM order_line where id= (SELECT order_line_id FROM asset_info) " +
			"      ) AND po.user_id = ? " +
			") " +
			"SELECT p.id AS plan_id, " +
			"(select content from international_description where foreign_id = p.item_id and table_id = 14 and language_id = 1) as plan_description, " +
			"(SELECT identifier FROM asset_info), " +
			"(SELECT subscriber_number FROM asset_info), " +
			"(SELECT user_id FROM active_order) " +
			"FROM item i " +
			"INNER JOIN PLAN p ON p.item_id = i.id " +
			"INNER JOIN order_line ol ON ol.item_id = i.id " +
			"WHERE ol.order_id = (SELECT order_id FROM active_order)";

	private static final String FIND_ALL_PLANS = "SELECT DISTINCT ON (i.internal_number) p.id AS plan_id, i.internal_number AS plan_description, " +
			"ratp.account_type, i.entity_id\n" +
			"FROM item i\n" +
			"JOIN PLAN p ON p.item_id = i.id\n" +
			"JOIN route_60_account_type_plan ratp ON ratp.plan_number = i.internal_number; ";

	private static final String FIND_CUSTOMER_DETAIL_BY_ID = " SELECT cum.user_id, cum.subscriber_number, cum.plan_description, cum.status AS plan_status, " +
			" round((SELECT (SUM(wl.amount) - ( SELECT COALESCE( SUM (rechReq.total_recharge_amount), 0.0) " +
			" FROM recharge_request rechReq " +
			" WHERE rechReq.user_id = cum.user_id " +
			" AND rechReq.status = 'PENDING' ) ) " +
			" FROM wallet_ledger wl " +
			" WHERE wl.user_id = cum.user_id), 2) AS wallet_balance " +
			" FROM consumption_usage_map cum " +
			" WHERE cum.id in ( SELECT max(id) " +
			" FROM consumption_usage_map " +
			" WHERE user_id IN (%s) GROUP BY user_id ) " +
			" ORDER BY user_id desc";

	private static final String FIND_USER_LOGIN_NAME_STATUS_AND_GOVERNORATE = "SELECT bu.user_name AS loginName,                                                                  \n" +
			"       CASE WHEN bu.account_disabled_date IS NOT NULL OR bu.account_locked_time IS NOT NULL" +
			"             THEN 'Inactive' " +
			"             ELSE 'Active' " +
			"       END AS status, " +
			" mfv.string_value AS governorate " +
			" FROM base_user bu " +
			" LEFT JOIN user_role_map urm on bu.id=urm.user_id " +
			" LEFT JOIN user_meta_field_map um on um.user_id=bu.id " +
			" LEFT JOIN meta_field_value mfv on mfv.id=um.meta_field_value_id " +
			" WHERE urm.role_id !=62 " +
			" ORDER BY user_name";

	private static final String FIND_USER_BY_SUBSCRIBER_NUMBER = "WITH variables AS (SELECT ? ::text AS number),\n" +
			"  un_accounted_asset AS (\n" +
			" SELECT \n" +
			"      0 AS user_id, 0 AS is_user_deleted, 0 AS entity_id, NULL AS identifier, subscriber_number, \n" +
			"      0 AS is_asset_deleted, false AS is_suspended, NULL AS asset_status, true AS isUnaccounted \n" +
			" FROM route_60_unlimited_usage \n" +
			"    WHERE subscriber_number = (SELECT number FROM variables)),\n" +
			" accounted_asset AS (\n" +
			" SELECT CASE \n" +
			"           WHEN i.content = 'Released' THEN 0  ELSE bu.id\n" +
			"        END AS user_id, bu.deleted AS is_user_deleted, bu.entity_id AS entity_id, a.identifier AS identifier,\n" +
			"      a.subscriber_number AS subscriber_number, a.deleted AS is_asset_deleted, a.is_suspended AS is_suspended,\n" +
			"      i.content AS asset_status, false AS isUnaccounted\n" +
			" FROM purchase_order AS po\n" +
			"    JOIN order_line AS ol ON ol.order_id = po.id\n" +
			"    JOIN base_user AS bu ON bu.id = po.user_id\n" +
			"    JOIN asset_assignment aa ON aa.order_line_id = ol.id\n" +
			"    JOIN asset a ON a.id = aa.asset_id\n" +
			"    JOIN asset_status a_st ON a_st.id = a.status_id \n" +
			"    JOIN international_description i ON i.foreign_id = a_st.id\n" +
			" WHERE i.table_id = 108 AND a.subscriber_number = (SELECT number FROM variables)\n" +
			"     AND ((a.order_line_id IS NOT NULL AND aa.end_datetime IS NULL) OR (a.order_line_id IS NULL AND aa.end_datetime IS NOT NULL))" +
			" ORDER BY aa.id DESC LIMIT 1)\n" +
			" SELECT \n" +
			"     COALESCE(aa.user_id, uaa.user_id) AS user_id,\n" +
			"     COALESCE(aa.is_user_deleted, uaa.is_user_deleted) AS is_user_deleted,\n" +
			"     COALESCE(aa.entity_id, uaa.entity_id) AS entity_id,\n" +
			"     COALESCE(aa.identifier, uaa.identifier) AS identifier,\n" +
			"     COALESCE(aa.subscriber_number, uaa.subscriber_number) AS subscriber_number,\n" +
			"     COALESCE(aa.is_asset_deleted, uaa.is_asset_deleted) AS is_asset_deleted,\n" +
			"     COALESCE(aa.is_suspended, uaa.is_suspended) AS is_suspended,\n" +
			"     COALESCE(aa.asset_status, uaa.asset_status) AS asset_status,\n" +
			"      COALESCE(aa.isUnaccounted, uaa.isUnaccounted) AS isUnaccounted\n" +
			" FROM un_accounted_asset AS uaa\n" +
			"   FULL OUTER JOIN accounted_asset AS aa ON aa.subscriber_number = uaa.subscriber_number\n" +
			" WHERE aa.subscriber_number IS NOT NULL OR uaa.subscriber_number IS NOT NULL ";

	private static final String GET_ASSET_STATUS = "SELECT content FROM asset a JOIN asset_status ass on a.status_id=ass.id JOIN international_description i \n" +
			" on i.foreign_id =ass.id WHERE i.table_id=(select id from jbilling_table where name='asset_status') AND a.subscriber_number= ? ";

	public static final String FOR_PROCESS_ID_EXCEPTION = " for process id={} , exception={}";
	protected static final String RECEIPT_TYPE_ORIGINAL = "Original";
	protected static final String RECEIPT_TYPE_REFUND = "Refund";
	protected static final String RECEIPT_TYPE_PLAN_PRICE = "Plan price";
	protected static final String RECEIPT_TYPE_TOP_UP = "Top up";
	protected static final String RECEIPT_TYPE_SIM_REISSUE_FEE = "SIM reissue fee";
	protected static final String RECEIPT_TYPE_MODEM = "Modem";
	protected static final String RECEIPT_TYPE_SIM_CARD_FEE = "Sim Card Fee";
	protected static final String RECEIPT_TYPE_DOWNGRADE_FEE = "Downgrade Fee";
	protected static final String RECEIPT_AMOUNT_FOR = "Receipt amount for {} = {}";
	public static final String RETCODE = "RETCODE =";
	protected static final String PREFIX_REFUND_OF = "Refund of";
	protected static final String REFUND_OF_SIM_RE_ISSUE_FEE = "Refund of SIM re-issue fee";
	public static final String USAGE_DETAILS_FAILED_FOR_USER_ID = "getConsumptionUsageDetails failed for userId={}";

	@Resource
	private CustomerDAS customerDAS;
	@Resource
	private UserDAS userDAS;
	@Resource
	private AssetDAS assetDAS;
	@Resource(name = "jBillingJdbcTemplate")
	private JdbcTemplate jdbcTemplate;
	@Resource(name = "externalClient")
	private RestOperations externalClient;
	@Resource(name = "webServicesSession")
	private IWebServicesSessionBean webServicesSessionBean;
	@Resource
	private MetaFieldDAS metaFieldDAS;
	@Resource
	private InternationalDescriptionDAS internationalDescriptionDAS;
	@Resource(name = "messageSource")
	private PluginAwareResourceBundleMessageSource messageSource;
	@Resource
	private OrderDAS orderDAS;
	@Resource
	private AssetAssignmentDAS assetAssignmentDAS;
	@Resource
	private OrderStatusDAS orderStatusDAS;
	@Resource
	private AssetStatusDAS assetStatusDAS;
	private EventLogger eLogger = EventLogger.getInstance();

	public UserAndAssetAssociationResponseWS getUserAssetAssociationsBySubscriberNumber(String subscriberNumber) {
		Assert.hasLength(subscriberNumber, "Subscriber number is a required parameter.");
		try {
			return jdbcTemplate.queryForObject(FIND_USER_BY_SUBSCRIBER_NUMBER,
					new Object[]{subscriberNumber}, new BeanPropertyRowMapper<>(UserAndAssetAssociationResponseWS.class));
		} catch (DataAccessException dataAccessException) {
			throw new SessionInternalError(String.format("Associated user not found for subscriber number %s", subscriberNumber), HttpStatus.SC_NOT_FOUND);
		} catch (Exception exception) {
			log.error("Unable to fetch record by subscriber number {}", subscriberNumber, exception);
			throw new SessionInternalError(String.format("Unable to fetch record by subscriber number %s", subscriberNumber), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public Integer getPlanIdForAssetIdentifier(Integer userId, String subscriberNumber, Integer entityId) {
		Assert.hasLength(subscriberNumber, "Please provide subscriber number!");
		SqlRowSet rs = jdbcTemplate.queryForRowSet(FIND_PLAN_ID_BY_IDENTIFIER, entityId, subscriberNumber, userId);
		if (rs.next()) {
			return rs.getInt("plan_id");
		}
		throw new SessionInternalError(String.format("Plan for subscriber number = %s not found", subscriberNumber), HttpStatus.SC_NOT_FOUND);
	}

	public AdennetPlanWS getActivePlanByAssetIdentifierAndUserID(Integer userId, String subscriberNumber) {
		try {
			Integer entityId = webServicesSessionBean.getCallerCompanyId();
			// Fetching data from the database
			List<SubscriptionAndPlanRowMapper> plans = jdbcTemplate.query(FIND_ACTIVE_PLAN_ID_BY_IDENTIFIER,new Object[]{subscriberNumber, entityId, userId},
					new BeanPropertyRowMapper<>(SubscriptionAndPlanRowMapper.class)	);
			if (CollectionUtils.isEmpty(plans)) {
				return null;
			}
			// Map each plan to a SubscriptionWS object
			List<SubscriptionWS> subscriptionWSList = plans.stream()
					.map(this::createSubscriptionWS)
					.collect(Collectors.toList());
			return AdennetPlanWS.builder().
					entityId(entityId).userId(userId)
					.subscriptions(subscriptionWSList).build();
		} catch (Exception exception) {
			log.error("Plan for user {} not found due to {}", userId, exception.getMessage(), exception);
			throw new SessionInternalError(String.format("Plan for user %d not found", userId),HttpStatus.SC_NOT_FOUND);
		}
	}

	private SubscriptionWS createSubscriptionWS(SubscriptionAndPlanRowMapper plan) {
        PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(plan.getPlanId());
        return SubscriptionWS.builder().number(plan.getSubscriberNumber())
                .plan(PlanDescriptionWS.builder()
                        .id(primaryPlanWS.getId())
                        .description(primaryPlanWS.getDescription())
                        .usageQuota(primaryPlanWS.getUsageQuota())
                        .validityInDays(primaryPlanWS.getValidityInDays())
                        .price(primaryPlanWS.getPrice())
                        .build())
                .build();
    }

	public AdennetBankPlanWS[] getAllPlans() {
		// Fetching data from the database
		List<PlanRowMapper> plans = jdbcTemplate.query(FIND_ALL_PLANS, new BeanPropertyRowMapper<>(PlanRowMapper.class));
		if (CollectionUtils.isEmpty(plans)) {
			return new AdennetBankPlanWS[0];
		}
		return plans.stream().map(plan -> {
            PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(plan.getPlanId());
            return createAdennetBankPlanWS(plan, primaryPlanWS);
        }).toArray(AdennetBankPlanWS[]::new);
	}

	private AdennetBankPlanWS createAdennetBankPlanWS(PlanRowMapper plan, PrimaryPlanWS primaryPlanWS) {
		return AdennetBankPlanWS.builder()
				.id(plan.getPlanId())
				.entityId(plan.getEntityId())
				.description(plan.getPlanDescription())
				.accountType(plan.getAccountType())
				.usageQuota(primaryPlanWS.getUsageQuota())
				.validityInDays(primaryPlanWS.getValidityInDays())
				.price(primaryPlanWS.getPrice()).build();
	}

	public BalanceResponseWS getWalletAmount(Integer userId) {
		try {
			String urlUsageManagementService =
					getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + END_POINT_WALLET + userId + END_POINT_BALANCE;
			HttpHeaders headers = new HttpHeaders();

			HttpEntity<String> request = new HttpEntity<>(headers);
			ResponseEntity<BalanceResponseWS> response = externalClient.exchange(url, HttpMethod.GET, request, BalanceResponseWS.class);
			return response.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getWalletAmount failed for userId={}, errorResponse={}", userId, exception.getResponseBodyAsString(), exception);
		} catch (Exception exception) {
			log.error("getWalletAmount failed for userId={}", userId, exception);
		}
		return null;
	}

	public String refundWalletAmount(Integer userId, BigDecimal refundAmount, String loggedInUser, String notes, String governorate, String subscriberNumber ) {
		try {
			String urlUsageManagementService =
					getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String urlWalletRefund = urlUsageManagementService + END_POINT_WALLET_REFUND;

			RefundRequestWS refundRequest = RefundRequestWS.builder()
					.entityId(webServicesSessionBean.getCallerCompanyId())
					.userId(userId)
					.subscriberNumber(subscriberNumber)
					.amount(refundAmount)
					.refundDateTime(OffsetDateTime.now())
					.refundedBy(loggedInUser)
					.source(SOURCE_POS)
					.note(notes)
					.governorate(governorate).build();

			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<RefundRequestWS> httpEntity = new HttpEntity<>(refundRequest, httpHeaders);

			ResponseEntity<TransactionResponseWS> responseEntity = externalClient.postForEntity(urlWalletRefund, httpEntity, TransactionResponseWS.class);
			return responseEntity.getBody().getTransactionId();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("refundWalletAmount failed for userId={}, errorResponse={}", userId, exception.getResponseBodyAsString(), exception);
		} catch (Exception exception) {
			log.error("refundWalletAmount failed for userId={}", userId, exception);
		}
		return null;
	}

	public WalletTransactionResponseWS getWalletTransactions(Integer userId, Integer pageNumber, Integer pageLimit, Long txnId) {
		try {
			String urlUsageManagementService =
					getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String urlWallet = urlUsageManagementService + END_POINT_WALLET + userId + OFF_SET + pageNumber + LIMIT + pageLimit;
			if (txnId != null) {
				urlWallet = urlWallet + String.format("&filters[0].field=TXN_ID&filters[0].value=%d", txnId);
			}
			return externalClient.getForObject(urlWallet, WalletTransactionResponseWS.class);
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getWalletTransaction failed for userId={}, errorResponse={}", userId, exception.getResponseBodyAsString(), exception);
		} catch (Exception exception) {
			log.error("getWalletTransactions failed for userId={}", userId, exception);
		}
		return null;
	}

	public ConsumptionUsageMapResponseWS getConsumptionUsageDetails(Integer userId, Integer pageNumber, Integer pageLimit) {
		try {
			String urlUsageManagementService = getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + END_POINT_USAGE + userId + OFF_SET + pageNumber;
			if (pageLimit != null && pageLimit > 0) {
				url = url + LIMIT + pageLimit;
			}
			return externalClient.getForObject(url, ConsumptionUsageMapResponseWS.class);
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getConsumptionUsage failed for userId={}, errorResponse={}", userId, exception.getResponseBodyAsString(), exception);
		} catch (Exception exception) {
			log.error(USAGE_DETAILS_FAILED_FOR_USER_ID, userId, exception);
		}
		return null;
	}

	public AdvanceRechargeResponseWS getRechargeRequests(Integer userId, Integer pageNumber, Integer pageLimit) {
		try {
			String urlUsageManagementService = getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + END_POINT_RECHARGE + "/" + userId + END_POINT_REQUEST + OFF_SET + pageNumber + LIMIT + pageLimit;

			return externalClient.getForObject(url, AdvanceRechargeResponseWS.class);
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getRechargeRequests failed for userId={}, errorResponse={}", userId, exception.getResponseBodyAsString(), exception);
			throw new SessionInternalError(RECHARGE_REQUESTS_FAILED, exception.getRawStatusCode());
		} catch (Exception exception) {
			log.error("getRechargeRequests failed for userId={}", userId, exception);
			throw new SessionInternalError(RECHARGE_REQUESTS_FAILED, HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public String getValueFromExternalConfigParams(ParameterDescription param) throws PluggableTaskException {
		return new ExternalConfig().getValueFromExternalConfigParams(param, webServicesSessionBean.getCallerCompanyId());
	}

	public RechargeResponseWS recharge(RechargeRequestWS rechargeRequestWS) {
		log.info("Recharge request received for userId={}, isActiveNow={}, rechargedBy={}, with amount={}", rechargeRequestWS.getUserId(),
				rechargeRequestWS.getActivatePrimaryPlanImmediately(), rechargeRequestWS.getRechargedBy(), rechargeRequestWS.getRechargeAmount());

		RechargeResponseWS.RechargeResponseWSBuilder rechargeResponseWS = RechargeResponseWS.builder();
		String transactionId;
		List<FeeWS> feesWSList = new ArrayList<>();

		try {
			validateRechargeRequestWS(rechargeRequestWS);
			AssetDTO asset = assetDAS.getAssetBySubscriberNumber(rechargeRequestWS.getSubscriberNumber());
			validateAsset(rechargeRequestWS, asset);

			if (isWalletTopUp(rechargeRequestWS)) {
				transactionId = callUmsToMakeRecharge(rechargeRequestWS);
				return rechargeResponseWS.transactionId(transactionId).build();
			}

			ConsumptionUsageMapResponseWS consumptionUsageMapResponse = getConsumptionUsageDetails(rechargeRequestWS.getUserId(), 1, 10);
			Integer currentPlanId = getCurrentPlanId(consumptionUsageMapResponse, asset);

			PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(rechargeRequestWS.getPrimaryPlan().getId());
			rechargeRequestWS.setPrimaryPlan(primaryPlanWS);

			if (Objects.equals(currentPlanId, rechargeRequestWS.getPrimaryPlan().getId()) && !isPlanChanged(consumptionUsageMapResponse)) {
				log.info("Recharge with same plan is considered as wallet top up, request received for userId={}, planId={}, subscriberNumber={}",
						rechargeRequestWS.getUserId(), rechargeRequestWS.getPrimaryPlan().getId(), rechargeRequestWS.getSubscriberNumber());

				transactionId = callUmsToMakeRecharge(rechargeRequestWS);
				return rechargeResponseWS.isTopUpRecharge(true).transactionId(transactionId).build();
			}

			applyDowngradeFeeIfApplicable(rechargeRequestWS, currentPlanId).ifPresent(feesWSList::add);
			applySimFeeIfApplicable(rechargeRequestWS).ifPresent(feesWSList::add);

			rechargeRequestWS.setFees(feesWSList);
			rechargeRequestWS.setAddOnProducts(applyAddOnProductIfApplicable(rechargeRequestWS));

			Optional<Integer> planOrderId = processOrder(rechargeRequestWS, asset, consumptionUsageMapResponse);

			if (planOrderId.isPresent()) {
				rechargeRequestWS.setOrderId(planOrderId.get());
				Integer paymentId = createPayment(rechargeRequestWS);
				log.info("Payment={} created for user={} with amount={}", paymentId, rechargeRequestWS.getUserId(), rechargeRequestWS.getRechargeAmount());
			}
			transactionId = callUmsToMakeRecharge(rechargeRequestWS);
			return rechargeResponseWS.transactionId(transactionId)
					.planOrderId(planOrderId.orElse(0))
					.build();
		} catch (Exception exception) {
			log.error("Recharge failed for userId={}, error={}", rechargeRequestWS.getUserId(), exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(exception, HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void validateAsset(RechargeRequestWS rechargeRequestWS, AssetDTO asset) {
		// check the subscriber number present in db
		if (null == asset) {
			log.error("Asset not found for subscriber number ={}", rechargeRequestWS.getSubscriberNumber());
			throw new SessionInternalError(String.format("Asset not found for subscriber number=%s", rechargeRequestWS.getSubscriberNumber()), HttpStatus.SC_NOT_FOUND);
		}

		if (Boolean.TRUE.equals(asset.isSuspended())) {
			log.error("Subscriber number {} is suspended", rechargeRequestWS.getSubscriberNumber());
			throw new SessionInternalError(String.format("Subscriber number %s is suspended", rechargeRequestWS.getSubscriberNumber()), HttpStatus.SC_BAD_REQUEST);
		}
	}

	private boolean isWalletTopUp(final RechargeRequestWS rechargeRequestWS) {
		log.info("Recharge without primary plan and subscriber number is considered as wallet top up, request received for userId= {}", rechargeRequestWS.getUserId());
		return (rechargeRequestWS.getPrimaryPlan() == null && !StringUtils.hasLength(rechargeRequestWS.getSubscriberNumber()));
	}

	private Integer getCurrentPlanId(ConsumptionUsageMapResponseWS consumptionUsageMapResponse, AssetDTO asset) {
		Integer currentPlanId = 0;
		if (asset.getAssetStatus().getIsAvailable() != 1) {
			// current get plan id from usage map
			for (ConsumptionUsageDetailsWS usageDetailsWS : consumptionUsageMapResponse.getConsumptionUsageDetails()) {
				if (!usageDetailsWS.getIsAddOn() && currentPlanId == 0) {
					currentPlanId = usageDetailsWS.getPlanId();
				}
			}
		}
		return currentPlanId;
	}

	private Optional<FeeWS> applyDowngradeFeeIfApplicable(final RechargeRequestWS rechargeRequestWS, final Integer currentPlanId) throws PluggableTaskException {
		Boolean isDowngradeFeesApplicable = PreferenceBL.getPreferenceValueAsBoolean(rechargeRequestWS.getEntityId(), CommonConstants.PREFERENCE_DOWNGRADE_FEES_APPLICABLE);
		if (!Objects.equals(currentPlanId, rechargeRequestWS.getPrimaryPlan().getId()) && currentPlanId != 0
				&& !rechargeRequestWS.getPrimaryPlan().getIsAddOn() && isDowngradeFeesApplicable) {
			BigDecimal currentPlanPrice = getPrimaryPlanWS(currentPlanId).getPrice();
			if (currentPlanPrice.compareTo(rechargeRequestWS.getPrimaryPlan().getPrice()) > 0) {
				String downgradeFeeId = getValueFromExternalConfigParams(DOWNGRADE_FEE_ID);
				if (StringUtils.hasLength(downgradeFeeId)) {
				return Optional.ofNullable(getFeeWS(Integer.parseInt(downgradeFeeId)));
				}
			}
		}
        return Optional.empty();
    }

	private Optional<FeeWS> applySimFeeIfApplicable(final RechargeRequestWS rechargeRequestWS) throws PluggableTaskException {
		if (Boolean.TRUE.equals(rechargeRequestWS.getIsSimIssued())) {
			String simFeeId = getValueFromExternalConfigParams(SIM_PRICE_ID);
			if (StringUtils.hasLength(simFeeId)) {
				return Optional.ofNullable(getFeeWS(Integer.parseInt(simFeeId)));
			}
		}
        return Optional.empty();
    }

	private List<AddOnProductWS> applyAddOnProductIfApplicable(RechargeRequestWS rechargeRequestWS) {
		// Retrieve the price and name for add-on products by product ID
		return Optional.ofNullable(rechargeRequestWS.getAddOnProducts())
				.map(addOnProducts -> addOnProducts.stream()
						.map(product -> getAddOnProductWS(product.getId()))
						.collect(Collectors.toList()))
				.orElse(Collections.emptyList()); // Return an empty list if no add-on products are available
	}

	private Optional<Integer> processOrder(final RechargeRequestWS rechargeRequestWS, final AssetDTO asset, final ConsumptionUsageMapResponseWS consumptionUsageMapResponse) throws PluggableTaskException {
		RechargeWS rechargeWS = getRechargeWS(rechargeRequestWS, asset.getIdentifier());
		if (asset.getOrderLine() == null && asset.getAssetStatus().getIsAvailable() == 1) {
			return Optional.ofNullable(buySubscription(rechargeRequestWS, rechargeWS));
		} else if (Boolean.TRUE.equals(rechargeRequestWS.getPrimaryPlan().getIsAddOn())) {
			return Optional.ofNullable(createAddonOrder(rechargeRequestWS, rechargeWS));
		} else if (Boolean.TRUE.equals(rechargeRequestWS.getActivatePrimaryPlanImmediately()) || isPlanChanged(consumptionUsageMapResponse)) {
			return Optional.ofNullable(changePlanImmediately(rechargeRequestWS, consumptionUsageMapResponse, rechargeWS));
		}
		return Optional.empty();
	}
	private RechargeWS getRechargeWS(RechargeRequestWS rechargeRequestWS, String assetIdentifier) {
		// collect ids of product for jbilling to create order and add it into itemList
		List<Integer> itemList = Optional.ofNullable(rechargeRequestWS.getFees())
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.map(FeeWS::getId).collect(Collectors.toList());

		List<Integer> addOnProductFeeId = Optional.ofNullable(rechargeRequestWS.getAddOnProducts())
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.map(AddOnProductWS::getId).collect(Collectors.toList());

		itemList.addAll(addOnProductFeeId);
		Date activeSince = Date.from(OffsetDateTime.parse(rechargeRequestWS.getRechargeDateTime()).toInstant());
		return RechargeWS.builder()
				.entityId(rechargeRequestWS.getEntityId())
				.userId(rechargeRequestWS.getUserId())
				.planId(rechargeRequestWS.getPrimaryPlan().getId())
				.identifier(assetIdentifier)
				.activeSince(activeSince)
				.activeUntil(new Date(activeSince.getTime() + TimeUnit.DAYS.toMillis(rechargeRequestWS.getPrimaryPlan().getValidityInDays())))
				.itemList(itemList).build();
	}

	private Integer buySubscription(final RechargeRequestWS rechargeRequestWS, RechargeWS rechargeWS) throws PluggableTaskException {
		rechargeWS.setAssetProductId(Integer.parseInt(getValueFromExternalConfigParams(ASSET_ENABLE_PRODUCT_ID)));
		log.info("Buy subscription for subscriberNumber={}, userId={}, planId={}", rechargeRequestWS.getSubscriberNumber(), rechargeRequestWS.getUserId(), rechargeWS.getPlanId());
		return webServicesSessionBean.associateAssetAndPlanWithCustomer(rechargeWS, null);
	}

	private Integer createAddonOrder(final RechargeRequestWS rechargeRequestWS, final RechargeWS rechargeWS) {
		Integer orderId = webServicesSessionBean.associateAssetAndPlanWithCustomer(rechargeWS, null);
		log.info("OrderId={} created with add-on planId={} for subscriberNumber={} and userId={}", orderId, rechargeWS.getPlanId(), rechargeRequestWS.getSubscriberNumber(), rechargeWS.getUserId());
		return orderId;
	}

	private Integer changePlanImmediately(final RechargeRequestWS rechargeRequestWS, final ConsumptionUsageMapResponseWS consumptionUsageMapResponse, final RechargeWS rechargeWS) {
		Integer previousOrderId = consumptionUsageMapResponse.getConsumptionUsageDetails().get(0).getOrderId();
		Integer orderId = webServicesSessionBean.associateAssetAndPlanWithCustomer(rechargeWS, previousOrderId);
		log.info("Plan changed for userId={}, orderId= {}", rechargeRequestWS.getUserId(), orderId);
		return orderId;
	}

	public String callUmsToMakeRecharge(RechargeRequestWS rechargeRequestWS) {
		try {
			log.info("Calling UMS for userId= {}, entityId= {} isActiveNow= {}, rechargedBy= {}, with amount= {}",rechargeRequestWS.getUserId(), rechargeRequestWS.getEntityId(), rechargeRequestWS.getActivatePrimaryPlanImmediately(), rechargeRequestWS.getRechargedBy(), rechargeRequestWS.getRechargeAmount());
			String urlUsageManagementService =getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + END_POINT_RECHARGE;
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<RechargeRequestWS> httpEntity = new HttpEntity<>(rechargeRequestWS, httpHeaders);

			ResponseEntity<TransactionResponseWS> result = externalClient.postForEntity( url, httpEntity, TransactionResponseWS.class);
			return result.getBody().getTransactionId();

		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error(UMS_CALL_FAILED_FOR_USER_ID_EXCEPTION, rechargeRequestWS.getUserId(), exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(convertToError(exception), exception.getRawStatusCode());
		} catch (Exception exception) {
			log.error(UMS_CALL_FAILED_FOR_USER_ID_EXCEPTION, rechargeRequestWS.getUserId(), exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(exception, HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private static String convertToError(HttpStatusCodeException exception) {
		try {
			ErrorResponse errorResponse = OBJECT_MAPPER.readValue(exception.getResponseBodyAsString(), ErrorResponse.class);
			return errorResponse.getErrors().getError().get(0).getDetails();
		} catch (IOException ioException) {
			log.error("Unable to convert UMS error to pojo.", ioException);
		}
		return "";
	}

	private AddOnProductWS getAddOnProductWS(Integer id) {
		ItemDTOEx item = webServicesSessionBean.getItem(id, null, null);
		return AddOnProductWS.builder()
				.id(item.getId())
				.name(item.getDescription())
				.price(item.getPriceAsDecimal()).build();
	}

	private FeeWS getFeeWS(Integer id) {
		ItemDTOEx feeItem = webServicesSessionBean.getItem(id, null, null);
		return FeeWS.builder()
				.id(feeItem.getId())
				.description(feeItem.getDescription())
				.amount(new BigDecimal(feeItem.getPrice())).build();
	}

	public PrimaryPlanWS getPrimaryPlanWS(Integer planId) {
		String dateFormat;
		String tableName;
		Integer addOnCategoryId = null;
		try {
			addOnCategoryId = Integer.valueOf(getValueFromExternalConfigParams(ADD_ON_PKG_CATEGORY_ID));
			MetaFieldValueWS[] metaFields = webServicesSessionBean.getCompany().getMetaFields();
			tableName = getMetaFieldValue(metaFields, TAX_TABLE_NAME);
			dateFormat = getMetaFieldValue(metaFields, TAX_DATE_FORMAT);
		} catch (PluggableTaskException e) {
			throw new RuntimeException(e);
		}
		return new PlanBL().getPlan(planId, addOnCategoryId, tableName, dateFormat);
	}

	public AdennetCustomerContactInformation getCustomerContactInformation(int userId) {
		AdennetCustomerContactInformation.AdennetCustomerContactInformationBuilder contactInformation = AdennetCustomerContactInformation.builder();
		UserDTO user = userDAS.findNow(userId);
		log.info("Fetching user contact information for userId = {}.", userId);
		if (user != null) {
			Stream<MetaFieldValueWS> metaFieldValuesWS = Arrays.stream(new UserBL(user).getUserWS().getMetaFields());
			metaFieldValuesWS.forEach(value -> {
				if (value.getFieldName().equals(FIRST_NAME)) {
					contactInformation.firstName(value.getStringValue());
				}
				if (value.getFieldName().equals(CONTACT_NUMBER)) {
					contactInformation.contactNumber(value.getStringValue());
				}
				if (value.getFieldName().equals(EMAIL_ID)) {
					contactInformation.emailId(value.getStringValue());
				}
				if (value.getFieldName().equals(ADDRESS_LINE_1)) {
					contactInformation.address(value.getStringValue());
				}
				contactInformation.build();
			});
		}
		return contactInformation.build();
	}

	public List<String> getInternalPlanNumbersByCustomerType(String customerType) {
		String findPlanByCustomerType = "SELECT plan_number\n" +
				"FROM   route_" + webServicesSessionBean.getCallerCompanyId() + "_account_type_plan\n" +
				"WHERE  account_type = ? ";
		List<String> planForCustomerTypeList = new ArrayList<>();

		SqlRowSet rs = jdbcTemplate.queryForRowSet(findPlanByCustomerType, customerType);
		while (rs.next()) {
			planForCustomerTypeList.add(rs.getString("plan_number"));
		}
		return planForCustomerTypeList;
	}

	public List<PrimaryPlanWS> getAllPlansByCustomerType(String customerType) {
		return Optional.ofNullable(getInternalPlanNumbersByCustomerType(customerType))
				.orElseGet(Collections::emptyList)
				.stream()
				.map(this::getPrimaryPlanWS)
				.collect(Collectors.toList());
	}

	public void saveImageFile(Integer userId, String identificationType, String identificationNumber, String imageExtension, InputStream inputStream, String originalFileName) {
		log.info("Saving image for userId={}, imageExtension={}", userId, imageExtension);
		String message = null;
		try {
			// audit customer
			UserWS userWS = new UserBL(userId).getUserWS();
			//Upload the image with name as "userId_timestamp.extension" to image server
			originalFileName = userId.toString() + "_" + System.currentTimeMillis() + originalFileName.substring(originalFileName.lastIndexOf('.'));

			try (   //Get the object of remoteFileObject
					FileObject newRemoteFileObject = getRemoteFileObject(originalFileName);
					OutputStream outputStream = newRemoteFileObject.getContent().getOutputStream()) {
				IOUtils.copy(inputStream, outputStream);
				log.info("File={} successfully copied to image server", originalFileName);

				//Link image_name to customer
				linkImage(userId, identificationType, originalFileName, identificationNumber);
				log.info("Image={} linked to userId={}", originalFileName, userId);

				//audit log on the change of identificationType
				if (userWS != null) {
					String oldIdentificationType = userWS.getIdentificationType();
					if (!StringUtils.isEmpty(oldIdentificationType)) {
						String oldIdentificationNumber = userWS.getIdentificationText();
						if (!oldIdentificationType.equals(identificationType)) {
							fireAuditEvent(userId, userWS.getId(), EventLogger.IDENTIFICATION_TYPE_UPDATED, String.format("Identification type was changed from '%s' to '%s'.", oldIdentificationType, identificationType));
						}
						if (!identificationNumber.equals(oldIdentificationNumber)) {
							if (StringUtils.isEmpty(oldIdentificationNumber)) {
								fireAuditEvent(userId, userWS.getId(), EventLogger.IDENTIFICATION_TEXT_UPDATED, String.format("%s '%s' was added.", identificationType, identificationNumber));
							} else if (!StringUtils.isEmpty(identificationNumber)) {
								fireAuditEvent(userId, userWS.getId(), EventLogger.IDENTIFICATION_TEXT_UPDATED, String.format("Identification text was changed from '%s' to '%s'.", oldIdentificationNumber, identificationNumber));
							}
						}
						if (!Objects.equals(userWS.getIdentificationImage(), originalFileName)) {
							fireAuditEvent(userId, userWS.getId(), EventLogger.IDENTIFICATION_IMAGE_UPDATED, String.format("Identification image was changed from '%s' to '%s'.", userWS.getIdentificationImage(), originalFileName));
						}
					}
				}
			}
		} catch (IOException ioException) {
			log.error("Failed to save image for userId={} due to {}", userId, ioException.getLocalizedMessage(), ioException);
			throw new SessionInternalError(String.format("validation.error.image," + userId), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public byte[] getImageBytes(Integer userId) {
		String imageName = getImageNameByCustomerID(userId);
		if (imageName != null && !imageName.isEmpty()) {
			log.info("Fetching image={} for userId={}", imageName, userId);
			try (//Retrieve the image file from the server.
					FileObject remoteFileObject = getRemoteFileObject(imageName)) {
				if (remoteFileObject == null || !remoteFileObject.exists()) {
					log.error(String.format("Image(%s) not found for user=%s", imageName, userId));
					throw new SessionInternalError(String.format("Image(%s) not found for user=%s", imageName, userId), HttpStatus.SC_INTERNAL_SERVER_ERROR);
				}
				return remoteFileObject.getContent().getByteArray();
			} catch (IOException ioException) {
				log.error("GetImage failed for userId={} due to {}", userId, ioException.getLocalizedMessage(), ioException);
				throw new SessionInternalError(String.format("Unable to fetch image(%s) for user=%s", imageName, userId), HttpStatus.SC_INTERNAL_SERVER_ERROR);
			}
		}
		return new byte[]{};
	}

	private FileObject getRemoteFileObject(String imageFileName) {
		try {
			String imageServerAddress = getValueFromExternalConfigParams(IMAGE_SERVER_ADDRESS);
			String imageServerUsername = getValueFromExternalConfigParams(IMAGE_SERVER_USERNAME);
			String imageServerPassword = getValueFromExternalConfigParams(IMAGE_SERVER_PASSWORD);
			String imageServerFolderLocation = getValueFromExternalConfigParams(IMAGE_SERVER_FOLDER_LOCATION);

			DefaultFileSystemManager defaultFileSystemManager = (DefaultFileSystemManager) VFS.getManager();

			SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fileSystemOptions, false);
			StaticUserAuthenticator authenticator = new StaticUserAuthenticator(imageServerAddress, imageServerUsername, imageServerPassword);

			DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(fileSystemOptions, authenticator);
			String sftpUrl = String.format("sftp://%s%s%s", imageServerAddress, imageServerFolderLocation, imageFileName);

			return defaultFileSystemManager.resolveFile(sftpUrl, fileSystemOptions);

		} catch (FileSystemException | PluggableTaskException fileSystemException) {
			log.error("Unable to resolve file from remote server. " + fileSystemException);
			throw new SessionInternalError("Unable to resolve file from remote server.", fileSystemException);
		}
	}

	public void linkImage(int userId, String identificationType, String imageFileName, String identificationNumber) {
		customerDAS.linkImage(userId, identificationType, imageFileName, identificationNumber);
	}

	public String getImageNameByCustomerID(Integer userId) {
		return customerDAS.getImageNameByCustomerID(userId);
	}

	public BHMRResponseWS fetchBhmrRecord(int processId, int pageNumber, int pageLimit) {
		try {
			log.info("Fetching BHMR records for processId = {}", processId);
			String urlConsumptionUsageBhmr = getValueFromExternalConfigParams(MEDIATION_SERVICE_URL);
			String passConsumptionUsageBhmr = getValueFromExternalConfigParams(MEDIATION_SERVICE_PASSWORD);
			String url = urlConsumptionUsageBhmr + processId + "?limit=" + pageLimit + "&page=" + pageNumber + "&sorts[0].attribute=eventDate&sorts[0].order=desc";

			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "Basic " + passConsumptionUsageBhmr);
			HttpEntity<String> request = new HttpEntity<>(headers);

			ResponseEntity<BHMRResponseWS> response = externalClient.exchange(url, HttpMethod.GET, request, BHMRResponseWS.class);
			return response.getBody();

		} catch (HttpClientErrorException | HttpServerErrorException statusCodeException) {
			log.error(UNABLE_TO_FETCH_BHMR_RECORDS + FOR_PROCESS_ID_EXCEPTION, processId, statusCodeException.getLocalizedMessage(),statusCodeException);
			throw new SessionInternalError(UNABLE_TO_FETCH_BHMR_RECORDS, statusCodeException.getRawStatusCode());
		} catch (Exception exception) {
			log.error(UNABLE_TO_FETCH_BHMR_RECORDS + FOR_PROCESS_ID_EXCEPTION, processId, exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(UNABLE_TO_FETCH_BHMR_RECORDS, HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public int isSubscriberNumberActive(String subscriberNumber) {
		try {
			AssetDTO assetDTO = assetDAS.getAssetBySubscriberNumber(subscriberNumber);
			if (assetDTO == null) {
				log.error("Asset not found for subscriberNumber={}", subscriberNumber);
				throw new SessionInternalError(String.format("Asset not found for subscriberNumber=%s", subscriberNumber), HttpStatus.SC_NOT_FOUND);
			} else if (assetDTO.isSuspended()) {
				log.error("{} is suspended hence any activity related to recharge can not be performed.", subscriberNumber);
				throw new SessionInternalError(String.format("validation.error.suspended," + subscriberNumber), HttpStatus.SC_BAD_REQUEST);
			}
			return assetDTO.getAssetStatus().getIsActive();
		} catch (Exception exception) {
			throw new SessionInternalError(String.format("Failed to check isSubscriberNumberActive for subscriber=%s", subscriberNumber), HttpStatus.SC_BAD_REQUEST);
		}
	}

	public String reissue(String oldIdentifier, String newIdentifier, String note, SimReissueRequestWS simReissueRequestWS) {
		String transactionId;
		try {
			log.info("SIM reissue request receive for {}", simReissueRequestWS.getSubscriberNumber());
			Integer reissueCountLimit = Integer.valueOf(getValueFromExternalConfigParams(REISSUE_COUNT_LIMIT));
			Integer reissueDuration = Integer.valueOf(getValueFromExternalConfigParams(REISSUE_COUNT_DURATION_IN_MONTH));

			AssetWS newAssetWS = webServicesSessionBean.reIssueSIM(oldIdentifier, newIdentifier, note, reissueDuration);

			if (newAssetWS != null && StringUtils.hasLength(newAssetWS.getImsi())) {
				provisionSubscriberNumberWithNewIMSINumber(simReissueRequestWS.getSubscriberNumber(), newAssetWS.getImsi());
			}
			log.debug(SIM_REISSUE_SUCCESSFUL, simReissueRequestWS.getSubscriberNumber());
			transactionId = callUmsToMakeSimReissue(simReissueRequestWS);

			// adding an audit log for simReissue
			eLogger.auditLog(webServicesSessionBean.getCallerId(), simReissueRequestWS.getUserId(),
					Constants.TABLE_CUSTOMER, simReissueRequestWS.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
					EventLogger.SIM_REISSUED, null, String.format(RE_ISSUE_AUDIT_LOG_MSG, oldIdentifier, newIdentifier), null);

			UserWS userWS = webServicesSessionBean.getUserWS(simReissueRequestWS.getUserId());
			if (userWS.getReissueCount() > reissueCountLimit) {
				String message = String.format("Reissue limit is exceeded for subscriber number %s. Number of reissue transactions %s.", simReissueRequestWS.getSubscriberNumber(), userWS.getReissueCount());

				//Audit log for when a user exceeds the re-issue limit.
				eLogger.auditLog(webServicesSessionBean.getCallerId(), simReissueRequestWS.getUserId(),
						Constants.TABLE_CUSTOMER, simReissueRequestWS.getUserId(), EventLogger.MODULE_USER_MAINTENANCE,
						EventLogger.REISSUE_LIMIT_EXCEEDED, null, message, null);
				log.info("Exceeds the reissue limit for subscriberNumber={}", simReissueRequestWS.getSubscriberNumber());
			}
			return transactionId;
		} catch (SessionInternalError sessionInternalError) {
			log.error(sessionInternalError.getLocalizedMessage(), sessionInternalError);
			throw sessionInternalError;
		} catch (Exception exception) {
			log.error(exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(REISSUE_OPERATION_HAS_FAILED, new String[]{REISSUE_OPERATION_HAS_FAILED}, HttpStatus.SC_BAD_REQUEST);
		}
	}

	public String callUmsToMakeSimReissue(SimReissueRequestWS simReissueRequestWS) {
		try {
			log.info(CALL_UMS_TO_REISSUE, simReissueRequestWS.getSubscriberNumber());
			String urlUsageManagementService =
					getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + SIM_REISSUE_END_POINT;
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<SimReissueRequestWS> httpEntity = new HttpEntity<>(simReissueRequestWS, httpHeaders);

			ResponseEntity<TransactionResponseWS> result = externalClient.postForEntity(url, httpEntity, TransactionResponseWS.class);
			return result.getBody().getTransactionId();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error(UMS_CALL_FAILED_FOR_USER_ID_EXCEPTION, simReissueRequestWS.getUserId(), exception.getLocalizedMessage());
			throw new SessionInternalError(String.format(UMS_CALL_FAILED_FOR_USER_ID, simReissueRequestWS.getUserId(), exception.getLocalizedMessage()), exception.getRawStatusCode());
		} catch (Exception exception) {
			log.error(UMS_CALL_FAILED_FOR_USER_ID_EXCEPTION, simReissueRequestWS.getUserId(), exception.getLocalizedMessage());
			throw new SessionInternalError(String.format(UMS_CALL_FAILED_FOR_USER_ID, simReissueRequestWS.getUserId(), exception), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void validateRechargeRequestWS(RechargeRequestWS rechargeRequestWS) throws PluggableTaskException {
		String maxRechargeLimit = getValueFromExternalConfigParams(MAXIMUM_RECHARGE_AND_REFUND_LIMIT);
		UserDTO user = userDAS.findNow(rechargeRequestWS.getUserId());
		if (rechargeRequestWS.getUserId() == null || rechargeRequestWS.getUserId() <= 0) {
			log.error("Invalid User Id={}", rechargeRequestWS.getUserId());
			throw new SessionInternalError(String.format("Invalid User Id %d", rechargeRequestWS.getUserId()), HttpStatus.SC_BAD_REQUEST);
		}

		if (null == user) {
			log.error("User not found with userId={}", rechargeRequestWS.getUserId());
			throw new SessionInternalError(String.format("User not found with userId=%s", rechargeRequestWS.getUserId()), HttpStatus.SC_NOT_FOUND);
		}

		if (user.getDeleted() == 1) {
			log.error("Current status of user is deleted, userId={}", rechargeRequestWS.getUserId());
			throw new SessionInternalError(String.format("Current status of user is deleted, userId=%d", rechargeRequestWS.getUserId()), HttpStatus.SC_BAD_REQUEST);
		}

		if (user.getCompany().getId() != rechargeRequestWS.getEntityId()) {
			log.error("A combination userId={} and entityId={} is not valid.", rechargeRequestWS.getUserId(), rechargeRequestWS.getEntityId());
			throw new SessionInternalError(String.format("A combination userId=%d and entityId=%d is not valid.", rechargeRequestWS.getUserId(), rechargeRequestWS.getEntityId()), HttpStatus.SC_BAD_REQUEST);
		}
		if (!StringUtils.hasLength(rechargeRequestWS.getRechargeDateTime())) {
			log.error("RechargeDateTime should not be null={}", rechargeRequestWS.getRechargeDateTime());
			throw new SessionInternalError("RechargeDateTime should not be null", HttpStatus.SC_BAD_REQUEST);
		}
		if (!StringUtils.hasLength(rechargeRequestWS.getRechargedBy())) {
			log.error("RechargedBy should not be null={}", rechargeRequestWS.getRechargedBy());
			throw new SessionInternalError("RechargedBy should not be null ", HttpStatus.SC_BAD_REQUEST);
		}
		if (!StringUtils.hasLength(rechargeRequestWS.getSource())) {
			log.error("Source should not be null={}", rechargeRequestWS.getSource());
			throw new SessionInternalError("Source should not be null ", HttpStatus.SC_BAD_REQUEST);
		}

		if (null == rechargeRequestWS.getPrimaryPlan() && StringUtils.hasLength(rechargeRequestWS.getSubscriberNumber())) {
			log.error("Subscriber number {} is provided but forgot to provide plan for userId {}", rechargeRequestWS.getSubscriberNumber(), rechargeRequestWS.getUserId());
			throw new SessionInternalError(String.format("Subscriber number %s is provided but forgot to provide plan ",
					rechargeRequestWS.getSubscriberNumber()), HttpStatus.SC_BAD_REQUEST);
		}

		if (null != rechargeRequestWS.getPrimaryPlan() && !StringUtils.hasLength(rechargeRequestWS.getSubscriberNumber())) {
			log.error("Plan {}. {} is provided but forgot to provide Subscriber number for userId {}", rechargeRequestWS.getPrimaryPlan().getId(),
					rechargeRequestWS.getPrimaryPlan().getDescription(), rechargeRequestWS.getUserId());
			throw new SessionInternalError(String.format("Plan %d . %s is provided but forgot to provide subscriber number",
					rechargeRequestWS.getPrimaryPlan().getId(), rechargeRequestWS.getPrimaryPlan().getDescription()), HttpStatus.SC_BAD_REQUEST);
		}

		if (null == rechargeRequestWS.getRechargeAmount() || rechargeRequestWS.getRechargeAmount().compareTo(BigDecimal.ZERO) < 0 || rechargeRequestWS.getRechargeAmount().compareTo(BigDecimal.valueOf(Long.parseLong(maxRechargeLimit))) > 0) {
			log.error("Recharge amount not valid={}", rechargeRequestWS.getRechargeAmount());
			throw new SessionInternalError(String.format("Recharge amount can not greater than %s",maxRechargeLimit), HttpStatus.SC_BAD_REQUEST);
		}

		if (null != rechargeRequestWS.getPrimaryPlan() && StringUtils.hasLength(rechargeRequestWS.getSubscriberNumber())) {
			if (PreferenceBL.getPreferenceValueAsBoolean(rechargeRequestWS.getEntityId(), CommonConstants.PREFERENCE_APPLY_CUSTOMER_USER_NAME_VALIDATION)
					&& !Pattern.matches(ADENNET_SUBSCRIBER_NUMBER_PATTERN, rechargeRequestWS.getSubscriberNumber())) {
				throw new SessionInternalError("validation.error.invalid.adennet.username", HttpStatus.SC_BAD_REQUEST);
			}

			// check plan applicable for user based on a customer type
			List<Integer> planIds = new ArrayList<>();
			String customerType = "";

			customerType = Arrays.stream(new UserBL(user).getUserWS().getMetaFields()).filter(metaFieldValueWS ->
			metaFieldValueWS.getMetaField().getName().equals(META_FIELD_CUSTOMER_TYPE)).findFirst()
			.map(MetaFieldValueWS::getStringValue).orElse("NA");

			List<PrimaryPlanWS> plans = getAllPlansByCustomerType(customerType);

			plans.forEach(plan -> planIds.add(plan.getId()));

			if (!planIds.contains(rechargeRequestWS.getPrimaryPlan().getId())) {
				log.error("{} not found for {}", rechargeRequestWS.getPrimaryPlan().getDescription(), rechargeRequestWS.getUserId());
				throw new SessionInternalError(String.format("Plan not found %d. %s for userId %d", rechargeRequestWS.getPrimaryPlan().getId(),
						rechargeRequestWS.getPrimaryPlan().getDescription(), rechargeRequestWS.getUserId()), HttpStatus.SC_NOT_FOUND);
			}

			//check for addOnProductWS ids present in jbilling
			if (null != rechargeRequestWS.getAddOnProducts()) {
				List<Integer> itemIds = new ArrayList<>();

				ItemDTOEx[] items = webServicesSessionBean.getItemByCategory(Integer.parseInt(getValueFromExternalConfigParams(ADD_ON_PRODUCT_ID)));
				Arrays.stream(items).forEach(item -> itemIds.add(item.getId()));

				rechargeRequestWS.getAddOnProducts().forEach(addOnProduct -> {
					if (!itemIds.contains(addOnProduct.getId())) {
						log.error("{} {} not found for userId {}", addOnProduct.getId(), addOnProduct.getName(), rechargeRequestWS.getUserId());
						throw new SessionInternalError(String.format("%d . %s not found for userId %d", addOnProduct.getId(),
								addOnProduct.getName(), rechargeRequestWS.getUserId()), HttpStatus.SC_NOT_FOUND);
					}
				});
			}
		}
	}

	public boolean isSubscriberNumberPresent(String subscriberNumber) {
		AssetDTO assetDTO = assetDAS.getAssetByIdentifier(subscriberNumber);
		return assetDTO != null;
	}

	public boolean isSubscriberNumberAvailable(String subscriberNumber) {
		AssetDTO assetDTO = assetDAS.getAssetByIdentifier(subscriberNumber);
		if (assetDTO != null) {
			return assetDTO.getAssetStatus().getIsAvailable() == 1;
		}
		return false;
	}

	public RechargeTransactionResponseWS getRechargeTransactions(boolean canViewAll, int pageNumber, int pageLimit, boolean applyFilter, List<Filter> filters, Integer userId) throws PluggableTaskException {
		String loggedInUserName = getUserNameByUserId(userId);
		Integer entityId = webServicesSessionBean.getCallerCompanyId();
		String endPointTransactionsByEntityId = String.format(END_POINT_RECHARGE_TRANSACTION_BY_ENTITY_ID, entityId);

		String defaultFilter = String.format("&filters[0].field=CREATED_BY&filters[0].value=%s", loggedInUserName);
		log.info("defaultFilter:{}", defaultFilter);

		StringBuilder queryString = new StringBuilder();
		int filterIndex = 0;
		log.info("canViewAll:{}", canViewAll);

		if (!canViewAll) {
			filterIndex = 1;
		}

		try {
			if (applyFilter) {
				for (Filter filter : filters) {
					if (filter.getValue() != null) {
						String searchField = filter.getField().toUpperCase();
						switch (searchField) {
							case "ID":
								queryString.append(String.format("&filters[%d].field=USER_ID&filters[%d].value=%s", filterIndex, filterIndex, filter.getValue()));
								break;
							case "RECHARGE_DATE":
								queryString.append(String.format("&filters[%d].field=TRN_DATE_FROM&filters[%d].value=%s", filterIndex, filterIndex, SIMPLE_DATE_FORMAT.format(filter.getStartDateValue())));
								filterIndex++;
								queryString.append(String.format("&filters[%d].field=TRN_DATE_TO&filters[%d].value=%s", filterIndex, filterIndex, SIMPLE_DATE_FORMAT.format(filter.getEndDateValue())));
								break;
							case "TRANSACTION_ID":
								queryString.append(String.format("&filters[%d].field=ID&filters[%d].value=%s", filterIndex, filterIndex, filter.getValue()));
								break;
							case "TRANSACTION_TYPE":
								queryString.append(String.format("&filters[%d].field=TYPE&filters[%d].value=%s", filterIndex, filterIndex, filter.getValue()));
								break;
							default:
								queryString.append(String.format("&filters[%d].field=%s&filters[%d].value=%s", filterIndex, searchField, filterIndex, filter.getValue()));
								break;
						}
						filterIndex++;
					}
				}
			}
			String url = "";
			String urlUsageManagementService = getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);

			if (!canViewAll) {
				if (applyFilter && queryString.length() > 0) {
					url = String.format("%s%s%s%s%s%s%s%s", urlUsageManagementService, endPointTransactionsByEntityId, "?limit=", pageLimit, "&offSet=", pageNumber, defaultFilter, queryString.toString());
				} else {
					url = String.format("%s%s%s%s%s%s%s", urlUsageManagementService, endPointTransactionsByEntityId, "?limit=", pageLimit, "&offSet=", pageNumber, defaultFilter);
				}
			} else {
				if (applyFilter && queryString.length() > 0) {
					url = String.format("%s%s%s%s%s%s%s", urlUsageManagementService, endPointTransactionsByEntityId, "?limit=", pageLimit, "&offSet=", pageNumber, queryString.toString());
				} else {
					url = String.format("%s%s%s%s%s%s", urlUsageManagementService, endPointTransactionsByEntityId, "?limit=", pageLimit, "&offSet=", pageNumber);
				}
			}

			HttpHeaders headers = new HttpHeaders();
			HttpEntity<String> request = new HttpEntity<>(headers);
			ResponseEntity<RechargeTransactionResponseWS> response = externalClient.exchange(url, HttpMethod.GET, request, RechargeTransactionResponseWS.class);
			return response.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getRechargeTransaction failed errorResponse={}", exception.getResponseBodyAsString(), exception);
		} catch (Exception exception) {
			log.error("getRechargeTransaction failed errorResponse {}", exception.getLocalizedMessage(), exception);
		}
		return null;
	}

	public String cancelRechargeTransaction(CancelRechargeTransactionWS cancelRechargeTransactionWS, String currentIdentifier,
											boolean isSimReIssued, boolean isSimIssued) throws Exception {
		log.info("Received transaction cancelling request against Transaction ID={} for ICCID ={}, isSimReIssued={}, isSimIssued={}"
				, cancelRechargeTransactionWS.getRechargeTransactionId(), currentIdentifier, isSimReIssued, isSimIssued);
		try {
			if (isSimReIssued) {
				// Cancel current order with currentIdentifier and
				// create order with discardedAsset which is configured at asset level as a discarded in the current asset

				AssetWS discardedAsset = webServicesSessionBean.cancelReIssueSIM(currentIdentifier, cancelRechargeTransactionWS.getNote());
				if (discardedAsset == null || !StringUtils.hasLength(discardedAsset.getImsi())) {
					throw new SessionInternalError("Asset can not be null", HttpStatus.SC_INTERNAL_SERVER_ERROR);
				}
				provisionSubscriberNumberWithNewIMSINumber(discardedAsset.getSubscriberNumber(), discardedAsset.getImsi());

				// adding an audit log for simReissue refund
				Integer userId = webServicesSessionBean.getUserId(discardedAsset.getIdentifier());
				eLogger.auditLog(webServicesSessionBean.getCallerId(), userId, Constants.TABLE_CUSTOMER, userId,
						EventLogger.MODULE_USER_MAINTENANCE, EventLogger.SIM_REISSUE_REFUND, null,
						String.format(RE_ISSUE_AUDIT_LOG_MSG, currentIdentifier, discardedAsset.getIdentifier()), null);

			} else if (isSimIssued) {
				// Cancel order in jbilling to refund 'Buy Subscription' and changed username = 'username + refunded'
				webServicesSessionBean.refundBuySubscription(currentIdentifier);
			}
			String urlUsageManagementService = getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + END_POINT_REFUND_RECHARGE_TRANSACTION;
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<CancelRechargeTransactionWS> request = new HttpEntity<>(cancelRechargeTransactionWS,httpHeaders);
			ResponseEntity<CancelRechargeTransactionResponseWS> responseEntity = externalClient.exchange(url, HttpMethod.PUT, request, CancelRechargeTransactionResponseWS.class);
			log.info("CancelRecharge Transaction ID={}, against RechargeTransactionId={}", responseEntity.getBody().getTransactionId(), cancelRechargeTransactionWS.getRechargeTransactionId());
			return responseEntity.getBody().getTransactionId();

		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("cancelRechargeTransaction failed for Transaction ID={} , exception={}", cancelRechargeTransactionWS.getRechargeTransactionId(), exception.getMessage(), exception);
			throw new SessionInternalError(convertToError(exception), exception.getRawStatusCode());
		} catch (SessionInternalError sessionInternalError) {
			log.error("cancelRechargeTransaction failed for Transaction ID={} , sessionInternalError={}", cancelRechargeTransactionWS.getRechargeTransactionId(), sessionInternalError.getLocalizedMessage(), sessionInternalError);
			throw sessionInternalError;
		} catch (Exception exception) {
			log.error("cancelRechargeTransaction failed for Transaction ID={} , exception={}", cancelRechargeTransactionWS.getRechargeTransactionId(), exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(exception.getLocalizedMessage());
		}
	}

	public RechargeTransactionResponseWS getRechargeTransactionById(long id) throws PluggableTaskException {
		try {
			String urlUsageManagementService = getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService + END_POINT_RECHARGE_TRANSACTION + "?offSet=1&limit=1&filters[0].field=ID&filters[0].value=" + id;
			HttpHeaders headers = new HttpHeaders();

			HttpEntity<String> request = new HttpEntity<>(headers);
			ResponseEntity<RechargeTransactionResponseWS> response = externalClient.exchange(url, HttpMethod.GET, request, RechargeTransactionResponseWS.class);
			return response.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getRechargeTransaction failed errorResponse={}", exception.getResponseBodyAsString(), exception);
			return null;
		} catch (Exception exception) {
			log.error("getRechargeTransaction failed errorResponse : ", exception);
			return null;
		}
	}

	public List<String> getSubscriberNumbersByIdentificationNumber(int userId, String identificationNumber, String identificationType) {
		String findSubscriberNumbersByIdentificationNumber = "SELECT subscriber_number " +
				"FROM asset WHERE identifier " +
				"IN (SELECT bu.user_name FROM base_user bu " +
				"JOIN customer cu on bu.id = cu.user_id " +
				"WHERE cu.identification_type = '" + identificationType + "' " +
				"AND cu.identification_number = '" + identificationNumber +"' ";
		if (userId != 0) {
			findSubscriberNumbersByIdentificationNumber += "AND bu.id != " + userId + ")";
		}
		else {
			findSubscriberNumbersByIdentificationNumber += ")";
		}
		return jdbcTemplate.queryForList(findSubscriberNumbersByIdentificationNumber, String.class);
	}

	public boolean isSubscriberNumberUnaccounted(String subscriberNumber) {
		String sql = "SELECT subscriber_number FROM route_" + webServicesSessionBean.getCallerCompanyId() + "_unlimited_usage WHERE subscriber_number=?";
		SqlRowSet rs = jdbcTemplate.queryForRowSet(sql, subscriberNumber);
		if (rs.next()) {
			return StringUtils.hasLength(rs.getString("subscriber_number"));
		} else {
			return false;
		}
	}

	public boolean isSubscriberNumberOnline(String subscriberNumber) {
		try {
			if(!StringUtils.hasLength(subscriberNumber)) {
				return false;
			}
			String urlRadiusServerService =
					getValueFromExternalConfigParams(RADIUS_SERVER_SERVICE_URL);
			String url = urlRadiusServerService + END_POINT_SESSION + subscriberNumber;

			HttpHeaders headers = new HttpHeaders();
			HttpEntity<String> request = new HttpEntity<>(headers);
			ResponseEntity<RadiusSessionResponseWS> response = externalClient.exchange(url, HttpMethod.GET, request, RadiusSessionResponseWS.class);
			return response.getBody().getIsValid();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("isSubscriberNumberOnline failed for subscriberNumber={} due to={}", subscriberNumber, exception.getResponseBodyAsString(), exception);
		} catch (Exception exception) {
			log.error("isSubscriberNumberOnline failed for subscriberNumber={}", subscriberNumber, exception);
		}
		return false;
	}

	public void releaseSim(String identifier) {
		log.info("Releasing subscriber number={}", identifier);
		webServicesSessionBean.releaseSim(identifier);
	}

	public RechargeTransactionResponseWS getRechargeTransactionByParentTransactionId(Long transactionId) throws Exception {
		try {
			String urlUsageManagementService = getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
			String url = urlUsageManagementService.concat(END_POINT_RECHARGE_TRANSACTION.concat("?filters[0].field=PARENT_TXN_ID&filters[0].value=").concat(Long.toString(transactionId)));

			HttpHeaders headers = new HttpHeaders();
			HttpEntity<String> request = new HttpEntity<>(headers);
			ResponseEntity<RechargeTransactionResponseWS> rechargeTransactionResponseWSResponseEntity = externalClient.exchange(url, HttpMethod.GET, request, RechargeTransactionResponseWS.class);
			return rechargeTransactionResponseWSResponseEntity.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException exception) {
			log.error("getRechargeTransactionByParentTransactionId failed due to {}", exception.getResponseBodyAsString(), exception);
			throw new SessionInternalError(exception.getResponseBodyAsString());
		} catch (Exception exception) {
			log.error("getRechargeTransactionByParentTransactionId failed due to {}", exception.getLocalizedMessage(), exception);
			throw new SessionInternalError(exception.getMessage());
		}
	}

	public ReceiptWS getReceiptWsForTransactions(WalletTransactionResponseWS walletTransactionResponseWS, String receiptType, Integer userId, Locale locale) {
		try {
			if (!Objects.isNull(walletTransactionResponseWS)) {

				List<WalletTransactionWS> walletTransactions = walletTransactionResponseWS.getWalletTransactions();
				AdennetCustomerContactInformation customerContactInformation = getCustomerContactInformation(userId);
				List<AddOnProductWS> addOnProductWsList = new ArrayList<>();
				List<FeeWS> feeWSList = new ArrayList<>();

				log.info("Preparing receiptWS object for userId = {}", userId);
				ReceiptWS.ReceiptWSBuilder receiptWS = ReceiptWS.builder()
						.userName(customerContactInformation.getFirstName())
						.address(customerContactInformation.getAddress())
						.contactNumber(customerContactInformation.getContactNumber())
						.email(customerContactInformation.getEmailId())
						.userId(userId);

				if (receiptType.equals(RECEIPT_TYPE_ORIGINAL)) {
					walletTransactions.forEach(walletTransaction -> {

						String narration = walletTransaction.getNarration();
						String action = walletTransaction.getAction();
						BigDecimal amount = walletTransaction.getAmount();
						BigDecimal actualAmount = walletTransaction.getActualAmount();

						if (narration.equals(RECEIPT_TYPE_TOP_UP) && !action.startsWith(PREFIX_REFUND_OF) && amount.compareTo(BigDecimal.ZERO) > 0) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							receiptWS.topUpAmount(amount.abs())
									.receiptNumber(String.format("%s%s", DATE_TIME_FORMATTER_TRANSACTION.format(LocalDateTime.parse(walletTransaction.getRechargeDate(), ADENNET_DATE_TIME_FORMAT)), walletTransaction.getTxnId()))
									.receiptDate(walletTransaction.getRechargeDate())
									.subscriberNumber(walletTransaction.getSubscriberNumber())
									.createdBy(walletTransaction.getCreatedBy())
									.receiptType(messageSource.getMessage("receipt.type.pre.paid", null, locale));

						} else if (narration.equals(RECEIPT_TYPE_REFUND) || (narration.equals(RECEIPT_TYPE_SIM_REISSUE_FEE) && (amount.compareTo(BigDecimal.ZERO) > 0))) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							receiptWS.totalReceiptAmount(amount.abs())
									.operationType(messageSource.getMessage("receipt.operation.type.sim.reissue.fee", null, locale))
									.receiptNumber(String.format("%s%s", DATE_TIME_FORMATTER_TRANSACTION.format(LocalDateTime.parse(walletTransaction.getRechargeDate(), ADENNET_DATE_TIME_FORMAT)), walletTransaction.getTxnId()))
									.receiptDate(walletTransaction.getRechargeDate())
									.subscriberNumber(walletTransaction.getSubscriberNumber())
									.createdBy(walletTransaction.getCreatedBy())
									.receiptType(messageSource.getMessage("receipt.type.pre.paid", null, locale));
							if (narration.equals(RECEIPT_TYPE_REFUND)) {
								receiptWS.operationType(messageSource.getMessage("receipt.operation.type.refund", null, locale))
										.receiptType(messageSource.getMessage("receipt.type.refund", null, locale));
							}

						} else if (narration.equals(RECEIPT_TYPE_PLAN_PRICE) && !action.startsWith(PREFIX_REFUND_OF) && actualAmount.compareTo(BigDecimal.ZERO) > 0) {
							log.info(RECEIPT_AMOUNT_FOR, narration, actualAmount.abs());
							PrimaryPlanWS.PrimaryPlanWSBuilder primaryPlanWS = PrimaryPlanWS.builder()
									.price(actualAmount.abs())
									.cashPrice(amount.abs())
									.walletPrice(BigDecimal.valueOf(actualAmount.abs().longValue()).subtract(BigDecimal.valueOf(amount.abs().longValue())));
							receiptWS.primaryPlanWS(primaryPlanWS.build())
									.operationType(messageSource.getMessage("receipt.operation.type.plan.price", null, locale))
									.receiptNumber(String.format("%s%s", DATE_TIME_FORMATTER_TRANSACTION.format(LocalDateTime.parse(walletTransaction.getRechargeDate(), ADENNET_DATE_TIME_FORMAT)), walletTransaction.getTxnId()))
									.receiptDate(walletTransaction.getRechargeDate())
									.subscriberNumber(walletTransaction.getSubscriberNumber())
									.createdBy(walletTransaction.getCreatedBy())
									.receiptType(messageSource.getMessage("receipt.type.pre.paid", null, locale));

						} else if (narration.contains(RECEIPT_TYPE_MODEM) && !action.startsWith(PREFIX_REFUND_OF) && amount.compareTo(BigDecimal.ZERO) > 0) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							AddOnProductWS.AddOnProductWSBuilder addOnProductWSBuilder = AddOnProductWS.builder()
									.price(amount.abs())
									.name(messageSource.getMessage("receipt.operation.type.modem", null, locale));
							addOnProductWsList.add(addOnProductWSBuilder.build());

						} else if ( (narration.equals(RECEIPT_TYPE_SIM_CARD_FEE) || narration.equals(RECEIPT_TYPE_DOWNGRADE_FEE)) && !action.startsWith(PREFIX_REFUND_OF) && amount.compareTo(BigDecimal.ZERO) > 0) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							FeeWS.FeeWSBuilder feeWSBuilder = FeeWS.builder()
									.amount(amount.abs());
							if (narration.equals(RECEIPT_TYPE_DOWNGRADE_FEE)) {
								feeWSBuilder.description(messageSource.getMessage("receipt.operation.type.downgrade.fee", null, locale));
							} else {
								feeWSBuilder.description(messageSource.getMessage("receipt.operation.type.sim.card.fee", null, locale));
							}
							feeWSList.add(feeWSBuilder.build());
						}
					});
				} else {
					walletTransactions.forEach(walletTransaction -> {

						String narration = walletTransaction.getNarration();
						String action = walletTransaction.getAction();
						BigDecimal amount = walletTransaction.getAmount();
						BigDecimal actualAmount = walletTransaction.getActualAmount();

						if (narration.equals(RECEIPT_TYPE_TOP_UP) && action.startsWith(PREFIX_REFUND_OF)) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							receiptWS.topUpAmount(amount.abs())
									.receiptNumber(String.format("%s%s", DATE_TIME_FORMATTER_TRANSACTION.format(LocalDateTime.parse(walletTransaction.getRechargeDate(), ADENNET_DATE_TIME_FORMAT)), walletTransaction.getTxnId()))
									.receiptDate(walletTransaction.getRechargeDate())
									.subscriberNumber(walletTransaction.getSubscriberNumber())
									.createdBy(walletTransaction.getCreatedBy())
									.receiptType(messageSource.getMessage("receipt.type.refund", null, locale));

						} else if (narration.equals(REFUND_OF_SIM_RE_ISSUE_FEE) || narration.equals(RECEIPT_TYPE_REFUND)) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							receiptWS.totalReceiptAmount(amount.abs())
									.operationType(messageSource.getMessage("receipt.operation.type.refund", null, locale))
									.receiptNumber(String.format("%s%s", DATE_TIME_FORMATTER_TRANSACTION.format(LocalDateTime.parse(walletTransaction.getRechargeDate(), ADENNET_DATE_TIME_FORMAT)), walletTransaction.getTxnId()))
									.receiptDate(walletTransaction.getRechargeDate())
									.subscriberNumber(walletTransaction.getSubscriberNumber())
									.createdBy(walletTransaction.getCreatedBy())
									.receiptType(messageSource.getMessage("receipt.type.refund", null, locale));
							if (narration.equals(REFUND_OF_SIM_RE_ISSUE_FEE)) {
								receiptWS.operationType(messageSource.getMessage("receipt.operation.type.sim.reissue.refunded", null, locale));
							}

						} else if (narration.equals(RECEIPT_TYPE_PLAN_PRICE) && action.startsWith(PREFIX_REFUND_OF)) {
							log.info(RECEIPT_AMOUNT_FOR, narration, actualAmount.abs());
							PrimaryPlanWS.PrimaryPlanWSBuilder primaryPlanWS = PrimaryPlanWS.builder()
									.price(actualAmount.abs())
									.cashPrice(amount.abs())
									.walletPrice(BigDecimal.valueOf(actualAmount.abs().longValue()).subtract(BigDecimal.valueOf(amount.abs().longValue())));
							receiptWS.primaryPlanWS(primaryPlanWS.build())
									.operationType(messageSource.getMessage("receipt.operation.type.plan.price", null, locale))
									.receiptNumber(String.format("%s%s", DATE_TIME_FORMATTER_TRANSACTION.format(LocalDateTime.parse(walletTransaction.getRechargeDate(), ADENNET_DATE_TIME_FORMAT)), walletTransaction.getTxnId()))
									.receiptDate(walletTransaction.getRechargeDate())
									.subscriberNumber(walletTransaction.getSubscriberNumber())
									.createdBy(walletTransaction.getCreatedBy())
									.receiptType(messageSource.getMessage("receipt.type.refund", null, locale));

						} else if (narration.contains(RECEIPT_TYPE_MODEM) && action.startsWith(PREFIX_REFUND_OF)) {
							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							AddOnProductWS.AddOnProductWSBuilder addOnProductWSBuilder = AddOnProductWS.builder()
									.price(amount.abs())
									.name(messageSource.getMessage("receipt.operation.type.modem", null, locale));
							addOnProductWsList.add(addOnProductWSBuilder.build());

						} else if ((narration.equals(RECEIPT_TYPE_SIM_CARD_FEE) || narration.equals(RECEIPT_TYPE_DOWNGRADE_FEE)) && action.startsWith(PREFIX_REFUND_OF)) {

							log.info(RECEIPT_AMOUNT_FOR, narration, amount.abs());
							FeeWS.FeeWSBuilder feeWSBuilder = FeeWS.builder()
									.amount(amount.abs());
							if (narration.equals(RECEIPT_TYPE_DOWNGRADE_FEE)) {
								feeWSBuilder.description(messageSource.getMessage("receipt.operation.type.downgrade.fee", null, locale));
							} else {
								feeWSBuilder.description(messageSource.getMessage("receipt.operation.type.sim.card.fee", null, locale));
							}
							feeWSList.add(feeWSBuilder.build());
						}
					});
				}
				receiptWS.addOnProductWS(addOnProductWsList);
				receiptWS.feeWSList(feeWSList);
				return receiptWS.build();
			}
			return null;

		} catch (SessionInternalError sessionInternalError) {
			log.error("getReceiptWsForTransactions failed due to : {}", sessionInternalError.getLocalizedMessage());
			throw sessionInternalError;
		}
	}

	public Integer getMetaFieldIdByName(Integer entityId, EntityType[] entityType, String metaFieldName) {
		MetaField metaField = metaFieldDAS.getFieldByName(entityId, entityType, metaFieldName);
		return metaField != null ? metaField.getId() : 0;
	}

	public String getLoggedInUserGovernorate(Integer userId) {
		UserWS loggedInUser = webServicesSessionBean.getUserWS(userId);
		if (loggedInUser == null) {
			throw new SessionInternalError(String.format("User with ID=%d not found", userId));
		}
		Optional<MetaFieldValueWS> metaFieldValue = Arrays.stream(loggedInUser.getMetaFields())
				.filter(metaFieldValueWS -> metaFieldValueWS.getMetaField().getName().equals(META_FIELD_GOVERNORATE))
				.findFirst();
        return metaFieldValue.map(MetaFieldValueWS::getStringValue).orElse(null);
	}


	private String executeSshCommand(String ipAddress, String userName, String password, String... commands) {
		log.info("SSH to {}", ipAddress);
		final Long DEFAULT_TIMEOUT_SECONDS = 30L;

		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		try (ClientSession session = client.connect(userName, ipAddress, 22)
				.verify(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
				.getSession()) {
			session.addPasswordIdentity(password);
			session.auth()
			.verify(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
					ByteArrayOutputStream errorResponseStream = new ByteArrayOutputStream();
					ClientChannel channel = session.createChannel(Channel.CHANNEL_SHELL)) {
				channel.setOut(responseStream);
				channel.setErr(errorResponseStream);
				try {
					channel.open()
							.verify(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
					try (OutputStream pipedIn = channel.getInvertedIn()) {
						for(String command: commands) {
							pipedIn.write(command.getBytes());
							pipedIn.flush();
						}
					}
					channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT_SECONDS));
					String errorString = errorResponseStream.toString();
					if(!errorString.isEmpty()) {
						throw new SessionInternalError(errorString);
					}

					return responseStream.toString();
				} finally {
					channel.close(false);
				}
			}
		} catch (Exception ioException) {
			log.error("Error while making SSH connection to server={}", ipAddress);
			throw new SessionInternalError(String.format("validation.error.invalid.ip,", ipAddress), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		} finally {
			client.stop();
		}
	}

	public void provisionSubscriberNumberWithNewIMSINumber(String subscriberNumber, String newImsiNumber) throws PluggableTaskException {

		if (!StringUtils.hasLength(subscriberNumber)) {
			throw new SessionInternalError("Subscriber number is a required field for provisioning", HttpStatus.SC_BAD_REQUEST);
		}

		if (!StringUtils.hasLength(newImsiNumber)) {
			throw new SessionInternalError("IMSI is a required field for provisioning", HttpStatus.SC_BAD_REQUEST);
		}

		try {
			log.info("Provisioning for subscriber number={}", subscriberNumber);

			String isProdEnv = getValueFromExternalConfigParams(IS_PROD_ENV);

			// Check the env, if other than production then don't connect to ssh
			if(!Boolean.parseBoolean(isProdEnv)){
				return;
			}

			String ipAddress = getValueFromExternalConfigParams(HSS_SSH_IP);

			if(!StringUtils.hasLength(ipAddress)){
				throw new SessionInternalError("HSS server connection can not made due to incorrect ip address", HttpStatus.SC_BAD_REQUEST);
			}
			String hssSshUserName = getValueFromExternalConfigParams(HSS_SSH_USERNAME);
			String hssSshUserPassword = getValueFromExternalConfigParams(HSS_SSH_PASSWORD);

			String cmdLoginToPGW = String.format("LGI:OPNAME=\"ocs\",PWD=\"%s\";\n", hssSshUserPassword);
			String cmdProvisionSubscriberWithNewIMSI = String.format("MOD IMSI: ISDN=\"%s\", NEWIMSI =\"%s\";\n", subscriberNumber, newImsiNumber);

			String response = executeSshCommand(ipAddress, hssSshUserName, hssSshUserPassword, cmdLoginToPGW, cmdProvisionSubscriberWithNewIMSI);

			List<String> reasonCodes = new ArrayList<>();
			try (Scanner scanner = new Scanner(response)) {
				scanner.useDelimiter("\n");
				while (scanner.hasNext()) {
					String paragraph = scanner.next();
					if(paragraph.startsWith(RETCODE)) {
						reasonCodes.add(paragraph.replace("\n", "").replace("\r", ""));
					}
				}
			}

			if(reasonCodes.contains(RETCODE_0_OPERATION_IS_SUCCESSFUL)  && reasonCodes.contains(RETCODE_0_SUCCESS_0001_OPERATION_IS_SUCCESSFUL) ) {
				return ;
			}
			reasonCodes.remove(RETCODE_0_OPERATION_IS_SUCCESSFUL);
			reasonCodes.remove(RETCODE_0_SUCCESS_0001_OPERATION_IS_SUCCESSFUL);
			throw new SessionInternalError(reasonCodes.toString());
		} catch (PluggableTaskException pluggableTaskException) {
			log.error("Unable to load external configuration for HSS(Provisioning of subscriber number with new IMSI number).", pluggableTaskException);
			throw pluggableTaskException;
		}
	}

	public Integer getReportTypeByName(String reportType) {
		String sql = "SELECT id FROM report_type WHERE name=?";
		SqlRowSet rs = jdbcTemplate.queryForRowSet(sql, reportType);
		if (rs.next()) {
			return rs.getInt("id");
		}
		throw new SessionInternalError(VALIDATION_FAILED,
				new String[]{String.format("Report type for %s not found!", reportType)}, HttpStatus.SC_NOT_FOUND);
	}

	public void rollbackRechargeResponse(RechargeResponseWS rechargeResponse) {
		if(null == rechargeResponse || rechargeResponse.isTopUpRecharge() ||
				CollectionUtils.isEmpty(rechargeResponse.getOrderIds())) {
			return;
		}

		log.info("Rollback changes for userId= {}", rechargeResponse.getRechargeRequest().getUserId());
		try {
			if(rechargeResponse.isPlanChange()) {
				List<Integer> orderIds = rechargeResponse.getOrderIds();
				for(Integer orderId : orderIds) {
					OrderDTO order = orderDAS.find(orderId);
					if(order.isActive()) {
						// deleted order created for new plan.
						webServicesSessionBean.deleteOrder(orderId);
						deleteAssetAssignmentsForOrderId(orderId);
					}
				}
				// activate old orders and move assets from new order to old orders.
				for(Integer orderId : orderIds) {
					OrderDTO order = orderDAS.find(orderId);
					if(order.isFinished()) {
						order.setActiveUntil(null);
						Integer entityId = webServicesSessionBean.getCallerCompanyId();
						// activate existing order.
						order.setOrderStatus(findActiveStatusForEntityId(entityId));
						List<AssetAssignmentDTO> assetAssignments =  assetAssignmentDAS.getAssignmentsForOrder(orderId);
						if(!CollectionUtils.isEmpty(assetAssignments)) {
							// update asset with older orderLines from deleted AssetAssignments.
							for(AssetAssignmentDTO assetAssignment : assetAssignments) {
								if(null!=assetAssignment.getEndDatetime()) {
									OrderLineDTO orderLine = assetAssignment.getOrderLine();
									AssetDTO asset = assetAssignment.getAsset();
									asset.setOrderLine(orderLine);
									asset.setAssetStatus(assetStatusDAS.findActiveStatusForItem(orderLine.getItemId()));
									assetAssignment.setEndDatetime(null);
								}
							}
						}
						orderDAS.save(order);
					}
				}
			} else {
				Integer orderId = rechargeResponse.getOrderIds().get(0);
				// delete newly created re charge order.
				webServicesSessionBean.deleteOrder(orderId);
				deleteAssetAssignmentsForOrderId(orderId);
			}
		} catch(Exception exception) {
			log.error("rollbackRechargeResponse failed for rechargeResponse={}", rechargeResponse);
		}
	}

	public OrderStatusDTO findActiveStatusForEntityId(Integer entityId) {
		for(OrderStatusDTO orderStatus : orderStatusDAS.findAll(entityId)) {
			if(OrderStatusFlag.INVOICE.equals(orderStatus.getOrderStatusFlag())) {
				return orderStatus;
			}
		}
		throw new SessionInternalError("findActiveStatusForEntityId failed", new String[] { "entity "+ entityId
				+ " has no Active OrderStatus" }, HttpStatus.SC_INTERNAL_SERVER_ERROR);
	}

	private void deleteAssetAssignmentsForOrderId(Integer orderId) {
		try {
			// fetch deleted order's assetAssignments .
			List<AssetAssignmentDTO> assetAssignments =  assetAssignmentDAS.getAssignmentsForOrder(orderId);
			if(!CollectionUtils.isEmpty(assetAssignments)) {
				// delete asset assignments for order.
				for(AssetAssignmentDTO assetAssignment : assetAssignments) {
					if(null!=assetAssignment.getEndDatetime()) {
						AssetDTO asset = assetAssignment.getAsset();
						// remove current assetAssignment from asset's assetAssignment association.
						Set<AssetAssignmentDTO> updatedAssetAssignments = asset.getAssignments()
								.stream()
								.filter(assetAssignment1 -> assetAssignment1.getId()!= assetAssignment.getId())
								.collect(Collectors.toSet());
						asset.setAssignments(updatedAssetAssignments);
						// remove asset association from current assetAssignment.
						assetAssignment.setAsset(null);

						OrderLineDTO orderLine = assetAssignment.getOrderLine();
						// remove current assetAssignment from orderLine's assetAssignment association.
						Set<AssetAssignmentDTO> updatedOlAssetAssignments = orderLine.getAssetAssignments()
								.stream()
								.filter(assetAssignment1 -> assetAssignment1.getId()!= assetAssignment.getId())
								.collect(Collectors.toSet());
						orderLine.setAssetAssignments(updatedOlAssetAssignments);

						// remove orderLines association from current assetAssignment.
						assetAssignment.setOrderLine(null);
						// delete assignment.
						assetAssignmentDAS.delete(assetAssignment);
					}
				}
			}
		} catch(Exception exception) {
			throw new SessionInternalError("deleteAssetAssignmentsForOrderId failed ", new String[] {"deleteAssetAssignmentsForOrderId "
					+ "failed, "+ exception.getLocalizedMessage()}, HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public List<UserRowMapper> getAllUsersLoginNameStatusAndGovernorate() {
		//Fetching data from db
		List<UserRowMapper> users = jdbcTemplate.query(FIND_USER_LOGIN_NAME_STATUS_AND_GOVERNORATE, new BeanPropertyRowMapper<>(UserRowMapper.class));
		if (!CollectionUtils.isEmpty(users)) {
			return users;
		} else {
			return null;
		}
	}

	public void fireAuditEvent(Integer userAffectedId, Integer rowId, Integer messageId, String oldStr) {
		if (!StringUtils.isEmpty(oldStr)) {
			eLogger.auditLog(webServicesSessionBean.getCallerId(), userAffectedId, Constants.TABLE_CUSTOMER, rowId,
					EventLogger.MODULE_USER_MAINTENANCE, messageId, null, oldStr, null);
		}
	}

	private boolean isPlanChanged(ConsumptionUsageMapResponseWS consumptionUsageMapResponse) {
		boolean isPlanChanged = false;
		if (consumptionUsageMapResponse != null && !consumptionUsageMapResponse.getConsumptionUsageDetails().isEmpty()) {
			if (!consumptionUsageMapResponse.getConsumptionUsageDetails().get(0).getStatus().equals(ACTIVE_CONSUMPTION_USAGE_MAP)) {
				isPlanChanged = true;
			}
		}
		return isPlanChanged;
	}

	public List<UserInfoRowMapper> getUserDetails(List<Integer> userIds) {
		List<UserInfoRowMapper> userInfoRowMapperList = Collections.emptyList();
		try {
			if (!CollectionUtils.isEmpty(userIds)) {
				JdbcTemplate umsJdbcTemplate = new JdbcTemplate(Context.getBean(Context.Name.DATA_SOURCE_UMS));
				//fetch data from ums db
				userInfoRowMapperList = umsJdbcTemplate.query(String.format(FIND_CUSTOMER_DETAIL_BY_ID, String.join(",", Collections.nCopies(userIds.size(), "?"))), userIds.toArray(),
						(resultSet, i) -> new UserInfoRowMapper(resultSet.getInt("user_id"), resultSet.getString("subscriber_number"),
								resultSet.getString("plan_description"), resultSet.getString("plan_status"),
								resultSet.getBigDecimal("wallet_balance")));
			}
			return userInfoRowMapperList;
		} catch (Exception exception) {
			log.error("export customer csv failed due to {}", exception.getMessage(), exception);
			throw new SessionInternalError("Unable to fetch subscriber details from ums database: ", HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private boolean isSubscriberNumberReleased(String subscriberNumber){
		String assetStatus = jdbcTemplate.queryForObject(GET_ASSET_STATUS,new Object[]{subscriberNumber},String.class);
		return assetStatus.equalsIgnoreCase(ASSET_STATUS_RELEASED);
	}

	public List<String> getUsernameForBankUsers() {
		try {
			DataSource dataSource = Context.getBean(Context.Name.DATA_SOURCE_UMS);
			JdbcTemplate umsJdbcTemplate = new JdbcTemplate(dataSource);

			String query = "SELECT username FROM bank_users ORDER BY username";
			List<String> usernames = umsJdbcTemplate.queryForList(query, String.class);

			return usernames;
		}
		catch (Exception exception) {
			log.error("getUsernameForBankUsers failed due to {}", exception.getMessage(), exception);
			throw new SessionInternalError("Unable to fetch bank usernames", HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public List<String> getAuditLogFields() {
		JdbcTemplate jdbcTemplate = Context.getBean(Context.Name.JDBC_TEMPLATE);
		List<String> fields = new ArrayList<>();
		String query = "select distinct(field_name) from route_60_audit_log_field";
		SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(query);
		while (sqlRowSet.next()) {
			fields.add(sqlRowSet.getString("field_name"));
		}
		//sort fields in ascending order
		Collections.sort(fields);
		return fields;
	}

	public static Date convertToAdenTimezone(Date date, String timezone) {
		if (date == null) {
			return null;
		}
		return Date.from(Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.of(timezone)).toLocalDateTime().atZone(ZoneId.of(AdennetConstants.ADENNET_TIMEZONE)).toInstant());
	}

	public String getUserNameByUserId(Integer userId) {
		String userName = userDAS.getUserNameById(userId);
		if (userName == null) {
			throw new SessionInternalError(String.format("User with Id=%d not found.", userId), HttpStatus.SC_NOT_FOUND);
		}
		return userName;
	}

	public AssetDTO getAssetByIdentifier(String identifier) {
		try {
			AssetDTO assetDTO = assetDAS.getAssetByIdentifier(identifier);
			if (assetDTO == null) {
				log.error("Asset not found by identifier={}", identifier);
				throw new SessionInternalError(String.format("Asset not found by identifier=%s", identifier), HttpStatus.SC_NOT_FOUND);
			}
			return assetDTO;
		} catch (Exception exception) {
			throw new SessionInternalError(String.format("Failed to get Asset by identifier=%s", identifier), HttpStatus.SC_BAD_REQUEST);
		}
	}

	public Integer entityIdByUserId(Integer userId){
		return userDAS.getEntityByUserId(userId);
	}

	public Integer createPayment(RechargeRequestWS rechargeRequestWS) {
		try {
			String note = String.format("Payment received for userId = %d against prepaid plan %s",rechargeRequestWS.getUserId(), rechargeRequestWS.getPrimaryPlan().getDescription());

			PaymentInformationWS paymentInformation = new PaymentInformationWS();
			paymentInformation.setUserId(rechargeRequestWS.getUserId());
			paymentInformation.setProcessingOrder(1);
			paymentInformation.setPaymentMethodTypeId(1);

			PaymentWS payment = new PaymentWS();
			payment.setAmount(getPaymentAmount(rechargeRequestWS));
			payment.setIsRefund(0);
			payment.setCurrencyId(userDAS.getCurrencyByUserId(rechargeRequestWS.getUserId()));
			payment.setMethodId(Constants.PAYMENT_METHOD_CUSTOM);
			payment.setPaymentDate(TimezoneHelper.companyCurrentDate(rechargeRequestWS.getEntityId()));
			payment.setUserId(rechargeRequestWS.getUserId());
			payment.setPaymentNotes(note);
			payment.setPaymentPeriod(1);
			payment.getPaymentInstruments().add(paymentInformation);

			return webServicesSessionBean.createPayment(payment);
		} catch (Exception exception) {
			log.error("Failed to create payment for userId={} and plan={}, exception={}", rechargeRequestWS.getUserId(), rechargeRequestWS.getPrimaryPlan().getDescription(), exception.getMessage());
			throw new SessionInternalError(String.format("Failed to create payment for userId=%s", rechargeRequestWS.getUserId()), HttpStatus.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private BigDecimal getPaymentAmount(RechargeRequestWS rechargeRequestWS) {
		BigDecimal amount = BigDecimal.ZERO;
		amount = amount.add(rechargeRequestWS.getPrimaryPlan() != null ? rechargeRequestWS.getPrimaryPlan().getPrice() : amount);

		if (rechargeRequestWS.getFees() != null) {
			for (FeeWS fee : rechargeRequestWS.getFees()) {
				amount = amount.add(fee.getAmount());
			}
		}
		if (rechargeRequestWS.getAddOnProducts() != null) {
			for (AddOnProductWS addOnProduct : rechargeRequestWS.getAddOnProducts()) {
				amount = amount.add(addOnProduct.getPrice());
			}
		}
		return amount;
	}

	public boolean isAssetAvailableBySubscriberNumber(String subscriberNumber) {
		AssetDTO assetDTO = assetDAS.getAssetBySubscriberNumber(subscriberNumber);
		if (assetDTO != null) {
			return assetDTO.getAssetStatus().getIsAvailable() == 1;
		}
		return false;
	}

	private PrimaryPlanWS getPrimaryPlanWS(String internalNumber) {
		String dateFormat;
		String tableName;
		Integer entityId;
		Integer addOnCategoryId = null;
		try {
			addOnCategoryId = Integer.valueOf(getValueFromExternalConfigParams(ADD_ON_PKG_CATEGORY_ID));
			entityId = webServicesSessionBean.getCallerCompanyId();
			MetaFieldValueWS[] metaFields = webServicesSessionBean.getCompany().getMetaFields();
			tableName = getMetaFieldValue(metaFields, TAX_TABLE_NAME);
			dateFormat = getMetaFieldValue(metaFields, TAX_DATE_FORMAT);
		} catch (PluggableTaskException e) {
			throw new RuntimeException(e);
		}
		return new PlanBL().getPlan(internalNumber, entityId, addOnCategoryId, tableName, dateFormat);
	}

	private String getMetaFieldValue(MetaFieldValueWS[] metaFields, String fieldName) {
		return Arrays.stream(metaFields)
				.filter(field -> field.getFieldName().equalsIgnoreCase(fieldName))
				.map(MetaFieldValueWS::getStringValue)
				.findFirst()
				.orElse(null);
	}
}
