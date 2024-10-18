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

<%@ page import="com.sapienter.jbilling.common.CommonConstants" %>
<html>
<head>
	<meta name="layout" content="main"/>
</head>

<body>
    <div class="table-info">
        <em><g:message code="billing.heading.period"/></em>
    </div>

    <div>
        <table class="dataTable no-wrap" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td class="small left-pad"><g:message code="billing.details.label.process.period"/></td>
                <td class="value small">${formattedPeriod}</td>

                <td class="small"><g:message code="billing.details.label.process.period.start"/></td>
                <td class="value small"><g:formatDate date="${process.billingDate}" formatName="date.pretty.format"/></td>

                <td class="small"><g:message code="billing.details.label.process.period.end"/></td>
                <td class="value small"><g:formatDate date="${process.billingPeriodEndDate}" formatName="date.pretty.format"/></td>
                <td class="remainder-width"/>
            </tr>

            <g:if test="${process?.isReview == 1}">
                <tr>
                    <td colspan="7" class="left-pad wrap"><g:message code="billing.details.is.review"/></td>
                </tr>
            </g:if>
            </tbody>
        </table>
    </div>

    <div class="sub-box btn-box table-box-below">
        <g:if test="${process?.isReview == 1}">
            <sec:access url="/billing/approve">
                <a onclick="showConfirm('approve-'+${process?.id});" class="submit apply button-primary">
                    <span><g:message code="billing.details.approve"/></span>
                </a>
            </sec:access>

            <sec:access url="/billing/disapprove">
                <a onclick="showConfirm('disapprove-'+${process?.id});" class="submit cancel">
                    <span><g:message code="billing.details.disapprove"/></span>
                </a>
            </sec:access>
        </g:if>
        <g:link action="showOrders" id="${process?.id}" class="submit order"><span>Show Orders</span></g:link>
        <g:link action="showInvoices" id="${process?.id}" params="[isReview: process?.isReview]" class="submit show"><span>Show Invoices</span></g:link>
        <g:if test="${canRestart}">
            <g:link action="restart" id="${process?.id}" class="submit play"><span>Restart</span></g:link>
        </g:if>
        <sec:ifAllGranted roles="BILLING_1920">
	        <g:if test="${(!isBillingRunning) && (!isEmailJobRunning) && (process?.isReview == 0) && (processRun?.status.id == 2 || processRun?.status.id == 3) && (emailProcessInfo.size() == 0) && (process?.id.toInteger() > cutOfBillingProcess)}">            
	            <a onclick="showConfirm('sendInvoiceEmails-'+${process?.id});" class="submit apply button-primary">
	                    <span><g:message code="billing.details.send.invoice.emails"/></span>
	            </a>
	        </g:if>
        </sec:ifAllGranted>
    </div>

    <div class="table-info">
        <em><g:message code="billing.heading.overview"/></em>
    </div>

    <div>
        <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td class="left-pad"><g:message code="billing.details.label.process.id"/></td>
                <td class="value">${process?.id}</td>

                <td><g:message code="billing.details.label.orders.processed"/></td>
                <td class="value">${orderProcessCount}</td>

                <td><g:message code="billing.details.label.invoices.generated"/></td>
                <td class="value">${invoiceProcessCount}</td>

                <td><g:message code="billing.details.label.payments.generated"/></td>
                <td class="value">${generatedPayments.size()}</td>

                <g:if test="${process?.isReview == 1}">
                    <td><g:message code="billing.details.label.review.status"/></td>
                    <td class="value">
                        <g:if test="${ CommonConstants.REVIEW_STATUS_GENERATED.intValue() == configuration?.reviewStatus}">
                            <g:message code="billing.details.review.generated"/>
                        </g:if>
                        <g:if test="${ CommonConstants.REVIEW_STATUS_APPROVED.intValue() == configuration?.reviewStatus}">
                            <g:message code="billing.details.review.approved"/>
                        </g:if>
                        <g:if test="${ CommonConstants.REVIEW_STATUS_DISAPPROVED.intValue() == configuration?.reviewStatus}">
                            <g:message code="billing.details.review.disapproved"/>
                        </g:if>
                    </td>

                </g:if>
            </tr>            
            </tbody>
        </table>

        <div class="sub-box">
            <table class="innerTable">
                <thead class="innerHeader">
                    <tr>
                        <th class="left-pad"><g:message code="billing.details.label.process.result"/></th>
                        <th><g:message code="billing.details.label.total.invoiced"/></th>
                        <th><g:message code="billing.details.label.total.paid"/></th>
                        <th><g:message code="billing.details.label.total.not.paid"/></th>
                        <th><g:message code="label.billing.batch.job.execution.total.users.passed"/></th>
                        <th class="last"><g:message code="label.billing.batch.job.execution.total.users.failed"/></th>
                    </tr>
                </thead>
                <tbody>
                    <!-- process summary -->
                    <tr>
                        <td class="left-pad">${processRun?.status?.getDescription(session['language_id'])}</td>
                        <td>
                            <%
                                def invoiced = [:]
                                invoices.each { invoice ->
                                    def invoiceLineTotal = 0.0;
                                    invoice.invoiceLines.each { line ->
                                        if (line.deleted == 0) {
                                            invoiceLineTotal += line.amount;
                                        }
                                    }
                                    invoiced[invoice.currency] = invoiced.get(invoice.currency, BigDecimal.ZERO).add(invoiceLineTotal);
                                }
                            %>
                            <g:each var="total" in="${invoiced.entrySet()}">
                                <g:formatNumber number="${total.value}" type="currency" currencySymbol="${total.key.symbol}"/> <br/>
                            </g:each>
                        </td>
                        <td>
                            <%
                                def generatedPaymentMethods = [:]
                                generatedPayments.each { payment ->
                                    def currencies = generatedPaymentMethods.get(payment.paymentMethod, [:])
                                    currencies[payment.currency] = currencies.get(payment.currency, BigDecimal.ZERO).add(payment.amount)
                                    generatedPaymentMethods[payment.paymentMethod] = currencies
                                }
                            %>

                            <g:each var="paymentMethod" in="${generatedPaymentMethods.entrySet()}">
                                <g:each var="paymentCurrency" in="${paymentMethod.value}">
                                    <div>
                                        <span class="small">
                                            <g:formatNumber number="${paymentCurrency.value}" type="currency" currencySymbol="${paymentCurrency.key.symbol}"/>
                                        </span>
                                        <span class="small">
                                            ${paymentMethod.key.getDescription(session['language_id'])} <br/>
                                        </span>
                                    </div>
                                </g:each>
                            </g:each>
                        </td>
                        <td>
                            <%
                                def paid = [:]
                                generatedPayments.each { payment ->
                                    paid[payment.currency] = paid.get(payment.currency, BigDecimal.ZERO).add(payment.amount)
                                }

                                def unpaid = invoiced.clone()
                                paid.each { currency, amount ->
                                    unpaid[currency] = unpaid.get(currency, BigDecimal.ZERO).subtract(amount)
                                }
                            %>
                            <g:each var="amount" in="${unpaid.entrySet()}">
                                <g:formatNumber number="${amount.value}" type="currency" currencySymbol="${amount.key.symbol}"/><br/>
                            </g:each>
                        </td>

                        <td>
                            <%
                                def successful = 0
                                    jobs.each { job ->
                                    successful = successful + job.totalSuccessfulUsers
                                }
                            %>
                            ${successful}
                        </td>
                        <td>
                            <%
                                def last = jobs ? jobs.get(0) : null
                            %>
                            <g:if test="${last?.totalFailedUsers > 0}">
                                <g:link class="cell" action="failed" id="${last.jobExecutionId}">
                                ${last.totalFailedUsers}
                                </g:link>
                            </g:if>
                            <g:else>
                                ${last?.totalFailedUsers}
                            </g:else>
                        </td>
                    </tr>

                    <!-- grand totals -->
                    <tr class="bg">
                        <td class="col02"></td>
                        <td>
                            <g:each var="amount" in="${invoiced.entrySet()}">
                                <strong>
                                    <g:formatNumber number="${amount.value}" type="currency" currencySymbol="${amount.key.symbol}"/>
                                </strong>
                            </g:each>
                        </td>
                        <td>
                            <g:each var="amount" in="${paid.entrySet()}">
                                <strong>
                                    <g:formatNumber number="${amount.value}" type="currency" currencySymbol="${amount.key.symbol}"/>
                                </strong>
                            </g:each>
                        </td>
                        <td>
                            <g:each var="amount" in="${unpaid.entrySet()}">
                                <strong>
                                    <g:formatNumber number="${amount.value}" type="currency" currencySymbol="${amount.key.symbol}"/>
                                </strong>
                            </g:each>
                        </td>
                        <td></td>
                        <td></td>
                    </tr>
                </tbody>
            </table>
        </div>

    </div>

