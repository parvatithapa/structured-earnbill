<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
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

<div class="form-edit">

    <div class="heading">
        <strong>
            <g:message code="customer.account.type.title"/>
        </strong>
    </div>

    <g:form name="user-account-select-form" action="edit">
        <fieldset>
            <g:if test="${parentId}">
                <g:hiddenField name="parentId" value="${parentId}"/>
            </g:if>
            <div class="form-columns">
                <div class="column">
                	<g:isRoot>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="prompt.user.company"/></content>
                            <content tag="label.for">user.entityId</content>
                            <input type="text" name="user.entityId" value="${com.sapienter.jbilling.server.user.db.CompanyDTO.get(session['company_id'])?.description}" readonly="true">
                        </g:applyLayout>

                        <div id="account-select">
                        	<g:render template="../customer/accountTypeDropDown" model="[accountTypes: accountTypes]"/>
                        </div>
					</g:isRoot>
					<g:isNotRoot>
						<g:hiddenField name="user.entityId" value="${session['company_id']}"/>
                    	<g:applyLayout name="form/select">
                        	<content tag="label"><g:message code="customer.detail.account.type"/></content>
                        	<content tag="label.for">user.accountTypeId</content>
                        	<g:select name="accountTypeId" from="${accountTypes}"
                                  optionKey="id" optionValue="description" />
                    	</g:applyLayout>
                    </g:isNotRoot>
                </div>
            </div>
            <!-- spacer -->
            <div>
                <br/>&nbsp;
            </div>
        </fieldset>
    </g:form>

    <div class="buttons">
        <div class="btn-row">
            <ul>
                <li>
                    <a onclick="$('#user-account-select-form').submit()" class="submit save">
                        <span><g:message code="button.select"/></span>
                    </a>
                </li>
                <li>
                    <g:link action="list" class="submit cancel">
                        <span><g:message code="button.cancel"/></span>
                    </g:link>
                </li>
            </ul>
        </div>
    </div>
</div>