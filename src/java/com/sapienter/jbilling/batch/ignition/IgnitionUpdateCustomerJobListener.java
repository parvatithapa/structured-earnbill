package com.sapienter.jbilling.batch.ignition;

import java.util.Date;

import javax.annotation.Resource;

import org.hibernate.ScrollableResults;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.batch.support.PartitionedJobListener;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.user.db.UserDAS;

/**
 * Created by wajeeha on 2/27/18.
 */
public class IgnitionUpdateCustomerJobListener extends PartitionedJobListener {

    @Resource
    private UserDAS userDAS;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobParameters['nextPaymentDate']}")
    private Date currentDate;

    @Override
    protected ScrollableResults findUsersForJob () {
        return userDAS.findUserIdsByNextPaymentDate(IgnitionConstants.METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER,
                currentDate, entityId);
    }
}
