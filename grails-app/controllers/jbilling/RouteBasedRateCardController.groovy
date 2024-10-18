package jbilling

import au.com.bytecode.opencsv.CSVWriter

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.Constants
import com.sapienter.jbilling.common.MissingRequiredFieldError
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.pricing.RouteBasedRateCardBL
import com.sapienter.jbilling.server.pricing.RouteRateCardBeanFactory
import com.sapienter.jbilling.server.pricing.cache.RouteBasedRateCardFinder
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.MatchingFieldDAS
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.NameValueString
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import org.apache.commons.lang.StringUtils;

import javax.sql.DataSource


@Secured(["MENU_99"])
class RouteBasedRateCardController {
    static pagination = [max: 10, offset: 0]

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils
    DataSource dataSource
    def breadcrumbService
    def messageSource
    def routeService
    SecurityValidator securityValidator


    def index (){
        list()
    }

    def list (){
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        def cards = RouteRateCardDTO.createCriteria().list(max: params.max, offset: params.offset) {
            eq('company', new CompanyDTO(session['company_id']))
            order('id', 'desc')
        }

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'), null)

        def selected = params.id ? RouteRateCardDTO.get(params.int("id")) : null
        securityValidator.validateCompany(selected?.company?.id, Validator.Type.VIEW)

        if (params.applyFilter || params.partial) {
            render template: 'routeRateCards', model: [cards: cards, selected: selected]
        }

       List<AccountTypeDTO> accountTypeDTOList=AccountTypeDTO.createCriteria().list(max: params.max, offset: params.offset) {
                order("id", "desc")
                eq('company', new CompanyDTO(session['company_id']))
            }

        def matchingFields = null
        if (selected) {
            matchingFields = new MatchingFieldDAS().getMatchingFieldsByRouteRateCardId(params.int('id'))
        }

        [              cards: cards,
                    selected: selected,
              matchingFields: matchingFields,
          accountTypeDTOList: accountTypeDTOList,
            showEditTemplate: params.showEditTemplate,
         availMatchingFields:chainModel ? chainModel?.availMatchingFields : null]

    }

    def show (){
        def selected = RouteRateCardDTO.get(params.int('id'))
        securityValidator.validateCompany(selected?.company?.id, Validator.Type.VIEW)

        def matchingFields = MatchingFieldDTO.createCriteria().list(){
            routeRateCard{
                eq('company',new CompanyDTO(session['company_id']))
                eq('id',selected.id)
            }
            order('orderSequence', 'asc')
        }
        session.setAttribute("routeRateCardId",params.int('id'))
        render template: 'show', model: [selected: selected,matchingFields:matchingFields]
    }

    def delete (){
        def routeRateCard = params.id ? webServicesSession.getRouteRateCard(params.int("id")) : null
        if (routeRateCard) {
            securityValidator.validateCompany(routeRateCard?.entityId, Validator.Type.EDIT)
            try {
                webServicesSession.deleteRouteRateCard(params.int('id'))
                flash.message = 'route.rate.card.deleted'
                flash.args = [params.id]
                log.debug("Deleted route rate card ${params.id}.")
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }

        // re-render the list of rate cards
        params.partial = false
        params.id = null
		redirect (action: 'list')
    }

    def edit (){
        def routeRateCard =  params.id ? webServicesSession.getRouteRateCard(params.int("id")) : new RouteRateCardWS()


        breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'update' : 'create', params.int('id'))
        if (params.id) {
            if(routeRateCard == null) {
                flash.error = 'route.rate.card.not.found'
                flash.args = [params.id as String]

                redirect controller: 'routeBasedRateCard', action: 'list'
                return
            }
            securityValidator.validateCompany(routeRateCard.entityId, Validator.Type.EDIT)
        }
        if(params.partial) {
            render template: 'edit', model: [routeRateCard: routeRateCard]
        } else {
            redirect(action: 'list', id: params.id, params: [showEditTemplate: true])
        }
    }

