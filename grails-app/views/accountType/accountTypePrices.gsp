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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
</head>
<body>

<div class="form-edit">

    <div class="heading">
        <strong>
            ${accountType.getDescription(session['language_id'])}
        </strong>
    </div>

    <div class="form-hold">
        <fieldset>

            <div id="prices" class="box-cards box-cards-open">
                <div class="box-cards-title">
                    <a class="btn-open"><span><g:message code="account.type.prices.title"/></span></a>
                </div>
                <div class="box-card-hold">
                    <div class="form-columns">
                        <div id="products-column" class="column">
                            <g:render template="products"/>
                        </div>

                        <div id="prices-column" class="column">
                            <g:render template="prices" model="[priceExpiryMap: priceExpiryMap]"/>
                        </div>
                    </div>
                </div>
            </div>

            <!-- spacer -->
            <div>
                <br/>&nbsp;
            </div>

        </fieldset>
    </div>  <!-- end form-hold -->

</div> <!-- end form-edit -->
</div>

</body>
</html>