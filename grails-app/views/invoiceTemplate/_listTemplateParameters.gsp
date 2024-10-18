<g:javascript src="knockout-min.js"/>

<div id="list-template-parameters" class="samples-dialog">

    <style type="text/css" scoped="scoped">
        
        #list-template-parameters {
            display: none;
        }
    
        #list-template-parameters table {
            width: 100%;
            padding-top: 5px;
            padding-bottom: 15px;
            font-size: smaller;
        }
    
        #list-template-parameters tr,
        #list-template-parameters th,
        #list-template-parameters td {
            border-collapse: collapse;
            border: solid lightgray 1px;
        }
    
        #list-template-parameters .code-sample {
            font-family: monospace;
        }
    
        #list-template-parameters .details {
            font-size: smaller;
        }
        
    </style>

    <h3><g:message code="invoiceTemplate.parameters.invoiceParameters"/></h3>

    <table id="invoice-parameters">
        <thead>
            <tr>
                <th><g:message code="invoiceTemplate.parameters.name"/></th>
                <th><g:message code="invoiceTemplate.parameters.statement"/></th>
            </tr>
        </thead>
        <tbody data-bind="foreach: invoiceParameters">
            <tr>
                <td data-bind="text: name"></td>
                <td class="code-sample" data-bind="text: statement"></td>
            </tr>
        </tbody>
    </table>

    <h3><g:message code="invoiceTemplate.parameters.dynamicParameters"/></h3>

    <table id="dynamic-invoice-parameters">
        <thead>
            <tr>
                <th><g:message code="invoiceTemplate.parameters.name"/></th>
                <th><g:message code="invoiceTemplate.parameters.statement"/></th>
            </tr>
        </thead>
        <tbody data-bind="foreach: dynamicInvoiceParameters">
            <tr>
                <td data-bind="text: name"></td>
                <td class="code-sample" data-bind="text: statement"></td>
            </tr>
        </tbody>
    </table>

    <div class="details">
        <p>
            <g:message code="invoiceTemplate.parameters.metaFields.details"/>
        </p>
    </div>

    <div class="details">
        <p>
            <g:message code="invoiceTemplate.parameters.format"/>
        </p>
        <ul>
            <li class="code-sample">date(Date):String</li>
            <li class="code-sample">money(Number):String</li>
            <li class="code-sample">pcnt(Number):String</li>
            <li class="code-sample">dec(Number):String</li>
            <li class="code-sample">parse(String):Number</li>
        </ul>
    </div>

    <div class="details">
        <p>
            <g:message code="invoiceTemplate.parameters.combineData.details"/>
        </p>
    </div>

    <h3><g:message code="invoiceTemplate.parameters.invoiceVariables"/></h3>

    <table id="invoice-variables">
        <thead>
            <tr>
                <th><g:message code="invoiceTemplate.parameters.name"/></th>
                <th><g:message code="invoiceTemplate.parameters.statement"/></th>
                <th><g:message code="invoiceTemplate.parameters.comments"/></th>
            </tr>
        </thead>
        <tbody data-bind="foreach: invoiceVariables">
            <tr>
                <td data-bind="text: name"></td>
                <td class="code-sample" data-bind="text: statement"></td>
                <td data-bind="text: comments"></td>
            </tr>
        </tbody>
    </table>

    <h3><g:message code="invoiceTemplate.parameters.subReport.variables.title"/></h3>

    <div class="details">
        <p>
            <g:message code="invoiceTemplate.parameters.subReport.variables"/>
            <g:message code="invoiceTemplate.parameters.subReport.variables.limitations"/>
        </p>
    </div>

    <h3><g:message code="invoiceTemplate.parameters.cellContent"/></h3>

    <p>
        <g:message code="invoiceTemplate.parameters.additionalFields.details"/>
    </p>

    <h3><g:message code="invoiceTemplate.parameters.javaExpressions"/></h3>

    <p>
        <g:message code="invoiceTemplate.parameters.java.details"/>
    </p>

</div>

