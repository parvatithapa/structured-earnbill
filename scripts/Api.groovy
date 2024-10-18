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
final targetAPIDir = "${basedir}/target/api"
final apiClassesDir = "${targetDir}/api/classes"

target (clean: 'Clean out old api classes, jar files') {
    println 'Cleaning old api classes..'
    delete(dir: apiClassesDir)
    delete(dir: targetAPIDir, includes: "**/*")
    ant.mkdir(dir: apiClassesDir)

}

apiClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    def dependencies = grailsSettings.runtimeDependencies
    if (dependencies) {
        for (File f in dependencies) {
            pathelement(location: f.absolutePath)
        }
    }

	dependencies = grailsSettings.buildDependencies
    if (dependencies) {
        for (File f in dependencies) {
            pathelement(location: f.absolutePath)
        }
    }

    pathelement(location: "${pluginClassesDir.absolutePath}")
}

target(api: "Packages all WS Client jbilling classes in a .jar file.") {

    tstamp()
    ant.path(id: "api.classpath", apiClasspath)

    println "compiling api classes only.. from ${basedir}/src/java to ${targetAPIDir}/classes"

    Ant.groovyc(srcdir: "${basedir}/src/java:${basedir}/src/groovy:${basedir}/grails-app/services/jbilling:${basedir}/grails-app/domain/jbilling",
                destdir: "${targetAPIDir}/classes", classpathref: "api.classpath") {

        Ant.javac() {

            include(name: "com/sapienter/jbilling/client/util/*.java")
            include(name: "com/sapienter/jbilling/server/**/*WS.java")
            include(name: "com/sapienter/jbilling/server/util/api/*.java")
            exclude(name: "com/sapienter/jbilling/server/**/*BL.java")
            exclude(name: "com/sapienter/jbilling/server/**/db/**/*.java")
            exclude(name: "com/sapienter/jbilling/server/**/tasks/*.java")
            exclude(name: "com/sapienter/jbilling/server/**/task/*.java")
            exclude(name: "com/sapienter/jbilling/server/invoiceTemplate/**/*.java")
        }
    }

    Ant.copy(todir: "${targetAPIDir}/classes") {
        fileset(dir: "${basedir}/service-modules/jbilling-service/target/classes")
        fileset(dir: "${basedir}/service-modules/filter-service/target/classes")
        fileset(dir: "${basedir}/service-modules/jbilling-common-impl/target/classes")
        fileset(dir: "${basedir}/service-modules/mediation-process-service/target/classes")
        fileset(dir: "${basedir}/service-modules/mediation-service/target/classes")
        fileset(dir: "${basedir}/service-modules/usage-pool-service/target/classes")
		fileset(dir: "${basedir}/service-modules/item-service/target/classes") {
			include(name: "com/sapienter/jbilling/server/item/PricingField*.class")
		}
		fileset(dir: "${basedir}/service-modules/event-service/target/classes") {
			include(name: "com/sapienter/jbilling/server/system/event/Event.class")
		}
		fileset(dir: "${basedir}/service-modules/audit-service/target/classes")
    }

    tstamp()
    println 'generating jar'
    Ant.jar(destfile: "${targetAPIDir}/${grailsAppName}-api-${grailsAppVersion}.jar",
            basedir: "${targetAPIDir}/classes") {
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

target(main: "Create jBilling Web Services Client API jar file. jbilling_api.jar") {
    println 'Create API Jar - main'
    depends(clean, api)
}

setDefaultTarget(main)
