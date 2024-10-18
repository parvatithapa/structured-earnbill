package jbilling

import au.com.bytecode.opencsv.CSVWriter

import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.client.util.BindHelper
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.pricing.DataTableQueryEntryWS
import com.sapienter.jbilling.server.pricing.DataTableQueryWS
import com.sapienter.jbilling.server.pricing.RouteBL
import com.sapienter.jbilling.server.pricing.RouteBeanFactory
import com.sapienter.jbilling.server.pricing.RouteRecord
import com.sapienter.jbilling.server.pricing.RouteRecordWS
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.pricing.cache.RouteFinder
import com.sapienter.jbilling.server.pricing.db.DataTableQueryDTO
import com.sapienter.jbilling.server.pricing.db.DataTableQueryEntryDTO
import com.sapienter.jbilling.server.pricing.db.RouteDTO
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteWS
import com.sapienter.jbilling.server.user.db.AccountTypeDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.NameValueString
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.db.StringList

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils

import groovy.json.JsonSlurper

import javax.sql.DataSource

import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import org.springframework.web.multipart.commons.CommonsMultipartFile

@Secured(["MENU_99"])
class RouteController {
    static pagination = [max: 10, offset: 0, sort: 'id', order: 'desc']

    IWebServicesSessionBean webServicesSession
    PluginAwareResourceBundleMessageSource messageSource;
    ViewUtils viewUtils
    DataSource dataSource
    def breadcrumbService
    def routeService
    def routes
    SecurityValidator securityValidator


    int MAX_ROWS = 200

    def index (){
        list()
    }

    def list (){
        routes = getList(params)

        def route = chainModel?.selected
        if (!route) {
            if (params.id) {
                route = RouteDTO.get(params.int('id'))
                securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)
            } else {
                route = params.route ?: null
            }
        }

        def matchingFields = null
        def availMatchingFields = null
        def edit = chainModel?.selected && !chainModel?.notEdition

        if (route?.id) {
            matchingFields = MatchingFieldDTO.createCriteria().list() {
                eq('route.id', route.id)
                order('orderSequence', 'asc')
            }

            availMatchingFields = (
                new RouteBL().getUnusedAdditionalRouteColumns(route?.tableName, route?.id) +
                chainModel?.matchFieldWS ? [chainModel?.matchFieldWS?.getMatchingField()] : []
            )
        }

