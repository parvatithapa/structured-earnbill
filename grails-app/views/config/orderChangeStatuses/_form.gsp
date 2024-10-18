<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO" %>
%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<div class="form-edit">

    <div class="heading">
        <strong><g:message code="configuration.title.orderChangeStatuses"/></strong>
    </div>
    <div class="form-hold">
        <div class="form-columns single" style="padding-bottom: 15px">
            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="configuration.orderChangeStatuses.edit.for.language"/></content>
                <content tag="label.for">filterStatusId</content>
                <g:select id="language_selector"
                        from="${languages}"
                        optionKey="id" optionValue="description"
                        name="languageId"
                        onchange="onLanguageChange(this);"
                        value="${com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID}"/>
            </g:applyLayout>
        </div>
    </div>

    <g:form name="save-orderChangeStatuses-form" action="saveOrderChangeStatuses" useToken="true">
        <g:render template="/config/orderChangeStatuses/statuses" model="[statuses: statuses, languages: languages]"/>
    </g:form>

    <g:javascript>
        function onLanguageChange(selector) {
            var languageId = $(selector).val();
            $("div[class^='lang_description']").hide();
            $('div.lang_description_' + languageId).show();
            if (languageId != ${com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID}) {
                $('div.lang_description_' + ${com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID}).show();
            }
        }
    </g:javascript>



</div>