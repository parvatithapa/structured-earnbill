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

<%@page import="com.sapienter.jbilling.common.Constants;" %>
<%@page import="com.sapienter.jbilling.common.Util;" %>
<%@page import="com.sapienter.jbilling.server.customer.CustomerBL;" %>
<%@page import="com.sapienter.jbilling.server.fileProcessing.FileConstants;" %>
<%@page import="com.sapienter.jbilling.server.invoice.db.InvoiceStatusDAS;" %>
<%@page import="com.sapienter.jbilling.server.metafields.EntityType;" %>
<%@page import="com.sapienter.jbilling.server.metafields.MetaFieldBL;" %>
<%@page import="com.sapienter.jbilling.server.metafields.db.MetaField;" %>
<%@page import="com.sapienter.jbilling.server.user.UserBL;" %>
<%@page import="com.sapienter.jbilling.server.user.db.CancellationRequestDTO;" %>
<%@page import="com.sapienter.jbilling.server.user.CancellationRequestBL;" %>
<%@page import="com.sapienter.jbilling.server.util.db.EnumerationDTO;" %>
<%@page import="com.sapienter.jbilling.server.util.Constants;" %>
<%@page import="com.sapienter.jbilling.server.timezone.TimezoneHelper;" %>
<%@page import="grails.plugin.springsecurity.SpringSecurityUtils;" %>
<%@page import="org.apache.commons.lang.StringEscapeUtils;" %>
<%@ page import="com.sapienter.jbilling.server.util.PreferenceBL;" %>
<%@ page import="com.sapienter.jbilling.common.CommonConstants;" %>
<%@ page import="java.time.LocalDateTime; java.time.format.DateTimeFormatter; java.time.ZoneId; java.time.Duration;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.ASSET_STATUS_RELEASED;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.ASSET_STATUS_IN_USE;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_RECHARGE;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_BUY_SUBSCRIPTION;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_SUSPEND_ACTIVATE;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_REISSUE;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_RELEASE;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_IDENTITY_DOCUMENT;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_REFUND_WALLET_BALANCE;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.ACTIVE_CONSUMPTION_USAGE_MAP;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.ROLE_OPERATION_USER;" %>
<%--
  Shows details of a selected user.

  @author Brian Cowdery
  @since  23-Nov-2010
--%>

<g:set var="customer" value="${selected.customer}"/>

<%
    def loggedInUser = new UserBL(session['user_id'] as Integer).getUserWS();
%>

<style>

×
	.ui-widget-content .ui-state-error{
		background-color:white;
		background: none;
	}
	.row .inp-bg{
		display: table-caption;
	}
	.long{
        display:inline-block;
        width:100px;
        white-space: nowrap;
        overflow:hidden !important;
        text-overflow: ellipsis;
	}
	.innerContent{
	    padding-bottom: 5px;
	}

	.tooltip {
      position: relative;
      display: inline-block;
      border-bottom: 1px dotted black;
    }

    .tooltip .tooltiptext {
      visibility: hidden;
      width: 180px;
      background-color: #ffff9b ;
      color: #343434;
      border: 1px solid #e06e00 ;
      text-align: center;
      border-radius: 6px;
      padding: 5px 0;
      top: 100%;
      left: 50%;
      margin-left: -70px;

      /* Position the tooltip */
      position: absolute;
      z-index: 1;
    }

    .tooltip:hover .tooltiptext {
      visibility: visible;
    }
</style>

<script>

$(function () {
    $(".datepicker").datepicker({
        dateFormat: "${message(code: 'datepicker.jquery.ui.format')}"
            });

});


