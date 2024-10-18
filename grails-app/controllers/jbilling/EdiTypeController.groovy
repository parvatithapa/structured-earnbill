package jbilling

import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.customerEnrollment.event.EnrollmentCompletionEvent
import com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS
import com.sapienter.jbilling.server.ediTransaction.EDIFileStatusWS
import com.sapienter.jbilling.server.ediTransaction.EDITypeWS
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO
import com.sapienter.jbilling.server.ediTransaction.TransactionType
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileExceptionCodeDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDTO
import com.sapienter.jbilling.server.fileProcessing.FileConstants
import com.sapienter.jbilling.server.fileProcessing.fileGenerator.FlatFileGenerator
import com.sapienter.jbilling.server.fileProcessing.fileParser.FlatFileParser
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat
import com.sapienter.jbilling.server.system.event.EventManager
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.io.FilenameUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.Criteria
import org.hibernate.criterion.CriteriaSpecification

@Secured(["MENU_903"])
class EdiTypeController {

    static scope = "prototype"
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']
    static versions = [max: 25]
    def messageSource

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['id'         : 'id',
             'name'       : 'name',
             'path'       : 'path',
             'company'    : 'company.description',
             'dateCreated': 'createDatetime']

    IWebServicesSessionBean webServicesSession
    IEDITransactionBean transactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def companyService

    def index() {
        list()
    }

    def list() {
        def filters = filterService.getFilters(FilterType.EDI_TYPE, params)
        def selected = params.id ? EDITypeDTO.get(params.int("id")) : null
        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id)
        // if id is present and invoice not found, give an error message along with the list

        List<EDITypeDTO> ediTypeDTOList = getFilteredEDITypes(filters, params)
        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders

