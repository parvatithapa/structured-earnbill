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

<%--
  Template lets the user select a file with products for upload

  @author Taimoor Choudhary
  @since  11-Dec-2017
--%>


<div class="column-hold">
    <div class="heading">
        <strong>
            <g:message code="product.heading.upload"/>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <p><g:message code="product.upload.format"/></p><br/>

            <g:if test="${params.id=='1'}">
                <p><g:message code="price.upload.template.file.message"/>
                    <g:link action="downloadTemplateFile" params="['actionTaken': 'default']">Download Template</g:link>
                </p><br/>

                <g:uploadForm name="upload-products-form" url="[action: 'uploadDefaultProductPrices']">
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="product.label.csv.file"/></content>
                        <input type="file" name="productFile"/>
                    </g:applyLayout>
                    <div class="btn-row">
                        <br/>
                        <a onclick="$('#upload-products-form').submit();" class="submit add"><span><g:message
                                code="button.upload"/></span></a>
                    </div>
                </g:uploadForm>
            </g:if>
            <g:if test="${params.id=='2'}">
                <p><g:message code="price.upload.template.file.message"/>
                    <g:link action="downloadTemplateFile" params="['actionTaken': 'accounttype']">Download Template</g:link>
                </p><br/>

                <g:uploadForm name="upload-products-form" url="[action: 'uploadAccountPrices']">
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="accountPrices.label.csv.file"/></content>
                        <input type="file" name="accountPricesFile"/>
                    </g:applyLayout>
                    <div class="btn-row">
                        <br/>
                        <a onclick="$('#upload-products-form').submit();" class="submit add"><span><g:message
                                code="button.upload"/></span></a>
                    </div>
                </g:uploadForm>
            </g:if>
            <g:if test="${params.id=='3'}">
                <p><g:message code="price.upload.template.file.message"/>
                    <g:link action="downloadTemplateFile" params="['actionTaken': 'customer']">Download Template</g:link>
                </p><br/>

                <g:uploadForm name="upload-products-form" url="[action: 'uploadCustomerPrices']">
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="customerPrices.label.csv.file"/></content>
                        <input type="file" name="customerPricesFile"/>
                    </g:applyLayout>
                    <div class="btn-row">
                        <br/>
                        <a onclick="$('#upload-products-form').submit();" class="submit add"><span><g:message
                                code="button.upload"/></span></a>
                    </div>
                </g:uploadForm>
            </g:if>
            <g:if test="${params.id=='4'}">
                <p><g:message code="price.upload.template.file.message"/>
                <g:link action="downloadTemplateFile" params="['actionTaken': 'plan']">Download Template</g:link>
                </p><br/>

                <g:uploadForm name="upload-products-form" url="[action: 'uploadPlanPrices']">
                    <g:applyLayout name="form/text">
                        <content tag="label"><g:message code="planPrices.label.csv.file"/></content>
                        <input type="file" name="planPricesFile"/>
                    </g:applyLayout>
                    <div class="btn-row">
                        <br/>
                        <a onclick="$('#upload-products-form').submit();" class="submit add"><span><g:message
                                code="button.upload"/></span></a>
                    </div>
                </g:uploadForm>
            </g:if>
        </div>
    </div>

</div>

