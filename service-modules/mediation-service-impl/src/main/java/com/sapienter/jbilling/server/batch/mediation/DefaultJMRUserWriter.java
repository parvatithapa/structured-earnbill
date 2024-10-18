package com.sapienter.jbilling.server.batch.mediation;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

public class DefaultJMRUserWriter implements ItemWriter<Integer> {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Override
    public void write(List<? extends Integer> userIds) throws Exception {
        userIds.stream()
               .forEach(userId -> logger.debug("User {} jmr processed for entity {}", userId, entityId));
    }

}