function getCancellation(cancelId,action){
	$("#cancellation_userId_delete").val(${selected?.id});
	$("#cancellation_userId_edit").val(${selected?.id});
	$("#cancellationId_edit").val(cancelId);
	$("#cancellationId_delete").val(cancelId);
	$('#review-messages-edit').hide();
    $('.ui-dialog-content').addClass('padding');
    $('.date.inp-bg').addClass('inp-bg-date').removeClass('inp-bg');
    window.avoidFilterBlur = true;
	allFields.removeClass( "ui-state-error" );
	resetTips("${g.message(code:'customer.cancellation.fields.required')}");
	var cancellationDate_val = $("#cancellationDate_val").val();
	 $.ajax({
     	type: "GET",
         url: "${createLink(controller: 'customer', action: 'getCustomerCancellation')}",
         data: {cancellationId:cancelId},
         success: function(data){
             if(action=='edit'){
            	 var jsonResponse = JSON.parse(data);
            	 var date = "${formatDate(date: new Date(), formatName: 'datepicker.format')}";
                 $("#customer-edit-cancellation-request-dialog").removeClass("hide-element");
            	 $("#customer-edit-cancellation-request-dialog").dialog("open" );
            	 $("#reasonText").val(jsonResponse.reasonText);
            	 $("#cancellationDate_edit").val(cancellationDate_val);
             }
             if(action=='delete'){
            	 var jsonResponse = JSON.parse(data);
                 $("#customer-delete-cancellation-request-dialog").removeClass("hide-element");
                 $("#customer-delete-cancellation-request-dialog").dialog("open" );
                 $("#reasonText_delete").val(jsonResponse.reasonText);
                 $("#cancellationDate_delete").val(cancellationDate_val);
                 updateTips( "" );
             }
         },
         error: function(xhr){
         	alert('failure');
            alert(xhr.responseText); //<----when no data alert the err msg
         }
     });

}

    function openCancellationDialog(){
        $("#cancellation_userId").val(${selected?.id});
        $("#cancellation_customerId").val(${UserBL.getUserEntity(selected?.id?:"")?.getCustomer()?.getId()});
        $("#startDate").val('');
        $("#reason").val('');
        resetTips("${g.message(code:'customer.cancellation.fields.required')}");
        window.avoidFilterBlur = true;
        $('#review-messages').hide();
        $( ".errorMsg" ).text("");
        $( "#customer-add-cancellation-request-dialog" ).removeClass("hide-element");
        $( "#customer-add-cancellation-request-dialog" ).dialog("open");
        $('.ui-dialog-content').addClass('padding');
        $('.date.inp-bg').addClass('inp-bg-date').removeClass('inp-bg');
    }

	function openDialog(){
    	$("#noteTitle"+${selected.id}).val('');
    	$("#noteContent"+${selected.id}).val('');
		resetTips("${g.message(code:'customer.note.fields.required')}");
        $( "#customer-add-note-dialog-"+${selected.id}).removeClass("hide-element");
    	$( "#customer-add-note-dialog-"+${selected.id} ).dialog("open");
        $('.ui-dialog-content').addClass('padding');
        $('.date.inp-bg').addClass('inp-bg-date').removeClass('inp-bg');
        $('ui-state-error').removeClass("ui-state-error");
        window.avoidFilterBlur = true;
	}

    function initializeIcon(jquerySelector) {
        var options = $.datepicker.regional['en'];
        if (options == null) options = $.datepicker.regional[''];

        options.dateFormat = "${message(code: 'datepicker.jquery.ui.format')}";
        options.showOn = "both";
        options.buttonImage = "${resource(dir:'images', file:'icon04.gif')}";
        options.buttonImageOnly = true;
        options.onClose = null;

        $(jquerySelector).datepicker(options);
    }

    $(document).ready(function() {
        var noteTitle = $("#noteTitle" +${selected.id}),
                userId = $("#userId"),
                noteContent = $("#noteContent" +${selected.id});
        allFields = $([]).add(noteTitle).add(noteContent),
                tips = $(".validateTips");

        var startdate = $("#startDate"),
                userId = $("#userId"),
                reason = $("#reason");
        reasonText = $("#reasonText");
        $('#review-messages').hide();
        $('#review-messages-edit').hide();
        errormsgs = $(".errorMsg");

        if (${cancellationRequests?.length <= 0}) {
            $('#cancellation_box').hide();
        } else {
            $('#cancellation_box').show();
        }

        $("#customer-add-note-dialog-" +${selected.id}).dialog({
            autoOpen: false,
            height: 380,
            width: 550,
            // Workaround for modal dialog dragging jumps
            create: function (event) {
                $(event.target).parent().css("position", "fixed");
            },
            modal: true, buttons: {
                "${g.message(code:'button.add.note')}": function () {
                    var bValid = true;
                    noteTitle.parent().removeClass("ui-state-error");
                    bValid = bValid && checkLength(noteTitle, "${message(code: 'customer.detail.note.form.title')}", 1, 50, true);
                    bValid = bValid && checkLength(noteContent, "${message(code: 'customer.detail.note.form.content')}", 1, 1000, false);
                    if (bValid) {
                        jQuery.ajax({
                            type: "POST",
                            url: "${createLink(controller: 'customer', action: 'saveCustomerNotes')}",
                            data: $("#notes-form" +${selected.id}).serialize(),
                            success: function (data) {
                                if (jQuery("#user-" +${selected.id}).find("#" +${selected.id}).length) {
                                    jQuery("#user-" +${selected.id}).find("#" +${selected.id}).first().trigger('click');
                                } else {
                                    location.reload();
                                }
                            },
                            async: false
                        });
                        $(this).dialog("destroy");
                        $(this).addClass("hide-element");
                    }
                },
                "${message(code: 'customer.detail.note.form.cancel.button')}": function () {
                    $(this).dialog("close");
                }
            },
            close: function () {
                window.avoidFilterBlur = false;
                allFields.val("").removeClass("ui-state-error");
                $(this).addClass("hide-element");
                $(this).dialog("close");
            }
        })

        $("#customer-add-note-dialog-" +${selected.id} + " input").keypress(function (e) {
            if (e.keyCode == $.ui.keyCode.ENTER) {
                e.preventDefault();
                $("#customer-add-note-dialog-" +${selected.id}).parent().find('div.ui-dialog-buttonset button').first().click();
            }
        });

        $("#customer-add-cancellation-request-dialog").dialog({
            autoOpen: false,
            height: 450,
            width: 700,
            resizable: false,
            // Workaround for modal dialog dragging jumps
            create: function (event) {
                $(event.target).parent().css("position", "fixed");
            },
            modal: true, buttons: {
                "${g.message(code:'button.add.cancellation.request')}": function () {
                    var bValid = true;
                    allFields.removeClass("ui-state-error");
                    $('#review-messages').hide();
                    cancellationDate = $("#cancellation\\.cancellationDate");
                    bValid = bValid && checkDate(cancellationDate, 1, 11);
                    bValid = bValid && checkLength(reason, "${message(code: 'customer.cancellationRequest.reasonText')}", 1, 1000, false);
                    if (bValid) {
                        resetTips("${g.message(code:'customer.cancellation.fields.required')}");
                        jQuery.ajax({
                            type: "POST",
                            url: "${createLink(controller: 'customer', action: 'saveCustomerCancellation')}",
                            data: $("#cancellation-form").serialize(),
                            success: function (response, responseText) {
                                var jsonResponse = JSON.parse(response);
                                if (jsonResponse.status == "success") {
                                    $("#customer-add-cancellation-request-dialog").dialog("close");
                                    var cancelUserId = $("#cancellation_userId").val();
                                    if ($("#user-" + cancelUserId).length == 0) {
                                        $('#' + cancelUserId).first().trigger('click');
                                    } else {
                                        jQuery("#user-" + cancelUserId).find("#" + cancelUserId).first().trigger('click');
                                    }
                                } else {
                                    updateMessage(jsonResponse.errorMessage);
                                }
                            }
                        });
                    }
                },
                "${message(code: 'customer.detail.cancellation.request.form.cancel.button')}": function () {
                    $(this).dialog("close");
                }
            },
            close: function () {
                $(this).addClass("hide-element");
                window.avoidFilterBlur = false;
                allFields.val("").removeClass("ui-state-error");
            }
        });

        $("#customer-edit-cancellation-request-dialog").dialog({
            autoOpen: false,
            height: 450,
            width: 700,
            resizable: false,
            // Workaround for modal dialog dragging jumps
            create: function (event) {
                $(event.target).parent().css("position", "fixed");
            },
            modal: true,
            buttons: {
                "${g.message(code:'button.update.cancellation.request')}": function () {
                    var bValid = true;
                    allFields.removeClass("ui-state-error");
                    $('#review-messages-edit').hide();
                    reasonText = $("#reasonText");
                    cancellationDate_edit = $("#cancellationDate_edit");
                    bValid = bValid && checkDate(cancellationDate_edit, 1, 11);
                    bValid = bValid && checkLength(reasonText, "${message(code: 'customer.cancellationRequest.reasonText')}", 1, 1000, false);
                    if (bValid) {
                        resetTips("${g.message(code:'customer.cancellation.fields.required')}");
                        jQuery.ajax({
                            type: "POST",
                            url: "${createLink(controller: 'customer', action: 'editCustomerCancellation')}",
                            data: $("#cancellationEdit-form").serialize(),
                            success: function (response) {
                                var jsonResponse = JSON.parse(response);
                                if (jsonResponse.status == "success") {
                                    $("#customer-edit-cancellation-request-dialog").dialog("close");
                                    var cancelUserId = $("#cancellation_userId_edit").val();
                                    if ($("#user-" + cancelUserId).length == 0) {
                                        $('#' + cancelUserId).first().trigger('click');
                                    } else {
                                        jQuery("#user-" + cancelUserId).find("#" + cancelUserId).first().trigger('click');
                                    }
                                } else {
                                    updateMessage(jsonResponse.errorMessage);
                                }
                            }
                        });
                    }
                },
                "${message(code: 'customer.detail.cancellation.request.form.cancel.button')}": function () {
                    $(this).dialog("close");
                }
            },
            close: function () {
                $(this).addClass("hide-element");
                window.avoidFilterBlur = false;
                allFields.val("").removeClass("ui-state-error");
            }
        });

        $("#customer-delete-cancellation-request-dialog").dialog({
            autoOpen: false,
            height: 450,
            width: 700,
            resizable: false,
            // Workaround for modal dialog dragging jumps
            create: function (event) {
                $(event.target).parent().css("position", "fixed");
            },
            modal: true, buttons: {
                "${g.message(code:'button.delete.cancellation.request')}": function () {
                    var bValid = true;
                    var cancelId = $("#cancellationId_delete").val();
                    allFields.removeClass("ui-state-error");
                    jQuery.ajax({
                        type: "POST",
                        url: "${createLink(controller: 'customer', action: 'deleteCustomerCancellation')}",
                        data: $("#cancellationDelete-form").serialize(),
                        success: function (response) {
                            var jsonResponse = JSON.parse(response);
                            if (jsonResponse.status == "success") {
                                $("#customer-delete-cancellation-request-dialog").dialog("close");
                                var cancelUserId = $("#cancellation_userId_delete").val();
                                if ($("#user-" + cancelUserId).length == 0) {
                                    $('#' + cancelUserId).first().trigger('click');
                                } else {
                                    jQuery("#user-" + cancelUserId).find("#" + cancelUserId).first().trigger('click');
                                }
                            } else {
                                updateMessage(jsonResponse.errorMessage);
                            }
                        }
                    });
                },
                "${message(code: 'customer.detail.cancellation.request.form.cancel.button')}": function () {
                    $(this).dialog("close");
                }
            },
            close: function () {
                $(this).addClass("hide-element")
                window.avoidFilterBlur = false;
                ;
                allFields.val("").removeClass("ui-state-error");
            }
        });

        initializeIcon("#cancellation\\.cancellationDate");
        initializeIcon("#cancellationDate_edit");
    });

    function checkLength(o, n, min, max, parent) {
        if ( o.val().length > max || o.val().length < min ) {
            if (parent) {
                o.parent().addClass( "ui-state-error" );
            } else {
                o.addClass( "ui-state-error" );
            }
            updateTips( "Length of " + n + " must be between " +min + " and " + max + "." );
            return false;
        } else {
            o.removeClass( "ui-state-error" );
            return true;
        }
    }

    function checkDate( o, min, max ) {
        if ( o.val().length > max || o.val().length < min ) {
            o.addClass( "ui-state-error" );
            updateTips( "Date field should not be blank." );
            return false;
        } else {
            o.removeClass( "ui-state-error" );
            return true;
        }
    }

    function resetTips(t){
        tips.text(t).removeClass( "ui-state-error" );
    }

    function updateTips( t ) {
        tips.text( t ).addClass( "ui-state-error" );
    }

    function updateMessage( t ){
        $('#review-messages').show();
        $('#review-messages-edit').show();
        errormsgs.text(t).addClass("error");
        }

