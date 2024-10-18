package com.sapienter.jbilling.saml.security.saml

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.saml.integration.oauth.OAuthUrlSignerImpl
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.user.CompanyWS
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
public class SamlMetadataLocationResolverImpl implements MetadataLocationResolver {

    IWebServicesSessionBean iWebServicesSessionBean = (IWebServicesSessionBean) Context.getBean(Context.Name.WEB_SERVICES_SESSION);

    @Autowired
    GrailsApplication grailsApplication

    /**
     * This implementations turns AppDirect entity IDs into AppDirect metadata URLs and signs it for OAuth.
     */
    @Override
    public String resolve(String entityId) {

        if (entityId.equals(grailsApplication.config.grails.serverURL)) {
            return null;
        }

        String samlIdpMetadataUrl = null;
        CompanyWS company = iWebServicesSessionBean.getCompanyByMetaFieldValue(entityId);

        Map<Integer, ArrayList<MetaFieldValueWS>> companyInfoTypeFieldsMap = company.getCompanyInfoTypeFieldsMap();
        if (null != companyInfoTypeFieldsMap && 0 < companyInfoTypeFieldsMap.size()) {
            Map<Integer, ArrayList<MetaFieldValueWS>> citMetaFieldMap = new TreeMap<Integer, ArrayList<MetaFieldValueWS>>(companyInfoTypeFieldsMap);
            for (Map.Entry<Integer, ArrayList<MetaFieldValueWS>> entry : citMetaFieldMap.entrySet()) {
                if (!samlIdpMetadataUrl) {
                    List<MetaFieldValueWS> metaFieldValueWSList = entry.getValue();
                    for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSList) {
                        if (metaFieldValueWS.getFieldName().equalsIgnoreCase(Constants.CIT_SAML_IDP_METADATA_URL)) {
                            samlIdpMetadataUrl = metaFieldValueWS.getStringValue();
                            if (samlIdpMetadataUrl){
                                break;
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        }

        OAuthUrlSignerImpl oauthUrlSigner = new OAuthUrlSignerImpl(grailsApplication.config.oauthConsumerKey, grailsApplication.config.oauthConsumerSecret);
        return oauthUrlSigner.sign(samlIdpMetadataUrl);
    }
}
