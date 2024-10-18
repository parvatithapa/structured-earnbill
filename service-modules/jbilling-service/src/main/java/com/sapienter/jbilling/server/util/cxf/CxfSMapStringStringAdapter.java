package com.sapienter.jbilling.server.util.cxf;

/**
 * @author Gerhard
 * @since 19/12/13
 */
public class CxfSMapStringStringAdapter extends BaseCxfSortedMapAdapter<String, String, CxfSMapStringString.KeyValueEntry, CxfSMapStringString> {

    @Override
    protected CxfSMapStringString createCxfMap() {
        return new CxfSMapStringString();
    }

    @Override
    protected CxfSMapStringString.KeyValueEntry createEntry() {
        return new CxfSMapStringString.KeyValueEntry();
    }

}
