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

<%@ page import="com.sapienter.jbilling.server.item.db.AssetDTO" %>

<%--
  Template lets the user select a file with assets for upload

  @author Gerhard Maree
  @since  14-May-2013
--%>


<div class="column-hold">
    <div class="heading">
        <strong>
            <g:message code="asset.heading.upload"/>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <p><g:message code="asset.upload.format"/></p><br/>

            <p><g:message code="asset.upload.label.columns"/>
            <strong>'${product.findItemTypeWithAssetManagement().assetIdentifierLabel ?: message([code:  'asset.detail.identifier'])}' (*), '<g:message code="asset.detail.notes"/>'
            <g:each in="${category.assetMetaFields.sort {it.displayOrder}}" var="metaField">
                , ${"'"+metaField.name + (metaField.mandatory ? "' (*)" : "'") }
            </g:each>
            </strong></p><br/>

            <p><g:message code="asset.upload.groups" /> </p><br/>

            <p><g:message code="asset.upload.defaults" args="${[category.description,defaultStatus.description]}" /> </p><br/>

            <g:uploadForm name="upload-assets-form" url="[action: 'uploadAssets']">
                <g:hiddenField name="prodId" value="${product.id}"/>
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="asset.label.csv.file"/></content>
                    <input type="file" name="assetFile"/>
                </g:applyLayout>
                <div class="btn-row">
                    <br/>
                    <a onclick="$('#upload-assets-form').submit();" class="submit save"><span><g:message
                            code="button.upload"/></span></a>
                </div>
            </g:uploadForm>

        </div>
    </div>

</div>

