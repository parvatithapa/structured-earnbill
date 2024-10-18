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

<%--
    The controller will update this template whether JQGrid
     is enable or not

    @author Nelson Secchi
    @since 29/04/2014
 --%>
<%@ page import="com.sapienter.jbilling.common.Constants" %>

<g:preferenceEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="1">
    <g:render template="allGrid" model=""/>
</g:preferenceEquals>
<g:preferenceIsNullOrEquals preferenceId="${Constants.PREFERENCE_USE_JQGRID}" value="0">
    <g:render template="all" model=""/>
</g:preferenceIsNullOrEquals>
