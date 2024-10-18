package com.sapienter.jbilling.batch.ageing;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.process.IBillingProcessSessionBean;

/**
 * 
 * @author Khobab
 *
 */
public class AgeingProcessUserStatusProcessor implements ItemProcessor<Integer, AgeingStatusResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IBillingProcessSessionBean local;

    @Value("#{jobParameters['ageingDate']}")
    private Date ageingDate;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    /**
     * Gets users id from reader and then done status reviewing
     */
    @Override
    public AgeingStatusResult process (Integer userId) throws Exception {
        logger.debug("Review Status of the user # {}", userId);
        return new AgeingStatusResult(userId, local.reviewUserStatus(entityId, userId, ageingDate));
    }
}
