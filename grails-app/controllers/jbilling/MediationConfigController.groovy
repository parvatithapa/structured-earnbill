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

import com.sapienter.jbilling.client.util.DownloadHelper
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.common.Util
import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord
import com.sapienter.jbilling.server.mediation.MediationConfigurationBL
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS
import com.sapienter.jbilling.server.mediation.MediationProcess
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS
import com.sapienter.jbilling.server.mediation.MediationService
import com.sapienter.jbilling.server.mediation.MediationVersion
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration
import com.sapienter.jbilling.server.mediation.task.AbstractFileReader
import com.sapienter.jbilling.server.mediation.task.IMediationReader
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO
import com.sapienter.jbilling.server.pricing.db.RouteDAS
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.timezone.TimezoneHelper
import com.sapienter.jbilling.server.user.db.CompanyDAS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.PreferenceBL
import com.sapienter.jbilling.server.util.SecurityValidator
import com.sapienter.jbilling.server.util.csv.CsvExporter
import com.sapienter.jbilling.server.util.csv.Exporter

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

/**
 * MediationConfigController
 *
 * @author Vikas Bodani
 * @since 15-Feb-2011
 */

@Secured(["MENU_99"])
class MediationConfigController {

    static pagination = [max: 10, offset: 0]

    MediationService mediationService
    def webServicesSession
    def viewUtils
    def breadcrumbService
    PluggableTaskDAS pluggableTaskDAS
    SecurityValidator securityValidator


    def index() {
        flash.invalidToken = flash.invalidToken
        redirect action: 'list'
    }

