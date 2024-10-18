%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.dt.ProductImportConstants"%>

<%--
   Wait screen while product prices import job runs.

  @author Taimoor Choudhary
  @since  11-Dec-2017
--%>

<g:if test="${'busy' == jobStatus}">
    <g:message code="product.upload.label.busy"/>
    <g:formRemote name="product-job-status-form" url="[controller: 'batchJob', action:'jobStatus', id: jobId]" update="product-upload-box">
        <g:hiddenField name="template" value="/bulkUpload/processProductPricesWait"/>
    </g:formRemote>
    <script type="text/javascript">
        <%-- Reload the page every 3 seconds, until the import job is finished --%>
        window.setInterval(submitJobStatusForm, 3000);

        var jobStatusFormSubmitted = false;

        function submitJobStatusForm() {
            if(!jobStatusFormSubmitted) {
                jobStatusFormSubmitted = true;
                $('#product-job-status-form').submit();
            }
        }
    </script>
</g:if>
<g:elseif test="${'done' == jobStatus}">
    <div><g:message code="product.upload.label.done"/></div>
    
    <div>
        <g:if test="${executionParams?.getInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0) > 0}">
            <g:message code="product.upload.label.error.occurred"/>
            <g:link controller="batchJob" action="jobFile" id="${jobId}" params="[key: 'error_file']" >
                <g:message code="product.upload.label.errorfile"/></g:link>
        </g:if>
    </div>

    <table class="dataTable" cellspacing="0" cellpadding="0">
        <tbody>
        <tr>
            <td><g:message code="product.upload.label.total.count"/></td>
            <td class="value">${executionParams?.getInt(ProductImportConstants.JOB_PARAM_TOTAL_LINE_COUNT, 0)}</td>
        </tr>
        <tr>
            <td><g:message code="product.upload.label.error.count"/></td>
            <td class="value">${executionParams?.getInt(ProductImportConstants.JOB_PARAM_ERROR_LINE_COUNT, 0)}</td>
        </tr>
        </tbody>
    </table>
</g:elseif>
<g:else>
    <div><g:message code="product.upload.label.error"/></div>
    <g:link controller="batchJob" action="jobFile" id="${jobId}" params="[key: 'error_file']" ><g:message code="product.upload.label.errorfile"/></g:link>
</g:else>
