%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
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
  Configurable Customer Information Screen.
--%>

<%@ page import="com.sapienter.jbilling.server.sapphire.cis.SappAsset; com.sapienter.jbilling.server.sapphire.cis.SappInvoinceLine; com.sapienter.jbilling.server.sapphire.cis.SappPayment; com.sapienter.jbilling.server.sapphire.cis.SappOrderLine;com.sapienter.jbilling.server.spa.cis.DistAsset; com.sapienter.jbilling.server.spa.cis.DistInvoinceLine; com.sapienter.jbilling.server.spa.cis.DistPayment; com.sapienter.jbilling.server.spa.cis.DistOrderLine; com.sapienter.jbilling.server.spa.DistributelOrderWS; com.sapienter.jbilling.server.util.db.CurrencyDTO; com.sapienter.jbilling.server.fileProcessing.FileConstants; com.sapienter.jbilling.server.item.db.ItemDAS; com.sapienter.jbilling.server.item.db.ItemDTO; com.sapienter.jbilling.server.payment.PaymentWS; com.sapienter.jbilling.server.invoice.InvoiceWS; com.sapienter.jbilling.server.order.OrderWS; java.lang.reflect.Array; com.sapienter.jbilling.server.customerInspector.domain.*; org.apache.commons.lang.StringUtils; org.apache.commons.lang.math.NumberUtils"%>

<style>
    .element {
        width: 690px;
        max-width: 690px;
        display: inline-block;
        overflow-y: auto;
    }
    .element2 {
        width: 400px;
        max-width: 400px;
        display: inline-block;
        vertical-align: top;
    }
    .element4 {
        width: 420px;
        max-width: 450px;
        min-width: 390px;
        display: inline-block;
        vertical-align: top;        
    }
    .fixed_headers {
        width: 100%;
        table-layout: auto;
        border-collapse: collapse;
        border-width: 0;
    }
    .fixed_headers thead {
        background-color: gray;
        color: #fdfdfd;
    }
    .fixed_headers tbody tr:nth-child(even) {
        background-color: #dddddd;
    }
    .dist-fixed-headers {
        width: 100%;
        table-layout: auto;
        border-collapse: collapse;
        border-width: 0;
    }
    .dist-fixed-headers thead {
        background-color: #ffffff;
        color: gray;
    }
    .dist-fixed-column-header {
        font-weight: normal;
        padding-left: 6px;
        padding-right: 6px;
    }
    .dist-btn-row {
        text-align: center;
        margin-top: 0;
        padding-top: 0;
        margin-left:auto;
        margin-right:auto;        
    }
    .dist-tombstone-table-data {
        width: 115px;
        text-align: left;
        padding-right: 0;
    }
    .dist-table-element {
        width: 430px;
    }
    .dist-table-element td {
        text-align: left;
    }
    td.dist-dataTable{
        vertical-align: top;
        color: #000000;
        text-align: left;
        padding-left: 6px;
        padding-right: 6px;
    }
    .link-element {
        display: inline-block;
        vertical-align: top;
    }
    .link-element a:hover span {
        color: #0a0a0a ;
    }
    .dist-table {
        clear: both;
    }
    .dist-table-header {
        width: 920px;
        max-width: 920px;
        font-weight: bold;
        color: #858585;
        display: inline-block;
    }
    .dist-table-header .title {
        float: left;
        text-align: left;
    }
    .dist-table-header .comment {
        float: right;
        text-align: right;
    }
    .dist-link {
        color: #009abf !important;
    }
    .dist-link:hover {
        color: #006080 !important;
    }
</style>

