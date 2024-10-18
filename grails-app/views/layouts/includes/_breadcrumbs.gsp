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

<%--
  _breadcrumbs

  @author Brian Cowdery
  @since  23-11-2010
--%>

<!-- breadcrumbs -->
<div id="breadcrumbs" class="breadcrumbs top-hold">
    <div id="spinner" style="display: none;">
        <img src="${resource(dir:'images', file:'spinner.gif')}" alt="loading..." />
    </div>

    <script type="text/javascript">
        jQuery(document).ajaxStart(function(){$("#spinner").show('fade');});
        jQuery(document).ajaxStop(function() {$("#spinner").hide('fade');})
    </script>

    <ul>
        <li>
            <g:link uri="/" style="font-family: AppDirectIcons;">&#xe03b;</g:link>
        </li>
        <g:set var="breadcrumbService" bean="breadcrumbService"/>
        <g:each var="crumb" in="${crumbs}" status="idx">
            <g:if test="${crumb != null && idx < (crumbs.size()-1)}">
                <li>
                    <g:link controller="${crumb.controller}" action="${crumb.action}" id="${crumb.objectId ?: crumb.uuid}" params="${crumb.parametersToMap}">
                        <g:message code="${crumb.messageCode}" args="[crumb.description ?: crumb.objectId?:'']"/>
                    </g:link>
                </li>
            </g:if>
        </g:each>
        <li class="current">
            <g:message code="breadcrumb.current.page" />
        </li>
    </ul>
</div>
