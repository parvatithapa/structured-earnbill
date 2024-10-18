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
includeTargets << grailsScript("_GrailsArgParsing")

/**
 * Classpath containing all the Grails runtime dependencies, compiled classes
 * and plug-in classes for the liquibase ant task.
 *
 * Example:
 *     ant.taskdef(resource: "liquibasetasks.properties")
 *     ant.path(id: "liquibase.classpath", liquibaseClasspath)
 *
 *     updateDatabase(classpathref: "liquibase.classpath", args...)
 */
liquibaseClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    def dependencies = grailsSettings.runtimeDependencies
    if (dependencies) {
        for (File f in dependencies) {
            pathelement(location: f.absolutePath)
        }
    }

    pathelement(location: "${pluginClassesDir.absolutePath}")
    pathelement(location: "${classesDir.absolutePath}")
}

/**
 * Returns the application version as a numeric [major].[minor] version number.
 *
 * Can be explicitly set using the -dbVersion command line argument.
 *
 * Example:
 *      "enterprise-3.2.0" => 3.2
 */
getApplicationMinorVersion = { argsMap ->
    def version = argsMap.dbVersion ? argsMap.dbVersion : grailsAppVersion
    // strip all alphanumeric characters, then trim the string down to the first dotted pair
    def number = version.replaceAll(/[^0-9\.]/, '')
    return number.count('.') > 1 ? number.substring(0, number.lastIndexOf('.')) : number;
}

/**
 * Return list of application versions that are preceding the current version.
 * The list of application version is order by the oldest first
 *
 * The hierarchy map is hardcoded
 *
 * Example: current version: 3.3 => 3.1, 3.2
 *
 */
getApplicationVersionsHierarchy = { argsMap ->

    def versionsHierarchy = argsMap.appHierarchy ? argsMap.appHierarchy : [

            '4.36' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30, 4.31, 4.32, 4.33, 4.34, 4.35, 4.36],
            '4.35' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30, 4.31, 4.32, 4.33, 4.34, 4.35],
            '4.34' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30, 4.31, 4.32, 4.33, 4.34],
            '4.33' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30, 4.31, 4.32, 4.33],
            '4.32' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30, 4.31, 4.32],
            '4.31' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30, 4.31],
            '4.30' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29, 4.30],
            '4.29' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28, 4.29],
            '4.28' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27, 4.28],
            '4.27' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26, 4.27],
            '4.26' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25, 4.26],
            '4.25' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24, 4.25],
            '4.24' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23, 4.24],
            '4.23' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22, 4.23],
            '4.22' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21, 4.22],
            '4.21' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20, 4.21],
            '4.20' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19, 4.20],
            '4.19' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18, 4.19],
            '4.18' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17, 4.18],
            '4.17' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16, 4.17],
            '4.16' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15, 4.16],
            '4.15' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14, 4.15],
            '4.14' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13, 4.14],
            '4.13' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 4.13],
            '4.12' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12],
            '4.11' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11],
            '4.10' : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10],
            '4.9'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9],
            '4.8'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8],
            '4.7'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6, 4.7],
            '4.6'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5, 4.6],
            '4.5'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4, 4.5],
            '4.4'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3, 4.4],
            '4.3.3': [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3],
            '4.3.2': [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3],
            '4.3.1': [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3],
            '4.3'  : [3.1, 3.2, 3.3, 3.4, 4.0, 4.1, 4.3],
            '4.1'  : [3.1, 3.2, 3.3, 3.4, 4.0],
            '4.0'  : [3.1, 3.2, 3.3, 3.4],
            '3.3'  : [3.1, 3.2],
            '3.2'  : [3.1]
    ]

    def version = getApplicationMinorVersion(argsMap)
    return versionsHierarchy["${version}"]
}

/**
 * Parses the command line arguments and builds a map of database parameters required
 * by all liquibase ant tasks. If no arguments are provided the defaults will be used.
 * When running PrepareTest, you must be in the Groovy shell if you want to specify
 * there parameters as in the example below.
 *
 *      -user   = Database username,   defaults to config.dataSource.username
 *      -pass   = Database password,   defaults to config.dataSource.password
 *      -db     = Database name, defaults to 'jbilling_test'
 *      -url    = Database url,        defaults to config.dataSource.url
 *      -driver = JDBC Driver class,   defaults to config.dataSource.driverClassName
 *      -schema = Default schema name, defaults to 'public'
 *
 * Example:
 *      grails liquibase -user=[username] -pass=[password] \
 *          -db=[db name] -url=[jdbc url] -driver=[driver class] -schema=[defalut schema name]
 */
getDatabaseParameters = { argsMap ->
    def database = argsMap.db ?: "jbilling_test";
    def url = argsMap.url ?: config.dataSource.url
    if (database != "jbilling_test") {
        url = url.replaceAll(url.split("/").last(), database)
    }

    def db = [
            username: argsMap.user ?: config.dataSource.username,
            password: argsMap.pass ?: config.dataSource.password,
            database: database,
            url     : url,
            driver  : argsMap.driver ?: config.dataSource.driverClassName,
            schema  : argsMap.schema ?: "public"
    ]

    return db
}

echoDatabaseArgs = {
    def db = getDatabaseParameters(argsMap)
    println "${db.url} ${db.username}/${db.password ?: '[no password]'} (schema: ${db.schema}) (driver ${db.driver})"
}

/**
 * set default params for use in ant.updateDatabase and other liquibase ant tasks.
 * supports adding extra params map
 *
 * Examples:
 *      ant.dropAllDatabaseObjects(liquibaseTaskAttrs())
 *      ant.updateDatabase(liquibaseTaskAttrs(changeLogFile: upgrade, contexts: 'test'))
 */
liquibaseTaskAttrs = { extraAttrsMap ->
    def resultMap = getDatabaseParameters(argsMap)
    resultMap["classpathref"] = "liquibase.classpath"
    resultMap.remove("database")
    resultMap["defaultSchemaName"] = resultMap.remove("schema")

    extraAttrsMap.each { entry ->
        resultMap[entry.key] = entry.value
    }
    resultMap
}

target(initLiquibase: "Initialized the liquibase ant tasks") {
    depends(createConfig)
    // see http://www.liquibase.org/manual/ant
    ant.taskdef(resource: "liquibasetasks.properties")
    ant.path(id: "liquibase.classpath", liquibaseClasspath)
}

target(echoArgs: "Prints the parsed liquibase parameters to the screen.") {
    depends(parseArguments, createConfig)

    println "This grails script does not have an executable target."

    def version = getApplicationMinorVersion(argsMap)

    println "jBilling minor version = ${version}"
    echoDatabaseArgs()

    def hierarchy = getApplicationVersionsHierarchy(argsMap)
    println "jBilling versions hierarchy = ${hierarchy}"
}

setDefaultTarget(echoArgs)
