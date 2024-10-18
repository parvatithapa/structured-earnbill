
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
  Recent items side panel.

  @author Brian Cowdery
  @since  09-12-2010
--%>

<div id="recent-items">
    <div class="heading">
        <strong><g:message code="recent.items.title"/></strong>
    </div>
    <ul class="list">
        <g:set var="recentItemService" bean="recentItemService"/>
        <g:each var="item" in="${recentItemService.load()}">
            <g:if test="${item != null}">
                <g:set var="type" value="${item.type}"/>
                <li>
                    <g:link controller="${type.controller}" action="${type.action}" id="${item.objectId != null ? item.objectId : item.uuid}" params="${type.params}">
                        <img src="${resource(dir:'images', file:type.icon)}" alt="${type.messageCode}"/>
                        <g:message code="${type.messageCode}" args="${item.objectId ? item.objectId : item.uuid ?: ''}"/>
                    </g:link>
                </li>
            </g:if>
        </g:each>
    </ul>
</div>