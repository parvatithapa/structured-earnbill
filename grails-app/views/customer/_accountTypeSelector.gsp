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

    <div class="form-hold">
    <g:form name="user-account-select-form" action="edit">
        <fieldset>
            <g:if test="${parentId}">
                <g:hiddenField name="parentId" value="${parentId}"/>
            </g:if>
            <div class="form-columns">
                <div class="column">
                	<g:isRoot>
                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="prompt.user.company"/></content>
                            <content tag="label.for">user.entityId</content>
                            <g:select name="user.entityId"
                                      from="${companies}"
                                      optionKey="id"
                                      optionValue="${{it?.description}}"
                                      value="${session['company_id']}"
                                      onChange="${remoteFunction(controller: 'customer', action: 'getAccountTypes',
                  									update: 'account-select',
                  									params: '\'user.entityId=\' + this.value')}" />
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
        <ul>
            <li>
                <a onclick="$('#user-account-select-form').submit()" class="submit save button-primary">
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
