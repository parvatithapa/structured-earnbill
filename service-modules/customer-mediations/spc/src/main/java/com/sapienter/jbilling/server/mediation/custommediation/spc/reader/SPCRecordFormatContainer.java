package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import org.springframework.util.Assert;
import com.sapienter.jbilling.server.mediation.converter.common.Format;

/**
 * @author Neelabh
 * @since Dec 18, 2018
  */
public class SPCRecordFormatContainer {

    private Format format;
    private String datePattern;

    public SPCRecordFormatContainer(Format format, String datePattern) {
        Assert.notNull(format, "Format Can not be Null !");
        Assert.hasLength(datePattern, "Date Pattern Can not be Null !");
        this.format = format;
        this.datePattern = datePattern;
    }

    public Format getFormat() {
        return format;
    }

    public String getDatePattern() {
        return datePattern;
    }

}
