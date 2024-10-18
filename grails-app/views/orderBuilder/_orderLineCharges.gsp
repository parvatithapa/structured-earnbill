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

<%@page import="com.sapienter.jbilling.server.order.db.OrderLineDTO"%>

<g:each var = "charge" in = "${OrderLineDTO.get(lineId)?.orderChangesSortedByStartDate}">
<tr>
    <td class = "left">${formatDate(date: charge.startDate,        formatName: 'date.format')}</td>
    <td class = "left">${formatDate(date: charge.endDate,          formatName: 'date.format')}</td>
    <td class = "left">${formatDate(date: charge.createDatetime,   formatName: 'date.format')}</td>
    <td class = "left">${formatDate(date: charge.nextBillableDate, formatName: 'date.format')}</td>
    <td class = "qty">${formatNumber(number: charge.quantity,      formatName: 'decimal.format')}</td>
    <td class = "money">${formatNumber(number: charge.price,       formatName: 'price.format')}</td>
</tr>
</g:each>
