%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2015] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<div class="heading">
    <strong><g:message code="asset.heading.transitions"/></strong>
</div>
<div class="box">
    <div class="sub-box">
        <g:if test="${transitions}">
            <table class="innerTable" >
                <thead class="innerHeader">
                <tr>
                    <th><g:message code="asset.label.transition.status"/></th>
                    <th><g:message code="asset.label.transition.changedate"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each var="transition" in="${transitions}" status="idx">
                    <tr>
                        <td class="innerContent" style="min-width: 75px" data-cy="assetTransitionStatus">
                            ${transition.newStatus.description}
                        </td>
                        <td class="innerContent">
                            <g:formatDate formatName="date.timeSecs.format" date="${transition.createDatetime}" timeZone="${session['company_timezone']}"/>
                        </td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <em><g:message code="asset.prompt.no.transitions"/></em>
        </g:else>
    </div>
</div>