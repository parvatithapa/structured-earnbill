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
  Shows edit form for a route.

  @author Rahul Asthana

--%>
<div class="column-hold">
    <g:set var="isNew" value="${!route || !route?.id || route?.id == 0}"/>

    <div class="heading">
        <g:if test="${isNew}">
            <strong><g:message code="route.add.title"/></strong>
        </g:if>
        <g:else>
            <strong><g:message code="route.edit.title"/></strong>
        </g:else>
    </div>
    <g:uploadForm id="route-form" name="route-form" url="[action: 'save']">
        <div class="box">
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">
                        <g:hiddenField name="id" value="${route?.id}"/>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="route.name"/><span id="mandatory-meta-field">*</span>  </content>
                            <content tag="label.for">name</content>
                            <g:textField class="field" name="name" value="${route?.name}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="route.output.field.name"/></content>
                            <content tag="label.for">outputFieldName</content>
                            <g:textField class="field" name="outputFieldName" value="${route?.outputFieldName}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="route.default.next.route"/></content>
                            <content tag="label.for">defaultRoute</content>
                            <g:textField class="field" name="defaultRoute" value="${route?.defaultRoute}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="route.csv.file"/><g:if test="${isNew}"><span id="mandatory-meta-field">*</span></g:if></content>
                            <g:applyLayout name="form/fileupload">
                                <content tag="input.name">routes_file</content>
                            </g:applyLayout>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="route.root.table"/></content>
                            <content tag="label.for">rootTable</content>
                            <g:checkBox class="cb checkbox" name="rootTable" checked="${route?.rootTable}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="route.route.table"/></content>
                            <content tag="label.for">routeTable</content>
                            <g:checkBox class="cb checkbox" name="routeTable" checked="${route?.routeTable}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label">&nbsp;</content>
                            <g:link controller="route" action="downloadExampleCSV">example_route.csv</g:link>
                        </g:applyLayout>
                    </div>
                </fieldset>
            </div>
            <div class="btn-box buttons">
                <ul>
                    <li><a class="submit save button-primary" onclick="$('#route-form').submit();"><span><g:message code="button.save"/></span></a></li>
                    <g:if test="${!isNew}">
                        <li>
                            <g:remoteLink action="show" class="submit cancel"
                                          params="[id: route.id]" update="column2">
                                <span><g:message code="button.cancel"/></span>
                            </g:remoteLink>
                        </li>
                    </g:if>
                </ul>
            </div>
        </div>
    </g:uploadForm>

</div>
