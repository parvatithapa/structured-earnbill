
<%--
  @author Parvati Thapa
  @since  11-Oct-2023
--%>

<div class="column-hold">

    <div class="heading">
        <strong>
                <g:message code="custom.invoice.inject.record.title"/>
        </strong>
    </div>

    <g:uploadForm id="inject-config-form" name="custom-invoice-inject-form" url="[action: 'uploadFile']">
        <div class="box" >
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">
                        <div class="column" style="width: 70%">
                            <table class="dataTable" cellspacing="3" cellpadding="3">
                                <tbody>
                                    <tr>
                                        <td><g:message code="custom.invoice.inject.file"/>:</td>
                                            <td class="value">
                                                <g:applyLayout name="form/fileupload">
                                                    <content tag="input.name">file</content>
                                                </g:applyLayout>
                                            </td>
                                        </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <g:applyLayout name="form/text">
                        <content tag="label">&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp</content>
                        <g:link action="downloadFile" >
                        example_ad_hoc_invoice_order_creation_data_v4.csv
                        </g:link>
                    </g:applyLayout>
                </fieldset>
            </div>
        </g:uploadForm>

        <div class="btn-box buttons">
            <ul>
                <li><a class="submit save button-primary" onclick="$('#inject-config-form').submit();"><span><g:message code="button.save"/></span></a></li>
                <li><a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a></li>
            </ul>
        </div>
        <div class="heading">
        <g:message code="Invoice Number"/>
        </div>
        <div class="sub-box">
        <g:each in ="${invoiceId}" var="id">
            <g:link controller="customInvoice" action="invoiceFilter" id="${id}" before="register(this);" onSuccess="render(data, next);">
            ${id}</p>
            </g:link>
        </g:each>
        </div>
    </div>
</div>
