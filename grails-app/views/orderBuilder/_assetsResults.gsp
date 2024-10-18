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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.item.db.AssetStatusDTO;  com.sapienter.jbilling.server.item.db.AssetDTO" %>

<%--
  Shows the asset list and provides some basic filtering capabilities.

  @author Gerhard Maree
  @since 24-April-2011
--%>
<g:if test="${errorMessages}">
    <div class="msg-box error">
        <ul>
            <g:each var="message" in="${errorMessages}">
                <li>${message.decodeHTML()}</li>
            </g:each>
        </ul>
    </div>
</g:if>

<g:formRemote name="add-assets-form-${assetFlow}" onSuccess="updateWithFilters();"
              url="[action: 'edit']" method="GET" >
    <input type="hidden" name="_eventId" value="addAssets">
    <input type="hidden" name="partial" value="true">

<%-- asset list --%>
<div class="table-box tab-table" style="margin-top: 10px">
    <div class="table-scroll">
        <table id="assets" cellspacing="0" cellpadding="0">
            <thead>
            <tr>
                <th class="tiny narrow spacing-narrow"><input type="checkbox" id="check-all-assets"/></th>
                <th class="narrow"><g:message code="asset.detail.identifier" /></th>
                <th class="medium narrow"><g:message code="asset.detail.assetStatus" /></th>
                <th class="medium narrow"><g:message code="asset.detail.createDatetime" /></th>
            </tr>
            </thead>
            <tbody>
            <g:each var="asset" in="${assets}" status="idx">
                <tr class="${asset.isReserved()?'reserved':''}">
                    <td class="tiny narrow ${asset.isReserved()?'reserved':''}">
                        <input type="checkbox" name="asset.select.${idx}" value="${asset.id}" onchange="updateSelectedAssets('asset.select.${idx}','${params.checkedAssets}')"/>
                        <input type="hidden" name="asset.${idx}" value="${asset.id}" />
                    </td>
                    <td class="narrow ${asset.isReserved()?'reserved':''}">
                        <g:remoteLink class="cell double" action="edit" id="${asset.id}" params="[_eventId: 'addAsset']"
                                      after="updateWithFilters()" method="GET">
                            <strong>${StringEscapeUtils.escapeHtml(asset?.identifier)}</strong> (<g:message code="table.id.format" args="[asset.id as String]"/>)
                        </g:remoteLink>
                    </td>
                    <td class="medium narrow ${asset.isReserved()?'reserved':''}">
                        <g:remoteLink class="cell double" action="edit" id="${asset.id}" params="[_eventId: 'addAsset']"
                                      after="updateWithFilters()" method="GET">
                            <span>${asset.isReserved() ? g.message(code:"asset.reserved.status") : StringEscapeUtils.escapeHtml(asset?.assetStatus?.description)}</span>
                        </g:remoteLink>
                    </td>
                    <td class="medium narrow ${asset.isReserved()?'reserved':''}">
                        <g:remoteLink class="cell double" action="edit" id="${asset.id}"
                                      params="[_eventId: 'addAsset']" after="updateWithFilters()" method="GET">
                            <span>${formatDate(date: asset.createDatetime, formatName: 'date.pretty.format')}</span>
                        </g:remoteLink>
                    </td>
                </tr>
            </g:each>

            </tbody>
        </table>
    </div>
</div>
</g:formRemote>

<div class="pager-box ui-tabs-panel">
    <div class="results">
        <div class="select-holder select-holder_small"><span class="select-value"></span>
            <g:set var="steps" value="${[10,20,50]}" />
            <g:select id="page-size-assets" name="page-size-assets" from="${steps}" value="${maxAssetsShown}" onchange="pageSizeChange_assetSearch(this);" optionValue="${{it + " " + message(code:"pager.show.max.results")}}" />
        </div>
    </div>

    <div class="row-center">
        <jB:remotePaginate action="edit"
                             params="${sortableParams(params: [partial: true, _eventId: 'assets', max: maxAssetsShown, statusId: params.statusId ?: "", filterBy: params.filterBy ?: ""])}"
                             total="${assets.totalCount ?: 0}"
                             update="assets-table-${assetFlow}"
                             method="GET"/>
    </div>
</div>

<div class="btn-box row">
    <a class="submit add" onclick="$('#add-assets-form-${assetFlow}').submit();">
        <span><g:message code="button.add.checked"/></span>
    </a>
    <g:remoteLink class="submit delete" action="edit" after="updateWithFilters(); clearAssets();"
                  params="[_eventId: 'clearAssets']" method="GET">
        <span><g:message code="button.clear.assets"/></span>
    </g:remoteLink>
    <sec:ifAllGranted roles="PRODUCT_CATEGORY_STATUS_AND_ASSETS_131">
        <g:remoteLink class="submit add" controller="product" action="editAsset"
                      params="[partial: 'true', add: 'true', userCompanyMandatory: 'true', prodId: productWithAsset]" update="new-asset-content" after="openNewAssetDialog(event);" method="GET">
            <span><g:message code="button.new.asset"/></span>
        </g:remoteLink>
    </sec:ifAllGranted>
</div>

<%-- Shows the list of selected assets. Users are able to remove assets from the list. --%>
<div class="form-columns single no-padding">
    <div id="selected-assets-${assetFlow}" class="row cloud">
        <ul class="cloud">
            <li class="invert">
                <div>
                    <strong><g:message code="assets.label.selected"/></strong>
                </div>
            </li>
            <g:each var="asset" in="${selectedAssets}">
                <li>
                    <strong>${asset.identifier}</strong>

                    <g:remoteLink class="cell double" action="edit" id="${asset.id}" after="updateWithFilters();"
                                  params="[_eventId: 'removeAsset']" method="GET">
                        <span>&#x00D7;</span>
                    </g:remoteLink>
                </li>
            </g:each>
        </ul>
    </div>
</div>

<script type="text/javascript">
    $('#check-all-assets').change(function() {
        $('#assets :checkbox').prop('checked', $(this).prop('checked'));
        $('#assets :checkbox').each(function() {
            updateSelectedAssets($(this).attr('name'),'${params.checkedAssets}');
        });
    });

    function updateWithFilters(){
        $('#assets-filter-form-${assetFlow}').submit();
    }

    function pageSizeChange_assetSearch(obj) {
        $('input[name="max"]').val(obj.value);
        updateWithFilters();
    }

    function moveResults_assets () {
        var col = '#column2';
        if($('#page-size-tmp-${action?:'actn'}').closest('#column1').size() > 0) {
            col = '#column1';
        } else if(${params.update ? 'true' : 'false'}  && $('#${params.update}').size() > 0) {
            col = '#${params.update}';
        }
        var colEl = $(col);
        console.log('Moving to '+col);
        var newEl = $('#page-size-tmp-${action?:'actn'}').children();
        console.log('New '+newEl);
        newEl.detach();
        colEl.empty();
        colEl.append(newEl);

        if(col == '#column1') {
            $('#column2').html('');
        }
    }

    $(document).ready(function() {
        $("select[name='page-size-assets']").each(function () {
            updateSelectLabel(this);
        });

        $("select[name='page-size-assets']").change(function () {
            updateSelectLabel(this);
        });
    });
</script>
