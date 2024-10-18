import groovy.io.FileType

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

includeTargets << grailsScript("_GrailsDocs")
includeTargets << new File("${basedir}/scripts/SetLicense.groovy")
includeTargets << new File("${basedir}/scripts/CheckDbConnection.groovy")

eventCreateWarStart = { warName, stagingDir ->
    println("Compiling documentation ...")
}

eventSetClasspath = {
//  uncomment if you want to see the entire classpath on compile/startup
//    println("eventSetClasspath:")
//    printClassPath classLoader
}

eventCompileEnd = { msg ->
    def sourceDir = "${basedir}/grails-app/conf/spring"
    def destination = "${classesDirPath}/spring"
    println "after compile: copying spring beans ... $sourceDir => $destination"

    ant.copy(todir: destination, verbose: true){
        fileset(dir: sourceDir){
            include(name: '*Beans.groovy')
        }
    }
    setLicense()
    checkDbConnection()
}

eventStatusUpdate = { msg ->
    if (msg == "Running Grails application") {
        setLicense()
//        println("eventStatusUpdate:")
//        printClassPath classLoader
        def extDir = new File("${basedir}/ext")
        if (extDir.exists()) {
            extDir.eachFileRecurse (FileType.FILES) { file ->
                println "Extending classpath with custom classes from ${file}"
                addURLsInClassLoader classLoader, file
            }
        }
    }
}

def addURLsInClassLoader (classLoader, file) {
    classLoader.addURL(file.toURI().toURL())
}

def printClassPath (classLoader) {
    println "$classLoader"
    classLoader.getURLs().each {url->
        println "- ${url.toString()}"
    }
    if (classLoader.parent) {
        printClassPath(classLoader.parent)
    }
}

