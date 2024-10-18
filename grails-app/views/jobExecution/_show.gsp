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
  Shows Job Execution Statistics.

--%>


<div class="column-hold">
    <div class="heading">
        <strong>
        <em>Job Execution (${selected?.id})</em>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="jobExecution.label.id"/></td>
                <td class="value">${selected.id}</td>
            </tr>
            <tr>
                <td><g:message code="jobExecution.label.jobType"/></td>
                <td class="value">${selected.jobType}</td>
            </tr>
            <tr>
                <td><g:message code="jobExecution.label.startDate"/></td>
                <td class="value"><g:formatDate date="${selected?.startDate}" formatName="date.time.format"/></td>
            </tr>
            <tr>
                <td><g:message code="jobExecution.label.endDate"/></td>
                <td class="value"><g:formatDate date="${selected?.endDate}" formatName="date.time.format"/></td>
            </tr>


            <g:each in="${selected.lines.findAll{it.type=='HEADER'}.sort{it.name}}" var="line">
                <tr>
                    <td><g:message code="jobExecution.header.${line.name}" default="${line.name}"/></td>
                    <td class="value">${line.value}</td>
                </tr>
            </g:each>

            <g:each in="${selected.lines.findAll{it.type!='HEADER'}.sort{it.name}}" var="line">
                <tr>
                    <td><g:message code="jobExecution.line.${line.name}" default="${line.name}"/></td>
                    <td class="value">${line.value}</td>
                </tr>
            </g:each>


            </tbody>
        </table>
      </div>
    </div>

</div>
