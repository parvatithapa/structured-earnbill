package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.util;

import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;;

/**
 * Created by neelabh on 03/08/16.
 */
public class MediationUtil {
	
	public static boolean isInbound(String mediationConfig, String direction, Integer entityId) {
 
        boolean found = false;
        if (mediationConfig.equals(FullCreativeConstants.INBOUND_CALL_MEDIATION_CONFIGURATION) && (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.INBOUND_CALL_TYPE.getMetaFieldName(), entityId)) || direction.equalsIgnoreCase(FullCreativeConstants.FILE_OUTBOUND_IDENTIFIER))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.SPANISH_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.SPANISH_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.SUPERVISOR_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.SUPERVISOR_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.CALL_RELAY_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.CALL_RELAY_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.LIVE_RECEPTION_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.LIVE_RECEPTION_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        return found;
    }

    public static boolean isIVR(String mediationConfig, String direction, Integer entityId) {

        boolean found = false;
        if (mediationConfig.equals(FullCreativeConstants.IVR_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.IVR_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.IVR_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.MOBILE_APP_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.IVR_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.VOICE_MAIL_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (mediationConfig.equals(FullCreativeConstants.IVR_MEDIATION_CONFIGURATION) && direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.WEB_FORM_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        return found;
    }
	
    public static boolean isDirectionInbound(String direction, Integer entityId) {

        boolean found = false;
        if ((direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.INBOUND_CALL_TYPE.getMetaFieldName(), entityId)) || direction.equalsIgnoreCase(FullCreativeConstants.FILE_OUTBOUND_IDENTIFIER))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.SPANISH_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.SUPERVISOR_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.CALL_RELAY_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.LIVE_RECEPTION_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        return found;
    }

    public static boolean isDirectionIVR(String direction, Integer entityId) {

        boolean found = false;
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.IVR_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.MOBILE_APP_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.VOICE_MAIL_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        if (direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.WEB_FORM_CALL_TYPE.getMetaFieldName(), entityId))) {
            found = true;
        }
        return found;
    }
}

