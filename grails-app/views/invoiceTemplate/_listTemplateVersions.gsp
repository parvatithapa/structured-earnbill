<%@ page import="org.apache.commons.lang.StringEscapeUtils; org.apache.commons.lang.StringUtils" %>
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

<%--
  Shows template versions.

  @author Prashant Gupta
  @since  23-Jan-2015
--%>

<div class="table-box">
    <div class="table-scroll">
        <table id="templateVersions" cellspacing="0" cellpadding="0">
            <thead>
            <tr>
                <th>
                    <g:remoteSort action="listVersions" id="${selectedTemplate?.id}" sort="versionNumber" update="column2">
                        <g:message code="invoiceTemplateVersion.versionNumber.details"/>
                    </g:remoteSort>
                </th>
                <th>
                    <g:remoteSort action="listVersions" id="${selectedTemplate?.id}" sort="tagName" update="column2">
                        <g:message code="invoiceTemplateVersion.tagName.details"/>
                    </g:remoteSort>
                </th>
                <th>
                    <g:remoteSort action="listVersions" id="${selectedTemplate?.id}" sort="createdDatetime" update="column2">
                        <g:message code="invoiceTemplateVersion.createdDatetime.details"/>
                    </g:remoteSort>
                </th>
                <th >
                    <g:message code="invoiceTemplate.useForInvoice.label.details"/>
                </th>
                <th >
                    <g:message code="invoiceTemplateVersion.label.includeCarriedInvoiceLines.details"/>
                </th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${invoiceTemplateVersions}" var="templateVersion">
                <tr class="${templateVersion.id == selectedTemplateVersion?.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="showVersion" params="[template:'showTemplateVersion']" id="${templateVersion.id}"
                                      before="register(this);" onSuccess="showVersion(data, next);">
                            <strong>${StringUtils.abbreviate(templateVersion.versionNumber, 45).encodeAsHTML()}</strong>
                            <em><g:message code="table.id.format" args="[templateVersion.id as String]"/></em>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="showVersion" params="[template:'showTemplateVersion']" id="${templateVersion.id}"
                                      before="register(this);" onSuccess="showVersion(data, next);">
                            ${StringEscapeUtils.escapeHtml(templateVersion.tagName)}
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="showVersion" params="[template:'showTemplateVersion']" id="${templateVersion.id}"
                                      before="register(this);" onSuccess="showVersion(data, next);">
                            <g:formatDate date="${templateVersion.createdDatetime}" formatName="date.time.format"/>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="showVersion" params="[template:'showTemplateVersion']" id="${templateVersion.id}"
                                      before="register(this);" onSuccess="showVersion(data, next);">
                            ${templateVersion.useForInvoice? g.message(code: 'default.boolean.true') : g.message(code: 'default.boolean.false')}
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="showVersion" params="[template:'showTemplateVersion']" id="${templateVersion.id}"
                                      before="register(this);" onSuccess="showVersion(data, next);">
                            ${templateVersion.includeCarriedInvoiceLines? g.message(code: 'default.boolean.true') : g.message(code: 'default.boolean.false')}
                        </g:remoteLink>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
        <div class="pager-box">
            <div class="results">
                <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], action: 'listVersions', update: 'column1',id: selectedTemplate?.id]"/>
            </div>
            <div class="row-center">
                <jB:remotePaginate  controller="invoiceTemplate" action="listVersions" id="${selectedTemplate?.id}" total="${versionsTotalCount}" onSuccess="closePanel(\'#column2\');" update="column1"/>
            </div>
        </div>
    </div>
    <g:if test="${showDeleteButton}">
        <div class="btn-hold">
            <div class="btn-box">
                <div class="row">
                    <sec:ifAllGranted roles="INVOICE_TEMPLATES_1804">
                        <a onclick="showConfirm('deleteTemplate-${selectedTemplate?.id}');" id="firstDeleteButton" class="submit delete"><span><g:message code="button.delete"/></span></a>
                    </sec:ifAllGranted>
                </div>
            </div>
        </div>
    </g:if>
    <g:render template="/confirm"
              model="[message   : 'invoiceTemplate.prompt.are.you.sure',
                      controller: 'invoiceTemplate',
                      action    : 'deleteTemplate',
                      id        : selectedTemplate?.id,
              ]"/>
</div>
<script>
    function showVersion(data, next) {
        render(data,next);
        $('#firstDeleteButton').hide();
    }
</script>