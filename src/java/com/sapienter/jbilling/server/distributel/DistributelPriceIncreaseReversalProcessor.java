package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.common.SessionInternalError;

public class DistributelPriceIncreaseReversalProcessor implements ItemProcessor<DistributelPriceUpdateRequest, DistributelPriceUpdateRequest> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;
    @Value("#{jobParameters['price_increase_data_table']}")
    private String priceIncreaseTable;
    @Value("#{jobParameters['price_reversal_data_table']}")
    private String priceReversalTable;
    @Resource
    private DistributelPriceHelperService distributelPriceService;
    @Value("#{stepExecution.stepName}")
    private String stepName;

    @Override
    public DistributelPriceUpdateRequest process(DistributelPriceUpdateRequest request) {
        logger.debug("Processing request {} for entity {}", request, entityId);
        if(DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS.equals(request.getStatus())) {
            logger.debug("price request {} already processed", request);
            return null;
        }
        if (DistributelPriceJobConstants.PRICE_UPDATE_INCREASE_STEP_NAME.equals(stepName)) {
            // add new line on exiting order.
            distributelPriceService.addNewOrderLine(request, priceIncreaseTable);
        } else if (DistributelPriceJobConstants.PRICE_UPDATE_REVERSE_STEP_NAME.equals(stepName)) {
            // remove line from order.
            distributelPriceService.deleteProduct(request, priceReversalTable);
        } else {
            logger.error("unknown step {} configured in job", stepName);
            throw new SessionInternalError("unknown  step "+ stepName + " configured in job");
        }
        return request;
    }
}
