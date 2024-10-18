package jbilling

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.ediTransaction.EDIFileBL
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusBL
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS
import com.sapienter.jbilling.server.ediTransaction.EDITypeWS
import com.sapienter.jbilling.server.ediTransaction.TransactionType
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileRecordDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDTO
import com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask
import com.sapienter.jbilling.server.fileProcessing.FileConstants
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.plugin.springsecurity.annotation.Secured

@Secured(["MENU_903"])
class EdiFileController {

    static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [ max: 25 ]
    def messageSource

    IWebServicesSessionBean webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def companyService
    SecurityValidator securityValidator

    def index () {
        list()
    }

    def showOrderEDIFile() {
        String ediFileId = null
        OrderWS order = webServicesSession.getOrder(params.int('orderId'))
        if (!order) {
            flash.error = message(code: 'order.not.exist')
        } else {
            securityValidator.validateUserAndCompany(order, Validator.Type.VIEW)
            MetaFieldValueWS[] metaFieldValueWSes = order.getMetaFields();
            for (MetaFieldValueWS field : metaFieldValueWSes) {
                if (field.getFieldName().equals(MeterReadParserTask.MeterReadField.edi_file_id.toString())) {
                    ediFileId = field.getValue()
                    break
                }
            }
            if (!ediFileId) {
                flash.error = message(code: 'edi.file.not.found')
                redirect(controller: 'order', action: 'list', params: [id: params.int('orderId')])
                return
            }
        }
        redirect(action: list(), params: [ediFileId: ediFileId])
    }

