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


<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.util.Constants" %>

<%--
  Account Information Type Details view

  @author Aamir Ali
  @since  02/21/2017
--%>


<g:set var="isNew" value="${!cit || !cit?.id || cit?.id == 0}"/>

<div id="details-box">
    <br/>
    <div id="error-messages" class="msg-box error" style="display: none;">
        <ul></ul>
    </div>

    <g:formRemote name="ait-details-form" url="[action: 'editCIT']" update="column2" method="GET">
        <g:hiddenField name="_eventId" value="update"/>
        <g:hiddenField name="execution" value="${flowExecutionKey}"/>

        <div class="form-columns">

            <g:applyLayout name="form/text">
                <content tag="label"><g:message code="company.information.type.id.label"/></content>

                <g:if test="${!isNew}">
                    <span>${cit.id}</span>
                </g:if>
                <g:else>
                    <em><g:message code="prompt.id.new"/></em>
                </g:else>

                <g:hiddenField name="cit.id" value="${cit?.id}"/>
            </g:applyLayout>

            <g:applyLayout name="form/input">
                <content tag="label">
                <g:message code="company.information.type.name.label" /></content>
                <content tag="label.for">name</content>
                <g:textField class="field text" name="name" value="${cit?.name}"/>
            </g:applyLayout>

            <g:applyLayout name="form/text">
                <content tag="label">
                    <g:message code="company.information.type.display.label" /></content>
                <content tag="label.for">displayOrder</content>
                <div class="inp-bg inp4">
                    <g:textField class="field" name="displayOrder" value="${cit?.displayOrder}"/>
                </div>
            </g:applyLayout>
        </div>

    </g:formRemote>


<script type="text/javascript">

    var submitForm = function() {
        var form = $('#ait-details-form');
        form.submit();
    };

    $('#ait-details-form').find('input.text').blur(function() {
        submitForm();
    });

    $('#ait-details-form').find('input').blur(function() {
        submitForm();
    });

    var validator = $('#ait-details-form').validate();
    validator.init();
    validator.hideErrors();

</script>

</div>