    def csv (){
        RouteRateCardDTO routeRateCard = params.id ? RouteRateCardDTO.get(params.int('id')) : null
        securityValidator.validateCompany(routeRateCard?.company?.id, Validator.Type.VIEW)

        if (params.id) {
            if(routeRateCard == null) {
                flash.error = 'route.rate.card.not.found'
                flash.args = [params.id as String]

                redirect controller: 'routeBasedRateCard', action: 'list'
                return
            }
            securityValidator.validateCompany(routeRateCard.getCompany().getId(), Validator.Type.EDIT)
        }

        def routeRateCardService = new RouteBasedRateCardBL(routeRateCard)

        // outfile
        def file = File.createTempFile(routeRateCard.tableName, '.csv')
        CSVWriter writer = new CSVWriter(new FileWriter(file), ',' as char)

        // write csv header
        def columns = routeRateCardService.getRouteTableColumnNames(routeRateCard.tableName)
        writer.writeNext(columns.toArray(new String[columns.size()]))

        // read rows and write file
        def exporter = CsvExporter.createExporter(RouteRateCardDTO.class)
        def resultSet = routeRateCardService.getRouteTableRows()
        while (resultSet.next()) {
            writer.writeNext(exporter.convertToString(resultSet.get()))
        }

        writer.close()

        // send file
        DownloadHelper.setResponseHeader(response, "${routeRateCard.tableName}.csv")
        render text: file.text, contentType: "text/csv"
    }

    def save (){
        def routeRateCardWS = new RouteRateCardWS();
        bindData(routeRateCardWS, params)

        // save uploaded file
        def routeRates = request.getFile("routeRates")
        def temp = null
        if (params.routeRates?.getContentType().toString().contains('text/csv') ||
                params.routeRates?.getOriginalFilename().toString().endsWith('.csv')
                || (routeRateCardWS.id && routeRates.empty )) {
            if (!routeRates.empty) {
                def name = routeRateCardWS.tableName ?: 'routeRates'
                temp = File.createTempFile(name, '.csv')
                routeRates.transferTo(temp)
                log.debug("route rate card csv saved to: " + temp?.getAbsolutePath());
            }

            if (!validateRateCardName(routeRateCardWS)) {
                flash.error = 'route.rate.card.special.chars.disallowed'
                chain action: 'list', model: [routeRateCardWS: routeRateCardWS], params: [showEditTemplate: true]
                return
            }

            if (routeRateCardWS.id) {
                securityValidator.validateCompany(RouteRateCardDTO.get(params.int('id'))?.company.id, Validator.Type.EDIT)
            }

            try {
//                save or update
                if (!routeRateCardWS.id) {
                    routeRateCardWS.id = webServicesSession.createRouteRateCard(routeRateCardWS,temp);
                    flash.message = 'route.rate.card.created'
                } else {
					webServicesSession.updateRouteRateCard(routeRateCardWS, temp)
                    flash.message = 'route.rate.card.updated'
                }
                flash.args = [routeRateCardWS.id as String]
                session.setAttribute("routeRateCardId",routeRateCardWS?.id)
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                chain action: 'list', model: [routeRateCardWS: routeRateCardWS], params: [showEditTemplate: true]
                return
            }
            finally {
                temp?.delete()
            }

        } else {
            flash.error = "csv.error.found"
            chain action: 'list', model: [routeRateCardWS: routeRateCardWS], params: [showEditTemplate: true]
            return
        }

        chain action: 'list', params: [id: routeRateCardWS?.id]
    }

