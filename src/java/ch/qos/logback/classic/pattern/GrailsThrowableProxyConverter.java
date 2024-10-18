package ch.qos.logback.classic.pattern;

import java.util.ArrayList;
import java.util.Arrays;

public class GrailsThrowableProxyConverter extends ThrowableProxyConverter {

    private static final String[] DEFAULT_INTERNAL_PACKAGES = new String[] {
            "org.grails.plugin.resource.DevMode",
            "org.codehaus.groovy.grails.",
            "gant.",
            "org.codehaus.groovy.runtime.",
            "org.codehaus.groovy.reflection.",
            "org.codehaus.groovy.ast.",
            "org.codehaus.gant.",
            "groovy.",
            "org.mortbay.",
            "org.apache.catalina.",
            "org.apache.coyote.",
            "org.apache.tomcat.",
            "net.sf.cglib.proxy.",
            "sun.",
            "java.lang.reflect.",
            "org.springframework.",
            "org.springsource.loaded.",
            "com.opensymphony.",
            "org.hibernate.",
            "javax.servlet."
        };

    private void ignoreDefaultInternalPackages () {
        if (ignoredStackTraceLines == null) {
            ignoredStackTraceLines = new ArrayList<String>();
        }
        ignoredStackTraceLines.addAll(Arrays.asList(DEFAULT_INTERNAL_PACKAGES));
    }

    @Override
    public void start () {
        ignoreDefaultInternalPackages();
        super.start();
    }
}
