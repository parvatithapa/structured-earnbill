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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; jbilling.FilterType; com.sapienter.jbilling.server.user.db.CompanyDTO; jbilling.FilterSet" %>
<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.PERMISSION_VIEW_ADD_FILTERS;" %>
<%--
  Filter side panel template. Prints all filters contained in the "filters" page variable.

  @author Brian Cowdery
  @since  03-12-2010
--%>

<g:set var="company" value="${CompanyDTO.get(session['company_id'])}"/>
<g:set var="filters" value="${filters.sort{ it?.field }}"/>
<g:set var="filtersets" value="${FilterSet.findAllByUserId(session['user_id'])}"/>
<g:set var="currentFilter" value="${filters?.filterSet}" />
<%
    filtersets?.removeAll{ filterset ->
        filterset.filters.find{ it.type != FilterType.ALL && it.type != session['current_filter_type']}
    }
%>



<div id="filters">
    <div class="heading">
        <strong><g:message code="filters.title"/></strong>
        <g:remoteLink title="${message(code:'filters.clear')}" class="clearFilter" controller="filter" action="clearFilters" update="filters" id="clearFilter"/> %{--&#xe603;--}%
    </div>

    <!-- hidden filters -->
    <%
      def hiddenFilters = [:]
    %>
    <g:set var="hiddenIdx" value="1" />
    <g:while test="${params['fhn'+hiddenIdx] || this['filter_hidden_'+hiddenIdx]}">
        <g:set var="hiddenFilterName" value="${params['fhn'+hiddenIdx]?:this['filter_hidden_'+hiddenIdx]}" />
        <g:set var="hiddenFilterValue" value="${params[hiddenFilterName]?:this[hiddenFilterName]}" />

        <g:hiddenField name="fhn${hiddenIdx}" value="${hiddenFilterName}" />
        <g:hiddenField name="${hiddenFilterName}" value="${hiddenFilterValue}" />

        <%
            hiddenFilters += [('fhn'+hiddenIdx) : hiddenFilterName,
                    (hiddenFilterName): hiddenFilterValue ]
        %>
        <g:set var="hiddenIdx" value="${hiddenIdx+1}" />
    </g:while>

    <!-- filters -->
    <ul class="accordion">
        <g:each var="filter" in="${filters}">
            <g:if test="${filter.visible}">
                <li>
                    <g:render template="/filter/${filter.template}" model="[filter: filter, company: company, hiddenFilters: hiddenFilters]"/>
                </li>
            </g:if>
        </g:each>
    </ul>

    <!-- filter controls -->
    <div class="btn-hold">
        <!-- apply filters -->
        <a class="submit-sm apply" data-cy="applyFilter" onclick="submitApply();">
            <span><g:message code="filters.apply.button"/></span>
        </a>

        <a class="submit-sm apply" data-cy="clearFilters" onclick="clearFilters();">
            <span><g:message code="filters.apply.clear"/></span>
        </a>

        <!-- add another filter -->
        <g:if test="${filters.find { !it.visible } && !SpringSecurityUtils.ifNotGranted("${PERMISSION_VIEW_ADD_FILTERS}")}">
            <div class="dropdown">
                <a class="submit-sm add open" data-cy="btnAddFilter" ><span><g:message code="filters.add.button"/></span></a>
                <div class="drop" data-cy="filterList">
                    <ul>
                        <g:each var="filter" in="${filters}">
                            <g:if test="${!filter.visible}">
                                <li data-cy="filters.${StringEscapeUtils.escapeHtml(filter?.field)}.title">
                                    <g:remoteLink controller="filter" action="add" params="${[name: filter.name] + hiddenFilters}" update="filters">
                                        <g:message code="filters.${StringEscapeUtils.escapeHtml(filter?.field)}.title"/>
                                    </g:remoteLink>
                                </li>
                            </g:if>
                        </g:each>
                    </ul>
                </div>
            </div>
        </g:if>
        <!-- Custom filters -->
        <g:if test = "${!isCustomFilter && !SpringSecurityUtils.ifNotGranted("${PERMISSION_VIEW_ADD_FILTERS}")}">
            <a class="submit-sm" onclick="$('#filter-custom-dialog').dialog('open');">
                <span id="submit-sm-span"><g:message code="filters.custom.button"/></span>
            </a>
        </g:if>
        <g:else>
            <div>
                <g:remoteLink controller="filter" action="load" params="[default:true, restoreFilters:true]" class="submit-sm apply" update="filters">
                    <span><g:message code="filters.default.button"/></span>
                </g:remoteLink>
            </div>
        </g:else>

        <!-- save current filter set-->
        <a class="submit-sm save" onclick="$('#filter-save-dialog').dialog('open');">
            <span><g:message code="filters.save.button"/></span>
        </a>

        <!-- load saved filter set -->
        <div class="dropdown">
            <a class="submit-sm load open"><span><g:message code="filters.load.button"/></span></a>
            <%
               FilterSet filterCurrent = (FilterSet)currentFilter[0]
               filtersets = filtersets-[filterCurrent];
            %>
            <g:if test="${filtersets}">
                <div class="drop" style="position: static;">
                    <ul>
                        <g:each var="filterset" in="${filtersets.sort{ it.id }}">
                            <li>
                                <g:remoteLink controller="filter" action="load" id="${filterset.id}" update="filters">
                                    ${StringEscapeUtils.escapeHtml(filterset?.name)}
                                </g:remoteLink>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </g:if>
        </div>

        <script type="text/javascript">
            $(function() {
                // reset popups and validations
                setTimeout(
                    function() {
                        initPopups();
                        initScript();

                        var validator = $('#filters-form').validate();
                        validator.init();
                        validator.hideErrors();
                    }, 500);

                // highlight active filters
                $('body').delegate('#filters-form', 'submit', function() {
                    $(this).find('li').each(function() {
                        $(this).find(':input[value!=""]').not(':checkbox').addClass('autoFill');
                        $(this).find(':input[value=""]').not(':checkbox').removeClass('autoFill');
                    });
                });
            });

            function submitApply () {
                if ($('#filters-form .error').size() < 1) {
                    $('#filters-form').submit();
                }
            }

            function clearFilters () {
                $('#filters-form').find('input').not(':checkbox')
                                                .not(':hidden')
                                                .each(function() {
                    $(this).val('');
                });

                $('#filters-form').find('select').each(function() {
                    $(this).val($(this).find(":first").val()).change()
                });

                $('#filters-form').find('input:checkbox').each(function() {
                    $(this).attr('checked', false);
                });

                $('input.autoFill').removeClass('autoFill');
                $('#clearFilter').click();
            }
        </script>
    </div>
</div>
