package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

public class DistributelPriceWriter implements ItemWriter<DistributelPriceUpdateRequest> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Override
    public void write(List<? extends DistributelPriceUpdateRequest> requests) throws Exception {
        logger.debug("Writing Request {} for entity {}", requests, entityId);
    }

}
