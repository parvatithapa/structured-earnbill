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

<%@ page import="com.sapienter.jbilling.common.CommonConstants;" %>
<%@ page import="com.sapienter.jbilling.common.Constants;" %>
<%@ page import="com.sapienter.jbilling.server.invoice.InvoiceTemplateDTO;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.UserDAS;" %>
<%@ page import="com.sapienter.jbilling.server.user.UserDTOEx;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.user.permisson.db.RoleDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.Util;" %>
<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.db.EnumerationDTO;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper;" %>
<%@ page import="com.sapienter.jbilling.server.util.PreferenceBL;" %>
<%@ page import="com.sapienter.jbilling.server.user.db.AccountTypeDAS;" %>
<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_NATIONAL_ID;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_PASSPORT;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_COMPANY_LETTER;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_OFFICIAL_LETTER;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.CUSTOMER_TYPE_VIP;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.CUSTOMER_TYPE_GOVERNMENT;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_CHANGE_CUSTOMER_TYPE;" %>
<html>


<head>
    <meta name="layout" content="main" />

    <style>
		.ui-widget-content .ui-state-error{
			background-color:white;
			background: none;
		}
		.row .inp-bg{
			display: table-caption;
		}

	</style>
    <r:script>
        var customerTypeElement;
        var governorateElement;
        var flag = true;
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
        function checkIdentificationType() {
            if ($('#identificationType').val()==="${IDENTIFICATION_TYPE_NATIONAL_ID}") {
                $('#national_id').show();
                $('#passport_id').hide();
                $('#company_letter').hide();
                $('#official_letter').hide();
            }
            else if ($('#identificationType').val()==="${IDENTIFICATION_TYPE_PASSPORT}") {
                $('#national_id').hide();
                $('#passport_id').show();
                $('#company_letter').hide();
                $('#official_letter').hide();
            }
            else if ($('#identificationType').val()==="${IDENTIFICATION_TYPE_COMPANY_LETTER}") {
                $('#national_id').hide();
                $('#passport_id').hide();
                $('#company_letter').show();
                $('#official_letter').hide();
            }
            else if ($('#identificationType').val()==="${IDENTIFICATION_TYPE_OFFICIAL_LETTER}") {
                $('#national_id').hide();
                $('#passport_id').hide();
                $('#company_letter').hide();
                $('#official_letter').show();
            }
            else {
                $('#national_id').hide();
                $('#passport_id').hide();
                $('#company_letter').hide();
                $('#official_letter').hide();
            }
        }

        function checkCustomerType() {
            if(flag && ${user && user?.userId && user?.userId != 0} && ${SpringSecurityUtils.ifNotGranted("${PERMISSION_CHANGE_CUSTOMER_TYPE}")}) {
                var customerType = $("[name='" + customerTypeElement + "']").val();
                $("[name='" + customerTypeElement + "'] option").not("option[value='" + customerType +"']").attr("disabled",true);
            }
            if(!flag) {
                $('#identificationType').val("").change();
            }
            flag = false;

            if ($("[name='" + customerTypeElement + "']").val()==="") {
                $('#upload_documents').hide();
            }
            else if ($("[name='" + customerTypeElement + "']").val()==="${CUSTOMER_TYPE_VIP}") {
                $("#identificationType option[value='${IDENTIFICATION_TYPE_NATIONAL_ID}']").attr('disabled', true);
                $("#identificationType option[value='${IDENTIFICATION_TYPE_PASSPORT}']").attr('disabled', true);
                $("#identificationType option[value='${IDENTIFICATION_TYPE_COMPANY_LETTER}']").removeAttr('disabled');
                $("#identificationType option[value='${IDENTIFICATION_TYPE_OFFICIAL_LETTER}']").attr('disabled', true);
                $('#upload_documents').show();
                $('#national_id').hide();
                $('#passport_id').hide();
                $('#company_letter').hide();
                $('#official_letter').hide();
            }
            else if ($("[name='" + customerTypeElement + "']").val()==="${CUSTOMER_TYPE_GOVERNMENT}") {
                $("#identificationType option[value='${IDENTIFICATION_TYPE_NATIONAL_ID}']").attr('disabled', true);
                $("#identificationType option[value='${IDENTIFICATION_TYPE_PASSPORT}']").attr('disabled', true);
                $("#identificationType option[value='${IDENTIFICATION_TYPE_COMPANY_LETTER}']").attr('disabled', true);
                $("#identificationType option[value='${IDENTIFICATION_TYPE_OFFICIAL_LETTER}']").removeAttr('disabled');
                $('#upload_documents').show();
                $('#national_id').hide();
                $('#passport_id').hide();
                $('#company_letter').hide();
                $('#official_letter').hide();
            }
            else {
                $("#identificationType option[value='${IDENTIFICATION_TYPE_NATIONAL_ID}']").removeAttr('disabled');
                $("#identificationType option[value='${IDENTIFICATION_TYPE_PASSPORT}']").removeAttr('disabled');
                $("#identificationType option[value='${IDENTIFICATION_TYPE_COMPANY_LETTER}']").attr('disabled', true);
                $("#identificationType option[value='${IDENTIFICATION_TYPE_OFFICIAL_LETTER}']").attr('disabled', true);
                $('#upload_documents').show();
                $('#national_id').hide();
                $('#passport_id').hide();
                $('#company_letter').hide();
                $('#official_letter').hide();
            }
        }

        function fetchUserGovernorate() {
            $.ajax({
                type: 'GET',
                url: '${createLink(action: 'fetchUserGovernorate')}',
                success: function(data) {
                    if(data != "") {
                        $("[name='" + governorateElement + "']").val(data).change();
                        $("[name='" + governorateElement + "'] option").not("option[value='" + data +"']").attr("disabled",true);
                    }
                }
            });
        }

        function fetchMetaFieldId(metaFieldName) {
            $.ajax({
                type: 'GET',
                url: '${createLink(action: 'fetchMetaFieldId')}',
                async: false,
                data: {metaFieldName: metaFieldName},
                success: function(data) {
                    if(metaFieldName == "Customer Type") {
                        customerTypeElement = "metaField_" + data + ".value";
                    }
                    else if(metaFieldName == "Governorate") {
                        governorateElement = "metaField_" + data + ".value";
                    }
                }
            });
        }

		$(function() {
	        $('#mainSubscription_periodId').change(function() {
	        	updateSubscription();
	        });
	    });

	    function updateSubscription() {
	        $.ajax({
	            type: 'POST',
	            url: '${createLink(action: 'updateSubscription')}',
	            data: $('#subscriptionTemplate').parents('form').serialize(),
	            success: function(data) { $('#subscriptionTemplate').replaceWith(data);}
	        });
	    }


	    $(function() {
	        $('#companyBillingCycle').click(function() {
	            if ($('#orderPeriodSubscriptionUnit').val()) {
	                updateSubscriptionOnBillingCycle();
	                $('#mainSubscription_periodId').val($('#orderPeriodSubscriptionUnit').val()).change();
	            } else {
	                $('#error-messages').show();
	                $('#error-messages ul').show();
                    $('#error-messages ul').html("<g:message code="validation.error.billing.cycle.not.exist" args="${ [periodUnitCompany] }"/>");
	            }
	        });
	    });

	    function updateSubscriptionOnBillingCycle() {
	        $.ajax({
	            type: 'POST',
	            url: '${createLink(action: 'updateSubscriptionOnBillingCycle')}',
	            data: $('#subscriptionTemplate').parents('form').serialize(),
	            success: function(data) { $('#subscriptionTemplate').replaceWith(data);}
	        });
	    }

   		var hasChild = ${user?.childIds?.toList()?.size() > 0}

    	$(document).ready(function() {
    	    fetchMetaFieldId("Customer Type");
    	    fetchMetaFieldId("Governorate");
    	    checkCustomerType();
    	    checkIdentificationType();
    	    fetchUserGovernorate();

    	    //apply date picker to each startDate-*
    		$.each( $("input[name^='startDate']"), function () {
  				$(this).datepicker({dateFormat: "${message(code: 'datepicker.jquery.ui.format')}", showOn: "both", buttonImage: "${resource(dir:'images', file:'icon04.gif')}", buttonImageOnly: true});
			});

			$('#user\\.isParent').change(function() {
	       		if ( ! this.checked && hasChild) {
	       		    $("#background").height($(document).height());
       			    $("#background").show();
	        		$("#user\\.isParent").prop('checked', true);
	        		$( "#dialog" ).dialog({
					  dialogClass: "no-close",
					  buttons: [
					    {
					      text: "Close",
					      click: function() {
					        $( this ).dialog( "close" );
					        $("#background").hide();
					      }
					    }
					  ]
					});
				}
	        });

            $("[name='" + customerTypeElement + "']").change(function() {
                checkCustomerType();
            });
	   	});

        var noteTitle = $( "#noteTitle" ),
                userId = "${new UserDAS().find(session['user_id']).getUserName()}",
                noteContent = $("#noteContent" );
                allFields = $( [] ).add( noteTitle ).add( noteContent ),
                tips = $( ".validateTips" );
                date = "${Util.formatDate(TimezoneHelper.currentDateForTimezone(session['company_timezone']), "MMM d, YYY")}"
                i=0;
        $( "#customer-add-note-dialog" ).dialog({
                autoOpen: false,
                height: 350,
                width: 530,
                // Workaround for modal dialog dragging jumps
                create: function (event) {
                    $(event.target).parent().css("position", "fixed");
                },
                modal: true,
                buttons: {
                   "${g.message(code:'button.add.note')}": function() {
                    var bValid = true;
                    allFields.removeClass( "ui-state-error" );
                    noteTitle.parent().removeClass( "ui-state-error" );
                    bValid = bValid && checkLength( noteTitle, "title", 1, 50, true);
                    bValid = bValid && checkLength( noteContent, "content", 1, 1000, false);
						if(bValid){
                        	$( "#users tbody" ).
                            prepend( "<tr>" +
                                "<input type='hidden' name ='notes."+i+".noteId' value='0'>" +
                                "<td style='background-position: 0 -71px;padding: 3px 0 3px 15px;'>" + userId + "<input type='hidden' name ='notes."+i+".userId' value='"+${session['user_id']}+"'></td>" +
                                "<td>" + date +"</td>"+
                                "<td>" + noteTitle.val() + "<input type='hidden' name ='notes."+i+".noteTitle' value='"+noteTitle.val()+"'></td>" +
                                "<td>" + noteContent.val().replace(/'/g, "&#39;") + "<input type='hidden' name ='notes."+i+".noteContent' value='"+noteContent.val().replace(/'/g, "&#39;")+"'></td>" +
                                "</tr>" );
                                 i++
                                $("#newNotesTotal").val(i)
                            $( this ).dialog( "close" );
                            $("#noResults").hide()
              	 		}
                    },
                    "${g.message(code:'button.cancel')}": function() {
                        $( this ).dialog( "close" );
                    }
                },
                "${g.message(code:'button.close')}": function() {
                	allFields.val( "" ).removeClass( "ui-state-error" );
                }
        });


        function checkLength( o, n, min, max, parent) {
	        if ( o.val().length > max || o.val().length < min ) {
	            if (parent) {
                    o.parent().addClass( "ui-state-error" );
	            } else {
	                o.addClass( "ui-state-error" );
	            }

	            updateTips( "Length of " + n + " must be between " +min + " and " + max + "." );
	            return false;
	        } else {
	            return true;
	        }
	    }

	    function updateTips( t ) {
	        tips.text( t ).addClass( "ui-state-error" );
	    }

        $('#contactType').change(function() {
            var selected = $('#contact-' + $(this).val());
            $(selected).show();
            $('div.contact').not(selected).hide();
        }).change();

		function openDialog() {
		    $("#noteTitle").val("");
		    $("#noteContent").val("");
		    tips.text("${g.message(code:'customer.note.fields.required')}").removeClass( "ui-state-error" );
		    $( "#customer-add-note-dialog" ).dialog( "open" );
		}

		function checkIdentificationNumber(identificationType) {
		    var identificationNumber = (identificationType === "National ID") ? $('#txtNationalId').val() : $('#txtPassportId').val();
		    $.ajax({
                url: '${createLink(action: 'getSubscriberNumbers')}',
                data: {userId : ${user?.userId}, identificationNumber : identificationNumber, identificationType: identificationType},
                success: function(data) {
                    if(data.length != 2) {
                        data = data.replace("[", "");
                        data = data.replace("]", "");
                        data = data.replace(/'/g, "");
                        var para_message = (identificationType === "National ID") ? '<g:message code="subscriber.message.already.present.national.id"/>' : '<g:message code="subscriber.message.already.present.passport.number"/>';
                        para_message+= '(' + identificationNumber + '): ' + data;
                        $('#dialog-confirm').html('<div><span class="ui-icon ui-icon-alert" id="customer-dialog-icon" style="margin:12px 12px 20px 12px;"></span></div><div>' + para_message + '</div>');
                        $("#dialog-confirm").dialog({
                            autoOpen: true,
                            height: "auto",
                            width: 375,
                            modal: true,
                            buttons: {
                                '<g:message code="prompt.yes"/>': function() {
                                    $(this).dialog("close");
                                },
                                '<g:message code="prompt.no"/>': function() {
                                    document.querySelector("#user-edit-form > fieldset > div.buttons > ul > li:nth-child(2) > a").click();
                                }
                            }
                        });
                    }
                }
            });
        }

        function checkSubscriberNumber(){

            var subscriberNumber =  $('#subNumber').val() ;
                $.ajax({
                     url: '${createLink(action: 'checkSubscriberNumberIsValid')}',
                     data: { userName : subscriberNumber },
                         success: function(data) {
                         if(data != 'true'){
                            $("html, body").animate({ scrollTop: 0 }, "slow");
                         }
                     }
                });
        }

    </r:script>

</head>
<body>

<div id="dialog-confirm" class="bg-lightbox" style="display: flex;" title="<g:message code="popup.continue.title"/>">
</div>

<div id="customer-add-note-dialog" title="${g.message(code:'button.add.note')}" class="jb-dialog">
    <div class="row"><p id="validateTips" class="validateTips" style=" border: 1px solid transparent;"></p></div>
    <g:form id="notes-form" name="notes-form" url="[action: 'saveCustomerNotes']" useToken="true">
		<g:render template="/customer/customerNotesForm" />
    </g:form>
</div>

<g:set var="defaultCurrency" value="${CompanyDTO.get(session['company_id']).getCurrency()}"/>

<div class="form-edit">

    <g:set var="isNew" value="${!user || !user?.userId || user?.userId == 0}"/>
    <g:set var="accountTypeId" value="${accountType?.id}"/>
    <g:set var="templateName" value="${null != templateName? templateName : "monthly"}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="customer.create.title"/>
            </g:if>
            <g:else>
                <g:message code="customer.edit.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="user-edit-form" action="save" useToken="true" enctype="multipart/form-data">
            <fieldset>
                <div class="form-columns">
                    <!-- user details column -->
                    <div class="column customer-column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.customer.number"/></content>

                            <g:if test="${!isNew}">
                                <span>
                                    <g:link controller="customerInspector" action="inspect"
                                            id="${user.userId}" title="${message(code: 'customer.inspect.link')}">
                                        ${user.userId}
                                    </g:link>
                                </span>
                            </g:if>
                            <g:else>
                                <em><g:message code="prompt.id.new"/></em>
                            </g:else>

                            <g:hiddenField name="user.userId" value="${user?.userId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.user.account.type"/></content>
                            <em>
                                <g:if test="${accountType}">
                                    ${accountType.getDescription(session['language_id'])}
                                </g:if><g:else>-</g:else>
                            </em>

                            <g:hiddenField name="user.accountTypeId" value="${accountType?.id}"/>
                            <g:hiddenField name="user.entityId" value="${company?.id}"/>
                             <g:hiddenField name="user.reissueCount" value="${user.reissueCount}"/>
                             <g:hiddenField name="user.reissueDate" value="${user.reissueDate}"/>
                        </g:applyLayout>

                        <g:if test="${isNew}">
                            <g:applyLayout name="form/input">
                                <content tag="label">
                                    <g:message code="label.adennet.iccid"/>
                                    <span id="mandatory-meta-field">*</span>
                                </content>
                                <content tag="label.for">user.userName</content>
                                <g:textField class="field" id="subNumber"  name="user.userName" value="${user?.userName}"  onfocusout="checkSubscriberNumber()"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="label.adennet.iccid"/></content>

                                ${displayer?.getDisplayName(user)}
                                <g:hiddenField name="user.userName" value="${user?.userName}"/>
                            </g:applyLayout>
                        </g:else>

                        <g:if test="${!isNew && user?.userId == loggedInUser?.id}">
                            <sec:ifAnyGranted roles="MY_ACCOUNT_161">
                                <g:if test="${isUserSSOEnabled && ssoActive}">
                                    <g:applyLayout name="form/text">
                                        <content tag="label">
                                            <g:message code="prompt.sso.user.password"/>
                                        </content>
                                        <a href="${resetPasswordUrl}" target="_blank">Update Password on IDP</a>
                                    </g:applyLayout>
                                </g:if>
                                <g:else>
                                    <g:applyLayout name="form/input">
                                        <content tag="label">
                                            <g:message code="prompt.current.password"/>
                                        </content>
                                        <content tag="label.for">oldPassword</content>
                                        <g:passwordField class="field" name="oldPassword"/>
                                    </g:applyLayout>

                                    <g:applyLayout name="form/input">
                                        <content tag="label">
                                            <g:message code="prompt.password"/>
                                        </content>
                                        <content tag="label.for">newPassword</content>
                                        <g:passwordField class="field" name="newPassword"/>
                                    </g:applyLayout>

                                    <g:applyLayout name="form/input">
                                        <content tag="label">
                                            <g:message code="prompt.verify.password"/>
                                        </content>
                                        <content tag="label.for">verifiedPassword</content>
                                        <g:passwordField class="field" name="verifiedPassword"/>
                                    </g:applyLayout>
                                </g:else>
                            </sec:ifAnyGranted>
                        </g:if>

                        <!-- CUSTOMER CREDENTIALS -->
                        <!--
                            <g:if test="${isNew}">
                                <g:preferenceEquals preferenceId="${Constants.PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT}" value="0">
                                    <g:applyLayout name="form/checkbox">
                                        <content tag="label"><g:message code="prompt.create.credentials"/></content>
                                        <content tag="label.for">user.createCredentials</content>
                                        <g:checkBox class="cb checkbox" name="user.createCredentials" checked="${user?.createCredentials}"/>
                                    </g:applyLayout>
                                </g:preferenceEquals>
                            </g:if>
                        -->
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.status"/></content>
                            <content tag="label.for">user.statusId</content>
                            %{--<content tag="label.value">${user?.statusId}</content>--}%
                            <g:if test="${params.id}">
                                <g:userStatus       name = "user.statusId"
                                                   value = "${user?.statusId}"
                                              languageId = "${session['language_id']}"
                                                  roleId = "${CommonConstants.TYPE_CUSTOMER}"/>
							</g:if>
							<g:else>
								<g:userStatus       name = "user.statusId"
                                                   value = "${user?.statusId}"
                                              languageId = "${session['language_id']}"
                                                  roleId = "${CommonConstants.TYPE_CUSTOMER}"
                                                disabled = "true"/>
							</g:else>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.subscriber.status"/></content>
                            <content tag="label.for">user.subscriberStatusId</content>
                            <g:subscriberStatus       name = "user.subscriberStatusId"
                                                     value = "${user?.subscriberStatusId}"
                                                languageId = "${session['language_id']}" />
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.language"/></content>
                            <content tag="label.for">user.languageId</content>
                            <g:select        name = "user.languageId"
                                             from = "${LanguageDTO.list(sort : "id",order :"asc")}"
                                        optionKey = "id"
                                      optionValue = "description"
                                            value = "${user?.languageId}"  />
                        </g:applyLayout>
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.currency"/></content>
                            <content tag="label.for">user.currencyId</content>
                            <g:select        name = "user.currencyId"
                                             from = "${currencies?.sort {it.description}}"
                                        optionKey = "id"
                                      optionValue = "${{it.getDescription(session['language_id'])}}"
                                            value = "${user?.currencyId ?: defaultCurrency?.id}" />
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.preferred.auto.payment"/></content>
                            <content tag="label.for">user.automaticPaymentType</content>
                            <g:select               name = "user.automaticPaymentType"
                                                    from = "${[Constants.AUTO_PAYMENT_TYPE_CC, Constants.AUTO_PAYMENT_TYPE_ACH]}"
                                      valueMessagePrefix = "auto.payment.type"
                                                   value = "${user?.automaticPaymentType}"/>
                        </g:applyLayout>

                        <g:if test="${user?.partnerIds || !partnerId}">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.partner.ids"/></content>
                                <content tag="label.for">user.partnerId</content>
                                <g:textField class = "field"
                                              name = "user.partnerIdList"
                                             value = "${user?.partnerIds?.join(',') ?: partnerId}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.partner.id"/></content>
                                ${partnerId}
                                <g:hiddenField class="field" name="user.partnerIdList" value="${partnerId}"/>
                            </g:applyLayout>
                        </g:else>

                        <g:if test="${parent?.customerId}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.parent.id"/></content>
                                <g:link action="list" id="${parent.userId}">${parent.userId} ${parent.userName}</g:link>
                                <g:hiddenField class="field" name="user.parentId" value="${parent.userId}"/>
                            </g:applyLayout>
                        </g:if>

                        <g:else>
                            <sec:ifAllGranted roles="CUSTOMER_1101">
                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="prompt.parent.id"/></content>
                                    <content tag="label.for">user.parentId</content>
                                    <g:textField class = "field"
                                                  name = "user.parentId"
                                                 value = "${user?.parentId}"/>
                                </g:applyLayout>
                            </sec:ifAllGranted>
                        </g:else>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="prompt.allow.sub.accounts"/></content>
                            <content tag="label.for">user.isParent</content>
                            <g:checkBox   class = "cb checkbox"
                                           name = "user.isParent"
                                        checked = "${user?.isParent}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="prompt.invoice.if.child"/></content>
                            <content tag="label.for">user.invoiceChild</content>
                            <g:checkBox   class = "cb checkbox"
                                           name = "user.invoiceChild"
                                        checked = "${user?.invoiceChild}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="prompt.use.parent.pricing"/></content>
                            <content tag="label.for">user.useParentPricing</content>
                            <g:checkBox   class = "cb checkbox"
                                           name = "user.useParentPricing"
                                        checked = "${user?.useParentPricing}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="prompt.exclude.ageing"/></content>
                            <content tag="label.for">user.excludeAgeing</content>
                            <g:checkBox   class = "cb checkbox"
                                           name = "user.excludeAgeing"
                                        checked = "${user?.excludeAgeing}"/>
                        </g:applyLayout>

                        <g:set var="isReadOnly" value="true"/>
                        <sec:ifAllGranted roles="CUSTOMER_11">
                            <g:set var="isReadOnly" value="false"/>
                        </sec:ifAllGranted>
                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="user.account.lock"/></content>
                            <content tag="label.for">user.isAccountLocked</content>
                            <g:checkBox    class = "cb checkbox"
                                            name = "user.isAccountLocked"
                                         checked = "${user?.isAccountLocked}"
                                        disabled = "${isReadOnly}"/>
                        </g:applyLayout>
                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="user.account.inactive"/></content>
                            <content tag="label.for">user.accountExpired</content>
                            <g:checkBox    class = "cb checkbox"
                                            name = "user.accountExpired"
                                         checked = "${user?.accountExpired}"
                                        disabled = "${isReadOnly}"/>
                        </g:applyLayout>


                    </div>

                    <div class="column customer-column">
                    %{-- Allow the user to add linked user codes --}%
                        <g:applyLayout name="form/input">
                            <content tag="label">&nbsp;<g:message code="prompt.userCode"/></content>
                            <content tag="label.for">user.userCode[${ucIndex}]</content>

                        %{-- Display the existing user codes --}%
                            <g:if test="${user?.userCodeLink}">
                                <div id="userCode">
                                    <sec:ifAllGranted roles="USER_144">
                                        <g:textField class="field userCode-marker" name="user.userCodeLink" value="${user.userCodeLink}" />
                                    </sec:ifAllGranted>
                                    <sec:ifNotGranted roles="USER_144">
                                        <g:hiddenField name="user.userCodeLink" value="${user.userCodeLink}" />
                                        ${uc}
                                    </sec:ifNotGranted>
                                </div>
                            </g:if>
                            <g:else>
                            %{-- Allow the user to add user codes --}%
                                <sec:ifAllGranted roles="USER_143">
                                    <div>
                                        <g:textField class="field userCode-marker" name="user.userCodeLink"  value=""/>
                                    </div>
                                </sec:ifAllGranted>
                            </g:else>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="prompt.credit.limit"/></content>
                            <content tag="label.for">user.creditLimitAsDecimal</content>
                            <g:textField class = "field"
                                          name = "user.creditLimit"
                                         value = "${formatNumber(number: user?.creditLimitAsDecimal ?: 0, formatName: 'money.format')}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="prompt.auto.recharge"/></content>
                            <content tag="label.for">user.autoRechargeAsDecimal</content>
                            <g:textField class = "field"
                                          name = "user.autoRechargeAsDecimal"
                                         value = "${formatNumber(number: user?.autoRechargeAsDecimal ?: 0, formatName: 'money.format')}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="prompt.auto.recharge.threshold"/></content>
                            <content tag="label.for">user.rechargeThreshold</content>
                            <g:textField  class = "field"
                                           name = "user.rechargeThreshold"
                                          value = "${formatNumber(number: user?.rechargeThresholdAsDecimal ?: 0, formatName: 'money.format')}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="prompt.auto.notification.below.threshold"/></content>
                            <content tag="label.for">user.lowBalanceThreshold</content>
                            <g:textField class = "field"
                                          name = "user.lowBalanceThreshold"
                                         value = "${formatNumber(number: user?.lowBalanceThresholdAsDecimal ?: 0, formatName: 'money.format')}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="prompt.auto.recharge.monthly.limit"/></content>
                            <content tag="label.for">user.monthlyLimit</content>
                            <g:textField class="field" name="user.monthlyLimit" value="${formatNumber(number: user?.monthlyLimitAsDecimal ?: 0, formatName: 'money.format')}"/>
                        </g:applyLayout>
                    </div>
                    <div class="column customer-column">
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.invoice.delivery.method"/></content>
                            <content tag="label.for">user.invoiceDeliveryMethodId</content>
                            <g:select               from = "${company.invoiceDeliveryMethods.sort{ it.id }}"
                                               optionKey = "id"
                                      valueMessagePrefix = "customer.invoice.delivery.method"
                                                    name = "user.invoiceDeliveryMethodId"
                                                   value = "${user?.invoiceDeliveryMethodId}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.due.date.override"/></content>
                            <content tag="label.for">user.dueDateValue</content>
                            <div class="inp-bg inp4">
                                <g:textField class="field" name="user.dueDateValue" value="${user?.dueDateValue}"/>
                            </div>
                            <div class="select select-holder"><span class="select-value"></span>
                                <g:select        from = "${periodUnits}"
                                            optionKey = "id"
                                          optionValue = "${{it.getDescription(session['language_id'])}}"
                                                 name = "user.dueDateUnitId"
                                                value = "${user?.dueDateUnitId ?: com.sapienter.jbilling.server.util.Constants.PERIOD_UNIT_DAY}"/>
                            </div>
                        </g:applyLayout>
                        <g:set var="mainSubscription" value="${null != user ? user?.mainSubscription : mainSubscription}"/>
                        <sec:ifAllGranted roles="CUSTOMER_121">
                            <g:applyLayout name="form/text">
                                <content tag="label">&nbsp;</content>
                                <content tag="label.for">orderPeriodSubscriptionUnit</content>
                                <div class="btn-row" style="vertical-align: top; margin: 0px; padding: 0px;">
                                    <a id="companyBillingCycle" class="submit save"><span><g:message code="button.use.company.billing.cycle"/></span></a>
                                    <g:hiddenField id="orderPeriodSubscriptionUnit" name="orderPeriodSubscriptionUnit" value="${orderPeriodSubscriptionUnit}"/>
                                </div>
                            </g:applyLayout>
                            <g:applyLayout name="form/select">
	                        %{--<div class="select">--}%
	                        	<content tag="label"><g:message code="prompt.main.subscription"/></content>
								<content tag="label.for">mainSubscription.periodId</content>
					        	 <g:select          id = "mainSubscription_periodId"
                                                  from = "${orderPeriods}"
                                             optionKey = "id"
                                           optionValue = "${{it.getDescription(session['language_id'])}}"
					                              name = "mainSubscription.periodId"
					                             value = "${mainSubscription?.periodId ?: orderPeriods.find{ it?.periodUnit?.id == com.sapienter.jbilling.server.util.Constants.PERIOD_UNIT_MONTH && it?.value == 1}?.id}"/>
					    	%{--</div>--}%
					        </g:applyLayout>
					        <g:render id="subscriptionTemplate" template="/customer/subscription/${templateName}" model="[mainSubscription: mainSubscription]" />
					    </sec:ifAllGranted>
                        <sec:ifNotGranted roles="CUSTOMER_121">
                            <g:hiddenField  name = "mainSubscription.periodId"
                                           value = "${mainSubscription?.periodId ?: orderPeriods.find{ it?.periodUnit?.id == com.sapienter.jbilling.server.util.Constants.PERIOD_UNIT_MONTH && it?.value == 1}?.id}"/>
                            <g:hiddenField  name = "mainSubscription.nextInvoiceDayOfPeriod"
                                           value = "${mainSubscription?.nextInvoiceDayOfPeriod ?: 1}"/>
                            <g:hiddenField  name = "mainSubscription.nextInvoiceDayOfPeriodOfYear"
                                           value = "${mainSubscription?.nextInvoiceDayOfPeriodOfYear}"/>
                        </sec:ifNotGranted>



						<g:if test="${!isNew}">
                            <sec:ifAllGranted roles="CUSTOMER_19">
                                <g:applyLayout name="form/date">
                                    <content tag="label"><g:message code="next.invoice.date"/></content>
                                    <content tag="label.for">user.nextInvoiceDate</content>
                                        <g:textField  class = "field"
                                                       name = "user.nextInvoiceDate"
                                                      value = "${formatDate(date: user?.nextInvoiceDate, formatName:'datepicker.format')}"
                                                     onblur = "validateDate(this)"/>
                                </g:applyLayout>
                            </sec:ifAllGranted>
                            <sec:ifNotGranted roles="CUSTOMER_19">
                                <g:hiddenField name="user.nextInvoiceDate" value="${formatDate(date: user?.nextInvoiceDate, formatName:'datepicker.format')}"/>
                            </sec:ifNotGranted>
                        </g:if>
                     	 <br/>&nbsp;
                        <g:preferenceEquals preferenceId="${Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION}" value="1">
                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="prompt.invoice.template"/></content>
                                <g:select        name = "user.invoiceTemplateId"
                                                 from = "${InvoiceTemplateDTO.findAllWhere("entity.id": session['company_id'])}"
                                                value = "${user?.invoiceTemplateId}"
                                            optionKey = "id"
                                          optionValue = "name"/>
                            </g:applyLayout>
                        </g:preferenceEquals>



                        <g:preferenceIsNullOrEquals preferenceId="${Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION}" value="0">
                        <g:if test="${isNew}">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="prompt.invoice.design"/></content>
                                <content tag="label.for">user.invoiceDesign</content>
                                <g:textField class="field" name="user.invoiceDesign" value="${user?.invoiceDesign}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="prompt.invoice.design"/></content>
                                ${user?.invoiceDesign}
                                <g:hiddenField name="user.invoiceDesign" value="${user?.invoiceDesign}"/>
                            </g:applyLayout>
                        </g:else>
                        </g:preferenceIsNullOrEquals>

                        <g:if test="${ssoActive}">
                            <sec:ifAllGranted roles="USER_158">
                                <g:if test="${availableFields.size()>0 && companyInfoTypes.size() > 0}">
                                    <g:applyLayout name="form/select">
                                        <content tag="label"><g:message code="prompt.idp.configurations"/></content>
                                        <content tag="label.for">idpConfigurationIds</content>
                                        <content tag="include.script">true</content>
                                        <g:select          id = "idp-configuration-select"
                                                         name = "idpConfigurationIds"
                                                         from = "${companyInfoTypes}"
                                                    optionKey = "id"
                                                  optionValue = "name"
                                                        value = "${defaultIdp}"/>
                                    </g:applyLayout>
                                </g:if>
                            </sec:ifAllGranted>
                        </g:if>

                        <!-- customer meta fields -->
                        <g:render template="/metaFields/editMetaFields" model="[ availableFields: availableFields, fieldValues: user?.metaFields ]"/>

                    </div>
                </div>

                <!-- separator -->
                <div class="form-columns">
                    <hr/>
                </div>

                <!-- dynamic balance and invoice delivery -->
                <div class="form-columns">
                    <div class="column">
                    </div>

                    <div class="column">
                    </div>
                </div>

                <!-- spacer -->
                <div>
                    <br/>&nbsp;
                </div>
                <g:if test="${PreferenceBL.getPreferenceValueAsBoolean(session['company_id'], CommonConstants.PREFERENCE_CAPTURE_IDENTIFICATION_DOC_FOR_CUSTOMER)}">
                    <div id="upload_documents" class="box-cards box-cards-open" >
                        <div class="box-cards-title">
                            <a class="btn-open">
                                <span>
                                    <label><g:message code="prompt.upload.document"/></label>
                                </span>
                            </a>
                        </div>
                        <div class="box-card-hold">
                            <g:set var="systemAccountType" value="${new AccountTypeDAS().findAccountTypeByName(session['company_id'],"Default")?.getId()}"/>
                            <g:if test="${accountType?.id == systemAccountType}">
                                <g:if test="${isNew}">
                                    <div class="row">
                                        <g:message code="flash.label.document.mandatory"/>
                                    </div>
                                </g:if>
                            </g:if>
                            <div class="form-columns">
                                <div class="column">

                                        <div class="row">
                                            <g:applyLayout name="form/select">
                                                <content tag="label">
                                                    <g:message code="customer.identification.type"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <content tag="label.for">identificationType</content>
                                                <content tag="include.script">true</content>
                                                <g:select
                                                        id="identificationType"
                                                        name="user.identificationType"
                                                        from="${identificationTypeValues}"
                                                        optionKey=""
                                                        noSelection="['':message(code: 'select.option.default.value.name')]"
                                                        value = "${user?.identificationType}"
                                                        onchange="checkIdentificationType()"/>
                                            </g:applyLayout>
                                        </div>

                                        <div id="national_id" class="row">
                                            <g:applyLayout name="form/input">
                                                <content tag="label">
                                                    <g:message code="label.customer.national.id"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <content tag="label.for">txtNationalId</content>
                                                <g:if test="${user?.identificationType == 'National ID'}">
                                                    <g:textField class="field" id="txtNationalId" name="txtNationalId" value="${user?.identificationText}" onfocusout="checkIdentificationNumber('National ID')"/>
                                                </g:if>
                                                <g:else>
                                                    <g:textField class="field" id="txtNationalId" name="txtNationalId" onfocusout="checkIdentificationNumber('National ID')"/>
                                                </g:else>
                                            </g:applyLayout>

                                            <g:applyLayout name="form/text">
                                                <content tag="label">
                                                    <g:message code="label.customer.national.id.document"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <content tag="label.for">imgNationalId</content>
                                                <input type="file" id="imgNationalId" name="imgNationalId" value="${imgNationalId}" class="submit delete" style="width: 44.15%;" onchange="validateFile('imgNationalId');"/>
                                            </g:applyLayout>

                                        </div>

                                        <div id="passport_id" class="row">
                                            <g:applyLayout name="form/input">
                                                <content tag="label">
                                                    <g:message code="label.customer.passport"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <content tag="label.for">txtPassportId</content>
                                                <g:if test="${user?.identificationType == 'Passport'}">
                                                    <g:textField class="field" id="txtPassportId" name="txtPassportId" value="${user?.identificationText}" onfocusout="checkIdentificationNumber('Passport')"/>
                                                </g:if>
                                                <g:else>
                                                    <g:textField class="field" id="txtPassportId" name="txtPassportId" onfocusout="checkIdentificationNumber('Passport')"/>
                                                </g:else>
                                            </g:applyLayout>

                                            <g:applyLayout name="form/text">
                                                <content tag="label">
                                                    <g:message code="label.customer.passport.document"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <content tag="label.for">imgPassport</content>
                                                <input type="file" id="imgPassport" name="imgPassport" value="${imgPassport}" class="submit delete" style="width: 44.15%;" onchange="validateFile('imgPassport');"/>
                                            </g:applyLayout>
                                        </div>

                                        <div id="company_letter" class="row" style="display:none;">
                                            <g:applyLayout name="form/text">
                                                <content tag="label">
                                                    <g:message code="label.company.letter"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <input type="file"user id="imgCompanyLetter" name="imgCompanyLetter" value="${imgCompanyLetter}" class="submit delete" style="width: 44.15%;" onchange="validateFile('imgCompanyLetter');"/>
                                            </g:applyLayout>
                                        </div>

                                        <div id="official_letter" class="row">
                                            <g:applyLayout name="form/text">
                                                <content tag="label">
                                                    <g:message code="label.customer.official.letter.document"/>
                                                    <span id="mandatory-meta-field">*</span>
                                                </content>
                                                <content tag="label.for">imgOfficialLetter</content>
                                                <input type="file" id="imgOfficialLetter" name="imgOfficialLetter" value="${imgOfficialLetter}" class="submit delete" style="width: 44.15%;" onchange="validateFile('imgOfficialLetter');"/>
                                            </g:applyLayout>
                                        </div>

                                </div>
                                <g:if test="${!isNew}">
                                    <div class="column">
                                        <table class="dataTable table-layout-fixed" cellspacing="0" cellpadding="0">
                                            <tbody>
                                                <g:render template="showCustomerImage" model="[userId:user?.userId, identificationType:oldIdentificationType, caller:'showOnEdit']"/>
                                            </tbody>
                                        </table>
                                    </div>
                                </g:if>
                            </div>
                        </div>
                    </div>
                </g:if>

                <g:hiddenField name="datesXml" value="${datesXml}"/>
                <g:hiddenField name="effectiveDatesXml" value="${effectiveDatesXml}"/>
                <g:hiddenField name="infoFieldsMapXml" value="${infoFieldsMapXml}"/>
                <g:hiddenField name="removedDatesXml" value="${removedDatesXml}"/>

                <g:if test="${accountInformationTypes && accountInformationTypes.size()>0}">
                    <g:each in="${accountInformationTypes}" var="ait">
                    <g:set var="effectiveDate" value="${effectiveDates.get(ait.id)}"/>
                        <div id="ait-${ait.id}" class="box-cards box-cards-open" >
                            <div class="box-cards-title">
                                <a class="btn-open"><span>
                                    ${ait.name}
                                </span></a>
                            </div>
                            <div class="box-card-hold">
                                <g:render template="/customer/timeline" model="[startDate : effectiveDate , pricingDates : pricingDates, aitVal : ait.id]"/>
                                <g:render template="/customer/aITMetaFields" model="[ait : ait , values : user?.metaFields, aitVal : ait.id]"/>
                            </div>
                        </div>
                    </g:each>
                </g:if>

				<!-- Payment Methods -->
				<g:if test="${paymentMethods?.size() > 0}">
					<div id="payment-methods" class="box-cards box-cards-open" >
                        <div class="box-cards-title">
                            <a class="btn-open"><span>
                                <label><g:message code="promt.payment.methods"/></label>
                            </span></a>
                        </div>
                        <div id= "payment-method-main" class="box-card-hold">
                            <g:render template="/customer/paymentMethods" model="[ paymentMethods: paymentMethods, paymentInstruments : user.paymentInstruments, user: user ]"/>
                        </div>
             		</div>
             	</g:if>

                <div id="partner-commission" class="box-cards box-cards-open" >
                    <div class="box-cards-title">
                        <a class="btn-open"><span>
                            <label><g:message code="prompt.customer.commission"/></label>
                        </span></a>
                    </div>
                    <div id= "partner-commission-main" class="box-card-hold">
                        <div class="form-columns">
                            <div class="column">
                                <g:applyLayout name="form/input">
                                    <content tag="label"><g:message code="prompt.customer.partner"/></content>
                                    <content tag="label.for">partner.name</content>
                                    <g:hiddenField name="partner-name-id" />
                                    <g:textField id="partner-name" class="field" name="partner.name" />
                                </g:applyLayout>
                            </div>
                            <div class="column">
                                <g:applyLayout name="form/text">
                                    <content tag="label"><g:message code="prompt.customer.partner.rate"/></content>
                                    <content tag="label.for">partner.rate</content>
                                    <input type="hidden" name="partner-name-id" />
                                    <div class="inp-bg inp4">
                                        <g:textField id="partner-rate" class="field" name="partner.rate" />
                                    </div>
                                    <a class="plus-icon" onclick="addCommission()">
                                        &nbsp;&#xe026;
                                    </a>
                                </g:applyLayout>
                            </div>

                    </div>
                    <!-- spacer -->
                    <div>
                        <br/>&nbsp;
                    </div>

                    <div class="table-box">
                        <table id="commissions" cellspacing="0" cellpadding="0" style="width: auto;margin: auto;">
                            <thead>
                            <tr class="ui-widget-header" >
                            <th class="first" style="width:30px;"><g:message code="customer.commission.partner.id.title"/></th>
                            <th style="width:250px;"><g:message code="customer.commission.partner.title"/></th>
                            <th style="width:100px;"><g:message code="customer.commission.rate.title"/></th>
                            <th class="last" style="width:20px;"></th>
                            </thead>

                            <tbody id="commission-lines">
                            <g:each in="${commissionDefinitions}" status="commIdx" var="it">
                                <tr>
                                    <td>
                                        <input type="hidden" name="comm.partner.${commIdx}" value="${it.partnerId}" />
                                        <input type="hidden" name="comm.rate.${commIdx}" value="${it.rate}" />
                                        <g:link controller="partner" action="show" id="${it.partnerId}">${it.partnerId}</g:link>
                                    </td>
                                    <td>${it.partnerName}</td>
                                    <td>${it.rate.toString().replaceAll('0*$', '').replaceAll('\\.$', '.00')}</td>
                                    <td><a class="plus-icon" onclick="removeCommission(this);">
                                        &#xe000;
                                    </a>
                                    </td>
                                </tr>
                            </g:each>
                            </tbody>
                        </table>
                    </div>
                </div>

				<!-- Notes-->
                <div id="ach" class="box-cards ${customerNotes ? 'box-cards-open' : ''}">
                    <div class="box-cards-title">
                        <a class="btn-open" href="#"><span><g:message code="prompt.notes"/></span></a>
                    </div>
                    <div class="box-card-hold">
                        <div id="users-contain"  >
                            <div class="table-box">
                                <table id="users" cellspacing="0" cellpadding="0">
                                <thead>
                                <tr class="ui-widget-header" >
                                <th class="first" width="150px"><g:message code="customer.detail.note.form.author"/></th>
                                <th width="150px"><g:message code="customer.detail.note.form.createdDate"/></th>
                                <th width="150px"><g:message code="customer.detail.note.form.title"/></th>
                                <th class="last" width="550px"><g:message code="customer.detail.note.form.content"/></th>
                                </thead>
                                <tbody>
                                <g:hiddenField name="newNotesTotal" id="newNotesTotal" />
                                    <g:if test="${customerNotes}">
                                        <g:each in="${customerNotes}">
                                            <tr>
                                                <td>${it?.user.userName}</td>
                                                <td><g:formatDate date="${it?.creationTime}" type="date" style="MEDIUM"/>  </td>
                                                <td>${it?.noteTitle}</td>
                                                <td> ${it?.noteContent}</td>
                                            </tr>
                                        </g:each>
                                    </g:if>
                                    <g:else>
                                        <p id="noResults"><em><g:message code="customer.detail.note.empty.message"/></em></p>
                                    </g:else>
                                </tbody>
                            </table>
                        </div>
                        </div>
                        <div class="form-columns">
                                <div class="btn-box">
                                    <div class="center">
                                        <a onclick="openDialog()" class="submit add"><span><g:message code="button.add.note"/></span></a>
                                    </div>
                                </div>
                            </div>
                        </div> </div>
                      </div>
                    <div class="buttons">
                        <ul>
                            <li>
                                <a onclick = "replacePhoneCountryCodePlusSign($('input[name*=phoneCountryCode]')); $('#user-edit-form').submit()"
                                     class = "submit save button-primary" data-cy="saveChanges">
                                    <span><g:message code="button.save"/></span>
                                </a>
                            </li>
                            <li>
                                <g:link action="list" class="submit cancel" data-cy="cancelButton"><span><g:message code="button.cancel"/></span></g:link>
                            </li>
                        </ul>
                    </div>
                </div>
            </fieldset>
        </g:form>
        <div id="dialog" style="display: none;" title="Alert!"><g:message code="allow.subAccounts.not.removed"/></div>
        <div id="background"></div>
    </div>
</div>

%{-- Template used to add a new commission line --}%
<div style="display: none;">
    <table>
        <tbody id="partner-comm-template">
        <tr>
        <td><input type="hidden" name="comm.partner.val_idx_" value="_id_" />
        <input type="hidden" name="comm.rate.val_idx_" value="_rate_" />
            <g:link controller="partner" action="show" id="_id_">_id_</g:link></td>
        <td>_partner_</td>
        <td>_rate_</td>
        <td><a class="plus-icon" onclick="removeCommission(this);">
            &#xe000;
        </a>
        </td>
    </tr>
        </tbody>
    </table>
</div>
</body>

<r:script>

	var userCodedIdx = ${ucIndex}+1;
	var commissionIdx = ${1 + (customerCommissions != null ? customerCommissions.length : 0)};
	var loggedInUserCodes = [
	    <g:each in="${userCodes}" var="uc" status="tmpIdx">${tmpIdx == 0 ? "" : ","}"${uc}"</g:each>
    ];

	$('.userCode-marker').autocomplete({ source: loggedInUserCodes });

    $( "#partner-name" ).autocomplete({
      source: "<g:createLink controller="customer" action='findPartners'/>" ,
      minLength: 2,
      select: function( event, ui ) {
        //alert('id='+ui.item.label+', val='+ui.item.value)  ;
        $("#partner-name-id").val(ui.item.value);
        $("#partner-name").val(ui.item.label);
        return false;
      }
    });

    function addCommission() {
        if (!$('input[name="partner.name"]').val()) {
            $("#error-messages ul").html("${message(code: 'customer.comission.name.not.entered')}");
        } else if (!$('input[name="partner.rate"]').val()) {
            $("#error-messages ul").html("${message(code: 'customer.comission.rate.not.entered')}");
        } else if(!$("#partner-name-id").val()) {
            $("#error-messages ul").html("${message(code: 'customer.comission.agent.not.valid')}");
        } else {
            commissionIdx++;
            var template = $("#partner-comm-template").clone().html()
                .replace(/_idx_/g, commissionIdx)
                .replace(/_id_/g, $("#partner-name-id").val())
                .replace(/_partner_/g, $("#partner-name").val())
                .replace(/_rate_/g, $("#partner-rate").val());
            $("#commission-lines").append(template);
            $("#partner-name-id").val("");
            $("#partner-name").val("");
            $("#partner-rate").val("");
            return true;
        }

        $("#error-messages ul").show();
        $("#error-messages").show();
        $('html, body').animate({scrollTop: ''}, 'fast');
    }

    function showErrorMessage(errorField) {
        $("#error-messages").css("display","block");
        $("#error-messages ul").css("display","block");
        $("#error-messages ul").html(errorField);
        $("html, body").animate({ scrollTop: 0 }, "slow");
    }

    function validateFile(elementId) {
        var fileInput = document.getElementById(elementId);

        var fileName = fileInput.value;

        // Allowing file type
        var allowedExtensions = /(\.jpg|\.jpeg|\.png|\.pdf)$/i;

        var size = parseFloat(fileInput.files[0].size / 1024).toFixed(2);

        if (!allowedExtensions.exec(fileName)) {
            showErrorMessage("<li><g:message code="error.file.type"/></li>");
            fileInput.value = '';
            return false;
        }
        else if(size>10000000){
            showErrorMessage("<li><g:message code="error.image.size"/></li>");
            fileInput.value = '';
            return false;
        }
    }

    function removeCommission(obj) {
        $(obj).closest("tr").remove();
    }

    $("#partner-rate").keydown(function (e) {
        // Allow: backspace, delete, tab, escape, enter and .
        if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 110, 190]) !== -1 ||
             // Allow: Ctrl+A
            (e.keyCode == 65 && e.ctrlKey === true) ||
             // Allow: Ctrl+C
            (e.keyCode == 67 && e.ctrlKey === true) ||
             // Allow: Ctrl+X
            (e.keyCode == 88 && e.ctrlKey === true) ||
             // Allow: home, end, left, right
            (e.keyCode >= 35 && e.keyCode <= 39)) {
                 // let it happen, don't do anything
                 return;
        }
        // Ensure that it is a number and stop the keypress
        if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105)) {
            e.preventDefault();
        }
    });
</r:script>
</html>