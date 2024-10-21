/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package jbilling

import com.opencsv.CSVWriter
import com.sapienter.jbilling.server.item.AssetBL
import com.sapienter.jbilling.server.pricing.db.RateCardDTO
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.pricing.RateCardBL
import com.sapienter.jbilling.server.pricing.RateCardWS;
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.client.ViewUtils
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Restrictions
import org.hibernate.criterion.CriteriaSpecification

import javax.sql.DataSource

@Secured(["MENU_99"])
class RateCardController {

    def grailsApplication

    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    // Matches the columns in the JQView grid with the corresponding field
    static final viewColumnsToFields =
            ['rateCardId': 'id',
             'company':'company',
             'tableName':'tableName']

    IWebServicesSessionBean webServicesSession
    ViewUtils viewUtils
    DataSource dataSource
	def breadcrumbService
    SecurityValidator securityValidator

    def index (){
        list()
    }

    def getList(params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        boolean isRoot = new CompanyDAS().isRoot(session['company_id']);

        return RateCardDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset
        ) {
            createAlias("childCompanies", "childCompanies", CriteriaSpecification.LEFT_JOIN)
            or {
                eq('company.id', session['company_id'])
                if (isRoot) {
                    eq('childCompanies.parent.id', CompanyDTO.get(session['company_id']).id)
                } else {
                    eq('childCompanies.id', session['company_id'])
                    and {
                        eq('company', retrieveParentCompany())
                        eq('global', true) //if the company is global, we must list them from his children
                    }
                }
            }
            if (params.rateCardId){
                def searchParam = params.rateCardId
                if (searchParam.isInteger()){
                    eq('id', Integer.valueOf(searchParam));
                } else {
                    addToCriteria(Restrictions.ilike("name", searchParam, MatchMode.ANYWHERE) );
                }
            }
            if (params.company){
                addToCriteria(Restrictions.ilike("company.description", params.company, MatchMode.ANYWHERE) );
            }
            if (params.tableName){
                addToCriteria(Restrictions.ilike("tableName", params.tableName, MatchMode.ANYWHERE) );
            }
            SortableCriteria.sort(params, delegate)
        }
    }

    def list (){
		params.max = params?.max?.toInteger() ?: pagination.max
		params.offset = params?.offset?.toInteger() ?: pagination.offset
		params.sort = params?.sort ?: pagination.sort
		params.order = params?.order ?: pagination.order
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'), null)

        def selected = params.id ? RateCardDTO.get(params.int("id")) : null

        securityValidator.validateCompanyHierarchy(selected?.childCompanies*.id, selected?.company?.id, selected?.global)

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid){
            if (params.applyFilter || params.partial) {
                render template: 'rateCardsTemplate', model: [selected: selected]
            }else {
                render view: 'list', model: [selected: selected]
            }
            return
        }

        def cards = getList(params)
        if (params.applyFilter || params.partial) {
            render template: 'rateCardsTemplate', model: [cards: cards, selected: selected]
        } else {
            render view: 'list', model: [cards: cards.unique(), selected: selected]
        }
    }

    def findRateCards (){
        params.sort = viewColumnsToFields[params.sidx]
        params.order = params.sord
        params.max = params.rows
        params.offset = params?.page ? (params.int('page') - 1) * params.int('rows') : 0
        params.alias = SortableCriteria.NO_ALIAS

        def rateCards = getList(params).unique()

        try {
            def jsonData = getAsJsonData(rateCards, params)

            render jsonData as JSON

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            render e.getMessage()
        }
    }

    /**
     * Converts * to JSon
     */
    private def Object getAsJsonData(elements, GrailsParameterMap params) {
        def jsonCells = elements
        def currentPage = params.page ? Integer.valueOf(params.page) : 1
        def rowsNumber = params.rows ? Integer.valueOf(params.rows): 1
        def totalRecords =  jsonCells ? jsonCells.totalCount : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show (){
        def rateCard = RateCardDTO.get(params.int('id'))

        if (rateCard) {
            securityValidator.validateCompanyHierarchy(rateCard?.childCompanies*.id, rateCard?.company?.id, rateCard?.global, true)
        }

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, rateCard.id, rateCard.name)

        render template: 'show', model: [selected: rateCard]
    }

    def delete (){
        if (params.id) {
            def rateCard = RateCardDTO.get(params.int('id'))
            securityValidator.validateCompanyHierarchy(rateCard?.childCompanies*.id, rateCard?.company?.id, rateCard?.global)

            try {
                webServicesSession.deleteRateCard(params.int('id'))
                flash.message = 'rate.card.deleted'
                flash.args = [params.id]
                log.debug("Deleted rate card ${params.id}.")
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
            }
        }

        // re-render the list of rate cards
        params.applyFilter = true
        params.partial = false
        params.id = null
        redirect action: 'list'
    }

    def edit (){
        def rateCard = params.id ? RateCardDTO.get(params.int('id')) : null

        if (rateCard) {
            securityValidator.validateCompanyHierarchy(rateCard?.childCompanies*.id, rateCard?.company?.id, rateCard?.global)
        }

        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', params.name ? 'update' : 'create', params.int('id'))
        if (params.id && rateCard == null) {
            flash.error = 'rate.card.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'rateCard', action: 'list'
            return
        }

        render template: 'edit', model: [rateCard: rateCard ]
    }

    def listEdit (){
        def rateCard = params.id ? RateCardDTO.get(params.int('id')) : null
        
        if (params.id?.isInteger() && !rateCard) {
            redirect action: 'list', params: params
            return
        }
        
        if(rateCard) {
            securityValidator.validateCompanyHierarchy(rateCard?.childCompanies*.id, rateCard?.company?.id, rateCard?.global)
        }

        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset

        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', params.id ? 'update' : 'create', params.int('id'))

        render view: 'list', model: [cards: getList(params), rateCard: rateCard]
    }

    def rates (){
        def RateCardDTO rateCard = params.id ? RateCardDTO.get(params.int('id')) : null

        if (rateCard) {
            securityValidator.validateCompanyHierarchy(rateCard?.childCompanies*.id, rateCard?.company?.id, rateCard?.global, true)
        }

        if (params.id && rateCard == null) {
            flash.error = 'rate.card.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'rateCard', action: 'list'
            return
        }
		breadcrumbService.addBreadcrumb(controllerName, actionName, params.id ? 'rates' : 'rates', params.int('id'))
        // get column names for the table header
        def rateCardService = new RateCardBL(rateCard)
        def columns = rateCardService.getRateTableColumnNames()

        // scrolling result set for reading the table contents
        def resultSet = rateCardService.getRateTableRows()

        render view: 'rates', model: [ rateCard: rateCard, columns: columns, resultSet: resultSet ]
    }

    def csv (){
        def rateCard = params.id ? RateCardDTO.get(params.int('id')) : null

        if (rateCard) {
            securityValidator.validateCompanyHierarchy(rateCard?.childCompanies*.id, rateCard?.company?.id, rateCard?.global, true)
        }

        if (params.id && rateCard == null) {
            flash.error = 'rate.card.not.found'
            flash.args = [ params.id as String ]

            redirect controller: 'rateCard', action: 'list'
            return
        }

        def rateCardService = new RateCardBL(rateCard)

        // outfile
        def file = File.createTempFile(rateCard.tableName, '.csv')
        CSVWriter writer = new CSVWriter(new FileWriter(file))

        // write csv header
        def columns = rateCardService.getRateTableColumnNames()
        writer.writeNext(columns.toArray(new String[columns.size()]))

        // read rows and write file
        def exporter = CsvExporter.createExporter(RateCardDTO.class)
        def resultSet = rateCardService.getRateTableRows()
        while (resultSet.next()) {
            writer.writeNext(exporter.convertToString(resultSet.get()))
        }

        writer.close()

        // send file
        DownloadHelper.setResponseHeader(response, "${rateCard.tableName}.csv")
        render text: file.text, contentType: "text/csv"
    }

    def save() {
        def rateCard = new RateCardWS()
        bindData(rateCard, params)

        if (!params.int('id')) {
            def oldRateCard = params.id ? RateCardDTO.get(params.int('id')) : null
            securityValidator.validateCompanyHierarchy(oldRateCard?.childCompanies*.id, oldRateCard?.company?.id, oldRateCard?.global)
        }

        // save uploaded file
        def rates = request.getFile("rates")
        def temp = null

        if (params.rates?.getContentType().toString().contains('text/csv') ||
                params.rates?.getOriginalFilename().toString().endsWith('.csv')
                || (rateCard.id && rates.empty)) {

            if (!rates.empty && validateRateCardName(rateCard)) {
                def name = rateCard.tableName ?: 'rate'
                temp = File.createTempFile(name, '.csv')
                rates.transferTo(temp)
                log.debug("rate card csv saved to: " + temp?.getAbsolutePath())
            }
            try {
                // save or update
                if (rateCard.isGlobal()) {
                    //Empty entities
                    rateCard.childCompanies = new ArrayList<Integer>(0)
                } else {
                    //Validate for entities
                    if (rateCard.getChildCompanies() == null || rateCard.getChildCompanies().size() == 0) {
                        String[] errors = ["RateCardWS,companies,validation.error.no.company.selected"]
                        throw new SessionInternalError("validation.error.no.company.selected", errors)
                    }
                }
                if (!rateCard.id) {
                    rateCard.id = webServicesSession.createRateCard(rateCard, temp);
                    flash.message = 'rate.card.created'
                    flash.args = [rateCard.id as String]
                } else {
                    //Validate if update is allowed
                    def rateCardDTO = RateCardBL.getDTO(rateCard)
                    rateCardDTO.setGlobal(rateCard.isGlobal())
                    rateCardDTO.setChildCompanies(AssetBL.convertToCompanyDTO(rateCard.getChildCompanies()));
                    def coName = new RateCardBL(rateCard.id).validateRateCardUpdate(rateCardDTO)
                    log.debug("Validate method returned: " + coName)
                    if (coName != null) {
                        log.debug("Invalid update call")
                        flash.error = 'rate.card.update.fail'
                        flash.args = [coName as String]
                    } else {
                        webServicesSession.updateRateCard(rateCard, temp)
                        flash.message = 'rate.card.updated'
                        flash.args = [rateCard.id as String]
                    }
                }
            } catch (SessionInternalError e) {
                viewUtils.resolveException(flash, session.locale, e)
                chain action: 'list', params: [id: rateCard.getId(), name: rateCard.name]
                return
            } finally {
                temp?.delete()
            }

        } else {
            flash.error = "csv.error.found"
            chain action: 'list', params: [id: rateCard.getId(), name: rateCard.name]
            return
        }

        chain action: 'list', params: [id: rateCard?.id]
    }

	def retrieveCompanies(){
		def childs = retrieveChildCompanies()
		childs.add(CompanyDTO.get(session['company_id']))
		return childs;
	}
	
	def retrieveChildCompanies() {
		return CompanyDTO.findAllByParent(CompanyDTO.get(session['company_id']))
	}

    def retrieveParentCompany(){
        return CompanyDTO.get(session['company_id']).getParent()
    }
	
    boolean validateRateCardName(rateCard) {
        boolean valid = false
        String name = rateCard.name

        if(name.length()>0 && name.length()<=50 && name.matches('^[a-zA-Z0-9_]+$')){
             valid = true
        }
        
        return valid;
    }    
    
    Set<CompanyDTO> convertToCompanyDTO(entities){
    	Set<CompanyDTO> childEntities = new HashSet<CompanyDTO>(0);
        for(Integer entity : entities){
        	childEntities.add(new CompanyDAS().find(entity));
        }
        return childEntities;
    }

    def downloadExampleCSV() {
        File file = grailsApplication.mainContext.getResource("examples/example_rate_card.csv").file
        DownloadHelper.setResponseHeader(response, "${file.name}")
        render text: file.text, contentType: "text/csv"
    }
}
