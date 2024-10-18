import org.junit.experimental.categories.Categories

/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
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

final resourcesDir = "${basedir}/resources"
final descriptorsDir = "${basedir}/descriptors"

target(cleanResources: "Removes the existing jbilling resources directory.") {
    //While cleaning resources excluding logos enables one to retain custom company logo
    //delete(dir: "${resourcesDir}", excludes : "**/logos/**")
	// The company logo does get copied over from descriptors logos so no need to exclude logos folder from clearing
	delete(dir: "${resourcesDir}")
}

target(createStructure: "Creates the jbilling resources directory structure.") {
    ant.sequential {
        mkdir(dir: "${resourcesDir}")
        mkdir(dir: "${resourcesDir}/api")
        mkdir(dir: "${resourcesDir}/designs")
        mkdir(dir: "${resourcesDir}/invoices")
		mkdir(dir: "${resourcesDir}/invoiceTemplates")
        mkdir(dir: "${resourcesDir}/logos")
		mkdir(dir: "${resourcesDir}/mediation")
		mkdir(dir: "${resourcesDir}/mediation/dt")
		mkdir(dir: "${resourcesDir}/mediation/errors")
        mkdir(dir: "${resourcesDir}/reports")
	    mkdir(dir: "${resourcesDir}/notifications")
        mkdir(dir: "${resourcesDir}/spring")
        mkdir(dir: "${resourcesDir}/customerInspector")
        mkdir(dir: "${resourcesDir}/nges")
        mkdir(dir: "${resourcesDir}/nges/defaultFormatFile")
        mkdir(dir: "${resourcesDir}/nges/edi")
        mkdir(dir: "${resourcesDir}/nges")
        mkdir(dir: "${resourcesDir}/nges/ediCommunication")
        mkdir(dir: "${resourcesDir}/distributel")
        mkdir(dir: "${resourcesDir}/distributel/tmp")
        mkdir(dir: "${resourcesDir}/bulkloader")
        mkdir(dir: "${resourcesDir}/bulkloader/products")
        mkdir(dir: "${resourcesDir}/bulkloader/plans")
    }
}

target(copyResources: "Creates the jbilling 'resources/' directories and copies necessary files.") {
    depends(cleanResources, createStructure)

    // copy default company logos
    copy(todir: "${resourcesDir}/logos") {
        fileset(dir: "${descriptorsDir}/logos")
    }

    // copy default mediation files
    copy(todir: "${resourcesDir}/mediation") {
        fileset(dir: "${descriptorsDir}/mediation", includes: "mediation.dtd")
        fileset(dir: "${descriptorsDir}/mediation", includes: "demo_mediation_sample.csv")
        fileset(dir: "${descriptorsDir}/mediation", includes: "*.xml")
    }

    copy(todir: "${resourcesDir}/mediation/dt") {
        fileset(dir: "${descriptorsDir}/mediation", includes: "dt-demo.csv")
    }

    // copy default invoice template
	copy(todir: "${resourcesDir}/invoiceTemplates") {
		fileset(dir: "${descriptorsDir}/invoiceTemplates", includes: "default_invoice_template.json")
	}

    // copy customer inspector schema
    copy(todir: "${resourcesDir}/customerInspector") {
        fileset(dir: "${descriptorsDir}/customerInspector", includes: "customer_inspector_schema.xsd")
    }
    // copy default customer inspector template
    copy(todir: "${resourcesDir}/customerInspector") {
        fileset(dir: "${descriptorsDir}/customerInspector", includes: "default_customer_inspector_template.xml")
    }

    // copy nges default files
    copy(todir: "${resourcesDir}/nges/defaultFormatFile") {
        fileset(dir: "${descriptorsDir}/nges/defaultFormatFile")
    }

    // copy nges default files
    copy(todir: "${resourcesDir}/nges/defaultFormatFile") {
        fileset(dir: "${descriptorsDir}/nges/defaultFormatFile")
    }

    // copy bulk loader product file templates
    copy(todir: "${resourcesDir}/bulkloader/products") {
        fileset(dir: "${descriptorsDir}/bulkloader/products")
    }
    copy(todir: "${resourcesDir}/bulkloader/plans") {
        fileset(dir: "${descriptorsDir}/bulkloader/plans")
    }

    // preserve empty directories when zipping
    touch(file: "${resourcesDir}/invoices/emptyfile.txt")
	touch(file: "${resourcesDir}/mediation/errors/emptyfile.txt")

    // Create an empty emails_sent.txt to prevent the creation from tomcat as super user
    touch(file: "${resourcesDir}/emails_sent.txt")
}

setDefaultTarget(copyResources)
