package com.sapienter.jbilling.server.integration.common.job.model;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.appdirect.vo.AccountInfo;
import com.sapienter.jbilling.appdirect.vo.UsageBean;
import com.sapienter.jbilling.appdirect.vo.UsageItemBean;

/**
 * Created by tarun.rathor on 1/19/18.
 */
public class Converter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Converter() {}
    public static Optional<UsageBean> convert(List<? extends MeteredUsageStepResult> stepResults) {
        if(stepResults == null || stepResults.isEmpty()) {
            return Optional.empty();
        }

        MeteredUsageStepResult stepResultFirst =  stepResults.stream().findFirst().get();

        try {
            UsageBean usageBean = new UsageBean();

            AccountInfo accountInfo = AccountInfo.builder().accountIdentifier(stepResultFirst.getAccountIdenfitier()).build();
            usageBean.setAccount(accountInfo);
            usageBean.setAddonInstance(null);

            List<UsageItemBean> usageItemBeanList = new ArrayList<>();
            for(MeteredUsageStepResult stepResult: stepResults) {

                for(MeteredUsageItem meteredUsageItem: stepResult.getItems()) {
                    UsageItemBean usageItemBean = UsageItemBean.builder()
                        .quantity(meteredUsageItem.getQuantity())
                        .price(meteredUsageItem.getPrice())
                        .description(meteredUsageItem.getFormattedDescription())
                        .customUnit(meteredUsageItem.getCustomUnit())
                        .build();

                    usageItemBeanList.add(usageItemBean);
                }
            }
            usageBean.setItems(usageItemBeanList);
            return Optional.of(usageBean);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return Optional.empty();
    }
}
