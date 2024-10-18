package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDAS;
import com.sapienter.jbilling.server.util.db.CurrencyExchangeDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 19/11/14.
 */
public class CurrencyExchangeCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CurrencyExchangeCopyTask.class));

    CurrencyExchangeDAS currencyExchangeDAS = null;
    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        init();
        List<CurrencyExchangeDTO> currencyExchangeDTOs = currencyExchangeDAS.findByEntity(targetEntityId);
        return currencyExchangeDTOs != null && !currencyExchangeDTOs.isEmpty();
    }

    public void init() {
        currencyExchangeDAS = new CurrencyExchangeDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        init();
        LOG.debug("Create CurrencyExchangeCopyTask");
        List<CurrencyExchangeDTO> currencyExchangeDTOs = currencyExchangeDAS.findByEntity(entityId);
        for (CurrencyExchangeDTO currencyExchangeDTO : currencyExchangeDTOs) {
            CurrencyBL currencyBl = new CurrencyBL(currencyExchangeDTO.getCurrency().getId());
            CurrencyExchangeDTO copyCurrencyExchangeDTO = currencyBl.setOrUpdateExchangeRate(currencyExchangeDTO.getRate(), targetEntityId, TimezoneHelper.companyCurrentDate(entityId));
            CopyCompanyUtils.oldNewCurrencyExchangeMap.put(currencyExchangeDTO.getId(), copyCurrencyExchangeDTO.getId());
        }
        LOG.debug("Create CurrencyExchangeCopyTask has been completed");
    }
}