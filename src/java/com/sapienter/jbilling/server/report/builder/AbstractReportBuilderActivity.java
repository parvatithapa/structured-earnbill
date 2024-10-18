package com.sapienter.jbilling.server.report.builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.report.util.EnrollmentScope;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

/**
 * AbstractReportBuilderActivity class.
 * 
 * This class obtain all rows so that its subclasses could use them.
 *
 * @author Leandro Bagur
 * @since 02/10/17.
 */
abstract public class AbstractReportBuilderActivity {

    protected static final String STAFF_NAME_COLUMN = "staff_name";
    protected static final String CREATE_DATE_COLUMN = "create_date";
    protected static final String CATEGORY_COLUMN = "category";
    protected static final String CATEGORIES_COLUMN = "categories";
    protected static final String SERVICES_COLUMN = "services";
    protected static final String CUSTOMERS_COLUMN = "customers";
    protected static final String CATEGORY_CUSTOMER_COLUMN = "cat_cust";
    protected static final String TOTAL_PRICE_COLUMN = "total_price";
    protected static final String MONTHS_COLUMN = "months";
    protected static final String PRICE_COLUMN = "price";
    protected static final String REVENUE_COLUMN = "revenue";
    protected static final String PRODUCT_ID_COLUMN = "product_id";
    protected static final String PRODUCT_NAME_COLUMN = "product_name";
    protected static final String PRODUCT_GROUP_COLUMN = "product_group";
    protected static final String TERM_COLUMN = "term";
    protected static final String SERVICE_CUSTOMER_COLUMN = "serv_cust";
        
    protected static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String PRODUCT_CLASS = "Product Class";
    private static final String REPORT_GROUP = "Report Group";

    protected List<RowActivityReport> rows;
    protected Integer entityId;

    AbstractReportBuilderActivity(Integer entityId, List<Integer> childEntities, Map<String, Object> parameters) {
        super();
        String type = (String) parameters.get("type");
        Date startDate = (Date) parameters.get("start_date");
        Date endDate = (Date) parameters.get("end_date");
        fillRows(entityId, childEntities, startDate, endDate, type);
        this.entityId = entityId;
        CompanyDTO company = new CompanyDAS().find(entityId);
        parameters.put("currency_symbol", Util.formatSymbolMoney(company.getCurrency().getSymbol(), false));
    }

    abstract public List<Map<String, ?>> getData();

    /**
     * Get all enrollment orders between two dates and set rows attribute with RowActivityReport.
     *
     * @param startDate start date
     * @param endDate   end date
     * @param type      enrollment type
     */
    private void fillRows(Integer entityId, List<Integer> childEntities, Date startDate, Date endDate, String type) {
        //The end date should include all day
        LocalDateTime endLocalDate = DateConvertUtils.asLocalDateTime(endDate);
        LocalDateTime startLocalDate = DateConvertUtils.asLocalDateTime(startDate);
        LocalDateTime actualEndLocalDate = endLocalDate.plusDays(1).minusSeconds(1);
        endLocalDate = endLocalDate.plusDays(2).minusSeconds(1);
        List<OrderDTO> orders = new OrderDAS().getEnrollmentOrdersByDate(entityId, childEntities, startDate, DateConvertUtils.asUtilDate(endLocalDate), OrderDAS.OrderDate.CREATION_DATE);

        List<OrderDTO> exactOrders = new ArrayList<>();
        for (OrderDTO orderDTO: orders) {
            LocalDateTime  convertedCreatedDate = TimezoneHelper.convertToTimezone(LocalDateTime.ofInstant(orderDTO.getCreateDate().toInstant(), ZoneId.systemDefault()),
                    TimezoneHelper.getCompanyLevelTimeZone(entityId));
            if ((convertedCreatedDate.isAfter(startLocalDate) || (convertedCreatedDate.isEqual(startLocalDate)))
                    && (convertedCreatedDate.isBefore(actualEndLocalDate) || convertedCreatedDate
                            .isEqual(actualEndLocalDate))) {
              exactOrders.add(orderDTO);
          }
        }

        rows = new ArrayList<>();

        for (OrderDTO order : exactOrders) {
            String staffName = getMetafieldValue(order.getMetaFields(), SpaConstants.MF_STAFF_IDENTIFIER);
            String enrollmentType = getMetafieldValue(order.getMetaFields(), SpaConstants.MF_ENROLLMENT_TYPE);

            if (StringUtils.isEmpty(staffName) || StringUtils.isEmpty(enrollmentType)) continue;
            
            if (EnrollmentScope.NEW_CUSTOMERS.toString().equals(type) && 
                SpaConstants.ENROLLMENT_TYPE_NEW_SERVICES.equals(enrollmentType)) 
                continue;

            OrderPeriodDTO orderPeriod = order.getOrderPeriod();
            int term = Constants.ORDER_PERIOD_ONCE.equals(orderPeriod.getId()) ? 1 :
                    (PeriodUnitDTO.YEAR == orderPeriod.getPeriodUnit().getId()) ? 12 * orderPeriod.getValue() : orderPeriod.getValue();
            Date createDate = order.getCreateDate();
            Integer userId = order.getUserId();

            List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrder(order.getId());
            orderChanges.stream().forEach(
                    orderChange -> {
                        ItemDTO item = orderChange.getItem();
                        if (item.isPlan()) {
                            if (orderChange.getStartDate().after(new Date())) {
                                PlanDTO plan = new PlanDAS().findPlanByItemId(item.getId());
                                List<PlanItemDTO> planItems = plan.getPlanItems();
                                planItems.stream().forEach(
                                        planItem -> {
                                            if (!planItem.getItem().isPlan()) {
                                                addRow(staffName, createDate, userId, planItem.getItem(), term, planItem.getItem().getPrice());
                                            }
                                        }
                                );
                            }
                        } else {
                            addRow(staffName, createDate, userId, item, term, orderChange.getPrice());
                        }
                    }
            );
        }
    }

    protected void addRow(String staff, Date createDate, Integer customerId, ItemDTO item, int term, BigDecimal price) {
        if (price != null) {
            rows.add(new RowActivityReport.RowReportActivityBuilder()
                    .staff(staff)
                    .createDate(createDate)
                    .customerId(customerId)
                    .productId(item.getId())
                    .productName(item.getDescription(1))
                    .productGroup(getItemType(item, REPORT_GROUP))
                    .term(term)
                    .service(this.getItemType(item, PRODUCT_CLASS))
                    .totalPrice(price)
                    .build());
        }
    }

    /**
     * Get a specific metafield value.
     *
     * @param metafields    metafields list
     * @param metafieldName metafield to search
     * @return String
     */
    protected String getMetafieldValue(List<MetaFieldValue> metafields, String metafieldName) {
        return metafields.stream()
                .filter(metafieldValue -> metafieldName.equals(metafieldValue.getField().getName()))
                .map(MetaFieldValue::getValue)
                .map(Object::toString)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }

    /**
     * Get a specific item type
     *
     * @param item           item
     * @param itemTypeToFind item type to search
     * @return String
     */
    protected String getItemType(ItemDTO item, String itemTypeToFind) {
        String itemType = item.getItemTypes().stream()
                .filter(type -> type.getDescription().startsWith(itemTypeToFind))
                .map(ItemTypeDTO::getDescription)
                .findFirst().orElse("");
        if (!StringUtils.isEmpty(itemType)) {
            itemType = itemType.substring(itemType.indexOf("-") + 1, itemType.length()).trim();
        }
        return itemType;
    }
}
