package com.sapienter.jbilling.server.util.cxf;

/**
 * 
 * @author jbilling-pranay
 * Date: 26-03-2019
 *
 */
public class CxfSMapStringObjectAdapter extends BaseCxfMapAdapter<String, Object, CxfSMapStringObject.StringObjectEntry, CxfSMapStringObject> {

    @Override
    protected CxfSMapStringObject createCxfMap() {
        return new CxfSMapStringObject();
    }

    @Override
    protected CxfSMapStringObject.StringObjectEntry createEntry() {
        return new CxfSMapStringObject.StringObjectEntry();
    }

}
