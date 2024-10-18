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

<%@ page import="com.sapienter.jbilling.server.metafields.EntityType; com.sapienter.jbilling.server.metafields.MetaFieldBL" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO" %>
<%@ page import="com.sapienter.jbilling.server.util.Constants" %>

<%--
  _status

  @author Amol Gadre
  @since  18-10-2012
--%>

<div id="${filter.name}">
    <span class="title"><g:message code="filters.${filter.field}.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>
    
    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div id="custom-div" class="select-bg full-width">
                    <div class="select-holder select-holder_small"><span class="select-value"></span>
                        <g:set var="company" value="${CompanyDTO.get(session['company_id'])}"/>
                        <g:select style="float:left;"
                            name="${filter?.getName()}.fieldKeyData"
                            from="${MetaFieldBL.getAvailableFieldsList (session['company_id'], EntityType.PAYMENT)}"
                            optionKey="id" optionValue="name"
                            noSelection="['': message(code: 'filters.contactFieldTypes.empty')]" value="${filter?.fieldKeyData  as Integer?:''}" />
                    </div>
                </div>
                <div class="input-bg">
                    <g:textField  name = "filters.${filter.name}.stringValue"
                                 value = "${filter.stringValue}"
                                 class = "{validate:{ maxlength: 50 }} ${filter.value ? 'autoFill' : ''}"/>
                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.value.label"/></label>
            </div>
        </fieldset>
    </div>
    <g:set var="filter_selector" value="select[name='${filter?.getName()}.fieldKeyData']"/>
    <g:applyLayout name="select_small_script" template="/layouts/includes/select_small_script" model="[selector: filter_selector]"/>
</div>

