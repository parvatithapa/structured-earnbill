<%--
  Parameters for the Overdue Account Summary report.

  @author Leandro Bagur
  @since  11-Oct-2017
--%>

<%@ page import="com.sapienter.jbilling.server.util.Util" %>

<div class="form-columns">
    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="report.overdue.customer.status"/></content>
        <content tag="label.for">customer_status</content>
        <content tag="include.script">true</content>
        <g:select name="customer_status" from="${Util.getOverdueUserStatus(session['company_id'])}"
                  optionKey="id" 
                  optionValue="${{it.getDescription(session['language_id'])}}"
                  noSelection="['': message(code: 'default.no.selection')]" />
    </g:applyLayout>
</div>