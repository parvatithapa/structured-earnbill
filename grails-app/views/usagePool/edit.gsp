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

<%@ page import="com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.util.db.LanguageDTO; org.apache.commons.lang.math.NumberUtils;" %>

<html>
<head>
    <meta name="layout" content="main" />
    
     <r:script disposition='head'>
        $(document).ready(function() {
            loadAvailableDecLang();
            setVisibilityOfActionsInfo();
        });

        function setVisibilityOfActionsInfo() {
            var attributeIndex = 1;
            while (showMoreInfo(attributeIndex)) {
                attributeIndex++;
            }
        }
        
        function addNewDescription(){
            var languageId = $('#newDescriptionLanguage').val();
            var previousDescription = $("#names div:hidden .descLanguage[value='"+languageId+"']");
            if(previousDescription.size()){
                previousDescription.parents('.row:first').show();
                previousDescription.parents('.row:first').find(".descDeleted").val(false);
                previousDescription.parents('.row:first').find(".descContent").val('');
            }else{
                var languageDescription = $('#newDescriptionLanguage option:selected').text();
                var clone = $('#descriptionClone').children().clone();
                var languagesCount = $('#names').children().size();
                var newName = 'usagePool.names['+languagesCount+']';
                clone.find("label").attr('for', newName+'.content');
                var label = clone.find('label').html();
                clone.find('label').html(label.replace('{0}', languageDescription));

                clone.find(".descContent").attr('id',newName+'.content');
                clone.find(".descContent").attr('name',newName+'.content');

                clone.find(".descLanguage").attr('id',newName+'.languageId');
                clone.find(".descLanguage").attr('name',newName+'.languageId');
                clone.find(".descLanguage").val(languageId);

                clone.find(".descDeleted").attr('id',newName+'.deleted');
                clone.find(".descDeleted").attr('name',newName+'.deleted');

                $('#names').append(clone);
            }
            removeSelectedLanguage();
            updateSelectLabel($("#newDescriptionLanguage"));
        }

        function removeDescription(elm){
            var div = $(elm).parents('.row:first');
            //set 'deleted'=true;
            div.find('.descDeleted').val(true);
            div.hide();

            if($("#addDescription").is(':hidden')){
                $("#addDescription").show();
            }
            var langId = div.find(".descLanguage").val();
            var langValue = getValueForLangId(langId);
            if(langValue){
                $("#newDescriptionLanguage").append("<option value='"+langId+"'>"+langValue+"</option>");
            }
            updateSelectLabel($("#newDescriptionLanguage"));
        }

        function loadAvailableDecLang(){
            var languages = $('#availableDescriptionLanguages').val().split(',');
            if(languages[0]!=''){
                $.each(languages,function(i,lang){
                   var lang = lang.split('-');
                   $("#newDescriptionLanguage").append("<option value='"+lang[0]+"'>"+lang[1]+"</option>");
                });
            }else{
                $('#addDescription').hide();
            }
        }

        function getValueForLangId(langId){
            var languages = $('#allDescriptionLanguages').val().split(',');
            if(languages[0]!=''){
                var value = false;
                $.each(languages,function(i,lang){
                   var lang = lang.split('-');
                   if(lang[0] == langId){
                       value = lang[1];
                   }
                });
                return value;
            }else{
                return false;
            }
            return false;
        }

        function removeSelectedLanguage(){
            $('#newDescriptionLanguage option:selected').remove();
            if(!$('#newDescriptionLanguage option').size()){
                $('#addDescription').hide();
            }
        }
        
        function showMoreInfo(attributeId) {
            var actionType = $('#usagePool\\.consumptionActions\\.' + attributeId + '\\.type').val();

            if (actionType == undefined) {
                return false
            }
            $('#notification-info-' + attributeId).hide();
            $('#fee-info-' + attributeId).hide();
            if (actionType.indexOf('Fee') > 0) {
                $('#fee-info-' + attributeId).show();
            } else if (actionType.indexOf('Notification') > 0) {
                $('#notification-info-' + attributeId).show();
            }
            return true;
        }
    </r:script>
    <r:external file="js/form.js" />
