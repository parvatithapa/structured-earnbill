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

final targetDir = "${basedir}/target"

target (clean: 'Clean out old hyperoptic classes, jar files') {
    println 'Cleaning old hyperoptic classes..'
    delete(file: "${targetDir}/${grailsAppName}-hessian-${grailsAppVersion}.jar")
}

target(hessianjar: "Packages hessian serializers n a .jar file.") {
    
    tstamp()
    println 'generating jar'
    Ant.jar(destfile: "${targetDir}/${grailsAppName}-hessian-${grailsAppVersion}.jar", basedir: "${basedir}/resources/hessian-jar") {
        manifest {
            attribute(name: "Built-By", value: System.properties.'user.name')
            attribute(name: "Built-On", value: "${DSTAMP}-${TSTAMP}")
            attribute(name: "Specification-Title", value: grailsAppName)
            attribute(name: "Specification-Version", value: grailsAppVersion)
            attribute(name: "Specification-Vendor", value: "jBilling.com")
            attribute(name: "Package-Title", value: grailsAppName)
            attribute(name: "Package-Version", value: grailsAppVersion)
            attribute(name: "Package-Vendor", value: "jBilling.com")
        }
    }
    println 'Build Successful'
}

target(main: "Create jBilling hessian patch jar file.") {
    println 'hessian patch jar  - main'
    depends(clean, hessianjar)
}

setDefaultTarget(main)
