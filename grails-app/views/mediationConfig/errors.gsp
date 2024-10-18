<%@ page import="com.sapienter.jbilling.server.item.PricingField" %>
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

<html>
<head>
    <meta name="layout" content="main"/>
    <script type="text/javascript">
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

        function submitForFirst() {
            $('#first').val('true');
            $('#order-events-form').submit();
        }
    </script>
</head>
<body>

<g:if test="${mediationConfiguration}">
    <div class="heading">
        <strong><g:message code="event.error.record.heading"/></strong>
    </div>

    <div class="box sub-box">
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="event.error.record.mediation.config.id"/></td>
                <td class="value">${mediationConfiguration?.id}</td>
            </tr>
            <tr>
                <td><g:message code="event.error.record.status.id"/></td>
                <td class="value">
                    <strong><g:formatNumber number="${record?.status}"/></strong>
                </td>
            </tr>
            <g:if test="${errorCodes}">
                <tr>
                    <td><g:message code="event.error.record.errorCodes"/></td>
                    <td class="value">${errorCodes}</td>
                </tr>
            </g:if>
            </tbody>
        </table>
    </div>

    <div class="table-area" id="mediationError">
        <table class="mediation-errors">
            <thead class="innerHeader">
            <tr>
                <td class="first"><g:message code="event.error.th.id"/></td>
                <td><g:message code="event.error.th.key"/></td>
                <td><g:message code="event.error.processId"/></td>
                <td><g:message code="event.error.processing.date"/></td>
                <g:each var="field" in="${pricingFieldsHeader}">
                    <td>${field}</td>
                </g:each>
                <td class="last"><g:message code="event.error.th.codes"/></td>
            </tr>
            </thead>
            <tbody class="innerContent">


            <g:each var="recordLine" in="${records}">
                <tr>
                    <td class="col02">
                        ${recordLine.jBillingCompanyId} - ${recordLine.mediationCfgId} - ${recordLine.recordKey} - ${recordLine.id}
                    </td>
                    <td>
                        ${recordLine.recordKey}
                    </td>
                    <td>
                        ${recordLine.mediationCfgId}
                    </td>
                    <td>
                        ${recordLine.processingDate}
                    </td>
                    <g:each var="pricingFieldValue" in="${PricingField.getPricingFieldsValue(recordLine.pricingFields, pricingFieldsHeader)}">
                        <td>
                            ${pricingFieldValue}
                        </td>
                    </g:each>
                    <td>
                        <strong>
                            ${recordLine.errorCodes}
                        </strong>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
    <div class="form-hold">
        <g:form name="order-events-form" controller="mediationConfig" action="${params.action}" id="${params?.id}">
            <fieldset>
                <div class="form-columns space-bottom">
                    <g:hiddenField name="offset" value="${offset}"/>
                    <div class="row">
                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="event.summary.start.date"/></content>
                            <content tag="label.for">startDate</content>
                            <g:textField class="field " name="startDate" value="${params.startDate}" onblur="validateDate(this)"/>
                        </g:applyLayout>
                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="event.summary.end.date"/></content>
                            <content tag="label.for">endDate</content>
                            <g:textField class="field" name="endDate" value="${params.endDate}" onblur="validateDate(this)"/>
                        </g:applyLayout>
                        <g:hiddenField name="first" value="false"/>
                    </div>
                </div>
            </fieldset>
        </g:form>
    </div>
</g:if>
<div class="pager-box">
    <div class="row">
        <div class="download">
            <sec:access url="/mediation/mediationErrorsCsv">
                <g:link action="mediationErrorsCsv" id="${mediationConfiguration?.id}" class="pager-button"
                        params="${params + ['status': record?.status, errorCodes: errorCodes?.join(':')]}">
                    <g:message code="download.csv.link"/>
                </g:link>
            </sec:access>
        </div>
    </div>
</div>
</body>
</html>
