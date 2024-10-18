package jbilling

import com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO
import com.sapienter.jbilling.server.invoice.InvoiceTemplateFileDTO
import com.sapienter.jbilling.server.security.Validator
import com.sapienter.jbilling.server.util.SecurityValidator
import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.apache.commons.httpclient.HttpStatus
import org.springframework.web.multipart.MultipartFile

/**
 * @author Klim Sviridov
 */
@Slf4j
class InvoiceTemplateFileController {

    SecurityValidator securityValidator

    @Secured(["hasAnyRole('PLAN_60', 'PLAN_61')"])
    def save (SaveCommand cmd) {
        if (!cmd.invoiceTemplateFile.empty) {
            def invoiceTemplateId = cmd.invoiceTemplateId
            def template = InvoiceTemplateDTO.get(invoiceTemplateId);
            securityValidator.validateCompany(template?.entity?.id, Validator.Type.EDIT)
            if (template != null) {
                def file = cmd.invoiceTemplateFile;
                def dto = new InvoiceTemplateFileDTO()
                dto.invoiceTemplateId =  invoiceTemplateId
                dto.name = file.originalFilename
                dto.data = file.bytes
                dto.save()
                render status: HttpStatus.SC_CREATED
            }
            render status : HttpStatus.SC_NOT_FOUND
        }
        render status: HttpStatus.SC_BAD_REQUEST
    }

}

class SaveCommand {
    MultipartFile invoiceTemplateFile;
    long invoiceTemplateId;
}
