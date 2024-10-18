%{--
  SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
  _____________________

  [2024] Sarathi Softech Pvt. Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Sarathi Softech.
  The intellectual and technical concepts contained
  herein are proprietary to Sarathi Softech.
  and are protected by IP copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.paymentUrl.db.Status" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%--
	Show Payment URL links Template.

	@author Thapa Parvati
	@since 01-Jun-2024
 --%>

<g:if test="${!paymentUrlLogs?.isEmpty()}">
    <g:set var="currency" value="${new com.sapienter.jbilling.server.util.db.CurrencyDAS().find(session['company_id'])}"/>
    <table class="innerTable" id="paymentUrlLogTable">
        <thead class="innerHeader">
            <tr>
                <th><g:message code="ID"/></th>
                <th><g:message code="payment.link.url.table.header"/></th>
                <th><g:message code="payment.link.created.time"/></th>
                <th><g:message code="payment.link.created.amount"/></th>
                <th><g:message code="payment.link.created.status"/></th>
            </tr>
        </thead>
        <tbody>
            <g:each in="${paymentUrlLogs}" var="paymentUrlLog" >
                <g:set var="lastLogId" value="${paymentUrlLog.id}" />
                <g:set var="lastPaymentStatus" value="${paymentUrlLog.status}" />
                <tr>
                    <td class="innerContent">
                       ${paymentUrlLog.getId()}
                    </td>
                    <td class="innerContent">
                    <g:if test="${!['INITIATED','FAILED'].contains(lastPaymentStatus.toString())}">
                        <a href="${paymentUrlLog.getPaymentUrl()}">Link</a>
                    </g:if>
                    </td>
                    <td>
                        ${new SimpleDateFormat("yyyy-MM-dd HH:mm").format(paymentUrlLog.getCreatedAt())}
                    </td>
                    <td class="innerContent">
                        ${currency?.symbol}${String.format("%.2f", paymentUrlLog.getPaymentAmount())}
                    </td>
                    <td class="innerContent">
                        ${paymentUrlLog.getStatus()}
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>
<g:hiddenField name="lastPaymentUrlLogId" value="${lastLogId}"/>
<div class="btn-box" style="text-align: center;">
    <g:hiddenField name="buttonFlag"  id="buttonFlag" />
        <g:if test="${!lastPaymentStatus.toString().equals(Status.SUCCESSFUL.toString())}">
            <g:if test="${['GENERATED', 'FAILED','TIMEOUT', 'EXPIRED', 'CANCELLED', 'INITIATED'].contains(lastPaymentStatus.toString())}">
                    <g:if test="${['GENERATED'].contains(lastPaymentStatus.toString())}">
                         <a onclick="sendPaymentLink(${selected.id});" class="submit button-primary" id="btnLink">
                        <span id="btnText"><g:message code="send.payment.link.button"/></span>
                    </g:if>
                    <g:if test="${['FAILED','TIMEOUT', 'EXPIRED', 'CANCELLED', 'INITIATED'].contains(lastPaymentStatus.toString())}">
                        <a onclick="reGeneratePaymentLink(${selected.id});" class="submit button-primary" id="btnLink">
                        <span id="btnText"><g:message code="regenerate.payment.link.button"/></span>
                    </g:if>
                </a>
            </g:if>
            <g:if test="${!['FAILED','TIMEOUT', 'EXPIRED', 'CANCELLED'].contains(lastPaymentStatus.toString())}">
                <a onclick="checkPaymentStatus(${lastLogId});" class="submit button-primary" id="btnLink">
                        <span id="btnText"><g:message code="check.payment.status.button"/></span>
                </a>
            </g:if>
            <g:if test="${['GENERATED'].contains(lastPaymentStatus.toString())}">
                <a onclick="cancelPaymentLink(${lastLogId});" class="submit button-primary" id="btnLink">
                        <span id="btnText"><g:message code="Cancel"/></span>
                </a>
            </g:if>
        </g:if>
    </g:if>
    <g:else>
        <a onclick="window.location.reload();" class="submit button-primary" id="btnLink">
                <span id="btnText"><g:message code="Refresh"/></span>
        </a>
    </g:else>

<script type="text/javascript">
    function checkPaymentStatus(lastPaymentUrlLogId) {
        $.ajax({
            url: '/invoice/checkPaymentStatus',
            type: 'POST',
            data: { paymentUrlLogId: lastPaymentUrlLogId },
            success: function(response) {
            window.location.reload()
            },
            error: function(xhr, status, error) {
                console.error('Error sending payment link:', error);
            }
        });
    }
    function cancelPaymentLink(lastPaymentUrlLogId) {
        $.ajax({
            url: '/invoice/cancelPaymentLink',
            type: 'POST',
            data: { paymentUrlLogId: lastPaymentUrlLogId },
            success: function(response) {
            window.location.reload()
            },
            error: function(xhr, status, error) {
                console.error('Error sending payment link:', error);
            }
        });
    }

    function sendPaymentLink(invoiceId) {
        $.ajax({
            url: '/invoice/sendPaymentLinkEmail',
            type: 'POST',
            data: { invoiceId: invoiceId },
            success: function(response) {
            window.location.reload()
            },
            error: function(xhr, status, error) {
                console.error('Error sending payment link:', error);
            }
        });
    }

    function reGeneratePaymentLink(invoiceId) {
        $.ajax({
            url: '/invoice/getData',
            type: 'POST',
            data: {invoiceId: invoiceId},
            success: function(response) {
                window.location.reload()
                $('#lastPaymentUrlLogId').val(data.id);
            },
            error: function() {
                console.error("Error Occurred");
            }
        });
    }

</script>
