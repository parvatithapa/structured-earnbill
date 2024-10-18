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
  Shows user role.

  @author Brian Cowdery
  @since  02-Jun-2011
--%>

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.name}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="invoiceTemplate.label.id"/></td>
                    <td class="value">${selected.id}</td>
                </tr>
                <tr>
                    <td><g:message code="invoiceTemplate.label.description"/></td>
                    %{--<td class="value">${selected.getDescription(session['language_id'])}</td>--}%
                    <td class="value">${selected.name}</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>

    <div class="btn-box">
        <div class="row">
            <sec:ifAllGranted roles="INVOICE_TEMPLATES_1801">
                <g:link action="edit" id="${selected.id}" class="submit edit button-primary"><span><g:message code="button.edit"/></span></g:link>
            </sec:ifAllGranted>
            <sec:ifAllGranted roles="INVOICE_TEMPLATES_1804">
                <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </sec:ifAllGranted>
            <a href="${createLink (action: 'json', id: selected.id)}" class="submit save">
                <span><g:message code="button.invoiceTemplate.downloadJson"/></span>
            </a>
            <sec:ifAllGranted roles="INVOICE_TEMPLATES_1802">
                <g:remoteLink action="create" class="submit add" params="[srcId: selected.id]" update="column2">
                    <span><g:message code="button.clone"/></span>
                </g:remoteLink>
            </sec:ifAllGranted>
        </div>
    </div>

    <g:render template="/confirm"
              model="[message   : 'invoiceTemplate.prompt.are.you.sure',
                      controller: 'invoiceTemplate',
                      action    : 'delete',
                      id        : selected.id,
              ]"/>
</div>