</script>

<div class="column-hold">
<div id="customer-add-note-dialog-${selected?.id?:""}" class="hide-element jb-dialog" title="${g.message(code:'button.add.note')}">
    <div class="row"> <p id="validateTips" class="validateTips" style=" border: 1px solid transparent; "></p></div>
    <g:form id="notes-form${selected?.id?:""}" name="notes-form" url="[action: 'saveCustomerNotes']" useToken="true">
        <g:render template="/customer/customerNotesForm" />
        <g:hiddenField name="notes.customerId" value="${UserBL.getUserEntity(selected?.id?:"")?.getCustomer()?.getId()}" />
    </g:form>
</div>
</div>

<div class="column-hold">
<div id="customer-add-cancellation-request-dialog" class="hide-element" title="${g.message(code:'button.add.cancellation.request')}">
	<div id="review-messages" class="msg-box error" style="height:20% !important">
	<img src="/jbilling/static/images/icon14.gif" alt="Error"/>
    <strong><g:message code="flash.error.title"/></strong>
		<p id="errorMsg" class="errorMsg" style="height:0% !important "></p>
	</div>
    <p id="validateTips" class="validateTips" style=" border: 1px solid transparent; padding: 0.3em; "></p>
    <g:form id="cancellation-form" name="cancellation-form" url="[action: 'saveCustomerCancellation']">
        <g:render template="../customer/cancellationRequest" />
        <g:hiddenField name="cancellation.customerId" value="${UserBL.getUserEntity(selected?.id?:"")?.getCustomer()?.getId()}" />
        <g:hiddenField name="cancellation.userId" id="cancellation.userId" value="${selected?.id?:""}"/>
        <g:hiddenField name="cancellation_userId" id="cancellation_userId" value=""/>
        <g:hiddenField name="cancellation_customerId" id="cancellation_customerId" value=""/>
    </g:form>
</div>
</div>
<div class="column-hold">
<div id="customer-edit-cancellation-request-dialog" class="hide-element" title="${g.message(code:'button.update.cancellation.request')}">
	<div id="review-messages-edit" class="msg-box error" style="height:20% !important">
	<img src="/jbilling/static/images/icon14.gif" alt="Error"/>
    <strong><g:message code="flash.error.title"/></strong>
		<p id="errorMsg" class="errorMsg" style="height:0% !important "></p>
	</div>
    <p id="validateTips" class="validateTips" style=" border: 1px solid transparent; padding: 0.3em; "></p>
    <g:form id="cancellationEdit-form" name="cancellationEdit-form">
        <g:hiddenField name="cancellation.customerId" value="${UserBL.getUserEntity(selected?.id?:"")?.getCustomer()?.getId()}" />
        <g:hiddenField name="cancellationId_edit" id="cancellationId_edit" />
        <g:hiddenField name="cancellation_userId_edit" id="cancellation_userId_edit" value=""/>
        <g:render template="../customer/editCancellationRequest" />
    </g:form>
</div>
</div>
<div class="column-hold">
<div id="customer-delete-cancellation-request-dialog" class="hide-element" title="${g.message(code:'button.delete.cancellation.request')}">
    <p id="validateTips" class="validateTips" style=" border: 1px solid transparent; padding: 0.3em; "></p>
    <g:form id="cancellationDelete-form" name="cancellationDelete-form" >
    	<g:hiddenField name="cancellation_userId_delete" id="cancellation_userId_delete" value=""/>
    	<g:hiddenField name="cancellation.customerId" value="${UserBL.getUserEntity(selected?.id?:"")?.getCustomer()?.getId()}" />
    	<g:hiddenField name="cancellationId_delete" id="cancellationId_delete" />
        <g:render template="../customer/deleteCancellationRequest" />
    </g:form>
