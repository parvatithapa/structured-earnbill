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

<%@ page import="jbilling.TabConfigurationTab; jbilling.Tab" contentType="text/html;charset=UTF-8" %>

<r:require module="disjointlistbox"/>
%{--
  Allows the user to edit the order of the tabs across the top of the screen

  @author Gerhard Maree
  @since  25-Apr-2013
--}%

<div class="form-hold">
<div class="form-edit">
    <div class="heading"><strong><g:message code="tabs.organize.title"/></strong></div>
    <g:form id="vis-cols-multi-sel-form" name="tab-config-form" url="[action:'save',controller:'tabConfig']" useToken="true">
        %{
            def hiddenTabs = [];
            def visibleTabs = []
        }%
        <g:each in="${tabConfigurationTabs}" var="tabConfig">
            <jB:userCanAccessTab tab="${tabConfig.tab}">
                <g:if test="${tabConfig.visible}">
                    %{ visibleTabs.add([value: tabConfig.tab.id, message: tabConfig.tab.messageCode]) }%
                </g:if>
                <g:else>
                    %{  hiddenTabs.add([value: tabConfig.tab.id, message: tabConfig.tab.messageCode]) }%
                </g:else>
            </jB:userCanAccessTab>
        </g:each>

        <jB:disjointListbox id="vis-cols-multi-sel" left="${visibleTabs}" right="${hiddenTabs}"
                            left-input="visible-order" right-input="hidden-order"
                            left-header="tabs.head.visible" right-header="tabs.head.hidden" />

    </g:form>
    <div class="buttons">
        <ul>
            <li>
                <a onclick="updateDLValues('vis-cols-multi-sel');$('#vis-cols-multi-sel-form').submit()" class="submit save button-primary">
                    <span><g:message code="button.save"/></span>
                </a>
            </li>
            <li>
                <g:link controller="myAccount" class="submit cancel">
                    <span><g:message code="button.cancel"/></span>
                </g:link>
            </li>
        </ul>
    </div>
</div>
</div>
