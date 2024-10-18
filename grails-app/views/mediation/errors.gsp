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

        function submitForBack() {
            $('#back').val('true');
            $('#order-events-form').submit();
        }

        function showEntitySpecificRecords(){
            var selected=$('#company-select').val();
            document.location.href = '${createLink(controller: 'mediation' , action:'showMediationErrors')}?selectedEntity=' + selected+'&id=${params.id}'+'&status=${params.status}';
        }
    </script>
</head>
<body>

<div class="table-info">
    <em><g:message code="event.error.record.heading"/></em>
</div>


<div class="">
    <table class="dataTable" cellspacing="0" cellpadding="0">
        <tbody>
        <tr>
            <td class="left-pad"><g:message code="event.error.record.mediation.id"/></td>
            <td class="value">${params?.id}</td>

            <td class="small"><g:message code="event.error.record.status.id"/></td>
            <td class="small value"><g:formatNumber number="${params?.status}"/></td>
            <g:isRoot>
                <td class="small"><g:message code="event.error.record.company"/></td>
                <td class="small value">
                    <g:applyLayout name="form/select_holder">
                        <content tag="label.for">product.entities</content>
                        <g:select id="company-select" name="product.entities" from="${companies}"
                                  optionKey="id" optionValue="${{it?.description?.decodeHTML()}}"
                                  value="${selected}"
                                  onChange="showEntitySpecificRecords()"/>
                    </g:applyLayout>
                </td>
            </g:isRoot>
            <td/>
        </tr>
        </tbody>
    </table>
</div>

    <g:if test="${record}">
        <div class="table-area" style="overflow:scroll;font-size: 10px;padding: 0px 0px;white-space: nowrap;">
            <table class="innerTable">
                <thead class="innerHeader">
                    <tr>
                        <td class="first"><g:message code="event.error.th.id"/></td>
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
            <g:form name="order-events-form" controller="mediation" action="showMediationErrors" id="${params?.id}">
                <fieldset>
                    <div class="form-columns">
                        <div class="row">
                            <g:applyLayout name="form/date">
                                <content tag="label"><g:message code="event.summary.start.date"/></content>
                                <content tag="label.for">event_start_date</content>
                                <g:textField class="field" name="event_start_date" value="${formatDate(date: params?.event_start_date?new Date(params?.event_start_date):null, formatName:'datepicker.format')}" onblur="validateDate(this)"/>
                            </g:applyLayout>
                            <g:applyLayout name="form/date">
                                <content tag="label"><g:message code="event.summary.end.date"/></content>
                                <content tag="label.for">event_end_date</content>
                                <g:textField class="field" name="event_end_date" value="${formatDate(date: params?.event_end_date?new Date(params?.event_end_date):null, formatName:'datepicker.format')}" onblur="validateDate(this)"/>
                            </g:applyLayout>
                            <g:hiddenField name="first" value="false"/>
                            <g:hiddenField name="back" value="false"/>
                            <g:hiddenField name="status" value="${params.status}"/>
                            <g:hiddenField name="offset" value="${offset}"/>
                            <g:hiddenField name="size" value="${size}"/>
                        </div>

                        <div class="row pad-below">
                            <g:if test="${offset > 0}">
                                <a onclick="submitForBack()" class="submit show mediation-error">
                                    <span><g:message code="button.view.back.events"/></span>
                                </a>
                                <a onclick="submitForFirst();" class="submit show mediation-error">
                                    <span><g:message code="button.view.first.events" /></span>
                                </a>
                            </g:if>
                            <g:if test="${next}">
                                <a onclick="$('#order-events-form').submit();" class="submit show mediation-error">
                                    <span><g:message code="button.view.next.events"/></span>
                                </a>
                            </g:if>
                        </div>
                    </div>
                </fieldset>
            </g:form>
        </div>
        <div class="pager-box">
            <div class="row">
                <div class="download">
                    <sec:access url="/mediation/mediationErrorsCsv">
                        <g:link action="mediationErrorsCsv" id="${record?.processId}" class="pager-button"
                                params="${params + ['isDuplicate': record?.errorCodes.contains("JB-DUPLICATE")]}">
                            <g:message code="download.csv.link"/>
                        </g:link>
                    </sec:access>
                </div>
            </div>
        </div>

    </g:if>

</body>
</html>
