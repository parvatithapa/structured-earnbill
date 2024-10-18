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

<%@ page import="com.sapienter.jbilling.server.item.CurrencyBL; com.sapienter.jbilling.common.CommonConstants;" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>
<%@ page import="com.sapienter.jbilling.server.user.db.CompanyDTO;" %>
<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDAS;" %>
<%@ page import="org.apache.commons.lang.WordUtils;" %>
<%@ page import="com.sapienter.jbilling.server.pricing.RatingUnitBL" %>
<%@ page import="com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS" %>
<%@ page import="com.sapienter.jbilling.server.usageratingscheme.service.UsageRatingSchemeBL" %>
<%@ page import="com.sapienter.jbilling.server.item.RatingConfigurationBL" %>
<%@ page import="com.sapienter.jbilling.server.util.db.LanguageDTO" %>
<%--
  Editor form for price models.
--%>

<!-- model and date to display -->
<g:set var="startDateRating" value="${startDateRating ?: TimezoneHelper.currentDateForTimezone(session['company_timezone'])}"/>
<g:set var="currentRatingConfig" value="${currentRatingConfig?:RatingConfigurationBL.getWsRatingConfigurationForDate(allRatingConfig,startDateRating)?:null}"/>
<!-- local variables -->
<g:set var="isNewConfig" value="${allRatingConfig == null || allRatingConfig.isEmpty()}"/>
<g:set var="isNewProduct" value="${!product || !product?.id || product?.id == 0}"/>

