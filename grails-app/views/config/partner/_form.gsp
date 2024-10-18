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

<%@ page import="com.sapienter.jbilling.server.process.db.PeriodUnitDTO; com.sapienter.jbilling.server.util.db.CountryDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO" %>
<%@ page import="com.sapienter.jbilling.server.user.contact.db.ContactMapDTO" %>


<div class="form-edit">
    <div class="heading">
        <strong><g:message code="configuration.menu.partner"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="saveCommissionConfig" action="saveCommissionConfig" useToken="true">
            <!-- company details -->
            <fieldset>
                <div class="form-columns" style="margin-bottom: 10px;">
                    <div class="column">
                        <div class="row">
                            <g:applyLayout name="form/date">
                                <content tag="label"><g:message code="billing.next.run.date"/></content>
                                <content tag="label.for">nextRunDate</content>
                                <g:textField class="field" name="nextRunDate"
                                             value="${formatDate(date: configuration?.nextRunDate, formatName: 'datepicker.format')}"
                                             onblur="validateDate(this)"/>
                            </g:applyLayout>
                        </div>

                        <div class="row">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="order.label.period"/></content>
                                <content tag="label.for">periodValue</content>
                                <g:field type="number" class="field" name="periodValue"
                                             value="${configuration?.periodValue}" maxlength="2" size="2"/>
                                <g:select style="float: right; position: relative; top: -20px;width:70px" class="field"
                                          name="periodUnitId" from="${PeriodUnitDTO.list()}"
                                          optionKey="id" optionValue="description"
                                          value="${configuration?.periodUnitId}"/>
                            </g:applyLayout>
                        </div>
                    </div>
                </div>
            </fieldset>

            <div class="btn-box buttons">
                <ul>
                    <li>
                        <a onclick="$('#saveCommissionConfig').submit();" class="submit save button-primary"><span><g:message
                                code="button.save"/></span></a>
                    </li>
                    <li>
                        <g:link controller="config" action="index" class="submit cancel"><span><g:message
                                code="button.cancel"/></span></g:link>
                    </li>
                    <li>
                        <g:link controller="config" action="triggerCommissionProcess" class="submit play"><span><g:message
                                code="button.run.commissionProcess"/></span></g:link>
                    </li>
                </ul>
            </div>
        </g:form>
    </div>
</div>
