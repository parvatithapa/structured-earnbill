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
  
<%@page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.process.db.PeriodUnitDTO" %>
<%@page import="com.sapienter.jbilling.server.order.OrderStatusFlag" %>
<%@page import="com.sapienter.jbilling.client.util.Constants" %>



<%@ page contentType="text/html;charset=UTF-8" %>

<%--
  Shows a list of order Statuses.

  @author Maruthi
  @since  20-June-2013
--%>

<div class="table-box">
    <table id="orderStatuses" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
               <th class="tiny"><g:message code="orderStatus.id"/></th>
                <th class="large"><g:message code="orderStatus.flag"/></th>
                <th class="large"><g:message code="orderStatus.description"/></th>
                
            </tr>
        </thead>


        <tbody>
            <g:each var="orderStatus" in="${orderStatusList}">
                <tr id="orderStatus-${orderStatus.id}" >
                    
                    <!-- Order Status Id -->
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${orderStatus.id}" before="register(this);" onSuccess="render(data, next);">
                            ${orderStatus?.id}
                        </g:remoteLink>
                    </td>
                    <!-- Flag -->
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${orderStatus.id}" before="register(this);" onSuccess="render(data, next);">
                            ${orderStatus?.orderStatusFlag}
                        </g:remoteLink>
                    </td>
                     <!-- Description -->
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${orderStatus.id}" before="register(this);" onSuccess="render(data, next);">
                            ${StringEscapeUtils.escapeHtml(orderStatus?.description)}
                        </g:remoteLink>
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>

    <div class="btn-hold">
        <div class="btn-box">
            <g:remoteLink class="submit add button-primary" action="edit" before="register(this);"
                          onSuccess="render(data, next);">
                <span><g:message code="button.create"/></span>
            </g:remoteLink>
        </div>
    </div>

</div>
