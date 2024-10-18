%{--
 SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 _____________________

 [2024] Sarathi Softech Pvt. Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is and remains
 the property of Sarathi Softech.
 The intellectual and technical concepts contained
 herein are proprietary to Sarathi Softech
 and are protected by IP copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
--}%

<%@ page import="org.joda.time.DateTime; com.sapienter.jbilling.server.user.db.UserDTO; com.sapienter.jbilling.server.adennet.AdennetHelperService;" %>
<%@ page import="com.sapienter.jbilling.server.user.UserBL;com.sapienter.jbilling.server.user.db.CompanyDTO; org.hibernate.criterion.CriteriaSpecification; java.util.Arrays; java.util.ArrayList;" %>
<%--
   Audit Log Report
  @author Nitin Yewale
  @since  04-Mar-2024
--%>
<style>
   #audit-log-report select, #audit-log-report .select-holder{
        width:221px;
   }
   #field_name{
   height:90px;
   }
</style>
<%
    /* Fetching jbilling user details */
    def userDTOs =  new UserBL().getAllUsers(session['company_id'] as Integer);
    def userNames = new ArrayList<>();

    for (UserDTO userDto : userDTOs){
        userNames.add(userDto.userName);
    }
    /*Fetching fields details*/
    def fieldsName = new AdennetHelperService().getAuditLogFields();
%>
<div class="form-columns" id="audit-log-report">
    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="start.date"/></content>
        <content tag="label.for">start_date</content>
        <g:textField class="field" name="start_date" value="${formatDate(date: new DateTime().withTimeAtStartOfDay().withDayOfMonth(1).toDate(), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

    <g:applyLayout name="form/date">
        <content tag="label"><g:message code="end.date"/></content>
        <content tag="label.for">end_date</content>
        <g:textField class="field" name="end_date" value="${formatDate(date: new DateTime().dayOfMonth().withMaximumValue().toDate(), formatName: 'datepicker.format')}" onblur="validateDate(this)"/>
    </g:applyLayout>

     <g:applyLayout name="form/input">
        <content tag="label"><g:message code="subscriber.number"/></content>
        <content tag="label.for">subscriber_number</content>
        <g:textField name="subscriber_number" class="field"/>
     </g:applyLayout>

     <g:applyLayout name="form/input">
        <content tag="label"><g:message code="audit.log.user.id"/></content>
        <content tag="label.for">user_id</content>
        <g:textField name="user_id" class="field"/>
     </g:applyLayout>

     <g:applyLayout name="form/select">
         <content tag="label"><g:message code="user.name"/></content>
         <content tag="label.for">user_name</content>
         <content tag="include.script">true</content>
         <g:select
            from="${userNames}"
            id="user_name"
            name="user_name"
            optionKey="${{it}}"
            optionValue="${{it}}"
            noSelection="['':message(code: 'select.option.default.value.name')]"/>
     </g:applyLayout>

     <g:applyLayout name="form/select_multiple" >
         <content tag="label"><g:message code="report.field.name"/></content>
         <content tag="label.for">field_name</content>
         <content tag="include.script">true</content>
         <g:select
             from="${fieldsName}"
             id="field_name"
             multiple="true"
             name="field_name"
             optionKey="${{it}}"
             optionValue="${{it}}"
         />
     </g:applyLayout>
</div>