        if (params.applyFilter || params.partial) {
            render template: 'routes',
                      model: [        routes: routes,
                                    selected: route,
                              matchingFields: matchingFields?:null,
            ]
        } else {
            render   view: 'list',
                    model: [        routes: routes,
                                  selected: route,
                                      edit: edit,
                              matchFieldWS: chainModel?.matchFieldWS,
                       availMatchingFields: availMatchingFields,
                            matchingFields: matchingFields?:null
            ]
        }
    }

    def getList(params){
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = (params?.sort && params.sort != 'null') ? params.sort : pagination.sort
        params.order = (params?.order && params.order != 'null') ? params.order : pagination.order

        return RouteDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            eq('company', CompanyDTO.get(session['company_id']))

            // apply sorting
            SortableCriteria.sort(params, delegate)
        }
    }

    def edit (){
        def route = params.id ? RouteDTO.get(params.int('id')) : null
        securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)

        if (params.id && route == null) {
            flash.error = 'route.not.found'
            flash.args = [params.id as String]

            redirect controller: 'route', action: 'list'
            return
        }
        render template: 'edit', model: [route: route]
    }

    def add (){
        render template: 'edit', model: [route: new RouteDTO()]
    }

    def editMatchingField (){

        def fieldId = params.int('fieldId')
        def routeId = params.int('routeId')

        def route = RouteDTO.get(routeId)
        securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)
        def selectedField = null
        if (fieldId) {
            try {
                selectedField = webServicesSession.getMatchingField(fieldId)
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                chain action: 'list'
                return
            }
        }

        def availMatchingFields = (
			    ( selectedField?.id )
				? new RouteBL().getUnusedAdditionalRouteColumns(route?.tableName,route?.id) + [selectedField?.matchingField]
				: new RouteBL().getUnusedAdditionalRouteColumns(route?.tableName,route?.id)
		)

        render template: 'editMatchingField', model:[
                route: route,
                matchingField: selectedField,
                availMatchingFields: availMatchingFields
        ]
    }

    def saveMatchingField (){
        def routeId = params.getInt('routeId')

        Map updatedTestAndMatchingField = [:]
        def matchFieldWS = new MatchingFieldWS()
        matchFieldWS = bindData(matchFieldWS, params)

        def oldRoute = RouteDTO.get(routeId)
        securityValidator.validateCompany(oldRoute?.company?.id, Validator.Type.EDIT)

        try {
            // save or update
            if (!matchFieldWS.id) {
                matchFieldWS.id = webServicesSession.createMatchingField(matchFieldWS)
                flash.message = 'matching.field.created'
                flash.args = [matchFieldWS.id as String]

            } else {
                webServicesSession.updateMatchingField(matchFieldWS)
                flash.message = 'matching.field.updated'
                flash.args = [matchFieldWS.id as String]
            }

        } catch (SessionInternalError e){
            def route = RouteDTO.get(routeId)
            List<String> selectMatchingField = new RouteBL().getUnusedAdditionalRouteColumns(route?.tableName, route?.id)

            viewUtils.resolveException(flash, session.locale, e)
            chain action: 'list',
                   model: [           selected: route,
                                    notEdition: true,
                                  matchFieldWS: matchFieldWS,
                           selectMatchingField: selectMatchingField]
            return
        }

        chain action: 'list', id: routeId
    }

    def save (){
        def route = new RouteWS();
        viewUtils.trimParameters(params)
        bindData(route, params)
        // save uploaded file
        CommonsMultipartFile routes = request.getFile("routes_file")
        def temp = null

        if (!validateRoute(route, flash)) {
            chain action: 'list', model: [selected: route]
            return
        }

        try {
            if (params.routes_file?.getContentType().toString().contains('text/csv') ||
                    params.routes_file?.getOriginalFilename().toString().endsWith('.csv')
                    || (route.id)) {

                if (!routes.empty) {
                    def name = route.tableName ?: 'route'
                    temp = File.createTempFile(name, '.csv')
                    routes.transferTo(temp)
                    log.debug("route csv saved to: " + temp?.getAbsolutePath());
                }
            } else {
                flash.error =  "routeWS.invalid.csv"
                chain action: 'list', model: [selected: route]
                return
            }

            // save or update
            if (!route?.id) {

                route.id = webServicesSession.createRoute(route, temp);
                flash.message = 'route.created'
                flash.args = [route.id as String]

            } else {
                route.id = webServicesSession.createRoute(route, temp);
                flash.message = 'route.updated'
                flash.args = [route.id as String]
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            chain action: 'list', model: [selected: route]
            return
        } finally {
            temp?.delete()
        }
        chain action: 'list', id: route.id
    }

    def csv (){
        def route = params.id ? RouteDTO.get(params.int('id')) : null
        securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)

        if (params.id && route == null) {
            flash.error = 'route.not.found'
            flash.args = [params.id as String]

            redirect controller: 'route', action: 'list'
            return
        }

        def rateCardService = new RouteBL(route)

        // outfile
        def file = File.createTempFile(route.tableName, '.csv')
        CSVWriter writer = new CSVWriter(new FileWriter(file), ',' as char)

        // write csv header
        def columns = rateCardService.getRouteTableColumnNames()
        writer.writeNext(columns.toArray(new String[columns.size()]))

        // read rows and write file
        def exporter = CsvExporter.createExporter(RouteDTO.class)
        def resultSet = rateCardService.getRouteTableRows()
        while (resultSet.next()) {
            writer.writeNext(exporter.convertToString(resultSet.get()))
        }

        writer.close()

        // send file
        DownloadHelper.setResponseHeader(response, "${route.tableName}.csv")
        render text: file.text, contentType: "text/csv"
    }


    def delete (){
        if (params.id) {
            securityValidator.validateCompany(RouteDTO.get(params.int('id')).company?.id, Validator.Type.EDIT)
            try {
                webServicesSession.deleteRoute(params.int('id'))
                flash.message = 'route.deleted'
                flash.args = [params.id]
                log.debug("Deleted rate card ${params.id}.")

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }
        redirect action: 'list'
    }

    def editDeleteMatchingField (){
        render template: 'editDeleteMatchingField', model: [
                matchingFieldSelectedId: params.int('matchingFieldId'),
                routeId: params.int('routeId')
        ]
    }

    def show (){
        def route = params.id ? RouteDTO.get(params.int('id')) : null
        securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)
        def matchingFields = null

        if (route) {
            matchingFields = MatchingFieldDTO.createCriteria().list() {
                eq('route', route)
                order('orderSequence', 'asc')
            }
        }

        render template: 'show', model: [
                selected: route,
                matchingFields: matchingFields?:null
        ]
    }

    def showMatchingField (){
        def matchingFields = MatchingFieldDTO.createCriteria().list() {
            route {
                eq('company', new CompanyDTO(session['company_id']))
                eq('id', params.int('routeId'))

            }
            order('orderSequence', 'asc')
        }
        securityValidator.validateCompany(RouteDTO.get(params.int('routeId')).company?.id, Validator.Type.VIEW)
        render template: "showMatchingField", model: ['matchingFields': matchingFields]
    }

    def deleteMatchingField (){
        if (params.id) {
            def route = RouteDTO.get(params.int('id'))
            securityValidator.validateCompany(route?.company?.id, Validator.Type.EDIT)
            try {
                webServicesSession.deleteMatchingField(params.int('id'))
                flash.message = 'matching.field.deleted'
                flash.args = [params.id]
                log.debug("Deleted matching field ${params.id}.")

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }
        chain action: 'list', id: params.routeId
    }

    /**
     * Go to the search page
     */
    def search (){
        def route = params.id ? RouteDTO.get(params.int('id')) : null
        securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)
        def columnNames = null
        def tableNames = RouteDTO.findAllByCompany(new CompanyDTO(session['company_id']), [sort: 'id', order: 'desc']).collect { [name: it.name, id: it.id] }
        DataTableQueryWS[] queryWS = null;

        if (route) {
            RouteBeanFactory factory = new RouteBeanFactory(route)
            columnNames = factory.getTableDescriptorInstance().columnsNames
            queryWS = webServicesSession.findDataTableQueriesForTable(route.id)
        } else {
            flash.message = 'route.not.found'
            flash.args = [params.id]
        }

        render  view: 'searchRouteRecordsGrid',
               model: [              route: route,
                               rootTableId: route.id,
                               columnNames: columnNames?:null,
                                tableNames: tableNames,
                                   queries: queryWS,
                       matchedColumnValues: [:]
        ]
    }

    /**
     * Save a nested query
     */
    def saveQuery (){
        DataTableQueryWS ws = new DataTableQueryWS()
        DataTableQueryEntryWS prevEntry = null;
        ws.name = params.name.trim()
        ws.routeId = params.int('rootTableId')
        if (SpringSecurityUtils.ifAllGranted("DATA_TABLES_170")) {
            BindHelper.bindPropertyPresentToInteger(params, ws, ["global"])
        } else {
            ws.global = 0
        }
        def depth = params.int('nestedDepth')

        def route = RouteDTO.get(ws.routeId)
        securityValidator.validateCompany(route?.company?.id, Validator.Type.EDIT)

        //create the query entries
        (0..depth-1).each {
            DataTableQueryEntryWS entry = new DataTableQueryEntryWS()
            entry.columns = new StringList(params['nestedColumns['+it+']'].tokenize(','))
            entry.routeId = params.int('nestedRouteIds['+it+']')
            if(prevEntry) {
                prevEntry.nextQuery = entry
            } else {
                ws.rootEntry = entry
            }
            prevEntry = entry
        }

        try {
            webServicesSession.createDataTableQuery(ws)
            flash.message = 'dataTable.query.created'
            flash.args = [RouteDTO.get(params.int('rootTableId')).name]
            log.debug("Created query ")
            render 'ok'
        } catch (SessionInternalError e) {
            log.debug("Error saving route", e)
            viewUtils.resolveException(flash, session.locale, e)
            render template: '/layouts/includes/messages'
        }
    }

    /**
     * Delete a saved query
     */
    def deleteQuery (){
        if (params.id) {
            def query = DataTableQueryDTO.get(params.int('id'))
            securityValidator.validateCompany(query?.route?.company?.id, Validator.Type.EDIT)

            try {
                webServicesSession.deleteDataTableQuery(params.int('id'))
                flash.message = 'dataTable.query.deleted'
                flash.args = [params.id]
                log.debug("Deleted query ${params.id}.")

            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }
    }

    /**
     * Execute a saved query
     */
    def execQuery (){
        def model = [error: false, nestedDepth: params.int('nestedDepth')]
        if (params.queryId) {
            int fromRouteId = params.int('curr.routeId')
            int rootTableId = fromRouteId
            DataTableQueryDTO query = DataTableQueryDTO.get(params.int('queryId'))
            RouteDTO routeDTO = new RouteBL(fromRouteId).getEntity()
            securityValidator.validateCompany(routeDTO?.company?.id, Validator.Type.VIEW)
            RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
            def columns = factory.tableDescriptorInstance.columnsNames;

            def colsToMatch = [:]
            try {
                //filters for the current search
                def filters = [:]
                columns.each {
                    if(params['curr.in.'+it]) {
                        filters['in.'+it] = params.list('curr.in.'+it)
                    }
                    if(params['curr.'+it]) {
                        filters[it] = params['curr.'+it]
                    }
                }

                def depth = model.nestedDepth
                //construct information to display nested search
                //these are TO table ids
                def nestedRouteIds = (depth > 0) ? (0..depth-1).collect { params['nestedRouteIds['+it+']']}: []
                //these are FROM table names
                def nestedTables = (depth > 0) ? (0..depth-1).collect { params['nestedTables['+it+']']}: []
                def nestedColumns = (depth > 0) ? (0..depth-1).collect { params['nestedColumns['+it+']']}: []
                def nestedValues = (depth > 0) ? (0..depth-1).collect { params['nestedValues['+it+']']}: []

                //Go through all the entries and of the query and perform the nested search for each
                DataTableQueryEntryDTO entry = query.getRootEntry()
                while(entry != null && !model.error) {
                    colsToMatch = [:]
                    entry.columns.value.each {
                        colsToMatch[it] = 0
                    }

                    model = performNestedSearch(rootTableId, colsToMatch, filters, fromRouteId, entry.route.id, model.nestedDepth,
                            nestedRouteIds, nestedTables,  nestedColumns, nestedValues, params)

                    //extract values for the next iteration
                    fromRouteId = entry.route.id
                    filters = model.matchedColumnValues
                    nestedRouteIds = model.nestedRouteIds
                    nestedTables = model.nestedTables
                    nestedColumns = model.nestedColumns
                    nestedValues = model.nestedValues

                    entry = entry.nextQuery
                }
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }

        //if there was an error clear the nested search results
        if(model.error) {
            model.nestedRouteIds = []
            model.nestedTables = []
            model.nestedColumns = []
            model.nestedValues = []
            model.nestedDepth = 0

            if(flash.error == 'dataTable.nested.search.no.data') {
                flash.error = 'dataTable.intermediate.query.no.data'
            }
        }
        def queryWS = webServicesSession.findDataTableQueriesForTable(model['route'].id)
        model['queries'] = queryWS
        render view: 'searchRouteRecordsGrid', model: model
    }

    /**
     * Perform a nested search. Use values from the current result set
     * to search another table.
     */
    def nestedSearch (){
        boolean error = false
        //SEARCH IN THE CURRENT TABLE TO GET THE COLUMN VALUES TO MATCH
        RouteDTO routeDTO = new RouteBL(params.int('curr.routeId')).getEntity()
        securityValidator.validateCompany(routeDTO?.company?.id, Validator.Type.VIEW)
        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        def columns = factory.tableDescriptorInstance.columnsNames;

        //filters for the current search
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
            //match.col for checked checkboxes and match.col_ for unchecked checkboxes
            if(it.key.startsWith('match.col.') && !it.key.startsWith('match.col._')) {
                colsToMatch[it.key.substring('match.col.'.length())] = 0
            }
        }

        def depth = params.int('nestedDepth')
        //construct information to display nested search
        //these are TO table ids
        def nestedRouteIds = (depth > 0) ? (0..depth-1).collect { params['nestedRouteIds['+it+']']}: []
        //these are FROM table names
        def nestedTables = (depth > 0) ? (0..depth-1).collect { params['nestedTables['+it+']']}: []
        def nestedColumns = (depth > 0) ? (0..depth-1).collect { params['nestedColumns['+it+']']}: []
        def nestedValues = (depth > 0) ? (0..depth-1).collect { params['nestedValues['+it+']']}: []

        def model = performNestedSearch(params['rootTableId'], colsToMatch, filters, params.int('curr.routeId'), params['match.table'] ? params.int('match.table') : null, depth,
                nestedRouteIds, nestedTables,  nestedColumns, nestedValues, params)

        def queryWS = webServicesSession.findDataTableQueriesForTable(model['route'].id)
        model['queries'] = queryWS
        println 'model.rowsLimited'+model.rowsLimited
        if(model.rowsLimited) {
            println 'ROWS limited'
            flash.info = 'dataTable.nested.search.rows.limited'
            flash.args = [MAX_ROWS as String]
        }

        render view: 'searchRouteRecordsGrid', model: model
    }

    private Object performNestedSearch(rootTableId, colsToMatch, filters, fromRoute, toTable, depth,
                                       nestedRouteIds, nestedTables, nestedColumns, nestedValues, params) {
        boolean error
        boolean rowsLimited = false
        RouteDTO routeDTO = new RouteBL(fromRoute).getEntity()
        securityValidator.validateCompany(routeDTO?.company?.id, Validator.Type.VIEW)
        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        def columns = factory.tableDescriptorInstance.columnsNames;

        if(colsToMatch.isEmpty()) {
            flash.message = 'dataTable.nested.search.columns.none.selected'
            error = true
        }

        //maps a column name to the set of unique values
        def matchedColumnValues = [:]
        colsToMatch.each { matchedColumnValues['in.'+it.key] = new HashSet() }
        try {
            def oldRows = params.rows
            params.rows = MAX_ROWS
            def result = routeService.getFilteredRecords(routeDTO.id, filters, params)
            params.rows = oldRows?: "20"
            rowsLimited = result.total >= MAX_ROWS

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
            log.info( "Exception while retrieving values for filter", e)
            viewUtils.resolveException(flash, session.locale, e)
            error = true
        }

        def route = toTable ? RouteDTO.get(toTable) : null
        def columnNames = null
        def tableNames = RouteDTO.findAllByCompany(new CompanyDTO(session['company_id']), [sort: 'id', order: 'desc']).collect { [name: it.name, id: it.id] }

        if(!error) {
            if (route) {
                factory = new RouteBeanFactory(route)
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

        //if there was an error do not continue with the nested search
        if(error) {
            route = new RouteBL(params.int('curr.routeId')).getEntity()
            factory = new RouteBeanFactory(routeDTO);
            columnNames = factory.tableDescriptorInstance.columnsNames;

            matchedColumnValues = [:]
            columns.each {
                if(params['curr.in.'+it]) {
                    matchedColumnValues['in.'+it] = params.list('curr.in.'+it)
                }
            }
        } else {
            nestedTables << routeDTO.tableName
            nestedRouteIds << route.id
            nestedColumns << colsToMatch.keySet().join(",")
            def nestedValue = ""

            //Filter values where there is only a difference in case for display purposes
            def caseFilteredColumnValues = [:]
            matchedColumnValues.each {
                def caseFilteredList = []

                def lowerCaseValues = new HashSet()
                it.value.each {v->
                    def lowerV = v.toLowerCase()
                    if(!lowerCaseValues.contains(lowerV)) {
                        lowerCaseValues.add(lowerV)
                        caseFilteredList << v
                    }
                }

                caseFilteredColumnValues[it.key] = caseFilteredList
            }

            colsToMatch.keySet().each{c-> nestedValue += c + ":" + caseFilteredColumnValues['in.'+c].join(',') + "&nbsp;&nbsp;&nbsp;"}
            nestedValues << nestedValue
        }

        return [
                route: route,
                rootTableId: rootTableId,
                columnNames: columnNames?:null,
                tableNames: tableNames,
                matchedColumnValues: matchedColumnValues,
                nestedDepth: depth+(error?0:1),
                nestedRouteIds: nestedRouteIds,
                nestedTables: nestedTables,
                nestedColumns: nestedColumns,
                nestedValues: nestedValues,
                rowsLimited: rowsLimited,
                error: error
        ]
    }

    /**
     * Download the filtered content as a CSV file.
     */
    def filteredCsv (){
        RouteDTO route = new RouteBL(params.int('_routeId')).getEntity()
        securityValidator.validateCompany(route?.company?.id, Validator.Type.VIEW)
        if (route == null) {
            flash.error = 'route.not.found'
            flash.args = [params.id as String]

            redirect controller: 'route', action: 'list'
            return
        }

        def filters = [:]

        RouteBeanFactory factory = new RouteBeanFactory(route);
        def columns = factory.tableDescriptorInstance.columnsNames;
        columns.each {
            if(params[it]) filters[it] = params[it]
        }

        params.rows = 10000
        params.page = 1
        def result = routeService.getFilteredRecords(route.id, filters, params)


        // outfile
        def file = File.createTempFile(route.tableName, '.csv')
        CSVWriter writer = new CSVWriter(new FileWriter(file), ',' as char)

        // write csv header
        writer.writeNext(columns.toArray(new String[columns.size()]))

        // read rows and write file
        def exporter = CsvExporter.createExporter(RouteDTO.class)
        result.rows.each {r->
            def line = []
            columns.eachWithIndex {c, i->
                line << r[i]
            }
            writer.writeNext(exporter.convertToString(line as Object[]))

        }

        writer.close()

        // send file
        DownloadHelper.setResponseHeader(response, "${route.tableName}.csv")
        render text: file.text, contentType: "text/csv"
    }

    /**
     * Search for routes
     */
    def findRoutes (){
        def filters = [:]

        RouteDTO routeDTO = new RouteBL(params.int('_routeId')).getEntity()
        securityValidator.validateCompany(routeDTO?.company?.id, Validator.Type.VIEW)
        RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
        def columns = factory.tableDescriptorInstance.columnsNames;
        columns.each {
            if(params[it]) {
                filters[it] = params[it]
            }
            if(params['in.'+it]) {
                filters['in.'+it] = params.list('in.'+it)
            }
        }
        try {
            def result = routeService.getFilteredRecords(routeDTO.id, filters, params)
            //get the correct index for the column name in the result
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

    /**
     * Add/edit/delete functionality for grid
     */
    def recordEdit (){
        RouteDTO routeDTO = new RouteBL(params.int('_routeId')).getEntity()
        securityValidator.validateCompany(routeDTO?.company?.id, Validator.Type.EDIT)

        //operation is add or edit
        if(params.oper == 'add' || params.oper == 'edit') {
            RouteBeanFactory factory = new RouteBeanFactory(routeDTO);
            def columns = factory.tableDescriptorInstance.columnsNames;
            columns -= ['id', 'name', 'routeId']
            RouteRecordWS record = new RouteRecordWS();
            record.id = params.int('id')
            record.routeId = params.routeId
            record.name = params.name
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
                    record.id = webServicesSession.createRouteRecord(record, routeDTO.id)
                    message = 'record.create.message'
                } else {
                    webServicesSession.updateRouteRecord(record, routeDTO.id)
                    message = 'record.update.message'

                }
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
                webServicesSession.deleteRouteRecord(routeDTO.id, recordId)
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

    def testRoute (){
        RouteFinder routeFinder
        RouteRecord routeRecord
        List<PricingField> pricingFields = new ArrayList<>();

        if (params.test) {

            def map = params.test as HashMap<String, String>;
            map.each { k, v ->
                PricingField.add(pricingFields, new PricingField(k as String, v as String))
            }
        }
        RouteDTO routeDTO = new RouteBL(params.int('routeId')).getEntity()
        securityValidator.validateCompany(routeDTO?.company?.id, Validator.Type.EDIT)
        try {
            routeFinder = new RouteBL(params.int('routeId')).getBeanFactory().getFinderInstance();
            routeRecord = routeFinder.findRoute(routeDTO, pricingFields) ?: null
            render routeRecord?.name ?: g.message(code: "route.test.not.found")
        } catch (SessionInternalError e) {
            render e.getMessage()
        }

    }

    def getMatchingField(Integer routeId) {

        def matchingFields = MatchingFieldDTO.createCriteria().list() {
            route {
                eq('company', new CompanyDTO(session['company_id']))
                eq('id', routeId)
            }
            order('orderSequence', 'asc')
        }
        return matchingFields
    }

    boolean validateRoute(route, flash) {
        boolean valid = true
        String name = route.name

        if(route.id) {
            def oldRoute = RouteDTO.get(route.id)
            securityValidator.validateCompany(oldRoute?.company?.id, Validator.Type.EDIT)
        }

        if (name.length() <= 0 || name.length() > 50) {
            flash.error = 'route.rate.card.validation.name.length'
            flash.args = [1,50]
            valid = false
        } else if(!name.matches('\\w*')) {
            flash.error = 'route.rate.card.special.chars.disallowed'
            valid = false
        }
        return valid;
    }

    def resolveDependency() {
        StringBuffer options = new StringBuffer("<option value=''>${g.message(code: "select.option.default.value.name")}</option>")
        if(params.metafieldId) {
            MetaField metaField = MetaField.get(params.metafieldId)
            AccountTypeDTO accountTypeDTO=AccountTypeDTO.get(params.int("accountTypeId"))
            if(metaField.getDependentMetaFields()?.size() > 0) {
                RouteDTO routeDTO = RouteDTO.get(metaField.getDataTableId());
                RouteBeanFactory factory = new RouteBeanFactory(routeDTO)
                def columnNames = factory.getTableDescriptorInstance().columnsNames
                def filters = [:]

                def jsonSlurper = new JsonSlurper()
                Map map = jsonSlurper.parseText(params.dependentFields)
                map.put("ACCOUNT_TYPE", accountTypeDTO.description)
//                if(!map.values().contains("")){
                filters.putAll(map)
                    params.sord = "asc"
                    params.rows = 500
                    try{
                        def result = routeService.getFilteredRecords(routeDTO.id, filters, params)
                        def searchNameIdx = columnNames.indexOf(params.get("searchName").toString().toLowerCase())

                        Set<String> optionList=[]
                        result.rows.each {
                            optionList.add(it.get(searchNameIdx))
                        }
                        optionList.each {
                            options.append("<option value='${it}'>${it}</option>")
                        }
                    }catch (Exception e){
                        flash.error=message(code: "dependency.field.not.matching.with.column.in.data.table", args: [params.get("searchName"), routeDTO.getName()])
                    }
//                }
            }
        }
        render options

    }

    def downloadExampleCSV() {
        File file = grailsApplication.mainContext.getResource("examples/example_route.csv").file
        DownloadHelper.setResponseHeader(response, "${file.name}")
        render text: file.text, contentType: "text/csv"
    }

}
