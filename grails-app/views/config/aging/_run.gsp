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

<%--
  form for date selection and triggering Collections process

  @author Igor Poteryaev
  @since  17-Mar-2014
--%>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<div id="collectionsRun" class="form-run-collections">
    <fieldset>
        <div class="form-columns single">
            <div class="column">
                 <g:applyLayout name="form/date">
                     <content tag="label"><g:message code="config.collections.run.date"/></content>
                     <content tag="label.for">collectionsRunDate</content>
                     <g:textField  class = "field"
                                    name = "collectionsRunDate"
                                   value = "${formatDate(      date: TimezoneHelper.currentDateForTimezone(session['company_timezone'] as String),
                                                         formatName: 'datepicker.format')}"
                                  onblur = "validateDate(this)"/>
                 </g:applyLayout>
            </div>
        </div>
    </fieldset>
    <div class="btn-row">
        <a onclick="openDialog();" id="runCollections" class="submit confirm play">
            <span><g:message code="button.run.collections"/></span>
        </a>
    </div>
</div>
<script type="text/javascript">
    setTimeout(
        function() {
            $("#collectionsRunDate").datepicker("option", "minDate", "${TimezoneHelper.currentDateForTimezone(session['company_timezone'] as String)}");
        },
        $("#collectionsRunDate").is(":visible") ? 10 : 510
    );

    $(document).ready(function() {
        $(function(){
            $('#confirm-dialog').dialog({
                autoOpen: false,
                width: 480,
                modal: true,
                resizable: false,
                create: function (event) {
                    $(event.target).parent().css("position", "fixed");
                },
                open: function() {
                    $('.ui-dialog-buttonpane').find('button:contains("Cancel")').focus();
                },
                buttons: [
                    {
                        id: "runCollectionsConfirm",
                        text: "${message(code: 'button.run.collections')}",
                        click: function () {
                            $('#run-collections-form').submit();
                        }
                    },{
                        text: "${message(code: 'button.cancel')}",
                        click: function () {
                            $(this).dialog("close");
                        }
                    }
                ]
            });
        });
    });

    function openDialog() {
        if (validateDate("#collectionsRunDate")) {
            $('#confirm-dialog').dialog('open');
        }
    }
</script>

<div id="confirm-dialog" class="hide-element jb-dialog" title="${g.message(code:'popup.confirm.title')}">
    <p>
        <span class='ui-icon ui-icon-alert' style='float:left; margin:0 7px 0 0;'></span>
        <g:message code="config.collections.run.confirm"/>
    </p>
</div>
