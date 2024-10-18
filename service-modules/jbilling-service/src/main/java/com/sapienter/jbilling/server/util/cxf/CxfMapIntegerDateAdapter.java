package com.sapienter.jbilling.server.util.cxf;

import java.util.Date;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public class CxfMapIntegerDateAdapter extends BaseCxfMapAdapter<Integer, Date, CxfMapIntegerDate.KeyValueEntry, CxfMapIntegerDate> {

    @Override
    protected CxfMapIntegerDate createCxfMap() {
        return new CxfMapIntegerDate();
    }

    @Override
    protected CxfMapIntegerDate.KeyValueEntry createEntry() {
        return new CxfMapIntegerDate.KeyValueEntry();
    }

}
