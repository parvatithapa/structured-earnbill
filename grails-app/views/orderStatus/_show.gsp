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
  Shows order status.

  @author Maruthi
  @since  20-June-2013
--%>

<%@page import="com.sapienter.jbilling.server.order.OrderStatusFlag; com.sapienter.jbilling.server.process.db.PeriodUnitDTO";
@page import="com.sapienter.jbilling.server.order.OrderStatusFlag"%>

<div class="column-hold">
    <div class="heading">
        <strong>
        <em>ORDER STATUS "${selected?.description}"</em>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="orderStatus.orderStatusFlag"/></td>
                <td class="value">${selected.orderStatusFlag}</td>
            </tr>
            <tr>
                <td><g:message code="orderStatus.description"/></td>
                <td class="value">${selected.description}</td>
            </tr>
            </tbody>
        </table>
      </div>
    </div>
	<g:form id="save-orderStatus-form" name="order-orderStatus-form" url="[action: 'delete']" >
		<g:hiddenField name="id" value="${selected.id}"/>
	</g:form>
	
	    <div class="btn-box">
	        <div class="row">
	            <g:remoteLink class="submit add" id="${selected.id}" action="edit" update="column2">
	                <span><g:message code="button.edit"/></span>
	            </g:remoteLink>
	        <g:if test="${selected?.orderStatusFlag?.equals(OrderStatusFlag.INVOICE) || selected?.orderStatusFlag?.equals(OrderStatusFlag.NOT_INVOICE)}">
	            <a onclick="showConfirm('delete-' + ${selected?.id});" class="submit delete"><span><g:message code="button.delete"/></span></a>
	         </g:if>
	           
	        </div>
	    </div>
</div>
<g:render template="/confirm"
     model="[message: 'Are you sure you want to delete order status "'+selected?.description+'" ?',
             controller: 'orderStatus',
             action: 'delete',
             id: selected.id,
            ]"/>