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

target(prepareTestMediationDb: "Import the test postgresql database.") {
    println "This script don't remove and create again the mediation database"
    depends(parseArguments, initLiquibase)

    def dir = new File("descriptors/database/")
    def mediationFileOrdererByVersioning = []
    dir.eachFileRecurse (FileType.FILES) { file ->
        def mediation = file.path
        if (mediation.contains("jbilling-mediation") && new File(mediation).exists()) {
            mediationFileOrdererByVersioning.add(file)
        }
    }

    mediationFileOrdererByVersioning.sort().each {
        file ->
            def mediation = file.path
            println "updating with file = " + file.path
            ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: mediation, contexts: 'base'))
            if (!argsMap.init && argsMap.client) {
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: mediation, contexts: 'client'))
            }
            if ((argsMap.test && !argsMap.init && !argsMap.client) || (!argsMap.init && !argsMap.client)) {
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: mediation, contexts: 'test'))
            }
            ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: mediation, contexts: 'post_base'))
    };
}

setDefaultTarget(prepareTestMediationDb)
