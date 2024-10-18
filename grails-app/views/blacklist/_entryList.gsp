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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.metafields.MetaFieldHelper; com.sapienter.jbilling.client.user.UserHelper; com.sapienter.jbilling.common.Constants" %>
<div class="table-box">
<table cellpadding="0" cellspacing="0" class="blacklist" width="100%">
    <thead>
    <tr>
        <th class="medium first"><g:message code="blacklist.th.name"/></th>
        <th class="small2"><g:message code="blacklist.th.credit.card"/></th>
        <th class="small2 last"><g:message code="blacklist.th.ip.address"/></th>
    </tr>
    </thead>

        <tbody>
            <g:each var="entry" status="i" in="${blacklist}">
                <tr class="${i % 2 == 0 ? 'even' : 'odd'}">
                    <td id="entry-${entry.id}">
                        <g:remoteLink class="cell" action="show" id="${entry.id}" before="register(this);" onSuccess="render(data, next);">
                            <g:set var="name" value="${UserHelper.getDisplayName(entry.user, entry.contact)}"/>
                            ${name ? StringEscapeUtils.escapeHtml(name) : entry.user?.id ?: entry.contact?.userId ?: entry.contact?.id}
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell" action="show" id="${entry.id}" before="register(this);" onSuccess="render(data, next);">
                            <g:if test="${entry.creditCard?.metaFields}">
                            %{-- obscure credit card by default, or if the preference is explicitly set --}%
                                    <g:set var="creditCardNumber" value="${new String(entry.creditCard?.metaFields?.find{it.field.name == Constants.METAFIELD_NAME_CC_NUMBER}?.getValue())?.replaceAll('^\\d{12}','************')}"/>
                                    ${StringEscapeUtils.escapeHtml(creditCardNumber)}
                            </g:if>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:set var="customer" value="${entry.user?.customer}"/>
                        <g:if test="${customer}">
                            <g:remoteLink class="cell" action="show" id="${entry.id}" before="register(this);" onSuccess="render(data, next);">
                                ${StringEscapeUtils.escapeHtml(MetaFieldHelper.getMetaField(customer, ipAddressType?.name)?.getValue())}
                            </g:remoteLink>
                        </g:if>
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>

<strong><g:message code="blacklist.label.entries" args="[blacklist.size()]"/></strong>
