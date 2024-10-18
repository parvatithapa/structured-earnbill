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
  Generic ID filter template.

  This filter template uses the defined filter field for translated message keys and can be re-used by
  any filter accepting an integer value.

  @author Brian Cowdery
  @since  30-11-2010
--%>
<%@ page import="jbilling.FilterSet" %>

<div id="${filter.name}">
    <span class="title"><g:message code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="${[name: filter.name] + hiddenFilters}" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <g:if test="${FilterSet.TABLES_WITH_UUID.contains(session['current_filter_type']) && filter.field == 'id'}">
                    <div class="input-bg">
                        <g:textField  name = "filters.${session['current_filter_type']}-EQ_Id.stringValue"
                                     value = "${filter.stringValue}"
                                     class = "${filter.value ? 'autoFill' : ''}"/>
                    </div>
                    <label for="filters.${filter.name}-EQ_Id.integerValue"><g:message code="filters.${filter.field}.label"/></label>
                </g:if>
                <g:else>
                    <div class="input-bg">
                        <g:textField  name = "filters.${filter.name}.integerValue"
                                     value = "${filter.integerValue}"
                                     class = "${filter.value ? 'autoFill' : ''}"/>
                        <script>
                            $('input[name="filters.${filter.name}.integerValue"]').keyup(function() {
                                if (/\D/g.test(this.value)) this.value = this.value.replace(/\D/g,'')
                            });
                        </script>
                    </div>
                    <label for="filters.${filter.name}.integerValue"><g:message code="filters.${filter.field}.label"/></label>
                </g:else>
            </div>
        </fieldset>
    </div>
</div>
