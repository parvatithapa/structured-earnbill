package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeTypeBL;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vivek on 18/11/14.
 */
public class OrderChangeTypeCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderChangeTypeCopyTask.class));

    OrderChangeTypeDAS orderChangeTypeDAS = null;
    ItemTypeDAS itemTypeDAS = null;
    IWebServicesSessionBean webServicesSessionBean;
    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<OrderChangeTypeDTO> orderChangeTypeDTOs = orderChangeTypeDAS.findAllOrderChangeTypesByEntity(targetEntityId);
        return orderChangeTypeDTOs != null && !orderChangeTypeDTOs.isEmpty();
    }

    public OrderChangeTypeCopyTask() {
        init();
    }

    private void init() {
        orderChangeTypeDAS = new OrderChangeTypeDAS();
        itemTypeDAS = new ItemTypeDAS();
        webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create OrderChangeTypeCopyTask");
        List<OrderChangeTypeDTO> orderChangeTypeDTOs = orderChangeTypeDAS.findAllOrderChangeTypesByEntity(entityId);

        for (OrderChangeTypeDTO orderChangeTypeDTO : orderChangeTypeDTOs) {
            OrderChangeTypeWS orderChangeTypeWS = OrderChangeTypeBL.getWS(orderChangeTypeDTO);
            orderChangeTypeWS.setId(null);
            orderChangeTypeWS.setEntityId(targetEntityId);
            List<Integer> itemTypeIds = new ArrayList<Integer>();
            if (orderChangeTypeWS.getItemTypes() != null) {
                for (Integer itemTypeId : orderChangeTypeWS.getItemTypes()) {
                    ItemTypeDTO itemTypeDTO = itemTypeDAS.find(itemTypeId);
                    ItemTypeDTO copyItemTypeDTO = itemTypeDAS.findByDescription(targetEntityId, itemTypeDTO.getDescription());
                    if (copyItemTypeDTO != null) {
                        itemTypeIds.add(copyItemTypeDTO.getId());
                    }
                }
            }
            orderChangeTypeWS.setItemTypes(itemTypeIds);

            if (orderChangeTypeWS.getOrderChangeTypeMetaFields() != null) {
                for (MetaFieldWS metaFieldWS : orderChangeTypeWS.getOrderChangeTypeMetaFields()) {
                    metaFieldWS.setId(0);
                    metaFieldWS.setEntityId(targetEntityId);
                }
            }
            createUpdateOrderChangeType(orderChangeTypeWS, targetEntityId);
        }
        LOG.debug("OrderChangeTypeCopyTask has been completed.");
    }

    public Integer createUpdateOrderChangeType(OrderChangeTypeWS orderChangeTypeWS, Integer targetEntityId) {
        OrderChangeTypeDTO dto = OrderChangeTypeBL.getDTO(orderChangeTypeWS, targetEntityId);
        OrderChangeTypeBL changeTypeBL = new OrderChangeTypeBL();
        OrderChangeTypeWS existedTypeWithSameName = getOrderChangeTypeByName(dto.getName(), targetEntityId);
        //name should be unique within entity
        if (existedTypeWithSameName != null && (dto.getId() == null || !dto.getId().equals(existedTypeWithSameName.getId()))) {
            throw new SessionInternalError("Order Change Type validation failed: name is not unique", new String[]{"OrderChangeTypeWS,name,OrderChangeTypeWS.validation.error.name.not.unique," + dto.getName()});
        }

        return changeTypeBL.createUpdateOrderChangeType(dto, targetEntityId);
    }

    /**
     * Find OrderChangeType by name for current entity
     *
     * @param name name for search
     * @return OrderChangeType found or null
     */
    public OrderChangeTypeWS getOrderChangeTypeByName(String name, Integer targetEntityId) {
        OrderChangeTypeDTO dto = new OrderChangeTypeDAS().findOrderChangeTypeByName(name, targetEntityId);
        return dto != null ? OrderChangeTypeBL.getWS(dto) : null;
    }
}