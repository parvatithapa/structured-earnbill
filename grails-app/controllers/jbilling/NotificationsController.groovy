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

package jbilling

import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.csrf.RequiresValidFormToken
import com.sapienter.jbilling.server.notification.MessageDTO
import com.sapienter.jbilling.server.notification.MessageSection
import com.sapienter.jbilling.server.notification.db.NotificationMessageDAS
import com.sapienter.jbilling.server.notification.db.NotificationMessageDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageLineDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageSectionDTO
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDAS
import com.sapienter.jbilling.server.notification.db.NotificationMessageTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.PreferenceTypeWS
import com.sapienter.jbilling.server.util.PreferenceWS
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS
import com.sapienter.jbilling.server.util.db.LanguageDTO
import com.sapienter.jbilling.server.util.db.NotificationCategoryDAS
import com.sapienter.jbilling.server.util.db.NotificationCategoryDTO
import com.sapienter.jbilling.server.util.db.PreferenceDTO
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

@Secured(["MENU_99"])
class NotificationsController {

    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    static final viewColumnsToFields = [    'categoryId': 'id',
                                        'notificationId': 'id']

	static scope = "prototype"

    IWebServicesSessionBean webServicesSession
    def breadcrumbService
    def viewUtils

	def index () {
        flash.invalidToken = flash.invalidToken
        redirect(action: 'list')
    }

    def preferences () {
        render template: "preferences", model: [subList: getPreferenceMapByTypeId()]
    }

