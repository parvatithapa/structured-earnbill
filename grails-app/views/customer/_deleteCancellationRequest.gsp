  
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
   <g:applyLayout name="form/date">
        <content tag="label"><g:message code="cancellation.request.date"/></content>
        <content tag="label.for">cancellationDate_delete</content>
        <content tag="avoid.js">true</content>
        <g:textField class="date-text txt-bg" name="cancellationDate_delete" id="cancellationDate_delete" value="" disabled="true"/>
    </g:applyLayout>
    <label class="lb"><g:message code="customer.cancellationRequest.reasonText" /></label> <br>
    <g:textArea name="reasonText_delete" value="" rows="5" cols="50" id="reasonText_delete" disabled="true"/>
<g:hiddenField name="cancellation.companyId" value="${session['user_id']}"/> 


