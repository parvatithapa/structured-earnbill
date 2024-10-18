package jbilling

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.dt.ProductImportConstants
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.DeutscheTelecomWebServicesSessionSpringBean
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@Secured(["isAuthenticated()","hasAnyRole('CONFIGURATION_1917')"])
class BulkDownloadController {

    @Autowired
    DeutscheTelecomWebServicesSessionSpringBean dtWebServicesSession
    @Autowired
    WebServicesSessionSpringBean webServicesSessionSpringBean

    def breadcrumbService
    def messageSource

    def downloadPrices() {
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        log.debug("download prices")
    }

    def showDownloadProducts() {
        render view: '/bulkDownload/_downloadPriceTypes'
    }

    def downloadPricesFile(){

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        String[] parameters = params.actionTaken.split("\\?")
        String fileTemplateName = parameters[0]
        String fileToProcess = StringUtils.EMPTY
        String downloadFileName = StringUtils.EMPTY


        switch (fileTemplateName.toLowerCase()){
            case"default":
                fileToProcess = downloadDefaultProductPrices()
                downloadFileName = "defaultPrices_download_file.csv"
                break
            case"accounttype":
                fileToProcess = downloadDefaultAccountLevelPrices()
                downloadFileName = "account_level_prices_download_file.csv"
                break
            case"customer":
                fileToProcess = downloadCustomerPrices()
                downloadFileName = "customer_prices_download_file.csv"
                break
            case"plan":
                fileToProcess = downloadPlans()
                downloadFileName = "plans_download_file.csv"
                break
            case"defaultindividual":
                if(parameters[1].split("=").length < 2){
                    flash.error = messageSource.getMessage("product.download.product.code.error", null, session.locale)
                    redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                    return
                }

                String[] temp = parameters[1].split("=")
                String productCode = temp[1]
                fileToProcess = downloadIndividualProductPrice(productCode);
                downloadFileName = "defaultPrices_download_file.csv"
                break
            case"accounttypeindividual":

                if(parameters[1].split("=").length < 2){
                    flash.error = messageSource.getMessage("product.download.account.id.error", null, session.locale)
                    redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                    return
                }

                String[] temp = parameters[1].split("=")
                String id = temp[1]
                fileToProcess = downloadIndividualAccountLevelPrice(id)
                downloadFileName = "account_level_prices_download_file.csv"
                break
            case"customerindividual":
                if(parameters[1].split("=").length < 2){
                    flash.error = messageSource.getMessage("product.download.customer.id.error", null, session.locale)
                    redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                    return
                }

                String[] temp = parameters[1].split("=")
                String id = temp[1]

                fileToProcess = downloadIndividualCustomerPrice(id)
                downloadFileName = "customer_prices_download_file.csv"
                break
            case"planindividual":
                if(parameters[1].split("=").length < 2){
                    flash.error = messageSource.getMessage("product.download.plan.id.error", null, session.locale)
                    redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                    return
                }

                String[] temp = parameters[1].split("=")
                String id = temp[1]

                fileToProcess = downloadIndividualPlan(id)
                downloadFileName = "plans_download_file.csv"
                break
            default:
                return
        }

        if(StringUtils.isNotEmpty()){
            FileInputStream fileInputStream = new FileInputStream(fileToProcess)
            byte[] csvBytes = IOUtils.toByteArray(fileInputStream)
            DownloadHelper.sendFile(fileToProcess, downloadFileName, "application/csv", csvBytes)
        }
    }

    def String downloadDefaultProductPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        File bulkDownloadProductDir = new File(ProductImportConstants.PRODUCT_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadProductDir.exists()) {
            try {
                bulkDownloadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "ProductDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadProductDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadProductDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadDefaultPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadIndividualProductPrice(String productCode) {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        if(productCode.isEmpty()){
            flash.error = messageSource.getMessage("product.download.product.code.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        if(null == webServicesSessionSpringBean.getItemID(productCode)){
            flash.error = messageSource.getMessage("product.download.product.code.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        File bulkDownloadProductDir = new File(ProductImportConstants.PRODUCT_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadProductDir.exists()) {
            try {
                bulkDownloadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "ProductDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadProductDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadProductDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadIndividualDefaultPrice(csvFile.absolutePath, csvErrorFile.absolutePath, productCode)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadDefaultAccountLevelPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        File bulkDownloadProductDir = new File(ProductImportConstants.ACCOUNT_LEVEL_PRICE_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadProductDir.exists()) {
            try {
                bulkDownloadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "AccountLevelPriceDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadProductDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadProductDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadAccountLevelPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadIndividualAccountLevelPrice(String id) {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        if(id.isEmpty()){
            flash.error = messageSource.getMessage("product.download.account.id.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        if(null == webServicesSessionSpringBean.getAccountType(Integer.parseInt(id))){
            flash.error = messageSource.getMessage("product.download.account.id.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }


        File bulkDownloadProductDir = new File(ProductImportConstants.ACCOUNT_LEVEL_PRICE_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadProductDir.exists()) {
            try {
                bulkDownloadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "AccountLevelPriceDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadProductDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadProductDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadIndividualAccountLevelPrice(csvFile.absolutePath, csvErrorFile.absolutePath, id)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadCustomerPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        File bulkDownloadProductDir = new File(ProductImportConstants.CUSTOMER_PRICE_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadProductDir.exists()) {
            try {
                bulkDownloadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "CustomerPriceDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadProductDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadProductDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadCustomerLevelPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadIndividualCustomerPrice(String id) {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        if(id.isEmpty()){
            flash.error = messageSource.getMessage("product.download.customer.id.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        if(null == dtWebServicesSession.getCustomerByMetaField(id, Constants.DeutscheTelekom.EXTERNAL_ACCOUNT_IDENTIFIER) ){
            flash.error = messageSource.getMessage("product.download.customer.id.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        File bulkDownloadProductDir = new File(ProductImportConstants.CUSTOMER_PRICE_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadProductDir.exists()) {
            try {
                bulkDownloadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "CustomerPriceDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadProductDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadProductDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadIndividualCustomerLevelPrice(csvFile.absolutePath, csvErrorFile.absolutePath, id)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadPlans() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        File bulkDownloadPlansDir = new File(ProductImportConstants.PLANS_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadPlansDir.exists()) {
            try {
                bulkDownloadPlansDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "PlanDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadPlansDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadPlansDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadPlans(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }

    def String downloadIndividualPlan(String id) {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        if(id.isEmpty()){
            flash.error = messageSource.getMessage("product.download.plan.id.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        if(null == webServicesSessionSpringBean.getPlanByInternalNumber(id, webServicesSessionSpringBean.getCallerCompanyId())){
            flash.error = messageSource.getMessage("product.download.plan.id.error", null, session.locale);
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        File bulkDownloadPlansDir = new File(ProductImportConstants.PLANS_DOWNLOAD_FILES_DIR);
        if (!bulkDownloadPlansDir.exists()) {
            try {
                bulkDownloadPlansDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to create directory on server."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file in which to download content
        String downloadFileName = "PlanDownload";
        def csvFile = File.createTempFile(downloadFileName + "_DP", ".csv", bulkDownloadPlansDir)

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(downloadFileName + "_DPError", ".csv", bulkDownloadPlansDir)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.downloadIndividualPlan(csvFile.absolutePath, csvErrorFile.absolutePath, id)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkDownload/processDownloadPrices', model: [jobId: executionId, jobStatus: 'busy']

        return csvFile.absolutePath
    }
}
