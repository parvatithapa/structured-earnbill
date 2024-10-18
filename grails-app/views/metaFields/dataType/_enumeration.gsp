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

<%@ page import="com.sapienter.jbilling.server.util.EnumerationWS; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.util.db.EnumerationDTO" %>

<g:set var="enumValues" value="${null}"/>
<g:set var="wssb" bean="${com.sapienter.jbilling.server.util.Context.Name.WEB_SERVICES_SESSION.name}"/>
<%
    EnumerationWS enumWS = wssb.getEnumerationByNameAndCompanyId(field.getName(), field.entityId)
    if(enumWS) {
        enumValues= []
        enumValues.addAll(enumWS.values.collect({it.value}))
    }

    def dependencyMetaFieldNames
    def dependencies = ""
    try{
        dependencyMetaFieldNames = field.getDependentMetaFields() ? field.getDependentMetaFields()*.collect {it.name + "-" + it.id + "-dependency"} : '';
        dependencies = dependencyMetaFieldNames.join(" ").replaceAll("\\[", "").replaceAll("\\]","");
    }catch (Exception e){

    }
%>

<g:if test="${field?.helpContentURL || field.helpDescription}">
    <style>
    .help_select{
        width: 51% !important;
    }
    </style>

</g:if>

<g:applyLayout name="form/select">
    <content tag="label"><g:message code="${field.name}"/><g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if></content>
    <content tag="label.for">metaField_${field.id}.value</content>
    <content tag="include.script">true</content>
    <g:select
            class="field ${validationRules} ${dependencies} ${field.fieldUsage ? 'field_usage':''} help_select"
            id="enumerationMetaField "
            name="metaField_${field.id}.value"
            from="${enumValues}"
            optionKey=""
            noSelection="['':'Please select a value']"
            value="${fieldValue}"/>
    <div class="fieldValue${field?.id}" style="display: none">${fieldValue}</div>
    <g:render template="/metaFields/metaFieldHelp" model="[field:field]" />
</g:applyLayout>


<script type="text/javascript">

    $(function () {
    <g:if test="${field}">
        if ("${dependencies && !dependencies.isEmpty()}" == "true") {
            var asd = "${dependencies}";
            var splitDependency = asd.split(" ");
            var i;
            for (i = 0; i < splitDependency.length; i++) {
                var parentMetaFieldId = splitDependency[i].split('-')[1];
                var parentMetaFieldName = getMetaFieldNameObj(parentMetaFieldId);
                if($(parentMetaFieldName).val()!=''){
                    var dependencyMap = new Object();
                    var j;
                    for (j = 0; j < splitDependency.length; j++) {
                        dependencyMap[splitDependencyfun(splitDependency[j])[0]] = $(getMetaFieldNameObj(splitDependencyfun(splitDependency[j])[1])).val();
                    }
                    resolveDependency(this, "${field?.id}", dependencyMap, "${field.name}")
                }
                $(parentMetaFieldName).on('change', function (e) {
                    var dependencyMap = new Object();
                    var j;
                    for (j = 0; j < splitDependency.length; j++) {
                        dependencyMap[splitDependencyfun(splitDependency[j])[0]] = $(getMetaFieldNameObj(splitDependencyfun(splitDependency[j])[1])).val();
                    }
                    resolveDependency(this, "${field.id}", dependencyMap, "${field.name}")
                });
            }
        }
    </g:if>
    });

    function getMetaFieldNameObj(metaFieldId) {
        return document.getElementsByName("metaField_" + metaFieldId + ".value")[0]
    }

    function splitDependencyfun(dependency) {
        return dependency.split("-");
    }

    function resolveDependency(obj, metafieldId, map, searchName) {

        $.ajax({
            type: 'POST',
            url: '${createLink(controller: 'route', action: 'resolveDependency')}',
            data: {
                metafieldId: metafieldId,
                metafieldValue: obj.value,
                searchName: searchName,
                dependentFields: JSON.stringify(map),
                accountTypeId:$("[name='user.accountTypeId']").val()

            },
            success: function (data) {
                var targetMetaField = getMetaFieldNameObj(metafieldId);
                $(targetMetaField).html(data);
                $(targetMetaField).val($(".fieldValue"+metafieldId+"").text());
            }
        });
    }
</script>
