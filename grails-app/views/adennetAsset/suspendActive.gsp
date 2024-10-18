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
    <g:render template="/layouts/includes/messages"/>
    <div class="heading">
        <strong data-cy="suspendActiveTitle">
            <g:message code="product.title.asset.suspension"/>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="save-suspension-form" action="suspendOrActivate" useToken="true" onSubmit="return validateNote()" >

            <fieldset>
                <div class="form-columns">

                    <%-- Base asset details --%>
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="recharge.user.id"/></content>
                             <content tag="label.for">userId</content>
                            <span><g:link controller="customer" action="list"
                                    id="${userId}">${userId}</g:link></span>
                            <g:hiddenField name="userId" value="${userId}"/>
                            <g:hiddenField name="isSuspended" value="${!asset.isSuspended}"/>
                            <g:hiddenField name="id" value="${asset?.id}"/>
                            <g:hiddenField name="identifier" value="${asset?.identifier}"/>

                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="user.subscriber.number"/></content>
                            <content tag="label.for">subscriberNumber</content>
                            ${asset?.subscriberNumber}
                            <g:hiddenField name="subscriberNumber" value="${asset?.subscriberNumber}"/>
                        </g:applyLayout>

                        <div id="childCompanies">
                            <g:hiddenField name="entities" value="${session['company_id']}"/>
                        </div>

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
                            <g:actionSubmit id="btnSuspendActive" class="submit save button-primary"
                                    value="${!asset.isSuspended ? message(code:'button.suspend') : message(code:'button.active')}" />
                        </li>
                        <li>
                            <g:link controller ="customer" action="list" class="submit cancel">
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

    function validateNote(){        
      if($('#notes').val() == "") {
        showErrorMessage("<li><g:message code="suspension.page.note"/></li>");
      } else{
        $('#btnSuspendActive').prop('disabled', true);
        return true;
      }        
      return false;
    }

</r:script>

</html>


