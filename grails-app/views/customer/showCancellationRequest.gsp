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
<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactDTO; com.sapienter.jbilling.common.Constants" %>
<html>
<head>
    <g:preferenceEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="1">
        <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet" media="screen, projection" />
        <g:javascript src="jquery.jqGrid.min.js"  />
        <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"  />
    </g:preferenceEquals>
</head>
<body>
<div class="heading"><strong><g:message code="customer.cancellation.request.Details"/></strong></div>
<div class="box narrow">
    <div class="sub-box-no-pad">
        <!-- product info -->
        <table class="dataTable narrow" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                	<td><g:message code="customer.cancellationRequest.cancellationDate"/></td>
                   	<td class="value">${cancellationRequest.cancellationDate}</td>
            </tr>
            <tr>
                	<td><g:message code="customer.cancellationRequest.createTimestamp"/></td>
                   	<td class="value">${cancellationRequest.createTimestamp}</td>
            </tr>
            <tr>
                	<td><g:message code="customer.cancellationRequest.status"/></td>
                   	<td class="value">${cancellationRequest.status}</td>
            </tr>
            <tr>
                	<td><g:message code="customer.cancellationRequest.customer.id"/></td>
                   	<td class="value">${cancellationRequest.customer.id}</td>
            </tr>
            <tr>
                	<td><g:message code="customer.cancellationRequest.reasonText"/></td>
                   	<td class="value">${cancellationRequest.reasonText}</td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
 
  <div class="btn-box">
  <a href="/customer/editCancellationRequest" class="submit add"  ><span><g:message code="customer.edit.cancellation.button"/></span></a>
  <a class="submit delete"><span><g:message code="button.delete"/></span></a>
  
  
    </div>
 
 
</body>
</html>
