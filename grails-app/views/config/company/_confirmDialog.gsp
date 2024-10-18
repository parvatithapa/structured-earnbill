%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<div id="confirm-dialog" class="bg-lightbox" title="<g:message code="popup.confirm.title"/>" style="display:none;">
    <div id="error-messages-copy-company" class="msg-box error" style="display: none;">
        <img src="${resource(dir:'images', file:'icon14.gif')}" alt="${message(code:'error.icon.alt',default:'Error')}"/>
        <strong><g:message code="flash.error.title"/></strong>
        <ul></ul>
    </div>

    <g:form name="confirm-command-form" url="[controller: 'signup', action: 'copyCompany', id: id, isCompanyChild:isCompanyChild, copyProducts:copyProducts, copyPlans:copyPlans]">
        <g:hiddenField name="id" value="${id}"/>
        <!-- confirm dialog content body -->
        <table>
            <tbody><tr>
                <td valign="top">
                    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="confirm">
                </td>
                <td class="col2">

                    <p id="confirm-dialog-msg">
                        <g:if test="${message}">
                            <g:message code="${message}" args="[id]"/>
                        </g:if>
                    </p>

                </td>
            </tr></tbody>
        </table>
        <table>
            <tbody>
            <tr>
                <td class="col1">
                    <g:applyLayout name="form/text">
                        <content tag="label">
                            <g:message code="copy.company.admin.email.label"/>
                            <span id="mandatory-meta-field">*</span>
                        </content>
                        <g:textField name="adminEmail" value=""/>
                    </g:applyLayout>

                    <div class="checkbox-group">
                        <p id="confirm-dialog-checkbox-products">
                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="copy.company.confirm.products" args="[id]"/></content>
                                <content tag="label.for">childCompanyFlag</content>
                                <g:checkBox class="cb checkbox" name="copyProducts"/>
                            </g:applyLayout>
                        </p>

                        <p id="confirm-dialog-checkbox-plans">
                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="copy.company.confirm.plans" args="[id]"/></content>
                                <content tag="label.for">childCompanyFlag</content>
                                <g:checkBox class="cb checkbox" name="copyPlans"/>
                            </g:applyLayout>
                        </p>

                        <p id="confirm-dialog-checkbox">
                            <g:applyLayout name="form/checkbox">
                                <content tag="label"><g:message code="copy.company.confirm.child" args="[id]"/></content>
                                <content tag="label.for">childCompanyFlag</content>
                                <g:checkBox class="cb checkbox" name="isCompanyChild"/>
                            </g:applyLayout>
                        </p>
                    </div>
                    <div id="childCompanyDiv">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="copy.company.child.template.label"/></content>
                            <g:textField name="childCompany" value=""/>
                        </g:applyLayout>
                    </div>
                </td>
            </tr></tbody>
        </table>
    </g:form>
</div>

<script type="text/javascript">
    $(function() {
        $("#childCompanyDiv").hide();
        setTimeout(function() {
            $('#confirm-dialog.ui-dialog-content').remove();
            $('#confirm-dialog').dialog({
                autoOpen: false,
                height: 370,
                width: 370,
                modal: true,
                closeOnEscape: false,
                open: function(event, ui) {
                    $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();//remove escape button
                    $(this).dialog('option', "beforeClose", function () {//remove close click
                        return false;
                    });
                },
                buttons: {
                    '<g:message code="prompt.yes"/>': function() {
                        $('.ui-dialog-buttonpane').find(':button').button("disable");
                        $.ajax({
                            type: 'POST',
                            url: '${createLink(controller: 'signup' ,action: 'copyCompany')}',
                            data: $("#confirm-command-form").serialize(),
                            success: function(data) {
                                var jsonData = JSON.parse(data);
                                if (jsonData.error) {
                                    if (!$('#error-messages-copy-company').is(':visible')) {
                                        $('#confirm-dialog').height($('#confirm-dialog').height() + 74);
                                    }

                                    $('#error-messages-copy-company ul').html(jsonData.error );
                                    $('#error-messages-copy-company').height("auto").show();
                                    $('.ui-dialog-buttonpane').find(':button').button("enable");
                                    $('#adminEmail,#copyProducts,#copyPlans,#isCompanyChild,#childCompany').prop("disabled", false);
                                } else {
                                    $('.msg-box.successfully p').html(jsonData.message);
                                    $('.msg-box.successfully').show();
                                    $('html, body').animate({ scrollTop: 0 }, 'fast');
                                    $('#confirm-dialog').dialog('option', "beforeClose", function () {
                                        return true;
                                    });
                                    $('#confirm-dialog').dialog('close');
                                }
                            }
                        });
                        $('#adminEmail,#copyProducts,#copyPlans,#isCompanyChild,#childCompany').prop("disabled", true);
                    },
                    '<g:message code="prompt.no"/>': function() {
                        $(this).dialog('option', "beforeClose", function () {
                            return true;
                        });
                        $(this).dialog('close');
                    }
                },
                close: function() {
                    $('.ui-dialog-buttonpane').find(':button').button("enable");
                    $('#error-messages-copy-company').hide();
                    $('.ui-dialog-buttonset').css('visibility', 'visible');
                    $('#adminEmail,#copyProducts,#copyPlans,#isCompanyChild,#childCompany').prop("disabled", false);
                }
            });
        }, 100);

        $('#isCompanyChild').change(function() {
            if($(this).is(":checked")) {
                $("#childCompanyDiv").show();
                $('#confirm-dialog').height($('#confirm-dialog').height() + 21);
            } else {
                $('#confirm-dialog').height($('#confirm-dialog').height() - 21);
                $("#childCompanyDiv").val("");
                $("#childCompanyDiv").hide();
            }
        });
    });


    function show() {
        $('#confirm-dialog').dialog('open');
        $('#adminEmail').val('');
        $('#copyProducts').prop("checked", false);
        $('#copyPlans').prop("checked", false);
        $('#isCompanyChild').prop("checked", false);
        $('#childCompany').val('');
        $("#childCompanyDiv").hide();
        $('.ui-dialog-buttonpane').find('button:contains("Yes")')
                                  .removeClass('ui-state-default')
                                  .addClass('button-primary');
    }
</script>
