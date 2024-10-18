<%@ page import="java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<div class="box-card-hold">

    <p>
        <g:applyLayout name="form/date">
            <content tag="label"><g:message code="cancellation.request.date"/></content>
            <content tag="label.for">cancellation.cancellationDate</content>
            <content tag="avoid.js">true</content>
            <g:textField name="cancellation.cancellationDate" id="cancellation.cancellationDate" class="date-text txt-bg"
                         data-dateformat='datepicker.format'/>
        </g:applyLayout>
    </p>

    <p>
        <label class="lb"><g:message code="customer.cancellationRequest.reasonText"/></label> <br>
        <g:textArea name="cancellation.reasonText" value="" class="txt-bg" id="reason" autofocus="autofocus"/>
        <g:hiddenField name="cancellation.createTimestamp" value="${null}" id="createTimestamp"/>
        <g:hiddenField name="cancellation.companyId" value="${session['user_id']}"/>
    </p>

</div>