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

 import groovy.sql.Sql

includeTargets << grailsScript("_GrailsArgParsing")

target(checkDbConnection: "Checks the connection to the database and prints the errors if there are any.") {
    depends(createConfig)

    String url      = config.dataSource.url
    String driver   = config.dataSource.driverClassName
    String userName = config.dataSource.username
    String password = config.dataSource.password

    try {
        println "Checking the connection to the DB..."
        println "url="+url
        println "driver="+driver
        println "userName=":userName
        def sql = Sql.newInstance(url, userName, password, driver)
        println "Connected to the DB successfully!!!"
    } catch (Exception e) {
        System.out.println("An error ocurred while trying to connect to the DB...");
        e.printStackTrace();
    }
}
