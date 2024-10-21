<%@ page import="com.sapienter.jbilling.server.fileProcessing.FileConstants; com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS" %>
<div class="table-box">
    <g:set var="showExceptionCode" value="${ediFileRecords.find{it?.fileFields?.find{it.ediFileFieldKey==FileConstants.EXCEPTION_CODE_KEY}} ? true:false}"/>


    <table cellspacing="0" cellpadding="0" >
        <thead>
        <tr>
            <th><g:remoteSort action="ediFileRecordList" sort="id" parame="[id:ediFile?.id]" update="ediFileRecords"><g:message code="edi.file.record.id"/></g:remoteSort></th>
            <th><g:remoteSort action="ediFileRecordList" sort="ediFileRecordHeader" parame="[id:ediFile?.id]" update="ediFileRecords"><g:message code="edi.file.record.header"/></g:remoteSort></th>
            <th><g:remoteSort action="ediFileRecordList" sort="recordOrder" parame="[id:ediFile?.id]" update="ediFileRecords"><g:message code="edi.file.record.order"/></g:remoteSort></th>
            <g:if test="${showExceptionCode}">
                <th><g:remoteSort action="ediFileRecordList" sort="id" parame="[id:ediFile?.id]" update="ediFileRecords"><g:message code="edi.file.record.exception.code"/></g:remoteSort></th>
            </g:if>
        </tr>
        </thead>

        <tbody>
        <g:each in="${ediFileRecords}" var="ediFileRecord">
            <tr id="edi-file-record-${ediFileRecord.id}" >

                <td>
                    <g:remoteLink class="cell double" action="ediFileRecordShow" id="${ediFileRecord?.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFileRecord?.id}
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" action="ediFileRecordShow" id="${ediFileRecord?.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFileRecord?.ediFileRecordHeader}
                    </g:remoteLink>
                </td>

                <td>
                    <g:remoteLink class="cell double" action="ediFileRecordShow" id="${ediFileRecord?.id}" before="register(this);" onSuccess="render(data, next);">
                        ${ediFileRecord?.recordOrder}
                    </g:remoteLink>
                </td>
                <g:if test="${showExceptionCode}">
                    <td>
                        <g:remoteLink class="cell double" action="ediFileRecordShow" id="${ediFileRecord?.id}" before="register(this);" onSuccess="render(data, next);">
                            ${ediFileRecord?.getFileFields()?.find{it.ediFileFieldKey==FileConstants.EXCEPTION_CODE_KEY}?.ediFileFieldValue}
                        </g:remoteLink>
                    </td>
                </g:if>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">


    <div class="row-center">
        <jB:remotePaginate  action="ediFileRecordList" total="${ediFileRecords?.totalCount ?: 0}" update="ediFileRecords" params="[partial:true, id:ediFile?.id]"/>
    </div>
</div>
