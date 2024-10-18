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
  @since  19-Jun-2018
--%>
<script type="text/javascript">
    jQuery(function(){
        $("a.pager-button").on("click", function(e) {
            window.location.href = $(this).attr("href") + "?id=" + $("#code").val();
            return false;
        });
    });
</script>
<div class="column-hold">
    <div class="heading">
        <strong>
            <g:message code="product.heading.individual.download"/>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <p><g:message code="product.detail.individual.download"/></p><br/>

            <g:if test="${params.id=='1'}">

                    <div class="btn-row">
                        <g:textField id= "code" name="code" placeholder="${message(code:'product.download.default.price')}"
                                     class="field text-field" />

                        <g:link action="downloadPricesFile" params="['actionTaken': 'defaultindividual']"
                                class="pager-button">Download Price</g:link>
                    </div>

            </g:if>
            <g:if test="${params.id=='2'}">
                <div class="btn-row">
                    <g:textField id= "code" name="code" placeholder="${message(code:'product.download.account.price')}"
                                 class="field text-field" />

                    <g:link action="downloadPricesFile" params="['actionTaken': 'accounttypeindividual']"
                            class="pager-button">Download Price</g:link>
                </div>
            </g:if>
            <g:if test="${params.id=='3'}">
                <div class="btn-row">
                    <g:textField id= "code" name="code" placeholder="${message(code:'product.download.customer.price')}"
                                                  class="field text-field" />

                <g:link action="downloadPricesFile" params="['actionTaken': 'customerindividual']"
                            class="pager-button">Download Price</g:link>
                </div>
            </g:if>
            <g:if test="${params.id=='4'}">
                <div class="btn-row">
                    <g:textField id= "code" name="code" placeholder="${message(code:'product.download.plan.price')}"
                                                  class="field text-field" />

                <g:link action="downloadPricesFile" params="['actionTaken': 'planindividual']"
                            class="pager-button">Download Price</g:link>
                </div>
            </g:if>
        </div>
    </div>

</div>

<div class="column-hold">
    <div class="heading">
        <strong>
            <g:message code="product.heading.bulk.download"/>
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <p><g:message code="product.detail.bulk.download"/></p><br/>

            <g:if test="${params.id=='1'}">
                <div class="btn-row">
                    <g:link action="downloadPricesFile" params="['actionTaken': 'default']"
                            class="submit add">Download Prices</g:link>
                </div>
            </g:if>
            <g:if test="${params.id=='2'}">
                <div class="btn-row">
                    <g:link action="downloadPricesFile" params="['actionTaken': 'accounttype']"
                            class="submit add">Download Prices</g:link>
                </div>
            </g:if>
            <g:if test="${params.id=='3'}">
                <div class="btn-row">
                    <g:link action="downloadPricesFile" params="['actionTaken': 'customer']"
                            class="submit add">Download Prices</g:link>
                </div>
            </g:if>
            <g:if test="${params.id=='4'}">
                <div class="btn-row">
                    <g:link action="downloadPricesFile" params="['actionTaken': 'plan']"
                            class="submit add">Download Prices</g:link>
                </div>
            </g:if>
        </div>
    </div>

</div>

