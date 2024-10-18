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

<%@ page import="org.joda.time.Period; com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.mediation.MediationProcessCDRCountInfo;" %>

<%--
    @author Vikas Bodani, Pance Isajeski
    @since 18 Feb 2011
 --%>

<div class="column-hold">
    <div class="heading">
        <strong><g:message code="mediation.process.title"/> <em>${selected.id}</em>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <!-- mediation process info -->
            <table cellspacing="0" cellpadding="0" class="dataTable">
                <tbody>
                    <tr>
                        <td><g:message code="mediation.label.id"/></td>
                        <td class="value">${selected.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.label.config"/></td>
                        <td class="value">${selected.configurationId}</td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.label.start.time"/></td>
                        <td class="value"><g:formatDate date="${selected.startDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.label.end.time"/></td>
                        <td class="value"><g:formatDate date="${selected.endDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/></td>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.label.total.runtime"/></td>
                        <td class="value">
                            <g:if test="${selected.startDate && selected.endDate}">
                                <g:set var="runtime" value="${new Period(selected.startDate?.time, selected.endDate?.time)}"/>
                                <g:message code="mediation.runtime.format" args="[runtime.getHours(), runtime.getMinutes(), runtime.getSeconds()]"/>
                            </g:if>
                            <g:else>
                                -
                            </g:else>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- records info -->
    <div class="heading">
        <strong><g:message code="mediation.process.records"/></strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <table cellpadding="0" cellspacing="0" class="dataTable">
                <tbody>
                    <tr>
                        <td><g:message code="mediation.label.orders.affected"/></td>
                        <td class="value">${ordersCreatedCount}</td>
                        <g:if test="${ordersCreatedCount > 0}">
                            <td class="value">
                                <sec:access url="/order/list">
                                    <g:link controller="order" action="byMediation" id="${selected.id}">
                                    	<g:message code="mediation.show.all.orders"/>
                                    </g:link>
                                </sec:access>
                            </td>
                        </g:if>
                    </tr>
                    <tr>
                        <td><g:message code="mediation.label.invoices.created"/></td>
                        <td class="value">${invoicesCreatedCount}</td>
                        <g:if test="${invoicesCreatedCount > 0}">
                            <td class="value">
                                <sec:access url="/invoice/list">
                                    <g:link controller="invoice" action="byMediation" id="${selected.id}">
                                        <g:message code="mediation.show.all.invoices"/>
                                    </g:link>
                                </sec:access>
                            </td>
                        </g:if>
                    </tr>
                </tbody>
            </table>
            <hr/>
            <!-- mediation process stats -->
            <table cellspacing="0" cellpadding="0" class="dataTable">
                <tbody>
                    <tr>
                    <g:render id="doneAndBillable" template="/mediation/showMediationRecords"
                            model="[messageCode: 'mediation.label.done.billable', value: selected?.doneAndBillable,
                                    action: 'showMediationRecords', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE]"/>
                    </tr>
                    <g:if test="${cdrCountInfos != null}">
                        <g:each var="callTypeInfo" in="${cdrCountInfos}">
                            <g:if test="${callTypeInfo.callType != null}">
                                <tr>
                                    <g:render id="doneAndBillable" template="/mediation/showMediationSubRecords"
                                        model="[messageCode: callTypeInfo.callType, value: callTypeInfo.count,
                                        action: 'showMediationRecords', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE,
                                        selectedCallType: callTypeInfo.callType]"/>
                                </tr>
                            </g:if>
                        </g:each>
                    </g:if>    
                    <tr>
                        <g:render id="errors" template="/mediation/showMediationRecords"
                            model="[messageCode: 'mediation.label.errors.detected', value: selected?.errors,
                                   action: 'showMediationErrors', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_ERROR_DETECTED]"/>
                    </tr>
                    <tr>
                        <g:render id="duplicates" template="/mediation/showMediationRecords"
                            model="[messageCode: 'mediation.label.duplicate.records', value: selected?.duplicates,
                                   action: 'showMediationErrors', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_DUPLICATE]"/>
                    </tr>
                    <tr>
                        <g:render id="doneAndNotBillable" template="/mediation/showMediationRecords"
                            model="[messageCode: 'mediation.label.done.not.billable', value: selected?.doneAndNotBillable,
                                   action: 'showMediationRecords', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE]"/>
                    </tr>
                    <g:if test="${selected?.aggregated != 0}">
                        <tr>
                            <g:render id="aggregated" template="/mediation/showMediationRecords"
                                      model="[messageCode: 'mediation.label.aggregated', value: selected?.aggregated,
                                              action: 'showMediationRecords', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_AGGREGATED]"/>
                        </tr>
                    </g:if>
                    <g:if test="${cdrCountInfosNB != null}">
                        <g:each var="cdrCountInfoNB" in="${cdrCountInfosNB}">
                            <g:if test="${cdrCountInfoNB.callType != null}">
                                <tr>
                                    <g:render id="doneAndNotBillable" template="/mediation/showMediationSubRecords"
                                        model="[messageCode: cdrCountInfoNB.callType, value: cdrCountInfoNB.count,
                                        action: 'showMediationRecords', processId: selected?.id, first: 'true', status: Constants.MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE,
                                        selectedCallType: cdrCountInfoNB.callType]"/>
                                </tr>
                            </g:if>
                        </g:each>
                    </g:if>

                    <tr class="column-hold">
                        <td class="col01"><g:message code="mediation.label.records"/></td>
                        <td class="value">${selected?.recordsProcessed}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    <div class="btn-box">
        <g:if test="${canBeUndone}">
            <a onclick="showConfirm('undo-${selected.id}');" class="submit delete">
                <span><g:message code="mediation.process.undo"/></span>
            </a>
        </g:if>
        <g:isRoot>
           <g:if test="${!isMediationProcessRunning && (selected.errors > 0 || selected.errors > 0)}">
               <g:link  class="submit" id="${selected.id}" action="recycleProcessCDRs">
                  <span ><g:message code="button.recycle.process"/></span>
               </g:link>
           </g:if>
        </g:isRoot>
        <g:if test="${canBeUndone}">

            <g:remoteLink  class="submit" id="${selected.id}" action="refreshMediationCounter" update="column2" params="[template: 'show']">
                <span><g:message code="button.refresh.process"/></span>
            </g:remoteLink>
        </g:if>

    </div>

    <g:render template="/confirm"
              model="['message': 'mediation.undo.confirm',
                      'controller': 'mediation',
                      'action': 'undo',
                      'id': selected?.id,
                      'ajax': false,
              ]"/>
</div>
