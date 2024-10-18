package com.sapienter.jbilling.server.mediation.customMediations.movius.reader;

import org.springframework.util.Assert;
import com.sapienter.jbilling.server.mediation.converter.common.Format;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class MoviusRecordFormatContainer {
    
    private Format format;
    private String datePattern;
    
    public MoviusRecordFormatContainer(Format format, String datePattern) {
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
