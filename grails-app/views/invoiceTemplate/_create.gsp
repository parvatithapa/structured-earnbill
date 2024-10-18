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

<div class="column-hold">
    <div class="heading">
        <strong><g:message code="invoiceTemplate.add.title"/></strong>
    </div>

    <g:form name="invoice-template-form" controller="invoiceTemplate" action="save">
        <div class="box">
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="invoiceTemplate.label.name"/></content>
                            <content tag="label.for">name</content>

                            <g:textField class="field" name="name" value="${invoiceTemplate?.name}"
                                         placeholder="${message(code: 'invoiceTemplate.label.name.default')}"/>
                        </g:applyLayout>
                    </div>
                </fieldset>

                <g:hiddenField name="srcId" value="${srcId}"/>
            </div>

            <div class="btn-box buttons">
                <ul>
                    <li>
                        <a class="submit save button-primary" onclick="$('#invoice-template-form').submit();">
                            <span>
                                <g:message code="button.save"/>
                            </span>
                        </a>
                    </li>
                    <li>
                        <g:link action="list" class="submit cancel">
                            <span>
                                <g:message code="button.cancel"/>
                            </span>
                        </g:link>
                </ul>
            </div>
        </div>
    </g:form>
</div>
