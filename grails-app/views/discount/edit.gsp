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

<%@ page import="com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType; com.sapienter.jbilling.server.util.db.LanguageDTO; org.apache.commons.lang.WordUtils; org.apache.commons.lang.StringUtils" %>
<html>
<head>
    <meta name="layout" content="main" />

    <r:script disposition='head'>
        $(document).ready(function() {
            loadAvailableDecLang();

            $('input[name="discount.rate"]').keyup(function() {
                if (/\d*$/.exec(this.value)[0].length > 4) {
                    this.value = parseFloat(this.value, 10).toFixed(4);
                }
            });
        });
        
        function validateDate(element) {
            var dateFormat= "<g:message code="date.format"/>";
            if(!isValidDate(element, dateFormat)) {
                $("#error-messages").css("display","block");
                $("#error-messages ul").css("display","block");
                $("#error-messages ul").html("<li><g:message code="invalid.date.format"/></li>");
                element.focus();
                return false;
            } else {
                return true;
            }
        }
        
        function addNewDescription(){
            var languageId = $('#newDescriptionLanguage').val();
            var previousDescription = $("#descriptions div:hidden .descLanguage[value='"+languageId+"']");
            if(previousDescription.size()){
                previousDescription.parents('.row:first').show();
                previousDescription.parents('.row:first').find(".descDeleted").val(false);
                previousDescription.parents('.row:first').find(".descContent").val('');
            }else{
                var languageDescription = $('#newDescriptionLanguage option:selected').text();
                var clone = $('#descriptionClone').children().clone();
                var languagesCount = $('#descriptions').children().size();
                var newName = 'discount.descriptions['+languagesCount+']';
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

                $('#descriptions').append(clone);
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
        
        function addDiscountAttribute(element, attributeIndex) {
            $('#attributeIndex').val(attributeIndex);

            $.ajax({
               type: 'POST',
               url: '${createLink(action: 'addAttribute')}',
               data: $('#discountStrategy').parents('form').serialize(),
               success: function(data) { $('#discountStrategy').replaceWith(data); }
            });
        }

        function removeDiscountAttribute(element, attributeIndex) {
            $('#attributeIndex').val(attributeIndex);

            $.ajax({
               type: 'POST',
               url: '${createLink(action: 'removeAttribute')}',
               data: $('#discountStrategy').parents('form').serialize(),
               success: function(data) { $('#discountStrategy').replaceWith(data); }
            });
        }
    </r:script>
    <r:external file="js/form.js" />
</head>
<body>
<div class="form-edit">

    <g:set var="isNew" value="${!discount || !discount?.id || discount?.id == 0}"/>
    <g:set var="types" value="${DiscountStrategyType.values()}"/>
    <g:set var="type" value="${!StringUtils.isEmpty(discount?.type?.trim()) ? DiscountStrategyType.valueOf(discount.type) : (types ? types[0] : null)}"/>
    <g:set var="templateName" value="${WordUtils.uncapitalize(WordUtils.capitalizeFully(type.name(), ['_'] as char[]).replaceAll('_',''))}"/>


    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                New Discount
            </g:if>
            <g:else>
                Edit Discount
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:form name="discount-edit-form" action="saveDiscount" useToken="true">
            <fieldset>
                <div class="form-columns">

                    <!-- discount details column -->
                    <div class="column">
						<g:applyLayout name="form/text">
                            <content tag="label"><g:message code="discount.id"/></content>

                            <g:if test="${isNew}"><em><g:message code="prompt.id.new"/></em></g:if>
                            <g:else>${discount?.id}</g:else>

                            <g:hiddenField name="discount.id" value="${discount?.id}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="discount.code"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">discount.code</content>
                            <g:textField class="field" name="discount.code" value="${discount?.code}" size="20"/>
                        </g:applyLayout>
                        
                        <g:render template="/discount/descriptions" model="[discount: discount]" />
                        
                        <g:render id="strategyTemplate" template="/discount/strategy/${templateName}" model="[discount: discount]" />
                        
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="discount.rate"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">discount.rate</content>
                      		<g:textField class = "field"
                                          name = "discount.rate"
                                         value = "${discount?.rate?.isNumber() ? formatNumber(number: discount?.rate?:0, formatName: 'price.format')
                                                                               : discount?.rate}"
                                          size = "20"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="discount.apply.to.all.periods"/></content>
                            <content tag="label.for">discount.applyToAllPeriods</content>
                            <g:checkBox      id = "discount.applyToAllPeriods"
                                          class = "cb checkbox"
                                           name = "discount.applyToAllPeriods"
                                        checked = "${discount?.applyToAllPeriods}"/>

                        </g:applyLayout>

                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="discount.startDate"/></content>
                            <content tag="label.for">discount.startDate</content>
                            <g:textField class="field" name="discount.startDate"
                                         value="${formatDate(date: discount?.startDate, formatName:'datepicker.format')}" onblur="validateDate(this);" />
                        </g:applyLayout>
                        
                        <g:applyLayout name="form/date">
                            <content tag="label"><g:message code="discount.endDate"/></content>
                            <content tag="label.for">discount.endDate</content>
                            <g:textField class="field" name="discount.endDate"
                                         value="${formatDate(date: discount?.endDate, formatName:'datepicker.format')}" onblur="validateDate(this);" />
                        </g:applyLayout>

                    </div>
                    
                </div>

				<!-- spacer -->
				<div>
					<br/>&nbsp;
				</div>

                <div class="buttons">
                    <ul>
                        <li>
                            <a onclick="$('#discount-edit-form').submit()" class="submit save button-primary"><span><g:message code="button.save"/></span></a>
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
