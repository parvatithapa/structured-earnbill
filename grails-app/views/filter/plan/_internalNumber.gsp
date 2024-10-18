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
  Plan internal number filter

  @author Leandro Zoi
  @since  04-10-2017
--%>

<div id="${filter.name}">
    <span class="title"><g:message code="filters.plan.i.internalNumber.title"/></span>
    <g:remoteLink class="delete" controller="filter" action="remove" params="[name: filter.name]" update="filters"/>

    <div class="slide">
        <fieldset>
            <div class="input-row">
                <div class="input-bg">
                    <g:textField  name = "filters.${filter.name}.stringValue"
                                 value = "${filter.stringValue}"
                                 class = "{validate:{ maxlength: 50 }} ${filter.value ? 'autoFill' : ''}"/>
                </div>
                <label for="filters.${filter.name}.stringValue"><g:message code="filters.plan.number.label"/></label>
            </div>
        </fieldset>
    </div>
</div>