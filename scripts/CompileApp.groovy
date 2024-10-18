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

/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.tools.ant.BuildException
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

/**
 *
 * This is a duplicated of the GrailsCompile script to implement double compilation
 *
 */

includeTargets << grailsScript("_GrailsCompile")

ant.taskdef (name: 'groovyc', classname : 'org.codehaus.groovy.grails.compiler.Grailsc')
ant.path(id: "grails.compile.classpath", compileClasspath)

target(compileIgnoreFail : "Implementation of compilation phase ignoring failures") {
    depends(compilePlugins)
    profile("Compiling sources to location [$classesDirPath]") {
        withCompilationErrorHandling (true) {
            projectCompiler.compile()
        }
        classLoader.addURL(grailsSettings.classesDir.toURI().toURL())
        classLoader.addURL(grailsSettings.pluginClassesDir.toURI().toURL())
    }
}

private withCompilationErrorHandling(boolean ignoreFailure, Closure callable) {
    try {
        callable.call()
    }
    catch (BuildException e) {
        if (e.cause instanceof MultipleCompilationErrorsException) {
            event("StatusError", ["Compilation error: ${e.cause.message}"])
        }
        else {
            grailsConsole.error "Fatal error during compilation ${e.class.name}: ${e.message}", e
        }
        if (!ignoreFailure){
            exit 1
        }
    }
    catch(Throwable e) {
        grailsConsole.error "Fatal error during compilation ${e.class.name}: ${e.message}", e
        if (!ignoreFailure){
            exit 1
        }
    }
}

target(compileApplication: "Compiles the entire application including the double compile issue") {
    println "Compiling the application with error handling..."

    println "First compilation (Will fail...)"
    compileIgnoreFail()
    println "Second compilation (Should not fail...)"
    compile()

    println "Application compiled"
}

setDefaultTarget(compileApplication)
