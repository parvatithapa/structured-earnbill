package jbilling

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.dt.PlanImportConstants
import com.sapienter.jbilling.server.dt.ProductImportConstants
import com.sapienter.jbilling.server.util.DeutscheTelecomWebServicesSessionSpringBean
import grails.plugin.springsecurity.annotation.Secured
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@Secured(["isAuthenticated()","hasAnyRole('CONFIGURATION_1917')"])
class BulkUploadController {

    private static final String BULKLOADER_FILES_DIR = Util.getSysProp("base_dir") + "bulkloader";
    private static final String PRODUCT_FILES_DIR = BULKLOADER_FILES_DIR + File.separator + "products";
    private static final String PLAN_FILES_DIR = BULKLOADER_FILES_DIR + File.separator + "plans";
    private static final String DEFAULT_PRICE_TEMPLATE = PRODUCT_FILES_DIR + File.separator + "template_bulkloader_defaultPrices.csv";
    private static final String ACCOUNT_TYPE_TEMPLATE = PRODUCT_FILES_DIR + File.separator + "template_bulkloader_accountTypePrices.csv";
    private static final String CUSTOMER_PRICE_TEMPLATE = PRODUCT_FILES_DIR + File.separator + "template_bulkloader_customerPrices.csv";
    private static final String PLAN_PRICE_TEMPLATE = PLAN_FILES_DIR + File.separator + "template_bulkloader_planPrices.csv";

    @Autowired
    DeutscheTelecomWebServicesSessionSpringBean dtWebServicesSession

    def breadcrumbService
    def messageSource

    def index() { }

    def uploadPrices() {
        breadcrumbService.addBreadcrumb(controllerName, actionName, null, null)
        log.debug("upload prices")
    }

    def showUploadProducts() {
        render view: '/bulkUpload/_uploadProducts'
    }

    def uploadDefaultProductPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        def file = request.getFile('productFile');
        File bulkuploadProductDir = new File(ProductImportConstants.PRODUCT_FILES_DIR);
        if (!bulkuploadProductDir.exists()) {
            try {
                bulkuploadProductDir.mkdirs();
            } catch (SecurityException se) {
                System.out.println("Exception while creating directory");
            }
        }

        //csv file we are uploading
        String fileExtension = FilenameUtils.getExtension(file.originalFilename)
        String incomingFileName = FilenameUtils.removeExtension(file.originalFilename)
        def csvFile = File.createTempFile(incomingFileName + "_DP", ".csv", bulkuploadProductDir)
        if (fileExtension && !fileExtension.equals("csv")) {
            flash.error = "csv.error.found"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }else if (!fileExtension) {
            flash.error = "validation.file.upload"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(incomingFileName + "_DPError", ".csv", bulkuploadProductDir)

        //copy the uploaded file to a temp file
        file.transferTo(csvFile)

        def executionId = 0
        try {
            //start a batch job to import the assets
            executionId = dtWebServicesSession.uploadDefaultPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale);
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkUpload/processProductPrices', model: [jobId: executionId, jobStatus: 'busy']
    }

    def uploadAccountPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        def file = request.getFile('accountPricesFile');
        def bulkuploadProductDir = new File(ProductImportConstants.PRODUCT_FILES_DIR);
        if (!bulkuploadProductDir.exists()) {
            try {
                bulkuploadProductDir.mkdirs();
            } catch (SecurityException se) {
                System.out.println("Exception while creating directory");
            }
        }

        //csv file we are uploading
        String fileExtension = FilenameUtils.getExtension(file.originalFilename)
        String incomingFileName = FilenameUtils.removeExtension(file.originalFilename)
        def csvFile = File.createTempFile(incomingFileName + "_AP", ".csv", bulkuploadProductDir)
        if (fileExtension && !fileExtension.equals("csv")) {
            flash.error = "csv.error.found"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }else if (!fileExtension) {
            flash.error = "validation.file.upload"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(incomingFileName + "_APError", ".csv", bulkuploadProductDir)

        //copy the uploaded file to a temp file
        file.transferTo(csvFile)

        def executionId = 0
        try {
            //start a batch job to import the Account Type Prices
            executionId = dtWebServicesSession.uploadAccountTypePrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale)
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkUpload/processProductPrices', model: [jobId: executionId, jobStatus: 'busy']
    }

