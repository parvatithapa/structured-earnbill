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

<%@ page import="com.sapienter.jbilling.server.item.CurrencyBL; com.sapienter.jbilling.common.CommonConstants;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.PriceModelBL;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.PriceModelWS;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.db.ChainPosition;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.db.PriceModelDTO;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.db.PriceModelStrategy" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDAS;" %>
<%@ page import="org.apache.commons.lang.WordUtils;" %>

<%--
  Editor form for price models.

  @author Brian Cowdery
  @since  02-Feb-2011
--%>

%{--include the javascript for checking whether the date can be parsed or not--}%

<script type="text/javascript">
    var startDateFormat= "<g:message code="date.format"/>";
</script>

<!-- model and date to display -->
<g:set var="startDate" value="${startDate ?: TimezoneHelper.currentDateForTimezone(session['company_timezone'])}"/>
<g:set var="model" value="${model ?: models ? PriceModelBL.getWsPriceForDate(models, startDate) : null}"/>
<!-- local variables -->
<g:set var="types" value="${PriceModelStrategy.getStrategyByChainPosition(ChainPosition.START)}"/>
<g:set var="type" value="${model?.type ? PriceModelStrategy.valueOf(model.type) : types?.asList()?.first()}"/>
<g:set var="templateName" value="${WordUtils.uncapitalize(WordUtils.capitalizeFully(type.name(), ['_'] as char[]).replaceAll('_',''))}"/>
<g:set var="modelIndex" value="${0}"/>
<g:set var="currencies" value="${new CurrencyBL().getActiveCurrenciesForEntity(session['language_id'] as Integer,
                                                                               session['company_id'] as Integer)}"/>

<g:set var="isNew" value="${model == null || model.id == 0}"/>
<g:set var="isNewProduct" value="${!product || !product?.id || product?.id == 0}"/>

