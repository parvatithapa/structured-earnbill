<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO" %>
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

<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>

<r:script disposition="head">
var jCount=0;
var newHtml= "<tr><td>jCount</td>"
    + "<td><input id='values[jCount].id' type='hidden' value='0' name='values[jCount].id'>"
    + "<input id='values[jCount].value' class='inp-bg field' type='text' value='' name='values[jCount].value'></td>"
    + "<td><a class='plus-icon default-height' onclick='removeEnumerationValue($(this))'>&nbsp;&#xe000;</a></td></tr>";

function addEnumerationValue(element) {

    modHtml= newHtml.replace(/jCount/g, jCount);

    var divId= "enum-body";
    var div= document.getElementById(divId)
    $(div).append(modHtml)
    ++jCount;
}

function removeEnumerationValue(element) {
    var divRow = $(element).parents('tr')[0];
    $(divRow).empty();
    $(divRow).remove();
}

function validateEnumerationValues(valueFields) {

	var hideEnumValValidationMsg = false; 
	for (var i=0; i < valueFields.length; i++) {
    	valueField = valueFields[i];
    	if(valueField.name.indexOf('.value') != -1) {
        	var valueFieldValue = valueField.value;
			if (valueFieldValue == null || $.trim(valueFieldValue) == '' || $('#name').val().trim()=='') {
				if(valueFieldValue == null || $.trim(valueFieldValue) == ''){
					hideEnumValValidationMsg = true;
				}

                /*
                *  if either of the error is present, show the error div
                * */
                if (($('input:text[name=name]').val() == '') || (hideEnumValValidationMsg==true)){
                    $("#error-messages").show();
                    $("#error-messages ul").show();
                }else{
                    $("#error-messages").hide();
                    $("#error-messages ul").hide();
                }

                $("#error-messages ul").html('');

                var enumNameEmptyErMsg = '<li>${g.message(code: "enumeration.name.empty")}</li>';
                var enumValMissingErMsg = "<li><g:message code="enumeration.value.missing"/></li>";

                if ($('input:text[name=name]').val() == ''){
                    $("#error-messages ul").append(enumNameEmptyErMsg );
                }

				if(hideEnumValValidationMsg==true){
	                $("#error-messages ul").append(enumValMissingErMsg);
				}

				return -1;
			}
		}
	}
	$("#error-messages ul").html("");
	return 0;
}
</r:script>

<div class="form-edit">

    <g:set var="isNew" value="${!enumeration || !enumeration?.id || enumeration?.id == 0}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="enumeration.add.title"/>
            </g:if>
            <g:else>
                <g:message code="enumeration.edit.title"/>
            </g:else>
        </strong>
    </div>
    
    <div class="form-hold">
        <g:form name="enumeration-edit-form" action="save" useToken="true">
            
            <g:hiddenField name="action_name" value=""/>
            <g:hiddenField name="remove_id" value=""/>

            <fieldset>

                <!-- enumeration values -->
                <div class="form-columns">
                    
                    <!-- column 1 -->
                    <div class="column" id="updateRemove">
                        <table>
                            <tbody id="enum-body">

                            <!-- enumeration -->
                            <g:hiddenField name="id" value="${accountType?.id ?: null}"/>
                            <g:hiddenField name="id" value="${enumeration?.id}"/>
                            <g:hiddenField name="entityId" value="${enumeration?.entityId}"/>
                                <tr>
                                    <td><g:message code="enumeration.label.name"/><span id="mandatory-meta-field">*</span></td>
                                    <td><g:textField class="inp-bg field" name="name" value="${enumeration?.name}"/></td>

                                </tr>
                                <tr>
                                    <td class="pad-below" colspan="2"></td>
                                </tr>
                                <tr>
                                    <td class="pad-below" colspan="3">
                                        <g:message code="enumeration.label.message"/>
                                        &nbsp;
                                    <a class="plus-icon default-height" onclick="addEnumerationValue(this)">
                                        &#xe026;
                                    </a>
                                    </td>
                                </tr>
                        <g:set var="count" value="${-1}"/>
                        <g:each var="values" status="n" in="${enumeration?.values}">
                            <g:set var="count" value="${n}"/>
                            <tr>
                                <td class="tiny pad-below">${count}</td>
                                <td>
                                    <g:hiddenField name="values[${n}].id" value="${values?.id}"/>
                                <g:textField
                                    class="inp-bg field"
                                    name="values[${n}].value"
                                    value="${values.value}"
                                />
                                </td>
                                <td>
                                <a class="plus-icon default-height" onclick="removeEnumerationValue($(this))">&nbsp;&#xe000;</a>
                                </td>
                            </tr>
                        </g:each>
                        <g:if test="${count.toInteger() == Integer.valueOf(-1)}">
                            <g:set var="count" value="${0}"/>
                            <tr>
                                <td class="tiny pad-below">${count}</td>
                                <td>
                                    <g:textField
                                            name="values[${count}].value"
                                            class="inp-bg field"
                                            value=""/>
                                    <g:hiddenField name="values[${count}].id" value="0"/>
                                </td>
                                <td>
                                </td>
                            </tr>
                        </g:if>
                        <script type="text/javascript">
                             jCount= ${++count}
                        </script>
                        </tbody>
                    </table>

                    </div>
                </div>
                
                <!-- spacer -->
                <div>
                    &nbsp;<br/>
                </div>

                <div class="buttons">
                    <ul>
                        <li>
                            <a onclick="if (validateEnumerationValues($('input[name*=values]')) != -1) {$('#enumeration-edit-form').submit();}"
                            	class="submit save button-primary"><span><g:message code="button.save"/></span></a>
                        </li>
                        <li>
                	            <g:link action="list" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link>
                        </li>
                    </ul>
                </div>

            </fieldset>
        
            
        </g:form>
    </div>

</div>
</body>
</html>
