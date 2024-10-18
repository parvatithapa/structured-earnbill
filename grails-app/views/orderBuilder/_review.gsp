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

<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO;" %>

<%--
  Renders an OrderWS as a quick preview of the order being built. This view also allows
  individual order lines to be edited and removed from the order.

  @author Brian Cowdery
  @since 23-Jan-2011
--%>

<div id="review-box">

    <!-- error messages -->
    <div id="review-messages">
        <g:if test="${params.errorMessages}">
            <div class="msg-box error">
                <ul>
                    %{--flatten is used if in case only string is return then convert to list--}%
                    <g:each var="message" in="${[params.errorMessages]?.flatten().findAll { it != null }}">
                        <li>${message.decodeHTML()}</li>
                    </g:each>
                </ul>
            </div>

            <ul></ul>
        </g:if>

        <g:if test="${repeatedProductError}">
            <div class="msg-box error">
                <ul>
                    <g:each var="message" in="${repeatedProductError}">
                        <li>${message}</li>
                    </g:each>
                </ul>
            </div>
        </g:if>
         <g:if test="${notProratingMessage}">
            <div class="msg-box error">
                <ul>
                    <g:each var="message" in="${notProratingMessage}">
                        <li>${message}</li>
                    </g:each>
                </ul>
            </div>
        </g:if>
        <g:if test="${params.message}">
            <div class="msg-box successfully">
                ${params.message}
            </div>
        </g:if>
    </div>

    <!-- order header -->
    <div class="header">
        <div class="column">
            <h1><g:message code="order.review.id" args="${[order?.id > 0 ? order.id : '']}"/></h1>
            <h3>
                ${user.userName}
            </h3>
        </div>

        <div class="column">
            <h2 class="right capitalize">
                <g:set var="period" value="${orderPeriods.find{ it.id == order.period }}"/>
                ${period?.getDescription(session['language_id'])},

                <g:set var="orderBillingType" value="${orderBillingTypes.find{ it.id == order.billingTypeId }}"/>
                ${orderBillingType?.getDescription(session['language_id'])}
            </h2>

            <h3 class="right capitalize">
                <g:set var="activeSince" value="${formatDate(date: order.activeSince ?: order.createDate, formatName: 'date.pretty.format')}"/>
                <g:set var="activeUntil" value="${formatDate(date: order.activeUntil, formatName: 'date.pretty.format')}"/>

                <g:if test="${order.activeUntil}">
                    <g:message code="order.review.active.date.range" args="[activeSince, activeUntil]"/>
                </g:if>
                <g:else>
                    <g:message code="order.review.active.since" args="[activeSince]"/>
                </g:else>
            </h3>
        </div>

        <div style="clear: both;"></div>
    </div>

    <hr/>

    <g:set var="currency" value="${CurrencyDTO.get(order.currencyId)}"/>
    <!-- list of order lines -->
    <ul id="review-lines">
        <g:each var="line" status="index" in="${order.orderLines}">
            <g:if test="${line.deleted == 0}">
                <g:render template="orderLine" model="[ order: order, line: line, index: index, user: user ]"/>
            </g:if>
        </g:each>

        <g:if test="${!order.orderLines}">
            <li><em><g:message code="order.review.no.order.lines"/></em></li>
        </g:if>
    </ul>

    <hr/>

    <!-- order total -->
    <div class="total">
        <g:message     code = "order.review.total"
                       args = "[formatNumber(           number: order.getTotalAsDecimal() ?: new BigDecimal('0.00'),
                                                          type: 'currency',
                                                currencySymbol: currency.symbol,
                                             maxFractionDigits: 4)]"
                   encodeAs = "None"/>
    </div>

    <!-- order notes -->
    <g:if test="${order.notes}">
        <div class="box-text">
            <ul>
                <li><p>${order.notes}</p></li>
            </ul>
        </div>
    </g:if>

    <div class="btn-box order-btn-box">
        <g:link class="submit save button-primary" action="edit" params="[_eventId: 'save']"  onClick="disableButtons()">
            <span><g:message code="button.save"/></span>
            <g:hiddenField name="saveInProgress" value="false"/>
        </g:link>

        <g:settingEnabled property="hbase.audit.logging">
            <g:if test="${order?.id > 0}">
                <sec:access url="/order/history">
                    <g:link controller="order" action="history" id="${order?.id}" class="submit show">
                        <span><g:message code="button.view.history"/></span>
                    </g:link>
                </sec:access>
            </g:if>
        </g:settingEnabled>

        <g:link class="submit cancel" action="edit" params="[_eventId: 'cancel']">
            <span><g:message code="button.cancel"/></span>
        </g:link>
    </div>

    <script type="text/javascript">
        $('#review-lines li[normal].line').click(function() {
            var id = $(this).attr('id');
            $('#' + id).toggleClass('active');
            $('#' + id + '-editor').toggle('blind');
        });
        function disableButtons() {
            $('.cancel').bind('click', false);
            $('.save').bind('click', false);
        }
    </script>
</div>

<script type="text/javascript">
    showTabWithoutClickIfNeeded('ui-tabs-review');
</script>
