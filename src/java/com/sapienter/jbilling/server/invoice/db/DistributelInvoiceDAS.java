package com.sapienter.jbilling.server.invoice.db;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.report.builder.AverageRevenueData;
import com.sapienter.jbilling.server.report.builder.RevenueInvoice;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 * Created by pablo on 26/06/18.
 */
public class DistributelInvoiceDAS extends AbstractDAS<InvoiceDTO> {
	private static final Logger logger = LoggerFactory.getLogger(DistributelInvoiceDAS.class);	

    /**
     * Query to get all revenue invoices. First Select is used to retrieve the migrated invoices which have Order Type and Month Term metafields setted.
     * The second Select retrieve the existed invoices and find the data from the related order.
     * The required data are: billing type (Prepaid or postpaid) and the month term (Monthly or Yearly)
     */
    private static final String findRevenueInvoices =
            new StringBuilder(
                    // Migrated invoices
                    "(SELECT i.id AS invoiceId, i.public_number AS invoiceNumber, bu.id AS userId, i.create_datetime AS createdDate, (SELECT c.code FROM currency c WHERE c.id=i.currency_id) AS currency, il.id AS invoiceLineId, il.item_id AS itemId, il.amount, :prePaid AS orderType, monthTerm.integer_value AS monthTerm, ")
                    .append("(SELECT mfv.string_value FROM meta_field_value mfv INNER JOIN invoice_meta_field_map imfm ON mfv.id = imfm.meta_field_value_id INNER JOIN meta_field_name mfn ON mfn.id=mfv.meta_field_name_id WHERE imfm.invoice_id=i.id AND mfn.name='Tax Province' AND mfn.entity_type='INVOICE') AS province, ")
                    .append("(SELECT mfv.boolean_value FROM meta_field_value mfv INNER JOIN item_meta_field_map pmfm ON mfv.id = pmfm.meta_field_value_id INNER JOIN meta_field_name mfn ON mfn.id=mfv.meta_field_name_id WHERE pmfm.item_id=il.item_id AND mfn.name='Tax Exempt' AND mfn.entity_type='PRODUCT') AS taxExempt")
                    .append(" FROM invoice i")
                    .append(" INNER JOIN base_user bu ON i.user_id = bu.id")
                    .append(" INNER JOIN entity e ON bu.entity_id = e.id")
                    .append(" INNER JOIN invoice_line il ON i.id = il.invoice_id")
                    .append(" INNER JOIN (SELECT mfv.id, mfv.integer_value, imfm.invoice_id")                               // Select to search the Month Term invoice metafield
                    .append(" FROM meta_field_value mfv")
                    .append(" INNER JOIN invoice_meta_field_map imfm ON mfv.id = imfm.meta_field_value_id")
                    .append(" INNER JOIN meta_field_name mfn ON mfn.id=mfv.meta_field_name_id")
                    .append(" WHERE mfn.name=:monthTermMF) monthTerm ON monthTerm.invoice_id=i.id")
                    .append(" WHERE i.deleted=0")
                    .append(" AND e.id=:entityId")
                    .append(" AND il.type_id!=:taxType")
                    .append(" AND il.item_id IS NOT NULL")
                    .append(" AND il.amount != 0")
                    .append(" AND il.order_id IS NULL")
                    .append(" AND ((i.create_datetime, i.create_datetime + INTERVAL '1 MONTH' * (monthTerm.integer_value - 1)) OVERLAPS (:startDate, :endDate)") // Each Month Term
                    .append("      OR (i.create_datetime + INTERVAL '1 MONTH' * (monthTerm.integer_value - 1)) = :startDate")
                    .append("      OR i.create_datetime = :endDate))")
                    .append(" UNION")
                            // Non-migrated invoices
                    .append(" (SELECT i.id AS invoiceId, i.public_number AS invoiceNumber, bu.id AS userId, i.create_datetime AS createdDate, (SELECT c.code FROM currency c WHERE c.id=i.currency_id) AS currency, il.id as invoiceLineId, il.item_id AS itemId, il.amount, po.billing_type_id AS orderType, CASE WHEN op.unit_id=:yearlyPeriod THEN :yearlyTerm ELSE :monthTerm END * CASE WHEN op.value IS NOT NULL THEN op.value ELSE 1 END AS monthTerm, ")
                    .append("(SELECT mfv.string_value FROM meta_field_value mfv INNER JOIN invoice_meta_field_map imfm ON mfv.id = imfm.meta_field_value_id INNER JOIN meta_field_name mfn ON mfn.id=mfv.meta_field_name_id WHERE imfm.invoice_id=i.id AND mfn.name='Tax Province' AND mfn.entity_type='INVOICE') AS province, ")
                    .append("(SELECT mfv.boolean_value FROM meta_field_value mfv INNER JOIN item_meta_field_map pmfm ON mfv.id = pmfm.meta_field_value_id INNER JOIN meta_field_name mfn ON mfn.id=mfv.meta_field_name_id WHERE pmfm.item_id=il.item_id AND mfn.name='Tax Exempt' AND mfn.entity_type='PRODUCT') AS taxExempt")
                    .append(" FROM invoice i")
                    .append(" INNER JOIN base_user bu ON i.user_id = bu.id")
                    .append(" INNER JOIN entity e ON bu.entity_id = e.id")
                    .append(" INNER JOIN invoice_line il ON i.id = il.invoice_id")
                    .append(" INNER JOIN purchase_order po ON po.id=il.order_id")
                    .append(" INNER JOIN order_period op ON op.id=po.period_id")
                    .append(" WHERE i.deleted=0")
                    .append(" AND e.id=:entityId")
                    .append(" AND il.type_id!=:taxType")
                    .append(" AND il.amount != 0")
                    .append(" AND il.item_id IS NOT NULL")
                    .append(" AND ((po.billing_type_id=:postPaid AND i.create_datetime BETWEEN :startDate AND :endDate)")              //Post paid
                    .append(" OR (po.billing_type_id=:prePaid AND (")                                                                    //Pre paid
                    .append("  (i.create_datetime, i.create_datetime + INTERVAL '1 MONTH' * ((CASE WHEN op.unit_id=:yearlyPeriod THEN :yearlyTerm ELSE :monthTerm END * op.value) - 1)) OVERLAPS (:startDate, :endDate)") // All periods
                    .append("  OR (i.create_datetime + INTERVAL '1 MONTH' * ((CASE WHEN op.unit_id=:yearlyPeriod THEN :yearlyTerm ELSE :monthTerm END * op.value) - 1)) = :startDate")
                    .append("  OR i.create_datetime = :endDate")
                    .append("))))")
                    .toString();
    
