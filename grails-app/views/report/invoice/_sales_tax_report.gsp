%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="org.joda.time.DateTime;" %>

<%--
  Sales Tax Report

  @author Mahesh Shivarkar
  @since  28-Feb-2017
--%>

<div class="form-columns">
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start_date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field" name="start_date" value="${formatDate(date: new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).toDate(), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end_date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date" value="${formatDate(date: new DateTime().dayOfMonth().withMaximumValue().toDate(), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>
    
    <input id="entityNames" type="hidden" name="entityNames" value="" />
    %{--If the user can not choose the entity (on show.gsp) run for the current entity--}%
    <g:if test="${!childEntities || company?.parent}">
        <g:hiddenField name="childs" value="${company.id}" />
    </g:if>
</div>

<script type="text/javascript">
    $(function() {
        $('#childs').change(function() {
            var val = '';
            $('#childs option:selected').each( function() {
                if(val != '') val += ', ';
                val += $(this).text();
            });
            $('#entityNames').val(val);
        });

        $("label[for='childs']").text('<g:message code="report.label.child.company"/>');
    });
</script>