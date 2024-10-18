package com.sapienter.jbilling.server.spc;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.server.invoice.db.SpcDetailedBillingReportDTO;

@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class SpcReportHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BATCH_INSERT_SQL = String.join("\n",
            "INSERT INTO detailed_billing_log",
            "            (billing_process_id, invoice_id, invoice_date, revenue_gl_code, user_id, user_name, product_code, call_identifier, plan_or_product_name, product_end_date, service_email, service_number , service_type, service_description, costs_gl_code  , plan_type, tax_code, sales_ex_gst, gst, rollup_code, super_rollup_code, super_super_rollup_code, tariff_code, cost_of_service, origin, tariff_description , rollup_description , super_rollup_description, super_super_rollup_description , from_date, to_date )",
            "     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

    @Resource(name = "jBillingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Resource(name = "namedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final String FROM_DATE_SQL = 
            "SELECT start_date " 
                    + "  FROM service_summary "
                    + " WHERE invoice_line_id =:invoiceLineId ";

    private static final String ORDER_PROCESS_FROM_DATE_SQL =
            "SELECT period_start "
                    + " FROM order_process "
                    + "WHERE invoice_id =:invoiceId "
                    + " AND order_id =:orderId ";

    private static final String ORDER_PROCESS_TO_DATE_SQL =
            "SELECT period_end "
                    + " FROM order_process "
                    + "WHERE invoice_id =:invoiceId "
                    + "  AND order_id =:orderId ";

    private static final String ACTIVE_SINCE_SQL =
            "SELECT active_since "
                    + " FROM purchase_order "
                    + "WHERE id =:orderId ";

    private static final String TO_DATE_SQL = 
            "SELECT end_date " 
                    + "  FROM service_summary "
                    + " WHERE invoice_line_id =:invoiceLineId ";

    private static final String SERVICE_TYPE_SQL = 
            "SELECT Trim(REPLACE(String_agg(description, ','), 'Service Type -', '')) "
                    + "FROM item_type it "
                    + "INNER JOIN item_type_map itm ON it.id = itm.type_id "
                    + "AND item_id = :itemId "
                    + "WHERE "
                    + "description LIKE 'Service Type -MODULUS' ";

    private static final String SERVICE_DESCRIPTION_SQL = 
            "SELECT "
                    + "String_agg(service_description, ', ') FROM route_70_service_type_description "
                    + "WHERE service_type IN (SELECT Trim(REPLACE(description, "
                    + "'Service Type -', '' )) "
                    + "FROM item_type it "
                    + "INNER JOIN item_type_map itm ON it.id = itm.type_id "
                    + "AND itm.item_id = :itemId "
                    + "WHERE description LIKE 'Service Type -MODULUS') ";

    private static final String PLAN_TYPE_SQL = 
            "SELECT String_agg(description, ',') "
                    + "FROM item_type it "
                    + "INNER JOIN item_type_map itm "
                    + "ON it.id = itm.type_id "
                    + "AND item_id = :itemId ";

    private static final String TAX_CODE_SQL = 
            "SELECT tax_code "
                    + "   FROM route_70_tax_scheme "
                    + "  WHERE description =:desc ";

    private static final String INTERNATIONAL_CONTENT =
            "SELECT content "
                    + " FROM international_description "
                    + "WHERE foreign_id = :foreignId "
                    + "  AND table_id = (SELECT id "
                    + "                    FROM jbilling_table "
                    + "                   WHERE name = :tableName) "
                    + "  AND language_id = 1 ";

    private static final String ROLLUP_CODE_SQL = 
            "SELECT rollup_code "
                    + "  FROM rollup_codes "
                    + " WHERE item_type_description = :content ";

    private static final String SUPER_ROLLUP_CODE_SQL = 
            "SELECT rollup_code_type "
                    + "  FROM rollup_codes "
                    + " WHERE item_type_description = :content ";

    private static final String SUPER_SUPER_ROLLUP_CODE_SQL = 
            "SELECT rollup_code_type "
                    + "  FROM rollup_codes "
                    + " WHERE rollup_code = (SELECT rollup_code_type "
                    + "                        FROM rollup_codes "
                    + "                       WHERE item_type_description = :content) ";

    private static final String TARRIF_CODE_SQL = 
            "SELECT TRIM(REPLACE(SPLIT_PART"
                    + "(SUBSTRING(jmr.pricing_fields FROM '%TARIFF_CODE:1:string:#\"%#\"%' FOR '#'),',',1), '%3A', ':')) "
                    + "FROM jbilling_mediation_record jmr "
                    + "WHERE jmr.order_id = :orderId "
                    +  "AND jmr.item_id = :itemId LIMIT 1 ";

    private static final String COST_OF_SERVICE_SQL = 
            "SELECT jmr.rated_price "
                    + "             FROM jbilling_mediation_record jmr "
                    + "            WHERE jmr.order_id = :orderId "
                    + "              AND jmr.item_id = :itemId ";

    private static final String TARIFF_DESCRIPTION_SQL =
            "SELECT tariff_description "
                    + "   FROM route_70_tariff_description "
                    + "  WHERE tariff_code = :tariffCode ";

    private static final String ROLLUP_DESCRIPTION_SQL =
            "SELECT description "
                    + "   FROM route_70_rollup_description "
                    + "  WHERE code = :rollUpCode ";

    private static final String SUPER_ROLLUP_DESCRIPTION_SQL =
            "SELECT description "
                    + "   FROM route_70_rollup_description "
                    + "  WHERE  code = :superRollUpCode ";

    private static final String SUPER_SUPER_ROLLUP_DESCRIPTION_SQL =
            "SELECT description "
                    + "   FROM route_70_rollup_description "
                    + "  WHERE code = :superSuperRollUpCode ";

    private static final String IS_PLAN_SQL =
            "SELECT id "
                    + "   FROM plan "
                    + "  WHERE item_id = :itemId ";

    private static final String PRODUCT_CODE_SQL =
            "SELECT internal_number,active_until,gl_code "
                    + "   FROM item "
                    + "  WHERE id = :itemId ";

    private static final String PLAN_META_FIELD_SQL =
            "SELECT mfv.string_value "
                    + "      FROM meta_field_value mfv "
                    + "INNER JOIN plan_meta_field_map pmfm "
                    + "        ON mfv.id = pmfm.meta_field_value_id "
                    + "INNER JOIN meta_field_name mfn "
                    + "        ON mfv.meta_field_name_id = mfn.id "
                    + "       AND mfn.name = :name "
                    + "INNER JOIN plan p "
                    + "        ON p.id = pmfm.plan_id "
                    + "       AND p.item_id = :itemId ";

    private static final String USER_META_FIELD_SQL = 
            "SELECT mfv.string_value FROM customer c "
                    + "INNER JOIN customer_meta_field_map map ON map.customer_id = c.id "
                    + "INNER JOIN meta_field_value mfv ON mfv.id = map.meta_field_value_id "
                    + "INNER JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id "
                    + "AND mfn.name = 'Origin' "
                    + "WHERE c.user_id = :userId ";

    private static final String ASSET_METAFIELDS = 
            "SELECT string_agg(string_value, ', ') "
                    + "      FROM   meta_field_value mfv "
                    + "INNER JOIN meta_field_name mfn " 
                    + "        ON mfn.id = mfv.meta_field_name_id " 
                    + "       AND mfn.name = :name "
                    + "       AND entity_type = 'ASSET' "
                    + "INNER JOIN asset_meta_field_map amfm "
                    + "        ON mfv.id = amfm.meta_field_value_id "
                    + "INNER JOIN asset a "
                    + "        ON a.id = amfm.asset_id "
                    + "       AND ";

    private static final String ASSET_METAFIELDS_01 = " a.id IN (SELECT id FROM asset WHERE identifier IN (:callIdentifiers)) ";
    private static final String ASSET_METAFIELDS_02 = " a.identifier IN (:callIdentifiers) ";
    private static final String ASSET_METAFIELDS_03 = " order_line_id IN (SELECT id FROM order_line WHERE order_id = :orderId) ";

    private static final String ASSET_IDENTIFIERS_01 = 
            "SELECT identifier " 
                    + "   FROM asset " 
                    + "  WHERE id IN " 
                    + "(SELECT asset_id " 
                    + "   FROM asset_assignment  "
                    + "  WHERE order_line_id IN (SELECT id " 
                    + "   FROM order_line " 
                    + "  WHERE order_id = :orderId)) ";

    private static final String ASSET_IDENTIFIERS_02 = 
            "SELECT a.identifier "
                    + "      FROM asset a " 
                    + "INNER JOIN asset_assignment am ON a.id=am.asset_id " 
                    + "INNER JOIN order_line ol ON am.order_line_id= ol.id " 
                    + "INNER JOIN purchase_order po_in ON ol.order_id=po_in.id "
                    + "     WHERE ol.order_id = :subOrderId"
                    + "       AND am.start_datetime=(SELECT max(am1.start_datetime) "
                    + "      FROM asset a1 "
                    + "INNER JOIN asset_assignment am1 ON a1.id=am1.asset_id " 
                    + "INNER JOIN order_line ol1 ON am1.order_line_id= ol1.id "
                    + "INNER JOIN purchase_order po1 ON ol1.order_id=po1.id "
                    + "     WHERE ol1.order_id = :subOrderId "
                    + "       AND am1.start_datetime <= po1.active_since) ";

    private static final String SUBSCRIPTION_ORDER_ID_SQL = 
            "SELECT mfv.integer_value "
                    + "      FROM order_meta_field_map omfm "
                    + "INNER JOIN meta_field_value mfv ON omfm.meta_field_value_id=mfv.id " 
                    + "INNER JOIN meta_field_name mfn ON mfv.meta_field_name_id=mfn.id "
                    + "     WHERE mfn.name='Subscription Order Id' and mfn.entity_type='ORDER' "
                    + "       AND omfm.order_id = :orderId ";

    private static final String ITEM_METAFIELD_SQL =
            "SELECT mfval.string_value "+
                    "FROM meta_field_value mfval, meta_field_name mfnam, item_meta_field_map imfmap "+
                    "WHERE mfval.meta_field_name_id = mfnam.id AND imfmap.meta_field_value_id = mfval.id AND "+
                    "mfnam.entity_type = 'PRODUCT' AND mfnam.name = :name AND imfmap.item_id =:itemId";

    private static final String USER_NAME_SQL =  "SELECT user_name FROM base_user WHERE id =:userId ";

    public String validateAndGetBillingProcessDate(Integer entityId, String billingDate) {
        String validBillingDate = null;
        String sql =
                "SELECT billing_date "
                        + "FROM billing_process "
                        + "WHERE is_review = 0 AND entity_id = ? AND billing_date = date(?)";

        SqlRowSet rs = jdbcTemplate.queryForRowSet(sql, entityId, "'" + billingDate + "'");
        if (rs.next()) {
            validBillingDate = rs.getString("billing_date");
        }
        return validBillingDate;
    }

    public String getLatestBillingProcessDate(Integer entityId) {
        String billingProcessDate = null;
        String sql = 
                "SELECT billing_date "
                        + "FROM billing_process "
                        + "WHERE id = (SELECT max(id) FROM billing_process WHERE is_review = 0 AND entity_id = ?)";

        SqlRowSet latestBPRowSet = jdbcTemplate.queryForRowSet(sql, entityId);
        if (latestBPRowSet.next()) {
            billingProcessDate = latestBPRowSet.getString("billing_date");
        }
        return billingProcessDate;
    }

    public boolean shouldPopulateDetailedBillingReportTable(Integer entityId, String billingDate) {
        boolean shouldPopulate = false;
        String sql =
                "SELECT id "
                        + "FROM billing_process "
                        + "WHERE is_review = 0 AND entity_id = ? AND billing_date = date(?) "
                        + "AND id NOT IN (SELECT billing_process_id FROM detailed_billing_log WHERE billing_process_id IS NOT NULL)";

        SqlRowSet rs = jdbcTemplate.queryForRowSet(sql, entityId, "'" + billingDate + "'");
        if (rs.next()) {
            shouldPopulate = rs.getInt("id") != 0;
        }
        return shouldPopulate;
    }

    public void deleteSpcDetailedBillingReportRecords(String strInvoiceDate) {
        String sql = 
                "DELETE "
                        + "FROM detailed_billing_log "
                        + "WHERE invoice_date = date(?)";
        int count = jdbcTemplate.update(sql, strInvoiceDate);
        logger.debug("Deleted {} SpcDetailedBillingReport records for invoice date {}", count, strInvoiceDate);
    }

    public List<Map<String, Object>> findInvoicesByBillingProcessPaged(String strBillingDate, Integer maxResults, Integer lastInvoiceId) {
        String sql = 
                "SELECT i.id, "
                        + "i.billing_process_id, "
                        + "i.user_id, "
                        + "i.create_datetime, "
                        + "i.public_number "
                        + "FROM invoice i "
                        + "WHERE i.deleted = 0 AND i.is_review = 0 "
                        + "AND date(i.create_datetime) = date(:billingDate) ";

        if (lastInvoiceId != null) {
            sql = sql + "AND i.id > :lastInvoiceId ";
        }
        sql = sql + "ORDER BY i.id ASC LIMIT :maxResults";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("billingDate", strBillingDate);
        if (lastInvoiceId != null) {
            parameters.addValue("lastInvoiceId", lastInvoiceId);
        }
        parameters.addValue("maxResults", maxResults);

        List<Map<String, Object>> invoicesList = new ArrayList<Map<String, Object>>();
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(sql, parameters);
        while(row.next()) {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("id", row.getInt("id"));
            rowMap.put("billing_process_id", row.getObject("billing_process_id"));
            rowMap.put("user_id", row.getInt("user_id"));
            rowMap.put("create_datetime", row.getDate("create_datetime"));
            rowMap.put("public_number", row.getString("public_number"));
            invoicesList.add(rowMap);
        }
        return invoicesList;
    }

    public List<Map<String, Object>> findInvoiceLinesByInvoiceId(Integer invoiceId) {
        String sql = 
                "SELECT il.id, "
                        + "il.item_id, "
                        + "il.description, "
                        + "il.amount, "
                        + "il.tax_amount, "
                        + "il.gross_amount, "
                        + "il.order_id, "
                        + "il.type_id, "
                        + "il.call_identifier "
                        + "FROM invoice_line il "
                        + "WHERE il.deleted = 0 "
                        + "AND il.type_id != 3 "
                        + "AND il.invoice_id = :invoiceId";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("invoiceId", invoiceId);

        List<Map<String, Object>> invoiceLinesList = new ArrayList<Map<String, Object>>();
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(sql, parameters);
        while(row.next()) {
            Map<String, Object> rowMap = new HashMap<>();
            rowMap.put("id", row.getInt("id"));
            rowMap.put("item_id", row.getInt("item_id"));
            rowMap.put("description", row.getString("description"));
            rowMap.put("amount", row.getBigDecimal("amount"));
            rowMap.put("tax_amount", row.getBigDecimal("tax_amount"));
            rowMap.put("gross_amount", row.getBigDecimal("gross_amount"));
            rowMap.put("order_id", row.getInt("order_id"));
            rowMap.put("type_id", row.getInt("type_id"));
            rowMap.put("call_identifier", row.getString("call_identifier"));
            invoiceLinesList.add(rowMap);
        }
        return invoiceLinesList;
    }

    public void executeBatchInsert(final List<SpcDetailedBillingReportDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        logger.debug("Inserting {} SpcDetailedBillingReport records:", rows.size());
        jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int batch) throws SQLException {
                SpcDetailedBillingReportDTO billingReportRow = rows.get(batch);
                ps.setObject(1, billingReportRow.getBillingProcessId());
                ps.setInt(2, billingReportRow.getInvoiceId());
                ps.setDate(3, new java.sql.Date(billingReportRow.getInvoiceDate().getTime()));
                ps.setString(4, billingReportRow.getRevenueGlCode());
                ps.setInt(5, billingReportRow.getUserId());
                ps.setString(6, billingReportRow.getUserName());
                ps.setString(7, billingReportRow.getProductCode());
                ps.setString(8, billingReportRow.getCallIdentifier());
                ps.setString(9, billingReportRow.getPlanOrProductName());
                Date productEndDate = billingReportRow.getProducEndDate();
                ps.setDate(10, productEndDate != null ? new java.sql.Date(productEndDate.getTime()) : null);
                ps.setString(11, billingReportRow.getServiceEmail());
                ps.setString(12, billingReportRow.getServiceNumber());
                ps.setString(13, billingReportRow.getServiceType());
                ps.setString(14, billingReportRow.getServiceDescription());
                ps.setString(15, billingReportRow.getCostsGlCode());
                ps.setString(16, billingReportRow.getPlanType());
                ps.setString(17, billingReportRow.getTaxCode());
                ps.setBigDecimal(18, billingReportRow.getSalesExGst());
                ps.setBigDecimal(19, billingReportRow.getGst());
                ps.setString(20, billingReportRow.getRollupCode());
                ps.setString(21, billingReportRow.getSuperRollupCode());
                ps.setString(22, billingReportRow.getSuperSuperRollupCode());
                ps.setString(23, billingReportRow.getTariffCode());
                ps.setBigDecimal(24, billingReportRow.getCostOfService());
                ps.setString(25, billingReportRow.getOrigin());
                ps.setString(26, billingReportRow.getTariffDescription());
                ps.setString(27, billingReportRow.getRollupDescription());
                ps.setString(28, billingReportRow.getSuperRollupDescription());
                ps.setString(29, billingReportRow.getSuperSuperRollupDescription());
                Date fromDate = billingReportRow.getFromDate();
                ps.setDate(30, fromDate != null ? new java.sql.Date(fromDate.getTime()) : null);
                Date toDate = billingReportRow.getToDate();
                ps.setDate(31, toDate != null ? new java.sql.Date(toDate.getTime()) : null);
            }

            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    /**
     * Write separate method here to get the value/data of each field of report.
     * Below is one of fields given as an example for developing the logic.
     */
    public String getItemMetafieldByName(Integer itemId,String name) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        parameters.addValue("name", name);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(ITEM_METAFIELD_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getServiceType(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SERVICE_TYPE_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getServiceDescription(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SERVICE_DESCRIPTION_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getPlanType(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(PLAN_TYPE_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getTaxCode(String description) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("desc", description);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(TAX_CODE_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getRollUpCode(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String content = getInternationalContent(itemId, "item");
        parameters.addValue("content", content);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(ROLLUP_CODE_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getSuperRollUpCode(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String content = getInternationalContent(itemId, "item");
        parameters.addValue("content", content);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SUPER_ROLLUP_CODE_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getSuperSuperRollUpCode(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String content = getInternationalContent(itemId, "item");
        parameters.addValue("content", content);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SUPER_SUPER_ROLLUP_CODE_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getTariffCode(Integer itemId,Integer orderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        parameters.addValue("orderId", orderId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(TARRIF_CODE_SQL, parameters);
        return row.next() ? decode(row.getString(1)) : StringUtils.EMPTY;
    }

    public BigDecimal getCostOfService(Integer itemId,Integer orderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        parameters.addValue("orderId", orderId);
        List<BigDecimal> ratedPriceList = namedParameterJdbcTemplate.queryForList(COST_OF_SERVICE_SQL, parameters , BigDecimal.class);
        return ratedPriceList.stream().reduce(BigDecimal.ZERO, BigDecimal :: add);
    }

    public String getTariffDescription(String tariffCode) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("tariffCode", tariffCode);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(TARIFF_DESCRIPTION_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getRollupDescription(String rollUpCode) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("rollUpCode", rollUpCode);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(ROLLUP_DESCRIPTION_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getSuperRollupDescription(String superRollUpCode) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("superRollUpCode", superRollUpCode);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SUPER_ROLLUP_DESCRIPTION_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getSuperSuperRollupDescription(String superSuperRollUpCode) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("superSuperRollUpCode", superSuperRollUpCode);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SUPER_SUPER_ROLLUP_DESCRIPTION_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public Date getFromDate(Integer lineId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("invoiceLineId", lineId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(FROM_DATE_SQL, parameters);
        return row.next() ? row.getDate(1) : null;
    }

    public Date getToDate(Integer lineId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("invoiceLineId", lineId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(TO_DATE_SQL, parameters);
        return row.next() ? row.getDate(1) : null;
    }

    public Date getOrderProcessFromDate(Integer orderId,Integer invoiceId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("orderId", orderId);
        parameters.addValue("invoiceId", invoiceId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(ORDER_PROCESS_FROM_DATE_SQL, parameters);
        return row.next() ? row.getDate(1) : null;
    }

    public Date getOrderProcessToDate(Integer orderId,Integer invoiceId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("orderId", orderId);
        parameters.addValue("invoiceId", invoiceId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(ORDER_PROCESS_TO_DATE_SQL, parameters);
        return row.next() ? row.getDate(1) : null;
    }

    public Date getActiveSince(Integer orderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("orderId", orderId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(ACTIVE_SINCE_SQL, parameters);
        return row.next() ? row.getDate(1) : null;
    }

    public boolean isPlan(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(IS_PLAN_SQL, parameters);
        return row.next() ? true : false;
    }

    public Map<String, Object> getProduct(Integer itemId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(PRODUCT_CODE_SQL, parameters);
        Map<String, Object> rowMap = new HashMap<>();
        if (row.next()) {
            rowMap.put("active_until", row.getDate("active_until"));
            rowMap.put("internal_number", row.getString("internal_number"));
            rowMap.put("gl_code", row.getString("gl_code"));
            return rowMap;
        }
        return null;
    }

    public String getPlanOrProductName(Integer itemId) {
        return getInternationalContent(itemId,"item");
    }

    public String getPlanMetaFiledByName(Integer itemId, String name) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("itemId", itemId);
        parameters.addValue("name", name);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(PLAN_META_FIELD_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getUserMetaFiledByName(Integer userId, String name) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);
        parameters.addValue("name", name);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(USER_META_FIELD_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public List<String> getAssetMetaFieldByName(Integer orderId, String name, List<String> callIdentifiers) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("orderId", orderId);
        parameters.addValue("callIdentifiers", callIdentifiers);
        parameters.addValue("name", name);
        String query = ASSET_METAFIELDS + ASSET_METAFIELDS_01;
        List<String> metaFieldList = namedParameterJdbcTemplate.queryForList(query, parameters,String.class);
        if (metaFieldList.isEmpty()) {
            query = ASSET_METAFIELDS + ASSET_METAFIELDS_02;
            metaFieldList = namedParameterJdbcTemplate.queryForList(query, parameters,String.class);
            if (metaFieldList.isEmpty()) {
                query = ASSET_METAFIELDS + ASSET_METAFIELDS_03;
                metaFieldList = namedParameterJdbcTemplate.queryForList(query , parameters,String.class);
            }
        }
        CollectionUtils.filter(metaFieldList, PredicateUtils.notNullPredicate());
        return metaFieldList;
    }

    public List<String> getAssetIdetifierForMonthlyOrder(Integer orderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("orderId", orderId);
        return namedParameterJdbcTemplate.queryForList(ASSET_IDENTIFIERS_01, parameters, String.class);
    }

    public List<String> getAssetIdetifierForSubscriptionOrder(Integer orderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("subOrderId", getSubscriptionOrderId(orderId));
        return namedParameterJdbcTemplate.queryForList(ASSET_IDENTIFIERS_02, parameters, String.class);
    }

    public Integer getSubscriptionOrderId(Integer orderId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("orderId", orderId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(SUBSCRIPTION_ORDER_ID_SQL, parameters);
        return row.next() ? row.getInt(1) : null;
    }

    public String getUserName(Integer userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userId", userId);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(USER_NAME_SQL, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    public String getInternationalContent(Integer foreignId,String tableName) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("foreignId", foreignId);
        parameters.addValue("tableName", tableName);
        SqlRowSet row = namedParameterJdbcTemplate.queryForRowSet(INTERNATIONAL_CONTENT, parameters);
        return row.next() ? row.getString(1) : StringUtils.EMPTY;
    }

    private String decode(String encoded) {
        String decodedValue = StringUtils.EMPTY;
        try {
            decodedValue = URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Should never happen
        }
        return decodedValue;
    }
}
