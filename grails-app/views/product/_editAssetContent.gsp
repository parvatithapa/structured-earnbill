%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%
<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.ItemTypeDTO;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldBL;" %>

<%--
  Form for editing an asset

 @author Gerhard Maree
 @since  18-Apr-2013
--%>

    <g:set var="isNew" value="${!asset || !asset?.id || asset?.id == 0}"/>
    <g:render template="/layouts/includes/messages"/>
    <div class="heading">
        <strong>
            <g:if test="${isNew}">
                <g:message code="product.asset.add.title"/>
            </g:if>
            <g:else>
                <g:message code="product.asset.edit.title"/>
            </g:else>
        </strong>
    </div>

    <div class="form-hold">
        <g:formRemote name="save-asset-form" url="[action:'saveAsset']" update="new-asset-content"  onSuccess="checkAssetSaveResponse(event);">
            <fieldset>
                <div class="form-columns">

                    <%-- Base asset details --%>
                    <div class="column">
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="asset.detail.id"/></content>

                            <g:if test="${isNew}"><em><g:message code="prompt.id.new"/></em></g:if>
                            <g:else>${asset?.id}</g:else>

                            <g:hiddenField name="id" value="${asset?.id}"/>
                            <g:hiddenField name="itemId" value="${asset.item.id}"/>
                            <g:hiddenField name="categoryId" value="${categoryAssetMgmt.id}"/>
                            <g:hiddenField name="partial" value="${partial}"/>
                            <g:hiddenField name="userCompanyMandatory" value="${userCompanyMandatory}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/input">
                            <content tag="label">
                                ${categoryAssetMgmt.assetIdentifierLabel ?: g.message([code: "asset.detail.identifier"])}
                                <span id="mandatory-meta-field">*</span>
                            </content>
                            <content tag="label.for">identifier</content>
                            <g:textField class="field" name="identifier" value="${asset?.identifier}"/>
                        </g:applyLayout>

                        <g:if test="${asset.assetStatus?.isInternal == 1 || asset.assetStatus?.isOrderSaved == 1}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="asset.detail.assetStatus"/></content>
                                <g:hiddenField name="assetStatusId" value="${asset.assetStatus.id}"/>
                                ${asset.assetStatus?.description}
                            </g:applyLayout>
                        </g:if>
                        <g:else>
                            <g:applyLayout name="form/select">
                                <content tag="label"><g:message code="asset.detail.assetStatus"/></content>
                                <content tag="include.script">true</content>
                                <content tag="label.for">assetStatusId</content>
                                <g:select        name = "assetStatusId"
                                                 from = "${statuses}"
                                            optionKey = "id"
                                          optionValue = "description"
                                                value = "${asset.assetStatus?.id}"/>
                            </g:applyLayout>
                        </g:else>

                        <g:applyLayout name="form/checkbox">
                            <content tag="label"><g:message code="asset.detail.isGroup"/></content>
                            <content tag="label.for">isGroup</content>
                            <g:checkBox       id = "asset-isGroup"
                                            name = "isGroup"
                                         checked = "${asset?.containedAssets || isGroup}"
                                           class = "cb checkbox field"
                                        onchange = "showAssetGroup()"/>
                        </g:applyLayout>
							<g:isGlobal>
                                <g:if test="${asset.item.isGlobal()}">
                                    <g:applyLayout name="form/checkbox">
                                        <content tag="label"><g:message code="product.assign.global"/></content>
                                        <content tag="label.for">asset?.global</content>
                                        <g:checkBox      id = "global-checkbox"
                                                    onClick = "hideCompanies()"
                                                      class = "cb checkbox"
                                                       name = "global"
                                                    checked = "${asset?.global}"/>
                                    </g:applyLayout>
                                </g:if>
	                        	</g:isGlobal>
	                        	<g:isNotRoot>
	                        		<g:hiddenField name="global" value="${asset?.global}"/>
	                        	</g:isNotRoot>
	                        
	                        	<div id="childCompanies">          
	                       		<g:isRoot>
	                       			<g:applyLayout name="form/select_multiple">
	                           			<content tag="label">
                                            <g:message code="product.assign.entities"/>
                                        </content>
	                           			<content tag="label.for">asset.entities</content>
	                           			<g:select          id = "company-select"
                                                     multiple = "multiple"
                                                         name = "entities"
                                                         from = "${companies}"
	                                   			    optionKey = "id"
                                                  optionValue = "${{it?.description}}"
	                           	    	    	        value = "${companies*.id.size == 1 ? companies?.id : asset.entities?.id}"
	                           	    	    	     onChange = "${remoteFunction(action: 'retrieveMetaFields',
	                  										                      update: 'product-metafields',
	                  										                      params: '\'entities=\' + getSelectValues(this)')}"/>
	                        		</g:applyLayout>
	                        	</g:isRoot>
	                        	<g:isNotRoot>     
	                        		<g:if test="${asset?.entities?.size()>0}">                   		
		                        		<g:each in="${asset?.entities}">
		                        			<g:hiddenField name="entities" value="${it}"/>
		                        		</g:each>
	                        		</g:if>
	                        		<g:else>
	                        				<g:hiddenField name="entities" value="${session['company_id']}"/>
									</g:else>
	                        	</g:isNotRoot>
	                        </div>
                        <g:if test="${!isNew}">
                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="asset.detail.createDatetime"/></content>
                                <g:formatDate format="dd-MM-yyyy HH:mm" date="${asset.createDatetime}" timeZone="${session['company_timezone']}"/>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label"><g:message code="asset.detail.order"/></content>
                                ${asset.orderLine?.purchaseOrder?.id}
                            </g:applyLayout>
                        </g:if>

                        <g:render template="/metaFields/editMetaFields" model="[ availableFields: categoryAssetMgmt.assetMetaFields,
                                                                                     fieldValues: MetaFieldBL.convertMetaFieldsToWS(categoryAssetMgmt.assetMetaFields, asset) ]"/>

                    </div>

                    <div class="column">
                        <g:applyLayout name="form/textarea">
                            <content tag="label"><g:message code="asset.detail.notes"/></content>
                            <content tag="label.for">notes</content>
                            <g:textArea class="narrow" name="notes" value="${asset.notes}" rows="5" cols="45"/>
                        </g:applyLayout>

                    </div>

                </div>

                <div>
                    <br/>&nbsp;
                </div>

                <%-- Assigning assets to groups --%>
                <div id="group-assets-view" class="box-cards box-cards-open">
                    <div class="box-cards-title">
                        <a class="btn-open" href="#"><span><g:message code="asset.group.member.assets"/></span></a>
                    </div>
                    <div class="box-card-hold">

                        <div class="form-columns">

                        <%-- Asset search --%>
                            <div class="column wide2">
                                <div class="heading"><strong><g:message code="asset.heading.filter"/></strong></div>
                                <div class="box narrow">
                                    <div id="group-search-holder" class="sub-box-no-pad asset-group-filters">
                                        <g:if test="${!isNew}">
                                            <input type="hidden" class="group-search-filter" name="searchAssetId" value="${asset.id}" />
                                            <input type="hidden" class="group-search-filter" name="searchIncludedAssetId" value="${asset.containedAssets.collect{it.id}.join(',')}" />
                                        </g:if>
                                        <input type="hidden" class="group-search-filter" name="searchExcludedAssetId" id="searchExcludedAssetId" value="" />
                                        <g:hiddenField name="max" class="group-search-filter" value="${params.max}"/>
                                        <g:hiddenField name="offset" class="group-search-filter" value="${params.offset}"/>
                                        <g:applyLayout name="form/input">
                                            <content tag="label"><g:message code="filters.title"/></content>
                                            <content tag="label.for">filterBy</content>
                                            <g:textField        name = "filterBy" class="field default group-search-filter"
                                                         placeholder = "${message(code: 'assets.filter.by.default')}"
                                                               value = "${params.filterBy}"/>
                                        </g:applyLayout>

                                        <g:applyLayout name="form/select">
                                            <content tag="label"><g:message code="asset.label.category"/></content>
                                            <content tag="label.for">searchCategoryId</content>
                                            <content tag="include.script">true</content>
                                            <g:select        name = "searchCategoryId"
                                                             from = "${availableCategories}"
                                                            class = "group-search-filter"
                                                      noSelection = "['': message(code: 'filters.asset.status.empty')]"
                                                        optionKey = "id"
                                                      optionValue = "description"
                                                         onchange = "loadCategoryFilters(this);"
                                                            value = "${params.categoryId}"/>
                                        </g:applyLayout>

                                        <div id="category-filters">
                                            <g:render template="groupSearchFilter" model="[products: [], assetStatuses: [], metaFields: []]" />
                                        </div>
                                    </div>

                                    <%-- Search results --%>
                                </div>
                                <div id="asset-search-results"></div>
                            </div>

                            <%-- Chosen assets --%>
                            <div class="column">
                                <div id="" class="">
                                    <div class="heading">
                                        <span><g:message code="asset.group.chosen.assets"/></span>
                                    </div>

                                    <div class="box">

                                        <div class="table-box">
                                            <table cellspacing="0" cellpadding="0">
                                                <tbody id="group-selected-assets">
                                                <g:each in="${asset.containedAssets}" var="containedAsset" >
                                                    <tr id="group-asset-${containedAsset?.id}" class="${selected?.id == asset.id ? 'active' : ''}">
                                                        <td class="narrow" >
                                                            <em class="narrow">${containedAsset?.identifier}</em>
                                                        </td>
                                                        <td class="narrow">
                                                            <span class="narrow"><g:formatDate format="dd-MM-yyyy HH:mm" date="${containedAsset?.createDatetime}" timeZone="${session['company_timezone']}"/></span>
                                                        </td>
                                                        <td class="narrow">
                                                            <span class="narrow">${containedAsset?.assetStatus?.description}</span>
                                                        </td>
                                                        <td class="tiny narrow">
                                                            <a onclick="removeAssetFromGroup(this);"><span class="narrow"><img src="${resource(dir: 'images', file: 'cross.png')}" /></span></a>
                                                        </td>
                                                    </tr>
                                                </g:each>
                                                </tbody>
                                            </table>
                                        </div>

                                    </div>

                                </div>
                            </div>
                            <input id="containedAssetIds" type="hidden" name="containedAssetIds" />
                        </div>
                    </div>
                </div>
                <div class="buttons">
                    <ul>
                        <li>
                            <g:remoteLink name="pasteButton" action="editAsset" class="submit edit"  update="new-asset-content"
                                          params="[categoryId: categoryAssetMgmt.id, 
                                                       prodId: asset.item.id,
                                                      partial: partial,
                                                        paste: true,
                                                          add: true]">
                                <span><g:message code="button.paste"/></span>
                            </g:remoteLink>
                        </li>
                        <li>
                            <a onclick="$('#save-asset-form').submit();" class="submit save"><span><g:message
                                    code="button.save"/></span></a></li>
                        <li>
                            <g:settingEnabled property="hbase.audit.logging">
                                <g:if test="${!isNew}">
                                    <sec:access url="/product/assetHistory">
                                        <g:link controller="product" action="assetHistory" id="${asset?.id}" class="submit show">
                                            <span><g:message code="button.view.history"/></span>
                                        </g:link>
                                    </sec:access>
                                </g:if>
                            </g:settingEnabled>
                        </li>
                        <li>
                            <a onclick="cancelCreateAsset();" class="submit cancel">
                                <span><g:message code="button.cancel"/></span>
                            </a>
                        </li>
                    </ul>
                </div>
            </fieldset>
        </g:formRemote>
    </div>

