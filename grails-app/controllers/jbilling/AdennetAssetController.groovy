/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package jbilling

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.adennet.AdennetConstants
import com.sapienter.jbilling.server.adennet.ws.SimReissueRequestWS
import com.sapienter.jbilling.server.item.db.AssetDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugin.springsecurity.annotation.Secured

import java.time.OffsetDateTime

import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_REISSUE
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_RELEASE
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_SUSPEND_ACTIVATE
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.REISSUE_COUNT_DURATION_IN_MONTH
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.REISSUE_COUNT_LIMIT
import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.SIM_REISSUE_FEE_ID

class AdennetAssetController {
    def adennetHelperService
    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils

    @Secured([PERMISSION_SUSPEND_ACTIVATE])
    def showSuspendOrActivate() {

        def asset = params.id ? AssetDTO.get(params.id) : new AssetDTO()

        render view: 'suspendActive',
                model: [asset      : asset,
                        isSuspended: Boolean.valueOf(asset.suspended),
                        userId     : params?.userId
                ]
    }

    @Secured([PERMISSION_REISSUE])
    def showReissueSim() {
        try {
            def asset = params.id ? AssetDTO.get(params.id) : new AssetDTO()
            // restrict sim reissue if the subscriber is suspended
            if (asset.isSuspended()){
                render view: '/login/denied'
                return
            }
            Integer simReissueFeeId = adennetHelperService.getValueFromExternalConfigParams(SIM_REISSUE_FEE_ID) as Integer
            Integer reissueCountLimit = adennetHelperService.getValueFromExternalConfigParams(REISSUE_COUNT_LIMIT) as Integer
            Integer reissueDuration = adennetHelperService.getValueFromExternalConfigParams(REISSUE_COUNT_DURATION_IN_MONTH) as Integer
            def simReissueFee = webServicesSession.getItem(simReissueFeeId, null, null).getPrice()
            def user = webServicesSession.getUserWS(params?.userId as Integer)
            render view: 'reissueSim',
                    model: [asset            : asset,
                            user             : user,
                            simReissueFee    : String.format("%.2f", new BigDecimal(simReissueFee)),
                            reissueCountLimit: reissueCountLimit,
                            reissueDuration  : reissueDuration
                    ]
        } catch (SessionInternalError sessionInternalError) {
            viewUtils.resolveException(flash, session.locale, sessionInternalError)
            redirect controller: 'customer', action: 'list'
        }
    }

    @Secured([PERMISSION_SUSPEND_ACTIVATE])
    def suspendOrActivate() {
        try {
            def assetId = params.id as Integer
            def asset = webServicesSession.getAsset(assetId)
            def notes = params.notes
            def suspendedBy = adennetHelperService.getUserNameByUserId(session['user_id'] as Integer)

            asset.setSuspended(Boolean.valueOf(params.isSuspended))
            asset.setNotes(notes)

            if(!asset.isSuspended){
                asset.setSuspendedBy("")
            }else{
                asset.setSuspendedBy(suspendedBy)
            }

            webServicesSession.updateAsset(asset)
            
            if (params.isSuspended != null) {
                if (Boolean.valueOf(params.isSuspended))
                    flash.info = asset.getIdentifier() + ' ' + g.message(code: "subscriber.status.suspended")
                else
                    flash.info = asset.getIdentifier() + ' ' + g.message(code: "subscriber.status.activated")
                redirect controller: 'customer', action: 'list'
            }
        } catch (SessionInternalError sessionInternalError) {
            viewUtils.resolveException(flash, session.locale, sessionInternalError)
            redirect controller: 'customer', action: 'list'
        }
    }

    @Secured([PERMISSION_REISSUE])
    def reissueSim() {
        try {
            def oldIdentifier = params.identifier as String
            def newIdentifier = params.newIdentifier as String
            def notes = params.notes
            // restrict sim reissue if subscriber is suspended
            if (webServicesSession.getAssetByIdentifier(oldIdentifier).isSuspended) {
                render view: '/login/denied'
                return
            }
            def loggedInUser = webServicesSession.getUserWS(session['user_id'] as Integer)
            def reissuedBy = loggedInUser.userName
            def governorate = adennetHelperService.getLoggedInUserGovernorate(loggedInUser.id as Integer)
            String transactionId

            SimReissueRequestWS reissueRequestWS = SimReissueRequestWS.builder()
                    .entityId(params.entities as Integer)
                    .userId(params.userId as Integer)
                    .subscriberNumber(params.subscriberNumber)
                    .amount(params.reissueFee as BigDecimal)
                    .narration(AdennetConstants.SIM_REISSUE_FEE_NARRATION)
                    .transactionAmount(params.reissueFee as BigDecimal)
                    .transactionDateTime(OffsetDateTime.now().toString())
                    .createdBy(reissuedBy)
                    .source(AdennetConstants.SOURCE_POS)
                    .governorate(governorate as String)
                    .build()

            transactionId = adennetHelperService.reissue(oldIdentifier, newIdentifier, notes, reissueRequestWS)
            def userId = reissueRequestWS.getUserId() as Integer
            flash.message = reissueRequestWS.getSubscriberNumber() + ' ' + g.message(code: "subscriber.status.reissued")
            redirect controller: 'customer', action : 'showReceipt', params:[userId: userId, transactionId: transactionId.substring(14) as Long]
        } catch (SessionInternalError error) {
            viewUtils.resolveException(flash, session.locale, error)
            redirect action: 'showReissueSim', params: [id: params.id as Integer, userId: params?.userId as Integer]
        }
    }

    def list() {
        redirect controller: 'customer', action: 'list'
    }

    @Secured([PERMISSION_RELEASE])
    def releaseSim() {
        try {
            adennetHelperService.releaseSim(params.identifier)
        }
        catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
        }
        redirect controller: 'customer', action: 'list'
    }

    def checkReissueAssetIsValid() {
        try {
            render webServicesSession.validateReissueAsset(params.identifier as String)
        } catch (SessionInternalError sessionInternalError) {
            viewUtils.resolveException(flash, session.locale, sessionInternalError)
            render sessionInternalError.errorMessages
        }
    }
}


