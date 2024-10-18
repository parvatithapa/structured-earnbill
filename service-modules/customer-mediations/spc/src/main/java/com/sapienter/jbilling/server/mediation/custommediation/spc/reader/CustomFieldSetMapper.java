package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import org.springframework.batch.item.file.transform.FieldSet;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;

/**
 * @author Neelabh Dubey
 * @since Jan 23, 2019
 */
public interface CustomFieldSetMapper<T extends ICallDataRecord> extends FieldSetMapper<T> {
    public ICallDataRecord mapLineToRecord(FieldSet fieldSet, MediationServiceType serviceType);
}