    def uploadCustomerPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        def file = request.getFile('customerPricesFile');
        def bulkuploadProductDir = new File(ProductImportConstants.PRODUCT_FILES_DIR);
        if (!bulkuploadProductDir.exists()) {
            try {
                bulkuploadProductDir.mkdirs();
            } catch (SecurityException se) {
                System.out.println("Exception while creating directory");
            }
        }

        //csv file we are uploading
        String fileExtension = FilenameUtils.getExtension(file.originalFilename)
        String incomingFileName = FilenameUtils.removeExtension(file.originalFilename)
        def csvFile = File.createTempFile(incomingFileName + "_CP", ".csv", bulkuploadProductDir)
        if (fileExtension && !fileExtension.equals("csv")) {
            flash.error = "csv.error.found"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }else if (!fileExtension) {
            flash.error = "validation.file.upload"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(incomingFileName + "_CPError", ".csv", bulkuploadProductDir)

        //copy the uploaded file to a temp file
        file.transferTo(csvFile)

        def executionId = 0
        try {
            //start a batch job to import the Customer Level Prices
            executionId = dtWebServicesSession.uploadCustomerLevelPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale)
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: 'uploadProducts'
        }
        render view: '/bulkUpload/processProductPrices', model: [jobId: executionId, jobStatus: 'busy']
    }

    def uploadPlanPrices() {

        Breadcrumb lastBreadcrumb = breadcrumbService.lastBreadcrumb as Breadcrumb

        def file = request.getFile('planPricesFile');
        def bulkuploadProductDir = new File(PlanImportConstants.PLAN_FILES_DIR);
        if (!bulkuploadProductDir.exists()) {
            try {
                bulkuploadProductDir.mkdirs();
            } catch (SecurityException se) {
                flash.error = "Unable to save incoming file."
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        }

        //csv file we are uploading
        String fileExtension = FilenameUtils.getExtension(file.originalFilename)
        String incomingFileName = FilenameUtils.removeExtension(file.originalFilename)
        def csvFile = File.createTempFile(incomingFileName + "_CP", ".csv", bulkuploadProductDir)
        if (fileExtension && !fileExtension.equals("csv")) {
            flash.error = "csv.error.found"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }else if (!fileExtension) {
            flash.error = "validation.file.upload"
            redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
            return
        }

        //file which will contain a list of errors
        def csvErrorFile = File.createTempFile(incomingFileName + "_PPError", ".csv", bulkuploadProductDir)

        //copy the uploaded file to a temp file
        file.transferTo(csvFile)

        def executionId = 0
        try {
            //start a batch job to import the Plan Prices
            executionId = dtWebServicesSession.uploadPlanPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

            if (executionId == null) {
                flash.error = messageSource.getMessage("bulkloader.plugin.notConfigured.error", null, session.locale)
                redirect(controller: lastBreadcrumb.controller, action: lastBreadcrumb.action, params: [id: lastBreadcrumb.objectId])
                return
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e);
            render view: '_uploadProducts'
        }
        render view: '/bulkUpload/processProductPrices', model: [jobId: executionId, jobStatus: 'busy']
    }

    def downloadTemplateFile(){

        String fileTemplateName = params.actionTaken
        String fileToProcess = StringUtils.EMPTY
        String outputFileName = StringUtils.EMPTY

        switch (fileTemplateName.toLowerCase()){
            case"default":
                fileToProcess = DEFAULT_PRICE_TEMPLATE
                outputFileName = "template_bulkloader_defaultPrices.csv"
                break
            case"accounttype":
                fileToProcess = ACCOUNT_TYPE_TEMPLATE
                outputFileName = "template_bulkloader_accountTypePrices.csv"
                break
            case"customer":
                fileToProcess = CUSTOMER_PRICE_TEMPLATE
                outputFileName = "template_bulkloader_customerPrices.csv"
                break
            case"plan":
                fileToProcess = PLAN_PRICE_TEMPLATE
                outputFileName = "template_bulkloader_planPrices.csv"
                break
            default:
                return
        }

        FileInputStream fileInputStream = new FileInputStream(fileToProcess)
        byte[] csvBytes = IOUtils.toByteArray(fileInputStream)
        DownloadHelper.sendFile(response, outputFileName, "application/csv", csvBytes)
    }

    def downloadPricesFile(){

        String fileTemplateName = params.actionTaken
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
            executionId = dtWebServicesSession.downloadCustomerPrices(csvFile.absolutePath, csvErrorFile.absolutePath)

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
}
