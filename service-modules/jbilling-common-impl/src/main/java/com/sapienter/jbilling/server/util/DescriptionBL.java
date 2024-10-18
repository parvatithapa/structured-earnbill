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

package com.sapienter.jbilling.server.util;

import java.util.Collection;
import java.util.List;

import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.LanguageDTO;


public class DescriptionBL {
    private InternationalDescriptionDAS descriptionDas;
    
    public DescriptionBL() {
        init(); 
    }
    
    void init()  {
        descriptionDas = Context.getBean(Context.Name.DESCRIPTION_DAS);
    }
    
    public static final  InternationalDescriptionWS getInternationalDescriptionWS(InternationalDescriptionDTO description) {
    	
    	InternationalDescriptionWS ws = new InternationalDescriptionWS();
    	if (description.getId() != null) {
            ws.setPsudoColumn(description.getId().getPsudoColumn());
            ws.setLanguageId(description.getId().getLanguageId());
        }
        ws.setContent(description.getContent());
        return ws;
    }
    
    public void delete(String table, Integer foreignId) {
        Collection toDelete = descriptionDas.findByTable_Row(table, 
                foreignId);
                
        toDelete.clear(); // this would be cool if it worked.
    }

    public static void setDescriptionForMultiLangBeans(List<InternationalDescriptionWS> beanDescriptions,
                                                       String newDescription) {
        for (InternationalDescriptionWS description : beanDescriptions) {
            if (description != null &&
            		Constants.LANGUAGE_ENGLISH_ID.intValue() == description.getLanguageId()) {
                description.setContent(newDescription);
                return;
            }
        }
        beanDescriptions.add(
                new InternationalDescriptionWS("description", Constants.LANGUAGE_ENGLISH_ID, newDescription));
    }

    public static String getDescriptionForMultiLangBeans(List<InternationalDescriptionWS> descriptions) {
        for (InternationalDescriptionWS description : descriptions) {
            if (description != null &&
            		Constants.LANGUAGE_ENGLISH_ID.intValue() == description.getLanguageId()) {
                return description.getContent();
            }
        }
        return "";
    }
    
    public static final LanguageDTO getLanguageDTO(LanguageWS ws){
        LanguageDTO languageDTO= new LanguageDTO();
        languageDTO.setId(ws.getId());
        languageDTO.setDescription(ws.getDescription());
        languageDTO.setCode(ws.getCode());
        languageDTO.setCountryCode(ws.getCountryCode());
        return languageDTO;
    }
}
