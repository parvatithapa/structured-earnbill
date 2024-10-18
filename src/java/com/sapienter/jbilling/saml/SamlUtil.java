package com.sapienter.jbilling.saml;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.RoleBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by faizan on 2/23/17.
 */
public class SamlUtil {
    private final static FormatLogger LOG = new FormatLogger(SamlUtil.class);
    private static IWebServicesSessionBean webServicesSessionBean = (IWebServicesSessionBean) Context.getBean(Context.Name.WEB_SERVICES_SESSION);

    public static String getDefaultIdpUrl(Integer entityId) {
        CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(entityId);
        String url = null;
        if (null != companyWS) {
            //Using tree map to sort the key to get default idp
            TreeMap<Integer, ArrayList<MetaFieldValueWS>> citMetaFieldMap = new TreeMap<Integer, ArrayList<MetaFieldValueWS>>(companyWS.getCompanyInfoTypeFieldsMap());
            boolean defaultIdp = false;
            for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                if (!defaultIdp) {
                    List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                    for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                        if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_SAML_IDP_ENTITY_ID)) {
                            url = metaFieldValueWS.getStringValue();
                        } else if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_DEFAULT_META_FIELD_NAME)) {
                            defaultIdp = metaFieldValueWS.getBooleanValue();
                        }
                    }
                } else {
                    break;
                }
            }

            //if no default idp is selected then choose the first one.
            if (!defaultIdp && 0 < citMetaFieldMap.size()) {
                List<MetaFieldValueWS> metaFieldValueWSList = citMetaFieldMap.firstEntry().getValue();
                for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                    if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_SAML_IDP_ENTITY_ID)) {
                        url = metaFieldValueWS.getStringValue();
                        break;
                    }
                }
            }
        }
        return url;
    }

    public static Integer getDefaultIdpViaIdpEntityURL(Integer entityId, String url) {
        Integer groupId = null;
        CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(entityId);
        if (null != companyWS) {
            //Using tree map to sort the key to get default idp
            TreeMap<Integer, ArrayList<MetaFieldValueWS>> citMetaFieldMap = new TreeMap<Integer, ArrayList<MetaFieldValueWS>>(companyWS.getCompanyInfoTypeFieldsMap());
            for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                    if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_SAML_IDP_ENTITY_ID)
                            && null != metaFieldValueWS.getStringValue() && metaFieldValueWS.getStringValue().contains(url)) {
                        groupId = metaFieldValueWS.getGroupId();
                    }
                }
            }

            // if url does not match then choose the idp which has default value set
            boolean defaultIdp = false;
            if(null==groupId){
                for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                    if (!defaultIdp) {
                        List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                        for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                            if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_SAML_IDP_ENTITY_ID)
                                    && null != metaFieldValueWS.getStringValue()) {
                                groupId = metaFieldValueWS.getGroupId();
                            } else if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_DEFAULT_META_FIELD_NAME)) {
                                defaultIdp = metaFieldValueWS.getBooleanValue();
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            //if no default idp is selected then choose the first one.
            if (null==groupId && 0 < citMetaFieldMap.size()) {
                List<MetaFieldValueWS> metaFieldValueWSList = citMetaFieldMap.firstEntry().getValue();
                for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                    if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_SAML_IDP_ENTITY_ID)
                            && null != metaFieldValueWS.getStringValue()) {
                        groupId = metaFieldValueWS.getGroupId();
                        break;
                    }
                }
            }
        }
        return groupId;
    }

    public static String getDefaultResetPasswordUrl(Integer entityId) {
        String url = null;
        CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(entityId);
        if (null != companyWS) {
            //Using tree map to sort the key to get default idp
            TreeMap<Integer, ArrayList<MetaFieldValueWS>> citMetaFieldMap = new TreeMap<Integer, ArrayList<MetaFieldValueWS>>(companyWS.getCompanyInfoTypeFieldsMap());
            boolean defaultIdp = false;
            for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                if (!defaultIdp) {
                    List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                    for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                        if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_RESET_PASSWORD_URL_META_FIELD_NAME)) {
                            url = metaFieldValueWS.getStringValue();
                        } else if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_DEFAULT_META_FIELD_NAME)) {
                            defaultIdp = metaFieldValueWS.getBooleanValue();
                        }
                    }
                } else {
                    break;
                }
            }

            //if no default idp is selected then choose the first one.
            if (!defaultIdp && 0 < citMetaFieldMap.size()) {
                List<MetaFieldValueWS> metaFieldValueWSList = citMetaFieldMap.firstEntry().getValue();
                for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                    if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_RESET_PASSWORD_URL_META_FIELD_NAME)) {
                        url = metaFieldValueWS.getStringValue();
                        break;
                    }
                }
            }
        }
        return url;
    }

    //Gets Status from Database
    public static boolean getUserSSOEnabledStatus(Integer userId) {
        UserDTO user = new UserDAS().find(userId);
        List<MetaFieldValue> metaFieldValueList;
        if (null != user.getPartner()) {
            PartnerDTO partner = user.getPartner();
            metaFieldValueList = partner.getMetaFields();
            for (MetaFieldValue value : metaFieldValueList) {
                if (value.getField().getName().equalsIgnoreCase(com.sapienter.jbilling.server.util.Constants.SSO_ENABLED_AGENT)) {
                    return (boolean) value.getValue();
                }
            }
        } else if (null != user.getCustomer()) {
            CustomerDTO customer = user.getCustomer();
            metaFieldValueList = customer.getMetaFields();
            for (MetaFieldValue value : metaFieldValueList) {
                if (value.getField().getName().equalsIgnoreCase(com.sapienter.jbilling.server.util.Constants.SSO_ENABLED_CUSTOMER)) {
                    return (boolean) value.getValue();
                }
            }
        } else {
            metaFieldValueList = user.getMetaFields();
            for (MetaFieldValue value : metaFieldValueList) {
                if (value.getField().getName().equalsIgnoreCase(com.sapienter.jbilling.server.util.Constants.SSO_ENABLED_USER)) {
                    return (boolean) value.getValue();
                }
            }
        }
        return false;
    }

    public static Integer getDefaultIdpRole(Integer entityId) {
        Integer role = Constants.TYPE_CLERK;
        CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(entityId);
        if (null != companyWS) {
            //Using tree map to sort the key to get default idp
            TreeMap<Integer, ArrayList<MetaFieldValueWS>> citMetaFieldMap = new TreeMap<Integer, ArrayList<MetaFieldValueWS>>(companyWS.getCompanyInfoTypeFieldsMap());
            String defaultRole = Constants.CIT_TYPE_CLERK_ENUM_NAME;
            boolean defaultIdp = false;
            for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                if (!defaultIdp) {
                    List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                    for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                        if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_DEFAULT_ROLE_META_FIELD_NAME)) {
                            defaultRole = metaFieldValueWS.getStringValue();
                        } else if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_DEFAULT_META_FIELD_NAME)) {
                            defaultIdp = metaFieldValueWS.getBooleanValue();
                        }
                    }
                } else {
                    break;
                }
            }

            //if no default idp is selected then choose the first one.
            if (!defaultIdp && citMetaFieldMap.size() > 0) {
                List<MetaFieldValueWS> metaFieldValueWSList = citMetaFieldMap.firstEntry().getValue();
                for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                    if (metaFieldValueWS.getFieldName().equalsIgnoreCase(com.sapienter.jbilling.client.util.Constants.CIT_DEFAULT_ROLE_META_FIELD_NAME)) {
                        defaultRole = metaFieldValueWS.getStringValue();
                        break;
                    }
                }
            }

            if (defaultRole.equalsIgnoreCase(Constants.CIT_TYPE_ROOT_ENUM_NAME)) {
                role = Constants.TYPE_ROOT;
            } else if (defaultRole.equalsIgnoreCase(Constants.CIT_TYPE_CLERK_ENUM_NAME)) {
                role = Constants.TYPE_CLERK;
            } else if (defaultRole.equalsIgnoreCase(Constants.CIT_TYPE_SYSTEM_ADMIN_ENUM_NAME)) {
                role = Constants.TYPE_SYSTEM_ADMIN;
            }

            RoleDTO roleDTO = new RoleBL().findByRoleTypeIdAndCompanyId(role, companyWS.getId());
            if(null!=roleDTO){
                return roleDTO.getId();
            }
        }else {
            RoleDTO roleDTO = new RoleBL().findByRoleTypeIdAndCompanyId(role, null);
            if(null!=roleDTO) {
                return roleDTO.getId();
            }
        }
        return role;
    }

    public static boolean getDefaultJITConfig(Integer entityId) {
        boolean justInTime = false;
        boolean defaultIdp = false;
        CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(entityId);
        if (null != companyWS) {
            //Using tree map to sort the key to get default idp
            Map<Integer, ArrayList<MetaFieldValueWS>> companyInfoTypeFieldsMap = companyWS.getCompanyInfoTypeFieldsMap();
            if (null != companyInfoTypeFieldsMap && 0 < companyInfoTypeFieldsMap.size()) {
                Map<Integer, ArrayList<MetaFieldValueWS>> citMetaFieldMap = new TreeMap<Integer, ArrayList<MetaFieldValueWS>>(companyInfoTypeFieldsMap);
                for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                    if (!defaultIdp) {
                        List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                        for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                            if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_JIT_META_FIELD_NAME)) {
                                justInTime = metaFieldValueWS.getBooleanValue();
                            } else if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_DEFAULT_META_FIELD_NAME)) {
                                defaultIdp = metaFieldValueWS.getBooleanValue();
                            }
                        }
                    } else {
                        break;
                    }
                }
            }

            //if no default idp is selected then justInTime will be false.
            if (!defaultIdp) {
                justInTime = false;
            }
        }
        return justInTime;
    }
}
