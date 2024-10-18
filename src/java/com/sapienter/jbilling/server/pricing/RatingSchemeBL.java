/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.*;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.mediation.db.MediationConfigurationDAS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Andres Canevaro on 30/07/15.
 */
public class RatingSchemeBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RatingSchemeBL.class));

    private MediationRatingSchemeDTO mediationRatingSchemeDTO = null;
    private static MediationRatingSchemeDAS mediationRatingSchemeDAS = new MediationRatingSchemeDAS();
    private static Integer MINUTE_IN_SECONDS = 60;


    public RatingSchemeBL() {
        init();
    }

    public RatingSchemeBL(Integer mediationRatingSchemeId) {
        init();
        setMediationRatingShceme(mediationRatingSchemeId);
    }

    private void setMediationRatingShceme(Integer mediationRatingSchemeId) {
        mediationRatingSchemeDTO = mediationRatingSchemeDAS.findNow(mediationRatingSchemeId);
    }

    private void init() {
        mediationRatingSchemeDTO = new MediationRatingSchemeDTO();
    }

    public MediationRatingSchemeWS getWS() {
        return getWS(mediationRatingSchemeDTO);
    }

    public static MediationRatingSchemeWS getWS(MediationRatingSchemeDTO dto) {
        MediationRatingSchemeWS ws = new MediationRatingSchemeWS();
        if(dto.getId() != null) {
            ws.setId(dto.getId());
        }
        ws.setEntity(dto.getEntity().getId());
        ws.setId(dto.getId());
        ws.setInitialIncrement(dto.getInitialIncrement());
        ws.setInitialRoundingMode(dto.getInitialRoundingMode());
        ws.setMainIncrement(dto.getMainIncrement());
        ws.setMainRoundingMode(dto.getMainRoundingMode());
        ws.setName(dto.getName());
        ws.setGlobal(dto.getGlobal());

        List<RatingSchemeAssociationWS> associations = new ArrayList<>();
        for(RatingSchemeAssociation ratingSchemeAssociation: dto.getAssociations()) {
            RatingSchemeAssociationWS association = new RatingSchemeAssociationWS();
            MediationConfiguration mediationConfiguration = new MediationConfigurationDAS().find(ratingSchemeAssociation.getMediation());
			MediationConfigurationWS mediationCfg = MediationConfigurationBL.getWS(mediationConfiguration);
            association.setId(ratingSchemeAssociation.getId());
            association.setRatingScheme(ratingSchemeAssociation.getRatingIncrement());
            association.setCompany(EntityBL.getCompanyWS(new CompanyDAS().find(ratingSchemeAssociation.getEntity())));
            association.setMediation(mediationCfg);
            associations.add(association);
        }
        ws.setAssociations(associations);

        return ws;
    }

    public static MediationRatingSchemeDTO getDTO(MediationRatingSchemeWS ws) {
        MediationRatingSchemeDTO dto = new MediationRatingSchemeDTO();
        CompanyDAS companyDAS = new CompanyDAS();
        if (ws.getId() != null) {
            dto.setId(ws.getId());
        }
        if(ws.getEntity() != null) {
            dto.setEntity(companyDAS.find(ws.getEntity()));
        }
        dto.setInitialIncrement(ws.getInitialIncrement());
        dto.setInitialRoundingMode(ws.getInitialRoundingMode());
        dto.setMainIncrement(ws.getMainIncrement());
        dto.setMainRoundingMode(ws.getMainRoundingMode());
        dto.setName(ws.getName());
        dto.setGlobal(ws.isGlobal());

        List<RatingSchemeAssociation> associationList = new ArrayList<>();
        if(ws.getAssociations() != null) {
            for (RatingSchemeAssociationWS ratingSchemeAssociation : ws.getAssociations()) {
                RatingSchemeAssociation association = new RatingSchemeAssociation();
                association.setEntity(ratingSchemeAssociation.getCompany().getId());
                association.setMediation(ratingSchemeAssociation.getMediation().getId());
                association.setRatingIncrement(ratingSchemeAssociation.getRatingScheme());
                association.setId(ratingSchemeAssociation.getId());
                associationList.add(association);
            }
            dto.setAssociations(new HashSet<>(associationList));
        }

        return dto;
    }

    public static BigDecimal getQuantity(Integer ratingSchemeId, Integer callDuration ) {

        BigDecimal quantity = BigDecimal.valueOf(callDuration);

        MediationRatingSchemeDAS mediationCfgDAS = new MediationRatingSchemeDAS();
        MediationRatingSchemeDTO ratingScheme = mediationCfgDAS.findNow(ratingSchemeId);

        if(ratingScheme != null) {
            Integer initialIncrement = ratingScheme.getInitialIncrement();
            Integer initialRoundingMode = ratingScheme.getInitialRoundingMode();
            Integer mainIncrement = ratingScheme.getMainIncrement();
            Integer mainRoundingMode = ratingScheme.getMainRoundingMode();

            if (callDuration <= initialIncrement) {
                quantity = BigDecimal.valueOf(callDuration)
                        .divide(BigDecimal.valueOf(initialIncrement), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND)
                        .setScale(0, initialRoundingMode).multiply(BigDecimal.valueOf(initialIncrement));
            } else {
                quantity = BigDecimal.valueOf(initialIncrement)
                        .add((BigDecimal.valueOf(callDuration - initialIncrement)
                                .divide(BigDecimal.valueOf(mainIncrement), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND))
                                .setScale(0, mainRoundingMode).multiply(BigDecimal.valueOf(mainIncrement)));
            }

            return quantity.divide(BigDecimal.valueOf(MINUTE_IN_SECONDS), 1, Constants.BIGDECIMAL_ROUND);
        }

        return quantity;

    }
    
    public MediationRatingSchemeDTO create(MediationRatingSchemeDTO mediationRatingSchemeDTO) {
        return mediationRatingSchemeDAS.save(mediationRatingSchemeDTO);
    }

    public boolean delete() {
        mediationRatingSchemeDAS.delete(mediationRatingSchemeDTO);
        return true;
    }

    public static List<Integer> findAssociatedCompaniesForMediation(Integer mediation, Integer ratingScheme) {
        return mediationRatingSchemeDAS.findAssociatedCompaniesForMediation(mediation, ratingScheme);
    }

    public static Integer getRatingSchemeIdForMediation(Integer mediationCfgId, Integer entity) {
        Integer ratingScheme = mediationRatingSchemeDAS.getRatingSchemeIdForMediation(mediationCfgId, entity);
        if(ratingScheme == null) {
            //Look for global ratingScheme
            ratingScheme = mediationRatingSchemeDAS.findGlobalRatingScheme(entity, null);
        }
        return ratingScheme;
    }

}
