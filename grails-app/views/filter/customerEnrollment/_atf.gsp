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

<%@ page import="com.sapienter.jbilling.server.metafields.EntityType; com.sapienter.jbilling.server.metafields.MetaFieldBL" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.Constants" %>

<%--
  _atf

  @author Khurram Cheema
  @since  23-04-2014
--%>

<div id="${filter.name}">
    <span class="title"><g:message code="enrollment.field" default="Enrollment Fields"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>
    
    <g:if test="${MetaFieldBL.getAllAvailableFieldsList(session['company_id'], EntityType.ACCOUNT_TYPE).toList().size() > 0}">
    <div class="slide">
        <fieldset>
            <div class="input-row">
                %{--<label for="filters.${filter.name}.stringValue"><g:message code="filters.value.label"/></label>--}%
                <div class="select-bg" style="float:right;">
                    <g:set var="company" value="${CompanyDTO.get(session['company_id'])}"/>
                    <g:accountTypeMetaFields filter="${filter}" />

                </div>
                <div class="input-bg">
                    <g:textField  name = "filters.${filter.name}.stringValue"
                                 value = "${filter.stringValue}"
                                 class = "{validate:{ maxlength: 50 }} ${filter.value ? 'autoFill' : ''}"/>
                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.value.label"/></label>
            </div>
        </fieldset>
    </div>
    </g:if>
    <g:else>
    	<fieldset>
    		<div class="slide">
		    	<div class="input-row">
		    		<p>Fields Not Available</p>
		    	</div>
	    	</div>
    	</fieldset>
    </g:else>
</div>

