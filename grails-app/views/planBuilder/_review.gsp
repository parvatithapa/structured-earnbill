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

<%@ page import="com.sapienter.jbilling.common.CommonConstants; com.sapienter.jbilling.server.pricing.PriceModelBL; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.util.Constants" %>
<%--
  Renders a PlanWS as a quick preview of the plan being built. This view also allows
  individual plan prices to be edited and removed from the order.

  @author Brian Cowdery
  @since 01-Feb-2011
--%>

<div id="review-box">
	<script type="text/javascript">
        showTabWithoutClickIfNeeded('ui-tabs-review');
    </script>
    <!-- error messages -->
    <div id="messages">
        <g:if test="${errorMessages}">
            <div class="msg-box error">
                <ul>
                    <g:each var="message" in="${errorMessages}">
                        <li>${message}</li>
                    </g:each>
                </ul>
            </div>

            <g:set var="errorMessages" value=""/>
        </g:if>
    </div>

    <!-- review -->
	<!-- plan review header -->
     <div class="header">
         <div class="column">
             <h2 class="product-description">${product?.description} &nbsp;</h2>
         </div>
         <div class="column">
             <h2 class="right">
                 <g:set var="defaultProductPrice" value="${PriceModelBL.getWsPriceForDate(product?.defaultPrices, startDate)}"/>

                 <g:if test="${defaultProductPrice}">
                     <g:set var="currency" value="${currencies.find{ it.id == defaultProductPrice.currencyId }}"/>
                     <g:set var="price" value="${formatNumber(number: defaultProductPrice.getRateAsDecimal(), type: 'currency', currencySymbol: currency.symbol.decodeHTML())}"/>
                 </g:if>
                 <g:else>
                     <g:set var="currency" value="${CompanyDTO.get(session['company_id']).currency}"/>
                     <g:set var="price" value="${formatNumber(number: BigDecimal.ZERO, type: 'currency', currencySymbol: currency.symbol.decodeHTML())}"/>
                 </g:else>

                 <g:if test="${plan?.periodId == Constants.ORDER_PERIOD_ONCE}">
                     <g:message code="plan.review.onetime.price" args="[price]"/>
                 </g:if>
                 <g:else>
                     <g:set var="orderPeriod" value="${orderPeriods.find{ it.id == plan?.periodId }}"/>
                     <g:message code="plan.review.period.price" args="[price, orderPeriod?.getDescription(session['language_id'])]"/>
                 </g:else>
             </h2>
             <h3 class="right"><g:message code="plan.review.view.on.date" args="[formatDate(date: startDate)]"/></h3>
         </div>

         <div style="clear: both;"></div>
     </div>

     <hr/>

     <!-- list of item prices (ordered by precedence) -->
     <ul id="review-lines">
         <g:each var="planItem" status="index" in="${plan?.planItems}">
     	   	<li>
             	<g:render template="priceLine" model="[ planItem: planItem, index: index, startDate: startDate ]"/>
                <g:render template="priceLinePlanItems" model="[ planItem: planItem, startDate: startDate, level: 0 ]"/>
             </li>
         </g:each>

         <g:if test="${!plan?.planItems}">
             <li><em><g:message code="plan.review.no.prices"/></em></li>
         </g:if>
     </ul>

     <!-- plan notes -->
     <g:if test="${plan?.description}">
         <hr/>
         <div class="box-text">
             <ul>
                 <li><p>${plan?.description}</p></li>
             </ul>
         </div>
     </g:if>

    <!-- buttons -->
    <div class="btn-box">
        <g:link class="submit save button-primary" action="edit" params="[_eventId: 'save']" onclick="submitPlanDetailsForm()">
            <span><g:message code="button.save"/></span>
        </g:link>

        <g:if test="${!CommonConstants.EPOCH_DATE.equals(startDate)}">
            <g:remoteLink class="submit delete" action="edit" params="[_eventId: 'removeDate', startDate: formatDate(date: startDate), index: index]" update="ui-tabs-review" method="GET" onSuccess="refreshPlan();">
                <span><g:message code="button.remove"/></span>
            </g:remoteLink>
        </g:if>

        <g:settingEnabled property="hbase.audit.logging">
            <g:if test="${plan.id}">
                <sec:access url="/plan/history">
                    <g:link controller="plan" action="history" id="${plan?.id}" class="submit show"><span><g:message code="button.view.history"/></span></g:link>
                </sec:access>
            </g:if>
        </g:settingEnabled>

        <g:link class="submit cancel" action="edit" params="[_eventId: 'cancel']">
            <span><g:message code="button.cancel"/></span>
        </g:link>
    </div>

    <!-- place holder for generated help dialogs -->
    <div id="plan-help-dialog-placeholder"></div>
    
    <script type="text/javascript">

        function submitPlanDetailsForm() {
            $('#plan-details-form').submit();
        }

        $('#review-lines li.line').click(function() {
            var id = $(this).attr('id');
            $('#' + id).toggleClass('active');
            $('#' + id + '-editor').toggle('blind');
        });

        $('.model-type').change(function() {
            var form = $(this).parents('form');
            form.find('[name=_eventId]').val('updateStrategy');
            form.submit();
        });

        $(document).ready(function() {
            // Adjust dynamic attributes padding
            $('.dynamicAttrs').removeClass().addClass("plan-price-attribute-dynamic");
            addDataTooltip();

            var help = $('#plan-help-dialog-placeholder');
            help.detach();
            $('body').append(help);

            // Initialize dialog
            $('.plan-help-dialog').dialog({
                appendTo: "#plan-help-dialog-placeholder",
                autoOpen: false,
                height: 380,
                width: 550,
                resizable: false,
                modal: true,
                // Workaround for modal dialog dragging jumps
                create: function(event){
                    $(event.target).parent().css('position', 'fixed');
                },
                buttons: {
                    "${g.message(code: 'button.close')}": function() {
                        $( this ).dialog( "close" );
                    }
                }
            });

        });

        function addChainModel(element) {
            var form = $(element).parents('form');
            form.find('[name=_eventId]').val('addChainModel');
            form.submit();
        }

        function removeChainModel(element, modelIndex) {
            var form = $(element).parents('form');
            form.find('[name=_eventId]').val('removeChainModel');
            form.find('[name=modelIndex]').val(modelIndex);
            form.submit();
        }

        function addModelAttribute(element, modelIndex, attributeIndex) {
            var form = $(element).parents('form');
            form.find('[name=_eventId]').val('addAttribute');
            form.find('[name=modelIndex]').val(modelIndex);
            form.find('[name=attributeIndex]').val(attributeIndex);
            form.submit();
        }

        function removeModelAttribute(element, modelIndex, attributeIndex) {
            var form = $(element).parents('form');
            form.find('[name=_eventId]').val('removeAttribute');
            form.find('[name=modelIndex]').val(modelIndex);
            form.find('[name=attributeIndex]').val(attributeIndex);
            form.submit();
        }

        function openHelpDialog(dialogName){
            $('#'+dialogName).dialog('open');
        }

    </script>
</div>
<script type="text/javascript">
    showTabWithoutClickIfNeeded('ui-tabs-review');
</script>
