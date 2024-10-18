<%@ page import="org.joda.time.DateTime;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.PlanDTO;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.adennet.AdennetHelperService; org.hibernate.criterion.CriteriaSpecification;"%>

<style>
   #bank-report select, #bank-report .select-holder{
        width:221px;
        padding-left: 2px;
   }
</style>

<%--
   bank-user-report

  @author Nitin Yewale
  @since  18-AUG-2022
--%>

<%
     /* Fetching all bank users */
     def userNames = new ArrayList<>();

     def bankUsers = new AdennetHelperService().getUsernameForBankUsers();

     for (String bankUser : bankUsers){
         userNames.add(bankUser);
     }

     /* Fetching  all plans */
     def plans =  PlanDTO.createCriteria().list() {
         createAlias("item","item")
         createAlias("item.entities","entities", CriteriaSpecification.LEFT_JOIN)
         and {
              'in'('entities.id', session['company_id'])
         }
         order('id', 'asc')
     }

%>

<div class="form-columns" id="bank-report">
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
        <g:textField name="subscriber_number" class="field" />
    </g:applyLayout>

     <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.package.name"/></content>
        <content tag="label.for">plan_name</content>
        <content tag="include.script">true</content>
        <g:select
            from="${plans}"
            name="plan_name"
            optionKey="${{it.item.description}}"
            optionValue="${{it.item.description}}"
            noSelection="['':message(code: 'select.option.default.value.name')]"
        />
     </g:applyLayout>

    <g:applyLayout name="form/select_multiple" >
           <content tag="label"><g:message code="bank.user.name"/></content>
            <content tag="label.for">bank_user_name</content>
            <content tag="include.script">true</content>
               <g:select
                    multiple="true"
                    id="bank_user_name"
                    name="bank_user_name"
                    from="${userNames}"
               />
     </g:applyLayout>

    <input id="entityNames" type="hidden" name="entityNames" value="" />
    %{--If the user can not choose the entity (on show.gsp) run for the current entity--}%
    <g:if test="${!childEntities || company?.parent}">
        <g:hiddenField name="childs" value="${company.id}" />
    </g:if>
</div>
