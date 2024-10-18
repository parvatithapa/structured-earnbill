package com.sapienter.jbilling.server.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation.DistributelMediationConstant;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * Created by marcolin on 09/10/15.
 */
public class CustomerJdbcTemplateService implements CustomerService {

    private JdbcTemplate jdbcTemplate = null;
    private IWebServicesSessionBean webServicesSessionBean;

    private static final String RESOLVE_USER_BY_USERNAME_QUERY = 
            "SELECT b.id, b.currency_id, b.deleted FROM base_user b, entity e " +
            "WHERE b.entity_id = e.id AND b.user_name = ? " +
            "AND ( e.id = ? OR e.parent_id = ? )";

    private static final String USER_WITH_METAFIELD = 
            " SELECT u.id, u.currency_id , mv.string_value AS mfv_value"  +
            " FROM base_user u" +
            " JOIN customer c ON c.user_id=u.id" +
            " JOIN customer_meta_field_map cmm ON cmm.customer_id=c.id" +
            " JOIN meta_field_value mv ON mv.id=cmm.meta_field_value_id" +
            " JOIN meta_field_name n ON n.id=mv.meta_field_name_id" +
            " WHERE n.name='externalAccountIdentifier' AND mv.string_value IS NOT NULL";
    
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setWebServicesSessionBean(IWebServicesSessionBean webServicesSessionBean) {
        this.webServicesSessionBean = webServicesSessionBean;
    }

    @Override
    public Integer createUser(UserWS user) throws SessionInternalError {
        return webServicesSessionBean.createUser(user);
    }

    @Override
    public User resolveUserByUsername(Integer entityId, String username) {
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(RESOLVE_USER_BY_USERNAME_QUERY, username, entityId, entityId);
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setCurrencyId(rs.getInt("currency_id"));
            if (rs.getInt("deleted") > 0) {
                user.setDeleted(rs.getBoolean("deleted"));
            }
            return user;
        }
        return null;
    }

    @Override
    public UserWS findUserById(Integer userId) {
        UserBL bl = new UserBL(userId);
        return bl.getUserWS();
    }


    @Override
    public OrderWS findUserOrderByItemTypeDescription(Integer entityId, Integer userId, String description) {
        List<OrderDTO> orders = new OrderDAS().getAllActiveOrdersByUserId(userId);
        ItemTypeDTO itemType = new ItemTypeDAS().findByDescription(entityId, DistributelMediationConstant.MCF_RATED_ITEM_TYPE);
        for (OrderDTO order : orders) {
            if (hasOrderItemType(order, itemType)) {
                return new OrderBL(order).getWS(UserBL.getUserEntity(userId).getLanguage().getId());
            }
        }
        return null;
    }

    private boolean hasOrderItemType(OrderDTO order, ItemTypeDTO itemType) {
        List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrder(order.getId());
        boolean hasItemType = orderChanges.stream().anyMatch(orderChange ->
                orderChange.getOrderChangePlanItems().stream().anyMatch(planItem -> planItem.getItem().getItemTypes().contains(itemType)));
        if (!hasItemType) {
            hasItemType = orderChanges.stream().anyMatch(orderChange -> orderChange.getItem().getItemTypes().contains(itemType));
        }
        return hasItemType;

    }

    @Override
    public OrderWS findUserOrderContainsAssetIdentifier(Integer userId, String assetIdentifier) {
        List<OrderDTO> orders = new OrderDAS().findAllUserByUserId(userId);
        Optional<OrderDTO> optionalOrder = orders.stream().filter(order -> containOrderAssetIdentifier(order, assetIdentifier)).findAny();
        if (optionalOrder.isPresent()) {
            return new OrderBL(optionalOrder.get()).getWS(UserBL.getUserEntity(userId).getLanguage().getId());
        }
        return null;
    }

    private boolean containOrderAssetIdentifier(OrderDTO order, String assetIdentifier) {
        List<OrderChangeDTO> orderChanges = new OrderChangeDAS().findByOrder(order.getId());
        boolean containAssetIdentifier = orderChanges.stream().anyMatch(
                orderChange -> orderChange.getOrderChangePlanItems().stream().anyMatch(
                        plantItem -> plantItem.getAssets().stream().anyMatch(
                                asset -> asset.getMetaFields().stream().anyMatch(
                                        mfValue -> mfValue.getValue() != null && mfValue.getValue().toString().contains(assetIdentifier)
                                )
                        )
                )
        );
        if (!containAssetIdentifier) {
            containAssetIdentifier = orderChanges.stream().anyMatch(
                    orderChange -> orderChange.getAssets().stream().anyMatch(
                            asset -> asset.getMetaFields().stream().anyMatch(
                                    mfValue -> mfValue.getValue() != null && mfValue.getValue().toString().contains(assetIdentifier)
                            )
                    )
            );
        }
        return containAssetIdentifier;
    }
    @Override
    public List<UserWS> resolveUsersWithMetaFieldPresent(Integer entityId, String metafieldName) {
        SqlRowSet rs = jdbcTemplate.queryForRowSet(USER_WITH_METAFIELD);
        List<UserWS> users = new ArrayList<>();
        while (rs.next()) {
            users.add(findUserById(rs.getInt("id")));
        }
        return users;
    }

}
