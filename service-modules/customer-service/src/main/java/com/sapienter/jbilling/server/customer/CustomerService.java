package com.sapienter.jbilling.server.customer;


import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import java.util.List;

/**
 * Created by marcolin on 09/10/15.
 */
public interface CustomerService {

    public Integer createUser(UserWS user) throws SessionInternalError;
    public User resolveUserByUsername(Integer entityId, String username);
    public UserWS findUserById(Integer userId);
    public OrderWS findUserOrderByItemTypeDescription(Integer entityId, Integer userId, String description);
    public OrderWS findUserOrderContainsAssetIdentifier(Integer userId, String assetIdentifier);
    public List<UserWS> resolveUsersWithMetaFieldPresent(Integer entityId, String metafieldName);
}
