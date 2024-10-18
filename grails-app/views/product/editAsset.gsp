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

<%@ page import="com.sapienter.jbilling.server.item.db.ItemTypeDTO;" %>
<%@ page import="com.sapienter.jbilling.server.metafields.MetaFieldBL;" %>
<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO;" %>

<%--
  Form for editing an asset

 @author Gerhard Maree
 @since  18-Apr-2013
--%>

<html>
<head>
    <meta name="layout" content="main"/>
</head>

<body>
<div id="new-asset-content" class="form-edit">
    <g:render template = "editAssetContent"
                 model = "[            asset: asset,
                                    statuses: statuses,
                           categoryAssetMgmt: categoryAssetMgmt,
                                     isGroup: isGroup,
                                   companies: companies]"/>
</div>
<g:link elementId="listAssets" action="assets" id="${asset.item.id}" />

<script type="text/javascript">
    function checkAssetSaveResponse(event) {
        if($('#new-asset-content > asset').length) {
            $('#listAssets')[0].click();
	   	}
		}

    function cancelCreateAsset() {
        $('#listAssets')[0].click();
    }

</script>
