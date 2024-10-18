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
<%@ page import="org.apache.commons.lang.StringEscapeUtils" contentType="text/html;charset=UTF-8" %>


<div class="table-box">
    <table id="periods" cellspacing="0" cellpadding="0">
        <thead>
        <tr>
        <th class="medium"><g:message code="accountType.description"/></th>

        </thead>

        <tbody>
        <g:each var="paymentMethod" in="${paymentMethods}">

            <tr id="payment-${paymentMethod.id}" class="${selected?.id == paymentMethod.id ? 'active' : ''}">
                <!-- ID -->
                <td>
                    <g:remoteLink class="cell double" action="show" id="${paymentMethod.id}" before="register(this);"
                                  onSuccess="render(data, next);">
                        <strong>${StringEscapeUtils.escapeHtml(paymentMethod?.methodName)}</strong>
                        <em><g:message code="table.id.format" args="[paymentMethod.id]"/></em>
                    </g:remoteLink>
                </td>

            </tr>

        </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults"
                      model="[steps: [10, 20, 50], update: 'column1']"/>
        </div>
    </div>

    <div class="row">
        <jB:remotePaginate controller="paymentMethodType" action="${action ?: 'list'}"
                             params="${sortableParams(params: [partial: true])}"
                             total="${paymentMethods?.totalCount ?: 0}" update="column1"/>
    </div>
</div>

<div class="btn-box">
    <a href="${createLink(action: 'edit')}" class="submit add button-primary"><span><g:message code="button.create"/></span></a>
</div>
