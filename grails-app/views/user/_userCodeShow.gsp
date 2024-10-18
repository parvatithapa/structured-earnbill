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

<%--
  Display information for a User Code

  @author Gerhard Maree
  @since  25-Nov-2013
--%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<div class="column-hold">
    <div class="heading">
	    <strong>
	    	${userCode.identifier}
	    </strong>
	</div>

	<div class="box">
        <div class="sub-box">
            <%-- product info --%>
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="userCode.detail.id"/></td>
                        <td class="value">${userCode.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="userCode.detail.identifier"/></td>
                        <td class="value">${userCode.identifier}</td>
                    </tr>
                    <tr>
                        <td><g:message code="userCode.detail.externalReference"/></td>
                        <td class="value">${userCode.externalReference}</td>
                    </tr>
                    <tr>
                        <td><g:message code="userCode.detail.type"/></td>
                        <td class="value">${userCode.type}</td>
                    </tr>
                    <tr>
                        <td><g:message code="userCode.detail.typeDescription"/></td>
                        <td class="value">${userCode.typeDescription}</td>
                    </tr>
                    <tr>
                        <td><g:message code="userCode.detail.validFrom"/></td>
                        <td class="value"><span><g:formatDate formatName="date.pretty.format" date="${userCode.validFrom}"/></span></td>
                    </tr>
                    <tr>
                        <td><g:message code="userCode.detail.validTo"/></td>
                        <td class="value"><span><g:formatDate formatName="date.pretty.format" date="${userCode.validTo}"/></span></td>
                    </tr>

                </tbody>
            </table>

        </div>
    </div>


    <div class="btn-box">

        <sec:ifAllGranted roles="USER_141">
            <g:link action="userCodeEdit" id="${userCode.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>

            <g:if test="${userCode.validTo == null || userCode.validTo.after(TimezoneHelper.currentDateForTimezone(session['company_timezone']))}">
                <a onclick="showConfirm('userCodeDeactivate-${userCode.id}');" class="submit delete"><span><g:message code="button.deactivate"/></span></a>
            </g:if>
        </sec:ifAllGranted>
    </div>

    <g:render template="/confirm"
              model="['message': 'userCode.deactivate.confirm',
                      'controller': 'user',
                      'action': 'userCodeDeactivate',
                      'id': userCode.id,
                      'formParams': ['userId': user.id],
                      'ajax': false
                     ]"/>
</div>

