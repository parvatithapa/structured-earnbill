package com.sapienter.jbilling.server.report.builder;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.report.util.EnrollmentScope;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ReportBuilderCustomerProductActivity class.
 *
 * @author Pablo Galera
 * @since 07/10/18.
 */
public class ReportBuilderCustomerProductActivity extends ReportBuilderCustomerPackages {

    public ReportBuilderCustomerProductActivity(Integer entityId, List<Integer> children, Map<String, Object> parameters) {
        super(entityId, children, parameters);
    }
    
    public List<OrderDTO> getOrders(LocalDateTime endLocalDate) {
        return new OrderDAS().getEnrollmentOrdersActiveBetweenDates(getEntityId(), getChildren(), getStartDate(), DateConvertUtils.asUtilDate(endLocalDate), OrderDAS.OrderDate.ACTIVE_SINCE);
    }

    protected Map<String, Object> getRowData(OrderChangeDTO orderChange, OrderDTO order, UserDTO user, String staffName, ItemDTO item) {
        Map<String, Object> row = super.getRowData(orderChange, order, user, staffName, item);
        row.put("price", orderChange.getPrice());
        return row;
    }
}
