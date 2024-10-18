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

<%@ page import="com.sapienter.jbilling.server.pricing.PriceModelBL; com.sapienter.jbilling.server.pricing.db.PriceModelDTO; com.sapienter.jbilling.server.util.db.CurrencyDTO; com.sapienter.jbilling.server.util.db.LanguageDTO; com.sapienter.jbilling.server.item.db.ItemTypeDTO" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<script type="text/javascript">
    var expiryDateFormat= "<g:message code="date.format"/>";

    function isValidStartDate(dateControl) {
        //alert(startDateFormat);
        //alert($(dateControl).val());

        if(!isValidDate(dateControl, expiryDateFormat)) {
            $("#error-messages ul").css("display","block");
            $("#error-messages ul").html("<li><g:message code="price.invalid.expiry.date.format"/></li>");
            return false;
        } else {
            return true;
        }
    }

</script>

<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>
<div class="form-edit">

    <div class="heading">
        <strong>
            <g:if test="${!price.id}">
                <g:message code="account.type.price.new.title"/>
            </g:if>
            <g:else>
                <g:message code="account.type.update.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="save-price-form" action="saveAccountTypePrice" useToken="true">
            <fieldset>
                <div class="form-columns">
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="product.internal.number"/></content>
                            <g:link controller="product" action="list" id="${product.id}">
                                ${product.number}
                            </g:link>
                            <g:hiddenField name="price.itemId" value="${product.id}"/>
                            <g:hiddenField name="price.id" value="${price.id}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="product.description"/></content>
                            ${product.description}
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="plan.item.precedence"/></content>
                            <content tag="label.for">price.precedence</content>
                            <g:textField class="field" name="price.precedence" value="${price.precedence}"/>
                        </g:applyLayout>

                    </div>

                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="account.type.id"/></content>
                            <span><g:link controller="accountType" action="list" id="${accountType.id}">${accountType.id}</g:link></span>
                            <g:hiddenField name="accountTypeId" value="${accountType.id}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="plan.item.price.expiry.date"/></content>
                            <content tag="label.for">priceExpiryDate</content>
                            <content tag="label.class">toolTipElement</content>
                            <content tag="label.title"><g:message code="plan.item.price.COMMON.expiry_date.tooltip.message"/></content>
                            <g:textField class="field toolTipElement toolTipHideShow" title="${message(code: 'plan.item.price.COMMON.expiry_date.tooltip.message')}"
                                         id="priceExpiryDate" name="priceExpiryDate" value="${formatDate(date: priceExpiryDate, formatName: 'datepicker.format')}" onblur="isValidStartDate(this);" />
                        </g:applyLayout>

                    </div>

                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>

                <!-- pricing controls -->
                <div class="box-cards box-cards-open">
                    <div class="box-cards-title">
                        <span><g:message code="account.type.price.title"/></span>
                    </div>
                    <div class="box-card-hold">
                        <g:if test="${price.id}">
                            <g:render template="/priceModel/model" model="[startDate:price.models.firstKey(),
                                                                           model: price.models.get(price.models.firstKey()) ,
                                                                           hideSaveOption:hideSaveOption]"/>
                        </g:if>
                        <g:else>
                            <g:render template="/priceModel/model" model="[model: PriceModelBL.getWsPriceForDate(price.models, TimezoneHelper.currentDateForTimezone(session['company_timezone'])),
                                                                           hideSaveOption:hideSaveOption]"/>
                        </g:else>
                    </div>
                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>

                <div class="buttons">
                    <ul>
                        <li><a onclick="$('#save-price-form').submit();" class="submit save"><span><g:message code="button.save"/></span></a></li>
                        <li><g:link controller="accountType" action="listPrices" id="${accountType.id}" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link></li>
                    </ul>
                </div>

            </fieldset>
        </g:form>
    </div>

</div>
</body>
</html>
