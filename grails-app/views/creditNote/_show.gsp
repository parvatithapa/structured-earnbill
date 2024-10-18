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

<%@ page import="com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.user.contact.db.ContactDTO
;com.sapienter.jbilling.server.creditnote.db.CreditType" %>

<%--
  Shows details of a selected credit note.

  @author Usman Malik
  @since 24-July-2015
--%>


<div class="column-hold">
    <div class="heading">
        <strong>
                <g:message code="creditNote.credit.title"/>
            <em>${selected.id}</em>
            <g:if test="${selected.deleted}">
                <span style="color: #ff0000;">(<g:message code="object.deleted.title"/>)</span>
            </g:if>
        </strong>
    </div>

    <div class="box">
      <div class="sub-box">
        <!-- user details -->
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>

                <tr>
                    <td><g:message code="payment.user.id"/></td>
                    <td class="value">
                        <jB:secRemoteLink controller="customer" action="show" id="${selected.creationInvoice.baseUser.id}" before="register(this);" onSuccess="render(data, next);">
                            ${selected.creationInvoice.baseUser.id}
                        </jB:secRemoteLink>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="payment.label.user.name"/></td>
                    <td class="value">${displayer?.getDisplayName(selected.creationInvoice.baseUser)}</td>
                </tr>
            </tbody>
        </table>

        <!-- payment details -->
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="creditNote.date"/></td>
                    <td class="value"><g:formatDate date="${selected.createDateTime}"/></td>
                </tr>
                <tr>
                    <td><g:message code="creditNote.amount"/></td>
                    <td class="value"><g:formatNumber number="${selected.amount}" type="currency" currencySymbol="${selected.currencySymbol}"/></td>
                </tr>
            <tr>
                <td><g:message code="creditNote.type"/></td>
                <td class="value">${selected.creditType.getTypeLabel()}</td>
            </tr>
            <tr>
                <g:if test="${CreditType.AUTO_GENERATED == selected.creditType}">
                    <td><g:message code="creditNote.generated.invoice.id"/></td>
                </g:if>
                <g:else>
                    <td><g:message code="invoice.label.id"/></td>
                </g:else>
                <td class="value">
                        <jB:secRemoteLink controller="invoice" action="show" id="${selected.creationInvoice.id}" before="register(this);" onSuccess="render(data, next);">
                            ${selected.creationInvoice.id}
                        </jB:secRemoteLink>
                </td>
            </tr>
            </tbody>
        </table>

        <hr/>

        <!-- payment balance & meta fields -->
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
                <tr>
                    <td><g:message code="creditNote.id"/></td>
                    <td class="value">${selected.id}</td>
                </tr>

                <tr>
                    <td><g:message code="payment.balance"/></td>
                    <td class="value">
                        <g:formatNumber number="${selected.balance}" type="currency" currencySymbol="${selected.currencySymbol}"/>
                    </td>
                </tr>



            </tbody>
        </table>
        <!-- list of linked invoices -->
        <g:if test="${selected.paidInvoices}">
            <g:hiddenField name="unlink_invoice_id" value="-1"/>
            <table cellpadding="0" cellspacing="0" class="innerTable">
                <thead class="innerHeader">
                    <tr>
                        <th class="first"><g:message code="payment.invoice.payment"/></th>
                        <th><g:message code="payment.invoice.payment.amount"/></th>
                        <th><g:message code="payment.invoice.payment.date"/></th>
                        <th class="last"><!-- action --> &nbsp;</th>
                    </tr>
                </thead>
                <tbody>
                    <g:each var="invoicePayment" in="${selected.paidInvoices}">
                    <tr>
                        <td class="innerContent">
                            <jB:secRemoteLink controller="invoice" action="show" id="${invoicePayment.invoiceEntity.id}" before="register(this);" onSuccess="render(data, next);">
                                <g:message code="payment.link.invoice" args="[invoicePayment.invoiceEntity.number]"/>
                            </jB:secRemoteLink>
                        </td>
                        <td class="innerContent">
                            <g:formatNumber number="${invoicePayment.amount}" type="currency" currencySymbol="${selected.currencySymbol}"/>
                        </td>
                        <td class="innerContent">
                            <g:formatDate date="${invoicePayment.createDatetime}"/>
                        </td>
                        <td class="innerContent">
                            <sec:access url="/payment/unlink">
                                <a onclick="setUnlinkInvoiceId(${selected.id}, ${invoicePayment.invoiceEntity.id});">
                                    <span><g:message code="payment.link.unlink"/></span>
                                </a>
                            </sec:access>
                        </td>
                    </tr>
                    </g:each>
                </tbody>
            </table>
        </g:if>
          <hr/>
          <g:if test="${selected.lines}">
              <table cellpadding="0" cellspacing="0" class="innerTable">
                  <thead class="innerHeader">
                  <tr>
                      <th class="first"><g:message code="ui.label.credit.detail.invoice.line"/></th>
                      <th class="last"><!-- action --> &nbsp;</th>
                  </tr>
                  </thead>
                  <tbody>
                  <g:each var="line" in="${selected.lines}">
                      <tr>
                          <td class="innerContent">
                              ${line?.description}
                          </td>
                      </tr>
                  </g:each>
                  </tbody>
              </table>
          </g:if>
     </div>
    </div>



    <div class="btn-box">
        <g:if test="${!selected.deleted}">
            <!-- edit or delete unlinked payments -->
            <div class="row">
                <sec:access url="/invoice/list">
                    <g:link controller="invoice" action="user" id="${selected.creationInvoice.baseUser.id}" class="submit edit">
                        <span><g:message code="customer.show.all.invoices"/></span>
                    </g:link>
                </sec:access>
                <g:if test="${!selected.paidInvoices && CreditType.AUTO_GENERATED != selected.creditType }">
                    <sec:ifAllGranted roles="PAYMENT_31">

                            <g:link action="edit" id="${selected.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>

                    </sec:ifAllGranted>
                        <sec:ifAllGranted roles="PAYMENT_32">
                            <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
                        </sec:ifAllGranted>
                </g:if>
                <g:else>
                    <em><g:message code="creditNote.cant.edit.linked"/></em>
                </g:else>
            </div>
        </g:if>
    </div>
</div>
<script type="text/javascript">
    function setUnlinkInvoiceId(paymentId, invoiceId) {
        $('#unlink_invoice_id').val(invoiceId);
        showConfirm("unlink-" + paymentId);
        return true;
    }
    function setInvoiceId() {
        $('#confirm-command-form-unlink-${selected.id} [name=invoiceId]').val($('#unlink_invoice_id').val());
    }
</script>

<g:render template="/confirm"
          model="[message: 'creditNote.prompt.confirm.remove.creditNote.link',
                  controller: 'creditNote',
                  action: 'unlink',
                  id: selected.id,
                  formParams: [ 'invoiceId': '-1' ],
                  onYes: 'setInvoiceId()',
          ]"/>

<g:render template="/confirm"
          model="[message: 'creditNote.delete.confirm',
                  controller: 'creditNote',
                  action: 'delete',
                  id: selected.id,
          ]"/>

