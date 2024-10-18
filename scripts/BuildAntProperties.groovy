/*
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

includeTargets << grailsScript("_GrailsClasspath")

target(default: "Create properties file with grails properies for ant") {
    depends(classpath)

    ant.path(id: "common.classpath",  commonClasspath)
    ant.path(id: "compile.classpath", compileClasspath)
    ant.path(id: "test.classpath",    testClasspath)
    ant.path(id: "runtime.classpath", runtimeClasspath)

    File file = new File('grails-ant.properties')
    def out = file.newWriter("UTF-8")
    try {
        dumpClasspathAsProperty(out, "common.classpath")
        dumpClasspathAsProperty(out, "compile.classpath")
        dumpClasspathAsProperty(out, "test.classpath")
        dumpClasspathAsProperty(out, "runtime.classpath")

        out << "build.classes.dir="+escapeJava("${grailsSettings.config.grails.project.class.dir}")+"\n"
        out << "build.test.dir="+escapeJava("${grailsSettings.config.grails.project.test.class.dir}")+"\n"
        out << "build.test-results.dir="+escapeJava("${grailsSettings.config.grails.project.test.reports.dir}")+"\n"
        out << "javac.target.level="+escapeJava("${grailsSettings.config.grails.project.target.level}")+"\n"
        out << "javac.source.level="+escapeJava("${grailsSettings.config.grails.project.source.level}")+"\n"
    } finally {
        out.close()
    }
}

def dumpClasspathAsProperty (def out, String pathId) {
    String strPath = escapeJava(ant.project.references.get(pathId).toString())
    out << "${pathId}=${strPath}\n"
}

String escapeJava(String s) {
    s.replaceAll("\\\\", "\\\\\\\\")
}
