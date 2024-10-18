package com.sapienter.jbilling.batch.support;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

public class NoOpWriter implements ItemWriter<Integer> {

    @Override
    public void write (List<? extends Integer> list) {
        // no-op
    }
}