<div id="ratingConfig">
    <g:hiddenField name="unsaved" value="${unsaved}"/>

    <g:if test="${!isNewConfig && allRatingConfig}">
        <div id="timeline">
            <div class="form-columns">
                <ul>
                    <g:each var="configEntry" in="${allRatingConfig.entrySet()}">
                        <g:if test="${(currentRatingConfig?.equals(configEntry.getValue())) || startDateRating.equals(configEntry.getKey())}">
                            <li class="current">
                                <g:set var="startDateRating" value="${configEntry.getKey()}"/>
                                <g:set var="date" value="${formatDate(date: startDateRating)}"/>
                                <a onclick="editRatingDate('${date}')">${date}</a>
                            </li>
                        </g:if>
                        <g:else>
                            <li>
                                <g:set var="date" value="${formatDate(date: configEntry.getKey())}"/>
                                <a onclick="editRatingDate('${date}')">${date}</a>
                            </li>
                        </g:else>
                    </g:each>

                    <g:if test="${!allRatingConfig.containsKey(startDateRating)}">
                        <li class="current">
                            <g:set var="date" value="${formatDate(date: startDateRating)}"/>
                            <a onclick="editRatingDate('${date}')">${date}</a>
                        </li>
                    </g:if>


                    <li class="new">
                        <a onclick="addRatingDate()"><g:message code="button.add.rating.date"/></a>
                    </li>
                </ul>
            </div>
        </div>
    </g:if>

    <!-- rating configuration -->
    <div class="form-columns">
        <div class="row">
            <g:if test="${startDateRating.getTime() == CommonConstants.EPOCH_DATE.getTime()}">
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="plan.item.start.date"/></content>
                    <g:formatDate date="${startDateRating}" formatName="date.format"/>
                    <g:hiddenField name="startDateRating" value="${formatDate(date: startDateRating, formatName: 'date.format')}"/>
                </g:applyLayout>
            </g:if>
            <g:else>
                <g:applyLayout name="form/date">
                    <content tag="label"><g:message code="plan.item.start.date"/></content>
                    <content tag="label.for">startDateRating</content>
                    <content tag="label.class">toolTipElement</content>
                    <content tag="label.title"><g:message code="ratingConfiguration.COMMON.start_date.tooltip.message"/></content>
                    <g:textField onfocus="disablingToolTip(this.id);" class="field toolTipElement" title="${message(code: 'ratingConfiguration.COMMON.start_date.tooltip.message')}" id="startDateRating" name="startDateRating" value="${formatDate(date: startDateRating, formatName: 'datepicker.format')}" onblur="isValidStartRatingDate(this);" />
                    <g:hiddenField name="originalStartDateRating" value="${formatDate(date: startDateRating, formatName: 'date.format')}"/>
                </g:applyLayout>

            </g:else>
        </div>

        <div class="row">
        <!-- Rating Unit drop down -->
            <g:isRoot>
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="ratingUnit.product.rating.unit.name"/></content>
                    <content tag="label.for">ratingUnitId</content>
                    <content tag="label.class">toolTipElement</content>
                    <content tag="include.script">true</content>
                    <g:select name="ratingUnitId"
                              class="toolTipElement"
                              from="${allRatingUnits}"
                              optionKey="id" optionValue="name"
                              noSelection="['': message(code: 'default.no.selection')]"
                              value="${currentRatingConfig?.ratingUnit?.id}"/>
                </g:applyLayout>
            </g:isRoot>
            <g:isNotRoot>
                <g:hiddenField name="product.priceModelCompanyId" value="${product?.priceModelCompanyId}"/>
            </g:isNotRoot>
        </div>

        <div class="row">
        <!-- Rating Scheme drop down -->
            <g:isRoot>
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="usageRatingScheme.product.rating.scheme.name"/></content>
                    <content tag="label.for">ratingSchemeId</content>
                    <content tag="label.class">toolTipElement</content>
                    <content tag="include.script">true</content>
                    <g:select name="ratingSchemeId"
                              class="toolTipElement"
                              from="${allusageRatingSchemes}"
                              optionKey="id" optionValue="ratingSchemeCode"
                              noSelection="['': message(code: 'default.no.selection')]"
                              value="${currentRatingConfig?.usageRatingScheme?.id}"/>
                </g:applyLayout>
            </g:isRoot>
        </div>

        <div class="row" id="addPricingUnit" class="add-desc">
            <label><g:message code='ratingConfiguration.add.pricingUnit.title'/></label>
            <g:applyLayout name="form/select_holder">
                <content tag="label.for">newPricingUnitLanguage</content>
                <select name="newPricingUnitLanguage" id="newPricingUnitLanguage"></select>

            </g:applyLayout>
        <a class="plus-icon" onclick="addNewPricingUnit()">&#xe026;</a>
        </div>

        <div id="pricingUnitClone" style="display: none;">
            <g:applyLayout name="form/description">
                <content tag="label"><g:message code="ratingConfiguration.pricingunit.label"/></content>
                <content tag="label.for">puCloneContent</content>

                <input type="text" id="puCloneContent" class="descContent field" size="26" value="" name="puCloneContent">
                <input type="hidden" id="puCloneLangId" class="descLanguage" value="" name="puCloneLangId">
                <input type="hidden" id="puCloneDeleted" class="descDeleted" value="" name="puCloneDeleted">
                <content tag="icon">
                    <a class="delete plus-icon" onclick="removePricingUnit(this)">&#xe000;</a>
                </content>
            </g:applyLayout>
        </div>

        <g:set var="availablePricingUnitLanguages" value="${LanguageDTO.list().collect {it.id+'-'+it.description}.sort{a,b-> a.compareTo(b)}}"></g:set>

        <div id="pricingUnits">
        <g:each in="${currentRatingConfig?.pricingUnit}" var="pricingUnit" status="index">
            <g:if test="(${pricingUnit?.languageId })">
              <g:if test="${!pricingUnit?.isDeleted()}">
                <g:applyLayout name="form/description">
                    <g:set var="currentLang" value="${LanguageDTO.get(pricingUnit?.languageId)}"></g:set>
                    <g:set var="availablePricingUnitLanguages" value="${availablePricingUnitLanguages - (currentLang?.id+'-'+currentLang?.description)}"></g:set>
                    <content tag="label"><g:message code="ratingConfiguration.pricingunit.label" args="${[currentLang?.description]}"/>
                    </content>
                    <content tag="label.for">pricingUnit.content</content>
                    <g:textField name="pricingUnit.${index}.content" class="descContent field" value="${pricingUnit.content}"/>
                    <g:hiddenField name="pricingUnit.${index}.languageId" class="descLanguage" value="${currentLang?.id}"/>
                    <g:hiddenField name="pricingUnit.${index}.deleted" value="" class="descDeleted"/>
                    <content tag="icon">
                        <a class="delete plus-icon" onclick="removePricingUnit(this)">&#xe000;</a>
                    </content>
                </g:applyLayout>
              </g:if>
            </g:if>
        </g:each>
        </div>

        <g:set var="allPricingLanguages" value="${LanguageDTO.list().collect {it.id+'-'+it.description}}"></g:set>
        <g:hiddenField name="availablePricingUnitLanguages" value="${availablePricingUnitLanguages?.join(',')}"/>
        <g:hiddenField name="allPricingUnitLanguages" value="${allPricingLanguages?.join(',')}"/>
    </div>

    <!-- spacer -->
    <div>
        <br/>&nbsp;
    </div>

    <div class="btn-row">
        <g:if test="${!hideSaveOption && !(!isNewConfig && allRatingConfig) && !isNewProduct}">
            <a class="submit save" onclick="saveRatingDate()"><span><g:message code="button.save"/></span></a>
        </g:if>
        <g:if test="${!isNewConfig && allRatingConfig}">
            <a class="submit save" onclick="saveRatingDate()"><span><g:message code="button.save"/></span></a>
            <a class="submit delete" onclick="removeRatingDate()"><span><g:message code="button.delete"/></span></a>
        </g:if>
    </div>