</div>
</div>
<g:if test="${!session['company_id']==60}">
    <div class="heading">
        <strong>
            <g:if test="${contact?.firstName || contact?.lastName}">
                ${contact.firstName} ${contact.lastName}
            </g:if>
            <g:else>
                ${displayer?.getDisplayName(selected)}
            </g:else>
            <em><g:if test="${contact}">${contact.organizationName}</g:if></em>
            <g:if test="${selected.deleted}">
                <span style="color: #ff0000;"><g:message code="user.status.deleted"/></span>
            </g:if>
        </strong>
    </div>
    <div class="box">
        <div class="sub-box">
            <g:if test="${customerNotes}">
                <div class="table-box">
                    <table id="users" cellspacing="0" cellpadding="0">
                        <thead>
                        <tr class="ui-widget-header first" >
                        <th width="50px"><g:message code="customer.detail.note.form.author"/></th>
                        <th width="60px"><g:message code="customer.detail.note.form.createdDate"/></th>
                        <th class="last" width="150px"><g:message code="customer.detail.note.form.title"/></th>
                        </thead>
                        <tbody>
                        <g:hiddenField name="newNotesTotal" id="newNotesTotal" />
                        <g:if test="${customerNotes}">
                            <g:each in="${customerNotes}">
                                <tr>
                                    <td>${it?.user.userName}</td>
                                    <td><g:formatDate date="${it?.creationTime}" formatName="date.time.format" timeZone="${session['company_timezone']}"/>  </td>
                                    <td>${it?.noteTitle}</td>
                                </tr>
                            </g:each>
                        </g:if>
                        <g:else>
                            <p><em><g:message code="customer.detail.note.empty.message"/></em></p>
                        </g:else>
                        </tbody>
                    </table>
                </div>
            </g:if>
            <g:else>
                <p><em><g:message code="customer.detail.note.empty.message"/></em></p>
            </g:else>
            <div id="custom-div7">
                <sec:access url="/customer/saveCustomerNotes">
                    <g:if test="${!selected.deleted}">
                        <a onclick="openDialog()"><span><g:message code="button.add.note"/></span></a>
                    </g:if>
                </sec:access>
                <sec:access url="/customerInspector/inspect">
                    <g:link controller = "customerInspector"
                                action = "inspect"
                                    id = "${selected.id}"
                                 title = "${message(code: 'customer.inspect.link')}">
                        <g:message code="button.show.all"/>
                    </g:link>
                </sec:access>
            </div>
        </div>
    </div>
