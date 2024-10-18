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

package com.sapienter.jbilling.server.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.csv.export.event.ReportExportNotificationEvent;
import com.sapienter.jbilling.server.mediation.movius.CDRType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 *
 * @author Harshad Pathan
 * @since 10/24/17
 */
public abstract class BackgroundReportExportUtil {

    public enum Format {
        CSV, XLS
    }

	private static final Logger LOG = LoggerFactory.getLogger(BackgroundReportExportUtil.class);

	public static void generateCSVReportBackground(JasperPrint print, Integer userId, Integer entityId, Map<String, Object> parameters) {

		JRCsvExporter exporter = new JRCsvExporter(); 
		//Creates a new File instance by converting the given pathname string into an abstract pathname.
		File outputFile = new File(getFilePathname(parameters.get("cdrType").toString(), parameters.get("user_id").toString(), ".csv"));

		try (FileOutputStream fos = new FileOutputStream(outputFile)) {
			exporter.setExporterOutput(new SimpleWriterExporterOutput(fos));
			exporter.setExporterInput(new SimpleExporterInput(print));
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.remove.empty.space.between.rows", "true");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.remove.empty.space.between.columns", "true");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.parameters.override.IgnorePagination", "true");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.*", "title");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.1", "pageHeader");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.keep.first.band.1","columnHeader");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.2", "lastPageFooter");
			print.getPropertiesMap().setProperty("net.sf.jasperreports.export.csv.exclude.origin.band.2", "pageFooter");
			
			exporter.exportReport();
			EventManager.process(new ReportExportNotificationEvent(entityId,userId,outputFile.getName(),
					ReportExportNotificationEvent.NotificationStatus.PASSED));
		} catch (IOException | JRException exception) {
			LOG.error("Exception occured while Report genetration {}", exception.getMessage());
			EventManager.process(new ReportExportNotificationEvent(entityId,userId,outputFile.getName(),
					ReportExportNotificationEvent.NotificationStatus.FAILED));
			throw new SessionInternalError(exception);
		} 
	}


	public static void generateXLSReportBackground(List<JasperPrint> jasperPrints,Integer userId,Integer entityId,Map<String, Object> parameters) {

		JRXlsxExporter exporter = new JRXlsxExporter();

		SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
		
		//Creates a new File instance by converting the given pathname string into an abstract pathname.
		File outputFile = new File(getFilePathname("CALLS-And-SMS", parameters.get("user_id").toString(), ".xls"));

		try (FileOutputStream fos = new FileOutputStream(outputFile)) {

			exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrints));
			configuration.setSheetNames(Stream.of(CDRType.values()).map(CDRType::name).toArray(String[]::new));
			configuration.setDetectCellType(false);
			configuration.setWhitePageBackground(false);
			configuration.setRemoveEmptySpaceBetweenColumns(true);
			configuration.setRemoveEmptySpaceBetweenRows(true);
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(fos));
			exporter.setConfiguration(configuration);
			exporter.exportReport();
			EventManager.process(new ReportExportNotificationEvent(entityId,userId,outputFile.getName(),
					ReportExportNotificationEvent.NotificationStatus.PASSED));
		} catch (IOException | JRException exception) {
			LOG.error("Exception occured while Report genetration {}", exception.getMessage());
			EventManager.process(new ReportExportNotificationEvent(entityId,userId,outputFile.getName(),
					ReportExportNotificationEvent.NotificationStatus.FAILED));
			throw new SessionInternalError(exception);
		} 

	}

	public static List<Integer> getCustomersInHierarchy(Integer userId) {
	    IMethodTransactionalWrapper txWrapper = Context.getBean("methodTransactionalWrapper");
	    return txWrapper.execute(() -> {
	        List<Integer> customers = new ArrayList<>();
	        CustomerDTO customer = new UserBL(userId).getEntity().getCustomer();
	        if (customer != null) {
	            customers.add(customer.getId());
	            customer.getChildren().forEach(child -> { if (!child.invoiceAsChild()) {
	                customers.addAll(getCustomersInHierarchy(child.getBaseUser().getId()));}
	            });
	        }
	        return customers;
	    });
	}

	private static String getFilePathname(String cdrType, String userId, String extension ) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss");
		UserDTO userDTO = new UserBL(Integer.valueOf(userId)).getEntity();

		//Finding Organization Id
		MetaFieldValue<?> value = userDTO.getCustomer().getMetaField(MoviusConstants.ORG_ID);
		String organizationId = null != value ? value.getValue().toString() : "";

		//Finding Company Name
		String companyName = userDTO.getCompany().getDescription();
		companyName = null != companyName ? companyName.replaceAll(" ", Constants.UNDERSCORE_DELIMITER) : "";

		//Constructing pathname to store in specific company id folder
	    return new StringBuilder()
			.append(Util.getSysProp(com.sapienter.jbilling.common.Constants.PROPERTY_GENERATE_CSV_FILE_PATH))
			.append(File.separator)
			.append(userDTO.getCompany().getId())
			.append(File.separator)
			.append(companyName)
			.append(Constants.UNDERSCORE_DELIMITER)
			.append(organizationId)
			.append(Constants.UNDERSCORE_DELIMITER)
			.append(cdrType)
			.append(Constants.UNDERSCORE_DELIMITER)
			.append(formatter.format(TimezoneHelper.serverCurrentDate()).replaceAll(" ", Constants.UNDERSCORE_DELIMITER))
			.append(extension).toString();
	}
}
