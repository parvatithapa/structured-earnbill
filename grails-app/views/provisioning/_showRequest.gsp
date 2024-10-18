<div class="column-hold">
    <div class="heading">
        <strong><g:message code="provisioning.request.information.title"/></strong>
    </div>

    <!-- command information -->
    <div class="box">
        <div class="sub-box">
            <table class="dataTable table-layout-fixed" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="provisioning.request.label.id"/></td>
                        <td class="value wide-width">${selected?.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.request.label.cmd_id"/></td>
                        <td class="value wide-width">${selected?.provisioningCommandId}</td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.request.label.execution_order"/></td>
                        <td class="value wide-width">${selected?.executionOrder}</td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.label.create_date"/></td>
                        <td class="value wide-width">
                            <g:formatDate date="${selected?.createDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.label.submit_date"/></td>
                        <td class="value wide-width">
                            <g:formatDate date="${selected?.submitDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.label.result_received_date"/></td>
                        <td class="value wide-width">
                            <g:formatDate date="${selected?.resultReceivedDate}" formatName="date.timeSecsAMPM.format" timeZone="${session['company_timezone']}"/>
                        </td>
                    </tr>
                </tbody>
            </table>

            <hr/>

            <table class="dataTable table-layout-fixed" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="provisioning.request.label.status"/></td>
                        <td class="value wide-width">${selected?.requestStatus}</td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.request.label.processor"/></td>
                        <td class="value wide-width">${selected?.processor}</td>
                    </tr>
                    <tr>
                        <td><g:message code="provisioning.request.label.submit"/></td>
                        <td class="value wide-width" id="column-text-wrap">${selected?.submitRequest}</td>
                    </tr>
                </tbody>
            </table>

        </div>
    </div>
</div>