</g:if>

    <!-- user details -->
    <div class="heading">
        <strong><g:message code="customer.detail.user.title"/></strong>
    </div>
    <div class="box">
        <div class="sub-box">
            <table class="dataTable table-layout-fixed" cellspacing="0" cellpadding="0">
                    <tbody>
                        <tr>
                            <td style="width: 80px;"><g:message code="customer.account.number"/></td>
                            <td class="value wide-width">
                                <sec:access url="/customerInspector/inspect">
                                    <g:link controller = "customerInspector"
                                                action = "inspect"
                                                    id = "${selected.id}"
                                                 title = "${message(code: 'customer.inspect.link')}">
                                            ${selected.id}
                                        <span style="font-family: AppDirectIcons;">&#xe03e;</span>
                                    </g:link>

                                </sec:access>
                                <sec:noAccess url="/customerInspector/inspect">
                                    ${selected.id}
                                </sec:noAccess>
                            </td>

                        </tr>
                        <tr>
                            <td><g:message code="label.adennet.iccid"/></td>
                            <td class="value wide-width" data-cy="iccidNumberShowPage">
                                <g:if test="${!SpringSecurityUtils.isSwitched() && selected.id != session['user_id']}">
                                        ${displayer?.getDisplayName(selected)}
                                </g:if>
                                <g:else>
                                    ${displayer?.getDisplayName(selected)}
                                </g:else>
                            </td>
                        </tr>
                        <g:if test="${customer.partners}">
                            <tr>
                                <td><g:message code="customer.related.agent"/></td>
                                <td class="value wide-width">
                                    <g:each var="partner" in="${customer.partners}" status="partnerIdx">
                                        <g:if test="${partnerIdx > 0}">,</g:if>
                                        <jB:secRemoteLink controller="partner" action="show" id="${partner.id}" before="register(this);" onSuccess="render(data, next);">
                                            ${StringEscapeUtils.escapeHtml(partner.baseUser.contact.firstName ? (partner.baseUser.contact.firstName + ' ' + partner.baseUser.contact.lastName) : partner.baseUser.userName)}
                                        </jB:secRemoteLink>
                                    </g:each>
                                </td>
                            </tr>
                        </g:if>
                        <g:isRoot>
                        	<tr>
                        	    <td><g:message code="customer.detail.user.company"/></td>
                            	<td class="value wide-width" data-cy="customerCompanyShowPage">${selected?.company?.description}</td>
                        	</tr>
                        </g:isRoot>
                        <tr>
                            <td><g:message code="customer.detail.user.status" /></td>
							<g:if test='${!selected.deleted}'>
								<td class="value wide-width" data-cy="customerStatusShowPage">
                                    ${selected.userStatus.getDescription(session['language_id'])}
                                </td>
							</g:if>
							<g:else>
								<td class="value wide-width" data-cy="customerStatusShowPage"><g:message code="user.userstatus.deleted" />
								</td>
							</g:else>
						</tr>
                        <tr>
                            <td><g:message code="user.locked"/></td>
                            <td class="value wide-width">
                                <g:formatBoolean boolean="${selected.isAccountLocked()}" true="Yes" false="No"/>
                            </td>
                        </tr>
                    <tr>
                        <td><g:message code="user.inactive"/></td>
                        <td class="value wide-width">
                            <g:formatBoolean boolean="${selected.isAccountExpired()}" true="Yes" false="No"/>
                        </td>
                    </tr>
                        <tr>
                            <td><g:message code="customer.detail.user.created.date"/></td>
                            <td class="value wide-width" data-cy="createdDate">
                                <g:formatDate       date = "${selected.createDatetime}"
                                              formatName = "date.time.format"
                                                timeZone = "${session['company_timezone']}"/>
                            </td>
                        </tr>
                        <tr>
                            <td><g:message code="customer.detail.userCodes"/></td>
                            <g:if test="${userCodes}">
                                <td class="value wide-width">
                                    <g:set var="ucLength" value="${userCodes.size()-1}" />
                                    <g:each in="${userCodes}" var="uc" status="ucIdx">
                                        <g:remoteLink controller = "user"
                                                          action = "show"
                                                          params = "[userCode: uc]"
                                                          before = "register(this);"
                                                       onSuccess = "render(data, next);"
                                                          method = "GET">
                                            ${uc}
                                        </g:remoteLink>
                                        <g:if test="${ucIdx < ucLength}">,</g:if>
                                    </g:each>
                                </td>
                            </g:if>
                        </tr>

                        <g:if test="${metaFields}">
                            <g:set var="aitMetaField" value="[]"/>
                            <g:each in="${selected.customer.accountType.informationTypes?.sort{ it.displayOrder }}" var="metaFieldGroup">
                                <g:each in="${metaFieldGroup?.metaFields?.sort{ it.displayOrder }}" var="metaField">
                                   <% aitMetaField.add(metaFields.find{it.field.id==metaField.id}) %>
                                </g:each>
                            </g:each>
                            %{--Displaying cusotmer metafield--}%
                            <g:render template="/metaFields/metaFields" model="[metaFields: metaFields-aitMetaField]"/>

                        <sec:ifAllGranted roles="${PERMISSION_VIEW_IDENTITY_DOCUMENT}">
                            %{--Displaying Customer Image--}%
                            <g:if test="${PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], CommonConstants.PREFERENCE_CAPTURE_IDENTIFICATION_DOC_FOR_CUSTOMER)}">
                                <g:render template="showCustomerImage" model="[userId:selected.id, identificationType:selected.getCustomer().getIdentificationType()]"/>
                            </g:if>
                        </sec:ifAllGranted>


                        %{--Display Renewal date & Termination Date on the customer screen even if they don't have any values or they are empty.--}%
                        %{--TODO:should be add the nges specific permission--}%
                            <g:set var="emptyMetaFields" value="[]"/>
                            <% List<String> emptyMetaFieldsToAdd = [FileConstants.CUSTOMER_COMPLETION_DATE_METAFIELD, FileConstants.CUSTOMER_TERMINATION_DATE_METAFIELD, FileConstants.CUSTOMER_NEXT_READ_DT_META_FIELD]
                            emptyMetaFieldsToAdd.each {
                                it ->
                                    MetaField metaField = MetaFieldBL.getFieldByName(selected?.getCompany().getId(), [EntityType.CUSTOMER] as EntityType[], it)
                                    if (metaField) emptyMetaFields.add(metaField)
                            }
                            emptyMetaFields = MetaFieldBL.convertMetaFieldsToWS(emptyMetaFields, selected?.getCustomer()).findAll { it -> it?.getValue() == null }
                            %>
                            <g:render template="/metaFields/metaFieldsWS" model="[metaFields: emptyMetaFields]"/>

                            %{--Displaying AIT metaField--}%
                            <g:each in="${selected.customer.accountType.informationTypes?.sort{ it.displayOrder }}" var="metaFieldGroup">
                                <tr><td colspan="2"><br/></td></tr>
                                <tr ><td colspan="2"><b>${metaFieldGroup.name}</b></td></tr>
                                <g:set var="accountMetafields" value="[]"/>
                                <g:each in="${metaFieldGroup?.metaFields?.sort{ it.displayOrder }}" var="metaField">
                                    <g:set var="fieldValue" value="${metaFields?.find{ (it.field.name == metaField.name) &&
                                            ((it.field.metaFieldGroups && metaFieldGroup.id && !it.field.metaFieldGroups.isEmpty() &&
                                              it.field.metaFieldGroups.first().id == metaFieldGroup.id) || (!it.field.metaFieldGroups.isEmpty() &&
                                              !it.field.metaFieldGroups.first().id && !metaFieldGroup.id)) }}"/>
                                    <g:if test="${fieldValue == null && metaField.getDefaultValue()}">
                                        <g:set var="fieldValue" value="${metaField.getDefaultValue()}"/>
                                    </g:if>
                                    <g:if test="${fieldValue}">
                                        <% accountMetafields.add(fieldValue) %>
                                    </g:if>
                                </g:each>
                                <g:render template="/metaFields/metaFields" model="[metaFields:accountMetafields]"/>
                            </g:each>
                        </g:if>

                        <g:if test="${customer?.parent}">
                            <!-- empty spacer row -->
                            <tr>
                                <td colspan="2"><br/></td>
                            </tr>
                            <tr>
                                <td><g:message code="prompt.parent.id"/></td>
                                <td class="value wide-width">
                                    <g:remoteLink action="show" id="${customer.parent.baseUser.id}" before="register(this);" onSuccess="render(data, next);">
                                        ${customer.parent.baseUser.id} - ${StringEscapeUtils.escapeHtml(customer?.parent?.baseUser?.userName)}
                                    </g:remoteLink>
                                </td>
                            </tr>
                            <tr>
                                <td><g:message code="customer.invoice.if.child.label"/></td>
                                <td class="value wide-width">
                                    <g:if test="${customer.invoiceChild > 0}">
                                        <g:message code="customer.invoice.if.child.true"/>
                                    </g:if>
                                    <g:else>
                                        <g:set var="parent" value="${new CustomerBL(customer.id).getInvoicableParent()}"/>
                                        <g:remoteLink action="show" id="${parent.baseUser.id}" before="register(this);" onSuccess="render(data, next);">
                                            <g:message code="customer.invoice.if.child.false" args="[ parent.baseUser.id ]"/>
                                        </g:remoteLink>
                                    </g:else>
                                </td>
                            </tr>
                            <tr>
                                <td><g:message code="prompt.use.parent.pricing"/></td>
                                <td class="value wide-width">
                                    <g:formatBoolean boolean="${customer.useParentPricing}"/>
                                </td>
                            </tr>
                        </g:if>

                        <g:if test="${customer?.children}">
                            <!-- empty spacer row -->
                            <tr>
                                <td colspan="2"><br/></td>
                            </tr>

                            <!-- direct sub-accounts -->
                            <g:each var="account" in="${customer.children.findAll{ it.baseUser.deleted == 0 }}">
                                <tr>
                                    <td><g:message code="customer.subaccount.title" args="[ account.baseUser.id ]"/></td>
                                    <td class="value wide-width">
                                        <g:remoteLink action="show" id="${account.baseUser.id}" before="register(this);" onSuccess="render(data, next);">
                                            ${StringEscapeUtils.escapeHtml(account.baseUser.userName)}
                                        </g:remoteLink>
                                    </td>
                                </tr>
                            </g:each>
                        </g:if>
                         <g:if test="${customerEnrollment}">
                        <tr>
                            <td><g:message code="customer.show.enrollment.id"/></td>
                            <td class="value wide-width">
                                <g:remoteLink controller="customerEnrollment"
                                                  action="show"
                                                      id="${customerEnrollment.id}"
                                                  before="register(this);"
                                               onSuccess="render(data, next);">
                                    Customer Enrollment-${customerEnrollment.id}
                                </g:remoteLink>
                            </td>
                        </tr>
                    </g:if>
                    </tbody>
                </table>
            </div>
    </div>

    <!-- user payment details -->
    <div class="heading">
        <strong><g:message code="customer.detail.payment.title"/></strong>
    </div>
    <div class="box">

        <div class="sub-box"><!-- show most recent order, invoice and payment -->
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="customer.detail.payment.order.date"/></td>
                    <td class="value">
                        <sec:access url="/order/show">
                            <g:remoteLink controller="order" action="show" id="${latestOrder?.id}" before="register(this);" onSuccess="render(data, next);">
                                <g:formatDate date="${latestOrder?.createDate}" formatName="date.pretty.format"/>
                            </g:remoteLink>
                        </sec:access>
                        <sec:noAccess url="/order/show">
                            <g:formatDate date="${latestOrder?.createDate}" formatName="date.pretty.format"/>
                        </sec:noAccess>
                    </td>
                    <td class="value">
                        <sec:access url="/order/list">
                            <g:link controller="order" action="user" id="${selected.id}">
                                <g:message code="customer.show.all.orders"/>
                            </g:link>
                        </sec:access>
                    </td>
                </tr>
                    <tr>
                        <td><g:message code="customer.detail.payment.invoiced.date"/></td>
                        <td class="value">
                            <sec:access url="/invoice/show">
                                <g:remoteLink controller="invoice" action="show" id="${latestInvoice?.id}" before="register(this);" onSuccess="render(data, next);">
                                    <g:formatDate date="${latestInvoice?.createDatetime}" formatName="date.pretty.format"/>
                                </g:remoteLink>
                            </sec:access>
                            <sec:noAccess url="/invoice/show">
                                <g:formatDate date="${latestInvoice?.createDatetime}" formatName="date.pretty.format"/>
                            </sec:noAccess>
                        </td>
                        <td class="value">
                            <sec:access url="/invoice/list">
                                <g:link controller="invoice" action="user" id="${selected.id}">
                                    <g:message code="customer.show.all.invoices"/>
                                </g:link>
                            </sec:access>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.payment.paid.date"/></td>
                        <td class="value">
                            <sec:access url="/payment/show">
                                <g:remoteLink controller="payment" action="show" id="${latestPayment?.id}" before="register(this);" onSuccess="render(data, next);">
                                    <g:formatDate date="${latestPayment?.paymentDate ?: latestPayment?.createDatetime}" formatName="date.pretty.format"/>
                                </g:remoteLink>
                            </sec:access>
                            <sec:noAccess url="/payment/show">
                                <g:formatDate date="${latestPayment?.paymentDate ?: latestPayment?.createDatetime}" formatName="date.pretty.format"/>
                            </sec:noAccess>
                        </td>
                        <td class="value">
                            <sec:access url="/payment/list">
                                <g:link controller="payment" action="user" id="${selected.id}">
                                    <g:message code="customer.show.all.payments"/>
                                </g:link>
                            </sec:access>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.credit.date"/></td>
                        <td class="value">
                            <sec:access url="/creditNote/show">
                                <g:remoteLink controller="creditNote" action="show" id="${latestCreditNote?.id}" before="register(this);" onSuccess="render(data, next);">
                                    <g:formatDate date="${latestCreditNote?.creditNoteDate}" formatName="date.pretty.format"/>
                                </g:remoteLink>
                            </sec:access>
                            <sec:noAccess url="/creditNote/show">
                                <g:formatDate date="${lastCreditNote?.creditNoteDate}" formatName="date.pretty.format"/>
                            </sec:noAccess>
                        </td>
                        <td class="value">
                            <sec:access url="/creditNote/list">
                                <g:link controller="creditNote" action="user" id="${selected.id}">
                                    <g:message code="customer.show.all.credits"/>
                                </g:link>
                            </sec:access>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.user.next.invoice.date"/></td>
                        <td class="value">
                            <g:set var="nextInvoiceDate" value="${customer.nextInvoiceDate}"/>

                            <g:if test="${nextInvoiceDate}">
                                <span><g:formatDate date="${nextInvoiceDate}" formatName="date.pretty.format"/></span>
                            </g:if>
                            <g:else>
                                <g:message code="prompt.no.active.orders"/>
                            </g:else>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.billing.cycle"/></td>
                        <td class="value">
                            <g:set var="subscriptionPeriod" value="${customer?.mainSubscription?.subscriptionPeriod}"/>
                            <g:set var="nextInvoiceDayOfPeriod" value="${customer?.mainSubscription?.nextInvoiceDayOfPeriod}"/>
                            ${Util.mapOrderPeriods(subscriptionPeriod.periodUnit.id.intValue(), nextInvoiceDayOfPeriod ?: '',subscriptionPeriod?.getDescription() ?: '',nextInvoiceDate)}
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.payment.due.date"/></td>
                        <td class="value"><g:formatDate date="${latestInvoice?.dueDate}" formatName="date.pretty.format"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="customer.detail.payment.invoiced.amount"/></td>
                        <td class="value"><g:formatNumber number="${latestInvoice?.totalAsDecimal}" type="currency" currencySymbol="${selected.currency.symbol}"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="invoice.label.status"/></td>
                        <td class="value">
                            <g:if test="${latestInvoice}">
                                <g:set var="invoiceStatus" value="${new InvoiceStatusDAS().find(latestInvoice?.statusId)}"/>
                                <g:if test="${latestInvoice?.statusId == Constants.INVOICE_STATUS_UNPAID && isCurrentCompanyOwning}">
                                    <g:link controller="payment" action="edit" params="[userId: selected.id, invoiceId: latestInvoice.id]" title="${message(code: 'invoice.pay.link')}">
                                        ${invoiceStatus.getDescription(session['language_id'])}
                                    </g:link>
                                </g:if>
                                <g:else>
                                    ${invoiceStatus?.getDescription(session['language_id'])}
                                </g:else>
                            </g:if>
                        </td>
                    </tr>
                <tr>
                    <td><g:message code="customer.detail.payment.amount.owed"/></td>
                    <td class="value"><g:formatNumber number="${UserBL.getBalance(selected.id)}" type="currency"  currencySymbol="${selected.currency.symbol}"/></td>
                    <td class="value">
                        <g:if test="${enableTotalOwnedPayment && isCurrentCompanyOwning}">
                            <g:link controller="payment" action="edit" params="[payOwned:true, userId:selected.id]">
                                <g:message code="customer.pay.total.owed"/>
                            </g:link>
                        </g:if>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="user.wallet.balance"/></td>
                    <td class="value" data-cy="walletBalance"><g:formatNumber number="${walletBalance}" type="currency"  currencySymbol="${selected.currency.symbol}" id="balance"/></td>
                    <td class="value">
                        <g:if test="${holdAmount>0}">
                            <sec:access url="/OnHold/edit">
                                <g:link controller="customer" action="showRechargeRequestPage" id="${selected.id}" params="[ currencySymbol : selected.currency.symbol]">
                                    <g:message code="refund.amount.hold"/>
                                    <g:formatNumber number="${holdAmount}" type="currency"  currencySymbol="${selected.currency.symbol}" id="balance"/>
                                </g:link>
                            </sec:access>
                        </g:if>
                    </td>
                    <sec:ifAllGranted roles="${PERMISSION_REFUND_WALLET_BALANCE}">
                        <td class="value">
                            <g:if test="${walletBalance>0}">
                                <sec:access url="/refund/edit">
                                    <g:link controller="customer" action="showRefundPage" id="${selected.id}" params="[ currencySymbol : selected.currency.symbol]">
                                        <g:message code="refund.title"/>
                                    </g:link>
                                </sec:access>
                            </g:if>
                        </td>
                    </sec:ifAllGranted>
                </tr>
                    <tr>
                        <td><g:message code="customer.detail.payment.lifetime.revenue"/></td>
                        <td class="value"><g:formatNumber number="${revenue}" type="currency"  currencySymbol="${selected.currency.symbol}"/></td>
                    </tr>
                </tbody>
            </table>
            <hr/>
            <g:each in="${selected.paymentInstruments}" var="paymentInstr">
                <g:render template="/customer/creditCard" model="[paymentInstr: paymentInstr]"/>
            </g:each>
        </div>
    </div>
