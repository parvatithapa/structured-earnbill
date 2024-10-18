%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<%@ page import="com.sapienter.jbilling.server.user.partner.PartnerCommissionValueWS; com.sapienter.jbilling.server.user.partner.PartnerCommissionType; com.sapienter.jbilling.server.user.partner.PartnerType; com.sapienter.jbilling.server.user.ContactWS; com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page import="com.sapienter.jbilling.server.user.permisson.db.RoleDTO" %>
<%@ page import="com.sapienter.jbilling.common.Constants" %>
<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO" %>
<%@ page import="com.sapienter.jbilling.server.process.db.PeriodUnitDTO" %>
<%@ page import="com.sapienter.jbilling.client.user.UserHelper" %>

<html>
<head>
    <meta name="layout" content="main"/>

    <style type="text/css">
    .date img {
        top: -17px !important;
        right: 2px !important;
    }
    </style>
</head>

<body>
<div class="form-edit">

<g:set var="isNew" value="${!user || !user?.userId || user?.userId == 0}"/>
<g:set var="defaultCurrency" value="${CompanyDTO.get(session['company_id']).getCurrency()}"/>

<div class="heading">
    <strong>
        <g:if test="${isNew}">
            <g:message code="partner.create.title"/>
        </g:if>
        <g:else>
            <g:message code="partner.edit.title"/>
        </g:else>
    </strong>
</div>

<div class="form-hold">
<g:form name="user-edit-form" action="save" useToken="true">
<fieldset>
<div class="form-columns">

<!-- user details column -->
<div class="column">
<g:applyLayout name="form/text">
    <content tag="label"><g:message code="prompt.customer.number"/></content>

    <g:if test="${!isNew}">
        <span>${partner.id}</span>
    </g:if>
    <g:else>
        <em><g:message code="prompt.id.new"/></em>
    </g:else>

    <g:hiddenField name="user.userId" value="${user?.userId}"/>
    <g:hiddenField name="id" value="${partner?.id}"/>

    <g:hiddenField name="totalPayments" value="${partner?.totalPayments ?: 0}"/>
    <g:hiddenField name="totalRefunds" value="${partner?.totalRefunds ?: 0}"/>
    <g:hiddenField name="totalPayouts" value="${partner?.totalPayouts ?: 0}"/>
    <g:hiddenField name="duePayout" value="${partner?.duePayout ?: 0}"/>

</g:applyLayout>

<g:if test="${isNew}">
    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.login.name"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">user.userName</content>
        <g:textField class="field" name="user.userName" value="${user?.userName}"/>
    </g:applyLayout>
</g:if>
<g:else>
    <g:applyLayout name="form/text">
        <content tag="label"><g:message code="prompt.login.name"/><span id="mandatory-meta-field">*</span></content>

        ${user?.userName}
        <g:hiddenField name="user.userName" value="${user?.userName}"/>
    </g:applyLayout>
</g:else>

<g:if test="${!isNew && user?.userId == loggedInUser?.id}">
   
    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.current.password"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">oldPassword</content>
        <g:passwordField class="field" name="oldPassword"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.password"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">newPassword</content>
        <g:passwordField class="field" name="newPassword"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.verify.password"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">verifiedPassword</content>
        <g:passwordField class="field" name="verifiedPassword"/>
    </g:applyLayout>
</g:if>

<!-- PARTNER CREDENTIALS -->
<g:if test="${isNew}">
    <g:preferenceEquals preferenceId="${Constants.PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT}" value="0">
        <g:applyLayout name="form/checkbox">
            <content tag="label"><g:message code="prompt.create.credentials"/></content>
            <content tag="label.for">user.createCredentials</content>
            <g:checkBox class="cb checkbox" name="user.createCredentials" checked="${user?.createCredentials}"/>
        </g:applyLayout>
    </g:preferenceEquals>
</g:if>

<g:applyLayout name="form/input">
    <content tag="label"><g:message code="prompt.partner.broker"/></content>
    <content tag="label.for">brokerId</content>
    <g:textField class="field" name="brokerId" value="${partner.brokerId}"/>
</g:applyLayout>

<g:if test="${partner?.parentId || !parentId}">
    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.parent.id"/></content>
        <content tag="label.for">parentId</content>
        <g:textField class="field" name="parentId" value="${partner.parentId ?: parentId}"/>
    </g:applyLayout>
</g:if>
<g:else>
    <g:applyLayout name="form/text">
        <content tag="label"><g:message code="prompt.parent.id"/></content>
        ${parentId}
        <g:hiddenField class="field" name="parentId" value="${parentId}"/>
    </g:applyLayout>
</g:else>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.user.status"/></content>
    <content tag="label.for">user.statusId</content>
    <g:userStatus name="user.statusId" value="${user?.statusId}" languageId="${session['language_id']}"/>
</g:applyLayout>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.user.language"/></content>
    <content tag="label.for">user.languageId</content>
    <g:select name="user.languageId" from="${LanguageDTO.list(sort : "id",order :"asc")}"
              optionKey="id" optionValue="description" value="${user?.languageId}"/>
