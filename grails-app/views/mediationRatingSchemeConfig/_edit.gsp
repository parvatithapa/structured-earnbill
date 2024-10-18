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

<%@ page contentType="text/html;charset=UTF-8" %>


<div class="column-hold">

    <g:set var="isNew" value="${!ratingScheme || !ratingScheme?.id || ratingScheme?.id == 0}"/>

    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="ratingScheme.add.title"/>
            </g:if>
            <g:else>
                <g:message code="ratingScheme.edit.title"/>
            </g:else>
        </strong>
    </div>

    <g:form id="save-unit-form" name="unit-form" url="[action: 'save']" >
        <g:hiddenField name="ratingSchemeId" value="${ratingScheme?.id}"/>
        <input type="hidden" name="isNew" value="${isNew}">
        <div class="box">
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">
                        <g:hiddenField name="id" value="${ratingScheme?.id}"/>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="ratingScheme.name"/></content>
                            <content tag="label.for">name</content>
                            <g:textField class="field" name="name" value="${ratingScheme?.name}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="ratingScheme.initial.increment"/></content>
                            <content tag="label.for">priceUnitName</content>
                            <g:field type="number" min="0" class="field" name="initialIncrement" value="${ratingScheme?.initialIncrement}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="ratingScheme.initial.rounding.mode"/></content>
                            <content tag="label.for">initialRoundingMode</content>
                            <content tag="include.script">true</content>
                            <g:select name="initialRoundingMode" from="['ROUND_UP', 'HALF ROUND UP', 'HALF ROUND DOWN', 'ROUND_DOWN']" keys="[BigDecimal.ROUND_UP, BigDecimal.ROUND_HALF_UP, BigDecimal.ROUND_HALF_DOWN, BigDecimal.ROUND_DOWN]" value="${ratingScheme?.initialRoundingMode}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="ratingScheme.main.increment"/></content>
                            <content tag="label.for">incrementUnitQuantity</content>
                            <g:field type="number" min="0" class="field" name="mainIncrement" value="${ratingScheme?.mainIncrement}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="ratingScheme.main.rounding.mode"/></content>
                            <content tag="label.for">mainRoundingMode</content>
                            <content tag="include.script">true</content>
                            <g:select name="mainRoundingMode" from="['ROUND_UP', 'HALF ROUND UP', 'HALF ROUND DOWN', 'ROUND_DOWN']" keys="[BigDecimal.ROUND_UP, BigDecimal.ROUND_HALF_UP, BigDecimal.ROUND_HALF_DOWN, BigDecimal.ROUND_DOWN]" value="${ratingScheme?.mainRoundingMode}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="ratingScheme.mediation.assign.global"/></content>
                            <content tag="label.for">mediation.global</content>
                            <g:checkBox id="global-checkbox"
                                        onClick="hideCompanies()"
                                        checked="${ratingScheme?.global}"
                                        class="cb checkbox" name="global"
                            />
                        </g:applyLayout>

                    </div>
                </fieldset>
            </div>
        </div>

            <g:if test="${ratingScheme?.global}">
                <g:set var="style" value="style=display:none"/>
            </g:if>
                <div class="" id="asocciation_panel" ${style}>
                    <div class="box-cards-title">
                        <span><g:message code="ratingScheme.associations.title"/></span>
                    </div>
                    <div class="box">
                        <div class="sub-box">
                            <table class="dataTable" cellspacing="0" cellpadding="0" style="width: 100%">
                                <tr>
                                    <td class="">
                                        <g:applyLayout name="form/select">
                                            <content tag="label"><g:message code="ratingScheme.assign.mediations"/>&nbsp;&nbsp;</content>
                                            <content tag="label.for">mediations</content>
                                            <content tag="label.row.class">row-left</content>
                                            <content tag="include.script">true</content>
                                            <div id="mediations" style="display: inline;">
                                                <g:select name="mediations"
                                                          from="${mediations}"
                                                          optionKey="id"
                                                          optionValue="name"
                                                          value=""
                                                          noSelection="['':'-Select Mediation']"
                                                          style="width:60%;"/>
                                            </div>
                                        </g:applyLayout>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="">
                                        <g:applyLayout name="form/select">
                                            <content tag="label"><g:message code="ratingScheme.assign.entities"/>&nbsp;&nbsp;</content>
                                            <content tag="label.for">companies</content>
                                            <content tag="label.row.class">row-left</content>
                                            <content tag="include.script">true</content>
                                            <div id="companies" style="display: inline;">
                                                <g:select name="companies"
                                                          from="${companies}"
                                                          optionKey="id"
                                                          optionValue="description"
                                                          value=""
                                                          noSelection="['':'-Select Company']"
                                                          style="width:60%;"/>
                                            </div>
                                            <content tag="icon"><a href="#" class="plus-icon" onclick="addAssociation(); return false;">&nbsp;&#xe026;</a></content>
                                        </g:applyLayout>
                                    </td>
                                    %{--<td class="short-width2 left">
                                        <a href="#" class="plus-icon" onclick="addAssociation(); return false;">&nbsp;&#xe026;</a>
                                    </td>--}%
                                </tr>
                            </table>
                        </div>

                        <div class="sub-box">
                            <table class="dataTable text-left" cellspacing="0" cellpadding="0" width="100%">
                                <thead>
                                    <tr>
                                        <th class="left"><g:message code="ratingScheme.associated.mediation"/></th>
                                        <th class="left"><g:message code="ratingScheme.associated.company"/></th>
                                        <th></th>
                                    </tr>
                                </thead>

                                <tbody id="associations">
                                    <g:render template="associations" model="[associations : ratingScheme?.associations]"  />
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
    </g:form>

    <div class="btn-box">
        <a class="submit save button-primary" onclick="$('#save-unit-form').submit();"><span><g:message code="button.save"/></span></a>
        <a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a>
    </div>