        if (params.applyFilter || params.partial) {
            render template: 'ediTypesTemplate', model: [selected: selected, ediTypes: ediTypeDTOList, filters: filters]
        } else {
            def entityId = session['company_id']
            render view: 'list', model: [selected: selected, ediTypes: ediTypeDTOList, filters: filters, ediTypeWS: chainModel ? chainModel?.ediType : null]
        }
    }

    def getFilteredEDITypes(filters, params) {
        params.max = params?.max ? params.max.toInteger() : pagination.max
        params.offset = params?.offset ? params.offset.toInteger() : pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order

        def company_id = session['company_id']
        return EDITypeDTO.createCriteria().list(
                max: params.max,
                offset: params.offset

        ) {
            createAlias("entities", "ce", CriteriaSpecification.LEFT_JOIN)
            and {

                or {
                    //list all global entities as well BUT only if they were created by me or my parent NOT other root companies.
                    and {
                        eq('global', 1)
                        eq('entity.id', companyService.getRootCompanyId())
                    }
                    'in'('ce.id', retrieveCompanies().id)
                }

                filters.each { filter ->
                    if (filter.value != null) {
                        addToCriteria(filter.getRestrictions());
                    }
                }
            }
            resultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }



    def edit() {
        Integer ediTypeId = params.int('id')
        EDITypeWS ediTypeWS = (ediTypeId > 0) ? webServicesSession.getEDIType(ediTypeId) : new EDITypeWS()
        if (request.isXhr()) {
            breadcrumbService.addBreadcrumb(controllerName, actionName, null, ediTypeId)
            render template: 'edit', model: [ediType: ediTypeWS, companies: retrieveCompanies()]
        } else {
            chain action: "list", model: [ediType: ediTypeWS], params: [max: params.max, offset: params.offset]
            return
        }

    }

    def show () {
        if(request.isXhr()){
            EDITypeDTO ediTypeDTO = EDITypeDTO.get(params.int("id"))
            breadcrumbService.addBreadcrumb(controllerName, 'show', null, ediTypeDTO?.id)
            render template:'show' , model: [ediType: ediTypeDTO]
            return
        }else{
            redirect(action: 'list', params: params)
        }

    }

    def save() {

        Integer id = params.int("id")
        EDITypeWS ediTypeWS = id > 0 ? webServicesSession.getEDIType(id) : new EDITypeWS()

        def global = params.boolean("global")
        bindData(ediTypeWS, params)
        ediTypeWS.setCreateDatetime(TimezoneHelper.serverCurrentDate())
        if (global) {
            ediTypeWS.setGlobal(1)
        } else {
            ediTypeWS.setGlobal(0)
        }

        if(id <= 0) ediTypeWS.setEntityId(session["company_id"])
        // Set EDI type statuses
        List<Integer> ediStatusIds = params.list("ediFileStatus.id")
        List<String> ediStatusName = params.list("ediFileStatus.name")

        try {
            if (!ediTypeWS.path) {
                ediTypeWS.path = ediTypeWS.name
            }

            def eventFile = request.getFile("events")

            String fileExtension = FilenameUtils.getExtension(eventFile?.originalFilename)
            if (fileExtension && !fileExtension?.equals("xml")) {
                String[] errors = ["xml.error.found"]
                throw new SessionInternalError("xml.error.found", errors)
            }

            def ediFileFormatDir = new File(Util.getSysProp("base_dir") + File.separator + "edi" + File.separator + "format" + File.separator + "/");
            if(!ediFileFormatDir.exists()) {
//                If directory not exist, create it first. TODO: This code can be moved in prepare test.
                ediFileFormatDir.mkdirs()
            }

            EDITypeWS oldObject
            if (ediTypeWS.getId()) oldObject = webServicesSession.getEDIType(ediTypeWS.getId())

            ediTypeWS.setEdiStatuses([]);
            ediStatusName.eachWithIndex { String status, Integer index ->
                if (status) {
                    EDIFileStatusWS ediFileStatusWS = new EDIFileStatusWS(createDatetime: TimezoneHelper.serverCurrentDate(), name: status)
                    if (ediStatusIds.get(index)) {
                        ediFileStatusWS.setId(ediStatusIds.get(index) as Integer)
                    }
                    ediTypeWS.getEdiStatuses().add(ediFileStatusWS);
                }
            }

            File ediFormatFile = null
            if (!eventFile?.isEmpty()) {
                ediFormatFile = eventFile ? new File(eventFile?.getOriginalFilename()) : null
                eventFile.transferTo(ediFormatFile)
            }
            Integer ediTypeId = webServicesSession.createEDIType(ediTypeWS, ediFormatFile)



            if(ediTypeWS.getId()){
                flash.message = message(code: "edi.type.updated.successfully", args: [ediTypeId])
            }else{
                flash.message = message(code: "edi.type.created.successfully", args: [ediTypeId])
            }
            redirect(action: "list", params: [id: ediTypeId, max: params.max, offset: params.offset])
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            chain action: "list", model: [ediType: ediTypeWS], params: [max: params.max, offset: params.offset]
            return
        }
    }

    def retrieveCompanies() {
        def parentCompany = CompanyDTO.get(session['company_id'])
        def childs = CompanyDTO.findAllByParent(parentCompany)
        childs.add(parentCompany)
        return childs;
    }

    def testEdi() {
        if (!params.id) {
            flash.error = "edit.select.one.type.error"
            redirect(action: 'list')
            return
        }
        [ediTypeId: params.id]

    }

    def testEdiResult() {
        List<EDIFileRecordWS> wsList
        try {
            if (TransactionType.OUTBOUND.toString().equals(params.fileType)) {
                wsList = generateFile(params)
            } else {
                wsList = parseFile(params)
            }
        } catch (SessionInternalError ex) {
            viewUtils.resolveException(flash, session.locale, ex)
        }

        render template: 'result', model: [wsList: wsList, fileType: params.fileType]
    }

    private def generateFile(GrailsParameterMap params) {
        String inputMap = params.fields
        List<Map<String, String>> input
        try {
            input = new GroovyShell().evaluate(inputMap)
        } catch (Exception e) {
            String[] errors = ["ediformat.test.input.invalid"]
            throw new SessionInternalError("ediformat.test.input.invalid", errors)
        }
        FileFormat fileFormat = FileFormat.getFileFormat(params.ediTypeId as int)
        List<EDIFileRecordWS> ediFileRecordWSes = new FlatFileGenerator(fileFormat, 1, null, input).processInput();
        return ediFileRecordWSes
    }

    private def parseFile(GrailsParameterMap params) {

        String input = params.fields
        List<EDIFileRecordWS> list
        try {
            list = input.split("\\r?\\n");
        } catch (Exception e) {
            String[] errors = ["ediformat.test.input.invalid"]
            throw new SessionInternalError("ediformat.test.input.invalid", errors)
        }
        FileFormat fileFormat = FileFormat.getFileFormat(params.ediTypeId as int);
        List<EDIFileRecordWS> ediFileRecordWSes = new FlatFileParser(fileFormat, null, session['company_id'] as int).validateAndParseFile(list);
        return ediFileRecordWSes
    }

    def download() {
        Integer id = params.int("id")

        def file = FileFormat.getFileFormat(id).getFormatFile()

        if (file.exists()) {
            response.setContentType("text/xml")
            response.setHeader("Content-disposition", "attachment;filename=\"${file.name}\"")
            response.outputStream << file.bytes
        } else {
            flash.error = message(code: "edi.types.file.download.fail")
            chain action: "list", params: params
        }
    }

    def uploadOutboundFile(){
        Integer ediTypeId=params.int('ediTypeId')
        try{
            def eventFile = request.getFile("file")

            if(eventFile?.originalFilename){
                    File temp = new File(System.getProperty("java.io.tmpdir")+File.separator+eventFile?.originalFilename)
                    eventFile.transferTo(temp)
                    webServicesSession.uploadEDIFile(temp)
                    flash.message=message(code: 'file.upload.successfully')

            }else{
                flash.error=message(code: 'select.file')
            }
        }catch (Exception e){
            viewUtils.resolveException(flash, session.locale, e)
        }
        redirect(action: 'list', params: [id:ediTypeId, max: params.max, offset: params.offset])

    }

}
