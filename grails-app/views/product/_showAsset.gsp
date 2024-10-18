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

<%@ page import="com.sapienter.jbilling.server.item.AssetReservationBL;"%>
<%@ page import="com.sapienter.jbilling.server.item.AssetStatusBL;"%>
<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO;"%>
<%@ page import="com.sapienter.jbilling.server.item.db.AssetStatusDTO;"%>

<%--
  Display information on an asset including meta fields, transitions

  @author Gerhard Maree
  @since  14-Apr-2013
--%>


<div class="column-hold">
    <div class="heading">
	    <strong>
	    	${asset.identifier}
	    	<g:if test="${asset.deleted}">
                <span style="color: #ff0000;">(<g:message code="object.deleted.title"/>)</span>
            </g:if>
	    </strong>
	</div>

	<div class="box">
        <div class="sub-box">
            <%-- product info --%>
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="asset.detail.id"/></td>
                        <td class="value">${asset.id}</td>
                    </tr>
                    <tr>
                        <td>${category?.assetIdentifierLabel?: g.message([code: "asset.detail.identifier"])}</td>
                        <td class="value">${asset.identifier}</td>
                    </tr>
                    <tr>
                        <g:if test="${asset.isReserved()}">
                            <td><g:message code="asset.detail.assetStatus"/></td>
                            <td class="value"><g:message code="asset.reserved.status"/></td>
                            <tr>
                                <td><g:message code="asset.detail.reservation.period"/></td>
                                <td class="value">
                                     <g:message code="asset.detail.reservation.period.value" args="[reservation?.getStartDate(), reservation?.getEndDate()]"/>
                                </td>
                            </tr>
                        </g:if><g:else>
                            <td><g:message code="asset.detail.assetStatus"/></td>
                            <td class="value">${asset.assetStatus?.description}</td>
                        </g:else>
                    </tr>
                    <tr>
                        <td><g:message code="asset.detail.product.category"/></td>
                        <td class="value">${asset.item.internalNumber}</td>
                    </tr>
                    <tr>
                        <td><g:message code="asset.detail.order"/></td>
                        <td class="value"><g:link controller="order" action="list" id="${asset.orderLine?.purchaseOrder?.id}">${asset.orderLine?.purchaseOrder?.id}</g:link></td>
                    </tr>
                    <g:if test="${asset.group != null}" >
                        <tr>
                            <td><g:message code="asset.detail.group"/></td>
                            <td class="value"><g:link controller="product" action="showAsset" id="${asset.group.id}">${asset.group.id}</g:link> ${asset.group.identifier}</td>
                        </tr>
                    </g:if>
                    <g:if test="${asset.containedAssets.size() > 0}" >
                        <tr>
                            <td><g:message code="asset.detail.contained.assets"/></td>
                            <td class="value">
                                <g:each in="${asset.containedAssets}" var="containedAsset">
                                    <g:link controller="product" action="showAsset" id="${containedAsset.id}">${containedAsset.id}</g:link> ${containedAsset.identifier},
                                </g:each>
                            </td>
                        </tr>
                    </g:if>
                    <g:render template="/metaFields/metaFields" model="[metaFields: asset.metaFields]"  />
                </tbody>
            </table>

        </div>
    </div>

    <%-- Assignments Box --%>
    <g:set var="assignments" value="${asset?.assignments?.asList().sort{ it.startDatetime.time * -1 }}" />
    <g:render template="assignments" model="[assignments: assignments]" />

    <%-- Transitions Box --%>
    <g:set var="transitions" value="${asset?.transitions?.asList().sort{ it.createDatetime.time * -1 } }" />
    <g:render template="transitions" model="[transitions: transitions]" />


    <div class="btn-box">
    <g:if test="${!asset.deleted}">
        <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_131">
            <g:link action="editAsset" id="${asset.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
        </sec:ifAllGranted>

        <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_132">
            <g:if test="${asset?.entity?.id == session['company_id']}">
                <a onclick="showConfirm('deleteAsset-${asset.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </g:if>
        </sec:ifAllGranted>

        <g:if test="${asset.isReserved()}">
            <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_133">
                <a onclick="showConfirm('releaseAssetReservation-${asset.id}');" class="submit release">
                    <span><g:message code="button.release.reservation"/></span>
                </a>
            </sec:ifAllGranted>
        </g:if>

        <g:link      class = "submit show"
                controller = "provisioning"
                    action = "showCommands"
                    params = "[          type: 'ASSET',
                               typeIdentifier: asset.id]">
            <span><g:message code="button.view.commands" /></span>
        </g:link>
        <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_131">
            <g:link action="copyAsset" id="${asset.id}" class="submit copy">
                <span><g:message code="button.copy"/></span>
            </g:link>
        </sec:ifAllGranted>        
	</g:if>
    </div>

    <g:render template="/confirm"
              model="['message': 'product.asset.delete.confirm',
                      'controller': 'product',
                      'action': 'deleteAsset',
                      'id': asset.id,
                      'formParams': ['itemId': asset.item.id],
                     ]"/>

    <g:render template="/confirm"
              model="['message': 'product.asset.release.reservation.confirm',
                      'controller': 'product',
                      'action': 'releaseAssetReservation',
                      'id': asset.id,
                      'formParams': ['itemId': asset.item.id, 'partial': 'true']
                     ]"/>

</div>

