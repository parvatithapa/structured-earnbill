%{--
 SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 _____________________

 [2024] Sarathi Softech Pvt. Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is and remains
 the property of Sarathi Softech.
 The intellectual and technical concepts contained
 herein are proprietary to Sarathi Softech
 and are protected by IP copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
--}%

<html>
<head>
    <meta name="layout" content="main"/>
</head>

<body>
<div id="dialog-confirm" class="bg-lightbox" style="display: flex;" title="<g:message code="popup.continue.title"/>">
</div>
    <g:render template="/layouts/includes/messages"/>
    <div class="heading">
        <strong data-cy="simReissueTitle">
            <g:message code="product.asset.reissue.sim"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="save-reissue-form" action="reissueSim" useToken="true" onSubmit="return validateForm()" >

            <fieldset>
                <div class="form-columns">

                    <%-- Base asset details --%>
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="recharge.user.id"/></content>
                            <span><g:link controller="customer" action="list"
                                   id="${user.id}">${user.id}</g:link></span>
                            <g:hiddenField name="userId" value="${user.id}"/>

                            <g:hiddenField name="isSuspended" value="${asset.isSuspended}"/>
                            <g:hiddenField name="id" value="${asset?.id}"/>
                            <g:hiddenField name="identifier" value="${asset?.identifier}"/>

                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="user.subscriber.number"/></content>
                            <content tag="label.for"><g:message code="user.subscriber.number"/></content>
                            ${asset?.subscriberNumber}
                            <g:hiddenField name="subscriberNumber" value="${asset?.subscriberNumber}"/>
                        </g:applyLayout>

                        <div id="childCompanies">
                            <g:hiddenField name="entities" value="${session['company_id']}"/>
                        </div>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="label.adennet.iccid"/></content>
                            <content tag="label.for"><g:message code="label.adennet.iccid"/></content>
                            <g:textField class="field" id="newIdentifier" name="newIdentifier" value="" onfocusout="checkReissueNumber()"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="reissue.fee"/></content>
                            <content tag="label.for"><g:message code="reissue.fee"/></content>
                            <g:hiddenField  name="reissueFee" id="reissueFee" value="${simReissueFee}"/>
                             ${simReissueFee}
                        </g:applyLayout>
                    </div>

                    <div class="column">
                        <g:applyLayout name="form/textarea">
                            <content tag="label"><g:message code="asset.detail.notes"/><span id="mandatory-meta-field"> *</span></content>
                            <content tag="label.for">notes</content>
                            <g:textArea class="narrow" id ="notes" name="notes" value="${asset.notes}" rows="5" cols="45"/>
                        </g:applyLayout>
                    </div>
                </div>

                <div>
                    <br/>&nbsp;
                </div>

                <div class="buttons">
                    <ul>
                        <li>
                           <g:actionSubmit id="btnReissue" class="submit save button-primary" value="${message(code:'button.reissue')}"/>
                         </li>
                        <li>
                            <g:link controller ="customer" action="list" class="submit cancel" data-cy="cancelButton">
                            <span><g:message code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>
            </fieldset>
        </g:form>
    </div>

</body>
<r:script >

    function showErrorMessage(errorField) {
            $("#error-messages").css("display","block");
            $("#error-messages ul").css("display","block");
            $("#error-messages ul").html(errorField);
            $("html, body").animate({ scrollTop: 0 }, "slow");
    }

    function validateForm() {
        if(!$('#newIdentifier').val().match(/^89967055\d{12}$/)) {
            showErrorMessage("<li><g:message code="validation.error.invalid.adennet.username"/></li>");
        } else if($('#notes').val() == "") {
            showErrorMessage("<li><g:message code="suspension.page.note"/></li>");
        } else {
            $('#btnReissue').prop('disabled', true);
            return true;
        }
        return false;
    }

    function parseNumber(number) {
            if (number === undefined || number === null || number === '' || isNaN(number) || number < 0) {
                return undefined;
            }
        return number;
        }

        $( document ).ready(function() {
            var reissueDate = new Date('${user.reissueDate}');
            var previousDate = new Date();
                previousDate.setMonth(previousDate.getMonth()-${reissueDuration});
            var currentDate = new Date();
            var message ='<g:message code="popup.count.reissue.sim" args="[asset?.subscriberNumber , user?.reissueCount]"/> ' ;

                if(reissueDate.valueOf() >= previousDate.valueOf() && reissueDate.valueOf() <= currentDate.valueOf() && ${user?.reissueCount >= reissueCountLimit }){

                       $('#dialog-confirm').html('<div><span class="ui-icon ui-icon-alert" id="customer-dialog-icon" style="margin:12px 12px 20px 12px;"></span></div><div> '+message+' </div>');
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
                                       document.querySelector("#save-reissue-form> fieldset > div.buttons > ul > li:nth-child(2) > a").click();
                                   }
                               }
                       });
                }

        });

         function checkReissueNumber(){

             var newIdentifier =  $('#newIdentifier').val() ;
                $.ajax({
                     url: '${createLink(action: 'checkReissueAssetIsValid')}',
                     data: { identifier : newIdentifier },
                         success: function(data) {
                         if(data != 'true'){
                            $("html, body").animate({ scrollTop: 0 }, "slow");
                            $('#btnReissue').prop('disabled', true);
                         }else{
                            $('#btnReissue').prop('disabled', false);
                         }
                     }
                });
         }

</r:script>

</html>


