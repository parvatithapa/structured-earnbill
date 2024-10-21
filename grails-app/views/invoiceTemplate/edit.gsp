<html>
<head>
    <meta name="layout" content="main"/>

    <r:require module="itg"/>
</head>

<body>
<g:javascript src="jquery.jstree.js"/>
<g:javascript src="alpaca.min.js"/>
<g:javascript src="evol.colorpicker.min.js"/>

<g:javascript>

    jQuery.fn.addBack = jQuery.fn.andSelf; //Hack to make jsTree compatible with old JQuery

    $jb = {}; // container for global JB vars

    var TemplateAction = {
        APPLY:'${com.sapienter.jbilling.client.util.Constants.APPLY}',
        SAVE_VERSION:'${com.sapienter.jbilling.client.util.Constants.SAVE_VERSION}',
        action:this.APPLY,
        isNew:${isNew ?: false},
        setApplyAction:function(){
            this.action = this.APPLY;
        },
        setSaveVersionAction:function(){
            this.action = this.SAVE_VERSION;
        },
        newVersionCreated:function(){
            this.isNew = true;
        }
    };

    (function () {

    var reportJson, selected = null, treeId = 0, movableKinds = ['Image', 'TextBox', 'List', 'Section', 'SubReport'];
    var alpacaDefault = {
                            ui: "jquery-ui",
                            view: "VIEW_JQUERYUI_EDIT",
                            options: {
                                fields: {
                                    sqlFields: { type: "hidden"},
                                    sections: { type: "hidden"},
                                    elements: { type: "hidden"},
                                    content: { type: "hidden"},
                                    header: { type: "hidden"},
                                    pageHeader: { type: "hidden"},
                                    footer: { type: "hidden"},
                                    pageFooter: { type: "hidden"},
                                    id: { type: "hidden"},
                                    kind: { type: "hidden"},
                                    expr: { type: "textarea", helper: "<a href='javascript:showListTemplateParameters()' style='color: #009abf !important;'>Available parameters</a>"},
                                    sortBy: { helper: "Data source field names (e.g., 'assetType') to sort by. Use comma to separate multiple fields."},
                                    color: {fieldClass: "colorpicker", size: 16},
                                    borderColor: {fieldClass: "colorpicker", size: 16},
                                    bgColor: {fieldClass: "colorpicker", size: 16},
                                    headerBgColor: {fieldClass: "colorpicker", size: 16},
                                    groupBgColor: {fieldClass: "colorpicker", size: 16},
                                    addGroupBgColor: {fieldClass: "colorpicker", size: 16},
                                    contentBgColor: {fieldClass: "colorpicker", size: 16},
                                    recordSeparationColor: {fieldClass: "colorpicker", size: 16},
                                    font: {fields:{color: {fieldClass: "colorpicker", size: 16}}},
                                    contentCellStyle: { fields: {horizontalBorderColor: {fieldClass: "colorpicker", size: 16},verticalBorderColor: {fieldClass: "colorpicker", size: 16}}},
                                    headerFont: {fields:{color: {fieldClass: "colorpicker", size: 16}}},
                                    headerCellStyle: { fields: {horizontalBorderColor: {fieldClass: "colorpicker", size: 16},verticalBorderColor: {fieldClass: "colorpicker", size: 16}}},
                                    groupHeaderFont: {fields:{color: {fieldClass: "colorpicker", size: 16}}},
                                    addGroupHeaderFont: {fields:{color: {fieldClass: "colorpicker", size: 16}}},
                                    imageSource: {
                                        type: "select"
                                    },
                                    imageUrl: {
                                        dependencies: {
                                            imageSource: "URL"
                                        },
                                        hideInitValidationError: true,
                                        allowOptionalEmpty: true,
                                        validator: function(control, callback) {
                                            var controlVal = control.getValue(),
                                                /* copy from original alpaca regexp for urls */
                                                urlPattern = /^(http|https):\/\/[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(\:[0-9]{1,5})?(([0-9]{1,5})?\/.*)?$/i,
                                                dataImagePattern = /^data:image/;
                                            if (controlVal.length == 0 || urlPattern.test(controlVal) || dataImagePattern.test(controlVal)) {
                                                callback({ message: "", status: true });
                                            } else {
                                                callback({ message: "The URL provided is not a valid web address nor image data source", status: false });
                                            }
                                        }
                                    },
                                    imageFile: {
                                        type: "file",
                                        dependencies: {
                                            imageSource: "File"
                                        },
                                        allowOptionalEmpty: true
                                    },
                                    alignment: {
                                        emptySelectFirst: true,
                                        optionLabels: {
                                            LEFT: "Left",
                                            CENTER: "Center",
                                            RIGHT: "Right",
                                            JUSTIFIED: "Justified"
                                        }
                                    },
                                    evaluationTime: {
                                        emptySelectFirst: true
                                    },
                                    columns: {
                                        readonly: true,
                                        hidden: true
                                    },
                                    source: {
                                        emptySelectFirst: true,
                                        optionLabels: {
                                            InvoiceLines: "Invoice Lines",
                                            CDREvents: "CDR Events",
                                            Assets: "Assets"
                                        },
                                        type: "select",
                                        helper: "See <a href='javascript:showListDataSourceFields()'>data source fields</a> for details"
                                    },
                                    orientation: {
                                        emptySelectFirst: true,
                                        optionLabels: {
                                            Horizontal: "Horizontal",
                                            Vertical: "Vertical"
                                        },
                                        type: "select"
                                    },
                                    subReportDataSourceType: {
                                        emptySelectFirst: true,
                                        optionLabels: {
                                            EMPTY: "Empty Source",
                                            JBILLING: "jBilling Database",
                                            XML: "XML File (*)",
                                            CSV: "CSV File (*)"
                                        },
                                        type: "select"
                                    },
                                    filterExpr: { type: "textarea", helper: "Any expression, which can be evaluated as boolean value. <a href='javascript:showFilterCriteriaSample()'>See example</a>" }
                                },
                                size: 20,
                                renderForm: true,
                                form: {
                                    attributes: {
                                        id: "json_gen_form"
                                    }
                                }
                            }
                        };

    // do not display starts for required field (override default function)
    Alpaca.styleInjections["jquery-ui"].required = function(targetDiv) {};

    function openState(id, isOpen) {
        if (typeof $jb.openState === 'undefined') {
            $jb.openState = {};
        }
        if (typeof isOpen === 'undefined') { // get current state
            return typeof $jb.openState[id] === 'undefined' ? true : $jb.openState[id];
        } else { // save state
            $jb.openState[id] = isOpen;
        }
    }

    function treeElementState(id, kind) {
        var extendableKinds = ['DocDesign', 'Band', 'Section', 'List'];
        return $.inArray(kind, extendableKinds) >= 0 ? (openState(id) ? 'open' : 'close') : 'close';
    }

    /**
    * Converts json to jsTree compatible structure
    * @param {Object} json
    * @returns {Array} js objects tree
    */
    function loadJson(json) {
        reportJson = {
            json: json,
            idmap: {}
        };
        treeId = typeof json.id === 'number' ? json.id : 0;
        var treeJson = {
                data: {
                    title: json.name,
                    icon: "DocDesign"
                },
                state: treeElementState(treeId, 'DocDesign'),
                attr: {
                    j_id: treeId,
                    j_schema_id: "DocDesign",
                    appendable: "Section,Band",
                    deletableChildren: "true"
                },
                children: []
            };
        reportJson.idmap[treeId] = json;
        for (var i = 0; i < json.sections.length; i++) {
            json.sections[i].kind = 'Section';
        }
        processBand("Page Header", treeJson.children, json.pageHeader, reportJson.idmap);
        collectTemplateTree(json.sections, reportJson.idmap, treeJson.children);
        processBand("Page Footer", treeJson.children, json.pageFooter, reportJson.idmap);
        processSQLFields(treeJson.children, json.sqlFields, reportJson.idmap);
        if (treeJson.children.length == 0) {
            treeJson.children = null;
            treeJson.state = null;
        }
        return [treeJson];
    }

    function collectTemplateTree(elements, idmap, treeStructure) {
        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];
            idmap["" + (++treeId)] = element;
            var treeElement = {
                data: {
                    title: element.name,
                    icon: element.kind
                },
                attr: {
                    j_id: treeId,
                    j_schema_id: element.kind,
                    copyable: (element.hasOwnProperty('copyable') ? element.copyable : true),
                    deletable: (element.hasOwnProperty('deletable') ? element.deletable : true),
                    movable: $.inArray(element.kind, movableKinds) >= 0
                },
                state: treeElementState(treeId, element.kind)
            };
            treeStructure.push(treeElement);


            switch(element.kind){
                case "Section":
                    treeElement.children = [];
                    processBand("Header", treeElement.children, element.header, idmap);

                    if (element.content != null) {
                        idmap["" + (++treeId)] = element.content;
                        treeElement.children.push({data: {
                                        title: element.content.name,
                                        icon: element.content.kind
                                    },
                                    attr: {
                                        j_id: treeId,
                                        j_schema_id: element.content.kind,
                                        copyable: true,
                                        deletable: true,
                                        movable: $.inArray(element.kind, movableKinds) >= 0
                                    }});
                    } else if (element.subReport != null) {
                        idmap["" + (++treeId)] = element.subReport;
                        treeElement.children.push({data: {
                                        title: element.subReport.name,
                                        icon: element.subReport.kind
                                    },
                                    attr: {
                                        j_id: treeId,
                                        j_schema_id: element.subReport.kind,
                                        copyable: true,
                                        deletable: true,
                                        movable: $.inArray(element.kind, movableKinds) >= 0
                                    }});
                    } else {
                        treeElement.attr.appendable = "InvoiceLines,EventLines,SubReport"
                    }

                    processBand("Footer", treeElement.children, element.footer, idmap);
                    break;
                case 'List':
                    if (typeof element.noDataText === 'undefined' || element.noDataText == null) {
                        element.noDataText = cif_schemas.List.stub.noDataText;
                    }
                    $.extend(true, element.noDataText, {
                        kind: 'Text',
                        copyable: false,
                        deletable: false
                    });
                    treeElement.children = [];
                    collectTemplateTree([element.noDataText], idmap, treeElement.children);
                    processBand("Cell Content", treeElement.children, element, idmap, "TextBox");
                    break;
            }
        }
    }

    function processSQLFields(treeStructure, elements, idmap) {
       // set the blank object for band. All the sql fields are visible under that tab
        var j_id = ("" + (++treeId)), treeElement = {
            data: {
                title: "SQL Fields",
                icon: "Band"
            },
            attr: {
                j_id: j_id,
                appendable: "SQLField"
            },
            state: treeElementState(j_id, 'Band'),
            children: []
        };
        idmap[treeElement.attr.j_id] = elements;
        treeStructure.push(treeElement);
        for (var i = 0; i < elements.length; i++) {
            var element = elements[i];
            if (element != null) {
                  if(idmap!=null) idmap["" + (++treeId)] = element;
                       treeElement.children.push({data: {
                                        title: element.name,
                                        icon: "Section"
                                    },
                                    attr: {
                                        j_id: treeId,
                                        j_schema_id: "SQLField",
                                        copyable: true,
                                        deletable: true,
                                        movable: false
                                    }});
            }
        }
        if (treeElement.children.length == 0) {
            treeElement.children = null;
            treeElement.state = null;
        }
    }

    function processBand(name, treeStructure, band, idmap, appendable) {
        var j_id = ("" + (++treeId)), treeElement = {
            data: {
                title: name,
                icon: "Band"
            },
            attr: {
                j_id: j_id,
                appendable: (typeof appendable == 'undefined' ? "Image,TextBox,List,SubReport" : appendable)
            },
            state: treeElementState(j_id, 'Band'),
            children: []
        };
        idmap[treeElement.attr.j_id] = band;
        treeStructure.push(treeElement);
        if (band != null) {
            collectTemplateTree(band.elements, idmap, treeElement.children)
        }
        if (treeElement.children.length == 0) {
            treeElement.children = null;
            treeElement.state = null;
        }
    }

    function treeReload() {
        saveJson(function () {
            var $tree = $("#tree");

            $jb.openState = {};
            $tree.find('li[j_id]').each(function (id, el) {
                var $el = $(el);
                openState($el.attr('j_id'), $el.is('.jstree-open'));
            });

            $tree.jstree("refresh");
            $tree.jstree("deselect_all");
            $("#tree-add").attr("disabled", "disabled");
            $("#tree-del").attr("disabled", "disabled");
            $("#tree-copy").attr("disabled", "disabled");
            $("#tree-paste").attr("disabled", "disabled");
            $("#tree-up").attr("disabled", "disabled");
            $("#tree-down").attr("disabled", "disabled");
        });
        $('#json-form').empty()
    }
    function deleteLeaf() {
        var isLastSection =  reportJson.json.sections.length == 1 && reportJson.piece.kind === 'Section';
        if (isLastSection) {
            $('#dialog-delete-error').dialog({
                resizable: true,
                height: 240,
                modal: true,
                buttons: {
                    "Ok": function () {
                        $(this).dialog("close");
                    }
                }
            });
        } else {
            $('#dialog-delete-confirm').dialog({
                resizable: false,
                height: 240,
                modal: true,
                buttons: {
                    "Delete item": function () {
                        $(this).dialog("close");
                        var parent = reportJson.parent;
                        var piece = reportJson.piece;
                        if (parent.content === piece) {
                            parent.content = null;
                        } else if (parent.subReport == piece) {
                            parent.subReport = null;
                        } else {
                            var fold = parent.elements == null ? parent.sections : parent.elements;
                            if(piece.kind=="SQLField") fold = parent;
                            for (var i = 0; i < fold.length; i++) {
                                if (fold[i] === piece) {
                                    fold.splice(i, 1);
                                    break;
                                }
                            }
                        }

                        treeReload();
                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        }
    }
    function addLeaf() {
        $('#dialog-add-new-element').dialog({
            resizable: false,
            height: 285,
            modal: true,
            buttons: {
                "Cancel": function () {
                    $(this).dialog("close");
                }
            }
        });
    }
    function lookupId(piece) {
        var map = reportJson.idmap ;
        for (var p in map) {
            if (map.hasOwnProperty(p) && map[p] == piece)
                return parseInt(p);
        }
        return NaN;
    }
    function subElements(parent) {
        var i, holder, holders = ['elements', 'sections'];
        if (parent) {
            for (i = 0; i < holders.length; i++) {
                holder = holders[i];
                if (parent.hasOwnProperty(holder)) {
                    return parent[holder];
                }
            }
        }
        return [];
    }
    function moveLeafUp() {
        var piece = reportJson.piece,
            elements = subElements(reportJson.parent),
            currPos = $.inArray(piece,  elements),
            id = lookupId(piece);
        if (currPos > 0) {
            elements[currPos] = elements[currPos - 1];
            elements[currPos - 1] = piece;
            selected = $('[j_id="' + (id - 1) + '"]');

            treeReload();
        }
    }
    function moveLeafDown() {
        var piece = reportJson.piece,
            elements = subElements(reportJson.parent),
            currPos = $.inArray(piece,  elements),
            id = lookupId(piece);
        if (currPos >= 0 && currPos < elements.length - 1) {
            elements[currPos] = elements[currPos + 1];
            elements[currPos + 1] = piece;
            selected = $('[j_id="' + (id + 1) + '"]');

            treeReload();
        }
    }
    function appendElementToTree(newElement) {
        var piece = reportJson.piece;
        if (piece.kind == "Section") {
            if(newElement.kind == "SubReport") {
                piece.subReport = newElement
            } else {
                piece.content = newElement
            }
        } else if (piece.elements != null) {
            piece.elements.push(newElement)
        } else {
            if(newElement.kind=='SQLField'){
                var test = reportJson.parent;
                if(test.sqlFields!=null) test.sqlFields.push(newElement);
                else test.push({"sqlFields":[newElement]});
            }else{
                piece.sections.push(newElement);
            }
        }
        treeReload();
    }
    function addInvoiceElement(elementType) {
        var newElement = JSON.parse(JSON.stringify(cif_schemas[elementType].stub)); // Cloning
        $('#dialog-add-new-element').dialog("close");
        appendElementToTree(newElement);
    }
    function copyLeaf() {
        reportJson.clipboard = reportJson.piece
    }
    function pasteLeaf() {
        //Clone element
        if (reportJson.clipboard == null) return;
        var newElement = JSON.parse(JSON.stringify(reportJson.clipboard));
        appendElementToTree(newElement);
    }

    function showErrorMsg(msg) {
        var $dialog = $('#dialog-save-error');
        $dialog.dialog({
            resizable: false,
            height: 240,
            modal: true,
            buttons: {
                "Ok": function () {
                    $(this).dialog("close");
                }
            }
        });
        $dialog.find('#save-error-msg').html(msg);
    }

    function saveJson(onsaved) {
        /*if ($('select[name="imageSource"]').val() == 'File') {
            $('form[name="imageFile"]').submit();
        }*/
        $.ajax({type: "POST", url: "../saveTemplate",
            data: {
                json: JSON.stringify(reportJson.json),
                id: "${selected.id}",
                invoice_id: $("#invoice_id").val(),
                tempAction: TemplateAction.action,
                isNew: TemplateAction.isNew
              },
               dataType: "text",
            success: function (data) {
                data = JSON.parse(data);
                if(data.message == 'SAVED'){
                    window.location.href = '${g.createLink(controller: 'invoiceTemplate', action: 'edit')}/'+data.version.id;
                    return;
                }else if(data.message == 'NEW'){
                    TemplateAction.newVersionCreated();
                    window.location.href = '${g.createLink(controller: 'invoiceTemplate', action: 'edit')}/'+data.version.id+'?isNew='+TemplateAction.isNew;
                    return;
                }
                reloadImage();
                if (onsaved) {
                    onsaved();
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                 if(jqXHR.status == "401") {
                      reloadPage();
                      return;
                 }
                if (textStatus == 'error') {
                    if (errorThrown != null && typeof errorThrown == 'string' && errorThrown.length > 0) {
                        if(jqXHR.error) {
                            reloadPage();
                            return;
                        }
                        showErrorMsg(jqXHR.responseText);
                        if (selected != null) {
                            $("#tree").jstree('deselect_all');
                            $("#tree").jstree('select_node','[j_id="' + $(selected).attr('j_id') + '"]');
                        }
                    }
                }
            }
        });
    }

    function reloadPage() {
        location.reload();
    }

    function savePreview() {
        $('#json_gen_form').find('input,select,textarea').each(function () {
            var i, lastPath, pathPart, v = $(this), path = v.attr("name").split("_"), targetObject = reportJson.piece;
            for (i = 0; i < path.length - 1; i++) {
                pathPart = path[i];
                if (typeof targetObject[pathPart] === 'undefined') {
                    targetObject[pathPart] = {};
                }
                targetObject = targetObject[pathPart]
            }
            lastPath = path[path.length - 1];
            switch (v.attr("type")) {
                case "checkbox":
                    targetObject[lastPath] = v.is(':checked');
                    break;
                case "radio":
                    if (v.is(':checked')) {
                        targetObject[lastPath] = v.val();
                    }
                    break;
                default:
                    targetObject[lastPath] = v.val();
            }
        });
        treeReload();
    }

    function shiftPage(shift) {
        var $pageNumber = $('#page-number');
        var pageIndex = parseInt($pageNumber.text()) + shift;

        if (pageIndex <= 1) {
            pageIndex = 1;
            $('#previous-page').hide();
        } else {
            $('#previous-page').show();
        }

        $pageNumber.text(pageIndex);
        reloadImage();
    }

    function alpacaSetting(elementInfo, add) {
        var i, prop, candidate = $.extend(true, {
            schema: elementInfo.schema,
            data: reportJson.piece
        }, alpacaDefault, add);
        if (elementInfo.hasOwnProperty("ignoredProperties")) {
            for (i = 0; i < elementInfo.ignoredProperties.length; i++) {
                prop =  elementInfo.ignoredProperties[i];
                if (candidate.options.fields.hasOwnProperty(prop)) {
                    candidate.options.fields[prop].hidden = true;
                } else {
                    candidate.options.fields[prop] = {hidden: true};
                }
            }
        }
        return candidate;
    }

    $(function () {

        var $tree = $("#tree").jstree({
            core: {"animation": 50},
            ui: {"select_limit": 1},
            plugins: ["themes", "ui", "json_data"],
            themes: {
                "theme": "classic",
                "url": "${resource(dir: 'jquery-ui/jstree/themes/classic', file: 'style.css')}"
            },
            json_data: {
                ajax: {
                    url: "../json/${selected.id}",
                    success: loadJson

                }}}).
                bind("select_node.jstree", function (evt, data) {
                    selected = data.inst.get_selected()[0];
                    reportJson.piece = reportJson.idmap[$.attr(selected, "j_id")];
                    var parentNode = data.inst._get_parent(data.rslt.obj);
                    if (parentNode == -1) {
                        reportJson.parent = null
                    } else {
                        reportJson.parent = reportJson.idmap[parentNode.attr("j_id")]
                    }
                    var schemaId = $.attr(selected, "j_schema_id");
                    var elementInfo = cif_schemas[schemaId];
                    var $jsonform = $("#json-form");
                    $jsonform.empty();
                    if (typeof elementInfo !== 'undefined' && elementInfo != null) {
                        if ($.inArray(schemaId, ['InvoiceLines', 'EventLines']) >= 0) {
                            var setupColumnsBtn = $('<button>').text('Setup Columns').button({
                                icons: {
                                    primary: 'ui-icon-pencil'
                                }
                            }).click(function () {
                                $('#field-setup').one('fieldsetupsave', function (event, data) {
                                    $.extend(reportJson.piece, {
                                        sortCriterion: (typeof data.sortCriterion === 'undefined' ? null : data.sortCriterion),
                                        groupCriteria: (typeof data.groupCriteria === 'undefined' ? null : data.groupCriteria),
                                        addGroupCriteria: (typeof data.addGroupCriteria === 'undefined' ? null : data.addGroupCriteria),
                                        columns: data.columns
                                    });
                                    savePreview();
                                }).fieldSetup('open', schemaId, reportJson.piece);
                            });
                            $tree.bind("select_node.jstree", function () {
                                setupColumnsBtn.remove();
                            });
                            $jsonform.before(setupColumnsBtn);
                        }
                        $jsonform.alpaca(alpacaSetting(elementInfo, {
                            options: {
                                fields: {
                                    imageFile: {
                                        selectionHandler: function (files, data) {
                                            if (files[0].size > 614400) {
                                                showErrorMsg('Image size is too large. 600kB is the maximum allowed size');
                                            } else if (files[0].type.indexOf("image") != 0) {
                                                showErrorMsg('The file you selected is not of a valid image type');
                                            } else {
                                                $jsonform.find('input[name="imageUrl"]').val(data[0]);
                                            }
                                        }
                                    }
                                }
                            }
                        }));
                        // Display checkboxes in a single row
                        $jsonform.find('div.alpaca-controlfield-checkbox').css({
                            'display': 'inline-block',
                            'vertical-align': 'middle',
                            'padding-left': '12px'
                        }).parent().find('div.alpaca-controlfield-label').css({
                            'display': 'inline-block',
                            'vertical-align': 'middle'
                        });
                        $jsonform.find('span.alpaca-controlfield-helper-text').each(function () {
                            $(this).html($(this).text()); // <-- use html inside helpers
                        });
                        $jsonform.find('input[type="file"]').attr('accept', 'image/*');
                        var $colorpicker = $('.colorpicker input');
                        $colorpicker.colorpicker();
                        $colorpicker.width($colorpicker.width() - 20);
                        $colorpicker.each(function (i, e) {
                            $(e).parents('fieldset').eq(0).css('overflow', 'visible');
                        });
                    }
                    $('#tree-del').attr("disabled", "true" == $.attr(selected, "deletable") ? null : "disabled");
                    $('#tree-copy').attr("disabled", "true" == $.attr(selected, "copyable") ? null : "disabled");

                    // If the selected leaf is movable we check if it's the first leaf we should disable the arrow up.
                    // The same if the leaf is the last we disable the arrow down.
                    var piece = reportJson.piece,
                    elements = subElements(reportJson.parent),
                    currPos = $.inArray(piece,  elements);
                    $('#tree-up').attr("disabled", (currPos > 0) ? null : "disabled");
                    $('#tree-down').attr("disabled", (elements != null && currPos >= 0 && currPos < (elements.length - 1)) ? null : "disabled");

                    var $treePaste = $('#tree-paste');
                    $treePaste.attr("disabled", "disabled");
                    var appendable = $.attr(selected, "appendable");
                    if (appendable != null) {
                        $('#tree-add').attr("disabled", null);
                        var allowedChildren = appendable.split(",");
                        var $dialog = $("#dialog-add-new-element");
                        $dialog.empty();
                        for (var i = 0; i < allowedChildren.length; i++) {
                            var allowedChild = allowedChildren[i];
                            if (Object.prototype.hasOwnProperty.call(cif_schemas, allowedChild)) {
                                $("<a href='#' class='submit add new-templateelement-button select-element'>").
                                    text(cif_schemas[allowedChild].schema.title).click((function (element) {
                                        return function () {
                                            return addInvoiceElement(element);
                                        }
                                    } (allowedChild))).appendTo($dialog);
                                if (reportJson.clipboard != null) {
                                    if (reportJson.clipboard.kind == allowedChild) {
                                        $treePaste.attr("disabled", null);
                                    }
                                }
                            }
                        }
                    } else {
                        $('#tree-add').attr("disabled", "disabled");
                    }

                    // When clicking on the Tree element we check for the transparent checkbox. If it's checked we disable the color input and hide the color-picker.
                    var transparentCheckbox = $jsonform.find('input[name="transparent"]');
                    var bgColorInput = transparentCheckbox.parents('[data-alpaca-item-container-item-key="transparent"]').siblings('[data-alpaca-item-container-item-key="bgColor"]').find('input');
                    if(transparentCheckbox.is(':checked')) {
                         bgColorInput.attr('disabled', true);
                        bgColorInput.parents('.alpaca-controlfield-container').find('.evo-colorind').hide();
                    }
                }).
                bind('refresh.jstree', function () {
                    /*
                        we try to re-select previously selected node, but we are very optimistic since rely on 'j_id'
                        which may change if structure was changed (e.g., new node was added), since it's just counter
                    */
                    var j_id;
                    if (selected != null) {
                        j_id = $(selected).attr('j_id');
                        $tree.jstree('select_node','[j_id="' + j_id + '"]');
                    }
                });

        var $btns = $('#btns');
        $btns.find('#tree-add').click(addLeaf);
        $btns.find('#tree-copy').click(copyLeaf);
        $btns.find('#tree-paste').click(pasteLeaf);
        $btns.find('#tree-del').click(deleteLeaf);
        $btns.find('#tree-up').click(moveLeafUp);
        $btns.find('#tree-down').click(moveLeafDown);

        $('#previous-page').click(function () {
            shiftPage(-1);
        });
        $('#next-page').click(function () {
            shiftPage(1);
        });

        var saveTemplateFun = function(){
            var $jsonForm = $('#json-form');
            if ($jsonForm.find('form').length == 0 || $jsonForm.find('.ui-state-error').length == 0) {
                savePreview();
            } else {
                showErrorMsg('Component properties are invalid. Please, check your input');
            }
        };

        $("#savePreview").click(function () {
            TemplateAction.setApplyAction();
            saveTemplateFun();
        });

        $("#saveVersion").click(function () {
            TemplateAction.setSaveVersionAction();
            saveTemplateFun();
        });

        $('.samples-dialog').dialog({
            autoOpen: false,
            resizable: true,
            height: 500,
            width: 700,
            modal: true,
            buttons: {
                "Ok": function () {
                    $(this).dialog("close");
                }
            }
        });

        // When the transparent checkbox is selected we disable the color input and hide the color-picker.
        $('#editor').on('click', 'input[name="transparent"]', function() {
            var bgColorInput = $(this).parents('[data-alpaca-item-container-item-key="transparent"]').siblings('[data-alpaca-item-container-item-key="bgColor"]').find('input');
            if($(this).is(':checked')) {
                bgColorInput.attr('disabled', true);
                bgColorInput.parents('.alpaca-controlfield-container').find('.evo-colorind').hide();
            } else {
                bgColorInput.removeAttr('disabled');
                bgColorInput.parents('.alpaca-controlfield-container').find('.evo-colorind').show();
            }
        });

        // If enter is pressed when in the invoice id field we prevent the submition of the form.
        $('[name="invoice_id"]').on('keypress',function(e){
            if(e.keyCode == 13) {
                e.preventDefault();
            }
        });
    });

    } ());

    function reloadImage() {
        var $preview = $("#preview-img");
        $('#spinner').show('fade');
        $preview.attr("src", "../report/?id=${selected.id}&format=img&pagenum=" + $('#page-number').text() + "&rnd=" + (new Date()).getTime());
        $preview.load(function(){
            windowHeight();
            $('#spinner').hide('fade');
        });
    }

    function showListDataSourceFields() {
        $('.ui-dialog-content').dialog('close');
        $('#list-data-source-fields').dialog('open');
    }

    function showListTemplateParameters() {
        $('.ui-dialog-content').dialog('close');
        $('#list-template-parameters').dialog('open');
    }

    function showFilterCriteriaSample() {
        $('.ui-dialog-content').dialog('close');
        $('#filter-criteria-sample-dialog').dialog('open');
    }

    //         From AY

    $('document').ready(function () {
        windowHeight();
        $(window).resize(function () {
            windowHeight();
        });
        $(window).scroll(function () {
            windowHeight();
        });
        $('.panel').parents('.form-edit').css('margin', '0 0 -44px');
    });

    function windowHeight() {
        var panelHeight = $(window).height() - $('#header').height() - $('#breadcrumbs').height() - $('#messages').height() - $('#error-messages').height() - $('.form-edit .heading').height() - $('.preview-panel').height() - 80;
        $('.panel #editor').height(panelHeight).css('min-height', '145px');
        $('.panel #tree').height(panelHeight - $('.btn-box').height() - 9).css('min-height', '98px');
        var $preview = $('#preview');
        $preview.css('padding', '10px 5px').css('vertical-align', 'middle');
        var $preview2 = $('#preview-img');
        $preview2.css('max-width', '100%');
        var $preview3 = $preview.find('#preview-img');
        if ($preview2.width() < ($preview.width() - 9)) {
            $preview3.height(panelHeight + 50).css('width', 'auto').css('min-height', '185px');
        }
        if ($preview2.height() > (panelHeight + 49)) {
            $preview3.height(panelHeight + 50).css('width', 'auto').css('min-height', '185px');
        }
        $('body').css('min-width', '1270px')
    }
    // END OF From AY
</g:javascript>

<div class="form-edit">

    <div class="heading">
        <div id="paging">
            <a id="previous-page" class="prev plus-icon" href="#" style="display: none;"
               title="${message(code: 'invoiceTemplate.previousPage.label')}">&#xe034;</a>
            <span id="page-number">1</span>
            <a id="next-page" class="next plus-icon" href="#" title="${message(code: 'invoiceTemplate.nextPage.label')}">&#xe035;</a>
        </div>
        <strong>
            <g:message code="invoiceTemplate.edit.title" args="${[selected.invoiceTemplate.id, selected.versionNumber]}"/>
        </strong>
    </div>

    <div class="form-hold">
        <table id="main-table">
            <tr>
                <td class="panel">
                    <div style="border-bottom: 1px solid #CCCCCC; padding: 0 0 10px;">
                        <g:form controller="invoiceTemplate" action="saveTemplate" class="btn-box">
                            <div class="box">
                                <div class="sub-box">
                                    <table class="dataTable temp-ver-form" cellspacing="1" cellpadding="1">
                                        <tbody>
                                        <tr>
                                            <td class="label">
                                                <g:message code="invoiceTemplate.label.invoice.id"/>
                                            </td>
                                            <td class="value">
                                                <g:textField class="field" name="invoice_id" value="${selected.invoiceTemplate.invoiceId}"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">
                                                <g:message code="invoiceTemplateVersion.tagName.details"/>
                                            </td>
                                            <td class="value">
                                                <g:textField class="field" name="tagName" value="${selected.tagName}"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">
                                                <g:message code="invoiceTemplateVersion.useForInvoice.details"/>
                                            </td>
                                            <td class="value">
                                                <g:checkBox class="field" name="useForInvoice" value="${selected.useForInvoice}"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">
                                                <g:message code="invoiceTemplateVersion.label.includeCarriedInvoiceLines.details"/>
                                            </td>
                                            <td class="value">
                                                <g:checkBox class="field" name="includeCarriedInvoiceLines" value="${selected.includeCarriedInvoiceLines}"/>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td> </td>
                                            <td class="value">
                                                <g:submitToRemote value="Change" class="submit ok change-invoice" action="saveTemplate"
                                                                  onSuccess="reloadImage()"/>
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                            <g:hiddenField name="id" value="${selected.id}"/>
                        </g:form>
                    </div>

                    <div id="btns" class="btn-box">
                        <button id="tree-add" class="submit2" disabled="disabled">Add</button>
                        <button id="tree-copy" class="submit2" disabled="disabled">Copy</button>
                        <button id="tree-paste" class="submit2" disabled="disabled">Paste</button>
                        <button id="tree-del" class="submit2" disabled="disabled">Del</button>
                        <button id="tree-up" class="submit2" style="width: 20px" disabled="disabled">&uparrow;</button>
                        <button id="tree-down" class="submit2" style="width: 20px"
                                disabled="disabled">&downarrow;</button>
                    </div>

                    <div id="tree"></div>
                </td>
                <td class="panel">

                    <div class="preview-panel">
                        <a href="#" id="savePreview" type="button" class="submit">
                            Apply
                        </a>
                        <g:link class="submit" controller="invoiceTemplate" action="report" params="[id: selected.id]">
                            Download
                        </g:link>
                        <a href="#" id="saveVersion" type="button" class="submit">
                            Save Version
                        </a>
                        <g:link class="submit" controller="invoiceTemplate" action="list">
                            <g:message code="button.cancel"/>
                        </g:link>
                    </div>

                    <div id="editor" style="overflow: auto; max-height: 800px">
                        <div id="json-form"></div>
                    </div>
                </td>
                <td id="preview"><img id="preview-img" src="../report/${selected.id}/?format=img"></td>
            </tr>
        </table>

    </div>
</div>

<div id="dialog-delete-confirm" title="Delete element?" style="display:none">
    <p><span class="ui-icon ui-icon-alert"></span>This item will be permanently deleted and cannot be recovered. Are you sure?
    </p>
</div>

<div id="dialog-add-new-element" title="Select Element Type" style="display:none">
    <div id="types-list"></div>
</div>

<div id="dialog-save-error" title="Save Error" style="display:none">
    <p>
        <span class="ui-icon ui-icon-alert"></span>
        <span id="save-error-msg"></span>
    </p>
</div>

<div id="dialog-delete-error" title="Save Error" style="display:none">
    <p>
        <span class="ui-icon ui-icon-alert"></span>
        <span>
            You can not delete the last section in the template.
            Please, create a new section or remove all children from the current, if you want to make it empty.
        </span>
    </p>
</div>

<div id="filter-criteria-sample-dialog" class="samples-dialog">
    <style type="text/css" scoped="scoped">
    #filter-criteria-sample-dialog > p > span {
        font-family: monospace
    }
    </style>

    <p>
        Every field of data source can be treated as <a
            href="http://docs.oracle.com/javase/6/docs/api/java/lang/String.html">String</a>
        instance. So you can use any Java-expression, which can be evaluated as boolean (true or false) value. E.g.,
        this expression will select assets having "Modem" asset type: <span>$F{assetType}.equals("Modem")</span>. It is
    also possible to use custom meta-fields; e.g., <span>$F{__originCountry}.startsWith("U")</span> will select
    assets (assuming they have "origin country" meta-field), which country of origin start with "U".
    </p>

    <p>
        You can also use NOT (<span>!</span>), AND (<span>&amp;&amp;</span>) and OR (<span>||</span>) operations to
    combine criteria. E.g., the expression <span>(!$F{assetType}.equals("Modem"))</span> will show all asset types
    except modems, and <span>($F{assetType}.equals("Modem") || $F{assetType}.equals("SIM Card"))</span> will include
    "modems or sim cards" in the output.
    </p>
</div>

<g:render template="fieldSetup"/>
<g:render template="listDataSourceFields"/>
<g:render template="listTemplateParameters"/>

</body>
</html>
