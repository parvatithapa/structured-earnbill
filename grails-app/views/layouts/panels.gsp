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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--[if lt IE 7]>      <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9"> <![endif]-->
<!--[if IE 9]>         <html xmlns="http://www.w3.org/1999/xhtml" class="ie9"> <![endif]-->
<!--[if gt IE 9]><!--> <html xmlns="http://www.w3.org/1999/xhtml" > <!--<![endif]-->
<head>
    <g:render template="/layouts/includes/head"/>
    <r:script disposition='head'>

		$(document).ready(function() {
            $(document).keypress(function(e) {
                if(e.which == 13) {
                    if (!window.avoidFilterBlur) {
                        $(this).blur();
                        submitApply ();
                    }
                }
            });
        });
        function renderRecentItems() {
            $.ajax({
                url: "${resource(dir:'')}/recentItem",
                global: false,
                cache: false,
                success: function(data) { $("#recent-items").replaceWith(data) }
            });
        }

        $(document).ajaxSuccess(function(e, xhr, settings) {
            renderRecentItems();
        });

        /*
        Following function has been added to take care of issue ##7746.
        On invoices tab clicking one of the record in the list displays the details of that particular invoice in the right panel.
        But even after applying a filter, that requires recalculating the invoice list, right panel still shows the details of
        earlier selected invoice record. 
        This function clears the content in right panel after a filter has been applied.  
        */
        function clearDetailsColumn () {
            //As per user request, currently we are handling only invoices tab. To extend this fix to other tabs use list table Id
            //corresponding to that tab in the if condition.
         	if ($("#viewport table").length > 0) {
         	    var controller = '${params.controller}';
                var renderHtml = '${!['invoice', 'order', 'billing', 'mediation', 'provisioning', 'discount', 'product', 'notifications', 'bulkUpload', 'bulkDownload'].contains(params.controller) ? raw(g.render(template : 'noSelected').toString().replaceAll("\\r?\\n", "")) : ""}'
                if(controller != 'product'){
				    $("#viewport #column2").html(renderHtml);
                }
			}
		}
         function refreshFilter(){
              $.ajax({
                url: "${createLink(controller: 'filter', action: 'load')}",
                global: false,
                success: function(data) { $("#filters").replaceWith(data) }
            });
        }
  </r:script>
    <r:require modules="panels"/>
    <r:layoutResources/>
    <g:layoutHead/>
</head>
<body>
<div id="wrapper">
    <g:render template="/layouts/includes/header"/>

    <div id="main">
        <g:render template="/layouts/includes/breadcrumbs"/>

        <div id="left-column">


            <!-- filters -->
            <g:if test="${filters}">
                <g:set var="target" value="${filterRender ?: 'first'}"/>
                <g:set var="action" value="${filterAction ?: 'list'}"/>
                <g:set var="urlId" value="${filterId ?: ''}"/>

                <g:formRemote name="filters-form" url="[action: action, id: urlId]" onSuccess="render(data, ${target}); clearDetailsColumn();refreshFilter();">
                    <g:hiddenField name="applyFilter" value="true"/>
                    <g:render template="/layouts/includes/filters" model="[filters: filters, filterRender: filterRender, filterAction: filterAction]"/>
                </g:formRemote>

                <g:render template="/layouts/includes/filterSaveDialog"/>
                <g:render template="/layouts/includes/filterCustomDialog"/>
            </g:if>
             <g:else>
                 <g:if test="${isFromConfiguration}">
            	 <div class="menu-items">
	                <g:render template="/layouts/includes/configMenu"/>
	            </div>
	           </g:if>
            </g:else>
            <!-- shortcuts -->
            <g:if test="${session['shortcuts']}">
                <g:render template="/layouts/includes/shortcuts"/>
                <inc:include controller="shortcut" action="index"/>
            </g:if>

            <!-- recently viewed items -->
            <g:render template="/layouts/includes/recent"/>
        </div>


        <!-- content columns -->
        <div class="columns-holder">
            <g:render template="/layouts/includes/messages"/>
            <g:render template="/layouts/includes/errors"/>

            <!-- viewport of visible columns -->
            <div id="viewport">
                <div class="column panel">
                    <div id="column1" class="column-hold">
                        <g:pageProperty name="page.column1"/>
                    </div>
                </div>

                <div class="column panel">
                    <div id="column2" class="column-hold">
                        <g:pageProperty name="page.column2"/>
                    </div>
                </div>
            </div>

            <!-- template for new column-->
            <div id="panel-template" class="column panel">
                <div class="column-hold"></div>
            </div>
        </div>
    </div>
</div>

<r:layoutResources/>

</body>
</html>
