<%@ page import="com.sapienter.jbilling.server.util.db.CurrencyDTO; com.sapienter.jbilling.server.util.db.LanguageDTO;" %>

<script type="text/javascript">
    function addNewDescription(){
        var languageId = $('#newDescriptionLanguage').val();
        var previousDescription = $("#description div:hidden .descLanguage[value='"+languageId+"']");
        if(previousDescription.size()){
            previousDescription.parents('.row:first').show();
            previousDescription.parents('.row:first').find(".descDeleted").val(false);
            previousDescription.parents('.row:first').find(".descContent").val('');
        }else{
            var languageDescription = $('#newDescriptionLanguage option:selected').text();
            var clone = $('#descriptionClone').children().clone();
            var languagesCount = $('#description').children().size();
            var newName = 'notification.description['+languagesCount+']';
            clone.find("label").attr('for', newName+'.content');
            var label = clone.find('label').html();
            clone.find('label').html(label.replace('{0}', languageDescription));
            if(languageDescription=="English"){
                clone.find('label').append("<span id='mandatory-meta-field'>*</span>");
            }

            clone.find(".descContent").attr('id',newName+'.content');
            clone.find(".descContent").attr('name',newName+'.content');

            clone.find(".descLanguage").attr('id',newName+'.languageId');
            clone.find(".descLanguage").attr('name',newName+'.languageId');
            clone.find(".descLanguage").val(languageId);

            clone.find(".descDeleted").attr('id',newName+'.deleted');
            clone.find(".descDeleted").attr('name',newName+'.deleted');

            $('#description').append(clone);
        }
        if(languageId==1){
            $('#newDescriptionLanguage').closest("div").find("label span").remove();
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
            if(langId==1){
                $("#newDescriptionLanguage").closest("div").find('label').append("<span id='mandatory-meta-field'>*</span>");
            }
        }
        updateSelectLabel($("#newDescriptionLanguage"));
    }

    function loadAvailableDecLang(){
        var languages = $('#availableDescriptionLanguages').val().split(',');
        if(languages[0]!=''){
            $.each(languages,function(i,lang){
                var lang = lang.split('-');
                $("#newDescriptionLanguage").append("<option value='"+lang[0]+"'>"+lang[1]+"</option>");
                if(lang[0]==1){
                    $("#newDescriptionLanguage").closest("div").find('label').append("<span id='mandatory-meta-field'>*</span>");
                }
            });
        }else{
            $('#addDescription').hide();
        }
    }

    function getValueForLangId(langId){
        var languages = $('#allDescriptionLanguages').val().split(',')
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


    function getSelectValues(select) {
        var result = [];
        var options = select && select.options;
        var opt;

        for (var i=0, iLen=options.length; i!=iLen; i++) {
            opt = options[i];

            if (opt.selected) {
                result.push(opt.value || opt.text);
                result.push(",")
            }
        }
        return result;
    }

    $(document).ready(function() {
        loadAvailableDecLang();
    });
</script>

<div class="row" id='addDescription'>
    <div class="add-desc">
        <label><g:message code='notification.detail.description.add.title'/></label>
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
        <content tag="label"><g:message code="notification.detail.description.label"/></content>
        <content tag="label.for">desCloneContent</content>

        <input type="text" id="desCloneContent" class="descContent field" size="26" value="" name="desCloneContent">
        <input type="hidden" id="desCloneLangId" class="descLanguage" value="" name="desCloneLangId">
        <input type="hidden" id="desCloneDeleted" class="descDeleted" value="" name="desCloneDeleted">
        <content tag="icon"><a class="plus-icon delete" onclick="removeDescription(this)">&#xe000;</a></content>
    </g:applyLayout>
</div>

<g:set var="availableDescriptionLanguages" value="${LanguageDTO.list().collect {it.id+'-'+it.description}.sort{a,b-> a.compareTo(b)}}"></g:set>


<div id="description">
    <g:each in="${notificationMessageType?.description}" var="description" status="index">
        <g:if test="${description?.languageId}">
            <g:applyLayout name="form/description">
                <g:set var="currentLang" value="${LanguageDTO.get(notificationMessageType?.description[index]?.languageId)}"></g:set>
                <g:set var="availableDescriptionLanguages" value="${availableDescriptionLanguages - (currentLang?.id+'-'+currentLang?.description)}"></g:set>
                <content tag="label"><g:message code="notification.detail.description.label" args="${[currentLang?.description]}"/>
                    <g:if test="${description?.languageId==1}">
                        <span id="mandatory-meta-field">*</span>
                    </g:if>
                </content>

                <content tag="label.for">notificationMessageType?.description[${index}]?.content</content>

                <g:textField name="notification.description[${index}].content" class="descContent field" value="${notificationMessageType?.description[index]?.content}"/>
                <g:hiddenField name="notification.description[${index}].languageId" class="descLanguage" value="${currentLang?.id}"/>
                <g:hiddenField name="notification.description[${index}].deleted" value="" class="descDeleted"/>
                <content tag="icon"><a class="plus-icon delete" onclick="removeDescription(this)">&#xe000;</a></content>
            </g:applyLayout>
        </g:if>
    </g:each>
</div>

<g:set var="allDescriptionLanguages" value="${LanguageDTO.list().collect {it.id+'-'+it.description}}"></g:set>
<g:hiddenField name="allDescriptionLanguages" value="${allDescriptionLanguages?.join(',')}"/>
<g:hiddenField name="availableDescriptionLanguages" value="${availableDescriptionLanguages?.join(',')}"/>

