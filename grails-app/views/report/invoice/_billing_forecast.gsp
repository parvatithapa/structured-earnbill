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

<%@ page import="java.time.LocalDateTime" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<%--
  Parameters for the Billing Forecast report.

  @author Leandro Bagur
  @since  07-Jun-2017
--%>
<script type="text/javascript">

    var previousStarDate =  new Date();
    function setEndMinDate() {
        var startDate = $("#start_date").datepicker("getDate");
        $("#end_date").datepicker("option", "minDate", startDate);
    }
    
    function disableStartDate() {
        if ($("#enable_start_date").is(':checked')) {
            $( "#start_date" ).datepicker( "option", "minDate", ${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().plusDays(1), session['company_timezone']))} );
            $("#start_date").datepicker("setDate", previousStarDate);
            $("#startDateDiv").show();
        } else {
            previousStarDate = $("#start_date").datepicker("getDate");
            $("#start_date").datepicker( "option", "minDate","01/01/1970");
            $("#start_date").datepicker("setDate", "01/01/1970");
            $("#startDateDiv").hide();
        }
    }
</script>

<div class="form-columns">
    <g:applyLayout name="form/checkbox">
        <content tag="label"><g:message code="report.billing.register.enable.startDate"/></content>
        <content tag="label.for">enable_start_date</content>
        <g:checkBox class="cb" name="enable_start_date" value="false" onClick="disableStartDate()"/>
    </g:applyLayout>
    
    <div id="startDateDiv">
        <g:applyLayout name="form/date">
            <content tag="label"><g:message code="start_date"/></content>
            <content tag="label.for">start_date</content>
            <g:textField class="field" name="start_date"
                         minDate="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().plusDays(1), session['company_timezone']))}"   
                         value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().plusDays(1), session['company_timezone']), formatName: 'datepicker.format')}"
                         onblur="validateDate(this)"/>
            <content tag="onClose">
                function(e) {
                setEndMinDate()
                }
            </content>
        </g:applyLayout>
    </div>
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end_date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date"
                     minDate="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().plusDays(1), session['company_timezone']))}"
                     value="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now().plusDays(1), session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
    </g:applyLayout>
</div>