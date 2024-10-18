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
  Payment "isReview" filter.

  @author Vikas Bodani
  @since  09-Feb-2011
--%>
<div id="${filter.name}">
    <span class="title"><g:message code="filters.isReview.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:set var="filter_name" value="filters.${filter.name}.integerValue"/>
                    <g:set var="filter_value" value="${filter.integerValue ?: 0}"/>
                    <g:set var="filter_from" value="${[0, 1]}"/>
                    <g:set var="filter_valueMessagePrefix" value="filters.isReview"/>
                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_value: filter_value,
                            select_from: filter_from,
                            select_valueMessagePrefix: filter_valueMessagePrefix
                    ]"/>

                </div>
                <label for="filters.${filter.name}.integerValue"><g:message code="filters.isReview.label"/></label>
            </div>
        </fieldset>
    </div>

    <g:set var="filter_selector" value="select[name='filters.${filter.name}.integerValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>
