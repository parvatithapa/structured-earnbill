<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--[if lt IE 7]>      <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9"> <![endif]-->
<!--[if IE 9]>         <html xmlns="http://www.w3.org/1999/xhtml" class="ie9"> <![endif]-->
<!--[if gt IE 9]><!--> <html xmlns="http://www.w3.org/1999/xhtml" > <!--<![endif]-->
<head>
    %{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

    <g:render template="/layouts/includes/head"/>

    <r:script disposition='head'>
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
            Highlight clicked rows in the configuration side menu
         */
        $(document).ready(function() {
            $('#left-column ul.list li').click(function() {
                $(this).parents('ul.list').find('li.active').removeClass('active');
                $(this).addClass('active');
            });
        });
    </r:script>

    <r:require module="panels"/>

    <r:layoutResources/>
    <g:layoutHead/>
</head>
<body>
<div id="wrapper">
    <g:render template="/layouts/includes/header"/>
    <div id="main">
        <g:render template="/layouts/includes/breadcrumbs"/>

        <div id="left-column">
            <!-- configuration menu -->
            <div class="menu-items">
                <g:render template="${menuContent}"/>
            </div>

            <!-- recently viewed items -->
            <g:render template="/layouts/includes/recent"/>
        </div>

        <!-- content columns -->
        <div class="columns-holder">
            <g:render template="/layouts/includes/messages"/>
            <g:render template="/layouts/includes/errors"/>

            <!-- viewport of visible columns -->
            <div id="viewport">
                <div class="column panel ${pageProperty(name: 'page.column.size') == 'double' ? 'column-double' : ''} ${pageProperty(name: 'page.column.size') == 'width-auto' ? 'column-auto' : ''}">
                    <div id="column1" class="column-hold">
                        <g:pageProperty name="page.column1"/>
                    </div>
                </div>

                <div class="column panel ${pageProperty(name: 'page.column.size') == 'double' ? 'column-double' : ''} ${pageProperty(name: 'page.column.size') == 'width-auto' ? 'column-auto' : ''}">
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
