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

<%@ page import="com.sapienter.jbilling.server.util.db.CountryDTO" %>
    <g:set var="contactDisabled" value="true"/>
    <sec:ifAnyGranted roles="MY_ACCOUNT_162,USER_147">
        <g:set var="contactDisabled" value="false"/>
    </sec:ifAnyGranted>

    <g:hiddenField name="contact.id" value="${contact?.id}"/>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.organization.name"/></content>
        <content tag="label.for">contact?.organizationName</content>
        <g:textField class="field" name="contact.organizationName" value="${contact?.organizationName}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.first.name"/></content>
        <content tag="label.for">contact.firstName</content>
        <g:textField class="field" name="contact.firstName" value="${contact?.firstName}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.last.name"/></content>
        <content tag="label.for">contact.lastName</content>
    	<g:textField class="field" name="contact.lastName" value="${contact?.lastName}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/text">
        <content tag="label"><g:message code="prompt.phone.number"/></content>
        <content tag="label.for">contact.phoneCountryCode</content>
        <span>
            <g:textField class="field" name="contact.phoneCountryCode" value="${contact?.phoneCountryCode}" maxlength="3" size="2" readonly="${contactDisabled}"/>
            -
            <g:textField class="field" name="contact.phoneAreaCode" value="${contact?.phoneAreaCode}" maxlength="5" size="3" readonly="${contactDisabled}"/>
            -
            <g:textField class="field" name="contact.phoneNumber" value="${contact?.phoneNumber}" maxlength="10" size="8" readonly="${contactDisabled}"/>
        </span>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.email"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">contact.email</content>
        <g:textField class="field" name="contact.email" value="${contact?.email}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.address1"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">contact.address1</content>
        <g:textField class="field" name="contact.address1" value="${contact?.address1}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.address2"/></content>
        <content tag="label.for">contact.address2</content>
        <g:textField class="field" name="contact.address2" value="${contact?.address2}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.city"/></content>
        <content tag="label.for">contact.city</content>
        <g:textField class="field" name="contact.city" value="${contact?.city}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.state"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">contact.stateProvince</content>
        <g:textField class="field" name="contact.stateProvince" value="${contact?.stateProvince}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="prompt.zip"/></content>
        <content tag="label.for">contact.postalCode</content>
        <g:textField class="field" name="contact.postalCode" value="${contact?.postalCode}" readonly="${contactDisabled}"/>
    </g:applyLayout>

    <g:applyLayout name="form/select">
        <content tag="label"><g:message code="prompt.country"/><span id="mandatory-meta-field">*</span></content>
        <content tag="label.for">contact.countryCode</content>
        <g:select name="contact.countryCode"
                 from="${CountryDTO.list()}"
                 optionKey="code"
                 optionValue="${{ it.getDescription(session['language_id']) }}"
                 noSelection="['': message(code: 'default.no.selection')]"
                 value="${contact?.countryCode}" disabled="${contactDisabled}"/>

    </g:applyLayout>

    <g:applyLayout name="form/checkbox">
        <content tag="label"><g:message code="prompt.include.in.notifications"/></content>
        <content tag="label.for">contact.include</content>
        <g:checkBox class="cb checkbox" name="contact.include" checked="${contact?.include}" disabled="${contactDisabled}"/>
    </g:applyLayout>
</div>
