%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%
  
<%@ page import="com.sapienter.jbilling.server.ediTransaction.TransactionType; org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.db.CompanyDAS; com.sapienter.jbilling.common.Util; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.mediation.MediationVersion" contentType="text/html;charset=UTF-8" %>

<div class="table-box" id="ediFiles">
    <div class="table-scroll">
        <table id="tbl-mediation-config" cellspacing="0" cellpadding="0">
            <thead>
                <tr><th><g:message code="edi.file.name"/></th></tr>
            </thead>
            <tbody>
                <tr><td><g:link class="cell double" controller="ediReport" action="ediStatistics"><g:message code="edi.ediStatistic"/></g:link></td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="ediStatisticsWithExceptions"><g:message code="edi.ediStatisticWithExceptions"/></g:link></td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="currentEdiExceptions"><g:message code="edi.currentExceptions"/></g:link></td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="customersNotInvoiced"><g:message code="edi.customersNotInvoiced"/></g:link></td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="downloadRegulatoryComplianceReport"><g:message code="edi.regulatoryComplianceReport"/></g:link> <g:link action="downloadRegulatoryComplianceReport" class="submit" style="float: right"><span><g:message code="edi.type.button.download"/></span></g:link></td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="autoRenewedCustomer"><g:message code="report.auto.renewed.customer"/></g:link> </td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="billingAdministrator"><g:message code="report.billing.administrator"/></g:link> </td></tr>
                <tr><td><g:link class="cell double" controller="ediReport" action="billingAdministratorEdiFile"><g:message code="report.billing.administrator.edi.file"/></g:link> </td></tr>
                %{--<tr><td><g:link class="cell double" controller="ediReport" action="subscriptionGoingEnd"><g:message code="report.subscription.going.to.end"/></g:link> </td></tr>--}%

                <g:each var="type" in="${com.sapienter.jbilling.server.ediTransaction.TransactionType.values()}">
                    <tr>
                        <td class="large">
                            <g:link class="cell" action="ldcFiles" id="${type.toString()}">
                                ${type.toString()}
                            </g:link>
                        </td>
                    </tr>
                </g:each>
            </tbody>
        </table>
        </div>
</div>
