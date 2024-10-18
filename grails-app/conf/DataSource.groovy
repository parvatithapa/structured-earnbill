/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration

def dbUser     = System.getenv("JBILLING_DB_USER")     ?: "jbilling"
def dbName     = System.getenv("JBILLING_DB_NAME")     ?: "jbilling_test"
def dbHost     = System.getenv("JBILLING_DB_HOST")     ?: "localhost"
def dbPort     = System.getenv("JBILLING_DB_PORT")     ?: "5432"
// N.B. db params string should starts with symbol "?"
def dbParams   = System.getenv("JBILLING_DB_PARAMS")   ?: ""
def dbPassword = System.getenv("JBILLING_DB_PASSWORD") ?: ""

if (dbParams) {
    dbParams = "?" + dbParams
}

dataSource {
    dialect         = "org.hibernate.dialect.PostgreSQLDialect"
    driverClassName = "org.postgresql.Driver"
    username = dbUser
    password = dbPassword
    url = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}${dbParams}"

    /*
        Other database configuration settings. Do not change unless you know what you are doing!
        See resources.groovy for additional configuration options
    */
    pooled = true
    configClass = GrailsAnnotationConfiguration.class
}

hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache        = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true     // configure OSIV singleSession mode
    flush.mode    = 'manual' // OSIV session flush mode outside of transactional context
}

environments {
    production {
        hibernate {
            config.location = [
                    "classpath:hibernate.cfg.xml"]
        }
    }
    development {
        hibernate {
            config.location = [
                "file:grails-app/conf/hibernate/hibernate.cfg.xml",
                "file:grails-app/conf/hibernate/hibernate-debug.cfg.xml"]
        }
    }
}
