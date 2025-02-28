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

<%--
  Order builder view.

  This view doubles as a way to render partial page templates by setting the 'template' parameter. This
  is used as a workaround for rendering AJAX responses from within the web-flow.

  @author Brian Cowdery
  @since 25-Jan-2011
--%>

<g:if test="${params.template}">
    <!-- render the template -->
    <g:render template="${params.template}"/>
</g:if>

<g:else>
    <!-- render the main builder view -->
    <html>
    <head>
        <meta name="layout" content="builder"/>
        <r:require module="showtab"/>
        <r:script disposition="head">

            $(document).ready(function() {
                $('#builder-tabs ul.ui-tabs-nav li a').each(function(index, link) {
                    $(link).attr('title', $(link).text());
                });

                $('#review-tabs').tabs({active: ${displayEditChangesTab ? 1 : 0} });
                $('#review-tabs ul.ui-tabs-nav li a').each(function(index, link) {
                    $(link).attr('title', $(link).text());
                });
                $('#builder-tabs').tabs();

                // prevent the Save Changes button to be clicked more than once.
                $('.order-btn-box .submit.save').on('click', function (e) {
                    var saveInProgress = $('#saveInProgress').val();

                    if (saveInProgress == "true") {
                        e.preventDefault();
                    } else {
                        $('#saveInProgress').val("true");
                    }
                });
                $('#order-line-charges-dialog').dialog({
                    autoOpen: false,
                    height: 450,
                    width: 800,
                    modal: true,
                    buttons: [{
                        text: '<g:message code="button.close"/>',
                        click: function() {$(this).dialog('close');}}]
                });
            });
            
            function showOrderLineCharges(id) {
                <g:remoteFunction controller="orderBuilder" action="renderOrderLineCharges" update="order-line-charges" params="'lineId='+id"/>
                $('#order-line-charges-dialog').dialog('open');
            }
        </r:script>
    </head>
    <body>

    <content tag="top">
        <!-- rendering some html and js that should exists only once in the page -->
        <g:render template="assetDialogs"/>
        <g:render template="subscriptionDialog"/>
    </content>

    <content tag="builder">
        <g:render template="orderLineChargesDialog" />
        <div id="builder-tabs">
            <ul>
                <li aria-controls="ui-tabs-details"><a  href="${createLink(action: 'edit', event: 'details')}"><g:message code="builder.details.title"/></a></li>
                <li aria-controls="ui-tabs-suborders"><a  href="${createLink(action: 'edit', event: 'suborders')}"><g:message code="builder.suborders.title"/></a></li>
                <li aria-controls="ui-tabs-products"><a  href="${createLink(action: 'edit', event: 'products')}"><g:message code="builder.products.title"/></a></li>
                <g:if test="${!order.parentOrderId}">
                    <li aria-controls="ui-tabs-plans"><a  href="${createLink(action: 'edit', event: 'plans')}"><g:message code="builder.plans.title"/></a></li>
                </g:if>

                <li aria-controls="ui-tabs-discounts"><a href="${createLink(action: 'edit', event: 'discounts')}"><g:message code="builder.discounts.title"/></a></li>

                <li aria-controls="ui-tabs-changes"><a  href="${createLink(action: 'edit', event: 'orderChanges')}"><g:message code="builder.changes.title"/></a></li>
            </ul>
        </div>
    </content>

    <content tag="review">
        <div id="review-tabs">
            <ul>
                <li aria-controls="ui-tabs-review"><a href="${createLink(action: 'edit', event: 'review')}"><g:message code="builder.review.title"/></a></li>
                <li aria-controls="ui-tabs-edit-changes"><a href="${createLink(action: 'edit', event: 'editOrderChanges')}"><g:message code="builder.edit.changes.title"/></a></li>
            </ul>
        </div>
    </content>

    </body>
    </html>
</g:else>
