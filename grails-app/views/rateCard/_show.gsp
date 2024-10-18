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
  Shows a contact type.

  @author Brian Cowdery
  @since  27-Jan-2011
--%>

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.name}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <fieldset>
            <div class="form-columns">
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.name"/></content>
                    ${selected.name}
                </g:applyLayout>
				
				<g:isRoot>
                       <g:applyLayout name="form/text">
                    		<content tag="label"><g:message code="customer.detail.user.company"/></content>
                    		<%
					     	def totalChilds = selected?.childCompanies?.size()
					     	def multiple = false
							if(totalChilds > 1) {
								multiple = true
							}
							%>
                        	<g:if test="${selected?.global}">
                        		<g:message code="product.label.company.global"/>
                        	</g:if>
                        	<g:elseif test="${multiple}">
                        		<g:message code="product.label.company.multiple"/>
                       		</g:elseif>
                        	<g:elseif test="${totalChilds==1}">
                       			${selected?.childCompanies?.toArray()[0]?.description}
                       		</g:elseif>
                       		<g:else>
                                <strong>-</strong>
                       		</g:else>
                	</g:applyLayout>
                </g:isRoot>
                
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.table.name"/></content>
                    ${selected.tableName}
                </g:applyLayout>
				
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.csv.file"/></content>
                    <g:link action="csv" id="${selected.id}">
                        ${selected.tableName}.csv
                    </g:link>
                </g:applyLayout>
            </div>
        </fieldset>
      </div>
    </div>


    <div class="btn-box">
        <div class="row">
            <g:link action="rates" id="${selected.id}" class="submit show">
                <span><g:message code="button.view.rates"/></span>
            </g:link>
        </div>
        <div class="row">
            <g:if test="${selected?.company?.id == session['company_id']}">
                <g:remoteLink action="edit" id="${selected.id}" class="submit edit" before="register(this);" onSuccess="render(data, second);">
                    <span><g:message code="button.edit"/></span>
                </g:remoteLink>
                <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </g:if>
        </div>
    </div>

    <g:render template="/confirm"
              model="['message': 'rate.card.delete.confirm',
                      'controller': 'rateCard',
                      'action': 'delete',
                      'id': selected.id,
                     ]"/>
</div>