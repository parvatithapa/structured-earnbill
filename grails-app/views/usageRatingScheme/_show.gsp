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

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.ratingSchemeCode}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                    <tr>
                        <td><g:message code="ratingScheme.id"/></td>
                        <td class="value">${selected?.id}</td>
                    </tr>
                    <tr>
                        <td><g:message code="usageRatingScheme.scheme.code"/></td>
                        <td class="value">${selected?.ratingSchemeCode}</td>
                    </tr>
                    <tr>
                        <td><g:message code="usageRatingScheme.scheme.type"/></td>
                        <td class="value">${selected?.ratingSchemeType}</td>
                    </tr>
                    <g:each var="attribute" in="${selected?.fixedAttributes}">
                        <tr>
                            <td>${attribute.key}</td>
                            <td class="value">${attribute.value}</td>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </div>
    </div>

    <g:hiddenField name="ratingSchemeId" value="${selected.id}"/>
    <div class="btn-box">
        <div class="row">
            <g:remoteLink class="submit add" id="${selected.id}" action="edit" update="column2">
                <span><g:message code="button.edit"/></span>
            </g:remoteLink>
            <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
        </div>
    </div>
    <g:render template="/confirm"
              model="['message': 'config.usage.rating.scheme.delete.confirm',
                      'controller': 'usageRatingScheme',
                      'action': 'delete',
                      'id': selected.id,
              ]"/>
</div>
