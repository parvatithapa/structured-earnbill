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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.common.CommonConstants; com.sapienter.jbilling.server.item.CurrencyBL; com.sapienter.jbilling.server.util.Constants" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<g:set var="startDate" value="${startDate ?: TimezoneHelper.currentDateForTimezone(session['company_timezone'])}"/>

<script type="text/javascript">
    function addDate() {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'addDatePoint')}',
            data: $('#currList > * > form').serialize(),
            success: function(data) {
                $('#currList').replaceWith(data);
                $("#error-messages").hide();
            }
        });
    }

    function editDate(date) {
        $('#startDate').val(date);

        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'editDatePoint')}',
            data: $('#currList > * > form').serialize(),
            success: function(data) {
                $('#currList').replaceWith(data);
            }
        });
    }

    function removeDate() {
        if (!validateDate($("#startDate"))) {
            return false;
        }

        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'removeDatePoint')}',
            data: $('#currList > * > form').serialize(),
            success: function(data) {
                $('#currList').replaceWith(data);
            }
        });
    }
//show confirmation dialog  when use 'delete currency' action
    function prepareConfirmDlg(currencyId,  currencyCode, url, message){
		$("#confirm-dialog-deleteCurrency-0 form").attr("action", url);
		$("#confirm-dialog-deleteCurrency-0 form input[name='id']").attr('value', currencyId);
		$("#confirm-dialog-deleteCurrency-0 form input[name='code']").attr('value', currencyCode);
		$("#confirm-dialog-deleteCurrency-0-msg").html(message);
		showConfirm('deleteCurrency-0');
	}

    function validateDate(element) {
        var dateFormat= "<g:message code="date.format"/>";
        if(!isValidDate(element, dateFormat)) {
            $("#error-messages").css("display","block");
            $("#error-messages ul").css("display","block");
            $("#error-messages ul").html("<li><g:message code="invalid.date.format"/></li>");
            element.focus();
            return false;
        } else {
            return true;
        }
    }

    function submitForm(){
        if (!validateDate($("#startDate"))) {
            return false;
        }

        $('#save-currencies-form').submit();
    }

</script>

