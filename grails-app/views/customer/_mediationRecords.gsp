<%@ page import="java.time.LocalDateTime; java.time.format.DateTimeFormatter; java.time.ZoneId;" %>
<div class="table-box tab-table">
  <div class="table-scroll">
      <table id="products" cellspacing="0" cellpadding="0">
          <thead>
              <tr>
                  <th class="header-sortable">
                      <g:message code="bhmr.record.user.id"/>
                  </th>
                  <th class="header-sortable">
                      <g:message code="bhmr.record.process.id"/>
                  </th>
                  <th class="header-sortable">
                      <g:message code="bhmr.record.eventDate"/>
                  </th>
                  <th class="header-sortable">
                      <g:message code="bhmr.record.quantity"/>
                  </th>
                  <th class="header-sortable">
                      <g:message code="bhmr.record.radius.avp"/>
                  </th>
                  <th class="header-sortable">
                      <g:message code="bhmr.record.processingDate"/>
                  </th>
              </tr>
          </thead>

          <tbody>
            <g:each var="record" in="${bhmrRecord?.getData()}" >
              <tr>
                <td>
                    ${record?.userId}
                </td>
                <td>
                    ${record?.processId}
                </td>
                <td>
                    <g:formatDate date="${Date.from(LocalDateTime.parse(record?.eventDate.replace('T',' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                        .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>

                </td>
                <td>
                    ${record?.quantity}
                </td>
                <td>
                    ${record?.pricingFields.replaceAll(":1:string:","=")}
                </td>
                <td>
                <g:formatDate date="${Date.from(LocalDateTime.parse(record?.processingDate.replace('T',' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault()).toInstant())}" formatName="date.time.24Hr.format" timeZone="${session['company_timezone']}"/>
                </td>
              </tr>
            </g:each>
          </tbody>
      </table>
  </div>
</div>
<div class="pager-box">
    <div class="results">
        <g:render template="/layouts/includes/pagerShowResults"
                  model="[steps: [10, 20, 50], update: 'mediationRecords', action: 'filterMediationRecords',
                          extraParams: [
                          userId: user?.id ?: params.userId,
                          processId : "${processId}",
                  ]]"/>
    </div>
    <jB:isPaginationAvailable total="${bhmrRecord?.getRecordsTotal() ?: 0}">
        <div class="row-center">
           <jB:remotePaginate action="filterMediationRecords"
                         params="${sortableParams(params: [partial: true, userId: user?.id ?: params.userId, max: params.max, processId : processId])}"
                         total="${bhmrRecord?.getRecordsTotal() ?: 0}"
                         update="mediationRecords"
                         method="GET"/>
        </div>
    </jB:isPaginationAvailable>
</div>