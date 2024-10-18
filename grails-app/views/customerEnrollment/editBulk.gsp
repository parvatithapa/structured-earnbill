%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2012] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%


<html>
<head>
    <meta name="layout" content="panels"/>
</head>
<body>
<content tag="column1">
    <div class="form-edit">
        <div class="heading">
            <strong><g:message code="customer.bulk.enrollment.edit.title" default="Bulk Customer Enrollment"/></strong>
        </div>
        <div>
            <g:uploadForm name="bulkEnrollmentUploadForm" action="uploadBulk">
                <table>
                    <tr>
                        <td><g:message code="customer.bulk.enrollment.edit.enrollmentFile"/></td>
                        <td><g:field name="bulkEnrollmentFile" type="file"/></td>
                        <td><g:submitButton name="bulkEnrollmentUploadSubmit" value="${message(code: 'customer.bulk.enrollment.upload.enrollmentFile.button')}" class="submit apply"/></td>
                    </tr>
                </table>
            </g:uploadForm>
        </div>
        <div>&nbsp;</div>
        <div>
            <g:form id="bulkEnrollmentResponsesDownloadForm" name="bulkEnrollmentResponsesDownloadForm" action="downloadBulkResponses">
                <table>
                    <tr>
                        <td><g:message code="customer.bulk.enrollment.edit.brokerId"/></td>
                        <td><g:textField name="brokerId"/></td>
                        <td><a href="#" onclick="document.getElementById('bulkEnrollmentResponsesDownloadForm').submit()"><g:message code="customer.bulk.enrollment.link.downloadResponses"/></a></td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td colspan="2"><span class="input-note"><g:message code="customer.bulk.enrollment.edit.brokerId.note"/></span></td>
                    </tr>
                </table>
            </g:form>
        </div>
        <div>&nbsp;</div>
        <div>
            <div>
                <g:link action="downloadCatalog"><g:message code="customer.bulk.enrollment.link.downloadCatalog"/></g:link>
            </div>
        </div>
    </div>
</content>
</body>
</html>
