/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

includeTargets << grailsScript("War")

final targetDir = "${basedir}/target"
final libDir = "${basedir}/lib"
final unzipDir = "${targetDir}/unZip"
final srcLibDir = "${unzipDir}/WEB-INF/lib"

target(lib: "Packages all core jbilling classes in a .jar file.") {
    depends(war)
    tstamp()
    ant.unzip(src: "${targetDir}/${grailsAppName}.war", dest: "${unzipDir}/", overwrite:"true")
    ant.copy(toDir: libDir) {
        fileset(dir: srcLibDir, excludes: "*-4.0.5.RELEASE.jar")
    }
}

setDefaultTarget(lib)
