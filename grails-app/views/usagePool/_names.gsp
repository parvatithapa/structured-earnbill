%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO; com.sapienter.jbilling.server.util.db.LanguageDTO;" %>

<%--
  @author Amol Gadre
  @since  29-Nov-2013
--%>

<div class="row" id='addDescription'>
    <div class="add-desc">
        <label><g:message code='usagePool.detail.name.label'/><span id="mandatory-meta-field">*</span></label>
        <g:applyLayout name="form/select_holder">
            <content tag="include.script">true</content>
            <content tag="label.for">newDescriptionLanguage</content>
            <select name="newDescriptionLanguage" id="newDescriptionLanguage"></select>
        </g:applyLayout>

        <a class="plus-icon" onclick="addNewDescription()">&#xe026;</a>
    </div>
</div>

<div id="descriptionClone" style="display: none;">
    <g:applyLayout name="form/description">
        <content tag="label"><g:message code="discount.detail.name.label"/></content>
        <content tag="label.for">desCloneContent</content>

        <input type="text" id="desCloneContent" class="descContent field" size="26" maxlength="50" value="" name="desCloneContent">
        <input type="hidden" id="desCloneLangId" class="descLanguage" value="" name="desCloneLangId">
        <input type="hidden" id="desCloneDeleted" class="descDeleted" value="" name="desCloneDeleted">
        <content tag="icon"><a class="plus-icon" onclick="removeDescription(this)">&#xe000;</a></content>
    </g:applyLayout>
</div>

<g:set var="availableDescriptionLanguages" value="${LanguageDTO.list().collect {it.id+'-'+it.description}.sort{a,b-> a.compareTo(b)}}" />

<div id="names">
    <g:each in="${usagePool?.names}" var="description" status="index">
        <g:if test="${description?.languageId}">
            <g:applyLayout name="form/description">
                <g:set var="currentLang" value="${LanguageDTO.get(usagePool?.names[index]?.languageId)}" />
                <g:set var="availableDescriptionLanguages" value="${availableDescriptionLanguages - (currentLang?.id+'-'+currentLang?.description)}" />

                <content tag="label"><g:message code="discount.detail.name.label" args="${[currentLang?.description]}"/></content>
                <content tag="label.for">usagePool.names[${index}]?.content</content>

                <g:textField name="usagePool.names[${index}].content" class="descContent field" maxlength="50" value="${usagePool?.names[index]?.content}"/>
                <g:hiddenField name="usagePool.names[${index}].languageId" class="descLanguage" value="${currentLang?.id}"/>
                <g:hiddenField name="usagePool.names[${index}].deleted" value="" class="descDeleted"/>
                <content tag="icon"><a class="plus-icon" onclick="removeDescription(this)">&#xe000;</a></content>
            </g:applyLayout>
        </g:if>
    </g:each>
</div>

<g:set var="allDescriptionLanguages" value="${LanguageDTO.list().collect {it.id+'-'+it.description}}" />
<g:hiddenField name="allDescriptionLanguages" value="${allDescriptionLanguages?.join(',')}"/>
<g:hiddenField name="availableDescriptionLanguages" value="${availableDescriptionLanguages?.join(',')}"/>

