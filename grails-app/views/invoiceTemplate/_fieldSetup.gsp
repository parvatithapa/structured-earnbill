<g:javascript src="knockout-min.js"/>

<style type="text/css">
#field-setup {
    display: none;
}

#field-setup button.add-field {
    float: right;
}

#field-setup label {
    vertical-align: middle;
}

#field-setup label > select {
    max-width: 150px;
    min-width: 150px;
    width: 150px;
}

#field-setup select {
    min-width: 0;
}

#field-setup table {
    width: 100%;
}

#field-setup td.action > button {
    width: 20px;
}

#field-setup td.format > select {
    min-width: 115px;
}

#field-setup td.title {
    max-width: 155px;
}

#field-setup td.title > input {
    max-width: 150px;
}

#field-setup td.width {
    max-width: 80px;
}

#field-setup td.width > input {
    max-width: 75px;
}

.invoice-template-msg-box {
    display: none;
    height: auto;
}
</style>

<div id="field-setup">
    <div class="invoice-template-msg-box msg-box error">
        %{--<img src="${resource(dir: 'images', file: 'icon14.gif')}" alt="${message(code: 'error.icon.alt', default: 'Error')}"/>--}%
        <strong><g:message code="flash.error.title"/></strong>

        <p></p>
    </div>

    <label>
        <g:message code="invoiceTemplate.fieldSetup.sortCriteria"/>
        <select data-bind="options: usedFields, optionsText: 'description', optionsCaption: 'NONE', value: sortCriterion"></select>
    </label>
    <br/>
    <label>
        <g:message code="invoiceTemplate.fieldSetup.groupCriteria"/>
        <select name="sortCriteriaSelect"
                data-bind="options: usedFields, optionsText: 'description', optionsCaption: 'NONE', value: groupCriteria"></select>
    </label>
    <label>
        <g:message code="invoiceTemplate.fieldSetup.additionalGroupCriteria"/>
        <select name="additionalSortCriteriaSelect"
                data-bind="options: usedFields, optionsText: 'description', optionsCaption: 'NONE', value: addGroupCriteria"></select>
    </label>
    <button class="add-field" data-bind="click: addNewColumn, jqButton: true" data-icon="ui-icon-plusthick"
            data-show-text="data-show-text"><g:message code="invoiceTemplate.fieldSetup.addField"/></button>
    <table>
        <thead>
        <tr>
            <th class="first"><g:message code="invoiceTemplate.fieldSetup.table.show"/></th>
            <th><g:message code="invoiceTemplate.fieldSetup.table.title"/></th>
            <th><g:message code="invoiceTemplate.fieldSetup.table.field"/></th>
            <th><g:message code="invoiceTemplate.fieldSetup.table.width"/></th>
            <th><g:message code="invoiceTemplate.fieldSetup.table.format"/></th>
            <th><g:message code="invoiceTemplate.fieldSetup.table.alignment"/></th>
            <th class="last"></th>
        </tr>
        </thead>
        <tbody data-bind="foreach: columns">
        <tr>
            <td><input type="checkbox" data-bind="checked: show"/></td>
            <td class="title"><input type="text" data-bind="value: title"/></td>
            <td><select name="field_select"
                    data-bind="options: $root.fields, optionsText: 'description', value: field, event:{ change: $root.setTitleName}"></select>
            </td>
            <td class="width"><input type="number" min="0" step="1" data-bind="value: width"/></td>
            <td class="format"><select data-bind="options: formatters, value: formatter"></select></td>
            <td><select data-bind="options: $root.alignments, value: alignment"></select></td>
            <td class="action" style="min-width: 125px">
                <button data-bind="click: $root.moveUp, jqButton: ($root.columns.indexOf($data) > 0)"
                        data-icon="ui-icon-arrow-1-n"></button>
                <button data-bind="click: $root.moveDown, jqButton: ($root.columns.indexOf($data) < ($root.columns().length - 1))"
                        data-icon="ui-icon-arrow-1-s"></button>
                <button data-bind="click: $root.removeColumn, jqButton: true" data-icon="ui-icon-cancel"></button>
            </td>
        </tr>
        </tbody>
        <tfoot></tfoot>
    </table>