</g:applyLayout>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.user.currency"/></content>
    <content tag="label.for">user.currencyId</content>
    <g:select name="user.currencyId"
              from="${currencies}"
              optionKey="id"
              optionValue="${{ it.getDescription() }}"
              value="${user?.currencyId ?: defaultCurrency?.id}"/>
</g:applyLayout>

<g:applyLayout name="form/text">
    <content tag="label"><g:message code="prompt.user.role"/></content>
    <content tag="label.for">user.mainRoleId</content>
    ${RoleDTO.findByRoleTypeId(Constants.TYPE_PARTNER)?.getTitle(session['language_id'])}
</g:applyLayout>

<g:set var="isReadOnly" value="true"/>
<sec:ifAllGranted roles="CUSTOMER_11">
    <g:set var="isReadOnly" value="false"/>
</sec:ifAllGranted>
<g:applyLayout name="form/checkbox">
    <content tag="label"><g:message code="user.account.lock"/></content>
    <content tag="label.for">user.isAccountLocked</content>
    <g:checkBox class="cb checkbox" name="user.isAccountLocked" checked="${user?.isAccountLocked}" disabled="${isReadOnly}"/>
</g:applyLayout>

<g:applyLayout name="form/checkbox">
    <content tag="label"><g:message code="user.account.inactive"/></content>
    <content tag="label.for">user.accountExpired</content>
    <g:checkBox class="cb checkbox" name="user.accountExpired" checked="${user?.accountExpired}" disabled="${isReadOnly}"/>
</g:applyLayout>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.partner.type"/></content>
    <content tag="label.for">type</content>
    <g:select name="type"
              from="${PartnerType.values()}"
              valueMessagePrefix="PartnerType"
              value="${partner?.type?PartnerType.valueOf(partner?.type):partner?.type}"/>
</g:applyLayout>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="prompt.partner.commissionType"/></content>
    <content tag="label.for">commissionType</content>
    <g:select name="commissionType"
              from="${PartnerCommissionType.values()}"
              valueMessagePrefix="PartnerCommissionType"
              noSelection="['': message(code: 'default.no.selection')]"
              onchange="checkCommissionType()"
              value="${partner?.commissionType?PartnerCommissionType.valueOf(partner?.commissionType):partner?.commissionType}"/>
</g:applyLayout>

<div id="commission-single">
    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.partner.fee"/></content>
        <content tag="label.for">commissionFee</content>
        %{--<g:hiddenField name="partner.commission.value.0.days" value="0"/>--}%
        <g:textField class="field" name="commissionFee" value="${partner.commissionValues?partner.commissionValues[0]?.rate : ''}"/>
    </g:applyLayout>
</div>

<%
    if(partner.commissionValues == null || partner.commissionValues.length == 0) {
        partner.commissionValues = [new PartnerCommissionValueWS(0, "")]
    }
%>
<div id="commission-dynamic">
<g:each in="${partner.commissionValues}" var="commissionValue" status="valueIdx">
    <div id="val-row-${valueIdx}">
        <g:applyLayout name="form/attribute" >
            <content tag="name">
                <g:if test="${valueIdx == 0}">
                    0
                    <g:hiddenField name="partner.commission.value.0.days" value="0"/>
                </g:if>
                <g:else>
                <g:textField class="field toolTipElement" id="days-${valueIdx}"
                             title="${message(code: 'partner.commission.value.days.tooltip.message')}"
                             name="partner.commission.value.${valueIdx}.days" value="${commissionValue.days}"/>
                </g:else>
            </content>
            <content tag="value">
                <div class="inp-bg">
                    <g:textField class="field toolTipElement" id="rate-${valueIdx}"
                                 title="${message(code: 'partner.commission.value.rate.tooltip.message')}"
                                 name="partner.commission.value.${valueIdx}.rate" value="${formatNumber(number: commissionValue.rate?:0, maxFractionDigits: 4)}"/>
                </div>
            </content>

            <a class="plus-icon" id="val-remove-${valueIdx}"
               onclick="removeCommissionValue(${valueIdx})">
                &#xe000;
            </a>
        </g:applyLayout>
    </div>
</g:each>

%{-- one empty row --}%
<div id="val-row-last">
    <g:set var="valueIdx" value="${partner.commissionValues.length}"/>
    <g:applyLayout name="form/attribute">
        <content tag="name">
            <g:textField class="field toolTipElement" id="comm-val-days"
                         title="${message(code: 'partner.commission.value.days.tooltip.message')}"
                         name="partner.commission.value.${valueIdx}.days" value=""/>
        </content>
        <content tag="value">
            <div class="inp-bg">
                <g:textField class="field toolTipElement" id="comm-val-rate"
                             title="${message(code: 'partner.commission.value.rate.tooltip.message')}"
                             name="partner.commission.value.${valueIdx}.rate" value=""/>
            </div>
        </content>

        <a id="add-${valueIdx}" class="plus-icon"
           onclick="addCommissionValue()">
            &#xe026;
        </a>
    </g:applyLayout>
