package com.sapienter.jbilling.log;

import java.util.Map;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.GrailsThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

public class PatternLayoutEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {

    static {
        Map<String, String> defaultConverterMap = PatternLayout.defaultConverterMap;
        defaultConverterMap.put("ex", GrailsThrowableProxyConverter.class.getName());
    }

    @Override
    public void start() {
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(getPattern());
        patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
        patternLayout.start();
        this.layout = patternLayout;
        super.start();
    }
}
