package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.usagePool.UsagePoolBL;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 6/11/14.
 */
public class UsagePoolCopyTask extends AbstractCopyTask {
    UsagePoolDAS usagePoolDAS = null;
    ItemTypeDAS itemTypeDAS = null;
    ItemDAS itemDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UsagePoolCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<UsagePoolDTO> usagePoolDTOs = new UsagePoolDAS().findByEntityId(targetEntityId);
        return usagePoolDTOs != null && !usagePoolDTOs.isEmpty();
    }

    public UsagePoolCopyTask() {
        init();
    }

    private void init() {
        usagePoolDAS = new UsagePoolDAS();
        itemTypeDAS = new ItemTypeDAS();
        itemDAS = new ItemDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create UsagePoolCopyTask");
        copyUsagePool(entityId, targetEntityId);
        LOG.debug("UsagePoolCopyTask has been completed");
    }

    public void copyUsagePool(Integer entityId, Integer targetEntityId) {
        Integer languageId = new CompanyDAS().find(targetEntityId).getLanguageId();
        List<UsagePoolDTO> usagePoolDTOs = usagePoolDAS.findByEntityId(entityId);

        for (UsagePoolDTO dto : usagePoolDTOs) {
            usagePoolDAS.reattach(dto);
            UsagePoolWS ws = UsagePoolBL.getUsagePoolWS(dto);
            Integer[] itemTypesIds = new Integer[ws.getItemTypes().length];
            int index = 0;
            for (ItemTypeDTO itemTypeDTO : dto.getItemTypes()) {
            	if (!itemTypeDTO.isGlobal()) {
            		itemTypesIds[index++] = itemTypeDAS.findByDescription(entityId, itemTypeDTO.getDescription()).getId();
            	} else	{
            		itemTypesIds[index++] = itemTypeDTO.getId();
            	}
            }

            Integer[] items = new Integer[ws.getItems().length];
            index = 0;
            for (ItemDTO itemDTO : dto.getItems()) {
                items[index++] = itemDAS.findItemByInternalNumber(itemDTO.getInternalNumber(), entityId).getId();
            }
            ws.setId(0);
            ws.setEntityId(targetEntityId);
            ws.setItems(items);
            ws.setName(dto.getDescription(languageId, "name"));
            ws.setItemTypes(itemTypesIds);

            CopyCompanyUtils.oldNewUsagePoolMap.put(dto.getId(), new UsagePoolBL().createOrUpdate(ws, languageId));
        }
    }
}