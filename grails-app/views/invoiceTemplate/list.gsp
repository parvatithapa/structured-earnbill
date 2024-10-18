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

<html>
<head>
    <meta name="layout" content="configuration"/>
</head>

<body>
<!-- selected configuration menu item -->
<content tag="menu.item">invoiceTemplates</content>
<g:if test="${selectedTemplateVersion}">
    <content tag="column1">
        <g:render template="listTemplateVersions" model="[invoiceTemplateVersions: invoiceTemplateVersions,selectedTemplateVersion:selectedTemplateVersion,
                                                          selectedTemplate: selectedTemplate , versionsTotalCount: versionsTotalCount]"/>
    </content>

    <content tag="column2">
        <sec:ifAllGranted roles="INVOICE_TEMPLATES_152">
            <g:render template="showTemplateVersion" model="[selected: selectedTemplateVersion]"/>
        </sec:ifAllGranted>
    </content>
</g:if>
<g:else>
    <content tag="column1">
        <g:render template="invoiceTemplates" model="[invoiceTemplates: invoiceTemplates, selectedInvoiceTemplates: selectedInvoiceTemplate]"/>
    </content>

    <content tag="column2">
        <g:if test="${selected}">
            <sec:ifAllGranted roles="INVOICE_TEMPLATES_152">
                <g:render template="show" model="[selected: selected]"/>
            </sec:ifAllGranted>
        </g:if>
        <g:elseif test="${invoiceTemplate}">
            <sec:ifAllGranted roles="INVOICE_TEMPLATES_151">
                <g:render template="create" model="[invoiceTemplate: invoiceTemplate]"/>
            </sec:ifAllGranted>
        </g:elseif>
    </content>
</g:else>

</body>
</html>