</div>

<script type="text/javascript">

    $('div[id="mediations"]').delegate("select","change",function(){
        var mediation = $('select[id="mediations"]').val();
        var ratingScheme = $('#ratingSchemeId').val();

        var company = $('select[id="companies"]').val();
        var selectedMediation = $('select[id="mediations"]').val();

        var itemIds = [];
        $('select[id="companies"]').find('option').each(function() {
            itemIds.push( $(this).val() );
        });

        var toExcludeCompanies = [];
        var id;
        $('tr[id^="association"]').each(function() {
            id = $(this).attr('id');
            mediationId = id.split('.')[1];
            if(mediationId == selectedMediation) {
                toExcludeCompanies.push( id.split('.')[2] );
            }
        });
        toExcludeCompanies.push(company)


        $.ajax({
            url: '${createLink(controller: 'mediationRatingSchemeConfig', action: 'retrieveAvailableCompanies')}',
            data: {mediation: mediation, ratingScheme: ratingScheme, toExcludeCompanies:toExcludeCompanies, company: company},
            cache: false,
            success: function(html) {
                $('div[id="companies"]').html(html);
                var select = $('select[id="companies"]');
                select.attr('style','width:50%');
                updateSelectHandlers();
            }
        });
    });

    function updateSelectHandlers() {
        var select = $('select[id="companies"]');

        if(select.size() > 0) {
            select.change(function () {
                updateSelectLabel(this);
            });
            select.each(function () {
                updateSelectLabel(this);
            });
            $('div[id="companies"]').parent('div').show();
        } else {
            $('div[id="companies"]').parent('div').hide();
        }
    }

    function addAssociation(){
        var mediation = $('select[id="mediations"]').val();
        var company = $('select[id="companies"]').val();
        var selectedMediation = $('select[id="mediations"]').val();

        if(!mediation || !company) {
            return;
        }

        var itemIds = [];
        $('select[id="companies"]').find('option').each(function() {
            itemIds.push( $(this).val() );
        });

        var toExcludeCompanies = [];
        var id;
        $('tr[id^="association"]').each(function() {
            id = $(this).attr('id');
            mediationId = id.split('.')[1];
            if(mediationId == selectedMediation) {
                toExcludeCompanies.push( id.split('.')[2] );
            }
        });
        toExcludeCompanies.push(company)

        callGetDependencyList(toExcludeCompanies, mediation);
        callAddDependencyRow(mediation, company);
    }

    function removeDependency(trId){
        $('tr[id="'+trId+'"]').remove();

        var mediation = $('select[id="mediations"]').val();
        var selectedMediation = $('select[id="mediations"]').val();

        var itemIds = [];
        $('select[id="companies"]').find('option').each(function() {
            itemIds.push( $(this).val() );
        });

        var toExcludeCompanies = [];
        var id;
        $('tr[id^="association"]').each(function() {
            id = $(this).attr('id');
            mediationId = id.split('.')[1];
            if(mediationId == selectedMediation) {
                toExcludeCompanies.push( id.split('.')[2] );
            }
        });

        callGetDependencyList(toExcludeCompanies, mediation);
    }

    function callGetDependencyList(toExcludeCompanies, mediation){
        var ratingScheme = $('#ratingSchemeId').val();
        $.ajax({
            url: '${createLink(controller: 'mediationRatingSchemeConfig', action: 'retrieveAvailableCompanies')}',
            data: {toExcludeCompanies: toExcludeCompanies, mediation: mediation, ratingScheme: ratingScheme},
            cache: false,
            success: function(html) {
                $('div[id="companies"]').html(html);
                $('select[id="companies"]').attr('style','width:50%');
                updateSelectHandlers();
            }
        });
    }

    function callAddDependencyRow(mediation, company){
        var ratingScheme = $('#ratingSchemeId').val();
        $.ajax({
            url: '${createLink(controller: 'mediationRatingSchemeConfig', action: 'addRatingSchemeAssociation')}',
            data: {mediation: mediation, company: company, ratingScheme: ratingScheme},
            cache: false,
            success: function(html) {
                if(mediation!="" && company==""){
                    $('tbody[id="associations"]').append(html);
                } else if(mediation!="" && company!=""){
                    $('tbody[id="associations"]').append(html);
                }
            }
        });
    }

    function hideCompanies() {
        $('#asocciation_panel').toggle()
    }

</script>
