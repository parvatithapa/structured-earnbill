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

<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils; com.sapienter.jbilling.common.Constants; jbilling.SearchType; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.user.db.CompanyDAS; com.sapienter.jbilling.server.util.ColorConverter"%>

<%--
  Page header for all common jBilling layouts.

  This contains the jBilling top-navigation bar, search form and main navigation menu.

  @author Brian Cowdery
  @since  23-11-2010
--%>

%{-- Load company and childEntities variables in page scope --}%
<g:company children="true" />

<script type="text/javascript" xmlns="http://www.w3.org/1999/html">
    function clearPlaceHolder(src)
    {
        var str = src.placeholder;
        str = str != null ? str.replace("${message(code:'search.title')}", "") : str;
        $('#id').attr('placeholder', str);
    }

    function setPlaceHolder()
    {
        var str = '${message(code:'search.title')}';
        $('#id').attr('placeholder', str);
    }
    function updateTips( t ) {
        tips.text( t ).addClass( "ui-state-error" );
    }

    $(document).ready(function() {
        setDirection();
        setImpersonationUser();
        $.widget(
            'ui.dialog',
            $.ui.dialog, {
                _createOverlay: function() {
                    this._super();
                    if (!this.options.modal) {
                        return;
                    }
                    this._on(this.overlay, { click: 'close' });
                    if (window.navigator.vendor != 'Google Inc.') {
                        $('html>body').css('position', 'relative')
                    }
                },
                _destroyOverlay: function() {
                    this._super();
                    if ( !this.options.modal ) {
                        return;
                    }
                    if (window.navigator.vendor != 'Google Inc.') {
                        $('html>body').css('position', 'inherit')
                    }
                }
            }
        );
    });

    function setDirection() {
        var localLang = '${session.locale.language}';
        if(localLang == 'ar') {
            $('html').attr('dir', 'rtl');
        }
    }

    function checkLength( o, n, min, max ) {
        if ( o.val().length > max || o.val().length < min ) {
            o.addClass( "ui-state-error" );
            updateTips( "Length of " + n + " must be between " + min + " and " + max + "." );
            return false;
        } else {
            return true;
        }
    }

    $(function() {
    	$( "#impersonation-dialogue" ).dialog({
            autoOpen: false,
            height: 200,
            width: 	450,
            modal: 	true,
            create: function(event){
                $(event.target).parent().css("position", "fixed");
            },
            close: function() {
                $("#authEventFail").remove();
            }
    	});

    	$( "#impersonate" ).click(function() {
    		$( "#impersonation-dialogue" ).dialog( "open" );
            $('.ui-dialog :button').blur();
    	});

        %{--The filters on the left gets applied if 'enter' is pressed anywhere in the UI. We want to stop that if 'enter' is pressed on the search form --}%
        $("#search-form").on('keypress', function(e) {
            if ( e.which == 13) {
                e.stopPropagation();
            }
        });
    });

    function setImpersonationUser() {
        $.ajax({
            url: '${createLink(action: 'getUserByCompany', controller: 'user')}',
            type: 'POST',
            data: { entityId: $( "#impersonation-select" ).val() },
            success: function (result) {
                $('input[name = j_username]').val(result.name);
            }
        });
    }

    //Following function added to fix #7338 - Tabs are not properly located on page in case of large number of tabs on screen.

    function adjustMenu() {
        var width = $(document).width() - 100;
        $('#navRight > li').each(function (i) {
            width -= $(this).width();
        });

        var hiddenTabsMenuItem = $('#hiddenTabsLi');
        var hiddenTabsMenu = $('#hiddenTabsUl');
        var showHiddenTabs = false;

        width -= hiddenTabsMenuItem.width();

        $('#navList > li').each(function (i) {
            var menuItem = $(this);
            width -= menuItem.width();
            if(width <= 0 && menuItem.attr('id') != 'hiddenTabsLi') {
                menuItem.detach();
                hiddenTabsMenu.append(menuItem);
                showHiddenTabs = true;
            }
        });

        if(showHiddenTabs) {
            hiddenTabsMenuItem.show();
        }
    }

    function menuClick(event) {
        var visible = $(this).parent().find('ul').is(':visible');
        if($(this).parent().parent().parent('#navigation').length == 1 ) {
        $(".hideOnClick").hide();
        } else {
            $(this).parent().parent().children(".hideOnClick").hide();
        }
        $(this).parent().children('ul').toggle(!visible);

        //close popup windows
        $('.open').parent('div').removeClass('active');

        event.stopPropagation();
    }

    function moveHiddenActiveTab() {
        var homeLogo = $('#home-logo');
        var moved = false;
        $("#hiddenTabsUl li.active").each(function (i) {
            var menuItem = $(this);
            menuItem.detach();
            homeLogo.after(menuItem);
            moved = true;
        });
        return moved;
    }

    $(document).ready(function() {
        adjustMenu();
        if(moveHiddenActiveTab()) {
            adjustMenu();
        }

        if ('${params.showAssetUploadTemplate}') {
            $("#uploadAsset").click();
        }
        if(${params.controller=="customerEnrollment"}){
            $(".customer").addClass("active");
        }
        if(${params.controller=="ediFile"}){
            $(".edi").addClass("active");
        }
        if(${params.controller=="ediReport"}){
            $(".edi").addClass("active");
        }

        $('.menuClick').click(menuClick);

        $('body').click(function() {
            $(".hideOnClick").hide();
        });

        $('.select-holder select').each(function () {
            updateSelectLabel(this);
        });

        $('.select-holder select').change(function () {
            updateSelectLabel(this);
        });

        $('.search-dropdown').click(function() {
            $(".hideOnClick").hide();
        });
    });

    function updateSelectLabel(selectEl) {
        var select = $(selectEl);
        var currentValue = select.find("option:selected");
        var span = select.closest(".select-holder").find("span.select-value");
        if(span.size() > 0 &&  currentValue.size() > 0) {
            span.text(currentValue.text());
        }
    }