<!-- Assets start  -->

<div class="heading">
    <strong><g:message code="order.label.assets"/></strong>
</div>
<table class="innerTable" >
    <thead class="innerHeader">
    <tr>
        <th class="first" style="min-width: 35px;"><g:message code="asset.detail.id"/></th>
        <th><g:message code="label.adennet.subscriber.number"/></th>
        <th><g:message code="recharge.request.status"/></th>
        <sec:ifAnyGranted roles="${PERMISSION_SUSPEND_ACTIVATE},${PERMISSION_REISSUE},${PERMISSION_RELEASE}">
            <th class="last" ><g:message code="asset.link"/></th>
        </sec:ifAnyGranted>
    </tr>
    </thead>
    <tbody>
    <g:each var="asset" in="${customerAssets}">
        <g:set var="subscriberNumber" value="${asset.subscriberNumber}"/>
        <tr>
            <td class="innerContent">
                <jB:secRemoteLink controller="product" action="showAsset" id="${asset.id}" params="['template': 'show']" before="register(this);" onSuccess="render(data, next);">
                    ${asset.id}
                </jB:secRemoteLink>
            </td>

            <td class="innerContent">
                <g:if test="${asset.status == ASSET_STATUS_RELEASED}">
                    ${asset.subscriberNumber}
                </g:if>
                <g:else>
                    <!-- <span id="userStatus" style="font-size:16px;">● </span> -->
                    ${asset.subscriberNumber}
                </g:else>
            </td>
            <td class="innerContent" data-cy="assetStatus">
                <g:if test="${asset.status == ASSET_STATUS_RELEASED}">
                    <g:message code="asset.status.released"/>
                </g:if>
                <g:elseif test="${asset.notes}">
                    <div class="tooltip" data-cy="suspendedOrActive">
                        <g:message code="${asset.isSuspended ? 'customer.status.suspended' : 'customer.status.active'}"/>
                        <span class="tooltiptext" data-cy="tooltiptext"> ${asset.notes}</span>
                    </div>
                </g:elseif>
                <g:else>
                    <g:message code="${asset.isSuspended ? 'customer.status.suspended' : 'customer.status.active'}"/>
                </g:else>
            </td>
            <g:if test="${asset.status != ASSET_STATUS_RELEASED}">
                <td class="innerContent">
                    <g:if test="${!selected.deleted}">
                        <sec:ifAllGranted roles="${PERMISSION_SUSPEND_ACTIVATE}">
                            <g:if test="${asset.isSuspended}">
                                <g:if test="${loggedInUser.getUserName() == asset.getSuspendedBy() || loggedInUser.getRole() != ROLE_OPERATION_USER }">
                                    <g:link controller="adennetAsset" action="showSuspendOrActivate" params="[id : asset.id, userId : "${selected.id}"]">
                                    <g:message code="${'asset.activation'}"/>
                                    </g:link>
                                </g:if>
                            </g:if>
                            <g:else>
                                <g:link controller="adennetAsset" action="showSuspendOrActivate" params="[id : asset.id, userId : "${selected.id}"]">
                                <g:message code="${'asset.suspension'}"/>
                                </g:link>
                            </g:else>
                        </sec:ifAllGranted>
                    </g:if>
                </td>
            </g:if>
        </tr>
    </g:each>
    </tbody>
