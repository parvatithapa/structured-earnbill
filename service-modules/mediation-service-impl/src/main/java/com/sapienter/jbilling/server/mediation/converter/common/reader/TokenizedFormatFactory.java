package com.sapienter.jbilling.server.mediation.converter.common.reader;/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

/**
 * Created by marcomanzi on 6/3/14.
 */
public class TokenizedFormatFactory implements FactoryBean<Format> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(TokenizedFormatFactory.class));

    private String formatFilename;
    private MediationTokenizer tokenizer;
    public TokenizedFormatFactory() {

    }

    @Override
    public Format getObject() throws Exception {
        // parse the formatFilename
        LOG.debug("Loading format file %s", formatFilename);

        Digester digester = new Digester();
        digester.setEntityResolver(new EmptyEntityResolver());
        digester.setValidating(true);
        digester.setUseContextClassLoader(true);
        digester.addObjectCreate("format", "com.sapienter.jbilling.server.mediation.converter.common.Format");
        digester.addObjectCreate("format/field", "com.sapienter.jbilling.server.mediation.converter.common.FormatField");
        digester.addCallMethod("format/field/name","setName",0);
        digester.addCallMethod("format/field/type","setType",0);
        digester.addCallMethod("format/field/startPosition","setStartPosition",0);
        digester.addCallMethod("format/field/durationFormat","setDurationFormat",0);
        digester.addCallMethod("format/field/length","setLength",0);
        digester.addCallMethod("format/field/isKey","isKeyTrue");
        digester.addSetNext("format/field", "addField", "com.sapienter.jbilling.server.mediation.converter.common.FormatField");

        Format format = (Format) digester.parse(getClass().getResourceAsStream(formatFilename));
        format.setTokenizer(tokenizer);
        return format;
    }

    @Override
    public Class<?> getObjectType() {
        return List.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getFormatFilename() {
        return formatFilename;
    }

    public void setFormatFilename(String formatFilename) {
        this.formatFilename = formatFilename;
    }

    public MediationTokenizer getTokenizer() {
        return tokenizer;
    }

    public void setTokenizer(MediationTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    private class EmptyEntityResolver implements org.xml.sax.EntityResolver {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String classPath = systemId.split("//")[1];
            return new InputSource(getClass().getResourceAsStream(classPath));
        }
    }
}
