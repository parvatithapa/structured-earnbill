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

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.db.AssetStatusDTO;  com.sapienter.jbilling.server.item.db.AssetDTO" %>

<%--
  Shows the list of selected assets. Users are able to remove assets from the list.

  @author Gerhard Maree
  @since 24-April-2011
--%>
<ul class="cloud">
    <li style="background: white">
        <div style="font-size: 14px; color: #000000">
            <strong><g:message code="assets.label.selected"/></strong>
        </div>
    </li>
    <g:each var="asset" in="${selectedAssets}">
        <li>

            <strong>${asset.identifier}</strong>

            <g:remoteLink class="cell double" action="edit" id="${asset.id}"
                          params="[_eventId: 'removeAsset']" update="assets-table-${assetFlow}" method="GET">
                <span>&#x00D7;</span>
            </g:remoteLink>
        </li>
    </g:each>
</ul>