</head>
<body>
<div class="form-edit">

    <g:set var="isNew" value="${!usagePool || !usagePool?.id || usagePool?.id == 0}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="usagePool.add.title"/>
            </g:if>
            <g:else>
                <g:message code="usagePool.edit.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="usage-pool-edit-form" action="save" useToken="true">
            <fieldset>

                <!-- role information -->
                <div class="form-columns">
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="usagePool.label.id"/></content>

                            <g:if test="${!isNew}">
                                <span>${usagePool.id}</span>
                            </g:if>
                            <g:else>
                                <em><g:message code="prompt.id.new"/></em>
                            </g:else>

                            <g:hiddenField name="usagePool.id" value="${usagePool?.id}"/>
                            <g:hiddenField name="isNew" value="${!usagePool || !usagePool?.id || usagePool?.id == 0}"/>
                        </g:applyLayout>
                        
                        %{--<g:applyLayout name="form/select">--}%
                        	<g:render template="/usagePool/names" model="[usagePool: usagePool]" />
                        %{--</g:applyLayout>--}%
						
						<g:applyLayout name="form/input">
                            <content tag="label"><g:message code="usagePool.quantity"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">usagePool.quantity</content>
                           <g:textField class="field" name="usagePool.quantity" value="${usagePool?.quantity && NumberUtils.isNumber(usagePool.quantity) ? usagePool?.quantity : ''}"/>
                        </g:applyLayout>

						<g:applyLayout name="form/input">
		                    <content tag="label"><g:message code="usagePool.precedence"/></content>
		                    <content tag="label.for">usagePool.precedence</content>
		                    <g:textField class="field" name="usagePool.precedence" value="${usagePool?.precedence ? usagePool.precedence : -1}"/>
		                </g:applyLayout>

                        <div class="row">
                            <label for=""/><g:message code="usagePool.cyclePeriod"/><span id="mandatory-meta-field">*</span></label>

                            <div class="inp-bg inp2">
                                <g:textField class="field" name="usagePool.cyclePeriodValue"
                                             value="${usagePool?.cyclePeriodValue}" maxlength="2" size="3"/>
                            </div>

                            <g:applyLayout name="form/select_holder">
                                <content tag="label.for">usagePool.cyclePeriodUnit</content>
                                <g:select from="${cyclePeriods}"
                                          name="usagePool.cyclePeriodUnit"
                                          value="${usagePool?.cyclePeriodUnit ?: Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS}"/>
                            </g:applyLayout>
                        </div>

                        <g:applyLayout name="form/select_multiple">
                            <content tag="label"><g:message code="usagePool.itemTypes"/></content>
                            <content tag="label.for">usagePool.itemTypes</content>

                            <g:set var="types" value="${usagePool?.itemTypes?.collect{ it as Integer }}"/>
                            <g:select name="usagePool.itemTypes" multiple="true"
                                      from="${categories}"
                                      optionKey="id"
                                      optionValue="${{it.description + (it.allowAssetManagement == 1 ? "*" : "")}}"
                                      value="${types ?: categoryId}"
                                      onchange="checkAssetManagement(this)"/>
                            <label for="">&nbsp;</label>
                            <span class="normal">*&nbsp;<g:message code="usagePool.categories.with.assetmanagement"/></span>
                        </g:applyLayout>
                        <g:applyLayout name="form/select_multiple">
                            <content tag="label"><g:message code="usagePool.items"/></content>
                            <content tag="label.for">usagePool.itemTypes</content>

                            <g:set var="types" value="${usagePool?.items?.collect{ it as Integer }}"/>
                            <g:select name="usagePool.items" multiple="true"
                                      from="${products}"
                                      optionKey="id"
                                      optionValue="${{it.getDescription(session['language_id'])}}"
                                      value="${types ?: productId}"
                                      />
                            <label for="">&nbsp;</label>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="usagePool.resetValue"/></content>
                            <content tag="label.for">usagePool.usagePoolResetValue</content>
                            <g:select from="${resetValues}"
                                      name="usagePool.usagePoolResetValue"
                                      value="${usagePool?.usagePoolResetValue}"/>
                        </g:applyLayout>
                    </div>
                	
                	<div class="column" style="width: 50%;">
					<div class="row" style="width: 90%;">
						<g:applyLayout name="form/text">
                           <div style="font-size: 12px; font-style: normal; font-weight: normal; vertical-align: bottom;" align="center">
	                            <table style="width: 80%;">
	                            <tr>
	                            	<td  id="custom-td6"> <g:message code="usagePool.mode.consumption.percentage"/> </td>
	                            	<td style="width: 72%;text-align: center;"> <g:message code="usagePool.mode.consumption.action"/> </td>	
	                           	</tr>
	                            </table>
                          	</div>
                        </g:applyLayout>
					</div>
                     <div class="row">
						<g:render template="/usagePool/consumption" model="[usagePool: usagePool, consuptionActions: consuptionActions]" />
					</div>
				</div>
                </div>
				
				<!-- spacer -->
				<div>
					<br/>&nbsp;
				</div>

                <div class="buttons">
                    <ul>
                        <li>
                            <a onclick="$('#usage-pool-edit-form').submit()" class="submit save button-primary"><span><g:message code="button.save"/></span></a>
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