</script>

<style>
    /*#navList .active ul li a { background: none !important; box-shadow: none !important; border: none !important;}*/
    /*#navList .active:hover ul li a { background: none !important;}*/
    /*#navList .active ul li:hover a{ background: #ffffff !important;}*/
    /*#navList .active ul li:hover a{ background: #ffffff !important;}*/
    /*#navList .active ul li a:active { background: #ffffff !important;}*/
</style>


<g:render template="/layouts/includes/uiColorStyle"/>
<html>

<head>
    <g:if test="${session.locale.language == 'ar'}">
        <link type="text/css" href="${resource(file: '/css/all_rtl.css')}" rel="stylesheet" media="screen, projection" />
    </g:if>
</head>

<body>

<!-- header -->
<div id="header">
<div id="navigation">
%{
    def hiddenTabs = []
}%
<%-- select the current menu item based on the controller name --%>
<ul id="navList">
    <li id="home-logo" class="">
        <g:link uri="/">&nbsp;</g:link>
    </li>
    <g:each in="${session['user_tabs'].tabConfigurationTabs.findAll{it.tab.parentTab == null}}" var="tabConfig">
        <jB:userCanAccessTab tab="${tabConfig.tab}">
            <g:if test="${tabConfig.visible && !tabConfig?.tab?.parentTab}">
                <g:if test="${tabConfig?.tab?.parentTab==null}">
                    <g:set var="childTabs" value="${session['user_tabs'].tabConfigurationTabs.tab.findAll{it?.parentTab?.id==tabConfig?.tab?.id}}"/>
                    <g:set var="childTabsToRemove" value="${new java.util.ArrayList()}"/>
                    <g:each in="${childTabs}" var="childTab">
                        <jB:userCanNotAccessTab tab="${childTab}">
                            %{
                                childTabsToRemove.add(childTab)
                            }%
                        </jB:userCanNotAccessTab>
                    </g:each>
                    %{
                        childTabs.removeAll(childTabsToRemove)
                    }%

                    <li class="${(controllerName == tabConfig.tab.controllerName || controllerName in childTabs?.controllerName)? 'active' : ''}">
                        <g:if test="${childTabs && childTabs.size() > 1}">
                            <a class="menuClick menu-dropdown"><span><g:message code="${tabConfig.tab.messageCode}"/></span></a>
                            <ul style="display: none;" class="hideOnClick">
                                <g:each in="${childTabs}" var="childTab">
                                    <jB:userCanAccessTab tab="${childTab}">
                                        <li>
                                            <g:link controller="${childTab.controllerName}">
                                                <span><g:message code="${childTab.messageCode}"/></span>
                                            </g:link>
                                        </li>
                                    </jB:userCanAccessTab>
                                </g:each>
                            </ul>
                        </g:if>
                        <g:else>
                            <g:link controller="${tabConfig.tab.controllerName}">
                                <span><g:message code="${tabConfig.tab.messageCode}"/></span>
                            </g:link>
                        </g:else>
                    </li>
                </g:if>
            </g:if>
            <g:else>
                %{
                    hiddenTabs.add(tabConfig.tab)
                }%
            </g:else>
        </jB:userCanAccessTab>
    </g:each>

    <li id="hiddenTabsLi" style="${hiddenTabs.size() <= 0 ? 'display:none;' : ''}"><a class="menuClick" ><span>+</span></a>
        <ul id="hiddenTabsUl" style="display: none;" class="hideOnClick">
            <g:each var="tab" in="${hiddenTabs}">
                <g:set var="childTabs" value="${session['user_tabs'].tabConfigurationTabs.tab.findAll{it?.parentTab?.id==tab?.id}}"/>
                <g:set var="childTabsToRemove" value="${new java.util.ArrayList()}"/>
                <g:each in="${childTabs}" var="childTab">
                    <jB:userCanNotAccessTab tab="${childTab}">
                        %{
                            childTabsToRemove.add(childTab)
                        }%
                    </jB:userCanNotAccessTab>
                </g:each>
                %{
                    childTabs.removeAll(childTabsToRemove)
                }%
                <li class="${(controllerName == tab.controllerName || controllerName in childTabs?.controllerName)? 'active' : ''}">
                    <g:if test="${childTabs && childTabs.size() > 1}">
                            <a class="menuClick menu-expand-right"><span><g:message code="${tab.messageCode}"/></span></a>
                        <ul style="display: none;" class="hideOnClick">
                            <g:each in="${childTabs}" var="childTab">
                                <jB:userCanAccessTab tab="${childTab}">
                                    <li><g:link controller="${childTab.controllerName}"><span><g:message code="${childTab.messageCode}"/></span><em></em></g:link></li>
                                </jB:userCanAccessTab>
                            </g:each>
                        </ul>
                    </g:if>
                    <g:else>
                        <g:link controller="${tab.controllerName}"><span><g:message code="${tab.messageCode}"/></span><em></em></g:link>
                    </g:else>
                </li>
            </g:each>
        </ul>
    </li>