<div class="dataTable">
    <g:each var="row" in="${customerInformation?.rows}">
        <div class="${isDistributel ? 'dist-btn-row' : 'box-cards btn-row'}" >
            <g:each var="column" in="${row?.columns}">
                <g:if test="${null!=column && column?.field}">
                    <g:if test="${column?.field instanceof ListField}">
                        <div class="dist-table-header">
                            <g:if test="${column?.field?.getTitle()}">
                                <div class="title">${column?.field?.getTitle()}</div>
                            </g:if>
                            <g:if test="${column?.field?.getComment()}">
                                <div class="comment">${column?.field?.getComment()}</div>
                            </g:if>
                        </div>
                        <div class="${isDistributel ? 'dist-table' : ''}">
                            <div class="element" style="${column?.field?.style}">
                                <table id="listField" class="${isDistributel ? 'dist-fixed-headers' : 'fixed_headers'}" frame="vsides">
                                    <thead>
                                        <tr>
                                            <g:set var="listFieldProperties" value="${column?.field?.properties}"/>
                                            <g:set var="labels" value="${column?.field?.properties}"/>
    
                                            <g:if test="${column?.field?.getLabels() && column?.field?.getLabels().size()==listFieldProperties.size()}">
                                                <g:set var="labels" value="${column?.field?.getLabels()}"/>
                                            </g:if>
    
                                            <g:if test="${column?.field?.getWidths()}">
                                                <g:set var="widths" value="${column?.field?.getWidths()}"/>
                                            </g:if>
                                            
                                            <g:each var="label" in="${labels}" status="i">
                                                <g:if test="${widths}">
                                                    <th align="left" class="${isDistributel ? 'dist-fixed-column-header' : ''}" style="width: ${widths[i]}%;">${StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(label), ' ').capitalize()}</th>
                                                </g:if>
                                                <g:else>
                                                    <th align="left" class="${isDistributel ? 'dist-fixed-column-header' : ''}">${StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(label), ' ').capitalize()}</th>
                                                </g:else>
                                            </g:each>
                                        </tr>
                                    </thead>
                                    <tbody>
                                    <g:each var="element" in="${column?.field.getValue(user?.id)}">
                                        <tr style="background-color:white">
                                            <g:each var="listFieldProperty" in="${listFieldProperties}">
                                                <g:if test="${column?.field.getType().equals(ListField.Type.ORDER)}">
                                                    <g:set var="classObject" value="${OrderWS.class}"/>
                                                </g:if>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.DIST_ORDER_LINE)}">
                                                    <g:set var="classObject" value="${DistOrderLine.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.DIST_INVOICE_LINE)}">
                                                    <g:set var="classObject" value="${DistInvoinceLine.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.DIST_PAYMENT)}">
                                                    <g:set var="classObject" value="${DistPayment.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.DIST_ASSET)}">
                                                    <g:set var="classObject" value="${DistAsset.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.SAPP_ORDER_LINE)}">
                                                    <g:set var="classObject" value="${SappOrderLine.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.SAPP_INVOICE_LINE)}">
                                                    <g:set var="classObject" value="${SappInvoinceLine.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.SAPP_PAYMENT)}">
                                                    <g:set var="classObject" value="${SappPayment.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.SAPP_ASSET)}">
                                                    <g:set var="classObject" value="${SappAsset.class}"/>
                                                </g:elseif>
                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.INVOICE)}">
                                                    <g:set var="classObject" value="${InvoiceWS.class}"/>
                                                </g:elseif>
                                                <g:else>
                                                    <g:set var="classObject" value="${PaymentWS.class}"/>
                                                </g:else>
                                                <td class="${isDistributel ? 'dist-dataTable' : ''}" align="left" style="background-color:white">
                                                    <g:set var="currency" value="${new com.sapienter.jbilling.server.util.db.CurrencyDAS().find(session['currency_id'])}"/>
                                                    <g:if test="${column?.field.isValidProperty(listFieldProperty, classObject)}">
                                                        <g:set var="listFieldPropertyValue"
                                                               value='${fieldValue(bean: element, field: listFieldProperty)}'/>
                                                        <g:if test="${isDistributel && listFieldProperty.equals("id")}">
                                                            <g:if test="${column?.field.getType().equals(ListField.Type.DIST_ASSET)}">
                                                                <g:set var="controller" value="product"/>
                                                                <g:set var="action" value="showAsset"/>
                                                            </g:if>
                                                            <g:else>
                                                                <g:set var="action" value="list"/>
                                                                <g:if test="${column?.field.getType().equals(ListField.Type.DIST_ORDER_LINE)}">
                                                                    <g:set var="controller" value="order"/>
                                                                </g:if>
                                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.DIST_INVOICE_LINE)}">
                                                                    <g:set var="controller" value="invoice"/>
                                                                </g:elseif>
                                                                <g:else>
                                                                    <g:set var="controller" value="payment"/>
                                                                </g:else>
                                                            </g:else>
                                                            <g:link controller="${controller}" action="${action}" id="${listFieldPropertyValue}" params="[filterEntity: true, max: 10]">
                                                                ${listFieldPropertyValue}
                                                            </g:link>
                                                        </g:if>
                                                        <g:elseif test="${listFieldProperty.equals("id")}">
                                                            <g:if test="${column?.field.getType().equals(ListField.Type.SAPP_ASSET)}">
                                                                <g:set var="controller" value="product"/>
                                                                <g:set var="action" value="showAsset"/>
                                                            </g:if>
                                                            <g:else>
                                                                <g:set var="action" value="list"/>
                                                                <g:if test="${column?.field.getType().equals(ListField.Type.SAPP_ORDER_LINE)}">
                                                                    <g:set var="controller" value="order"/>
                                                                </g:if>
                                                                <g:elseif test="${column?.field.getType().equals(ListField.Type.SAPP_INVOICE_LINE)}">
                                                                    <g:set var="controller" value="invoice"/>
                                                                </g:elseif>
                                                                <g:else>
                                                                    <g:set var="controller" value="payment"/>
                                                                </g:else>
                                                            </g:else>
                                                            <g:link controller="${controller}" action="${action}" id="${listFieldPropertyValue}" params="[filterEntity: true, max: 10]">
                                                                ${listFieldPropertyValue}
                                                            </g:link>
                                                        </g:elseif>
                                                        <g:elseif test="${listFieldProperty.equals("orderId")}">
                                                                <g:set var="action" value="list"/>
                                                                <g:if test="${column?.field.getType().equals(ListField.Type.SAPP_ASSET)}">
                                                                    <g:set var="controller" value="order"/>
                                                                </g:if>
                                                            <g:link controller="${controller}" action="${action}" id="${listFieldPropertyValue}" params="[filterEntity: true, max: 10]">
                                                                ${listFieldPropertyValue}
                                                            </g:link>
                                                        </g:elseif>
                                                        <g:elseif test="${!listFieldProperty.contains("Id") && listFieldPropertyValue.isNumber()}">
                                                            <g:if test="${column?.field.isMoneyProperty(listFieldProperty)}">
                                                                ${currency?.symbol}</g:if><g:formatNumber number="${listFieldPropertyValue.toString()}" formatName="decimal.format"/>
                                                        </g:elseif>
                                                        <g:elseif test="${listFieldPropertyValue && isDistributel && column?.field.getLinks() != null && column?.field.getLinks().contains(listFieldProperty)}">
                                                            <div class="link-element">
                                                                <table>
                                                                    <tr>
                                                                        <td>                                                                            
                                                                            <a target="_blank">
                                                                                <input type="hidden" value="${listFieldPropertyValue}" name="url">
                                                                                <span class="${isDistributel ? 'dist-link' : ''}">Link</span>
                                                                            </a>
                                                                        </td>
                                                                    </tr>
                                                                </table>
                                                            </div>
                                                        </g:elseif>
                                                        <g:else>${listFieldPropertyValue}</g:else>
                                                    </g:if>
                                                    <g:elseif
                                                            test="${listFieldProperty?.equals('nges_rate') && column?.field.getType().equals(ListField.Type.INVOICE)}">
                                                        <g:set var="invoiceWS" value="${(InvoiceWS) element}"/>
                                                        <g:set var="value" value="${invoiceWS.invoiceLines.find {
                                                            [FileConstants.COMMODITY_ELECTRICITY.toLowerCase(), FileConstants.COMMODITY_GAS.toLowerCase()].contains(new ItemDAS().find(it.itemId)?.internalNumber)
                                                        }?.priceAsDecimal}"/>
                                                        <g:formatNumber number="${value}" type="currency" currencySymbol="${currency?.symbol}" maxFractionDigits="5"/>
                                                    </g:elseif>
                                                    <g:elseif
                                                            test="${listFieldProperty?.equals('nges_quantity') && column?.field.getType().equals(ListField.Type.ORDER)}">
                                                        <g:set var="orderWS" value="${(OrderWS) element}"/>
                                                        <g:set var="value" value="${orderWS.orderLines.find {
                                                            [FileConstants.COMMODITY_ELECTRICITY.toLowerCase(), FileConstants.COMMODITY_GAS.toLowerCase()].contains(new ItemDAS().find(it.itemId)?.internalNumber)
                                                        }?.quantity}"/>
                                                        <g:formatNumber number="${value}" formatName="decimal.format"/>
                                                    </g:elseif>
                                                </td>
                                            </g:each>
                                        </tr>
                                    </g:each>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </g:if>
                    <g:elseif test="${column?.field instanceof StaticField && column?.field?.header}">
                        <div class="box-cards-title">
                            <span style="${column?.field?.style}">${column?.field.getValue(user?.id)}</span>
                        </div>
                    </g:elseif>
                    <g:elseif test="${column?.field instanceof SpecificField && !column?.field.getType().equals(SpecificField.SpecificType.TEXT)}">
                        <g:if test="${column?.field.getType().equals(SpecificField.SpecificType.BUTTON)}">
                            <div class="link-element">
                                <table style="${column?.field?.style}">
                                    <tr>
                                        <td>
                                            <g:link controller="customerInspector" action="redirectAction" class="submit" params='[urlRedirect:"${column?.field.getValue(user?.id)}"]' target="${column?.field?.target}">
                                                <span><g:message code="${column?.field?.label}"/></span>
                                            </g:link>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                        </g:if>
                        <g:if test="${column?.field.getType().equals(SpecificField.SpecificType.LINK)}">
                            <div class="link-element">
                                <table style="${column?.field?.style}">
                                    <tr>
                                        <td>
                                            <g:link controller="customerInspector" action="redirectAction" params='[urlRedirect:"${column?.field.getValue(user?.id)}"]'>
                                                <span class="${isDistributel ? 'dist-link' : ''}"><g:message code="${column?.field?.label}"/></span>
                                            </g:link>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                        </g:if>
                    </g:elseif>
                    <g:else>
                        <div class="${isDistributel ? 'element4' : 'element2'}">
                            <table class="${isDistributel ? 'dist-table-element' : ''}">
                                <tr>
                                    <td class="${isDistributel ? 'dist-tombstone-table-data' : ''}">
                                        <g:set var="fieldValue" value="${column?.field.getValue(user?.id)}"/>
                                        <g:if test="${column?.field instanceof StaticField}">
                                            <g:if test="${column?.field?.label}">
                                                <label><span style="${column?.field?.style}">${column.field.label}:</span></label>
                                            </g:if>
                                        </g:if>
                                        <g:else>
                                            <g:if test="${column?.field?.label || column?.field?.name}">
                                                <label><span>${column.field.label ?: column.field.name}:</span></label>
                                            </g:if>
                                        </g:else>
                                    </td>
                                    <td>
                                        <g:if test="${fieldValue instanceof java.util.Collection}">
                                            <g:each in="${fieldValue}" var="value">
                                                <li style="list-style-type:none">
                                                    <span class="value" style="${column?.field?.style}">${value}</span>
                                                </li>
                                            </g:each>
                                        </g:if>
                                        <g:else>
                                            <span class="value" style="${column?.field?.style}; display: inline-block;">
                                                <g:if test="${column?.field.isMoneyProperty(listFieldProperty)}">
                                                    $
                                                </g:if>
                                                <g:if test="${fieldValue instanceof java.lang.Boolean}">
                                                    <g:formatBoolean boolean="${fieldValue}"/>
                                                </g:if>
                                                <g:elseif test="${fieldValue instanceof java.util.Date}">
                                                    <g:formatDate date="${fieldValue}" formatName="date.pretty.format"/>
                                                </g:elseif>
                                                <g:else>${fieldValue}</g:else>
                                            </span>
                                        </g:else>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </g:else>
                </g:if>
            </g:each>
        </div>
    </g:each>
    
    <form id="redirectPostURL" target="_blank" method="POST"></form>
    
</div>

<script type="text/javascript">
    $("span.dist-link").parent().on('click', function () {
        var form = document.getElementById("redirectPostURL");        
        var url = $(this).find('input[type="hidden"]').val();
        var parameters = url.split("?")[1].split("&");
        url = url.split("?")[0];      
        
        // Remove the elements that already exist into the form
        $(form).empty();
        // Append an input for each parameter
        for (var i=0; i < parameters.length; i++) {
            var parameterName = parameters[i].split('=')[0];
            var parameterValue = parameters[i].split('=')[1];
            var input = document.createElement("input");
            input.type = "hidden";
            input.name = parameterName;
            input.value = parameterValue;
            form.appendChild(input);
        }
        form.action = url;
        form.submit();
    });
</script>