</div>

<script type="text/javascript">
    $(document).ready(function() {
        addDataTooltip();
        $('.help-dialog').dialog({
            appendTo: "#help-dialog-placeholder",
            autoOpen: false,
            height: 380,
            width: 550,
            resizable: false,
            modal: true,
            // Workaround for modal dialog dragging jumps
            create: function(event){
                $(event.target).parent().css('position', 'fixed');
            },
            buttons: {
                "${g.message(code: 'button.close')}": function() {
                    $( this ).dialog( "close" );
                }
            }
        });

        loadAvailableLang();
    });

    function saveRatingDate() {
        if(!isValidStartRatingDate($('#startDateRating'))) {
            return false;
        }
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'saveRatingDate')}',
            data: $('#ratingConfig').parents('form').serialize(),
            success: function(data) { $('#ratingConfig').replaceWith(data); }
        });
    }

    function addRatingDate() {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'addRatingDate')}',
            data: $('#ratingConfig').parents('form').serialize(),
            success: function(data) { $('#ratingConfig').replaceWith(data); }
        });
    }

    function editRatingDate(date) {
        $('#startDateRating').val(date);
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'editRatingDate')}',
            data: $('#ratingConfig').parents('form').serialize(),
            success: function(data) { $('#ratingConfig').replaceWith(data); }
        });
    }

    function removeRatingDate() {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'removeRatingDate')}',
            data: $('#ratingConfig').parents('form').serialize(),
            success: function(data) { $('#ratingConfig').replaceWith(data); }
        });
    }

    function disablingToolTip(object) {
        $( "#" + object ).tooltip( "disable" );
    }

    function isValidStartRatingDate(dateControl) {
        $( "#" + dateControl.id ).tooltip( "enable" );
        if(!isValidDate(dateControl, startDateFormat)) {
            $("#error-messages ul").css("display","block");
            $("#error-messages ul").html("<li><g:message code="product.invalid.startdate.format"/></li>");
            return false;
        } else {
            return true;
        }
    }

    function loadAvailableLang(){
        var languages = $('#availablePricingUnitLanguages').val().split(',');
        if(languages[0]!=''){
            $.each(languages,function(i,lang){
                var lang = lang.split('-');
                $("#newPricingUnitLanguage").append("<option value='"+lang[0]+"'>"+lang[1]+"</option>");
            });
            updateSelectLabel($("#newPricingUnitLanguage"));
        }else{
            $('#addPricingUnit').hide();
        }
    }

    function addNewPricingUnit(){
        var languageId = $('#newPricingUnitLanguage').val();
        var previousDescription = $("#pricingUnits div:hidden .descLanguage[value='"+languageId+"']");
        if(previousDescription.size()){
            previousDescription.parents('.row:first').show();
            previousDescription.parents('.row:first').find(".descDeleted").val(false);
            previousDescription.parents('.row:first').find(".descContent").val('');
        }else{
            var languageDescription = $('#newPricingUnitLanguage option:selected').text();
            var clone = $('#pricingUnitClone').children().clone();
            var languagesCount = $('#pricingUnits').children().size();
            var newName = 'pricingUnit.'+languagesCount;
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
            $('#pricingUnits').append(clone);
        }
        removePricingUnitSelectedLanguage();
        updateSelectLabel($("#newPricingUnitLanguage"));
    }

    function removePricingUnitSelectedLanguage(){
        $('#newPricingUnitLanguage option:selected').remove();
        if(!$('#newPricingUnitLanguage option').size()){
            $('#addPricingUnit').hide();
        }
    }

    function removePricingUnit(elm){
        var div = $(elm).parents('.row:first');
        div.find('.descDeleted').val(true);
        div.hide();
        if($("#addPricingUnit").is(':hidden')){
            $("#addPricingUnit").show();
        }
        var langId = div.find(".descLanguage").val();
        var langValue = getLangValueForLangId(langId);
        if(langValue){
            $("#newPricingUnitLanguage").append("<option value='"+langId+"'>"+langValue+"</option>");
        }
        updateSelectLabel($('#newPricingUnitLanguage'));    
    }

    function getLangValueForLangId(langId) {
        var languages = $('#allPricingUnitLanguages').val().split(',')
        if (languages[0] != '') {
            var value = false;
            $.each(languages, function (i, lang) {
                var lang = lang.split('-');
                if (lang[0] == langId) {
                    value = lang[1];
                }
            });
            return value;
        } else {
            return false;
        }
    }

    $('#newPricingUnitLanguage').change(function(){
        updateSelectLabel($('#newPricingUnitLanguage'));
    });
</script>