<div class="form-edit" id="currList">
    <div class="heading">
        <strong><g:message code="currency.config.title"/></strong>
    </div>

    <div class="form-hold">
        <g:form name="save-currencies-form" url="[action: 'saveCurrencies']" useToken="true">
            <fieldset>
                <div class="form-columns single">
                    <g:applyLayout name="form/select">
                        <content tag="label"><g:message code="currency.config.label.default"/></content>
                        <content tag="label.for">defaultCurrencyId</content>
                        <content tag="include.script">true</content>
                         <g:select name="defaultCurrencyId" from="${CompanyDTO.get(session['company_id'] as Integer)?.currencies.sort{it.getDescription(session['language_id'])}}"
                                  optionKey="id"
                                  optionValue="${{ it.getDescription(session['language_id']) }}"
                                  value="${entityCurrency}"/>

                    </g:applyLayout>
                </div>

                <div class="form-columns single">
                    <div class="column single">
                        <div id="timeline">
                            <ul>
                                <g:each var="dateEntry" in="${timePoints}">
                                    <g:if test="${startDate.equals(dateEntry)}">
                                        <li class="current">
                                            <g:set var="date" value="${formatDate(date: startDate)}"/>
                                            <a onclick="editDate('${date}')">${date}</a>
                                        </li>
                                    </g:if>
                                    <g:else>
                                        <li>
                                            <g:set var="date" value="${formatDate(date: dateEntry)}"/>
                                            <a onclick="editDate('${date}')">${date}</a>
                                        </li>
                                    </g:else>
                                </g:each>

                                <li class="new">
                                    <a onclick="addDate();"><g:message code="button.add.price.date"/></a>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="form-columns single">
                    <g:applyLayout name="form/date">
                        <content tag="label"><g:message code="currency.config.label.date"/></content>
                        <content tag="label.for">startDate</content>
                        <g:textField class = "field" name="startDate"
                                     value = "${formatDate(date: startDate, formatName: 'datepicker.format')}"/>
                    </g:applyLayout>
                </div>

                <div class="form-columns single">
                    <table cellpadding="0" cellspacing="0" class="innerTable" width="100%">
                        <thead class="innerHeader">
                        <tr>
                            <th class="first"></th>
                            <th class="left tiny2"><g:message code="currency.config.th.symbol"/></th>
                            <th class="left tiny2"><g:message code="currency.config.th.active"/></th>
                            <th class="left medium"><g:message code="currency.config.th.rate"/></th>
                            <th class="left medium last"><g:message code="currency.config.th.sysRate"/></th>
                        </tr>
                        </thead>
                        <tbody>

                        <g:each var="currency" in="${currencies.sort{ it.description }}">
                            <tr>
                                <td class="innerContent">
                                    ${currency.getDescription(session['language_id'])}
                                    <g:hiddenField name="currencies.${currency.id}.id" value="${currency.id}"/>
                                </td>
                                <td class="innerContent">
                                    ${StringEscapeUtils.unescapeHtml(currency?.symbol)}
                                    <g:hiddenField name="currencies.${currency.id}.symbol" value="${currency.symbol}"/>
                                    <g:hiddenField name="currencies.${currency.id}.code" value="${currency.code}"/>
                                    <g:hiddenField name="currencies.${currency.id}.countryCode" value="${currency.countryCode}"/>
                                </td>
                                <td class="innerContent">
                                    <g:checkBox class="cb checkbox" name="currencies.${currency.id}.inUse"
                                                checked="${currency.inUse}"/>
                                </td>
                                <td class="innerContent">
                                    <div class="inp-bg inp4">
                                        <g:textField name="currencies.${currency.id}.rate" class="field" value="${formatNumber(number: currency.rate, formatName: 'exchange.format')}"/>
                                    </div>
                                </td>
                                <td id="inner-content-td3" class="innerContent">
                                    <g:if test="${currency.id != 1}">
                                    %{-- editable rate --}%
                                        <div class="inp-bg inp4" style="width: 100px;">
                                            <g:textField name="currencies.${currency.id}.sysRate" class="field" value="${formatNumber(number: currency.sysRate, formatName: 'exchange.format')}"/>
                                            <a class="plus-icon" onclick="prepareConfirmDlg('${currency.id}','${currency.code}',
	                                            '${createLink(controller:'config', action:'deleteCurrency', id:currency.id)}',
	                                            '${message(code:'currency.delete.confirm', args:[currency.getDescription(session['language_id'])]) }'
	                                            );">
            									&nbsp;&#xe000;
        									</a>
		                                 </div>
                                        
                                    </g:if>
                                    <g:else>
                                    %{-- USD always has a rate of 1.00 --}%
                                        <strong>
                                            <g:formatNumber number="${currency.sysRate}" type="currency" currencySymbol="${currency.symbol}"/>
                                            <g:hiddenField name="currencies.${currency.id}.sysRate" value="${currency.sysRate}"/>
                                        </strong>
                                    </g:else>

                                </td>
                            </tr>
                        </g:each>

                        </tbody>
                    </table>
                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>

            </fieldset>
        </g:form>
        <div class="btn-box buttons">
            <ul>
                <li>
                    <a onclick="submitForm()"
                       class="submit save button-primary"><span><g:message
                            code="button.save"/></span></a>
                </li>
                <g:if test="${!CommonConstants.EPOCH_DATE.equals(startDate)}">
                    <li><a class="submit delete" onclick="removeDate()"><span><g:message code="button.delete"/></span>
                    </a></li>
                </g:if>
                <li>
                    <g:link controller="config" action="index" class="submit cancel"><span><g:message
                            code="button.cancel"/></span></g:link>
                </li>
            </ul>
        </div>
    </div>

    <div class="btn-box table-box-below">
        <g:remoteLink controller="config" action="editCurrency" class="submit add button-secondary"
                      before="register(this);"
                      onSuccess="render(data, next);">
            <span><g:message code="button.create"/></span>
        </g:remoteLink>
    </div>

    <g:render template="/confirm"
              model="[
                      'controller': 'config',
                      'action': 'deleteCurrency',
                      'id': 0,
                      'formParams': ['code': '']
                    ]"/>
    
</div>