    def list () {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        log.debug "METHOD: list\nId=${params.id} selectedId= ${params.selectedId}"

        Integer categoryId = params.int('id')
        def category = categoryId ? NotificationCategoryDTO.get(categoryId) : null
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, categoryId)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'] as Integer, Constants.PREFERENCE_USE_JQGRID);
        boolean isFromConfiguration = true

        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            if (params.template || params.partial) {
                render template: 'notificationsTemplate', model: [categoryId: categoryId, isFromConfiguration: isFromConfiguration]
            } else {
                render(view: 'list', model: [selected: categoryId, isFromConfiguration: isFromConfiguration])
            }
            return
        }

        def categories = getCategories(params)
        def notifications = category ? getNotifications(categoryId, params) : null

        if (!(categoryId) && params.partial) {
            render template: 'categoriesTemplate',
                      model: [     selected: categoryId,
                                        lst: categories,
                              lstByCategory: notifications]
        } else if (params.template || params.partial) {
            render template: 'notificationsTemplate',
                      model: [lstByCategory: notifications,
                                   selected: categoryId]
        } else {
            render(  view: 'list',
                    model: [           selected: categoryId,
                                            lst: categories,
                                  lstByCategory: notifications,
                            isFromConfiguration: isFromConfiguration])
        }
    }

    def findCategories () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def prefs = getCategories(params)

        try {
            def jsonData = getNotificationJsonData(prefs, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def findNotifications () {
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        Integer categoryId = params.int('id')
        def prefs = getNotifications(categoryId, params)

        try {
            def jsonData = getNotificationJsonData(prefs, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts Notification Categories or Notification Message to JSon
     */
    private def Object getNotificationJsonData(categories, GrailsParameterMap params) {
        def jsonCells = categories
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    private def getCategories(GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        def languageId = session['language_id']

        return NotificationCategoryDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ){
            if (params.categoryId){
                def searchParam = params.categoryId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_NOTIFICATION_CATEGORY, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    private def getNotifications(Integer categoryId, GrailsParameterMap params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        def languageId = session['language_id']

        return NotificationMessageTypeDTO.createCriteria().list(
                max: params.max,
                offset: params.offset
        ){
            eq('category.id', categoryId)
            if (params.notificationId){
                def searchParam = params.notificationId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    searchParam = searchParam.toLowerCase()
                    sqlRestriction(
                            """ exists (
                                            select a.foreign_id
                                            from international_description a
                                            where a.foreign_id = {alias}.id
                                            and a.table_id =
                                             (select b.id from jbilling_table b where b.name = ? )
                                            and a.language_id = ?
                                            and lower(a.content) like ?
                                        )
                                    """, [Constants.TABLE_NOTIFICATION_MESSAGE_TYPE, languageId, "%" + searchParam + "%"]
                    )
                }
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def show () {
        log.debug "METHOD: show"
        log.debug "Id is=" + params.id
        Integer messageTypeId = params.id.toInteger()

        Integer _languageId = StringUtils.isNotEmpty(params.get('languageId')) ? params.get('languageId')?.toInteger() : null
        if (params.get('language.id')) {
            log.debug "params.language.id is not null= " + params.get('language.id')
            _languageId = params.get('language.id')?.toInteger()
            log.debug "setting language id from requrest= " + _languageId
        }

        Integer entityId = webServicesSession.getCallerCompanyId();

        NotificationMessageTypeDTO typeDto = NotificationMessageTypeDTO.findById(messageTypeId)
        typeDto.getNotificationMessages().removeIf { it -> it.entity.id != entityId }
        LanguageDTO languageDTO = LanguageDTO.findById(_languageId)
        if(_languageId) {
            def dto = typeDto.getNotificationMessages().find { it -> it.language.id == _languageId }
            typeDto.getNotificationMessages().clear()
            typeDto.getNotificationMessages().add(dto ?: new NotificationMessageDTO(language: languageDTO))
        }

        if (params.template) {
            // render requested template, usually "_show.gsp"
            render template: "show", model: [      typeDto: typeDto,
                                             messageTypeId: messageTypeId,
                                               languageDto: languageDTO,
                                                  entityId: entityId]
        } else {
            Integer categoryId = typeDto?.getCategory()?.getId()
            def category = categoryId ? NotificationCategoryDTO.get(categoryId) : null
            def categories = getCategories(params)
            def notifications = category ? getNotifications(categoryId, params) : null

            render(view: 'list', model: [selectedNotification:typeDto.id,
                                                     selected: categoryId,
                                                          lst: categories,
                                                lstByCategory: notifications,
                                                      typeDto: typeDto,
                                                messageTypeId: messageTypeId,
                                                  languageDto: languageDTO,
                                                     entityId: entityId])
        }

    }

    private getPreferenceMapByTypeId() {
        Map<PreferenceDTO> subList = new HashMap<PreferenceDTO>();
        List<PreferenceDTO> masterList = PreferenceDTO.findAllByForeignId(webServicesSession.getCallerCompanyId())
        log.debug "masterList.size=" + masterList.size()
        for (PreferenceDTO dto : masterList) {
            Integer prefid = dto.getPreferenceType().getId()
            switch (prefid) {
                case Constants.PREFERENCE_TYPE_SELF_DELIVER_PAPER_INVOICES:
                case Constants.PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES:
                case Constants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP:
                case Constants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP2:
                case Constants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP3:
                case Constants.PREFERENCE_TYPE_USE_INVOICE_REMINDERS:
                case Constants.PREFERENCE_TYPE_NO_OF_DAYS_INVOICE_GEN_1_REMINDER:
                case Constants.PREFERENCE_TYPE_NO_OF_DAYS_NEXT_REMINDER:
                    log.debug "Adding dto: " + dto.getPreferenceType().getId()
                    subList.put(dto.getPreferenceType().getId(), dto)
                    break;
            }
        }
        subList
    }

    def cancelEditPrefs () {
        render view: "viewPrefs", model: [lst: NotificationCategoryDTO.list(), subList: getPreferenceMapByTypeId()]
    }

    def editPreferences () {

        Map<PreferenceDTO> subList = new HashMap<PreferenceDTO>();
        List<PreferenceDTO> masterList = PreferenceDTO.findAllByForeignId(webServicesSession.getCallerCompanyId())
        log.debug "masterList.size=" + masterList.size()
        for (PreferenceDTO dto : masterList) {
            Integer prefid = dto.getPreferenceType().getId()
            switch (prefid) {
                case Constants.PREFERENCE_TYPE_SELF_DELIVER_PAPER_INVOICES:
                case Constants.PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES:
                case Constants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP:
                case Constants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP2:
                case Constants.PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP3:
                case Constants.PREFERENCE_TYPE_USE_INVOICE_REMINDERS:
                case Constants.PREFERENCE_TYPE_NO_OF_DAYS_INVOICE_GEN_1_REMINDER:
                case Constants.PREFERENCE_TYPE_NO_OF_DAYS_NEXT_REMINDER:
                    log.debug "Adding dto: " + dto.getPreferenceType().getId()
                    subList.put(dto.getPreferenceType().getId(), dto)
                    break;
            }
        }
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        [subList: subList, languageId: session['language_id']]
    }

    @RequiresValidFormToken
    def savePrefs () {
        log.debug "pref[5].value=" + params.get("pref[5].value")
        List<PreferenceWS> prefDTOs

        try {
            prefDTOs = bindDTOs(params)
        } catch (SessionInternalError e) {
            viewUtils.resolveExceptionForValidation(flash, session.locale, e);
            redirect action: "editPreferences"
        }
        log.debug "Calling: webServicesSession.saveNotificationPreferences(prefDTOs); List Size: " + prefDTOs.size()
        PreferenceWS[] array = new PreferenceWS[prefDTOs.size()]
        array = prefDTOs.toArray(array)
        try {
            webServicesSession.updatePreferences(array)
        } catch (SessionInternalError e) {
            log.error "Error: " + e.getMessage()
            flash.errorMessages = e.getErrorMessages()
            //boolean retValue = viewUtils.resolveExceptionForValidation(flash, session.'org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE', e);
        }
        log.debug "Finished: webServicesSession.saveNotificationPreferences(prefDTOs);"
        if (flash.errorMessages?.size() > 0) {
            redirect(action: 'editPreferences')
        } else {
            flash.message = 'preference.saved.success'
            redirect(action: 'cancelEditPrefs')
        }
    }


    def List<PreferenceWS> bindDTOs(params) {
        log.debug "bindDTOs"
        List<PreferenceWS> prefDTOs = new ArrayList<PreferenceWS>();

        def count = params.recCnt.toInteger()

        for (int i = 0; i < count; i++) {
            log.debug "loop=" + params.get("pref[" + i + "].id")
            PreferenceWS dto = new PreferenceWS()
            dto.setPreferenceType(new PreferenceTypeWS())

            dto.setForeignId(webServicesSession.getCallerCompanyId())

            bindData(dto, params["pref[" + i + "]"])

            switch (i) {
                case 0:
                case 1:
                case 5:
                    if (params["pref[" + i + "].value"]) {
                        dto.setValue("1")
                    } else {
                        dto.setValue("0")
                    }
                    break;
                default:
                    if (params["pref[" + i + "].value"]) {
                        def val = params["pref[" + i + "].value"]
                        try {
                            Integer value = val.toInteger()
                            dto.setValue(value?.toString())
                        } catch (NumberFormatException e) {
                            String[] errmsgs = new String[1]
                            errmsgs[0] = "PreferenceWS,intValue,validation.error.nonnumeric.days.order.notification," + val;
                            throw new SessionInternalError("Validation of Preference Value", errmsgs);
                        }
                    } else {
                        dto.setValue("0")
                    }
            }
            log.debug "dto.intValue=" + dto.value
            prefDTOs.add(dto);
        }
        return prefDTOs;
    }

    def edit () {
        log.debug "METHOD: edit"

        //set cookies here..
        log.debug("doNotAskAgain=" + params.doNotAskAgain + " askPreference=" + params.askPreference)

        def askPreference = request.getCookie("doNotAskAgain")
        log.debug("Cooke set to was=" + askPreference)
        if ("true".equals(params.doNotAskAgain)) {
            response.setCookie('doNotAskAgain', String.valueOf(params.askPreference), 604800)
            log.debug("Setting the cookie to value ${params.askPreference}")
            askPreference = params.askPreference
        }

        log.debug "Id is=" + params.id
        Integer messageTypeId = params.id?.toInteger()

        if (!messageTypeId) {
            redirect action: 'list'
        }

        Integer _languageId = params.get('languageId')?.toInteger() ?: session['language_id']
        if (params.get('language.id')) {
            log.debug "Param 'language.id' is Not Null [${params.get('language.id')}]"
            _languageId = params.get('language.id')?.toInteger()
        }
        Integer entityId = webServicesSession.getCallerCompanyId()?.toInteger()

        log.debug "Language Id Set to ${_languageId}, Entity ${entityId}, askPreference= ${askPreference}"

        NotificationMessageTypeDTO typeDto = messageTypeId ? NotificationMessageTypeDTO.findById(messageTypeId) : null
        NotificationMessageDTO dto = null
        for (NotificationMessageDTO messageDTO : typeDto?.getNotificationMessages()) {
            if (messageDTO?.getEntity()?.getId().equals(entityId)
                    && messageDTO?.getLanguage()?.getId().equals(_languageId)) {
                dto = messageDTO;
                break;
            }
        }
        // If notification under category "Order" then show order specific tokens
        def isOrderTypeNotification = typeDto?.category.equals(NotificationMessageTypeDTO.findById(Constants.NOTIFICATION_TYPE_ORDER_EXPIRE_1).category)
        def isPaymentTypeNotification = typeDto?.category.equals(NotificationMessageTypeDTO.findById(Constants.NOTIFICATION_TYPE_PAYMENT_FAILED).category)
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, messageTypeId)

        [dto: dto, messageTypeId: messageTypeId, isOrderTypeNotification: isOrderTypeNotification, isPaymentTypeNotification: isPaymentTypeNotification, languageId: _languageId, entityId: entityId, askPreference: askPreference, includeAttachment: params.includeAttachment]
    }

    def saveAndRedirect () {
        log.debug "METHOD: saveAndRedirect"
        try {
            saveAction(params)
        } catch (SessionInternalError e) {
            log.error "Error: " + e.getMessage()
            flash.error = "error.illegal.modification"
        }
        redirect(action: 'edit', params: params)
    }

    @RequiresValidFormToken
    def saveNotification () {

        log.debug "METHOD: saveNotification"
        try {
            saveAction(params)
        } catch (SessionInternalError e) {
            log.error "Error: " + e.getMessage()
            viewUtils.resolveException(flash, session.locale, e)
            flash.error = flash.errorMessages[0]
            flash.errorMessages = null
        }

        if (params.id) {
            if (flash.error) {
                render view: 'edit', model: failSave(params)
            } else {
                redirect(action: 'cancelEdit', params: [id: params.id])
            }
        } else {
            redirect(action: 'list')
        }
    }

    def saveAction(params) {
        log.debug "METHOD: saveAction\nAll params\n${params}"

        NotificationMessageDTO msgDTO = new NotificationMessageDTO()
        msgDTO.setLanguage(new LanguageDTO())
        msgDTO.setEntity(new CompanyDTO())
        params.includeAttachment = params.includeAttachment == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        log.debug("binding data with params ${params}")
        bindData(msgDTO, params)

        def _id = null;
        if (params.id) {
            _id = params.id?.toInteger()
            msgDTO.setId(_id)
        }

        log.debug "useFlag: '${params.useFlag}', NotificationMessageDTO.useFlag=${msgDTO.getUseFlag()}"
        if ('on' == params.useFlag) {
            msgDTO.setUseFlag((short) 1)
        } else {
            msgDTO.setUseFlag((short) 0)
        }

        log.debug "NotificationMessageType ID=${_id}, Entity=${params.get('entity.id')?.toInteger()}, Language = ${params._languageId}"
        MessageDTO messageDTO = new MessageDTO()

        if (params.id) {
            messageDTO.setTypeId(_id)
        } else {
            messageDTO.setTypeId(saveNotificationMessageType(params))
        }
        messageDTO.setLanguageId(params.get('_languageId')?.toInteger())
        messageDTO.setUseFlag(1 == msgDTO.getUseFlag())
        messageDTO.setContent(bindSections(params))
        //setting additional params
        messageDTO.setAttachmentDesign(params.attachmentDesign)
        messageDTO.setAttachmentType(params.attachmentType)
        messageDTO.setIncludeAttachment(params.includeAttachment)

        Integer entityId = params.get('entity.id')?.toInteger()
        Integer messageId = null;
        if (params.msgDTOId) {
            messageId = params.msgDTOId.toInteger()
        } else {
            //new record
            messageId = null;
        }

        log.debug "msgDTO.language.id=" + messageDTO?.getLanguageId()
        log.debug "msgDTO.type.id=" + messageDTO?.getTypeId()
        log.debug "msgDTO.use.flag=" + messageDTO.getUseFlag()

        if (params.notifyAdmin) {
            messageDTO.setNotifyAdmin(1)
        } else {
            messageDTO.setNotifyAdmin(0)
        }

        if (params.notifyPartner) {
            messageDTO.setNotifyPartner(1)
        } else {
            messageDTO.setNotifyPartner(0)
        }

        if (params.notifyParent) {
            messageDTO.setNotifyParent(1)
        } else {
            messageDTO.setNotifyParent(0)
        }

        if (params.notifyAllParents) {
            messageDTO.setNotifyAllParents(1)
        } else {
            messageDTO.setNotifyAllParents(0)
        }
        messageDTO.setMediumTypes(msgDTO.getMediumTypes());
        log.debug "EntityId = ${entityId?.intValue()}, callerCompanyId= ${webServicesSession.getCallerCompanyId()?.intValue()}"
        if (entityId?.intValue() == webServicesSession.getCallerCompanyId()?.intValue()) {
            log.debug "Calling createUpdateNotifications..."
            try {
                webServicesSession.createUpdateNotification(messageId, messageDTO)
                flash.message = 'notification.save.success'
            } catch (Exception e) {
                log.error("ERROR: " + e.getMessage())
                throw new SessionInternalError(e)
            }
        } else {
            log.error("ERROR: Entity Idis do not match.")
            throw new SessionInternalError("Cannot update another company data.")
        }
    }

    def MessageSection[] bindSections(params) {
        log.debug "METHOD: bindSections"
        MessageSection[] lines = new MessageSection[3];
        Integer section = null;
        String content = null;
        MessageSection obj = null;

        for (int i = 1; i <= 3; i++) {
            log.debug "messageSections[" + i + "].section=" + params.get("messageSections[" + i + "].section")
            log.debug "messageSections[" + i + "].id=" + params.get("messageSections[" + i + "].id")

            if (params.get("messageSections[" + i + "].notificationMessageLines.content")) {
                content = params.get("messageSections[" + i + "].notificationMessageLines.content")
                obj = new MessageSection(i, content)
            } else {
                obj = new MessageSection(i, "")
            }
            lines[(i - 1)] = obj;
        }
        log.debug "Line 1= " + lines[0]
        log.debug "Line 2= " + lines[1]
        log.debug "Line 3= " + lines[2]
        return lines;
    }

    def saveAndCancel () {
        log.debug "METHOD: saveAndCancel"
        try {
            saveAction(params)
        } catch (SessionInternalError e) {
            log.error "Error: " + e.getMessage()
            flash.error = "error.illegal.modification"
        }
        redirect(action: 'cancelEdit', params: params)
    }

    def editCategory () {
        if (!params.categoryId && !params.boolean('add')) {
            flash.error = 'notification.category.not.selected'
            flash.args = [params.categoryId as String]

            redirect(controller: 'notifications', action: 'list')
            return
        } else {
            def category
            try {
                category = params.categoryId ? new NotificationCategoryDAS().find(params.int('categoryId')) : null
            } catch (SessionInternalError e) {
                log.error("Could not fetch object", e)

                flash.error = 'notification.category.not.found'
                flash.args = [params.categoryId as String]

                redirect controller: 'notifications', action: 'list'
                return
            }

            [category: category]
        }
    }

    def cancelEdit () {
        log.debug "METHOD: cancelEdit\nid=${params.id}"
        if (!params.id) {
            redirect(action: 'list')
            return
        }

        NotificationMessageTypeDTO typeDto = NotificationMessageTypeDTO.findById(Integer.parseInt(params["id"]))
        chain action: 'show', params: [id: typeDto.getId(), selected:typeDto.getCategory().getId()]

    }

    /**
     * Validate and save a category.
     */
    @RequiresValidFormToken
    def saveCategory () {
        try {
            NotificationCategoryDTO nc
            if (params?.id) {
                nc = new NotificationCategoryDAS().find(params.int('id'))
            } else {
                nc = new NotificationCategoryDTO()
            }

            if (params?.description) {
                createUpdateNotificationCategory(nc, params?.description, session['language_id'])
                flash.message = 'notification.category.save.success'
                redirect(action: 'list')
            } else {
                flash.error = 'Description is required'
                redirect(action: 'editCategory')
            }
        } catch (Exception e) {
            log.error("ERROR: " + e.getMessage())
            throw new SessionInternalError(e)
        }
    }

    /**
     * Validate and save a category.
     */
    def saveCategory1 () {
        try {
            def das = new NotificationCategoryDAS()
            def catDTO
            if (params?.id) {
                catDTO = das.find(params.int('id'))
            } else {
                catDTO = new NotificationCategoryDTO()
            }

            log.debug "CATDescription ${params.description}"
            log.debug "IS ${catDTO.id} - ${!catDTO.id}"
            if (params?.description) {
                if (!catDTO.id) {
                    catDTO = das.save(catDTO)
                }
                log.debug "Category id = ${catDTO.id}"
                log.debug "Table name is ${Constants.TABLE_NOTIFICATION_CATEGORY}"
                new InternationalDescriptionDAS().create(Constants.TABLE_NOTIFICATION_CATEGORY, catDTO.id, "description", session['language_id'], params.description)
                catDTO = das.get(catDTO.id)
                flash.message = 'notification.category.save.success'
                redirect(action: 'list')
            } else {
                flash.error = 'Description is required'
                redirect(action: 'editCategory')
            }
        } catch (Exception e) {
            e.printStackTrace()
            log.error("ERROR: " + e.getMessage())
            throw new SessionInternalError(e)
        }
    }

    def editNotification () {
        if (params?.categoryId != null) {
            int categoryId = Integer.parseInt(params?.categoryId)
            def category = new NotificationCategoryDAS().find(categoryId)

            render template: 'editNotification', model: [category: category, selectedCategoryId: params.categoryId,
                                                         notificationMessageType: new NotificationMessageTypeDTO()]
        } else {
            flash.error = "Category is not selected"
            redirect(action: 'list')
        }
    }

    @RequiresValidFormToken
    def saveNotificationMessage () {
        if (saveNotificationMessageType(params)) {
            redirect action: 'list', params: [id: params.categoryId]
        }
    }

    Integer saveNotificationMessageType(params) {
        NotificationCategoryDTO notificationCategory = null

        if (!params.categoryId) {
            log.error("Category not selected.")
            flash.error = "Category not selected."
            return 1
        } else {
            notificationCategory = new NotificationCategoryDAS().find(params.int('categoryId'))
        }

        if(!params.count {key, value -> key.contains('].content') && !value.isEmpty()}) {
            flash.error = "Description not inserted"
            return 1
        }

        NotificationMessageTypeDTO notificationMessageType = new NotificationMessageTypeDTO()
        return createUpdateNotificationMessageType(notificationMessageType, notificationCategory, params)
    }

    Integer createUpdateNotificationCategory(def notificationCategoryDTO, String description, Integer language_id) {
        NotificationCategoryDAS notificationCategoryDAS = new NotificationCategoryDAS();
        notificationCategoryDTO = notificationCategoryDAS.save(notificationCategoryDTO);
        notificationCategoryDAS.flush();

        notificationCategoryDTO.setDescription(description, language_id);
        notificationCategoryDTO = notificationCategoryDAS.save(notificationCategoryDTO);
        new NotificationMessageTypeDAS().flush();

        log.debug("Notification category saved successfully" + notificationCategoryDTO.getId());
        return notificationCategoryDTO.getId();
    }

    void deleteNotificationCategory(def notificationCategoryDTO) {
        new NotificationCategoryDAS().delete(notificationCategoryDTO);
        log.debug("Notification category deleted successfully");
    }

    Integer createUpdateNotificationMessageType(def notificationMessageType, def notificationCategory, def params) {
        notificationMessageType.setCategory(notificationCategory);
        notificationMessageType = new NotificationMessageTypeDAS().save(notificationMessageType);
        new NotificationMessageTypeDAS().flush();

        for (int i = 0; i < params['allDescriptionLanguages'].split(',').size(); i++){
            if(params['notification.description[' + i + '].content'] && params['notification.description[' + i + '].languageId']) {
                notificationMessageType.setDescription(params['notification.description[' + i + '].content'],
                                                       params.int('notification.description[' + i + '].languageId'));
            }
        }

        notificationMessageType = new NotificationMessageTypeDAS().save(notificationMessageType);
        new NotificationMessageTypeDAS().flush();

        log.debug("Notification message type saved successfully" + notificationMessageType.getId());
        flash.message = 'Notification Saved Successfully'
        return notificationMessageType.getId();
    }

    void deleteNotificationMessageType(def notificationMessageTypeDTO) {
        new NotificationMessageTypeDAS().delete(notificationMessageTypeDTO);
        log.debug("Notification message type deleted successfully");
    }

    void deleteNotificationMessage(def notificationMessage) {
        new NotificationMessageDAS().delete(notificationMessage);
        log.debug("Notification message deleted successfully");
    }

    def failSave(params){
        NotificationMessageTypeDTO typeDto = params.get('id') ? NotificationMessageTypeDTO.findById(params.get('id')) : null
        NotificationMessageDTO dto = null
        for (NotificationMessageDTO messageDTO : typeDto?.getNotificationMessages()) {
            if (messageDTO?.getEntity()?.getId().equals(params.int('entity.id'))
                    && messageDTO?.getLanguage()?.getId().equals(params.int('language.id'))) {
                dto = messageDTO;
                break;
            }
        }

        params.includeAttachment = params.includeAttachment == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        params.useFlag = params.useFlag == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        params.notifyAdmin = params.notifyAdmin == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        params.notifyPartner = params.notifyPartner == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        params.notifyParent = params.notifyParent == "on" ? Integer.valueOf(1) : Integer.valueOf(0)
        params.notifyAllParents = params.notifyAllParents == "on" ? Integer.valueOf(1) : Integer.valueOf(0)

        bindData(dto, params)
        dto.setNotificationMessageSections(bindMessageSection(dto, params))

        // If notification under category "Order" then show order specific tokens
        def isOrderTypeNotification = typeDto?.category.equals(NotificationMessageTypeDTO.findById(Constants.NOTIFICATION_TYPE_ORDER_EXPIRE_1).category)
        def isPaymentTypeNotification = typeDto?.category.equals(NotificationMessageTypeDTO.findById(Constants.NOTIFICATION_TYPE_PAYMENT_FAILED).category)

        return [                      dto : dto,
                            messageTypeId : params.get('id'),
                  isOrderTypeNotification : isOrderTypeNotification,
                isPaymentTypeNotification : isPaymentTypeNotification,
                               languageId : params.int('language.id'),
                                 entityId : params.int('entity.id'),
                            askPreference : request.getCookie("doNotAskAgain"),
                        includeAttachment : params.includeAttachment]
    }

    Set<NotificationMessageSectionDTO> bindMessageSection(NotificationMessageDTO dto, GrailsParameterMap params){
        Set<NotificationMessageSectionDTO> notificationMessageSection = new HashSet<>()
        NotificationMessageSectionDTO notificationMessageSectionDTO = null;
        NotificationMessageLineDTO notificationMessageLineDTO = null;

        for (int i = 1; i <= 3; i++) {
            notificationMessageSectionDTO = new NotificationMessageSectionDTO()
            notificationMessageLineDTO = new NotificationMessageLineDTO();

            log.debug "messageSections[" + i + "].section=" + params.get("messageSections[" + i + "].section")
            log.debug "messageSections[" + i + "].id=" + params.get("messageSections[" + i + "].id")

            if (params.get("messageSections[" + i + "].notificationMessageLines.content")) {
                notificationMessageLineDTO.setContent(params.get("messageSections[" + i + "].notificationMessageLines.content"))
            } else {
                notificationMessageLineDTO.setContent("")
            }

            notificationMessageSectionDTO.setSection(i);
            notificationMessageSectionDTO.getNotificationMessageLines().add(notificationMessageLineDTO);
            notificationMessageSection.add(notificationMessageSectionDTO);

        }

        return notificationMessageSection
    }
}
