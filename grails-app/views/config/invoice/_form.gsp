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

<%@ page import="com.sapienter.jbilling.server.util.Constants" %>

<div class="form-edit">
    <div class="heading">
        <strong><g:message code="invoice.config.title"/></strong>
    </div>
    <div class="form-hold">
        <g:uploadForm name="save-invoice-form" url="[action: 'saveInvoice']" useToken="true">
            <g:hiddenField name="removeLogo" value="false"/>
            <fieldset>
                <div class="form-columns">
                    <div class="column-550 single-81">
                        <!-- invoice numbering -->
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="invoice.config.label.number"/></content>
                            <content tag="label.for">number</content>
                            <g:textField name="number" class="field" value="${number.value ?: number.preferenceType.defaultValue}"/>

                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="invoice.config.label.prefix"/></content>
                            <content tag="label.for">prefix</content>
                            <g:textField name="prefix" class="field" value="${prefix.value ?: prefix.preferenceType.defaultValue}"/>
                        </g:applyLayout>

                        <!-- spacer -->
                        <div>
                            <br/>&nbsp;
                        </div>

                        <!-- invoice logo upload -->
                        <g:applyLayout name="form/text">
                            <content tag="label">
                                <g:message code="invoice.config.label.logo"/>
                                <a id="form-edit-submit" onclick="defaultInvoiceLogo()" class="submit">
                                    <g:message code="entity.remove.logo"/>
                                </a>
                            </content>
                            <img    src = "${createLink(controller: 'config', action: 'invoiceDisplayLogo')}"
                                     id = "logoImg"
                                  class = "logoImg"
                                    alt = "logo"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label">&nbsp;</content>
                            <g:applyLayout name="form/fileupload">
                                <content tag="reset.default">removeLogo</content>
                                <content tag="input.name">logo</content>
                                <content tag="img.id">logoImg</content>
                                <content tag="img.src">
                                    "${createLink(controller: 'config', action: 'invoiceDisplayLogo')}"
                                </content>
                                <content tag="update.image">
                                    onchange="updateImage(this, 'logoImg');"
                                </content>
                            </g:applyLayout>
                        </g:applyLayout>

                        <br/>
                        <span><g:message code="configuration.invoice.display.logo.text"/> </span>

                        <!-- spacer -->
                        <div>
                            <br/>&nbsp;
                        </div>
                    </div>
                </div>
            </fieldset>
        </g:uploadForm>

        <div class="btn-box buttons">
            <ul>
                <li><a onclick="$('#save-invoice-form').submit();" class="submit save button-primary"><span><g:message code="button.save"/></span></a></li>
                <li><g:link controller="config" action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link></li>
            </ul>
        </div>
    </div>
</div>
<script>
    function defaultInvoiceLogo() {
        $('#logoImg').attr('src', "${createLink(controller: 'config', action: 'defaultInvoiceLogo')}")
        $('#file-name-logo').text("${message(code: 'file.upload.nofile')}");
        $('input[name="removeLogo"]').val(true);
        $('#file-remove-logo').hide();
        $('#logo').val('');

    }
</script>