    def list () {
        def filters = filterService.getFilters(FilterType.EDI_FILE, params)

        Integer fileId=params.int("fileId")
        EDIFileDTO selected
        if(fileId){
            selected = EDIFileDTO.get(params.int("fileId"))
            securityValidator.validateCompany(selected?.entity?.id, Validator.Type.VIEW)
        }
        // if id is present and invoice not found, give an error message along with the list
        params.typeId=params.typeId?:params?.id
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, params.int("typeId"))
        List<EDIFileDTO> ediFileList= getFilteredEDIFiles(filters, params)
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (params.applyFilter || params.partial) {
            render template: 'ediFilesTemplate', model: [ selected: selected, ediFiles: ediFileList,filters: filters]
        } else {
            List<EDIFileRecordDTO> ediFileRecordDTOList=getFilteredEDIFileRecords(selected?.id, null)
            session["ediType"]=ediFileList? ediFileList.first().ediType:null
            if(session["ediType"] && session["ediType"].statuses) session["ediType"].statuses.size()
            render view: 'list', model: [selected: selected, ediFiles: ediFileList,filters: filters, ediFileRecords:ediFileRecordDTOList, filterId: params.typeId]
        }
    }

    def getEdiRecord () {
        EDIFileRecordDTO ediFileRecordDTO = EDIFileRecordDTO.get(params.int("id"))
        securityValidator.validateCompany(ediFileRecordDTO?.ediFile?.entity?.id, Validator.Type.VIEW)
        render template:'ediFileRecordShow' , model: [ediFileRecord: ediFileRecordDTO]
        return
    }

    def getFilteredEDIFiles(filters, params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def company_id = session['company_id']
        return EDIFileDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset

        ) {
            createAlias("entity", "entity")
            createAlias("ediType", "ediType")
            createAlias("fileStatus", "fileStatus")
            and {
                if (companyService.rootCompanyId == company_id) {
                    'in'('entity.id', companyService.getEntityAndChildEntities()*.id)
                } else {
                    eq("entity.id", company_id)
                }
                if(params.ediFileId && params.ediFileId!=null){
                    eq("id", params.int("ediFileId"))
                }
                if(params.enrollmentId && params.enrollmentId!='null'){
                    ediFileRecords{
                        fileFields{
                            eq("ediFileFieldKey", com.sapienter.jbilling.server.customerEnrollment.task.CustomerEnrollmentFileGenerationTask.TRANS_REF_NR)
                            ilike("ediFileFieldValue", "%${params.enrollmentId}")
                        }
                    }
                }
                if(params.typeId && params.typeId!="null"){
                    eq('ediType.id', params.int('typeId'))
                }

                filters.each { filter ->
                    if (filter.value != null) {
                        if (filter.field == 'type') {
                            TransactionType transactionType=TransactionType.INBOUND
                            if(filter.value=="INBOUND"){
                                transactionType=TransactionType.INBOUND
                            }
                            if(filter.value=="OUTBOUND"){
                                transactionType=TransactionType.OUTBOUND
                            }
                            eq("type", transactionType)
                        }else if(filter.field == 'fileStatus.id'){
                            eq("fileStatus.id", filter.value as Integer)
                        }else{
                            addToCriteria(filter.getRestrictions());
                        }
                    }
                }
            }
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }


    def ediFiles() {
        def filters = filterService.getFilters(FilterType.EDI_FILE, params)
        if (request.isXhr()) {
            EDITypeDTO ediTypeDTO = EDITypeDTO.get(params.int("id"))
            securityValidator.validateCompany(ediTypeDTO?.entity?.id, Validator.Type.VIEW)
            List<EDIFileDTO> ediFileDTOList = getFilteredEDIFiles(ediTypeDTO?.id, filters)
            render template: '/ediFile/ediFiles', model: [ediFiles: ediFileDTOList, ediType: ediTypeDTO]
            return
        } else {
            redirect(action: 'list')
        }

    }

    def getFilteredEDIFileRecords(ediFileId, filters) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: "recordOrder"
        params.order = params?.order ?: "asc"

        return EDIFileRecordDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset

        ) {
            and {
                eq('ediFile.id', ediFileId)
            }
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    def show () {
        Integer id=params.int("id")
        if(request.isXhr()){
            EDIFileDTO ediFileDTO = EDIFileDTO.get(id)
            securityValidator.validateCompany(ediFileDTO?.entity?.id, Validator.Type.VIEW)
            List<EDIFileRecordDTO> ediFileRecordDTOList=getFilteredEDIFileRecords(ediFileDTO?.id, null)
            render template:'show' , model: [ediFile: ediFileDTO, ediFileRecords:ediFileRecordDTOList]
            return
        }else{
            redirect(action: 'list', params: [id:id])
        }
    }

    def ediFileRecordList(){

        params.max = params.max!="null" ?  params?.max?.toInteger() : pagination.max
        params.offset = params?.offset!="null"  ? params?.offset?.toInteger() : pagination.offset
        params.sort = params?.sort !="null"  ? params?.sort : pagination.sort
        params.order = params?.order !="null"  ? params?.order : pagination.order
        def filters = null
        Integer ediFileId=params.int("id")
        EDIFileDTO ediFileDTO=EDIFileDTO.get(ediFileId)
        securityValidator.validateCompany(ediFileDTO?.entity?.id, Validator.Type.VIEW)
        List<EDIFileRecordDTO> ediFileRecordDTOList= getFilteredEDIFileRecords(ediFileDTO?.id, filters)

        render template: "ediRecordList", model: [ediFileRecords:ediFileRecordDTOList,ediFile:ediFileDTO ]
    }

    def ediFileRecordShow(){
        if(request.isXhr()){
            EDIFileRecordDTO ediFileRecordDTO = EDIFileRecordDTO.get(params.int("id"))
            securityValidator.validateCompany(ediFileRecordDTO?.ediFile?.entity?.id, Validator.Type.VIEW)
            render template:'ediFileRecordShow' , model: [ediFileRecord: ediFileRecordDTO]
            return
        }
    }

    def download() {

        Integer id = params.int("id")
        EDIFileDTO fileDTO = EDIFileDTO.get(id)
        securityValidator.validateCompany(fileDTO?.entity?.id, Validator.Type.VIEW)
        EDITypeWS ediTypeWS = webServicesSession.getEDIType(fileDTO.ediType?.id)


        def file = new File(FileConstants.getEDITypePath(ediTypeWS.getEntityId(), ediTypeWS.getPath(), fileDTO.type.toString().toLowerCase()) + File.separator +  "${fileDTO.name}")

        if (file.exists()) {
            response.setContentType("application/octet-stream")
            response.setCharacterEncoding("UTF-8")
            response.setHeader("Content-disposition", "attachment;filename=\"${file.name}\"")
            response.outputStream << file.bytes
        } else {
            flash.error = message(code: "edi.file.file.download.fail")
            chain action: "list", params: [typeId:fileDTO?.ediType?.id]
        }
    }

    def updateFileStatus(){
        Integer fileId=params.int("id")
        EDIFileDTO ediFileDTO=EDIFileDTO.get(fileId)
        securityValidator.validateCompany(ediFileDTO?.entity?.id, Validator.Type.VIEW)
        EDIFileWS ediFileWS=new EDIFileBL(ediFileDTO).getWS()
        try{
            ediFileWS.setExceptionCode(null)
            EDIFileStatusDTO ediFileStatusDTO=EDIFileStatusDTO.get(params.int("ediFileStatusId"))
            EDIFileStatusWS ediFileStatusWS=new EDIFileStatusBL().getWS(ediFileStatusDTO)
            webServicesSession.updateEDIStatus(ediFileWS, ediFileStatusWS, true)
            flash.message=message(code:"edi.file.status.successfully.updates")
        }catch (Exception e){
            viewUtils.resolveException(flash, session.locale, e)
        }

        redirect(action: "list", params: [fileId:fileId, typeId:ediFileDTO.getEdiType().id])
    }

}
