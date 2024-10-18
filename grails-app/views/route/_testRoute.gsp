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
    <strong><g:message code="route.test"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <g:formRemote id="find-route" name="find-route"
                      url="[controller: 'route', action: 'testRoute']"
                      update="test-route-result">
            <g:hiddenField name="routeId" value="${selected?.id}"/>
            <fieldset>
                <div class="form-columns">
                    <g:if test="${matchingFields}">
                        <g:each in="${matchingFields}">
                            <g:applyLayout name="form/input">
                                <content tag="label">${it.description}</content>
                                <content tag="label.for">${it?.mediationField}</content>
                                <g:textField name="test.${it?.mediationField}" class="field" value=""/>
                            </g:applyLayout>
                        </g:each>
                    </g:if>
                </div>
            </fieldset>

            <div id="test-route-result">
            </div>
            <!-- spacer -->
            <div>
                <br/>&nbsp;
            </div>
        </g:formRemote>
    </div>

    <div class="btn-box buttons">
        <div class="row">
            <div>
                <a class="submit save" onclick="$('#find-route').submit();">
                    <span><g:message code="button.test"/></span>
                </a>
            </div>
        </div>
    </div>

</div>