    def editMatchingField (){

        def fieldId = params.int('fieldId')
        def routeRateCardId = params.int('routeRateCardId')
        def matchingField

        def routeRateCard = RouteRateCardDTO.get(routeRateCardId)
        securityValidator.validateCompany(routeRateCard?.company?.id, Validator.Type.VIEW)

        if (fieldId)
        {
            try{
                matchingField = webServicesSession.getMatchingField(fieldId)
            }
            catch (SessionInternalError e){
                viewUtils.resolveException(flash,session.locale,e)
                redirect controller: 'routeBasedRateCard', action: 'list'
                return
            }

        }

        def selectMatchingField = matchingField?.id ?
            new RouteBasedRateCardBL().getUnusedRouteRateCardColumns(routeRateCard?.tableName,routeRateCardId) + [matchingField?.matchingField]
           :new RouteBasedRateCardBL().getUnusedRouteRateCardColumns(routeRateCard?.tableName,routeRateCardId)
        if (!selectMatchingField) {
            render flash.error = message(code: 'matching.field.not.available')
            return
        }
        render template: "/route/editMatchingField",
                model:[matchingField:matchingField,
                       availMatchingFields:selectMatchingField,
                       routeRateCard:routeRateCard]
    }
    def saveMatchingField (){

        def routeRateCardId = params.getInt('routeRateCardId')
        def matchFieldWS = new MatchingFieldWS()
        matchFieldWS= bindData(matchFieldWS,params)

        if (matchFieldWS.id) {
            def routeRateCard = RouteRateCardDTO.get(routeRateCardId)
            securityValidator.validateCompany(routeRateCard?.company?.id, Validator.Type.EDIT)
        }

        try {

            // save or update
            if (!matchFieldWS.id) {
                matchFieldWS.id = webServicesSession.createMatchingField(matchFieldWS)
                flash.message = 'matching.field.created'
                flash.args = [matchFieldWS.id as String]
            }else{
                webServicesSession.updateMatchingField(matchFieldWS)
                flash.message = 'matching.field.updated'
                flash.args = [matchFieldWS.id as String]
            }

            //Get Updated Matching Field
            //matchingFields = getMatchingField(session.getAttribute("routeRateCardId"))

        }catch (SessionInternalError e){
            viewUtils.resolveException(flash,session.locale,e)
            def routeRateCard = RouteRateCardDTO.get(routeRateCardId)
            def selectMatchingField = matchFieldWS?.id ? new RouteBasedRateCardBL().getUnusedRouteRateCardColumns(routeRateCard?.tableName,routeRateCardId) + [matchFieldWS?.matchingField] :
                                                         new RouteBasedRateCardBL().getUnusedRouteRateCardColumns(routeRateCard?.tableName,routeRateCardId)
            chain(controller: 'routeBasedRateCard',
                      action: 'list',
                       model: [      matchingField: matchFieldWS,
                               availMatchingFields: selectMatchingField,
                                     routeRateCard: routeRateCard],
                      params: [id: routeRateCardId])
            return
        }

        chain action: 'list', params: [ id: routeRateCardId ]
    }

    def editDeleteMatchingField (){

        render template: "/route/editDeleteMatchingField" , model:['matchingFieldSelectedId':params.int('matchingFieldId'),
                routeRateCardId: params.int('routeRateCardId')]

    }

