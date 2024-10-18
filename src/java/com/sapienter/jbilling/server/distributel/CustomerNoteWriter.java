package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.CustomerNoteDAS;
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;

public class CustomerNoteWriter implements ItemWriter<CustomerNoteDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Override
    public void write(List<? extends CustomerNoteDTO> notes) {
        CustomerNoteDAS noteDAS = new CustomerNoteDAS();
        for(CustomerNoteDTO note : notes) {
            note = noteDAS.save(note);
            logger.debug("Note {} saved for user {} for entity Id {}", note, note.getUser().getId(), entityId);
        }
    }

}
