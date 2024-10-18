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
<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO" %>
<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactMapDTO" %>

<g:set var="contact" value="${company?.contact}"/>

<g:javascript src="jquery.colorpicker.js"/>
<link type="text/css" href="${resource(file: '/css/jquery.colorpicker.css')}" rel="stylesheet"/>

<script>
    $(function() {
        $("input[name='uiColor']").colorpicker({
            altField: "input[name='uiColor']",
            altProperties: 'background-color',
            parts:  [ 'map', 'bar'],
            part:	{
                map:		{ size: 128 },
                bar:		{ size: 128 }
            }
        });
    });
</script>
<div>
    <script type="text/javascript">
        canReloadMessages = false;
    </script>
    <div class="msg-box successfully" style="display: none">
        <img src="${resource(dir:'images', file:'icon20.gif')}" alt="${message(code:'success.icon.alt',default:'Success')}"/>
        <strong><g:message code="flash.success.title"/></strong>
        <p><g:message code="${session.message}" args="${session.args}"/></p>

        <g:set var="message" value="" scope="session"/>
    </div>
</div>
<div class="form-edit">
    <div class="heading">
        <strong><g:message code="configuration.title.company" />
        </strong>
    </div>
    <div class="form-hold">
        <g:form name="save-company-form" action="saveCompany" useToken="true">
            <!-- company details -->
            <fieldset>
                <div class="form-columns">
                    <%--Use two columns --%>
                    <div class="column pad-below">
                        <div class="row">
                            <g:applyLayout name="form/input">
                                <content tag="label">
                                    <g:message code="config.company.description"/><span id="mandatory-meta-field">*</span>
                                </content>
                                 <content tag="label.for">description</content>
                                <g:textField class="field" name="description" value="${company?.description}"/>
                            </g:applyLayout>
                            <g:applyLayout name="form/input">
                                <content tag="label">
                                    <g:message code="prompt.address1"/><span id="mandatory-meta-field">*</span>
                                </content>
                                <content tag="label.for">address1</content>
                                <g:textField class="field" name="address1" value="${contact?.address1}" />
                            </g:applyLayout>
                        
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.address2"/></content>
                                <content tag="label.for">address2</content>
                                <g:textField class="field" name="address2" value="${contact?.address2}" />
                            </g:applyLayout>
                        
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.city"/></content>
                                <content tag="label.for">city</content>
                                <g:textField class="field" name="city" value="${contact?.city}" />
                            </g:applyLayout>
                        
                            <g:applyLayout name="form/input">
                                <content tag="label">
                                    <g:message code="prompt.state"/><span id="mandatory-meta-field">*</span>
                                </content>
                                <content tag="label.for">stateProvince</content>
                                <g:textField class="field" name="stateProvince" value="${contact?.stateProvince}" />
                            </g:applyLayout>
                        
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.zip"/></content>
                                <content tag="label.for">postalCode</content>
                                <g:textField class="field" name="postalCode" value="${contact?.postalCode}" />
                            </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label">
                                <g:message code="config.company.free.call.limit"/>
                            </content>
                            <content tag="label.for">numberOfFreeCalls</content>
                                <g:textField class="field" name="numberOfFreeCalls" value="${company?.numberOfFreeCalls}"/>
                        </g:applyLayout>

                            <g:applyLayout name="form/select">
                                <content tag="label">
                                    <g:message code="prompt.country"/><span id="mandatory-meta-field">*</span>
                                </content>
                                <content tag="label.for">countryCode</content>
                        
                                <g:select name="countryCode"
                                          from="${CountryDTO.list()}"
                                          optionKey="code"
                                          optionValue="${{ it.getDescription(session['language_id']) }}"
                                          noSelection="['': message(code: 'default.no.selection')]"
                                          value="${contact?.countryCode}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="prompt.timezone"/></content>
                                <content tag="label.for">timezone</content>
                                <g:select name="timezone" from="${availableTimezones}"
                                          optionKey="key" optionValue="value" value="${company?.timezone}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.phone.number"/></content>
                                <content tag="label.for">contact.phoneCountryCode</content>
                                <span>
                                    <g:textField class="field" name="phoneCountryCode" value="${contact?.phoneCountryCode}" maxlength="3" size="2"/>
                                    -
                                    <g:textField class="field" name="phoneAreaCode" value="${contact?.phoneAreaCode}" maxlength="5" size="2"/>
                                    -
                                    <g:textField class="field" name="phoneNumber" value="${contact?.phoneNumber}" maxlength="10" size="7"/>
                                </span>
                            </g:applyLayout>

                            <g:applyLayout name="form/input">
                                <content tag="label">
                                    <g:message code="prompt.email"/><span id="mandatory-meta-field">*</span>
                                </content>
                                <content tag="label.for">contact.email</content>
                                <g:textField class="field" name="email" value="${contact?.email}" />
                            </g:applyLayout>

                            <g:applyLayout name="form/input">
                                <content tag="label">
                                    <g:message code="prompt.failed.email.notification"/><span id="mandatory-meta-field">*</span>
                                </content>
                                <content tag="label.for">company.failedEmailNotification</content>
                                <g:textField class="field" name="failedEmailNotification" value="${company?.failedEmailNotification}"/>
                            </g:applyLayout>

                        </div>
                        <!-- two columns do not work in configuration page 
                    </div>
                    <div class="column">
                    -->
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.company.currency"/></content>
                            <content tag="label.for">currencyId</content>
                            <g:select name="currencyId" 
                                      from="${CompanyDTO.get(company?.id)?.currencies.sort{it.description}}"
                                      optionKey="id"
                                      optionValue="${{it.getDescription(session['language_id'])}}"
                                      value="${company.currencyId}" />
                        </g:applyLayout>
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.company.language"/></content>
                            <content tag="label.for">languageId</content>
                            <g:select name="languageId" from="${LanguageDTO.list()}"
                                    optionKey="id" optionValue="description" value="${company.languageId}"  />
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="config.company.ui.color"/></content>
                            <content tag="label.for">uiColor</content>
                            <g:textField class="field" name="uiColor" value="${company.uiColor ? Integer.toHexString(company.uiColor) : ''}" />
                        </g:applyLayout>
                        <!-- customer meta fields -->
                        <g:render template="/metaFields/editMetaFields"
                              model="[availableFields: availableFields, fieldValues: company?.metaFields]"/>

                        <div class="row">&nbsp;</div>
                    </div>
                </div>
                <g:if test="${companyInformationTypes && companyInformationTypes.size()>0}">
                    <g:each in="${companyInformationTypes}" var="cit">
                        <div id="ait-${cit.id}" class="box-cards box-cards-open" >
                            <div class="box-cards-title">
                                <a class="btn-open"><span>
                                    ${cit.name}
                                </span></a>
                            </div>
                            <div class="box-card-hold">
                                <g:render template="/config/company/cITMetaFields" model="[cit : cit , values : company?.metaFields, citVal : cit.id]"/>
                            </div>
                        </div>
                    </g:each>
                    </br>
                </g:if>
            </fieldset>

            <div class="btn-box buttons">
                <ul>
                    <li>
                        <a onclick="replacePhoneCountryCodePlusSign($('input[name*=phoneCountryCode]'));
                        $('#save-company-form').submit();" class="submit save button-primary"><span><g:message
                                code="button.save"/></span></a>
                    </li>
                    <li>
                        <g:link controller="config" action="index" class="submit cancel"><span><g:message
                                code="button.cancel"/></span></g:link>
                    </li>
                    <sec:ifAnyGranted roles="CONFIGURATION_1909">
                        <li>
                            <a onclick="show(); " class="submit save"><span>
                                <g:message code="copy.company.label" default="Copy Company"/>
                            </span></a>
                        </li>
                    </sec:ifAnyGranted>
                    <sec:ifAnyGranted roles="CONFIGURATION_1910">
                        <li>
                            <g:link controller="signup" action="reseller" class="submit"><span><g:message
                                    code="create.reseller" default="Create Reseller"/></span></g:link>
                        </li>
                    </sec:ifAnyGranted>
                </ul>
            </div>
        </g:form>
        <g:render template="company/confirmDialog"
                  model="['message': 'copy.company.confirm',
                          'controller': 'signup',
                          'action': 'copyCompany',
                          'id': company.id,
                  ]"/>
    </div>
</div>
<r:script>
    $(document).ready(function() {
       if ($('.box-cards-open').length > 0) {
           $('div.form-hold').width('200%');
           $('div.heading').width('197.7%');
       }
    });

    function replacePhoneCountryCodePlusSign(phoneCountryCodeFields) {

        for (var i=0; i < phoneCountryCodeFields.length; i++) {
            phoneCountryCodeField = phoneCountryCodeFields[i];
            var phoneCountryCode = phoneCountryCodeField.value;

            if (phoneCountryCode != null && $.trim(phoneCountryCode) != '') {
                if (phoneCountryCode.indexOf('+') == 0) {
                    phoneCountryCode = phoneCountryCode.replace('+', '');
                }
                phoneCountryCodeField.value = phoneCountryCode;
            }
        }
    }
</r:script>