<!-- main billing run -->
    <div class="table-info">
        <em><g:message code="label.billing.batch.job.execution.header"/></em>
    </div>
    <div class="sub-box">
        <table class="innerTable">
            <thead class="innerHeader">
                <tr>
                    <th class="first"><g:message code="label.billing.batch.job.execution.start.date"/></th>
                    <th><g:message code="label.billing.batch.job.execution.end.date"/></th>
                    <th><g:message code="label.billing.batch.job.execution.total.users.passed"/></th>
                    <th class="last"><g:message code="label.billing.batch.job.execution.total.users.failed"/></th>
                </tr>
            </thead>
            <tbody>
                <g:set var = "batchJobService" bean = "billingBatchJobService"/>
                <g:each var="job" in="${jobs}">
                    <tr id="job-${job.id}" class="${selected?.id == job.id ? 'active' : ''} ${job?.id > 0 ? 'isReview' : ''}">
                        <td class="left-pad">
                            <g:formatDate date       = "${job.jobExecutionId ? batchJobService.getStartDate(job.jobExecutionId) : null}"
                                          formatName = "date.time.format"
                                          timeZone   = "${session['company_timezone']}"/>
                        </td>
                        <td>
                            <g:formatDate date       = "${job.jobExecutionId ? batchJobService.getEndDate(job.jobExecutionId) : null}"
                                          formatName = "date.time.format"
                                          timeZone   = "${session['company_timezone']}"/>
                        </td>
                        <td>${job.totalSuccessfulUsers}</td>
                        <td>
                            ${job.totalFailedUsers}
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>
    
    <!-- Email Info -->
    <g:if test="${process?.isReview != 1 && emailProcessInfo}">
    <div class="table-info">
        <em><g:message code="label.billing.batch.job.execution.email.header"/></em>
    </div>
    <div class="sub-box">
        <table class="innerTable">
            <thead class="innerHeader">
                <tr>
                    <th class="first"><g:message code="label.billing.batch.job.execution.email.result"/></th>
                    <th><g:message code="label.billing.batch.job.execution.email.estimated"/></th>
                    <th><g:message code="label.billing.batch.job.execution.email.sent"/></th>
                    <th><g:message code="label.billing.batch.job.execution.email.failed"/></th>
                    <th><g:message code="label.billing.batch.job.execution.email.skipped"/></th>
                    <th><g:message code="label.billing.batch.job.execution.start.date"/></th>
                    <th><g:message code="label.billing.batch.job.execution.end.date"/></th>
                    <th class="last"><g:message code="label.billing.batch.job.execution.email.SentVia"/></th>
                </tr>
            </thead>
            <tbody>
            	<g:each var="emailProcess" in="${emailProcessInfo}">                               
					<tr id="job-${emailProcess.id}">
						<g:if test="${emailProcess?.endDatetime == null}">
							<td class="left-pad"><g:message code="label.billing.batch.job.execution.email.running"/></td>
	                    </g:if>
	                    <g:else>
							<td class="left-pad"><g:message code="label.billing.batch.job.execution.email.completed"/></td>
	                    </g:else>
						<td>${emailProcess.emailsEstimated}</td>
						<td>${emailProcess.emailsSent}</td>
						<td>${emailProcess.emailsFailed}</td>
						<g:if test="${emailProcess?.endDatetime == null}">
							<td>0</td>
						</g:if>
	                    <g:else>
	                    	<td>${emailProcess.emailsEstimated - emailProcess.emailsSent - emailProcess.emailsFailed}</td>	                    	
	                    </g:else>
	                    <td>
                            <g:formatDate date       = "${emailProcess.startDatetime}"
                                          formatName = "date.time.format"
                                          timeZone   = "${session['company_timezone']}"/>
                        </td>
                        <td>
                            <g:formatDate date       = "${emailProcess.endDatetime}"
                                          formatName = "date.time.format"
                                          timeZone   = "${session['company_timezone']}"/>
                        </td>
						<td>${emailProcess.source}</td>
					</tr>
				</g:each>
            </tbody>
        </table>
    </div>
    </g:if>

    <!-- payments made after the billing process by retries -->
    <div class="table-info">
        <em><g:message code="billing.details.payments.after.billing"/> <strong>${invoicePayments.size()}</strong></em>
    </div>
    <div class="sub-box">
        <table class="innerTable">
            <thead class="innerHeader">
                <tr>
                    <th class="first"><g:message code="billing.details.payments.payment.date"/></th>
                    <th><g:message code="billing.details.payments.number.of.payments"/></th>
                    <th><g:message code="billing.details.payments.total.paid"/></th>
                </tr>
            </thead>
            <tbody>
            <%
                def invoicePaymentDates = new TreeMap()
                invoicePayments.each { invoicePayment ->
                    invoicePaymentDates[invoicePayment.payment.paymentDate] = invoicePaymentDates.get(invoicePayment.payment.paymentDate, []) << invoicePayment
                }
            %>

                <!-- all payments -->
                <g:each var="paymentDate" status="i" in="${invoicePaymentDates.entrySet()}">
                    <tr>
                        <td class="left-pad" valign="top"><g:formatDate date="${paymentDate.key}"/></td>
                        <td valign="top">${paymentDate.value.size()}</td>
                        <td valign="top">
                            <%
                                def invoicePaymentMethods = [:]
                                paymentDate.value.each { invoicePayment ->
                                    def currencies = invoicePaymentMethods.get(invoicePayment.payment.paymentMethod, [:])
                                    currencies[invoicePayment.payment.currency] = currencies.get(invoicePayment.payment.currency, BigDecimal.ZERO).add(invoicePayment.amount)
                                    invoicePaymentMethods[invoicePayment.payment.paymentMethod] = currencies
                                }
                            %>

                            <g:each var="paymentMethod" in="${invoicePaymentMethods.entrySet()}">
                                <g:each var="paymentCurrency" in="${paymentMethod.value}">
                                    <div>
                                        <span class="small">
                                            <g:formatNumber number="${paymentCurrency.value}" type="currency" currencySymbol="${paymentCurrency.key.symbol}"/>
                                        </span>
                                        <span class="small">
                                            ${paymentMethod.key.getDescription(session['language_id'])}
                                        </span>
                                    </div>
                                </g:each>
                            </g:each>
                        </td>
                    </tr>
                </g:each>

                <!-- grand totals -->
                <tr class="bg">
                    <td class="col02"></td>
                    <td><strong>${invoicePayments.size()}</strong></td>
                    <td>
                        <%
                            def invoiceTotalPaid = [:]
                            invoicePayments.each { invoicePayment ->
                                invoiceTotalPaid[invoicePayment.payment.currency] = invoiceTotalPaid.get(invoicePayment.payment.currency, BigDecimal.ZERO).add(invoicePayment.amount)
                            }
                        %>

                        <g:each var="amount" in="${invoiceTotalPaid.entrySet()}">
                            <strong>
                                <g:formatNumber number="${amount.value}" type="currency" currencySymbol="${amount.key.symbol}"/>
                            </strong>
                        </g:each>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <g:render template="/confirm"
              model="['message':'billing.details.approve.confirm',
                 'controller':'billing',
                 'action':'approve',
                 'id':process.id,
                ]"/>

    <g:render template="/confirm"
              model="['message':'billing.details.disapprove.confirm',
                 'controller':'billing',
                 'action':'disapprove',
                 'id':process.id,
                ]"/>
     <g:render template="/confirm"
              model="['message':'billing.details.send.invoice.confirm',
                 'controller':'billing',
                 'action':'sendInvoiceEmails',
                 'id':process.id,
                ]"/>
</body>
</html>