    def deleteMatchingField (){
        if(params.id){
            def routeRateCard = MatchingFieldDTO.get(params.int('id')).routeRateCard
            securityValidator.validateCompany(routeRateCard?.company?.id, Validator.Type.EDIT)
            try {
                webServicesSession.deleteMatchingField(params.int('id'))
                flash.message = 'matching.field.deleted'
                flash.args = [params.id]
                log.debug("Deleted matching field ${params.id}.")

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }
        //Get Updated Matching Field
        chain action: 'list'
    }

    def test (){

        List<MatchingFieldDTO> matchingFields = new MatchingFieldDAS().getMatchingFieldsByRouteRateCardId(session.getAttribute("routeRateCardId"))
        def routeRateCardId = params.getInt('routeRateCardId')
        securityValidator.validateCompany(RouteRateCardDTO.get(routeRateCardId)?.company?.id, Validator.Type.VIEW)
        render template: 'test' , model :[matchingFields:matchingFields,routeRateCardId:routeRateCardId]

    }

    def findPriceByRouteRateCard (){

        def routeRateCardId = params.getInt('routeRateCardId')
        RouteBasedRateCardBL routeBasedRateCardBL = new RouteBasedRateCardBL(routeRateCardId);
		RouteRateCardDTO routeRateCardDTO= routeBasedRateCardBL.getEntity()
        securityValidator.validateCompany(routeRateCardDTO?.company?.id, Validator.Type.VIEW)
        Map matchingFieldMap = params?.test as Map
        
        def price = null 
		try {
			List<PricingField> fields= new ArrayList<PricingField>();

            String code = validateDuration(params['business.duration'])
            if (code) {
                throw new SessionInternalError(message(code: code));
            }

            fields.add(new PricingField (Constants.DEFAULT_DURATION_FIELD_NAME, new Double (params['business.duration'] )) );

			if (matchingFieldMap) {
				for (Map.Entry entry : matchingFieldMap.entrySet()) {
                    PricingField.add(fields, new PricingField(entry.getKey(), entry.getValue() ));
				}
			}
			RouteBasedRateCardFinder routeBasedRateCardFinder = routeBasedRateCardBL.getBeanFactory().getFinderInstance()
			
			price= routeBasedRateCardFinder.findRoutePrice(routeRateCardDTO, fields)
		} catch (MissingRequiredFieldError e) {
			viewUtils.resolveException(flash, session.locale, e)
			chain action: 'test'
			return
		} catch (SessionInternalError e) {
            log.debug("Exception at finding route : " + e)
            render e.getMessage()
            return
        }
        if (price != null) {
            render price.compareTo(BigDecimal.ZERO) == 0? BigDecimal.ZERO: price
        } else {
            render "No Valid Route/Price Found"
        }
    }

    private String validateDuration(String duration) {
        if (StringUtils.isEmpty(duration)) {
            return "business.duration.is.required"
        }
        try {
            new Double(duration)
        } catch (NumberFormatException nfe) {
            return "business.duration.is.not.valid"
        }
        return null
    }

    boolean validateRateCardName(routeRateCard) {
        boolean valid = false
            String name = routeRateCard.name

        if(name.length()>0 && name.length()<=50 && name.matches('\\w*')){
            valid = true
        }

        return valid;
    }

    public def getMatchingField(Integer routeRateCardId){
        def matchingFields = MatchingFieldDTO.createCriteria().list(){
            routeRateCard{
                eq('company',new CompanyDTO(session['company_id']))
                eq('id',routeRateCardId)
            }
            order('orderSequence', 'asc')
        }
        return matchingFields
    }

    def search (){
        RouteRateCardDTO routeRateCard = params.id ? RouteRateCardDTO.get(params.int('id')) : null
        securityValidator.validateCompany(routeRateCard?.company?.id, Validator.Type.VIEW)
        def columnNames = null
        def tableNames = RouteRateCardDTO.findAll().collect { [name: it.name, id: it.id]}

        if (routeRateCard) {
            RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(routeRateCard)
            columnNames = factory.getTableDescriptorInstance().columnsNames
        } else {
            flash.message = 'route.rate.card.not.found'
            flash.args = [params.id]
        }
        render  view: 'searchRouteRateCardRecordsGrid',
               model: [      routeRateCard: routeRateCard,
                               columnNames: columnNames?:null,
                                tableNames: tableNames,
                       matchedColumnValues: [:]
        ]
    }

    def findRouteRateCardRecord(){
        def filters = [:]
        RouteRateCardDTO routeRateCardDTO=RouteRateCardDTO.get(params.routeRateCardId)
        securityValidator.validateCompany(routeRateCardDTO?.company?.id, Validator.Type.VIEW)
        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(routeRateCardDTO);
        def columns = factory.tableDescriptorInstance.columnsNames;

        columns.each {
            if(params[it]) {
                filters[it] = params[it]
            }
            if(params['in.'+it]) {
                filters['in.'+it] = params.list('in.'+it)
            }
        }

        try{
            def result = routeService.getFilteredRecords(routeRateCardDTO.id, filters, params);
            def colNameIdx = [:]

            if(result.rows) {
                columns.each {
                    colNameIdx[it] = result.columnNames.indexOf(it)
                }
            }

            def jsonCells = result.rows.collect {
                def cells = [:]
                columns.each {c->cells[c] = it[colNameIdx[c]]}

                [cell:  cells
                        , id: cells.id]
            }

            def currentPage = Integer.valueOf(params.page) ?: 1
            def numberOfPages = Math.ceil(result.total / Integer.valueOf(params.rows))

            def jsonData= [rows: jsonCells,page:currentPage, records:result.total, total:numberOfPages]

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    def recordEdit (){
        RouteRateCardDTO routeRateCardDTO = new RouteBasedRateCardBL(params.int('routeRateCardId')).getEntity()
        securityValidator.validateCompany(routeRateCardDTO?.company?.id, Validator.Type.EDIT)
        //operation is add or edit
        if(params.oper == 'add' || params.oper == 'edit') {
            RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(routeRateCardDTO);
            def columns = factory.tableDescriptorInstance.columnsNames;
            columns -= ['id', 'name']
            RouteRateCardWS record = new RouteRateCardWS();
            record.id = params.int('id')
            record.name = params.name
            record.ratingUnitId=routeRateCardDTO?.ratingUnit?.getId()

            def attributes = []
            columns.each {
                attributes << new NameValueString(it, params[it]?:'')
            }
            record.attributes = attributes

            def state = "ok"
            def messages = []
            try {
                def message
                if(params.oper == 'add') {
                    record.id = webServicesSession.createRouteRateCardRecord(record, routeRateCardDTO.id)
                    message = 'record.create.message'
                } else {
                    webServicesSession.updateRouteRateCardRecord(record, routeRateCardDTO.id)
                    message = 'record.update.message'                }
                messages << messageSource.getMessage(message, [record.id] as Object[], session.locale);
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                messages = flash.errorMessages
                //if the operation is add we want the add dialog to display the error message and not the error div
                if(params.oper == 'add') {
                    flash.errorMessages = null
                }
                state = 'fail'
            }
            def response = [messages:messages,state:state,id:record.id]
            render response as JSON
        } else if(params.oper == 'del') {
            def messages = []
            def state = "ok"
            def recordId = params.int('id')

            try {
                webServicesSession.deleteRateCardRecord(routeRateCardDTO.id, recordId)
                messages << messageSource.getMessage('record.delete.message', [recordId] as Object[], session.locale);
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                messages = flash.errorMessages
                state = 'fail'
            }
            def response = [messages:messages,state:state,id:recordId]
            render response as JSON
        }
    }

    def nestedSearch (){

        boolean error = false
        RouteRateCardDTO routeRateCardDTO = new RouteBasedRateCardBL(params.int('curr.routeRateCardId')).getEntity()
        securityValidator.validateCompany(routeRateCardDTO?.company?.id, Validator.Type.VIEW)
        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(routeRateCardDTO);
        def columns = factory.tableDescriptorInstance.columnsNames;
        def filters = [:]
        columns.each {
            if(params['curr.in.'+it]) {
                filters['in.'+it] = params.list('curr.in.'+it)
            }
            if(params['curr.'+it]) {
                filters[it] = params['curr.'+it]
            }
        }

        //columns that must be matched in the nested search
        //maps column name to column index in the search result
        def colsToMatch = [:]
        params.each {
            if(it.key.startsWith('match.col.')) {
                colsToMatch[it.key.substring('match.col.'.length())] = 0
            }
        }

        if(colsToMatch.isEmpty()) {
            flash.message = 'dataTable.nested.search.columns.none.selected'
            error = true
        }

        //maps a column name to the set of unique values
        def matchedColumnValues = [:]
        colsToMatch.each { matchedColumnValues['in.'+it.key] = new HashSet() }
        try {
            def oldRows = params.rows
            params.rows = "1000"
            def result = routeService.getFilteredRecords(routeRateCardDTO.id, filters, params)
            params.rows = oldRows?: "20"

            if(result.total > 0) {
                //get the correct index for the column name in the result
                colsToMatch.each{
                    colsToMatch[it.key] = result.columnNames.indexOf(it.key)
                }

                //collect the unique column values for each column
                result.rows.each {r->
                    colsToMatch.each {c->
                        matchedColumnValues['in.'+c.key].add(r[c.value])
                    }
                }
            } else {
                //there are no records in the current selection
                flash.error = 'dataTable.nested.search.no.data'
                error = true
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            error = true
        }

        RouteRateCardDTO routeRateCard = params['match.table'] ? RouteRateCardDTO.get(params.int('match.table')) : null
        securityValidator.validateCompany(routeRateCard?.company?.id, Validator.Type.VIEW)

        def columnNames = null
        def tableNames = RouteRateCardDTO.findAll().collect { [name: it.name, id: it.id] }

        if(!error) {
            if (routeRateCard) {
                factory = new RouteRateCardBeanFactory(routeRateCard)
                columnNames = factory.getTableDescriptorInstance().columnsNames
                if(!columnNames.containsAll(colsToMatch.keySet())) {
                    flash.message = 'dataTable.nested.search.columns.no.match'
                    error = true
                }
            } else {
                flash.message = 'route.not.found'
                flash.args = [params.id]
                error = true
            }
        }

        //construct information to display nested search
        def depth = params.int('nestedDepth')
        def nestedTables = (depth > 0) ? (0..depth-1).collect { params['nestedTables['+it+']']}: []
        def nestedColumns = (depth > 0) ? (0..depth-1).collect { params['nestedColumns['+it+']']}: []
        def nestedValues = (depth > 0) ? (0..depth-1).collect { params['nestedValues['+it+']']}: []

        //if there was an error do not continue with the nested search
        if(error) {
            routeRateCard = new RouteBasedRateCardBL(params.int('curr.routeRateCardId')).getEntity()
            factory = new RouteRateCardBeanFactory(routeRateCardDTO);
            columnNames = factory.tableDescriptorInstance.columnsNames;

            matchedColumnValues = [:]
            columns.each {
                if(params['curr.in.'+it]) {
                    matchedColumnValues['in.'+it] = params.list('curr.in.'+it)
                }
            }
        } else {
            nestedTables << routeRateCardDTO.tableName
            nestedColumns << colsToMatch.keySet().join(",")
            def nestedValue = ""

            //Filter values where there is only a difference in case for display purposes
            def caseFilteredColumnValues = [:]
            matchedColumnValues.each {
                def caseFilterdList = []

                def lowerCaseValues = new HashSet()
                it.value.each {v->
                    def lowerV = v.toLowerCase()
                    if(!lowerCaseValues.contains(lowerV)) {
                        lowerCaseValues.add(lowerV)
                        caseFilterdList << v
                    }
                }

                caseFilteredColumnValues[it.key] = caseFilterdList
            }

            colsToMatch.keySet().each{c-> nestedValue += c + ":" + caseFilteredColumnValues['in.'+c].join(',') + "&nbsp;&nbsp;&nbsp;"}
            nestedValues << nestedValue
        }

        render view: 'searchRouteRateCardRecordsGrid', model: [
                routeRateCard: routeRateCard,
                columnNames: columnNames?:null,
                tableNames: tableNames,
                matchedColumnValues: matchedColumnValues,
                nestedDepth: depth+(error?0:1),
                nestedTables: nestedTables,
                nestedColumns: nestedColumns,
                nestedValues: nestedValues
        ]
    }


    def filteredCsv (){
        RouteRateCardDTO routeRateCardDTO = new RouteBasedRateCardBL(params.int('routeRateCardId')).getEntity()
        securityValidator.validateCompany(routeRateCardDTO?.company?.id, Validator.Type.VIEW)

        if (routeRateCardDTO == null) {
            flash.error = 'rate.card.not.found'
            flash.args = [params.id as String]

            redirect controller: 'routeBasedRateCard', action: 'list'
            return
        }

        def filters = [:]

        RouteRateCardBeanFactory factory = new RouteRateCardBeanFactory(routeRateCardDTO);
        def columns = factory.tableDescriptorInstance.columnsNames;
        columns.each {
            if(params[it]) filters[it] = params[it]
        }

        params.rows = 10000
        params.page = 1
        def result = routeService.getFilteredRecords(routeRateCardDTO.id, filters, params)

        // outfile
        def file = File.createTempFile(routeRateCardDTO.tableName, '.csv')
        CSVWriter writer = new CSVWriter(new FileWriter(file), ',' as char)

        // write csv header
        writer.writeNext(columns.toArray(new String[columns.size()]))

        // read rows and write file
        def exporter = CsvExporter.createExporter(RouteRateCardDTO.class)
        result.rows.each {r->
            def line = []
            columns.eachWithIndex {c, i->
                line << r[i]
            }
            writer.writeNext(exporter.convertToString(line as Object[]))
        }

        writer.close()

        // send file
        DownloadHelper.setResponseHeader(response, "${routeRateCardDTO?.tableName}.csv")
        render text: file.text, contentType: "text/csv"
    }
}
