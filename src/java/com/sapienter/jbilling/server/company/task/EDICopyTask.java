package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.*;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Created by vivek on 21/10/15.
 */
public class EDICopyTask extends AbstractCopyTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EDICopyTask.class));
    EDITypeDAS ediTypeDAS;
    EDITypeBL ediTypeBL;
    IWebServicesSessionBean webServicesSessionBean;

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        Long count = ediTypeDAS.countByEntity(entityId);
        return count != 0;
    }

    public EDICopyTask() {
        init();
    }

    private void init() {
        ediTypeDAS = new EDITypeDAS();
        ediTypeBL = new EDITypeBL();
        webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("EDI copy task has been started.");

        copyEDIType(entityId, targetEntityId);

        LOG.debug("EDI copy task has been finished.");


    }

    public void copyEDIType(Integer entityId, Integer targetEntityId) {

        List<EDITypeDTO> ediTypeDTOs = ediTypeDAS.getEDITypesByEntity(entityId);
        for (EDITypeDTO ediTypeDTO : ediTypeDTOs) {
            EDITypeWS copyEDITypeWS = ediTypeBL.getWS(ediTypeDTO);

//          Treat EDITypeWS as new Object.
            copyEDITypeWS.setId(0);
//          create EDI type for new entity
            copyEDITypeWS.setEntityId(targetEntityId);
//          Treat EDIFileStatusWS as new Object.
            for (EDIFileStatusWS ediFileStatusWS : copyEDITypeWS.getEdiStatuses()) {
                ediFileStatusWS.setId(null);
                for (EDIFileExceptionCodeWS ediFileExceptionCodeWS : ediFileStatusWS.getExceptionCodes()) {
                    ediFileExceptionCodeWS.setId(null);
                }
            }
            copyEDITypeWS.getEntities().remove(entityId);
            copyEDITypeWS.getEntities().add(targetEntityId);

            String ediFormatFileName = "" + ediTypeDTO.getPath() + FileConstants.HYPHEN_SEPARATOR + ediTypeDTO.getEntity().getId() + ".xml";
            File formatFile = new File(FileConstants.getFormatFilePath() + File.separator + ediFormatFileName);
            // EDI type may using default format file so in that case, file will not present in format dir.
            // Just send null and it will use default file for new edi type too based on its suffix.
            if(!formatFile.exists()) {
                formatFile=null;
            }
            Integer copiedEDITypeId = ediTypeBL.createEDIType(copyEDITypeWS, formatFile);
//          Set old new EDI type id in map
            CopyCompanyUtils.oldNewEDITypeMap.put(ediTypeDTO.getId(), copiedEDITypeId);
            copyCompanyMetaFieldValue(entityId, targetEntityId);
        }
    }

    private void copyCompanyMetaFieldValue(Integer entityId, Integer targetEntityId) {

        CompanyDAS companyDAS = new CompanyDAS();
        CompanyDTO oldCompany = companyDAS.find(entityId);
        CompanyDTO targetCompany = companyDAS.find(targetEntityId);

        CompanyWS oldCompanyWS = EntityBL.getCompanyWS(oldCompany);
        CompanyWS targetCompanyWS = EntityBL.getCompanyWS(targetCompany);

        MetaFieldValueWS[] oldCompanyWSMetaFields = oldCompanyWS.getMetaFields();
        MetaFieldValueWS[] targetMetaFieldValueWSes = MetaFieldHelper.copy(oldCompanyWSMetaFields, true);

//        Fetch company meta fields in Map. It will help to set new data in new metafield.
        Map<String, MetaFieldValueWS> companyMetaFieldValueMap = new HashMap<String, MetaFieldValueWS>();
        for(MetaFieldValueWS metaFieldValueWS : oldCompanyWSMetaFields) {
            for(MetaFieldValueWS  copyMetaFieldValueWS : targetMetaFieldValueWSes) {
                if(metaFieldValueWS.getFieldName().equals(copyMetaFieldValueWS.getFieldName())) {
                    companyMetaFieldValueMap.put(metaFieldValueWS.getFieldName(), copyMetaFieldValueWS);
                }
            }
        }

//        Set new EDI types here.
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.TERMINATION_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.PAYMENT_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.ACKNOWLEDGE_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME);
        setEDITypeValue(companyMetaFieldValueMap, FileConstants.AUTO_RENEWAL_EDI_TYPE_ID_META_FIELD_NAME);

//        Set Supplier DUNS and Utility DUNS numbers. These number will be random UUID.
        setDUNSNumberValue(companyMetaFieldValueMap, FileConstants.SUPPLIER_DUNS_META_FIELD_NAME);
        setDUNSNumberValue(companyMetaFieldValueMap, FileConstants.UTILITY_DUNS_META_FIELD_NAME);

        targetCompanyWS.setMetaFields(targetMetaFieldValueWSes);
        new EntityBL().updateEntityAndContact(targetCompanyWS, targetEntityId, webServicesSessionBean.getCallerId());
    }

    private void setEDITypeValue(Map<String, MetaFieldValueWS> companyMetaFieldValueMap, String metaFieldName) {
        MetaFieldValueWS metaFieldValueWS = companyMetaFieldValueMap.get(metaFieldName);
        if(metaFieldValueWS != null) {
            metaFieldValueWS.setValue(CopyCompanyUtils.oldNewEDITypeMap.get(metaFieldValueWS.getValue()));
        }
    }

    private void setDUNSNumberValue(Map<String, MetaFieldValueWS> companyMetaFieldValueMap, String metaFieldName) {
        MetaFieldValueWS metaFieldValueWS = companyMetaFieldValueMap.get(metaFieldName);
        if(metaFieldValueWS != null) {
            metaFieldValueWS.setValue(RandomStringUtils.randomNumeric(10));
        }
    }

    private Map<EDIFileStatusDTO, EDIFileStatusDTO> getNewEDIFileStatusMap(EDITypeDTO ediTypeDTO, EDITypeDTO copiedEDITypeDTO) {
        Map<EDIFileStatusDTO, EDIFileStatusDTO> map = new HashMap<EDIFileStatusDTO, EDIFileStatusDTO>();
        for (EDIFileStatusDTO fileStatusDTO : ediTypeDTO.getStatuses()) {
            for (EDIFileStatusDTO copiedFileStatusDTO : copiedEDITypeDTO.getStatuses()) {
                if (fileStatusDTO.getName().equals(copiedFileStatusDTO.getName())) {
                    map.put(fileStatusDTO, copiedFileStatusDTO);
                }
            }
        }
        return map;
    }

    public void cleanUp(Integer targetEntityId){
        LOG.debug("Call cleanUp for "+this.getClass().getName());
        deleteDir(new File(FileConstants.getEDITypePath(targetEntityId, "", "")));
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir
                        (new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