<div style="display: none;" >
    <g:formRemote name="loadFilters" url="[action:'loadAssetGroupFilters']" update="category-filters" method="GET">
        <g:hiddenField id="loadFilters-categoryId" name="categoryId" />
    </g:formRemote>

    <g:formRemote name="group-asset-search" url="[action: 'groupAssetSearch']" update="asset-search-results">
    </g:formRemote>
</div>

<script type="text/javascript">

	$(document).ready(function() {
		if ($("#global-checkbox").is(":checked")) {
	    	$("#company-select").attr('disabled', true);
	   	}

        <g:if test="${!session['Copied Asset'] || asset.id}">
            $("a[name='pasteButton']").attr("href", "#");
            $("a[name='pasteButton']").attr("onclick", "");
            $("a[name='pasteButton']").addClass("link_disabled");
        </g:if>

        if ($('#flash-errormsg > ul').length > 0) {
            registerSlideEvents();
        }
	});
	
	function hideCompanies() {
		if ($("#global-checkbox").is(":checked")) {
			$("#company-select").attr('disabled', true);
		} else {
			$("#company-select").removeAttr('disabled');
		}
	}

    <%-- Load the statuses, products and meta fields for the chosen category--%>
    function loadCategoryFilters(obj) {
        $('#loadFilters-categoryId').val($(obj).val());
        $('#loadFilters').submit();
    }

    <%-- Do asset search --%>
    function searchAssetsForGroup() {
        $('#group-asset-search').empty();
        $(".group-search-filter").each(function(idx) {
            var clone = $(this).clone();
            $("#group-asset-search").append(clone);
            clone.val($(this).val())
        });
        $(".mf-input").each(function(idx) {
            var clone = $(this).clone();
            $("#group-asset-search").append(clone);
            clone.val($(this).val())
        });
        $('#group-asset-search').submit();
    }

    <%-- Add the selected asset from the search results to the list of chosen assets--%>
    function addAssetToSelected(obj) {
        if ($(obj).closest('tbody').attr('id') == 'group-selected-assets')
            return false;

        var row = $(obj).closest("tr").remove();
        row.off('click');

        <%-- Check if the row is not already in the list of selected assets --%>
        if($('#group-selected-assets #'+row.attr('id')).length == 0) {
            $('#group-selected-assets').append(row);
            if (row.find('td.removeAsset').length == 0) {
                row.append('<td class="tiny narrow removeAsset"><a onclick="removeAssetFromGroup(this);"><span class="narrow"><img src="${resource(dir: 'images', file: 'cross.png')}" /></span></a></td>');
            }
        }

        updateContainedAssetIds();
    }

    <%-- Update the input which contains the list of group asset ids--%>
    function updateContainedAssetIds() {
        var assetIds = '';
        $('#group-selected-assets tr').each(function(idx) {
            if(idx > 0) assetIds += ',';
            var id = $(this).attr('id');
            assetIds += id.substring(id.lastIndexOf('-')+1);
        });
        $('#containedAssetIds').val(assetIds);
        $('#searchExcludedAssetId').val(assetIds);
    }

    function removeAssetFromGroup(obj) {
        var clone = $(obj).closest("tr").remove();
        clone.find("td")[3].remove();
        clone.removeClass('active');
        $("table#users tbody").append(clone);
        updateContainedAssetIds();
    }

    <%-- Display the asset group portion of the screen --%>
    function showAssetGroup() {
        var allow = $("#asset-isGroup").prop("checked");

        $("#group-assets-view").css("display", (allow?"block":"none"));
        $('#group-asset-search').submit();
    }

    <%-- event listeners to reload results --%>
    $('#group-search-holder :input[name=filterBy]').blur(function () {
        searchAssetsForGroup();
    });

    var assetSearchFunction = searchAssetsForGroup;

    updateContainedAssetIds();
    showAssetGroup();
</script>
