package com.sapienter.jbilling.server.mediation.converter.common.job;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemWriter;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.ConversionResult;

/**
 * @since 08-25-16
 * @author Krunal Bhavsar
 *
 */
public class JMRLogWriter implements ItemWriter<ConversionResult> {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(JMRLogWriter.class));
	
	public void write(List<? extends ConversionResult> list) throws Exception {
		list.forEach(
				result -> 
				LOG.debug("CDR -> "+ result.getRecordProcessed().getKey() +" Converted To JMR ") );
	}

}
