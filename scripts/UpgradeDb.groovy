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

includeTargets << grailsScript("Init")
includeTargets << new File("${basedir}/scripts/Liquibase.groovy")

target(upgradeDb: "Upgrades database to the latest version") {
    depends(parseArguments, initLiquibase)

    def version = getApplicationMinorVersion(argsMap)

    println "Upgrading database to version ${version}"
    echoDatabaseArgs()

    String baseDir = "descriptors/database/"
    def dir = new File(baseDir)
    def jbillingUpgradeFiles = []
    def context = "base"
    if (argsMap.test) {
        context = "test"
    } else if (argsMap.client) {
        context = "client"
    } else if (argsMap.demo) {
        context = "demo"
    } else if (argsMap.post_base) {
        context = "post_base"
    }

    // Gathers Jbilling upgrade files
    def lastUpgrade = "${baseDir}jbilling-upgrade-${version}.xml"
    if (new File(lastUpgrade).exists()) {
        def versionHierarchy = getApplicationVersionsHierarchy(argsMap)
//        versionHierarchy.add(version)
        versionHierarchy.each { dbVersion ->
            def upgrade = "${baseDir}jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
                jbillingUpgradeFiles.add(upgrade)
            }
        };
        jbillingUpgradeFiles.add(lastUpgrade)
    } else {
        println "Application version ${version} is not valid."
        return
    }

    // Gather all mediation upgrade files
    def mediationFileOrdererByVersioning = []
    dir.eachFileRecurse (FileType.FILES) { file ->
        def mediation = file.path
        if (mediation.contains("jbilling-mediation") && new File(mediation).exists()) {
            mediationFileOrdererByVersioning.add(mediation)
        }
    }

    println "Updating jbilling with context = $context"
    jbillingUpgradeFiles.each {
        upgrade ->
            println "Running jbilling upgrade file ${upgrade}"
            ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    };

    println "Updating mediation with context = $context"
    mediationFileOrdererByVersioning.each {
        upgrade ->
            println "Running mediation upgrade file ${upgrade}"
            ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    };

    if (argsMap.fcclient) {
        upgrade = "descriptors/database/fc-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }

    if (argsMap.distributelclient) {
      upgrade = "descriptors/database/distributel-client-upgrade.xml"
      println "Running jbilling upgrade file ${upgrade}"
      ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }
	
    if (argsMap.moviusclient) {
	upgrade = "descriptors/database/movius-client-upgrade.xml"
	println "Running jbilling upgrade file ${upgrade}"
	ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }

    if (argsMap.ignitionclient) {
        upgrade = "descriptors/database/ignition-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }

    if (argsMap.dtclient) {
        upgrade = "descriptors/database/dt/dt-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))

        upgrade = "descriptors/database/dt/dt-client-data.xml"
        println "Running jbilling data file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }
    
    if (argsMap.spcclient) {
        upgrade = "descriptors/database/spc-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }
    
    if (argsMap.sapphireclient) {
        upgrade = "descriptors/database/sapphire-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }
    if (argsMap.indiaclient) {
        upgrade = "descriptors/database/india-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }
    if (argsMap.usaclient) {
        upgrade = "descriptors/database/usa-client-upgrade.xml"
        println "Running jbilling upgrade file ${upgrade}"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: context))
    }
}

setDefaultTarget(upgradeDb)