</table>
<br/>
<!-- Assets end -->

<!-- cancellation start -->
    <sec:ifAllGranted roles="CUSTOMER_1906">
    <div class="heading">
        <strong><g:message code="customer.label.cancellation.request"/></strong>
    </div>
    <div class="box" id="cancellation_box">

        <div class="sub-box"><!-- show most recent order, invoice and payment -->
            <table class="innerTable" >
                        <thead class="innerHeader">
                        <tr>
                            <th style="min-width: 25px;"><g:message code="customer.label.cancellation.id"/></th>
                            <th><g:message code="customer.label.cancellation.status"/></th>
                            <th><g:message code="cancellation.date"/></th>
                            <th><g:message code="customer.cancellationRequest.reasonText"/></th>
                            <th/>
                            <th/>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each var="request" in="${cancellationRequests}">

                            <tr>
                                <td>${request.id}</td>
                                <td class="innerContent">
                                    ${request.status}
                                </td>
                                <td class="innerContent">
                                	<g:formatDate date="${request?.cancellationDate}" formatName="date.pretty.format"/>
                                	<g:hiddenField class="field" name="cancellationDate_val" id="cancellationDate_val" value="${formatDate(date: request.cancellationDate, formatName: 'datepicker.format')}"/>
                                </td>
                                <td class="innerContent">
                                    <span class="long">${request.reasonText}</span>
                                </td>
                                <g:if test="${request.status ==com.sapienter.jbilling.server.user.db.CancellationRequestStatus.APPLIED}">
                                    <g:set var="cancel_status" value="${request.status}"/>
                                        <sec:ifAllGranted roles="CUSTOMER_1906">
                                        <td>
                                            <a onclick="getCancellation(${request.id},'edit');" class=""><g:message code="button.edit"/></a>
                                        </td>
                                        <td>
                                            <a onclick="getCancellation(${request.id},'delete');" class="delete"><g:message code="button.delete"/></a>
                                        </td>
                                        </sec:ifAllGranted>
                                </g:if>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
        </div>
    </div>
    </sec:ifAllGranted>
    <br/>
