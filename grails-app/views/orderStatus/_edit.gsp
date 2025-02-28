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

<%@ page contentType="text/html;charset=UTF-8" %>
<%@page import="com.sapienter.jbilling.server.order.OrderStatusFlag" %>
<%@page import="com.sapienter.jbilling.client.util.Constants" %>

<%--
  Shows edit form for a contact type.

  @author Maruthi
  @since  20-Jun-2013
--%>

<div class="column-hold">
    
    <g:set var="isNew" value="${!orderStatusWS || !orderStatusWS?.id || orderStatusWS?.id == 0}"/>
    
    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="orderStatus.add.title"/>
            </g:if>
            <g:else>
                <g:message code="orderStatus.edit.title"/>
            </g:else>
        </strong>
    </div>

    <g:form id="save-orderStatus-form" name="order-orderStatus-form" url="[action: 'save']" useToken="true">
    <input type="hidden" name="isNew" value="${isNew}">
    <div class="box">
        <div class="sub-box">
          <fieldset>
            <div class="form-columns">
                <g:hiddenField name="id" value="${orderStatusWS?.id}"/>
                <g:if test="${orderStatusWS?.orderStatusFlag==OrderStatusFlag.FINISHED  || orderStatusWS?.orderStatusFlag==OrderStatusFlag.SUSPENDED_AGEING}">
                	<g:hiddenField name="orderStatusFlag" value="${orderStatusWS?.orderStatusFlag}"/>
                </g:if>

	             <g:applyLayout name="form/select">
	                <content tag="label"><g:message code="Flag"/></content>
	                <content tag="label.for">orderStatusFlag</content>
	                <content tag="include.script">true</content>
	                <g:select name="orderStatusFlag" from="${OrderStatusFlag.values()}" value="${orderStatusWS?.orderStatusFlag}" disabled="${orderStatusWS?.orderStatusFlag==OrderStatusFlag.FINISHED  || orderStatusWS?.orderStatusFlag==OrderStatusFlag.SUSPENDED_AGEING}"/>
           		</g:applyLayout>
           		
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="Description"/><span id="mandatory-meta-field">*</span></content>
                    <content tag="label.for">description</content>
                    <g:textField class="field" name="description" value="${orderStatusWS?.description}"/>
                </g:applyLayout>
                
            </div>
        </fieldset>
      </div>

        <div class="btn-box buttons">
            <ul>
                <li><a class="submit save button-primary" onclick="$('#save-orderStatus-form').submit();"><span><g:message code="button.save"/></span></a></li>
                <li><a class="submit cancel" href="index"><span><g:message code="button.cancel"/></span></a></li>
            </ul>
        </div>
    </div>

    </g:form>
</div>
