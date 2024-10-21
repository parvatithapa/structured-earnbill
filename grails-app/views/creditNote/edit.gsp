<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<html>
<head>
    <meta name="layout" content="main"/>
    <r:script disposition='head'>
        function validateDate(element) {
            var dateFormat= "<g:message code="date.format"/>";
            if(!isValidDate(element, dateFormat)) {
                $("#error-messages").css("display","block");
                $("#error-messages ul").css("display","block");
                $("#error-messages ul").html("<li><g:message code="invalid.date.format"/></li>");
                element.focus();
                return false;
            } else {
                return true;
            }
        }
    </r:script>
    <r:external file="js/form.js"/>
</head>

<body>
<div class="form-edit">

    <div class="heading">
        <strong>
            <g:message code="creditNote.new"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="credit-note-form" action="save" useToken="true">
            <fieldset>
                <div class="form-columns">

                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="creditNote.id"/></content>
                            <em><g:message code="prompt.id.new"/></em>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="creditNote.currency"/></content>
                            ${currency}
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="creditNote.amount"/><span
                                    id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">creditNote.amount</content>
                            <g:textField class="field"
                                         name="creditNote.amount"
                                         value="${creditNote?.amount?.isNumber() ? formatNumber(number: creditNote?.amount ?: 0, formatName: 'price.format')
                                                 : creditNote?.amount}"
                                         size="20"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="creditNote.date"/></content>
                            <content tag="label.for">creditNote.creditNoteDate</content>
                            <g:set var="creditNoteDate" value="${TimezoneHelper.currentDateForTimezone(session['company_timezone'])}"/>
                            <g:textField class="field" name="creditNote.creditNoteDate"
                                         value="${formatDate(date: creditNoteDate, formatName: 'datepicker.format')}"
                                         onblur="validateDate(this);"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="creditNote.item.id"/><span
                                    id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">creditNote.itemId</content>
                            <g:textField class="field" name="creditNote.itemId" value="${creditNote?.itemId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="creditNote.description"/></content>
                            <content tag="label.for">creditNote.description</content>
                            <g:textField class="field" name="creditNote.description"
                                         value="${creditNote?.description}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="creditNote.service.id"/></content>
                            <content tag="label.for">creditNote.service.id</content>
                            <g:textField class="field" name="creditNote.serviceId" value="${creditNote?.serviceId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="creditNote.subscription.order.id"/></content>
                            <content tag="label.for">creditNote.subscription.order.id</content>
                            <g:textField class="field" name="creditNote.subscriptionOrderId"
                                         value="${creditNote?.subscriptionOrderId}"/>
                        </g:applyLayout>

                        <div class="box-text">
                            <label for="creditNote.notes"><g:message code="creditNote.notes"/></label>
                            <g:textArea name="creditNote.notes" value="${creditNote?.notes}" rows="5" cols="60"/>
                        </div>

                    </div>

                    <div class="column">

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="creditNote.user"/></content>
                            <span><g:link controller="customer" action="list"
                                          id="${user.userId}">${user.userId}</g:link></span>
                            <g:hiddenField name="creditNote.userId" value="${user.userId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.login.name"/></content>
                            <span>${displayer?.getDisplayName(user)}</span>
                        </g:applyLayout>

                        <g:if test="${user.contact?.firstName || user.contact?.lastName}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.customer.name"/></content>
                                <em>${user.contact.firstName} ${user.contact.lastName}</em>
                            </g:applyLayout>
                        </g:if>

                        <g:if test="${user.contact?.organizationName}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.organization.name"/></content>
                                <em>${user.contact.organizationName}</em>
                            </g:applyLayout>
                        </g:if>

                    </div>
                </div>

                <div><br/></div>

                <div class="buttons">
                    <ul>
                        <li>
                            <a onclick="$('#credit-note-form').submit()"
                               class="submit save button-primary"><span><g:message code="button.save"/></span></a>
                        </li>
                        <li>
                            <g:link action="list" class="submit cancel"><span><g:message
                                    code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>

            </fieldset>
        </g:form>
    </div>
</div>
</body>
</html>
