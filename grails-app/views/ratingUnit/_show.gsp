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
            ${selected.name}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="ratingUnit.id"/></td>
                <td class="value">${selected.id}</td>
            </tr>
            <tr>
                <td><g:message code="ratingUnit.priceUnitName"/></td>
                <td class="value">${selected?.priceUnit?.name}</td>
            </tr>
            <tr>
                <td><g:message code="ratingUnit.incrementUnitName"/></td>
                <td class="value">${selected?.incrementUnit?.name}</td>
            </tr>
            <tr>
                <td><g:message code="ratingUnit.incrementUnitQuantity"/></td>
                <td class="value">
                    <g:formatNumber number="${selected?.incrementUnit?.quantity}" maxFractionDigits="2" />
                </td>
            </tr>
            </tbody>
        </table>
      </div>
    </div>

    <div class="btn-box">
        <div class="row">
            <g:remoteLink class="submit add" id="${selected.id}" action="edit" update="column2">
                <span><g:message code="button.edit"/></span>
            </g:remoteLink>
            <g:if test="${selected.canBeDeleted}">
                <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </g:if>
        </div>
    </div>

    <g:render template="/confirm"
              model="['message': 'config.rating.unit.delete.confirm',
                      'controller': 'ratingUnit',
                      'action': 'delete',
                      'id': selected.id,
                     ]"/>
</div>