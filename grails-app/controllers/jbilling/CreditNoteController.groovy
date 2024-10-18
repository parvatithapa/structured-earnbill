package jbilling

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.client.util.SortableCriteria
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.creditnote.CreditNoteBL
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.creditnote.db.CreditNoteDTO
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.hibernate.Criteria
import org.hibernate.FetchMode
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.MatchMode
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Property
import org.hibernate.criterion.Restrictions

/**
 * CreditNoteController
 *
 * @author Usman Malik
 * @since 22/07/15
 */
@Secured(["MENU_905"])
class CreditNoteController {
    static scope = "prototype"
    static pagination = [ max: 10, offset: 0, sort: 'id', order: 'desc' ]

    def webServicesSession
    def viewUtils
    def filterService
    def recentItemService
    def breadcrumbService
    def subAccountService

    def index () {
        list()
    }

    def getList(filters, params) {
        params.max = params?.max?.toInteger() ?: pagination.max
        params.offset = params?.offset?.toInteger() ?: pagination.offset
        params.sort = params?.sort ?: pagination.sort
        params.order = params?.order ?: pagination.order
        def user_id = session['user_id']
        def partnerDtos = PartnerDTO.createCriteria().list(){
            eq('baseUser.id', user_id)
        }
        log.debug "### partner:"+partnerDtos

        def customersForUser = new ArrayList()
        if(partnerDtos.size>0){
            customersForUser = 	CustomerDTO.createCriteria().list(){
                createAlias("partners", "partners")
                'in'('partners.id', partnerDtos*.id)
            }
        }

        log.debug "### customersForUser:"+customersForUser
        def company_id = session['company_id']
        return CreditNoteDTO.createCriteria().list(
                max:    params.max,
                offset: params.offset ) {
                createAlias('creationInvoice', 'ci')
                createAlias('ci.baseUser', 'u')

                // create alias only if applying invoice filters to prevent duplicate results
                if (filters.find{ it.field.startsWith('i.') && it.value })
                    createAlias('paidInvoices', 'i', Criteria.LEFT_JOIN)

                and {
                    filters.each { filter ->
                        if (filter.value != null) {
                            if (filter.field == 'contact.fields') {
                                String typeId = params['contactFieldTypes']
                                String ccfValue = filter.stringValue;
                                log.debug "Contact Field Type ID: ${typeId}, CCF Value: ${ccfValue}"

                                if (typeId && ccfValue) {
                                    MetaField type = findMetaFieldType(typeId.toInteger());
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
                                                break;
                                            case DataType.ENUMERATION:
                                            case DataType.JSON_OBJECT:
                                                addToCriteria(Restrictions.ilike("fieldValue.value", ccfValue, MatchMode.ANYWHERE))
                                                break;
                                            default:
                                                // todo: now searching as string only, search for other types is impossible
                                                addToCriteria(Restrictions.eq("fieldValue.value", ccfValue))
                                                break;
                                        }

                                    }
                                }
                            } else {
                                addToCriteria(filter.getRestrictions());
                            }
                        }
                    }

                    eq('u.company', new CompanyDTO(company_id))
                    eq('deleted', 0)

                    if (SpringSecurityUtils.ifNotGranted("PAYMENT_36")) {
                        if (SpringSecurityUtils.ifAnyGranted("PAYMENT_37")) {
                            // restrict query to sub-account user-ids
                            def subAccountIds = subAccountService.getSubAccountUserIds()
                            if (subAccountIds.size() > 0) {
                                'in'('u.id', subAccountIds)
                            }
                        } else {
                            if(customersForUser.size() > 0){
                                // limit list to only this customer
                                'in'('u.id', customersForUser*.baseUser.userId)
                            }
                        }
                    }
                }

                // apply sorting
                SortableCriteria.sort(params, delegate)
            }
        }

    def list () {
        def filters = filterService.getFilters(FilterType.CREDITNOTE, params)

        def selected = params.id ? CreditNoteDTO.get(params.int("id")) : null
        def creditNotes = selected ? getListWithSelected(selected) : getList(filters, params)

        breadcrumbService.addBreadcrumb(controllerName, 'list', null, selected?.id )

//        // if the id exists and is valid and there is no record persisted for that id, write an error message
        if(params.id?.isInteger() && selected == null){
            flash.error = message(code: 'flash.creditNote.not.found')
        }
//
        if (params.applyFilter || params.partial) {
            render template: 'creditNotes', model: [creditNotes: creditNotes, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        } else {
            render view: 'list', model: [creditNotes: creditNotes, selected: selected, filters: filters, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
        }
    }

    def getListWithSelected(selected) {
        def idFilter = new Filter(type: FilterType.ALL, constraintType: FilterConstraint.EQ,
                field: 'id', template: 'id', visible: true, integerValue: selected.id)
        getList([idFilter], params)
    }

    def show () {
        CreditNoteDTO creditNote = new CreditNoteBL().getDTO(webServicesSession.getCreditNote(params.int('id')))
        if (!creditNote) {
            log.debug "redirecting to list"
            redirect(action: 'list')
            return
        }
        recentItemService.addRecentItem(params.int('id'), RecentItemType.CREDITNOTE)
        breadcrumbService.addBreadcrumb(controllerName, 'list', params.template ?: null, params.int('id'))

        render template: 'show', model: [selected: creditNote, displayer: UserHelperDisplayerFactory.factoryUserHelperDisplayer(session['company_id'])]
    }

    def delete () {
        if (params.id) {
            try {
                webServicesSession.deleteCreditNote(params.int('id'))
                log.debug("Deleted credit Note ${params.id}.")
                flash.message = 'credit.note.deleted'
                flash.args = [params.id]
            } catch (SessionInternalError e) {
                viewUtils.resolveExceptionMessage(flash, session.local, e)
                params.applyFilter = false
                params.partial = true
                list()
                return
            }
        }

        // render the partial creditNotes list
        params.applyFilter = true
        redirect action: 'list'

    }

    def unlink () {
        try {
            webServicesSession.removeCreditNoteLink(params.int('invoiceId'), params.int('id'))
            flash.message = "creditNote.unlink.success"

        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);

        } catch (Exception e) {
            log.error("Exception unlinking invoice.", e)
            flash.error = "error.invoice.unlink.creditNote"
        }

        redirect action: 'list', params: [id: params.id]
    }

    def csv () {
        def filters = filterService.getFilters(FilterType.CREDITNOTE, params)

        params.max = CsvExporter.MAX_RESULTS
        def creditNotes = getList(filters, params)

        if (creditNotes.totalCount > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [ CsvExporter.MAX_RESULTS ]
            redirect action: 'list', id: params.id

        } else {
            DownloadHelper.setResponseHeader(response, "creditNotes.csv")
            Exporter<CreditNoteDTO> exporter = CsvExporter.createExporter(CreditNoteDTO.class);
            render text: exporter.export(creditNotes), contentType: "text/csv"
        }
    }
}