</div>
</div>

</div>

<!-- contact information column -->
<div class="column">

    <g:set var="contact" value="${contacts && contacts.size > 0 ? contacts[0] : new ContactWS()}"/>
    <g:render template="/customer/contact" model="[contact: contact]"/>

    <br/>&nbsp;

<!-- customer meta fields -->
<g:render template="/metaFields/editMetaFields" model="[
        availableFields: availableFields, fieldValues: user?.metaFields]"/>

    <g:if test="${ssoActive}">
        <sec:ifAllGranted roles="USER_158">

            <g:if test="${availableFields.size()>0 && companyInfoTypes.size() > 0}">
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="prompt.idp.configurations"/></content>
                    <content tag="label.for">idpConfigurationIds</content>
                    <content tag="include.script">true</content>
                    <g:select id="idp-configuration-select" name="idpConfigurationIds"
                              from="${companyInfoTypes}"
                              optionKey="id"
                              optionValue="name"
                              value="${defaultIdp}"/>
                </g:applyLayout>
            </g:if>

        </sec:ifAllGranted>
    </g:if>
</div>

</div>
<!-- commission exceptions -->
<div id="commission-exception" class="box-cards">
    <div class="box-cards-title">
        <a class="btn-open" href="#"><span><g:message code="partner.commission.exception"/></span></a>
    </div>

    <div class="box-card-hold">
        <g:render template="commissionExceptions" model="[partner: partner]"/>
    </div>
</div>

<div><br/></div>

<!-- referral commissions -->
<div id="referral-commissions" class="box-cards">
    <div class="box-cards-title">
        <a class="btn-open" href="#"><span><g:message code="partner.referral.commissions"/></span></a>
    </div>

    <div class="box-card-hold">
        <g:render template="referralCommissions" model="[partner: partner]"/>
    </div>
</div>

<div><br/></div>

<div class="buttons">
    <ul>
        <li>
            <a onclick="$('#user-edit-form').submit()" class="submit save button-primary"><span><g:message code="button.save"/></span>
            </a>
        </li>
        <li>
            <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
        </li>
    </ul>
</div>

</fieldset>
</g:form>
</div>
</div>

%{--
    Template that will be used to add a new commission. The following variables gets replaced
    _%VALIDX%_ - Value index. Each row has a unique value
--}%
<div id="commissionValueTemplate" style="display: none;">
    <div id="val-row-_%VALIDX%_">
        <g:applyLayout name="form/attribute">
            <content tag="name">
                <g:textField class="field toolTipElement"
                             title="${message(code: 'partner.commission.value.days.tooltip.message')}"
                             name="partner.commission.value._%VALIDX%_.days" value="_%DAYS%_"/>
            </content>
            <content tag="value">
                <div class="inp-bg">
                    <g:textField class="field toolTipElement"
                                 title="${message(code: 'partner.commission.value.rate.tooltip.message')}"
                                 name="partner.commission.value._%VALIDX%_.rate" value="_%RATE%_"/>
                </div>
            </content>

            <a id="val-remove-_%VALIDX%_" class="plus-icon"
               onclick="removeCommissionValue(_%VALIDX%_)">
                &#xe000;
            </a>
        </g:applyLayout>
    </div>
</div>

<script type="text/javascript">
    var commissionValueTemplate;
    var commissionValueIdx = ${partner.commissionValues.length + 1};

    function removeCommissionValue(rowIdx) {
        $("#val-row-"+rowIdx).remove();
    }

    function addCommissionValue() {
        var template = commissionValueTemplate.clone().html()
                .replace(/_%VALIDX%_/g, commissionValueIdx)
                .replace(/_%DAYS%_/g, $("#comm-val-days").val())
                .replace(/_%RATE%_/g, $("#comm-val-rate").val());

        console.log("template="+template);
        $(template.trim()).insertBefore("#val-row-last");

        $("#comm-val-days").val("");
        $("#comm-val-rate").val("");

        $('#val-remove-'+commissionValueIdx).click(function() {
            removeCommissionValue(commissionValueIdx);
        });
        commissionValueIdx++;
    }

    function checkCommissionType() {
        var type = $("#commissionType").val();
        if(type == '${PartnerCommissionType.CUSTOMER}') {
            $("#commission-dynamic").hide();
            $("#commission-single").show();
        } else {
            $("#commission-dynamic").show();
            $("#commission-single").hide();
        }

    }

    $(document).ready(function() {
        $("[id^='val-row']").find('.dynamicAttrs').removeClass().css({'padding-left':'68px','overflow':'visible'});
//    $('#commissionValueTemplate').find('.dynamicAttrs').removeClass().css({'padding-left':'68px','overflow':'visible'});
        commissionValueTemplate = $("#commissionValueTemplate").detach();
        checkCommissionType();
    });

</script>

</body>
</html>
