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

<%@ page import="com.sapienter.jbilling.server.user.db.UserStatusDAS" %>

<%--
  _status

  @author Brian Cowdery
  @since  30-11-2010
--%>


<div id="${filter.name}">
    <span class="title"><g:message code="filters.${filter.field}.title"/> </span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="select-bg">
                    <g:set var="filter_name" value="filters.${filter.name}.stringValue"/>
                    <g:set var="filter_value" value="${filter.stringValue}"/>
                    <g:set var="filter_from" value="${com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus}"/>
                    <g:set var="filter_optionKey" value="name"/>
                    <g:set var="filter_noSelection" value="['': message(code: 'filters.status.empty')]"/>
                    <g:applyLayout name="select_small" template="/layouts/includes/select_small" model="[
                            select_name: filter_name,
                            select_value: filter_value,
                            select_from: filter_from,
                            select_optionKey: filter_optionKey,
                            select_noSelection: filter_noSelection
                    ]">
                    </g:applyLayout>

                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.status.label"/></label>
            </div>
        </fieldset>
    </div>

    <g:set var="filter_selector" value="select[name='filters.${filter.name}.stringValue']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>

