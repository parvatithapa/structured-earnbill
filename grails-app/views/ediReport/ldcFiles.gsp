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

<%@ page import="com.sapienter.jbilling.server.ediTransaction.TransactionType; com.sapienter.jbilling.common.Constants" %>

<html>
<head>
    <meta name="layout" content="panels"/>
    <link type="text/css" href="${resource(file: '/css/ui.jqgrid.css')}" rel="stylesheet"
          media="screen, projection"/>
    <g:javascript src="jquery.jqGrid.min.js"/>
    <g:javascript src="jqGrid/i18n/grid.locale-${session.locale.language}.js"/>
</head>

<body>
<!-- selected configuration menu item -->
<content tag="menu.item"><g:message code="ldc.files.type.header" args="${params.id}"/></content>

<content tag="column1">

    <table id="jqGrid"></table>

    <div id="jqGridPager"></div>

    <div class="btn-box">
        <g:form id="delete-file" name="delete-file" url="[action: 'delete']">
            <g:hiddenField name="id" value="${params.id}"/>
            <g:hiddenField id="fileNames" name="fileNames"/>
            <button onclick="getSelectedRows()" class="submit add"><span><g:message code="button.delete"/></span>
            </button>
        </g:form>
    </div>

    <script type="text/javascript">


        $(document).ready(function () {

            $("#jqGrid").jqGrid({
                url: '<g:createLink action="getLdcFiles" params="[id:params.id]"/>',
                datatype: "json",
                colNames: [
                    '<g:message code="label.edi.file"/>',
                    '<g:message code="label.edi.file.date"/>',
                    '<g:message code="label.edi.file.download"/>'
                ],
                colModel: [
                    {name: 'fileName', editable: false},
                    {name: 'date', editable: false, fixed: true, width: 130},
                    {
                        name: 'filename1',
                        width: 80,
                        fixed: true,
                        search: false,
                        jsonmap: 'fileName',
                        formatter: linkFormatter
                    }
                ],
                viewrecords: true, // show the current page, data rang and total records on the toolbar
                multiselect: true,
                autowidth: true,
                height: 'auto',
                rowNum: 10,
                rowList: [10, 20, 50, 100],
                loadonce: true, // this is just for the demo
                pager: "#jqGridPager"
            });
            $("#jqGrid").jqGrid('filterToolbar', {autosearch: true});
        });


        // For displaying download link
        function linkFormatter(cellvalue, options, rowObject) {
            return '<a href="/jbilling/ediReport/download/${params.id}?fileName=' + cellvalue + '" class="submit2 save"><span></span></a>';
        }

        function getSelectedRows() {
            var grid = $("#jqGrid");
            var rowKey = grid.getGridParam("selrow");

            if (rowKey){
                var selectedIDs = grid.getGridParam("selarrrow");
                var result = [];
                for (var i = 0; i < selectedIDs.length; i++) {
                    result[i] = grid.jqGrid('getCell', selectedIDs[i], 'fileName');
                }

                $('#fileNames').val(result);
                $('#delete-file').submit();
            }
        }

    </script>

</content>
</body>
</html>
