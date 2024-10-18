package com.sapienter.jbilling.server.mediation.processor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.OrderService;

/**
 * Created by marcolin on 13/10/15.
 */
public class JMRProcessorWriterImpl implements ItemWriter<JbillingMediationRecord> {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(JMRProcessorWriterImpl.class));

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Autowired
    private OrderService orderService;

    public void setJmrRepository(JMRRepository jmrRepository) {
        this.jmrRepository = jmrRepository;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void write(List<? extends JbillingMediationRecord> list) throws Exception {
		/**
         * The code of this method should be included inside try/catch block
         * because if one JMR fails midway then it turns rest of others JMRs
         * into same status especially when anything goes wrong during process.
         * Having exception handling here makes mediation fault-tolerant too.
         */
    	for (JbillingMediationRecord jmr: list) {
    		MediationEventResult mediationEventResult = orderService.addMediationEvent(jmr);
			if(!mediationEventResult.hasException()) {
				if (mediationEventResult.hasQuantityEvaluated()) {
    				jmr.setOrderId(mediationEventResult.getCurrentOrderId());
    				jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
    				jmr.setRatedPrice(mediationEventResult.getAmountForChange());
    				jmr.setRatedCostPrice(mediationEventResult.getCostAmountForChange());
    				jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
				} else {
					jmr.setOrderId(mediationEventResult.getCurrentOrderId());
					jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
					jmr.setRatedPrice(mediationEventResult.getAmountForChange());
					jmr.setStatus(JbillingMediationRecord.STATUS.NOT_BILLABLE);
				}
				updateMediationRecord(jmr);
			} else {
				LOG.error("Exception occurred while processing JMR for CDR key '"+jmr.getRecordKey()+"' : "
						+ mediationEventResult.getExceptionMessage());
				jmErrorRepository.save(DaoConverter.getMediationErrorRecordDao(jmr));
				jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
			}
    		sendMetric(jmr);
    	}
    }

    private static void sendMetric(JbillingMediationRecord callDataRecord) {
        try {
            MetricsHelper.log("Readed JMR: " + callDataRecord.toString(),
                    InetAddress.getLocalHost().toString(),
                    MetricsHelper.MetricType.ORDER_CREATED.name());
        } catch (Exception e) {}
    }


    public JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(
                jmrRepository.save(DaoConverter.getMediationRecordDao(jbillingMediationRecord)));
    }
}
