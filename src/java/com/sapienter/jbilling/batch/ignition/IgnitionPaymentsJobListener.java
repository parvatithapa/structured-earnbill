package com.sapienter.jbilling.batch.ignition;

import java.util.Date;

import javax.annotation.Resource;

import org.hibernate.ScrollableResults;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.batch.support.PartitionedJobListener;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;

/**
 * Created by wajeeha on 2/27/18.
 */
public class IgnitionPaymentsJobListener extends PartitionedJobListener {

    @Resource
    private MetaFieldDAS metaFieldDAS;
    @Resource
    private UserDAS userDAS;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobParameters['actionDate']}")
    private Date currentDate;

    private static final EntityType[] types = new EntityType[] { EntityType.CUSTOMER };

    @Override
    protected ScrollableResults findUsersForJob () {
        return userDAS.findUserIdsByMetaFieldNameAndValue(
                metaFieldDAS.getFieldByName(entityId, types, IgnitionConstants.USER_ACTION_DATE).getId(), currentDate,
                entityId);
    }
}
