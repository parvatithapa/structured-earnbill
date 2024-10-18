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

<g:if test="${field.filename}">
    <jB:templateExists var="templateExists" template="/metaFields/script/${field.filename}" />

    <g:if test="${templateExists}">
        <g:render template="/metaFields/script/${field.filename}" model="[
                field: field, fieldName: fieldName,
                fieldValue: fieldValue, validationRules: validationRules]"/>
    </g:if>
    <g:else>
        <g:message code="metafield.validation.file.not.exist"
                   args="${["/metaFields/script/_${field.filename}.gsp"]}"/>
    </g:else>
</g:if>
<g:else>
    <g:message code="metafield.validation.filename.not.defined" />
</g:else>