<div id="priceModel">
    <g:hiddenField name="unsaved" value="${unsaved}"/>
    <g:if test="${!isNew && models}">
    <div id="timeline">
        <div class="form-columns">
            <ul>
                <g:each var="modelEntry" in="${models.entrySet()}">
                    <g:if test="${model.equals(modelEntry.getValue()) || startDate.equals(modelEntry.getKey())}">
                        <li class="current">
                            <g:set var="startDate" value="${modelEntry.getKey()}"/>
                            <g:set var="date" value="${formatDate(date: startDate)}"/>
                            <a onclick="editDate('${date}')">${date}</a>
                        </li>
                    </g:if>
                    <g:else>
                        <li>
                            <g:set var="date" value="${formatDate(date: modelEntry.getKey())}"/>
                            <a onclick="editDate('${date}')">${date}</a>
                        </li>
                    </g:else>
                </g:each>

                <g:if test="${!models.containsKey(startDate)}">
                    <li class="current">
                        <g:set var="date" value="${formatDate(date: startDate)}"/>
                        <a onclick="editDate('${date}')">${date}</a>
                    </li>
                </g:if>


                <li class="new">
                    <a onclick="addDate()"><g:message code="button.add.price.date"/></a>
                </li>
            </ul>
        </div>
    </div>
    </g:if>

    <!-- root price model -->
    <div class="form-columns">
        <div class="column">

            <g:if test="${startDate.getTime() == CommonConstants.EPOCH_DATE.getTime()}">
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="plan.item.start.date"/></content>
                    <g:formatDate date="${startDate}" formatName="date.format"/>
                    <g:hiddenField name="startDate" value="${formatDate(date: startDate, formatName: 'date.format')}"/>
                </g:applyLayout>
            </g:if>
            <g:else>
                <g:applyLayout name="form/date">
                    <content tag="label"><g:message code="plan.item.start.date"/></content>
                    <content tag="label.for">startDate</content>
                    <content tag="label.class">toolTipElement</content>
                    <content tag="label.title"><g:message code="price.strategy.COMMON.start_date.tooltip.message"/></content>
                    <g:textField onfocus="disableToolTip(this.id);" class="field toolTipElement" title="${message(code: 'price.strategy.COMMON.start_date.tooltip.message')}" id="startDate" name="startDate" value="${formatDate(date: startDate, formatName: 'datepicker.format')}" onblur="isValidStartDate(this);" />
                    <g:hiddenField name="originalStartDate" value="${formatDate(date: startDate, formatName: 'date.format')}"/>
                </g:applyLayout>
            </g:else>
			<g:hiddenField name="priceModelCompanyId" value="${product?.priceModelCompanyId}"/>
			<g:hiddenField name="forceRefreshModel" value="${0}"/>
			<!-- Company drop down -->
			<g:isRoot>
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="item.price.company"/></content>
                    <content tag="label.for">product.priceModelCompanyId</content>
                    <content tag="label.title"><g:message code="price.strategy.COMMON.company.tooltip.message"/></content>
                    <content tag="label.class">toolTipElement</content>
                    <content tag="include.script">true</content>
                    <g:select name="product.priceModelCompanyId"
                            class="toolTipElement"
                            title="${message(code: 'price.strategy.COMMON.company.tooltip.message')}"
                            from="${allCompanies}"
                            optionKey="id" optionValue="description"
                            noSelection="['': message(code: 'default.no.selection')]"
                            value="${product?.priceModelCompanyId}"
                            onChange="refreshChainModel();"/>
                </g:applyLayout>
			</g:isRoot>
			<g:isNotRoot>
				<g:hiddenField name="product.priceModelCompanyId" value="${product?.priceModelCompanyId ?: session['company_id']}"/>
			</g:isNotRoot>
            %{-- Div for help dialog pop-up --}%
            <div id="${type?.name() + modelIndex}"
                 title="${message(code: 'price.strategy.' + type?.name() + '.help.dialog.title')}"
                 class="help-dialog">
                <g:render template="/priceModel/strategy/${templateName}_help"/>
            </div>

            <g:render id="strategyTemplate" template="/priceModel/strategy/${templateName}" model="[model: model, type: type, modelIndex: modelIndex, types: types, currencies: currencies]"/>
        </div>
        <div class="column">
            <g:render template="/priceModel/attributes" model="[model: model, type: type, modelIndex: modelIndex, templateName: templateName, priceModelData: priceModelData]"/>
        </div>
    </div>

    <!-- price models in chain -->
    <g:set var="types" value="${PriceModelStrategy.getStrategyByChainPosition(ChainPosition.MIDDLE, ChainPosition.END)}"/>
    <g:set var="next" value="${model?.next}"/>
    <g:while test="${next}">
        <g:set var="type" value="${next?.type ? PriceModelStrategy.valueOf(next?.type) : types?.asList()?.first()}"/>
        <g:set var="templateName" value="${WordUtils.uncapitalize(WordUtils.capitalizeFully(type.name(), ['_'] as char[]).replaceAll('_',''))}"/>
        <g:set var="modelIndex" value="${modelIndex + 1}"/>

        <div class="form-columns">
            %{-- Div for help dialog pop-up chained models--}%
            <div id="${type?.name() + modelIndex}"
                 title="${message(code: 'price.strategy.' + type?.name() + '.help.dialog.title')}"
                 class="help-dialog">
                <g:render template="/priceModel/strategy/${templateName}_help"/>
            </div>
            <hr/>
            <div class="left row tooltipElement" style="padding-left: 5em">
                <a class="plus-icon" onclick="removeChainModel(this, ${modelIndex});" title="${message(code: 'price.strategy.COMMON.pricing.remove.chain.tooltip.message')}">
                    &#xe000;
                </a>
            </div>
            <div class="column">
                <g:render template="/priceModel/strategy/${templateName}" model="[model: next, type: type, modelIndex: modelIndex, types: types, currencies: currencies]"/>
            </div>
            <div class="column">
                <g:render template="/priceModel/attributes" model="[model: next, type: type, modelIndex: modelIndex,  priceModelData: priceModelData]"/>
            </div>
        </div>

        <g:set var="next" value="${next.next}"/>
    </g:while>

    <!-- place holder for generated help dialogs -->
    <div id="help-dialog-placeholder"></div>

    <!-- spacer -->
    <div>
        <br/>&nbsp;
    </div>

    <!-- controls -->
    <div class="btn-row">
        <a class="submit add" onclick="addChainModel()"><span><g:message code="button.add.chain"/></span></a>

		<g:if test="${!hideSaveOption && !(!isNew && models) && !isNewProduct}">
		 	<a class="submit save" onclick="saveDate()"><span><g:message code="button.save"/></span></a>
		</g:if>
		 
        <g:if test="${!isNew && models}">
            <a class="submit save" onclick="saveDate()"><span><g:message code="button.save"/></span></a>
            <a class="submit delete" onclick="removeDate()"><span><g:message code="button.delete"/></span></a>
        </g:if>

        <g:hiddenField name="attributeIndex"/>
        <g:hiddenField name="modelIndex"/>
    </div>


    <script type="text/javascript">

        $(document).ready(function() {

            addDataTooltip();

            $('.help-dialog').dialog({
                appendTo: "#help-dialog-placeholder",
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
            // Adjust dynamic attributes padding
            $('.dynamicAttrs').removeClass().addClass("product-price-attribute-dynamic")
        });

        /**
         * Re-render the pricing model form when the strategy is changed
         */
        $(function() {
            $('.model-type').change(function() {
                if(!isValidStartDate($('#startDate'))) {
                    return false;
                }
                else {
                    updateStrategy();
                }
            });
        });

        function updateStrategy() {
            $.ajax({
                type: 'POST',
                url: '${createLink(action: 'updateStrategy')}',
                data: $('#priceModel').parents('form').serialize(),
                success: function(data) { $('#priceModel').replaceWith(data); }
            });
        }

        function editDate(date) {
            $('#startDate').val(date);

            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'editDate')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function addDate() {
            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'addDate')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function removeDate() {
            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'removeDate')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function saveDate() {
        	if(!isValidStartDate($('#startDate'))) {
                return false;
            }
            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'saveDate')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function addChainModel() {
            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'addChainModel')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function removeChainModel(element, modelIndex) {
            $('#modelIndex').val(modelIndex);

            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'removeChainModel')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function addModelAttribute(element, modelIndex, attributeIndex) {
            $('#modelIndex').val(modelIndex);
            $('#attributeIndex').val(attributeIndex);

            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'addAttribute')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function removeModelAttribute(element, modelIndex, attributeIndex) {
            $('#modelIndex').val(modelIndex);
            $('#attributeIndex').val(attributeIndex);

            $.ajax({
                       type: 'POST',
                       url: '${createLink(action: 'removeAttribute')}',
                       data: $('#priceModel').parents('form').serialize(),
                       success: function(data) { $('#priceModel').replaceWith(data); }
                   });
        }

        function isValidStartDate(dateControl) {
            $( "#" + dateControl.id ).tooltip( "enable" );
            //alert(startDateFormat);
            //alert($(dateControl).val());

            if(!isValidDate(dateControl, startDateFormat)) {
                $("#error-messages ul").css("display","block");
                $("#error-messages ul").html("<li><g:message code="product.invalid.startdate.format"/></li>");
                return false;
            } else {
                return true;
            }
        }

        function openHelpDialog(dialogName){
            $('#'+dialogName).css('padding', '10px').dialog('open');
        }

        function disableToolTip(object) {
            $( "#" + object ).tooltip( "disable" );
        }

    </script>
</div>
