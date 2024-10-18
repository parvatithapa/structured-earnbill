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
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.adennet.ws.ReceiptWS
import com.sapienter.jbilling.server.adennet.ws.recharge_history.CancelRechargeTransactionWS
import com.sapienter.jbilling.server.adennet.ws.recharge_history.RechargeTransactionResponseWS
import com.sapienter.jbilling.server.adennet.ws.recharge_history.RechargeTransactionWS
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugin.springsecurity.SpringSecurityUtils
import org.hibernate.FetchMode
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions
import org.springframework.security.access.annotation.Secured
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_RECHARGE_HISTORY_MENU
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_REFUND_RECHARGE_TRANSACTION
import static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS

@Secured([PERMISSION_RECHARGE_HISTORY_MENU])
class RechargeTransactionHistoryController {
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    public static final String RECEIPT_TYPE_CANCELLED = "Cancelled"
    def filterService
    def adennetHelperService
    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    RechargeTransactionResponseWS rechargeHistoryData;
    ViewUtils viewUtils

    def index() {
        list()
    }

    def getList(filters, params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def company_id = session['company_id']
        return RechargeTransactionResponseWS.createCriteria().list(
                max: params.max,
                offset: params.offset) {
            createAlias('user', 'u')

            and {
                filters.each { filter ->
                    if (filter.value != null) {
                        if (filter.field == 'contact.fields') {
                            String typeId = params['contactFieldTypes']
                            String ccfValue = filter.stringValue
                            log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"

                            if (typeId && ccfValue) {
                                MetaField type = findMetaFieldType(typeId.toInteger())
                                if (type != null) {
                                    createAlias("metaFields", "fieldValue")
                                    createAlias("fieldValue.field", "type")
                                    setFetchMode("type", FetchMode.JOIN)
                                    eq("type.id", typeId.toInteger())

                                    switch (type.getDataType()) {
                                        case DataType.STRING:
                                            def subCriteria = DetachedCriteria.forClass(StringMetaFieldValue.class, "stringValue")
                                                    .setProjection(Projections.property('id'))
                                                    .add(Restrictions.like('stringValue.value', ccfValue + '%').ignoreCase())

                                            addToCriteria(Property.forName("fieldValue.id").in(subCriteria))
                                            break;
                                        case DataType.INTEGER:
                                            def subCriteria = DetachedCriteria.forClass(IntegerMetaFieldValue.class, "integerValue")
                                                    .setProjection(Projections.property('id'))
                                                    .add(Restrictions.eq('integerValue.value', ccfValue.toInteger()))

                                            addToCriteria(Property.forName("fieldValue.id").in(subCriteria))
                                            break
                                        case DataType.JSON_OBJECT:
                                            addToCriteria(Restrictions.ilike("fieldValue.value", ccfValue, MatchMode.ANYWHERE))
                                            break
                                        default:
                                            // todo: now searching as string only, search for other types is impossible
                                            addToCriteria(Restrictions.eq("fieldValue.value", ccfValue))
                                            break
                                    }

                                }
                            }
                        } else {
                            addToCriteria(filter.getRestrictions());
                        }
                    }

                }
                eq('u.company', new CompanyDTO(company_id as Integer))
                eq('deleted', 0)

                if (SpringSecurityUtils.ifNotGranted("PAYMENT_36")) {
                    if (SpringSecurityUtils.ifAnyGranted("PAYMENT_37")) {
                        // restrict query to sub-account user-ids
                        def subAccountIds = subAccountService.getSubAccountUserIds()
                        if (subAccountIds.size() > 0) {
                            'in'('u.id', subAccountIds)
                        }
                    } else {
                        if (customersForUser.size() > 0) {
                            // limit list to only this customer
                            'in'('u.id', customersForUser*.baseUser.userId)
                        }
                    }
                }

            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def show() {
        def transactionId = params.transactionId as long

        def rechargeDate = params.rechargeDate as String
        def viewAllTransactions = SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS) as Boolean
        def refundTransactions = SpringSecurityUtils.ifAllGranted(PERMISSION_REFUND_RECHARGE_TRANSACTION) as Boolean
        log.debug "recharge date: " + rechargeDate

        try {
            LocalDateTime currentDateTimeInUTC = LocalDateTime.now(ZoneId.of("UTC"))
            LocalDateTime transactionDateTimeInUtc = LocalDateTime.parse(rechargeDate.replaceAll(" ", "T"))

            if (Duration.between(transactionDateTimeInUtc, currentDateTimeInUTC).toHours() > 24 && !viewAllTransactions) {
                refundTransactions = false
            }

            rechargeHistoryData = adennetHelperService.getRechargeTransactionById(transactionId)
            RechargeTransactionWS rechargeTransactionWS = rechargeHistoryData.getData().get(0)

            render template: 'show', model: ['rechargeTransactionWS': rechargeTransactionWS, 'flag': refundTransactions]
        } catch (Exception exception) {
            log.error("error while displaying customers recharge transactions:{}", exception)
            flash.error = exception
        }
    }

    def list() {
        def filters = filterService.getFilters(FilterType.RECHARGE_HISTORY, params)
        def selected = params.id ? RechargeTransactionResponseWS.get(params.int("id")) : null
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.filterBy = params?.filterBy ?: ""
        def pageNumber = (params.offset / params.max) + 1 as Integer
        def pageLimit = params.max
        def loggedInUserId = session['user_id'] as Integer

        try {
            rechargeHistoryData = adennetHelperService.getRechargeTransactions(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS), pageNumber, pageLimit, params.applyFilter as boolean, filters as ArrayList, loggedInUserId)
        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Error occur while canceling recharge request: {}", exception)
            flash.error = exception
            redirect action: 'list'
        } catch (Exception exception) {
            log.error("Error occur while canceling recharge request: {}", exception)
            flash.error = exception
            redirect action: 'list'
        }
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)

        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if (params.id?.isInteger() && selected == null) {
            flash.error = message(code: 'recharge.history.not.found')
        }

        if (params.applyFilter || params.partial) {
            render template: 'rechargeTransactionHistory', model: [rechargeHistoryData: rechargeHistoryData, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'] as Integer)]
        } else {
            render view: 'list', model: [rechargeHistoryData: rechargeHistoryData, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'] as Integer)]
        }
    }

    @Secured([PERMISSION_REFUND_RECHARGE_TRANSACTION])
    def cancelRechargeRequest() {
        try {
            def rechargeTransactionID = params.transactionId as Long
            def sourceOfRefund = params.source as String
            def note = params.note as String
            def userId = params.userId as Integer
            def transactionType = params.type as String
            def isSimIssued = params.getBoolean('isSimIssued')
            def isSimReIssued = params.getBoolean('isSimReIssued')
            def isWalletTopUp = params.getBoolean("isWalletTopUp")
            def loggedInUser = webServicesSession.getUserWS(session['user_id'] as Integer)
            def refundedBy = loggedInUser.userName as String
            def currentIdentifier = null
            def walletTransactionResponseWS
            def rechargeTransactionResponseWS
            def offSet = Integer.valueOf(1)
            def limit = Integer.valueOf(20)
            def createdBy = params.createdBy as String
            def transactionDate = params.transactionDate as String
            def viewAllTransactions = SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS) as Boolean
            LocalDateTime currentDateTimeInUTC = LocalDateTime.now(ZoneId.of("UTC"))
            LocalDateTime transactionDateTimeInUtc = LocalDateTime.parse(transactionDate.replaceAll(" ", "T"))

            if((!viewAllTransactions && createdBy!=refundedBy) || (Duration.between(transactionDateTimeInUtc, currentDateTimeInUTC).toHours() > 24 && !viewAllTransactions)){
                render view: '/login/denied'
                return
            }

            log.info "transactionType: " + transactionType
            CancelRechargeTransactionWS cancelRechargeTransactionWS = CancelRechargeTransactionWS.builder()
                    .rechargeTransactionId(rechargeTransactionID)
                    .sourceOfRefund(sourceOfRefund)
                    .refundedBy(refundedBy)
                    .note(note)
                    .refundDateTime(OffsetDateTime.now())
                    .build()

            log.debug "cancelRechargeTransaction: " + cancelRechargeTransactionWS

            UserWS userWS = webServicesSession.getUserWS(userId)
            if(userWS.getDeleted()){
                flash.error = "validation.error.refund.not.allowed"
                flash.args = [userWS.id]

                redirect action: 'list'
                return
            }
            currentIdentifier = userWS.getUserName()

            String transactionId = adennetHelperService.cancelRechargeTransaction(cancelRechargeTransactionWS, currentIdentifier, isSimReIssued, isSimIssued)
            log.info("Cancel recharge TransactionId=" + transactionId + " for userId=" + userId)

            if (isWalletTopUp) {
                rechargeTransactionResponseWS = adennetHelperService.getRechargeTransactionByParentTransactionId(rechargeTransactionID)
                walletTransactionResponseWS = adennetHelperService.getWalletTransactions(userId, offSet, limit, (rechargeTransactionResponseWS.getData().stream().findFirst()).get().getId())
            } else {
                walletTransactionResponseWS = adennetHelperService.getWalletTransactions(userId, offSet, limit, rechargeTransactionID)
            }
            ReceiptWS receiptWS = adennetHelperService.getReceiptWsForTransactions(walletTransactionResponseWS, RECEIPT_TYPE_CANCELLED, userId, session.locale)

            flash.message = "flash.refund.amount.done"

            if ( (receiptWS.primaryPlanWS == null || receiptWS.primaryPlanWS.cashPrice == 0) && (receiptWS.feeWSList == null || receiptWS.getFeeWSList().size() == 0) && (receiptWS.totalReceiptAmount == null || receiptWS.totalReceiptAmount == 0)) {
                redirect action: 'list'
            } else {
                chain action : 'showReceipt', params: [userId: userId, transactionId: rechargeTransactionID, isWalletTopUp:isWalletTopUp, receiptType: 'RefundReceipt' ], model: [receiptWS: receiptWS]
            }

        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Error occur while cancelling recharge request: ", exception)
            flash.error = exception.getMessage().toString()
            redirect action: 'list'
        } catch (SessionInternalError error) {
            viewUtils.resolveException(flash, session.locale, error)
            redirect action: 'list'
        } catch (Exception exception) {
            log.error("Error occur while cancelling recharge request Exception", exception)
            flash.error = exception.getMessage().toString()
            redirect action: 'list'
        }
    }

    def filterRechargeTransactions() {
        def filters = filterService.getFilters(FilterType.RECHARGE_HISTORY, params)
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def pageNumber = (params.offset / params.max) + 1 as Integer
        def pageLimit = params.max
        def loggedInUserId = session['user_id'] as Integer

        try {
            if (params.applyFilter || params.partial) {
                rechargeHistoryData = adennetHelperService.getRechargeTransactions(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS), pageNumber, pageLimit, params.partial as boolean, filters as ArrayList, loggedInUserId)
                render template: 'rechargeTransactionHistory', model: [rechargeHistoryData: rechargeHistoryData]
            } else {
                rechargeHistoryData = adennetHelperService.getRechargeTransactions(SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS), pageNumber, pageLimit, false, null, loggedInUserId)
                render template: 'rechargeTransactionHistory', model: [rechargeHistoryData: rechargeHistoryData]
            }

        } catch (HttpClientErrorException | HttpServerErrorException exception) {
            log.error("Error occur while cancelling recharge request: {}", exception)
            flash.error = exception
            redirect action: 'list'
        } catch (Exception exception) {
            log.error("Error occur while cancelling recharge request: {}", exception)
            flash.error = exception
            redirect action: 'list'
        }
    }

    def showReceipt() {
        try {
            if (chainModel) {
                render view: '/customer/rechargeReceipt', model: [receiptWS: chainModel?.receiptWS]
            } else {
                def userId = params?.userId as Integer
                def transactionId = params?.transactionId as Long
                def receiptType = params?.receiptType as String
                def isWalletTopUp = params?.getBoolean("isWalletTopUp")
                def walletTransactionResponseWS
                def rechargeTransactionResponseWS
                def offSet = Integer.valueOf(1)
                def limit = Integer.valueOf(20)

                def canViewAll = SpringSecurityUtils.ifAllGranted(PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS) as Boolean
                def loggedInUser = webServicesSession.getUserWS(session['user_id'] as Integer).userName as String

                if (isWalletTopUp && (receiptType.equals(RECEIPT_TYPE_CANCELLED) || receiptType.equals("RefundReceipt"))) {
                    rechargeTransactionResponseWS = adennetHelperService.getRechargeTransactionByParentTransactionId(transactionId)
                    if (rechargeTransactionResponseWS.total <= 0) {
                        redirect action: 'list'
                        return
                    }
                    walletTransactionResponseWS = adennetHelperService.getWalletTransactions(userId, offSet, limit, (rechargeTransactionResponseWS.getData().stream().findFirst()).get().getId())
                } else {
                    walletTransactionResponseWS = adennetHelperService.getWalletTransactions(userId, offSet, limit, transactionId)
                }

                ReceiptWS receiptWS = adennetHelperService.getReceiptWsForTransactions(walletTransactionResponseWS, receiptType, userId, session.locale)
                if (receiptWS.receiptNumber != null && (canViewAll || loggedInUser.equals(walletTransactionResponseWS.walletTransactions.get(0).getCreatedBy()))) {
                    render view: '/customer/rechargeReceipt', model: [receiptWS: receiptWS]
                } else {
                    redirect action: 'list'
                }
            }
        } catch (HttpClientErrorException | HttpServerErrorException httpException) {
            log.error("Error occurred in refund receipt while calling getWalletTransactions(): ", httpException)
            viewUtils.resolveException(flash, session.locale, httpException)
            redirect action: 'list'
        } catch (SessionInternalError sessionInternalError) {
            log.error("Error occurred in refund receipt while calling getWalletTransactions(): ", sessionInternalError)
            viewUtils.resolveException(flash, session.locale, sessionInternalError)
            redirect action: 'list'
        } catch (Exception exception) {
            log.error("Error occurred in refund receipt while calling getWalletTransactions(): ", exception)
            redirect action: 'list'
        }
    }
}
