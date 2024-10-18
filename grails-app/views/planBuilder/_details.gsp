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

<%@ page import="com.sapienter.jbilling.server.pricing.PriceModelBL; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page import="com.sapienter.jbilling.server.item.db.FreeTrialPeriod" %>
<%--
  Order details form. Allows editing of primary order attributes.

  @author Brian Cowdery
  @since 01-Feb-2011
--%>
<div id="details-box">
    <g:if test="${params.errorMessages}">
        <div class="msg-box error">
            <ul>
                <li>${params.errorMessages}</li>
            </ul>
        </div>
    </g:if>
    <g:set var="isNew" value="${!plan || !plan?.id || plan?.id == 0}"/>
    <g:formRemote name="plan-details-form" url="[action: 'edit']" update="ui-tabs-review" method="GET">
        <g:hiddenField name="_eventId" value="update"/>
        <g:hiddenField name="execution" value="${flowExecutionKey}"/>

        <div class="form-columns">
            <g:applyLayout name="form/input">
                <content tag="label"><g:message code="plan.item.internal.number"/><span
                        id="mandatory-meta-field">*</span></content>
                <content tag="label.for">product.number</content>
                <g:textField class="field text" name="product.number" value="${product?.number}" size="40"/>
            </g:applyLayout>

            <g:render template="/descriptions/descriptions"
                      model="[product: product, itemName: 'product', form_to_submit_on_update: 'plan-details-form']"/>

            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="order.label.period"/></content>
                <content tag="label.for">plan.periodId</content>
                <content tag="include.script">true</content>
                <g:select from="${orderPeriods}"
                          optionKey="id" optionValue="${{ it.getDescription(session['language_id']) }}"
                          name="plan.periodId"
                          value="${plan?.periodId}"/>
            </g:applyLayout>

            <g:set var="defaultProductPrice"
                   value="${PriceModelBL.getWsPriceForDate(product?.defaultPrices, startDate)}"/>

            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="prompt.user.currency"/></content>
                <content tag="label.for">price.currencyId</content>
                <content tag="include.script">true</content>
                <g:select name="price.currencyId"
                          from="${CompanyDTO.get(session['company_id'] as Integer)?.currencies.sort { it.description }}"
                          optionKey="id" optionValue="description"
                          value="${defaultProductPrice?.currencyId}"/>
            </g:applyLayout>

            <g:applyLayout name="form/date">
                <content tag="label"><g:message code="product.detail.availability.start.date"/></content>
                <content tag="label.for">product.activeSince</content>
                <g:textField class="field" name="product.activeSince"
                             value="${formatDate(date: product?.activeSince, formatName: 'datepicker.format')}"/>
            </g:applyLayout>

            <g:applyLayout name="form/date">
                <content tag="label"><g:message code="product.detail.availability.end.date"/></content>
                <content tag="label.for">product.activeUntil</content>
                <g:textField class="field" name="product.activeUntil"
                             value="${formatDate(date: product?.activeUntil, formatName: 'datepicker.format')}"/>
            </g:applyLayout>

            <g:hiddenField name="originalStartDate"
                           value="${formatDate(date: startDate, formatName: 'datepicker.format')}"/>
            <g:if test="${plan?.id}">
                <g:applyLayout name="form/date">
                    <content tag="label"><g:message code="plan.item.start.date"/></content>
                    <content tag="label.for">startDate</content>
                    <content tag="onClose">
                        function(e) {
                        refreshPlan();
                        }
                    </content>
                    <g:textField class="field" name="startDate"
                                 value="${formatDate(date: startDate, formatName: 'datepicker.format')}"/>
                </g:applyLayout>
            </g:if>
            <g:else>
                <g:hiddenField name="startDate"
                               value="${formatDate(date: startDate, formatName: 'datepicker.format')}"/>
            </g:else>


            <g:applyLayout name="form/select_multiple">
                <content tag="label"><g:message code="product.categories"/><span id="mandatory-meta-field">*</span>
                </content>
                <content tag="label.for">product.types</content>
                <g:set var="types" value="${product?.types?.collect { it as Integer }}"/>
                <g:select name="product.types" multiple="true"
                          from="${productCategories}"
                          optionKey="id"
                          optionValue="${{ it.description }}"
                          value="${types ?: categoryId}"/>
                <label for="">&nbsp;</label>
            </g:applyLayout>

            <g:isGlobal>
                <g:applyLayout name="form/checkbox">
                    <content tag="label"><g:message code="product.assign.global"/></content>
                    <content tag="label.for">global-checkbox</content>
                    <g:checkBox id="global-checkbox" onClick="hideCompanies()" class="cb checkbox" name="product.global"
                                checked="${product?.global}"/>
                </g:applyLayout>
            </g:isGlobal>

            <g:isNotRoot>
                <g:hiddenField name="product.global" value="${product?.global}"/>
            </g:isNotRoot>

            <div id="childCompanies">
                <g:isRoot>
                    <g:applyLayout name="form/select_multiple">
                        <content tag="label">
                            <g:message code="product.assign.entities"/>
                            <span id="mandatory-meta-field">*</span>
                        </content>
                        <content tag="label.for">product.entityId</content>
                        <g:select id="company-select" multiple="multiple" name="product.entities" from="${allCompanies}"
                                  optionKey="id" optionValue="${{ it?.description }}"
                                  value="${allCompanies.size == 1 ? allCompanies?.id : product?.entities}"
                                  onChange="${remoteFunction(action: 'retrieveMetaFields',
                                          update: 'product-metafields',
                                          params: '\'entities=\' + getSelectValues(this)')}"/>
                    </g:applyLayout>
                </g:isRoot>
                <g:isNotRoot>
                    <g:if test="${product?.entities?.size() > 0}">
                        <g:each in="${product?.entities}">
                            <g:hiddenField name="product.entities" value="${it}"/>
                        </g:each>
                    </g:if>
                    <g:else>
                        <g:hiddenField name="product.entities" value="${session['company_id']}"/>
                    </g:else>
                </g:isNotRoot>
            </div>


            <g:applyLayout name="form/input">
                <content tag="label"><g:message code="plan.model.rate"/></content>
                <content tag="label.for">price.rateAsDecimal</content>
                <g:textField class="field text" name="price.rateAsDecimal"
                             value="${formatNumber(number: defaultProductPrice?.rate, formatName: 'price.format.edit')}"/>
            </g:applyLayout>

        %{--<g:applyLayout name="form/checkbox">
            <content tag="label"><g:message code="plan.editable"/></content>
            <content tag="label.for">plan.editable</content>
            <g:checkBox  name="plan.editable" checked="${plan?.editable > 0}"/>
        </g:applyLayout>--}%
            <g:applyLayout name="form/select_multiple">
                <content tag="label"><g:message code="plan.usagePoolIds"/></content>
                <content tag="label.for">plan.usagePoolIds</content>
                <g:set var="fups" value="${plan?.usagePoolIds?.collect { it as Integer }}"/>
                <g:select name="plan.usagePoolIds" multiple="true"
                          from="${usagePools}"
                          optionKey="id"
                          noSelection="['': message(code: 'default.no.selection')]"
                          optionValue="${{ it.getUsagePoolNameByLanguageId(session['language_id']) }}"
                          value="${fups ?: usagePoolId}"/>
                <label for="">&nbsp;</label>
            </g:applyLayout>
            <br/>
            <!-- meta fields -->
            <div id="product-metafields">
                <g:render template="/metaFields/editMetaFields"
                          model="[availableFields: availableFields, fieldValues: plan?.metaFields]"/>
            </div>

            <div>
                <g:applyLayout name="form/checkbox">
                    <content tag="label"><g:message code="filters.plan.free.trial"/></content>
                    <content tag="label.for">freeTrial</content>
                    <g:checkBox class="cb checkbox" id="plan-freeTrial" name="plan.freeTrial"
                                checked="${plan.freeTrial}" onClick="toggleFreeTrial()"/>
                </g:applyLayout>
            </div>

            <div id="freeTrial">
                %{-- <div id="numberOfFreeCalls">
                     <g:applyLayout name="form/input">
                         <content tag="label"><g:message code="filters.plan.free.call.limit"/></content>
                         <content tag="label.for">plan.numberOfFreeCalls</content>
                         <g:textField class="field text" type="number" pattern="^[0-9]" name="plan.numberOfFreeCalls"
                                      value="${plan?.numberOfFreeCalls}" size="40"/>
                     </g:applyLayout>
                 </div>--}%
                <div id="freeTrialPeriod">
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="filters.plan.free.trial.period"/></content>
                        <content tag="label.for">plan.freeTrialPeriodUnit</content>

                        <div class="inp-bg inp4">
                            <g:textField class="field text" type="number" pattern="^[0-9]" name="plan.freeTrialPeriodValue"
                                         value="${plan?.freeTrialPeriodValue}" size="40"/>
                        </div>

                        <div class="select4">
                            <g:applyLayout name="form/select_holder">
                                <content tag="holder.class">select-holder-nofloat</content>
                                <content tag="label.for">plan.freeTrialPeriodUnit</content>
                                <content tag="include.script">true</content>
                                <g:select from="${FreeTrialPeriod.ALL}"
                                   optionKey="key" optionValue="${{ message(code: it.getMessageKey()) }}"
                                   name="plan.freeTrialPeriodUnit"
                                   value="${plan?.freeTrialPeriodUnit}"/>
                            </g:applyLayout>
                        </div>
                    </g:applyLayout>
                </div>
            </div>
        </div>

        <hr/>

        <div class="form-columns">
            <div class="box-text">
                <label class="lb"><g:message code="plan.description"/></label>
                <g:textArea name="plan.description" rows="5" cols="60" value="${plan?.description}"/>
            </div>
        </div>
    </g:formRemote>

    <script type="text/javascript">
        $(document).ready(function () {
            toggleFreeTrial();
            if ($("#global-checkbox").is(":checked")) {
                $("#company-select").attr('disabled', true);
            }
        });

        $('#plan-details-form').find('select').change(function () {
            $('#plan-details-form').submit();
        });

        $('#plan-details-form').find('input:checkbox').change(function () {
            $('#plan-details-form').submit();
        });

        $('#plan-details-form').find('input.text').blur(function () {
            $('#plan-details-form').submit();
        });

        $('.date').find('[type=text]').change(function () {
            $('#plan-details-form').submit();
        });

        $('#plan-details-form').find('textarea').blur(function () {
            $('#plan-details-form').submit();
        });

        function hideCompanies() {
            $("#company-select option").removeAttr("selected");
            if ($("#global-checkbox").is(":checked")) {
                $("#company-select").attr('disabled', true);
                $.ajax({
                    type: 'POST',
                    url: '${createLink(action: 'retrieveAllMetaFields')}',
                    success: function (data) {
                        document.getElementById('product-metafields').innerHTML = data;
                    }
                });
            } else {
                $("#company-select").removeAttr('disabled');
                $.ajax({
                    type: 'POST',
                    url: '${createLink(action: 'getAvailableMetaFields')}',
                    success: function (data) {
                        document.getElementById('product-metafields').innerHTML = data;
                    }
                });
            }
        }

        function getSelectValues(select) {
            var result = [];
            var options = select && select.options;
            var opt;

            for (var i = 0, iLen = options.length; i != iLen; i++) {
                opt = options[i];

                if (opt.selected) {
                    result.push(opt.value || opt.text);
                    result.push(",")
                }
            }
            return result;
        }

        function toggleFreeTrial(){
            if ($("#plan-freeTrial").is(":checked")) {
                $("#freeTrial").show();
            }else{
                $("#freeTrial").hide();
                $("#plan.freeTrialPeriodValue").val(null);
                $("#plan.freeTrialPeriodUnit").val(null);
            }
        }
    </script>
</div>


