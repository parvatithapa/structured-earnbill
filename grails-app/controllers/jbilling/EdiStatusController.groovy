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

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.ediTransaction.EDIFileExceptionCodeWS
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDTO
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap


@Secured(["isAuthenticated()"])
class EdiStatusController {

    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    def breadcrumbService
    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils
    def filterService

    def index (){
        redirect action: 'list', params: params
    }

    def list (){

        def filters = filterService.getFilters(FilterType.EDI_TYPE, params)

        params.typeId=params.typeId?:params.id
        EDIFileStatusDTO selected = params.id ? EDIFileStatusDTO.get(params.int("id")) : null
        // if id is present and invoice not found, give an error message along with the list
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, params.int("typeId"))
        List<EDIFileStatusDTO> ediFileStatusDTOList= getFilteredEDIStatus(filters, params)
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (params.applyFilter || params.partial) {
            render template: 'ediStatusTemplate', model: [ selected: selected, ediStatusList: ediFileStatusDTOList,filters: filters]
        }else if(params.showEdit) {
            def ediFileStatus = params.id ? EDIFileStatusDTO.get(params.int('id')): new EDIFileStatusDTO()
            Set<EDIFileStatusDTO> ediFileStatusDTOs=EDITypeDTO.get(params.typeId)?.statuses
            if (ediFileStatusDTOs){
                ediFileStatusDTOs=ediFileStatusDTOs-ediFileStatus
            }
            render view: 'list', model: [ediStatusList: ediFileStatusDTOList, ediStatus: ediFileStatus, childEdiFileStatus:ediFileStatusDTOs]
        } else {
            render view: 'list', model: [selected: selected, ediStatusList: ediFileStatusDTOList,filters: filters,filterId: params.typeId]
        }
    }

    def getFilteredEDIStatus(filters, params){
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        Integer ediTypeId=params.int("typeId")

        EDITypeDTO ediTypeDTO= EDITypeDTO.get(params.int("typeId"))
        def company_id = session['company_id']

        if(ediTypeDTO?.getStatuses()?.size()>0){
            return EDIFileStatusDTO.createCriteria().list(
                    max:    params.max,
                    offset: params.offset

            ) {
                inList("id", ediTypeDTO?.getStatuses()?.id)
                filters.each { filter ->
                    if (filter.value != null) {
                        addToCriteria(filter.getRestrictions());
                    }
                }
                // apply sorting
                SortableCriteria.sort(params, delegate)
            }
        }
        return null
    }

    def show (){

        EDIFileStatusDTO fileStatusDTO=EDIFileStatusDTO.get(params.int('id'))
        render template: 'show', model: [ediFileStatus: fileStatusDTO]
    }

    def edit (){
        def ediFileStatus = params.id ? EDIFileStatusDTO.get(params.int('id')) : new EDIFileStatusDTO()
        Set<EDIFileStatusDTO> ediFileStatusDTOs=EDITypeDTO.get(params.typeId)?.statuses
        if (ediFileStatusDTOs){
            ediFileStatusDTOs=ediFileStatusDTOs-ediFileStatus
        }
        render template: 'edit', model: [ediStatus: ediFileStatus, childEdiFileStatus:ediFileStatusDTOs]
    }

    def save (){
        List<Integer> exceptionCodeIds = params.list("exceptionCode.id")
        List<String> exceptionCodes = params.list("exceptionCode.code")
        List<String> exceptionCodeDescription = params.list("exceptionCode.description")
        List<Integer> childStatusIds = params.list("childStatusIds")

        EDIFileStatusWS ediFileStatusWS = new EDIFileStatusWS();
        bindData(ediFileStatusWS, params)

        ediFileStatusWS.setCreateDatetime(TimezoneHelper.serverCurrentDate())
        if (params.int("id")){
            ediFileStatusWS.id=params.int("id")
        }

        if (childStatusIds){
            childStatusIds.each {

                ediFileStatusWS.getChildStatuesIds().add(it as Integer)
            }
        }
        exceptionCodes.eachWithIndex{ String code, int index ->
            if(code){
                EDIFileExceptionCodeWS ediFileExceptionCodeWS = new EDIFileExceptionCodeWS(code:code, ediFileStatusWS: ediFileStatusWS)
                if(exceptionCodeIds.get(index)){
                    ediFileExceptionCodeWS.setId(exceptionCodeIds.get(index) as Integer)
                }
                if(exceptionCodeDescription.get(index)){
                    ediFileExceptionCodeWS.setDescription(exceptionCodeDescription.get(index))
                }
                ediFileStatusWS.getExceptionCodes().add(ediFileExceptionCodeWS);
            }

        }


        try {
            def id = webServicesSession.createUpdateEdiStatus(ediFileStatusWS);
            if (params?.isNew?.equals('true')) {
                flash.message = 'edi.status.created'
                flash.args = [id]
            } else {
                flash.message = 'edi.status.updated'
                flash.args = [id]
            }
            redirect action: 'list', params: [typeId:params.typeId,id:id]
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            redirect action: 'list', params: [showEdit: true, typeId: params.typeId, id:params.id]
        }
    }

}