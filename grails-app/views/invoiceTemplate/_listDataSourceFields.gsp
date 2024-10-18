<g:javascript src="knockout-min.js"/>

<div id="list-data-source-fields" class="samples-dialog">

    <style type="text/css" scoped="scoped">
    #list-data-source-fields {
        display: none;
    }

    #list-data-source-fields table {
        width: 100%;
        padding-top: 5px;
        padding-bottom: 15px;
        font-size: smaller;
    }

    #list-data-source-fields table tr,
    #list-data-source-fields table th,
    #list-data-source-fields table td {
        border-collapse: collapse;
        border: solid lightgray 1px;
    }

    #list-data-source-fields .code-sample {
        font-family: monospace;
    }

    #list-data-source-fields p.details {
        font-size: smaller;
    }

    </style>

    <h3>Asset Fields</h3>

    <p class="details">
        Assets are collected from orders attached to the invoice's orders (processed orders).
    </p>

    <table id="asset-fields-table">
        <thead>
        <tr>
            <th>Field Name</th>
            <th>Field Statement</th>
            <th>Comments</th>
        </tr>
        </thead>
        <tbody data-bind="foreach: assetFields">
        <tr>
            <td data-bind="text: name"></td>
            <td class="code-sample" data-bind="text: statement"></td>
            <td data-bind="text: comments"></td>
        </tr>
        </tbody>
    </table>

    <p class="details">
        Since number and names of meta-fields are asset-type-specific, all possible values are not listed here.
        To reference a meta-field a special notation can be used. Each meta-field is converted into special asset's field,
        which name starts with double underscore signs, also all spaces and dots are replaced with single underscore,
        letters' case will be preserved. E.g., we have "Tax Exemption Code" meta-field, then we can reference it using
        <span class="code-sample">$F{__Tax_Exemption_Code}</span> statement.
    </p>

    <h3>Invoice Lines Fields</h3>

    <table id="invoice-lines-fields">
        <thead>
        <tr>
            <th>Field Name</th>
            <th>Field Statement</th>
        </tr>
        </thead>
        <tbody data-bind="foreach: invoiceLinesFields">
        <tr>
            <td data-bind="text: name"></td>
            <td class="code-sample" data-bind="text: statement"></td>
        </tr>
        </tbody>
    </table>

    <h3>CDR Events Fields</h3>

    <table id="cdr-events-fields">
        <thead>
        <tr>
            <th>Field Name</th>
            <th>Field Statement</th>
        </tr>
        </thead>
        <tbody data-bind="foreach: cdrEventsFields">
        <tr>
            <td data-bind="text: name"></td>
            <td class="code-sample" data-bind="text: statement"></td>
        </tr>
        </tbody>
    </table>

</div>

<g:javascript>
    (function ($) {

        $(function () {

            var model = {
                assetFields: ko.observableArray($.map([
                    ["Asset Status ID", "assetStatusId", "Asset status identifier in DB"],
                    ["Asset Type", "assetType", "Asset (product category) type"],
                    ["Create Date Time", "createDatetime", "Date/Time of asset's creation"],
                    ["Deleted", "deleted", "Whether the asset is deleted"],
                    ["Entity ID", "entityId", "Entity (company) identifier in DB"],
                    ["Group ID", "groupId", "Identifier of asset group"],
                    ["ID", "id", "Asset identifier in DB"],
                    ["Identifier", "identifier", "Asset text identifier (regardless actual label)"],
                    ["Identifier Label", "identifierLabel", "Label used for asset identifier"],
                    ["Item ID", "itemId", "Item (product) identifier in DB"],
                    ["Meta-Field", "__<meta_field_name>", "Meta-field data (see disclaimer below)"],
                    ["Notes", "notes", "Any notes applied to asset"],
                    ["Order Line ID", "orderLineId", "Order line identifier"],
                    ["Status", "status", "Asset Status"]
                ], function (source) {
                    return {
                        name: source.length > 0 ? source[0] : '',
                        statement: source.length > 1 ? '$F{' + source[1] + '}' : '',
                        comments: source.length > 2 ? source[2] : ''
                    }
                })),
                invoiceLinesFields: ko.observableArray([]),
                cdrEventsFields: ko.observableArray([])
            };

            ko.applyBindings(model, $('#list-data-source-fields').get(0));

            $.getJSON('../fields', function (data) {
                function wrapFields(fields) {
                    var i, field, result = [];
                    for (i = 0; i < fields.length; i++) {
                        field = fields[i];
                        if (field.type === 'Field') {
                            result.push({
                                name: field.description,
                                statement: '$F{' + field.name + '}'
                            });
                        }
                    }
                    return result;
                }

                model.cdrEventsFields(wrapFields(data.eventFields));
                model.invoiceLinesFields(wrapFields(data.invoiceFields));
            });
        });
    })(jQuery);
</g:javascript>