</ul>


<ul id="navRight">
    <li>
        <div class="search">
            <g:form controller="search" name="search-form" onsubmit="canReloadMessages = false;">
                <fieldset>
                    <div class="search-combo">
                        <div class="input-bg search-filter">
                            <g:applyLayout name="form/select_holder">
                                <content tag="label.for">type</content>
                                <content tag="holder.class">select-holder_small</content>
                                <select name="type" id="search-type">
                                    <sec:access url="/customer/list">
                                        <option value="CUSTOMERS" ${(!cmd || cmd?.type?.toString() == 'CUSTOMERS') ? 'selected="true"' : ''}><g:message code="search.option.customers"/></option>
                                    </sec:access>
                                    <sec:access url="/order/list">
                                        <option value="ORDERS" ${cmd?.type?.toString() == 'ORDERS' ? 'selected="true"' : ''}><g:message code="search.option.orders"/></option>
                                    </sec:access>
                                    <sec:access url="/invoice/list">
                                        <option value="INVOICES" ${cmd?.type?.toString() == 'INVOICES' ? 'selected="true"' : ''}><g:message code="search.option.invoices"/></option>
                                    </sec:access>
                                    <sec:access url="/payment/list">
                                        <option value="PAYMENTS" ${cmd?.type?.toString() == 'PAYMENTS' ? 'selected="true"' : ''}><g:message code="search.option.payments"/></option>
                                    </sec:access>
                                </select>
                            </g:applyLayout>

                        </div>
                        <div class="input-bg search-action">
                            <g:textField name="id" placeholder="${cmd?.id ?: message(code:'search.title')}" class="default" onclick="clearPlaceHolder(this);" onkeydown="setPlaceHolder();" />
                            <a href="#" class="search-button" onclick="canReloadMessages=false;$('#search-form').submit()">&#xe03e;</a>
                        </div>
                    </div>
                </fieldset>
            </g:form>
        </div>
    </li>
    <li>
        <a class="menuClick menu-dropdown" data-cy="forLogOut"><span><jB:userFriendlyName/></span></a>
        <ul id="custom-ul" style="display: none;" class="hideOnClick">
            <li class="not-active"><span><%=company.getDescription()%></span></li>
            <sec:ifSwitched>
                <g:set var="switchedUserOriginalUsername" value="${SpringSecurityUtils.switchedUserOriginalUsername}"/>
                <g:if test="${switchedUserOriginalUsername?.substring(switchedUserOriginalUsername.indexOf(';') + 1, switchedUserOriginalUsername.length()).equals(session['company_id'].toString())}">
                    <li>
                        <a href="${request.contextPath}/j_spring_security_exit_user">
                            <span><g:set var="plainUsername" value="${switchedUserOriginalUsername?.substring(0, switchedUserOriginalUsername.indexOf(';'))}"/>
                                <g:message code="switch.user.resume.session.as"/></span>
                        </a>
                    </li>
                </g:if>
            </sec:ifSwitched>

            <g:if test="${company?.parent == null || (childEntities != null && childEntities.size() > 0)}">
                <sec:ifSwitched>
                    <g:set var="switchedUserOriginalUsername" value="${SpringSecurityUtils.switchedUserOriginalUsername}"/>
                    <g:if test="${!switchedUserOriginalUsername?.substring(switchedUserOriginalUsername.indexOf(';') + 1, switchedUserOriginalUsername.length()).equals(session['company_id'].toString())}">
                        <li>
                            <a href="${request.contextPath}/j_spring_security_exit_user" class="dissimulate"><span>
                                <g:message code="switch.user.resume.session.as"/>
                            </span>
                            </a>
                        </li>
                    </g:if>
                </sec:ifSwitched>
                <sec:ifNotSwitched>
                    <sec:ifAnyGranted roles="CONFIGURATION_1913">
                        <li>
                            <a id="impersonate">
                                <span>
                                    <g:message code="topnav.link.impersonate"/>
                                </span>
                            </a>
                        </li>
                    </sec:ifAnyGranted>
                </sec:ifNotSwitched>
            </g:if>
            <g:else>
                <sec:ifSwitched>
                    <g:set var="switchedUserOriginalUsername" value="${SpringSecurityUtils.switchedUserOriginalUsername}"/>
                    <g:if test="${!switchedUserOriginalUsername?.substring(switchedUserOriginalUsername.indexOf(';') + 1, switchedUserOriginalUsername.length()).equals(session['company_id'].toString())}">
                        <li>

                            <a href="${request.contextPath}/j_spring_security_exit_user" class="dissimulate">
                                <span><g:message code="switch.user.resume.session.as"/></span>
                            </a>
                        </li>
                    </g:if>
                </sec:ifSwitched>
            </g:else>

            <sec:ifAnyGranted roles="MY_ACCOUNT_160,MY_ACCOUNT_161,MY_ACCOUNT_162">
                <li>
                    <g:link controller="myAccount" class="account">
                        <span><g:message code="topnav.link.account"/></span>
                    </g:link>
                </li>
            </sec:ifAnyGranted>

            <li>
                <a href="http://www.jbilling.com/professional-services/training" class="training">
                    <span><g:message code="topnav.link.training"/></span>
                </a>
            </li>
            <li>
                <a href="${resource(dir:'manual', file: 'index.html')}" class="help">
                    <span><g:message code="topnav.link.help"/></span>
                </a>
            </li>
            <li>
                <g:link controller='logout' class="logout" data-cy="logout">
                    <span><g:message code="topnav.link.logout"/></span>
                </g:link>
            </li>
        </ul>
    </li>
