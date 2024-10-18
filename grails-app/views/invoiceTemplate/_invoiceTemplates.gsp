<%@ page import="com.sapienter.jbilling.server.invoiceTemplate.report.InvoiceTemplateVersionBL" %>
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

<div class="table-box">
    <div class="table-scroll">
        <table id="invoiceTemplates" cellspacing="0" cellpadding="0">
            <thead>
            <tr>
                <th class="first" colspan="4">
                    <g:message code="invoiceTemplate.label.details"/>
                </th>
                <th class="last" colspan="1">
                    <g:message code="invoiceTemplate.qualifiedForInvoice.label.details"/>
                </th>
            </tr>
            </thead>

            <tbody>
            <g:each var="templ" in="${invoiceTemplates}">

                <tr id="invoiceTemplate-${templ.id}" class="${selectedInvoiceTemplates?.id == templ.id ? 'active' : ''}">
                    <td colspan="4">
                        <g:remoteLink class="cell double" action="listVersions" id="${templ.id}" before="register(this);"
                                      onSuccess="render(data, next);">
                            <strong>${templ.name.toString().encodeAsHTML()}</strong>
                            <em><g:message code="table.id.format" args="[templ.id as String]"/></em>
                        </g:remoteLink>
                    </td>
                    <td colspan="1">
                        <g:remoteLink class="cell double" action="listVersions" id="${templ.id}" before="register(this);"
                                      onSuccess="render(data, next);">
                            <strong>${InvoiceTemplateVersionBL.getVersionForInvoice(templ.invoiceTemplateVersions)?.versionNumber}</strong>
                        </g:remoteLink>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>

    <div class="btn-hold">
        <div class="btn-box">
            <div class="row">
                <sec:ifAllGranted roles="INVOICE_TEMPLATES_1802">
                    <g:remoteLink action='create' class="submit add button-primary" before="register(this);"
                                  onSuccess="render(data, next);">
                        <span><g:message code="button.create"/></span>
                    </g:remoteLink>

                    <a id="upload-new-template" class="submit load"><span><g:message code="button.upload"/></span></a>
                    <g:form name="create-template" method="POST" action="create">
                        <div id="invoice-template-error-messages" class="msg-box error" style="display: none; padding: 0;">
                            <ul style="margin-left: 0; list-style: none">
                                <li style="font-weight: normal;"></li>
                            </ul>
                        </div>

                        <div id="invoice-template-success-messages" class="msg-box successfully"
                             style="display: none; padding: 0;">
                            <ul style="margin-left: 0;">
                                <li style="font-weight: normal; list-style: none"></li>
                            </ul>
                        </div>

                        <div id="fields-container">
                            <g:message code="invoiceTemplate.label.name"/><br/>
                            <g:field type="text" maxlength="50" name="name"/><br/>
                            <g:message code="button.invoiceTemplate.downloadJson"/><br/>
                            <g:textArea name="json" cols="40" rows="10"/>

                            <g:hiddenField name="uploadJsonUrl"
                                           value="${createLink(controller: 'invoiceTemplate', action: 'uploadJson')}"/>
                        </div>

                        <g:hiddenField name="successUrl"
                                       value="${createLink(controller: 'invoiceTemplate', action: 'list')}"/>
                    </g:form>
                    <r:script>
                            $(function() {
                                var $createTemplateForm = $('#create-template').dialog({
                                    autoOpen: false,
                                    resizable: false,
                                    height: 380,
                                    width: 400,
                                    modal: true,
                                    buttons: {
                                        "Save": function () {
                                            var data = {};
                                            data.name = $createTemplateForm.find('#name').val();
                                            data.json = $createTemplateForm.find('#json').val();

                                            $.ajax({
                                                type: "POST",
                                                url: $createTemplateForm.find('#uploadJsonUrl').val(),
                                                context: $createTemplateForm,
                                                data: data,
                                                success: function(data, textStatus, jqXHR) {
                                                    // Hide the fields from the form.
                                                    
                                                    var jsonData = JSON.parse(data);
                                                    
                                                    if(jsonData.error){
                                                        $createTemplateForm.find('#invoice-template-error-messages li').html(jsonData.error );
                                                        $createTemplateForm.find('#invoice-template-error-messages').height("auto").show();
                                                        $(this).dialog("option", "height", 450);
                                                    } else {
                                                        $createTemplateForm.find('#invoice-template-error-messages').hide();
                                                        $createTemplateForm.find('#fields-container').hide();
                                                        // Show the success message.
                                                        $createTemplateForm.find('#invoice-template-success-messages li').html(jsonData.message);
                                                        $createTemplateForm.find('#invoice-template-success-messages').height("auto").show();
    
                                                        // Append the new template id to the redirect url.
                                                        $createTemplateForm.find('#successUrl').val($createTemplateForm.find('#successUrl').val() + "/" + jsonData.id)
                                                        // Change the buttons to only show the OK.
                                                        $(this).dialog( "option", "buttons", [
                                                            {
                                                                text: "Ok",
                                                                click: function() {
                                                                    window.location = $('#successUrl').val();
                                                                }
                                                            }
                                                        ]);
                                                        // Change the height.
                                                        $(this).dialog("option", "height", 195);
                                                    }
                                                }
                                            });
                                        },
                                        "Cancel": function () {
                                            // Clean up the fields in the dialog.
                                            $createTemplateForm.find('#name').val("");
                                            $createTemplateForm.find('#json').val("");
                                            $createTemplateForm.find('#invoice-template-error-messages').hide();
                                            // Close the dialog.
                                            $(this).dialog("option", "height", 380);
                                            $(this).dialog("close");
                                        }
                                    },
                                    create: function() {
                                        $('.ui-dialog-buttonpane').find('button:contains("Save")')
                                                                  .addClass('button-primary')
                                                                  .removeClass('ui-state-default');

                                        $('.ui-dialog-content').addClass('padding')
                                    },
                                    close: function(event, ui) {
                                        // Clean up the fields in the dialog.
                                        $createTemplateForm.find('#name').val("");
                                        $createTemplateForm.find('#json').val("");
                                        $createTemplateForm.find('#invoice-template-error-messages').hide();
                                        // Close the dialog.
                                        $(this).dialog("option", "height", 380);
                                        $(this).dialog("close");
                                    },
                                    title: '<g:message code="button.upload"/>'
                                });

                                $('#upload-new-template').on('click', function () {
                                    $createTemplateForm.dialog('open');
                                    $('.ui-dialog :button').blur();
                                });
                            });
                    </r:script>
                </sec:ifAllGranted>
            </div>
        </div>
    </div>
</div>

