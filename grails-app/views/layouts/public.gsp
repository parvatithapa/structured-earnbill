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

<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--[if lt IE 7]>      <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html xmlns="http://www.w3.org/1999/xhtml" class="lt-ie9"> <![endif]-->
<!--[if IE 9]>         <html xmlns="http://www.w3.org/1999/xhtml" class="ie9"> <![endif]-->
<!--[if gt IE 9]><!--> <html xmlns="http://www.w3.org/1999/xhtml" > <!--<![endif]-->
    <head>
        <meta http-equiv="content-type" content="text/html; charset=utf-8" />

        <title><g:layoutTitle default="${defaultTitle()}" /></title>

        <link rel="shortcut icon" href="${logoLink(favicon:true) + '?' + Math.random()}" type="image/x-icon" />

        <r:require modules="jquery, core, ui, input"/>
        <g:layoutHead/>
        <r:layoutResources/>

    <script type="text/javascript" xmlns="http://www.w3.org/1999/html">
        $(document).ready(function () {
            $('.select-holder select').each(function () {
                updateSelectLabel(this);
            });

            $('.select-holder select').change(function () {
                updateSelectLabel(this);
            });
        });

        function updateSelectLabel(selectEl) {
            var select = $(selectEl);
            var currentValue = select.find("option:selected");
            var span = select.closest(".select-holder").find("span.select-value");
            if (span.size() > 0 && currentValue.size() > 0) {
                span.text(currentValue.text());
            }
        }
    </script>
    <sec:ifLoggedIn>
        <g:render template="/layouts/includes/uiColorStyle"/>
    </sec:ifLoggedIn>
        <g:isBrandingJBilling>
            <style type="text/css">
               #home-logo a {
                   background: url(${createLink(uri : '/images/EarnBill.png')}) no-repeat;
                   background-position: 50%;
                   background-size: auto 40px;
               }
            </style>
        </g:isBrandingJBilling>

</head>
    <body>
        <div id="wrapper">
            <!-- header -->
            <div id="header">
                <div id="navigation">
                    <ul id="navList">
                        <li id="home-logo" class="">
                            <g:link uri="/"></g:link>
                        </li>
                    </ul>
                </div>
            </div>

            <!-- content -->
            <div id="main">
                <g:layoutBody />
            </div>
        </div>
        <r:layoutResources/>
    </body>
</html>