</ul>
</div>
<div id="impersonation-dialogue" title="Impersonate">

    <g:if test = "${childEntities.size() > 0}">
        <form id="impersonation-form" action="${request.contextPath}/j_spring_security_switch_user" method="POST">
            <g:hiddenField id="security_username"  name="j_username"/>
            <div id="impersonation-text">Please select a child entity to impersonate:</div>
            <g:if test="${flash.errorAuthToFail}">
                <div id="authEventFail" class="msg-box error">
                    <g:message code="${flash.errorAuthToFail}"/>
                </div>
            </g:if>


            <div>
                <div class="select-holder"><span class="select-value"></span>
                    <g:select id="impersonation-select" name="entityId"
                          from="${childEntities}"
                          optionKey="id"
                          optionValue="${{it?.description}}"
                          value="${entityId}"
                          onChange="setImpersonationUser();"
                    />
                </div>
                <g:set var="filter_selector" value="select[name='entityId']"/>
                <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
            </div>
            <div class="buttons dialog-buttons">
                <ul>
                    <li>
                        <a id="impersonation-button" onclick="$('#impersonation-form').submit()" class="submit select button-primary"><span><g:message code="button.select"/></span></a>
                    </li>
                </ul>
            </div>
        </form>
    </g:if>
    <g:else>
        <strong>This Company does not have any child company!</strong>
    </g:else>
</div>
</div>

</body>
</html>