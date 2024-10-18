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

target(rollbackDb: "Upgrades database to the latest version") {
    depends(parseArguments, initLiquibase)

    def version = getApplicationMinorVersion(argsMap)
    def tag = argsMap.tag

    if (!tag) throw new IllegalArgumentException("Argument -tag=[tag name] is required for tag / rollback operations!");

    println "Rolling back database to tag '${tag}'"
    echoDatabaseArgs()


    // liquibase upgrade changelog
    def changelog = "./descriptors/database/jbilling-upgrade-${version}.xml"

    ant.rollbackDatabase(liquibaseTaskAttrs(changeLogFile: changelog, rollbackTag: tag))
}

setDefaultTarget(rollbackDb)
