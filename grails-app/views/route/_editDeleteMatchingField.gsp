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

<ul>
    <li>
        <g:remoteLink controller = "${params.controller}"
                         action = "editMatchingField"
                         update = "matching-field-form-holder"
                          class = "submit"
                         params = "[        routeId: routeId,
                                    routeRateCardId: routeRateCardId,
                                            fieldId: matchingFieldSelectedId]">
            <g:message code="button.edit"/>
        </g:remoteLink>
    </li>
    <li>
        <a onclick="showConfirmAnother('deleteMatchingField-${matchingFieldSelectedId}');" class="submit delete">
            <span><g:message code="button.delete"/></span>
        </a>
    </li>
</ul>

<g:render template="/confirm"
          model="[   message: 'matching.field.delete.confirm',
                  controller: params.controller,
                          id: matchingFieldSelectedId,
                  formParams: ['routeId': routeId],
                      action: 'deleteMatchingField',
                        ajax: false,
                      update: 'users-contain',
                        name:'delete'+matchingFieldSelectedId]" />
