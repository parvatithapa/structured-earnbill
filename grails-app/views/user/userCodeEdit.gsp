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

<%--
  Form for editing an User Code

 @author Gerhard Maree
 @since  18-Apr-2013
--%>

<html>
<head>
    <meta name="layout" content="main"/>
</head>

<body>
<div class="form-edit">

    <g:set var="isNew" value="${!userCode || !userCode?.id || userCode?.id == 0}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="userCode.add.title"/>
            </g:if>
            <g:else>
                <g:message code="userCode.edit.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="save-userCode-form" action="userCodeSave" useToken="true">
            <fieldset>
                <div class="form-columns">

                    <%-- Base asset details --%>
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="userCode.detail.id"/></content>

                            <g:if test="${isNew}"><em><g:message code="prompt.id.new"/></em></g:if>
                            <g:else>${userCode?.id}</g:else>

                            <g:hiddenField name="id" value="${userCode?.id}"/>
                            <g:hiddenField name="userId" value="${user?.id}"/>
                            <g:hiddenField name="userName" value="${user?.userName}"/>
                        </g:applyLayout>

                        <g:if test="${isNew || isEditable}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="userCode.detail.identifier" /></content>
                                <content tag="label.for">identifier</content>
                                ${user?.userName}<g:textField name="identifier" value="${userCode?.identifier?.substring(user.userName.length())}" style="width: 50px"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="userCode.detail.identifier"/></content>${userCode?.identifier}
                            </g:applyLayout>
                            <g:hiddenField name="identifier" value="${userCode?.identifier?.substring(user.userName.length())}"/>
                        </g:else>

                        <g:if test="${isNew || isEditable}">
                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="userCode.detail.externalReference" /></content>
                                <content tag="label.for">externalReference</content>
                                <g:textField class="field" name="externalReference" value="${userCode?.externalReference}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="userCode.detail.externalReference"/></content>${userCode?.externalReference}
                            </g:applyLayout>
                            <g:hiddenField name="externalReference" value="${userCode?.externalReference}"/>
                        </g:else>

                        <g:if test="${isNew || isEditable}">
                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="userCode.detail.type" /></content>
                                <content tag="label.for">identifier</content>
                                <g:select name="type" from="${types}"
                                          value="${userCode?.type}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="userCode.detail.type"/></content>${userCode?.type}
                            </g:applyLayout>
                            <g:hiddenField name="type" value="${userCode?.type}"/>
                        </g:else>


                        <g:if test="${isNew || isEditable}">
                            <g:applyLayout name="form/textarea">
                                <content tag="label"><g:message code="userCode.detail.typeDescription" /></content>
                                <content tag="label.for">typeDescriptoin</content>
                                <g:textArea class="narrow" name="typeDescription" value="${userCode?.typeDescription}" rows="5" cols="45"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="userCode.detail.typeDescription"/></content>${userCode?.typeDescription}
                            </g:applyLayout>
                            <g:hiddenField name="typeDescription" value="${userCode?.typeDescription}"/>
                        </g:else>

                    </div>

                    <div class="column">
                        <g:if test="${isNew || isEditable}">
                            <g:applyLayout name="form/date">
                                <content tag="label"><g:message code="userCode.detail.validFrom"/></content>
                                <content tag="label.for">validFrom</content>
                                <g:textField class="field" name="validFrom"
                                             value="${formatDate(date: userCode?.validFrom, formatName: 'datepicker.format')}"/>
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="userCode.detail.validFrom"/></content><g:formatDate formatName="date.pretty.format" date="${userCode?.validFrom}"/>
                            </g:applyLayout>
                            <g:hiddenField name="validFrom" value="${formatDate(date: userCode?.validFrom, formatName: 'datepicker.format')}"/>
                        </g:else>

                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="userCode.detail.validTo"/></content>
                            <content tag="label.for">validTo</content>
                            <g:textField class="field" name="validTo"
                                         value="${formatDate(date: userCode?.validTo, formatName: 'datepicker.format')}"/>
                        </g:applyLayout>

                    </div>
                </div>

                <div>
                    <br/>&nbsp;
                </div>

                <div class="buttons">
                    <ul>
                        <li><a onclick="$('#save-userCode-form').submit();" class="submit save button-primary"><span><g:message
                                code="button.save"/></span></a></li>
                        <li><g:link action="userCodeList" id="${user.id}" class="submit cancel"><span><g:message
                                code="button.cancel"/></span></g:link></li>
                    </ul>
                </div>
            </fieldset>
        </g:form>
    </div>

</div>

</body>

</html>