    /**
     * Query invoice, invoice_line, invoice_line_type, base_user, and customer to gather data which are required to build up average-revenue report
     * naderm: there's no need to use StringBuilder since there's one assignment, and the entire query is one string
     */
    private static final String AVERAGE_REVENUE_INVOICE_QUERY =
            "SELECT u.id AS userId, "
                 + "CONCAT((SELECT mfv.string_value "
                 + "        FROM   customer_account_info_type_timeline caitl JOIN meta_field_value mfv ON mfv.id=caitl.meta_field_value_id "
                 + "             JOIN meta_field_name mfn on mfn.id = mfv.meta_field_name_id "
                 + "             WHERE c.id = caitl.customer_id and mfn.field_usage='FIRST_NAME'), ' ', "
                 + "        (SELECT mfv.string_value "
                 + "         FROM   customer_account_info_type_timeline caitl JOIN meta_field_value mfv ON mfv.id=caitl.meta_field_value_id "
                 + "               JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id "
                 + "          WHERE c.id = caitl.customer_id AND mfn.field_usage='LAST_NAME')) AS customerName, "
                 + "c.account_type_id AS customerAccountId, "
                 + "(SELECT idesc.content "
                 + " FROM international_description idesc "
                 + " WHERE idesc.foreign_id=c.account_type_id "
                 + "     AND idesc.language_id=1 "
                 + "     AND idesc.table_id = 111) AS customerAccountName, "
                 + "(SELECT idesc.content "
                 + " FROM international_description idesc "
                 + " WHERE idesc.foreign_id=u.status_id "
                 + "    AND idesc.language_id=1 "
                 + "    AND idesc.table_id=9) AS customerAccountStatus, "
                 + "i.id AS invoiceId, "
                 + "i.create_datetime AS invoiceDate, "
                 + "i.total AS invoiceAmount, "
                 + "il.id AS invoiceLineId, "
                 + "il.amount AS invoiceLineAmount, "
                 + "il.type_id AS invoiceLineTypeId, "
                 + "ilt.description AS invoiceLineType "
          + "FROM base_user u JOIN customer c ON u.id=c.user_id "
                + "JOIN invoice i ON i.user_id=u.id "
                + "JOIN invoice_line il ON il.invoice_id = i.id "
                + "JOIN invoice_line_type ilt ON ilt.id = il.type_id "
          + "WHERE u.entity_id= :entityId AND  i.create_datetime between :startDate AND :endDate and i.total>0 AND il.amount>0";

