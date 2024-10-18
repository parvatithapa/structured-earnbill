package com.sapienter.jbilling.server.mediation.sapphire.batch;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.sapphire.cdr.SapphireCdrCreator;
import com.sapienter.jbilling.server.mediation.sapphire.model.CallType;
import com.sapienter.jbilling.server.mediation.sapphire.model.FileType;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class SapphireMediationReader implements ItemReader<ICallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<Object> cdrs = new ArrayList<>();

    @Value("#{jobParameters['filePath']}")
    private String fileName;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    private Unmarshaller cdrParser;

    private SapphireCdrCreator cdrCreator;

    public SapphireMediationReader(SapphireCdrCreator cdrCreator, Unmarshaller cdrParser) {
        this.cdrCreator = cdrCreator;
        this.cdrParser  = cdrParser;
    }

    @SuppressWarnings("unchecked")
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws JAXBException {
        try {
            logger.debug("Reading cdr from file {} for entity {}", fileName, entityId);
            JAXBElement<FileType> file = (JAXBElement<FileType>) cdrParser.unmarshal(new File(fileName));
            cdrs.addAll(file.getValue().getCDRs().getCallOrEvent());
        } catch (JAXBException e) {
            logger.error("CDR Parsing failed!", e);
            throw e;
        }
    }

    @Override
    public synchronized ICallDataRecord read() {
        if(CollectionUtils.isNotEmpty(cdrs)) {
            CallType cdr = (CallType) cdrs.remove(0);
            logger.trace(" Reader Method Returned {} for entity {}", cdr, entityId);
            return cdrCreator.createJbillingCdr(cdr);
        }
        return null;
    }

}
