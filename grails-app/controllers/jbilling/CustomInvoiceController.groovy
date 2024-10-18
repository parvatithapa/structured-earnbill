package jbilling

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.util.BetaCustomerConstants
import grails.plugin.springsecurity.annotation.Secured


/**
 * CreditNoteController
 *
 * @author Parvati Thapa
 * @since 11/10/2023
 */
@Secured(["MENU_99"])
class CustomInvoiceController {
    def webServicesSession
    def breadcrumbService
    def filterService
    def viewUtils
    def grailsApplication
    def index() {
        list()
    }

    def list() {
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        render view: 'customInvoicePage'
    }

    def uploadFile() {
        def invoiceId
        def file
        def customInvoiceFile
        try {
            file = request.getFile("file")
            if (file.empty) {
                flash.error = 'validation.file.upload'
                render view: 'customInvoicePage'
            } else {
                if (params.file?.getContentType().toString().contains('text/csv') ||
                        params.file?.getOriginalFilename().toString().endsWith('.csv')) {
                    if (!file.empty) {
                        customInvoiceFile = File.createTempFile(file.fileItem.name, '.tmp')
                        file.transferTo(customInvoiceFile)
                        if (getNumberOfLines(customInvoiceFile) < BetaCustomerConstants.MAX_LINE) {
                            invoiceId = webServicesSession.createCustomInvoice(customInvoiceFile)
                        } else {
                            flash.error = 'line.validation'
                            render view: 'customInvoicePage'
                        }
                    }
                } else {
                    flash.error = "csv.error.found"
                }
            }
        } catch (SessionInternalError sessionInternalError) {
            log.error sessionInternalError.getErrorDetails()
            viewUtils.resolveException(flash, session.locale, sessionInternalError);
        } catch (Exception e) {
            log.error e.getMessage()
            flash.error = e.getMessage()
        }
        if (invoiceId) {
            flash.message = 'file.upload.success'
        }
        render view: 'customInvoicePage', model: [invoiceId: invoiceId]
    }

    def invoiceFilter() {
        def filter = new Filter(type: FilterType.INVOICE, constraintType: FilterConstraint.EQ, field: 'id', template: 'id', visible: true, integerValue: params.int('id'))
        filterService.setFilter(FilterType.INVOICE, filter)
        redirect controller: 'invoice', action: 'list'
    }

    def downloadFile()
    {
        File file = grailsApplication.mainContext.getResource("examples/example_ad_hoc_invoice_order_creation_data_v4.csv").file
        DownloadHelper.setResponseHeader(response, "${file.name}")
        render text: file.text, contentType: "text/csv"
    }
    private Integer getNumberOfLines(File customInvoiceFile) {
        return Util.executeCommand(["wc", "-l", "<", customInvoiceFile.getName()] as String[],
                new File(customInvoiceFile.getParent()), true).split(" ")[0] as Integer
    }
}
