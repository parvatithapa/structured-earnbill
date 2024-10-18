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

<%--
  Parameters for the Accounts Receivable report.

  @author Brian Cowdery
  @since  30-Mar-2011
--%>
<%@ page import="java.time.LocalDateTime;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<script type="text/javascript">
    function validateAsOfValue() {
        var asOfEl = $("#as_of"),
                asOf = asOfEl.datepicker('getDate'),
                todayDate = new Date();

        if (asOf > todayDate) {
            asOfEl.datepicker('option', 'maxDate', todayDate);
        }
    }
</script>

<div class="form-columns">

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="as_of"/></content>
        <content tag="label.for">as_of</content>
        <g:textField class="field"
                     name="as_of"
                     maxDate="${formatDate(date: TimezoneHelper.convertToTimezoneAsUtilDate(LocalDateTime.now(), session['company_timezone']))}"
                     value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}"
                     onblur="validateDate(this)"/>
        <content tag="onClose">
            function(e) {
                validateAsOfValue()
            }
        </content>
    </g:applyLayout>

</div>