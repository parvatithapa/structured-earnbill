<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>

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
  Reports list

  @author Brian Cowdery
  @since  07-Mar-2011
--%>

<%-- list of reports --%>
<g:if test="${reports}">
    <div class="table-box">
        <div class="table-scroll">
            <table id="reports" cellspacing="0" cellpadding="0">
                <thead>
                    <tr>
                        <th><g:message code="report.th.name"/></th>
                    </tr>
                </thead>
                <tbody>

                <g:each var="report" in="${reports}">

                    <tr id="report-${report.id}" class="${selected?.id == report.id ? 'active' : ''}">
                        <td>
                            <g:remoteLink class="cell double" action="show" id="${report.id}" params="[template: 'show']" before="register(this);" onSuccess="render(data, next);">
                                <strong><g:message code="${StringEscapeUtils.escapeHtml(report?.name)}"/></strong>
                                <em>${StringEscapeUtils.escapeHtml(report?.fileName)}</em>
                            </g:remoteLink>
                        </td>
                    </tr>

                </g:each>

                </tbody>
            </table>
        </div>
    </div>
</g:if>

<%-- no report to show --%>
<g:if test="${!reports}">
    <div class="heading"><strong><em><g:message code="report.type.no.reports.title"/></em></strong></div>
    <div class="box">
        <g:if test="${selectedTypeId}">
            <em><g:message code="report.type.no.reports.warning"/></em>
        </g:if>
        <g:else>
            <em><g:message code="report.type.not.selected.message"/></em>
        </g:else>
    </div>
</g:if>

<%-- list pager and buttons --%>
<div class="pager-box">
    <g:set var="paginateAction" value="${actionName == 'reports' ? 'reports' : 'allReports'}"/>
    <g:set var="columnUpdate" value="${paginateAction == 'allReports' ? 'column1' : 'column2'}"/>
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], action: paginateAction, id:selectedTypeId]"/>
        </div>
    </div>

    <jB:isPaginationAvailable total="${reports?.totalCount ?: 0}">
        <div class="row-center">
            <jB:remotePaginate controller="report" action="${paginateAction}" id="${selectedTypeId}" total="${reports?.totalCount ?: 0}" update="tmp-reports" onSuccess="moveReports();"/>
        </div>
    </jB:isPaginationAvailable>
</div>

<g:if test="${paginateAction != 'allReports'}">
    <div class="btn-box">
        <g:remoteLink action="allReports" update="tmp-reports" onSuccess="moveReports();" class="submit show"><span><g:message code="button.show.all"/></span></g:remoteLink>
    </div>
</g:if>

<div id="tmp-reports" style="display: none;"></div>

<script type="text/javascript">
    function moveReports() {
        var col = '#column2';
        if($('#tmp-reports').closest('#column1').size() > 0) {
            col = '#column1';
            $('#column2').empty();
        }
        var colEl = $(col);

        var newEl = $('#tmp-reports').children();
        newEl.detach();
        colEl.empty();
        colEl.append(newEl);
    }

</script>
