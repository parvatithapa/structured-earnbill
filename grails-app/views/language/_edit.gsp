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

<%--
  Shows edit form for a contact type.

  @author Neeraj Bhatt
  @since  10-June-2014
--%>
<%@ page import="com.sapienter.jbilling.server.util.db.CountryDTO"%>
<div class="column-hold">
    <div class="heading">
        <strong><g:message code="language.${language?.id ? 'edit' : 'add'}.title"/></strong>
    </div>

    <g:form name="language-form" id="language-form" url="[action: 'save']" >

    <div class="box">
        <div class="sub-box">
          <fieldset>
            <g:hiddenField name="id" value="${language?.id}"/>
            <div class="form-columns">
                <g:applyLayout name="form/input">
                    <content tag="label">
                        <g:message code="language.label.code"/> <span style="color: red">*</span>
                    </content>
                    <content tag="label.for">code</content>
                    <g:textField class="field" name="code" value="${language?.code}"/>
                </g:applyLayout>

                <g:applyLayout name="form/input">
                    <content tag="label">
                        <g:message code="language.label.description"/> <span style="color: red">*</span>
                    </content>
                    <content tag="label.for">description</content>
                    <g:textField class="field" name="description" value="${language?.description}"/>
                </g:applyLayout>

                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="prompt.country"/></content>
                    <content tag="label.for">countryCode</content>
                    <content tag="include.script">true</content>
                    <g:select        name = "countryCode"
                                     from = "${CountryDTO.list()}"
                                optionKey = "code"
                              optionValue = "${{ it.getDescription(session['language_id']) }}"
                              noSelection = "['': message(code: 'default.no.selection')]"
                                    value = "${language?.countryCode}"/>
               </g:applyLayout>
            </div>
        </fieldset>
      </div>

        <div class="btn-box buttons">
            <ul>
                <li><a class="submit save" onclick="$('#language-form').submit();">
                    <span><g:message code="button.save"/></span></a>
                </li>
                <li><g:link controller="language" action="list" class="submit cancel" params="[id:language?.id]">
                    <span><g:message code="button.cancel"/></span></g:link>
                </li>
            </ul>
        </div>
    </div>
    </g:form>
</div>
