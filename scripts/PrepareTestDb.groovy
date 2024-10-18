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

target(cleanDb: "Clean the test postgresql database, will drop/create the database if --hard.") {
    depends(parseArguments, initLiquibase)

    def db = getDatabaseParameters(argsMap)

    // execute the postgresql dropdb command to forcibly drop the database
    // when --drop or --hard
    if (argsMap.drop || argsMap.hard) {
        println "dropping database ${db.database}"
        exec(executable: "dropdb", failonerror: false) {
            arg(line: "-U ${db.username} -e ${db.database}")
        }
    }

    // execute postgresql createdb to create the database
    // when --create or --hard
    if (argsMap.create || argsMap.hard) {
        println "creating database ${db.database}"
        exec(executable: "createdb", failonerror: true) {
            arg(line: "-U ${db.username} -O ${db.username} -E UTF-8 -e ${db.database}")
        }
    }

    // default, just use liquibase to drop all existing objects within the database
    if (!argsMap.drop && !argsMap.create && !argsMap.hard) {
        println "dropping all objects in ${db.database}"
        ant.dropAllDatabaseObjects(liquibaseTaskAttrs())
    }

    /** Delete the ActiveMQ Data folder
        This stores the JMS message queues for payments, and causes
        tests to fail if the queue gains a considerable size */
    println "Cleaning the ActiveMQ data directory..."
    def amqDataDir = new File("")
    if (amqDataDir.exists()) {
        def result = amqDataDir.deleteDir()
        if (!result) {
            throw new RuntimeException("The ActiveMQ directory couldn't be deleted!!")
        }
    }

}

target(prepareTestDb: "Import the test postgresql database.") {
    depends(parseArguments, initLiquibase)

    def version = getApplicationMinorVersion(argsMap)
    println "Loading database version ${version}"
    echoDatabaseArgs()
    //dump liquibase.classpath for closer inspection
    //echo(message: '\${toString:liquibase.classpath}')

    // clean the db
    cleanDb()

    // changelog files to load
    def schema = "descriptors/database/jbilling-schema.xml"
    def init   = "descriptors/database/jbilling-init_data.xml"
    def test   = "descriptors/database/jbilling-test_data.xml"

    def versionHierarchy = getApplicationVersionsHierarchy(argsMap)
	
    // load the jbilling database
    // by default this will load the testing data
    // if the -init argument is given then only the base jbilling data will be loaded
    // if the -client argument is given then the client reference data will be loaded
    if (argsMap.init) {
        println "updating with context = base. Loading init jBilling data"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'base'))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: init))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'FKs'))

        versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
            def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
                println "Loading upgrade ${upgrade}"
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
            }
        };

    } else if (argsMap.client) {
        println "updating with context = base. Loading client reference Db"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'base'))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: client))

        versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
            def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'client'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
            }
        };
    }
    if ((argsMap.test && !argsMap.init && !argsMap.client) || (!argsMap.init && !argsMap.client)) {
        println "updating with context = test. Loading test data"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'base'))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: test))
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: schema, contexts: 'FKs'))

        versionHierarchy.eachWithIndex { dbVersion, versionIndex ->
            def upgrade = "descriptors/database/jbilling-upgrade-${dbVersion}.xml"
            if (new File(upgrade).exists()) {
                println "Loading upgrade ${upgrade}"
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'base'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'test'))
                ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'post_base'))
            }
        };
        def fcTestData = "descriptors/database/fc-client-upgrade.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: fcTestData))
        def distributelTestData = "descriptors/database/distributel-client-upgrade.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: distributelTestData))

        def moviusTestData = "descriptors/database/movius-client-upgrade.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: moviusTestData))

        def ignitionTestData = "descriptors/database/ignition-client-upgrade.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: ignitionTestData))

        def dtTestDataUpgrade = "descriptors/database/dt/dt-client-upgrade.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: dtTestDataUpgrade))

        def sapphireTestData = "descriptors/database/sapphire-client-upgrade.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: sapphireTestData))

        def testData = "descriptors/database/test-data-features.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: testData))
		
		println "Loading test data for company hierarchy improper access(REST API) unit testing"
		def testCompHierarchyImpAccessData = "descriptors/database/test-data-company-hierarchy-improper-access.xml"
		ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: testCompHierarchyImpAccessData))

        def earnbillTestData = "descriptors/database/test-data-mobile-api-access.xml"
        ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: earnbillTestData))
    }
}

setDefaultTarget(prepareTestDb)
