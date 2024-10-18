/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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
package jbilling

import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.Constants
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.saml.SamlUtil
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured

import org.hibernate.FetchMode
import org.springframework.security.saml.SAMLEntryPoint

@Secured(["isFullyAuthenticated()"])
class MyAccountController {
	static scope = "prototype"
    def breadcrumbService
    IWebServicesSessionBean webServicesSession

    def index () {
        if(!chainModel) {
            breadcrumbService.addBreadcrumb("myAccount", "index", null, null)
            flash.isChainModel = true
            chain controller: getControllerName(), action: "show", id: session['user_id']
        } else {
            flash.message = params.message
            flash.args = [params.args]
            if(isCustomer()){
                chainModel.selected?.attach()
                chainModel.selected?.customer?.attach()
                chainModel.selected?.customer?.children?.each {
                    it.attach()
                }
                chainModel.selected?.paymentInstruments?.each{
                    it.paymentMethodType.attach()
                }
                chainModel.selected?.customer?.partners?.each {
                    it.attach()
                }
                chainModel.customerNotes?.each{
                    it.attach()
                }
                chainModel.metaFields?.each{
                    it.field?.attach()
                }
            } else {
                chainModel.selected?.attach()
            }
            chainModel['f'] = 'myAccount'
            render view : isCustomer() ? "showCustomer" : "showUser", model: chainModel
        }
    }

    def edit () {
        Integer userId = session['user_id'] as Integer
        boolean isUserSSOEnabled = SamlUtil.getUserSSOEnabledStatus(userId)
        def ssoActive = PreferenceBL.getPreferenceValue(session['company_id'] as int, CommonConstants.PREFERENCE_SSO) as int
        if(!chainModel) {
            flash.isChainModel = true
            chain controller: getControllerName(), action: "edit", id: session['user_id']
        } else {
            if(isCustomer()){
                chainModel.company?.attach()
                chainModel.paymentMethods.each {
                    it.attach()
                }
                chainModel.customerNotes?.each {
                    it.attach()
                }
                chainModel.metaFields?.each {
                    it.field?.attach()
                }
                chainModel.accountInformationTypes?.each {
                    it.attach()
                }
            }

            Integer companyId = session['company_id'] as Integer
            def url = null
            if (isUserSSOEnabled && 1 == ssoActive) {
                url = SamlUtil.getDefaultResetPasswordUrl(companyId)
                if (url) {
                    def defaultIdp = SamlUtil.getDefaultIdpUrl(companyId)
                    if (defaultIdp) {
                        url += "?" + SAMLEntryPoint.IDP_PARAMETER + "=" + defaultIdp
                    } else {
                        log.error("No default Idp is configured for company : " + companyId)
                        flash.error = message(code: 'default.idp.error')
                    }
                } else {
                    log.error("No default reset password url of Idp is configured for company : " + companyId)
                    flash.error = message(code: 'default.reset.password.url.idp.error')
                }
            }

            chainModel.put("isUserSSOEnabled", isUserSSOEnabled)
            chainModel.put("ssoActive", ssoActive)
            chainModel.put("resetPasswordUrl", url)

            if (session['main_role_id'] != Constants.TYPE_CUSTOMER && session['main_role_id'] != Constants.TYPE_PARTNER) {
                def availableFields;
                def defaultIdp = null;
                def user = session['user_id'] ? webServicesSession.getUserWS(session['user_id'] as Integer) : new UserWS()
                def company = CompanyDTO.createCriteria().get {
                    eq("id", companyId)
                    fetchMode('contactFieldTypes', FetchMode.JOIN)
                }

                def companyInfoTypes = company.getCompanyInformationTypes();
                availableFields = getMetaFields()

                if (user.id > 0) {
                    MetaFieldValueWS[] metaFieldValueWS = user.getMetaFields();
                    for (int i = 0; i < metaFieldValueWS.length; i++) {
                        if (metaFieldValueWS[i].getFieldName().equalsIgnoreCase(com.sapienter.jbilling.server.util.Constants.SSO_IDP_ID_USER)) {
                            defaultIdp = metaFieldValueWS[i].getIntegerValue()
                            break;
                        }
                    }
                }

                chainModel.put("availableFields", availableFields)
                chainModel.put("companyInfoTypes", companyInfoTypes)
                chainModel.put("defaultIdp", defaultIdp)
                if(!SpringSecurityUtils.ifAnyGranted('MY_ACCOUNT_162')) {
                    chainModel.put("notEditable", true)
                }
            }


            render view: isCustomer() ? '/customer/edit' : 'editUser', model: chainModel
        }
    }

    /**
     * This call returns meta fields according to an entity
     * @param product
     * @return
     */
    def getMetaFields () {
        def availableFields = new HashSet<MetaField>();
        availableFields.addAll(retrieveAvailableMetaFields(session['company_id']))
        return availableFields;
    }

    def retrieveAvailableMetaFields(entityId) {
        return MetaFieldBL.getAvailableFieldsList(entityId, EntityType.USER)
    }

    @RequiresValidFormToken
    def save (){
        flash.isChainModel = true
        forward controller: getControllerName(), action: isCustomer() ? 'saveCustomer':'saveUser', params: params
    }

    private boolean isCustomer(){
        return session['main_role_id'] == Constants.TYPE_CUSTOMER
    }

    private String getControllerName() {
        return isCustomer() ? "customer" : "user"
    }
}