</div>

<g:javascript>

    (function () {

        $.widget('jb.fieldSetup', {
            _create: function () {
                var i, format, self = this;

                this._formatters = [];
                for (i = 0; i < cif_formats.length; i++) {
                    format = cif_formats[i];
                    this._formatters.push(new Formatter(format.types, format.variants));
                }

                $(this.element).dialog({
                    autoOpen: false,
                    buttons: {
                        Save: function () {
                            var sortCriteria = $(this).find("[name='sortCriteriaSelect'] :selected").html();
                            var additionalSortCriteria = $(this).find("[name='additionalSortCriteriaSelect'] :selected").html();
                            var selectedFields = $("[name='field_select'] :selected").text();
                            if ((sortCriteria == additionalSortCriteria) && (sortCriteria != "NONE")) {
                                $('.invoice-template-msg-box.error p').html("${message(code: 'invoiceTemplate.fieldSetup.error.sameGroups')}");
                                $('.invoice-template-msg-box.error').show();
                            } else {
                                if (selectedFields.indexOf("GROUP_COUNT") >= 0 && sortCriteria == "NONE") {
                                    $('.invoice-template-msg-box.error p').html("${message(code: 'invoiceTemplate.fieldSetup.error.group.count.error')}");
                                    $('.invoice-template-msg-box.error').show();
                                } else {
                                    self._trigger('save', null, ko.toJS(self._model));
                                    $('.invoice-template-msg-box.error').hide();
                                    $(this).dialog('close');
                                }
                            }
                        },
                        Discard: function () {
                            $(this).dialog('close');
                        }
                    },
                    height: 350,
                    modal: true,
                    resizable: true,
                    title: 'Field Setup',
                    width: 860
                });

                $.getJSON('../fields', function (data) {
                    function wrapFields(fields) {
                        var i, result = [];
                        for (i = 0; i < fields.length; i++) {
                            result.push(new Field(fields[i]));
                        }
                        return result;
                    }

                    self._fields.EventLines(wrapFields(data.eventFields));
                    self._fields.InvoiceLines(wrapFields(data.invoiceFields));
                });

                this._model = new FieldSetupModel(this._formatters, this._alignments);
                ko.applyBindings(this._model, this.element.get(0));
            },
            _alignments: ['LEFT', 'CENTER', 'RIGHT', 'JUSTIFIED'],
            _fields: {
                InvoiceLines: ko.observableArray([]),
                EventLines: ko.observableArray([])
            },
            /**
             * @param type {String} type of fields to show ('InvoiceLines' or 'EventLines')
             * @param data
             */
            open: function (type, data) {
                var i, rf, reqFields = {columns: [], sortCriterion: null, groupCriteria: null, addGroupCriteria: null};
                this.element.dialog('open');
                this._model.fields(this._fields[type]());
                for (rf in reqFields) {
                    if (typeof data[rf] === 'undefined' || typeof data[rf] !== 'object') {
                        data[rf] = reqFields[rf];
                    }
                }
                this._model.columns([]);
                for (i = 0; i < data.columns.length; i++) {
                    var field = data.columns[i];
                    this._model.addColumn(field.field, field.title, field.width, field.formatter, field.show, field.alignment);
                }
                if (typeof data.sortCriterion !== 'undefined' && data.sortCriterion != null)
                    this._model.sortCriterion(this._model.findField(data.sortCriterion.name));
                if (typeof data.groupCriteria !== 'undefined' && data.groupCriteria != null)
                    this._model.groupCriteria(this._model.findField(data.groupCriteria.name));
                if (typeof data.addGroupCriteria !== 'undefined' && data.addGroupCriteria != null)
                    this._model.addGroupCriteria(this._model.findField(data.addGroupCriteria.name));
            }
        });

        ko.bindingHandlers.jqButton = {
            init: function (element) {
                var el = $(element);
                el.button({
                    icons: {
                        primary: el.attr('data-icon')
                    },
                    text: el.is('[data-show-text]')
                });
            },
            update: function (element, valueAccessor) {
                var currentValue = valueAccessor();
                // Here we just update the "disabled" state, but you could update other properties too
                $(element).button("option", "disabled", currentValue === false);
            }
        };

        $(function () {
            $('#field-setup').fieldSetup();
        });

        function formattersForType(formatters, type) {
            var result = [];
            for (var i = 0; i < formatters.length; i++) {
                var formatter = formatters[i];
                if ($.inArray(type, formatter.types) >= 0) {
                    result = result.concat(formatter.variants);
                }
            }
            return result;
        }

        /**
         * @param types {Array, Object} array of full qualified Java types, which can use this formatter
         * @param variants {Array} array of string patterns, which are available in this formatter
         * @constructor
         */
        function Formatter(types, variants) {
            if ($.isArray(types)) {
                this.types = types;
                this.variants = variants;
            } else {
                $.extend(this, types);
            }
        }

        function Field(name, description, valueClass) {
            if (typeof name === 'object') {
                $.extend(this, name);
            } else {
                this.name = name;
                this.description = description;
                this.valueClass = valueClass;
            }
        }

        /**
         * @param field {Field} field shown in this column
         * @param title {String}
         * @param width {Number}
         * @param formatters {Array} ref to all available formatters
         * @param formatter {String} selected format for column
         * @param show {Boolean}
         * @param alignment {String}
         * @constructor
         */
        function Column(field, title, width, formatters, formatter, show, alignment) {

            var self = this;

            this.field = ko.observable(field);
            this.title = ko.observable(title);
            this.width = ko.observable(width);
            this.formatter = ko.observable(formatter);
            this.formatters = ko.computed(function () {
                return formattersForType(formatters, self.field().valueClass);
            });
            this.show = ko.observable(show);
            this.alignment = ko.observable(alignment);
        }

        /**
         * @param formatters {Array}
         * @param alignments {Array}
         * @constructor
         */
        function FieldSetupModel(formatters, alignments) {

            var self = this;

            this.columns = ko.observableArray([]);
            this.fields = ko.observableArray([]);
            this.formatters = formatters;
            this.alignments = alignments;

            this.sortCriterion = ko.observable();
            this.groupCriteria = ko.observable();
            this.addGroupCriteria = ko.observable();

            this.usedFields = ko.computed(function () {
                var i, field, columns = self.columns(), result = [];
                for (i = 0; i < columns.length; i++) {
                    field = columns[i].field();
                    if ($.inArray(field, result) < 0) {
                        result.push(field);
                    }
                }
                return result;
            });

            this.findField = function (name) {
                var fields = self.fields();
                for (var i = 0; i < fields.length; i++) {
                    if (name === fields[i].name) {
                        return fields[i];
                    }
                }
                return null;
            };
            /**
             * @param field {String, Field}
             * @param title {String}
             * @param width {Number}
             * @param formatter {String}
             * @param show {Boolean}
             * @param alignment {String}
             */
            this.addColumn = function (field, title, width, formatter, show, alignment) {
                field = self.findField(typeof field === 'string' ? field : field.name);
                self.columns.push(new Column(field, title, width, self.formatters, formatter, show, alignment));
            };
            this.addNewColumn = function () {
                var firstField = self.fields()[0];
                self.addColumn(firstField, firstField.description, 100, formatters[0], true, 'LEFT');
            };
            this.removeColumn = function (data) {
                self.columns.remove(data);
            };
            this.moveUp = function (column) {
                var i = self.columns.indexOf(column);
                if (i > 0) {
                    var el = self.columns()[i - 1];
                    self.columns.replace(column, el);
                    self.columns.replace(el, column);
                }
            };
            this.moveDown = function (column) {
                var i = self.columns.indexOf(column);
                if (i >= 0 && i < self.fields().length - 1) {
                    var el = self.columns()[i + 1];
                    self.columns.replace(el, column);
                    self.columns.replace(column, el);
                }
            };
            this.setTitleName = function (column) {
                column.title(column.field().description);
            };
        }

    }());
</g:javascript>
