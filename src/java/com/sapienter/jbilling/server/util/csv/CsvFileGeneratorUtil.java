package com.sapienter.jbilling.server.util.csv;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * CsvFileGeneratorUtil
 *
 * @author Ashok Kale
 * @since 02/11/14
 */
public class CsvFileGeneratorUtil {

    private CsvFileGeneratorUtil() {}

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	 /**
	  * Generate csv file on this basis of csvType
	  */
	 public static void generateCSV(final String csvType, final List<? extends Exportable> values, Integer entityId, Integer exporterUserId) {
		try {
				if( StringUtils.isNotEmpty(csvType) ) {
					export(values, entityId, csvType,exporterUserId);
				}
			} catch(Exception ex) {
			    logger.error("Exception occurred while generating CSV for entity Id {} and CSV type {} ", entityId, csvType, ex);
			}
	 	}

	 /**
	  * This common generic method is responsible to generate
	  * csv file on this basis of csv type e.g. Users and Orders.
	  * If any new csv types come please add in this method.
	  */
	@SuppressWarnings("unchecked")
    public static String export(List<? extends Exportable> list,
			Integer entityId, String type,Integer exporterUserId) {
		if (type.equals(Constants.USER_CSV)) {
			Exporter<UserDTO> exporter = CsvFileExporter.createExporter(UserDTO.class, entityId, type,exporterUserId);
			return exporter.export((List<UserDTO>) list);
		} else if (type.equals(Constants.ORDER_CSV)) {
			Exporter<OrderDTO> exporter = CsvFileExporter.createExporter(OrderDTO.class, entityId, type,exporterUserId);
			return exporter.export((List<OrderDTO>) list);
		} else if (type.equals(Constants.INVOICE_CSV)) {
		Exporter<InvoiceDTO> exporter = CsvFileExporter.createExporter(InvoiceDTO.class, entityId, type,exporterUserId);
		return exporter.export((List<InvoiceDTO>) list);
		} else if (type.equals(Constants.PAYMENT_CSV)) {
			Exporter<PaymentDTO> exporter = CsvFileExporter.createExporter(PaymentDTO.class, entityId, type,exporterUserId);
			return exporter.export((List<PaymentDTO>) list);
		}
		return StringUtils.EMPTY;
	}
}
