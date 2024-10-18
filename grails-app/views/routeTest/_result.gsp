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

<div class="heading">
    <strong><g:message code="route.test.results.title"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <fieldset>
            <div class="form-columns">

                <g:if test="${!flash.errorMessages || flash?.errorMessages.size() == 0}">

                    <g:applyLayout name="form/text">
                        <content tag="label" class="label2"><g:message code="root.route.name"/></content>
                        ${rootRoute?.name}
                    </g:applyLayout>

                    <g:applyLayout name="form/text">
                        <content tag="label" class="lab el2"><g:message code="route.record.product"/></content>
                        <g:if test="${routeRecord?.product}" >
                            ${routeRecord?.product}
                        </g:if>
                    </g:applyLayout>

                    <g:applyLayout name="form/text">
                        <content tag="label" class="label2"><g:message code="route.last.route"/></content>
                        ${routeRecord?.routeTable?.tableName}
                    </g:applyLayout>

                    <g:applyLayout name="form/text">
                        <content tag="label" class="label2"><g:message code="route.next.route"/></content>
                        ${routeRecord?.nextRoute}
                    </g:applyLayout>

                    <!-- spacer -->
                    <div>
                        <br/>&nbsp;
                    </div>

                    <g:each in="${fields}" var="field" >

                        <g:applyLayout name="form/text">
                            <content tag="label" class="label2">${field?.name}</content>
                            ${field?.value?.toString()}
                        </g:applyLayout>

                    </g:each>

                </g:if>

            </div>
        </fieldset>
    </div>
</div>


