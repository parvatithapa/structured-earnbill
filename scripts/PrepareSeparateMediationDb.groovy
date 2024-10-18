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
includeTargets << new File("${basedir}/scripts/PrepareTestMediationDb.groovy")

target(prepareSeparateMediationDb: "Import the mediation database.") {
    depends(parseArguments, initLiquibase)
    if (argsMap.db == null) {
        println "This script should be used with a db parameter that point to a separate database where mediation run"
    } else {
        prepareTestMediationDb()
        def upgrade = "descriptors/database/jbilling-spring-batch-db.xml"
        if (new File(upgrade).exists()) {
            println "updating with file = " + upgrade

            ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
            if (!argsMap.init && argsMap.client) {
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'client'))
            }
            if ((argsMap.test && !argsMap.init && !argsMap.client) || (!argsMap.init && !argsMap.client)) {
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'test'))
            }
            ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
        }
    }
}

setDefaultTarget(prepareSeparateMediationDb)
