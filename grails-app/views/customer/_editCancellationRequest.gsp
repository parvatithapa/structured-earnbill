  
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<div class="box-card-hold">
${cancellationData }
	<g:applyLayout name="form/date">
		<content tag="label"> <g:message code="cancellation.request.date" /></content>
		<content tag="label.for">cancellationDate_edit</content>
		<content tag="avoid.js">true</content>
		<g:textField name="cancellationDate_edit" id="cancellationDate_edit" class="date-text txt-bg" value="" data-dateformat='datepicker.format' />
	</g:applyLayout>
	<label class="lb"><g:message code="customer.cancellationRequest.reasonText" /></label> <br>
	<g:textArea name="reasonText" id="reasonText" value="" rows="5" cols="50" />
	<g:hiddenField name="cancellation.createTimestamp" value="${null}"	id="createTimestamp" />
	<g:hiddenField name="cancellation.userId" value="${session['user_id']}" />

</div>
