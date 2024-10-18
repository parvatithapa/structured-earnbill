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


includeTargets << grailsScript("_GrailsInit")


isInternalLicensee  = { licensee, licenseKey ->
    ("jbilling.com".equals(licensee)) && licenseKey.startsWith("CxDQ") && licenseKey.endsWith("wIHM")
}


replaceLicenseTokens = { fileName ->
    ant.replace(file: fileName, propertyFile: "license.txt") {
        replacefilter(token: "licensee name", property: "licensee")
        replacefilter(token: "place license key here", property: "licenseKey")
    }
}


copyAndReplaceLicenseTokens = { dirName ->
    ant.mkdir(dir: dirName)
    ant.copy(file: "${basedir}/src/java/jbilling.properties", toDir: dirName, overwrite: "yes", verbose: "yes")
    replaceLicenseTokens("${dirName}/jbilling.properties")
}


target(setLicense: "Set the license key in jbilling.properties with whatever is in license.txt") {

    ant.available(file: "license.txt", property: "licenseAvailable")

    if (ant.project.getProperty("licenseAvailable")) {
        println "Setting license in jbilling.properties from license.txt"

        ant.loadproperties(srcFile:"license.txt")

        if (! isInternalLicensee(ant.project.getProperty("licensee"), ant.project.getProperty("licenseKey"))) {
            replaceLicenseTokens("${basedir}/src/java/jbilling.properties")
        }
        copyAndReplaceLicenseTokens("${projectWorkDir}/resources/") // for "grails run-app"
    }
}