<!-- cancellation end -->


    <div class="btn-box">
        <g:if test="${!selected.deleted}">
            <div class="row">

                <g:if test = "${isAssetAvailable}">
                    <g:if test="${isSimIssued}">
                        <sec:ifAllGranted roles="${PERMISSION_BUY_SUBSCRIPTION}">
                            <g:if test="${isCurrentCompanyOwning && (f != 'myAccount')}">
                                <g:link controller="customer" action="showBuySubscription" params="[userId: selected.id]"
                                 class="submit payment" data-cy="rechargeBtn"><span><g:message code="recharge.buy.subscription.button"/></span></g:link>
                            </g:if>
                        </sec:ifAllGranted>
                    </g:if>
                    <g:else>
                    <g:each var="asset" in="${customerAssets}">
                        <g:if test="${asset.identifier == selected.userName && !asset.isSuspended && asset.status==ASSET_STATUS_IN_USE}">
                            <sec:ifAllGranted roles="${PERMISSION_RECHARGE}">
                                <g:if test="${isCurrentCompanyOwning && (f != 'myAccount')}">
                                    <g:link controller="customer" action="showRecharge" params="[userId: selected.id]"
                                        class="submit payment" data-cy="rechargeButton"><span><g:message code="recharge.button"/></span></g:link>
                                </g:if>
                            </sec:ifAllGranted>
                        </g:if>
                    </g:each>
                    </g:else>
                </g:if>

                <sec:ifAllGranted roles="ORDER_20">
                    <g:if test="${isCurrentCompanyOwning}">
                        <g:link controller="orderBuilder" action="edit" params="[userId: selected.id]"
                                class="submit order"><span><g:message code="button.create.order"/></span></g:link>
                    </g:if>
                </sec:ifAllGranted>
                <sec:ifAllGranted roles="PAYMENT_30">
                    <g:if test="${isCurrentCompanyOwning && (f != 'myAccount')}">
                        <g:link controller="payment" action="edit" params="[userId: selected.id]"
                                class="submit payment"><span><g:message code="button.make.payment"/></span></g:link>
                    </g:if>
                </sec:ifAllGranted>

                <sec:ifAllGranted roles="CREDIT_NOTE_2000">
                    <g:if test="${isCurrentCompanyOwning && (f != 'myAccount')}">
                        <g:link controller="creditNote" action="edit" params="[userId: selected.id]"
                                class="submit credit"><span><g:message code="button.create.creditNote"/></span></g:link>
                    </g:if>
                </sec:ifAllGranted>
                <sec:ifAllGranted roles="CUSTOMER_1906">
                    <g:if test="${!isCancelled}">
                        <a onclick="openCancellationDialog()" class="submit add">
                            <span><g:message code="customer.create.cancellation.button"/></span>
                        </a>
                    </g:if>
                </sec:ifAllGranted>

                <sec:ifAllGranted roles="${permission ? permission : 'CUSTOMER_11'}">
                    <g:if test="${isCurrentCompanyOwning}">
                        <g:link action="edit" id="${selected.id}" class="submit edit button-secondary" data-cy="editButton"><span><g:message code="button.edit"/></span></g:link>
                    </g:if>
                </sec:ifAllGranted>

                <sec:ifAllGranted roles="CUSTOMER_12">
                    <g:if test="${isCurrentCompanyOwning}">
                        <a onclick="showConfirm('delete-${selected.id}');" class="submit delete" data-cy="deleteButton"><span><g:message code="button.delete"/></span></a>
                    </g:if>
                </sec:ifAllGranted>

                <sec:ifAllGranted roles="CUSTOMER_1100">
                    <g:set var="terminationMf" value="${customer.getMetaField('Termination')}"/>
                    <g:if test="${terminationMf == null || (terminationMf.getValue() != 'Termination Processing' && terminationMf.getValue() != 'Dropped' && terminationMf.getValue() != 'Esco Initiated')}">
                        <a onclick="showConfirmterminate('terminate-${selected.id}');" class="submit delete"><span><g:message code="button.terminate"/></span></a>
                    </g:if>
                </sec:ifAllGranted>

                <sec:ifAllGranted roles="CUSTOMER_1101">
                    <g:if test="${customer?.isParent > 0}">
                        <g:link controller="customer" action="edit" params="[parentId: selected.id]" class="submit add"><span><g:message code="customer.add.subaccount.button"/></span></g:link>
                    </g:if>
                </sec:ifAllGranted>
            </div>
        </g:if>
    </div>

    <g:render template="/confirm"
              model="['message': 'customer.delete.confirm',
                      'controller': 'customer',
                      'action': 'delete',
                      'id': selected.id
                     ]"/>

    <g:applyLayout name="confirm"
              model="['message': 'customer.terminate.confirm',
                      'controller': 'customer',
                      'action': 'terminate',
                      'id': selected.id,
                      'width': 420,
                      'height': 320
              ]">
        <p>
            <g:if test="${EnumerationDTO.findByName('TERMINATION_CODES') != null}">
                <g:message code="customer.termination.reason" /> <g:select name="reason" from="${EnumerationDTO.findByName('TERMINATION_CODES')?.getValues()}" optionKey="value" valueMessagePrefix="enum.termination-codes" />
            </g:if>
        </p>
        <p>
            <g:applyLayout name="form/date">
                <content tag="label"><g:message code="customer.termination.effective.date"/></content>
                <content tag="label.for">effectiveDate</content>
                <g:textField class="field" name="effectiveDate" value="${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}"/>
            </g:applyLayout>
        </p>
    </g:applyLayout>

</div>
<script type="text/javascript" xmlns="http://www.w3.org/1999/html">
    $(document).ready(function () {
        ${partial};
        <g:if test="${partial}">
            registerSlideEvents();
        </g:if>
    });
/*
$(document).ready(()=> {
    isSubscriberOnline()
        .then((response)=>{
            const userStatusElement = document.getElementById('userStatus');
            const [title, status] = response.split(" ");
            const isOnline = status === 'true';
            userStatusElement.title = title;
            userStatusElement.style.color = isOnline ? 'green' : 'grey';
        })
        .catch((error) => {
                    console.error("Error:", error);
    });
});

//checking subscriber session status
function isSubscriberOnline(){
   return new Promise((resolve, reject)=>{
   const subscriberNumber = ${subscriberNumber}
       $.ajax({
           url: '${createLink(action: 'checkIsSubscriberOnline')}',
           data: {subscriberNumber :subscriberNumber },
           success: (response) => resolve(response),
           error: (error) => reject(error)
       });
   });
}
*/
</script>