<g:javascript>
    $(function () {
        var model = {
            invoiceParameters: ko.observableArray($.map([
                ['Account Charges Product Category ID', 'account_charges_product_category_id'],
                ['BASE_DIR', 'BASE_DIR'],
                ['Billing Date', 'billing_date'],
                ['Billing Period End Date', 'billing_period_end_date'],
                ['Currency Symbol', 'currency_symbol'],
                ['Customer Notes', 'customer_notes'],
                ['Format Utility', 'format_util'],
                ['Invoice Create Date/Time', 'invoice_create_datetime'],
                ['Invoice Due Date', 'invoice_dueDate'],
                ['Invoice ID', 'invoice_id'],
                ['Invoice Line with Tax ID', 'invoice_line_tax_id'],
                ['Invoice Notes', 'invoice_notes'],
                ['Invoice Number', 'invoice_number'],
                ['Invoice Status', 'invoice_status'],
                ['Invoice User ID', 'invoice_user_id'],
                ['Balance', 'invoice_number'],
                ['Carried Balance', 'carried_balance'],
                ['Total Due as of Invoice Date', 'total_due_as_of_invoice_date'],
                ['Total Paid', 'total_paid'],
                ['Total Paid With Carried', 'total_paid_with_carried'],
                ['Message 1', 'message1'],
                ['Message 2', 'message2'],
                ['Other Charges And Credits Product Category ID', 'other_charges_and_credits_product_category_id'],
                ['Owner City', 'owner_city'],
                ['Owner Company', 'owner_company'],
                ['Owner Country', 'owner_country'],
                ['Owner Email', 'owner_email'],
                ['Owner Meta-Field', '__owner__<meta_field_name>'],
                ['Owner Phone', 'owner_phone'],
                ['Owner State/Province', 'owner_state'],
                ['Owner Street Address', 'owner_street_address'],
                ['Owner Zip/Post Code', 'owner_zip'],
                ['Payment Terms', 'payment_terms'],
                ['Receiver City', 'receiver_city'],
                ['Receiver Company', 'receiver_company'],
                ['Receiver Country', 'receiver_country'],
                ['Receiver Email', 'receiver_email'],
                ['Receiver ID', 'receiver_id'],
                ['Receiver Meta-Field', '__receiver__<meta_field_name>'],
                ['Receiver Name', 'receiver_name'],
                ['Receiver Phone', 'receiver_phone'],
                ['Receiver State/Province', 'receiver_state'],
                ['Receiver Street Address', 'receiver_street_address'],
                ['Receiver Zip/Post Code', 'receiver_zip'],
                ['Report Locale', 'REPORT_LOCALE'],
                ['Entity ID', 'entity_id'],
                ['Sales Tax', 'sales_tax'],
                ['Sub Total', 'sub_total'],
                ['Sub Account List', 'sub_account_list'],
                ['SUBREPORT_DIR', 'SUBREPORT_DIR'],
                ['Tax Price', 'tax_price'],
                ['Tax Amount', 'tax_amount'],
                ['Numeric Total', 'numeric_total'],
                ['Total  Without Carried', 'total_without_carried'],
                ['Total', 'total'],
                ['Total Tax Amount', 'total_tax_amount'],
                ['Total Gross Amount', 'total_gross_amount']
            ], function (source) {
                return {
                    name: source.length > 0 ? source[0] : '',
                    statement: source.length > 1 ? '$P{' + source[1] + '}' : ''
                }
            })),
            dynamicInvoiceParameters: ko.observableArray($.map([
                <% session["itg_dynamic_parameters"]?.entrySet()?.each {
    out << raw("['${it.key.replace("_", " ").replace(".", " ").split(" ").collect { it.capitalize() }.join(" ")}','$it.key'],")
}
%>],
            function (source) {
                return {
                    name: source.length > 0 ? source[0] : '',
                    statement: source.length > 1 ? '$P{' + source[1] + '}' : ''
                }
            })),
            invoiceVariables: ko.observableArray($.map([
                ['Column Count', 'COLUMN_COUNT', 'Contains the current number of records that have been processed during the current column creation.'],
                ['Column Number', 'COLUMN_NUMBER', 'Contains the current number of columns.'],
                ['Page Count', 'PAGE_COUNT', 'Contains the current number of records that have been processed in the current page.'],
                ['Page Number', 'PAGE_NUMBER', 'Contains the current number of pages. At "report" time, this variable will contain the total number of pages.'],
                ['Report Count', 'REPORT_COUNT', 'Contains the current number of records that have been processed.'],
            ], function (source) {
                return {
                    name: source.length > 0 ? source[0] : '',
                    statement: source.length > 1 ? '$V{' + source[1] + '}' : '',
                    comments: source.length > 2 ? source[2] : ''
                }
            }))
        };

    ko.applyBindings(model, $('#list-template-parameters').get(0));
});
</g:javascript>
