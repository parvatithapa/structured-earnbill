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

<%@ page contentType="text/html;charset=UTF-8" %>

<div id="schemeModel">
<div class="column-hold">

    <div class="heading">
        <strong>

                <g:message code="apiUserDetails.add.title"/>

        </strong>
    </div>

    <g:form id="save-unit-form" name="unit-form" url="[action: 'save']" >

        <div class="box">
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="api.user.name"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">userName</content>
                            <g:textField class="field" name="userName" value=""/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="api.user.password"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">password</content>
                            <g:field type="password" class="field" name="password" value=""/>
                        </g:applyLayout>

                    </div>
                </fieldset>
            </div>
        </div>

    </g:form>

    <div class="btn-box">
        <a class="submit save button-primary" onclick="$('#save-unit-form').submit();"><span><g:message code="button.save"/></span></a>
        <a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a>
    </div>

</div>
</div>