    def list() {
		
        def lastMediationProcessStatus

        def isMediationProcessRunning = webServicesSession.isMediationProcessRunning();
        if (isMediationProcessRunning) {
            flash.info = 'mediation.config.prompt.running'
        } else {
            lastMediationProcessStatus = webServicesSession.getMediationProcessStatus()
        }
        def configurations = webServicesSession.getAllMediationConfigurations() as List

        def hasNonGlobalConfig = false
        for (MediationConfigurationWS config : configurations) {
            hasNonGlobalConfig |= !config.global
        }

        breadcrumbService.addBreadcrumb(controllerName, actionName, null, params.int('id'))

        def usingJQGrid = PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], Constants.PREFERENCE_USE_JQGRID);
        //If JQGrid is showing, the data will be retrieved when the template renders
        if (usingJQGrid) {
            if (params.applyFilter) {
                render template: 'configsTemplate', model: [isMediationProcessRunning : isMediationProcessRunning,
                                                           lastMediationProcessStatus: lastMediationProcessStatus,
                                                            hasNonGlobalConfig        : hasNonGlobalConfig, mediationConfigurationWS: chainModel?.mediationConfiguration]
            } else {
                render view: 'list', model: [isMediationProcessRunning : isMediationProcessRunning,
                                             lastMediationProcessStatus: lastMediationProcessStatus,
                                             hasNonGlobalConfig        : hasNonGlobalConfig]
            }
            return
        }

        if (params.applyFilter) {
            render template: 'configsTemplate', model: [configs                  : configurations, readers: readers,
                    isMediationProcessRunning: isMediationProcessRunning, lastMediationProcessStatus: lastMediationProcessStatus,
                                                        hasNonGlobalConfig       : hasNonGlobalConfig]
		} else {
            MediationConfiguration selected = null
            if (params.id) {
                selected = MediationConfiguration.get(params.int('id'))
                securityValidator.validateUserAndCompany(MediationConfigurationBL.getWS(selected), Validator.Type.VIEW)
            }
            render view: 'list', model: [selected                 : selected, mediationConfigurationWS: chainModel?.mediationConfiguration, configs: configurations, readers: readers,
                    isMediationProcessRunning: isMediationProcessRunning, lastMediationProcessStatus: lastMediationProcessStatus,
                                         hasNonGlobalConfig       : hasNonGlobalConfig, processors: processors]
		}
    }

    def findMediationConfigs() {
        def configurations = webServicesSession.getAllMediationConfigurations() as List

        try {
            def jsonData = getAsJsonData(configurations, params)

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
        def rowsNumber = params.rows ? Integer.valueOf(params.rows) : 1
        def totalRecords =  jsonCells ? jsonCells.size() : 0
        def numberOfPages = Math.ceil(totalRecords / rowsNumber)

        def jsonData = [rows: jsonCells, page: currentPage, records: totalRecords, total: numberOfPages]

        jsonData
    }

    def show() {

        def configId = params.int('id')
        MediationRatingSchemeWS mediationRatingScheme
        Integer companyId = CompanyDTO.get(session['company_id']).id
        Integer ratingSchemeId = webServicesSession.getRatingSchemeForMediationAndCompany(configId, companyId)
        //Associated Rating Scheme
        if (ratingSchemeId) {
            mediationRatingScheme = webServicesSession.getRatingScheme(ratingSchemeId)
        }
        log.debug "Show config id $params.id"
        
        def config = MediationConfiguration.get(configId)
        securityValidator.validateUserAndCompany(MediationConfigurationBL.getWS(config), Validator.Type.VIEW)
        def fileInjectionEnabled;

        switch (MediationVersion.getMediationVersion(Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION))) {
            case MediationVersion.MEDIATION_VERSION_2_0:
                PluggableTaskBL<IMediationReader> readerTask = new PluggableTaskBL<IMediationReader>();
                readerTask.set(config.getPluggableTask());
                if (config.getPluggableTask() != null) {
                    IMediationReader reader = readerTask.instantiateTask();
                    fileInjectionEnabled = (reader instanceof AbstractFileReader);
                }

                break;
            case MediationVersion.MEDIATION_VERSION_3_0:
            case MediationVersion.MEDIATION_VERSION_4_0:
                // by default enable file injection in mediation 3.0
                fileInjectionEnabled = true;
				try {
                    Long cdrForRecycleCount = webServicesSession.getMediationErrorRecordsCount(configId)
                    config?.cdrsForRecycle = cdrForRecycleCount
                } catch (Exception e) {/*TODO do nothing right now.*/
                }
				break;
        }

        if (config.entityId == session['company_id'] ||
                (config.global && config.entityId == CompanyDTO.get(session['company_id']).parent.id)) {

            //on show rememeber list breadcrumb with given parameter
            breadcrumbService.addBreadcrumb(controllerName, 'list', null, configId)
            render template: 'show', model: [selected: config, fileInjectionEnabled: fileInjectionEnabled, mediationRatingScheme: mediationRatingScheme]

        } else {
            flash.error = 'configuration.does.not.exists.for.entity'
            list()
        }
    }
	
    def edit() {
        
        def configId = params.int('id')
        
        MediationConfigurationWS mediationConfigurationWS = configId ? MediationConfigurationBL.getWS(MediationConfiguration.get(configId)) : new MediationConfigurationWS()
        securityValidator.validateCompany(mediationConfigurationWS?.entityId, Validator.Type.EDIT)
		def config = configId ? MediationConfiguration.get(configId) : null
        def crumbName = configId ? 'update' : 'create'
        def crumbDescription = params.id ? config?.name : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, configId, crumbDescription)
        def entityId = session['company_id']
        def routes = new RouteDAS().getRootRoutes(entityId)
        def global = new CompanyDAS().getChildEntitiesIds(entityId)?.isEmpty() ? false : true

        render template: 'edit', model: [config: mediationConfigurationWS, readers: readers, processors: processors, routes: routes, global: global]
    }

    def listEdit() {

        def configId = params.int('id')
        
        def config = configId ? MediationConfiguration.get(configId) : null
        securityValidator.validateCompany(config?.entityId, Validator.Type.VIEW)
        def configurations = webServicesSession.getAllMediationConfigurations() as List
		
        def crumbName = configId ? 'update' : 'create'
        def crumbDescription = params.id ? config?.name : null
        breadcrumbService.addBreadcrumb(controllerName, 'listEdit', crumbName, configId, crumbDescription)
        
        render view: 'listEdit', model: [configs: configurations, readers: readers, config: config]
    }

    def save() {

        def ws = new MediationConfigurationWS()
        bindData(ws, params)
        securityValidator.validateCompany(ws?.entityId, Validator.Type.EDIT)
        Boolean isSaved = true

        if (params.int('id') <= 0) {
            ws.setCreateDatetime TimezoneHelper.serverCurrentDate()
            ws.setEntityId webServicesSession.getCallerCompanyId()
        }

        try {
            ws.id = webServicesSession.createMediationConfiguration(ws)

            if (isSaved) {
                flash.message = ws.id < 0 ? 'mediation.config.create.success' : 'mediation.config.update.success'
            }
        } catch (SessionInternalError e) {
            viewUtils.resolveException(flash, session.locale, e)
            isSaved = false;
        }

        chain action: 'list',
              params: [id: ws?.id > 0 ? ws?.id : null],
               model: [mediationConfiguration: !isSaved ? ws : null]
	}

    def delete() {
        def config = MediationConfiguration.get(params.int('id'))
        securityValidator.validateCompany(config?.entityId, Validator.Type.EDIT)

        try {
			webServicesSession.deleteMediationConfiguration(params.int('id'))
			flash.message = 'mediation.config.delete.success'
        } catch (SessionInternalError e) {
			viewUtils.resolveExceptionMessage(flash, session.locale, e);
		} catch (Exception e) {
			log.error e.getMessage()
			flash.error = 'mediation.config.delete.failure'
		}

		// render list
		params.applyFilter = true
		redirect action: 'list'

	}


    def run() {
        try {
            if (!webServicesSession.isMediationProcessRunning()) {
                webServicesSession.triggerMediation()
                flash.message = 'mediation.config.prompt.trigger'
            } else {
                flash.error = 'mediation.config.prompt.running'
            }
        } catch (Exception e) {
            log.error e.getMessage()
            viewUtils.resolveException(flash, session.locale, e);
        }

        params.applyFilter = null

        redirect action: 'list'
    }

    def showInject() {

        def configId = params.int('id')
        def config = configId ? MediationConfiguration.get(configId) : null
        securityValidator.validateCompany(config?.entityId, Validator.Type.VIEW)
        def fileInjectionEnabled = params.boolean('fileInjectionEnabled')

        render template: 'inject', model: [config: config, fileInjectionEnabled: fileInjectionEnabled]
    }

    def doInject() {

        def configId = params.int('id')
        def entityId = params.int('entityId')
		def eventFile = request.getFile("events")
		MediationConfiguration mdConfig =  MediationConfiguration.get(configId);
        securityValidator.validateCompany(mdConfig?.entityId, Validator.Type.EDIT)
        PluggableTaskDTO plDto = mdConfig.getPluggableTask();
        boolean doRun = true
        if (!eventFile?.getOriginalFilename() && !params?.recordsString) {
			flash.error = 'validation.file.or.records.upload'
			doRun = false
        } else if (eventFile && !eventFile?.isEmpty()) {
            switch (MediationVersion.getMediationVersion(Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION))) {
				case MediationVersion.MEDIATION_VERSION_2_0:
                    if (eventFile) {
						for (PluggableTaskParameterDTO plParamDto : plDto.getParameters()) {
                            if (plParamDto.getName().equals("suffix")) {
								if (!eventFile.getOriginalFilename().endsWith(plParamDto.getValue())) {
									flash.error = 'validation.file.extension'
									doRun = false
									break;
								}
							}
						}
					}
					break;
				case MediationVersion.MEDIATION_VERSION_3_0:
                case MediationVersion.MEDIATION_VERSION_4_0:
                    // Removed the validation of CSV file extension since, Distributal uses INV file extension
                    // TODO add validation in future
                    break;
            }
        }
        def fileInjectionEnabled = params.boolean('fileInjectionEnabled')
        def recordsString = params.recordsString
        try {
            if (doRun) {
				log.debug "mediation will be triggered or attempted."
				if (fileInjectionEnabled) {
                    File temp = new File(System.getProperty("java.io.tmpdir"), eventFile.fileItem.name);
					eventFile.transferTo(temp)
					log.debug("Injected event file saved to: " + temp?.getAbsolutePath());
					webServicesSession.triggerMediationByConfigurationByFile(configId, temp)
				} else {
					if (recordsString) {
                        mediationService.launchMediationForCdr(entityId, configId, mdConfig.getMediationJobLauncher(), recordsString)
	                } else {
                        doRun = false
	                    flash.error = 'mediation.config.inject.record.failure'
	                }
	            }
			}
        } catch (SessionInternalError e) {
            doRun = false
            viewUtils.resolveException(flash, session.locale, e);
        } catch (Exception e) {
            doRun = false
            log.error e.getMessage()
            flash.error = 'mediation.config.inject.failure'
        }

        params.applyFilter = null

        if (!doRun) {
            def configurations = webServicesSession.getAllMediationConfigurations()
            def lastMediationProcessStatus = webServicesSession.getMediationProcessStatus()
            def hasNonGlobalConfig = false
            for (MediationConfigurationWS config : configurations) {
                hasNonGlobalConfig |= !config.global
            }

            if (fileInjectionEnabled) {
                render view: 'list', model: [mediationConfigurationWS  : chainModel?.mediationConfiguration, configs: configurations, readers: readers,
                                             lastMediationProcessStatus: lastMediationProcessStatus, insertRecord: true,
                                             hasNonGlobalConfig        : hasNonGlobalConfig, fileInjectionEnabled: true, config: mdConfig]
			} else {
                render view: 'list', model: [mediationConfigurationWS  : chainModel?.mediationConfiguration, configs: configurations, readers: readers,
                                             lastMediationProcessStatus: lastMediationProcessStatus, insertRecord: true,
                                             hasNonGlobalConfig        : hasNonGlobalConfig, config: mdConfig, recordsString: recordsString]
			}
        } else {
            redirect action: 'list'
        }
    }

    def recycleCDRs() {
		Integer configId;
		try {
            configId = params.id as Integer
			log.debug "Triggering recycle mediation for config ID ${configId}"
			
            webServicesSession.runRecycleForConfiguration(configId)
			
        } catch (SessionInternalError e) {
			viewUtils.resolveException(flash, session.locale, e);
		} catch (Exception e) {
			log.error e.getMessage()
			flash.error = 'mediation.config.recycle.failure'
			flash.args = [configId]
			return
		}

		flash.info = 'mediation.config.recycle.running'
		flash.args = [configId]
        redirect action: 'list'
	}
	
    def getReaders() {
        
        def readers = new ArrayList<PluggableTaskDTO>()
        securityValidator.validateCompany(session.company_id, Validator.Type.VIEW)
        CompanyDTO company = CompanyDTO.get(session.company_id)
        if (session.company_id) {
            readers = pluggableTaskDAS.findByEntityCategory(session.company_id as Integer, Constants.PLUGGABLE_TASK_MEDIATION_READER);
        }
        return readers
    }

    def getProcessors() {
        def processors = new ArrayList<>()

        Integer languageId = session.language_id;
        Integer entityId = session.company_id;
        securityValidator.validateCompany(session.company_id, Validator.Type.VIEW)
        CompanyDTO company = CompanyDTO.get(session.company_id)
        processors = pluggableTaskDAS.findByEntityCategory(entityId, Constants.PLUGGABLE_TASK_MEDIATION_PROCESS);

        return processors
    }

    def showAllMediationErrorOfMediationConfig() {
        def errorCodes = params.errorCodes ? (params.errorCodes.length() == 0 ? null : params.errorCodes.split(':') as List) : null
        MediationConfiguration mediationConfiguration = MediationConfiguration.get(params.id)
        securityValidator.validateCompany(mediationConfiguration?.entityId, Validator.Type.VIEW)
        def offset = params.offset ?: null
        if (params.first == 'true') {
            offset = null
        }
        //TODO MODULARIZATION: retrieve error records with this fields
        def mediationErrorRecords = mediationService.getMediationErrorRecordsForMediationConfigId(mediationConfiguration?.id);
        def pricingFieldsHeader = JbillingMediationErrorRecord.getPricingHeaders(mediationErrorRecords).sort()
        Map<UUID, Date> mediationProcess = new HashMap<>()
        mediationErrorRecords?.each { error ->
            if (mediationProcess.containsKey(error.processId)) {
                error.setProcessingDate(mediationProcess.get(error.processId))
            } else {
                MediationProcess process = webServicesSession.getMediationProcess(error.processId);
                mediationProcess.put(process.id, process.startDate)
                error.setProcessingDate(process.startDate)
            }
        }

        def record
        if (mediationErrorRecords) {
            record = mediationErrorRecords?.get(0)
        } else {
            flash.info = message(code: 'event.mediation.config.records.not.available')
            flash.args = [params.id]
        }

        render view: 'errors', model: [records               : mediationErrorRecords,
                                          pricingFieldsHeader : pricingFieldsHeader,
                                       mediationConfiguration: mediationConfiguration,
                                                       record : record,
                                                   errorCodes : errorCodes,
                                                       offset : offset,
                                                    startDate : params.startDate,
                                                      endDate : params.endDate]
    }

    def mediationErrorsCsv() {
        MediationConfiguration mediationConfiguration = MediationConfiguration.get(params.id)
        securityValidator.validateCompany(mediationConfiguration?.entityId, Validator.Type.VIEW)

        def mediationErrorRecords = mediationService.getMediationErrorRecordsForMediationConfigId(mediationConfiguration?.id);
        params.max = CsvExporter.MAX_RESULTS

        if (mediationErrorRecords.size() > CsvExporter.MAX_RESULTS) {
            flash.error = message(code: 'error.export.exceeds.maximum')
            flash.args = [CsvExporter.MAX_RESULTS]
            redirect action: 'list', id: params.id
        } else {
            DownloadHelper.setResponseHeader(response, "mediation_errors.csv")
            Exporter<JbillingMediationErrorRecord> exporter = CsvExporter.createExporter(JbillingMediationErrorRecord.class);
            render text: exporter.export(mediationErrorRecords), contentType: "text/csv"
        }
    }

    private getMediationErrorKey(JbillingMediationErrorRecord record, MediationConfiguration mediationConfiguration) {
        //TODO MODULARIZATION: why we had this logic?
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(mediationConfiguration.getId()).append("-");
        keyBuilder.append(record.processId).append("-");
        keyBuilder.append(record.recordKey);
        return keyBuilder.toString();
    }

}
