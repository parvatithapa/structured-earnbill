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
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<%--
  Shows edit form for a contact type.

  @author Brian Cowdery
  @since  27-Jan-2011
--%>

<script language="javascript">
    if ($("#global-checkbox").is(":checked")) {
        $("#company-select").attr('disabled', true);
    }
</script>
<div class="column-hold">
    <g:set var="isNew" value="${!rateCard || !rateCard?.id || rateCard?.id == 0}"/>

    <div class="heading">
        <g:if test="${isNew}">
            <strong><g:message code="rate.card.add.title"/></strong>
        </g:if>
        <g:else>
            <strong><g:message code="rate.card.edit.title"/></strong>
        </g:else>
    </div>

    <g:uploadForm id="rate-card-form" name="rate-card-form" url="[action: 'save']">

    <div class="box">
        <div class="sub-box">
          <fieldset>
            <div class="form-columns">
                <g:hiddenField name="id" value="${rateCard?.id}"/>

                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="rate.card.name"/></content>
                    <content tag="label.for">name</content>
                    <g:textField class="field" name="name" value="${rateCard?.name}"/>
                </g:applyLayout>

                <g:if test="${!isNew}">
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="rate.card.table.name"/></content>
                        <content tag="label.for">tableName</content>
                        <g:textField class="field" name="tableName" value="${rateCard?.tableName}"/>
                    </g:applyLayout>
                </g:if>

                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.csv.file"/></content>
                    <g:applyLayout name="form/fileupload">
                        <content tag="input.name">rates</content>
                    </g:applyLayout>
                </g:applyLayout>


                <g:isRoot>
                    <g:applyLayout name="form/checkbox">
                        <content tag="label"><g:message code="product.assign.global"/></content>
                        <content tag="label.for">global-checkbox</content>
                        <g:checkBox id="global-checkbox" onClick="hideCompanies()" class="cb checkbox" name="global" checked="${rateCard?.global}"/>
                    </g:applyLayout>
                </g:isRoot>
                <g:isNotRoot>
                    <g:hiddenField name="global" value="${rateCard?.global}"/>
                </g:isNotRoot>
              	<div id="childCompanies">

                    <g:isRoot>
                        <g:applyLayout name="form/select_multiple">
                            <content tag="label"><g:message code="product.assign.entities"/></content>
                            <content tag="label.for">childCompanies</content>
                            <%
                                def parent = CompanyDTO.get(session['company_id'])
                                def companies = CompanyDTO.findAllByParent(parent)
                                //Add current company. As per update 16 on #5409
                                companies.add(CompanyDTO.get(session['company_id']))
                            %>
                            <g:select id="company-select" multiple="multiple" name="childCompanies" from="${companies}"
                                      optionKey="id" optionValue="description"
                                      value="${rateCard?.childCompanies*.id}"/>
                        </g:applyLayout>
                    </g:isRoot>
                    <g:isNotRoot>
                        <g:if test="${rateCard?.childCompanies?.size() > 0}">
                            <g:each in="${rateCard?.childCompanies}">
                                <g:hiddenField name="childCompanies" value="${it?.id}"/>
                            </g:each>
                        </g:if>
                        <g:else>
                            <g:hiddenField name="childCompanies"  value="${session['company_id']}"/>
                        </g:else>
                    </g:isNotRoot>
            	</div>
                        
                <g:applyLayout name="form/text">
                    <content tag="label">&nbsp;</content>
                    <g:link controller="rateCard" action="downloadExampleCSV">example_rate_card.csv</g:link>
                </g:applyLayout>
            </div>
        </fieldset>
      </div>

        <div class="btn-box buttons">
            <ul>
                <li><a class="submit save button-primary" onclick="$('#rate-card-form').submit();"><span><g:message code="button.save"/></span></a></li>
                <li><a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a></li>
            </ul>
        </div>
    </div>

    </g:uploadForm>
</div>
