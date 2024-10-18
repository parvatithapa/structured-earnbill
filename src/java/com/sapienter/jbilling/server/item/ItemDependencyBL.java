/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.*;
import org.apache.log4j.Logger;

import java.util.*;

public class ItemDependencyBL {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ItemDependencyBL.class));

    private ItemDependencyDAS das = null;
    private ItemDependencyDTO entity = null;

    public ItemDependencyBL(Integer itemTypeId)  {
        init();
        set(itemTypeId);
    }

    public ItemDependencyBL() {
        init();
    }
    
    private void init() {
        das = new ItemDependencyDAS();
    }

    public ItemDependencyDTO getEntity() {
        return entity;
    }
    
    public void set(Integer id) {
        entity = das.find(id);
    }

    /**
     * Convert ItemDependencyDTOEx[] to Set of ItemDependencyDTO
     * @param dependencies
     * @return
     */
    public static Set<ItemDependencyDTO> toDto(ItemDependencyDTOEx[] dependencies, ItemDTO item) {
        if(dependencies == null) {
            return new HashSet<ItemDependencyDTO>(0);
        }
        Set<ItemDependencyDTO> dependencyDTOs = new HashSet<ItemDependencyDTO>(dependencies.length * 2);
        for(ItemDependencyDTOEx dtoEx : dependencies) {
            ItemDependencyDTO dto = toDto(dtoEx);
            dto.setItem(item);
            dependencyDTOs.add(dto);
        }
        return dependencyDTOs;
    }

    /**
     * Convert a single ItemDependencyDTOEx to ItemDependencyDTO
     * @param dtoEx
     * @return
     */
    public static ItemDependencyDTO toDto(ItemDependencyDTOEx dtoEx) {
        ItemDependencyDTO dto = create(dtoEx.getType());
        if(dtoEx.getId() != null) dto.setId(dtoEx.getId());
        if(dtoEx.getItemId() != null) dto.setItem(new ItemDTO(dtoEx.getItemId()));
        dto.setMinimum(dtoEx.getMinimum());
        dto.setMaximum(dtoEx.getMaximum());
        dto.setDependentObject(findDependent(dtoEx.getType(), dtoEx.getDependentId()));

        return dto;
    }

    /**
     * Create a new subclass of ItemDependencyDTO for the type.
     *
     * @param type
     * @return
     */
    private static ItemDependencyDTO create(ItemDependencyType type) {
        switch (type) {
            case ITEM: return new ItemDependencyOnItemDTO();
            case ITEM_TYPE: return new ItemDependencyOnItemTypeDTO();
            default: throw new SessionInternalError("ItemDependencyDTOEx.Type is "+type);
        }
    }

    /**
     * Load the instance of the dependent object specified by the id for the given type.
     *
     * @param type
     * @param id
     * @return
     */
    private static Object findDependent(ItemDependencyType type, int id) {
        switch (type) {
            case ITEM: return new ItemBL(id).getEntity();
            case ITEM_TYPE: return new ItemTypeBL(id).getEntity();
            default: throw new SessionInternalError("ItemDependencyDTOEx.Type is "+type);
        }
    }

    /**
     * Convert a set of ItemDependencyDTO to ItemDependencyDTOEx[]
     *
     * @param dependencies
     * @return
     */
    public static ItemDependencyDTOEx[] toWs(Set<ItemDependencyDTO> dependencies) {
        ItemDependencyDTOEx[] ws = new ItemDependencyDTOEx[dependencies.size()];
        int idx=0;

        for(ItemDependencyDTO dependencyDTO : dependencies) {
            ws[idx++] = toWs(dependencyDTO);
        }
        return ws;
    }

    /**
     * Convert a ItemDependencyDTO to ItemDependencyDTOEx
     * @param dep
     * @return
     */
    public static ItemDependencyDTOEx toWs(ItemDependencyDTO dep) {
        ItemDependencyDTOEx ws = new ItemDependencyDTOEx();
        ws.setDependentId(dep.getDependentObjectId());
        ws.setId(dep.getId());
        ws.setItemId(dep.getItem().getId());
        ws.setDependentDescription(dep.getDependentDescription());
        ws.setMaximum(dep.getMaximum());
        ws.setMinimum(dep.getMinimum());
        ws.setType(dep.getType());

        return ws;
    }
}