    /**
     * Function to get all revenue invoices. The addScalar function is needed to keep the fields with uppercase.
     * But the aliasToBean transforms all fields to lowercase
     * @param month month
     * @param year year
     * @param entityId entity id
     * @return List
     */
    public List<RevenueInvoice> getRevenueInvoices(int month, int year, Integer entityId) {
        LocalDate startLocalDate = LocalDate.of(year, month, 1);
        LocalDate endLocalDate = startLocalDate.withDayOfMonth(startLocalDate.lengthOfMonth());
        Date startDate = DateConvertUtils.asUtilDate(startLocalDate);
        Date endDate = DateConvertUtils.asUtilDate(endLocalDate);

        @SuppressWarnings("unchecked")
        List<RevenueInvoice> invoices = getSession().createSQLQuery(findRevenueInvoices)
                .addScalar("invoiceId", StandardBasicTypes.INTEGER)
                .addScalar("invoiceNumber", StandardBasicTypes.STRING)
                .addScalar("userId", StandardBasicTypes.INTEGER)
                .addScalar("createdDate", StandardBasicTypes.DATE)
                .addScalar("invoiceLineId", StandardBasicTypes.INTEGER)
                .addScalar("itemId", StandardBasicTypes.INTEGER)
                .addScalar("orderType", StandardBasicTypes.INTEGER)
                .addScalar("monthTerm", StandardBasicTypes.INTEGER)
                .addScalar("currency", StandardBasicTypes.STRING)
                .addScalar("amount", StandardBasicTypes.BIG_DECIMAL)
                .addScalar("province", StandardBasicTypes.STRING)
                .addScalar("taxExempt", StandardBasicTypes.BOOLEAN)
                .setParameter("entityId", entityId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("prePaid", Constants.ORDER_BILLING_PRE_PAID)
                .setParameter("postPaid", Constants.ORDER_BILLING_POST_PAID)
                .setParameter("yearlyPeriod", PeriodUnitDTO.YEAR)
                .setParameter("monthTerm", SpaConstants.MONTHLY)
                .setParameter("yearlyTerm", SpaConstants.YEARLY)
                .setParameter("taxType", Constants.INVOICE_LINE_TYPE_TAX)
                .setParameter("monthTermMF", SpaConstants.MONTH_TERM)
                .setResultTransformer(Transformers.aliasToBean(RevenueInvoice.class))
                .list();

        return invoices;
    }
    //---------------------------------------------------------------------------------------------
    
    /**
     * Query data which are required to build up average revenue report
     * @param month month
     * @param year year
     * @param entityId entity id
     * @return list of objects that represent data to build-up/calculate average revenue report
     */
    public List<AverageRevenueData> getAverageRevenueData(int month, int year, Integer entityId) {
    	logger.debug("getAverageRevenueData is called with month: {}, year: {}, and entity-ID: {}", month, year, entityId);
    	
        LocalDate startLocalDate = LocalDate.of(year, month, 1);
        LocalDate endLocalDate = startLocalDate.withDayOfMonth(startLocalDate.lengthOfMonth());
        Date startDate = DateConvertUtils.asUtilDate(startLocalDate);
        Date endDate = DateConvertUtils.asUtilDate(endLocalDate);
        String sqlQueryStr = AVERAGE_REVENUE_INVOICE_QUERY;
        logger.debug("SQL query to rerieve data for average-revenue report. \n :{}", sqlQueryStr);
        
        @SuppressWarnings("unchecked")
        List<AverageRevenueData> retv = getSession().createSQLQuery(sqlQueryStr)
                .addScalar("userId", StandardBasicTypes.INTEGER)
                .addScalar("customerName", StandardBasicTypes.STRING)
                .addScalar("userId", StandardBasicTypes.INTEGER) 
                .addScalar("customerAccountId", StandardBasicTypes.INTEGER)
                .addScalar("customerAccountName", StandardBasicTypes.STRING)
                .addScalar("customerAccountStatus", StandardBasicTypes.STRING)
                .addScalar("invoiceId", StandardBasicTypes.INTEGER)
                .addScalar("invoiceDate", StandardBasicTypes.DATE)
                .addScalar("invoiceAmount", StandardBasicTypes.BIG_DECIMAL)
                .addScalar("invoiceLineId", StandardBasicTypes.INTEGER)
                .addScalar("invoiceLineAmount", StandardBasicTypes.BIG_DECIMAL)
                .addScalar("invoiceLineTypeId", StandardBasicTypes.INTEGER)
                .addScalar("invoiceLineType", StandardBasicTypes.STRING)
                .setParameter("entityId", entityId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setResultTransformer(Transformers.aliasToBean(AverageRevenueData.class)).list();
        
        return retv;
    }
    //---------------------------------------------------------------------------------------------
}
