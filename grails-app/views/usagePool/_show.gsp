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

<%--
  Shows usage pool details.

  @author Amol Gadre
  @since  14-Nov-2013
--%>

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.getDescription(session['language_id'], 'name')}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <table class="dataTable" cellspacing="0" cellpadding="0">
            <tbody>
            <tr>
                <td><g:message code="usagePool.label.id"/></td>
                <td class="value">${selected.id}</td>
            </tr>
            <tr>
                <td><g:message code="usagePool.label.name"/></td>
                <td class="value">${selected.getDescription(session['language_id'], 'name')}</td>
            </tr>
            <tr>
                <td><g:message code="usagePool.quantity"/></td>
                <td class="value">${selected?.quantity}</td>
            </tr>
            <tr>
                <td><g:message code="usagePool.precedence"/></td>
                <td class="value">${selected.precedence}</td>
            </tr>
            <tr>
                <td><g:message code="usagePool.cyclePeriod"/></td>
                <td class="value">${selected.cyclePeriodValue} ${selected.cyclePeriodUnit}</td>
            </tr>
            <tr>
                <td><g:message code="usagePool.itemTypes"/></td>
                <td class="value">
                <g:each var="itemType" in="${selected.itemTypes}">
                ${itemType.getDescription()} </br>
                </g:each>
                </td>
            </tr>
            <tr>
                <td><g:message code="usagePool.items"/></td>
                <td class="value">
                <g:each var="item" in="${selected.items}">
                ${item.getDescription()} </br>
                </g:each>
                </td>
            </tr>
            <tr>
                <td><g:message code="usagePool.resetValue"/></td>
                <td class="value">${selected.usagePoolResetValue}</td>
            </tr>
            <tr style="height: 7px;"></tr>
            <tr>
            	<td colspan="2" style="color:#858585;"><u> <g:message code="usagePool.label.consumption.actions"/> </u></td>
            </tr>
            <tr style="height: 7px;"></tr>
            <g:if test="${selected.consumptionActions}">
            	<tr class="price">
					<td style="color:#858585;width: 36%;text-align: center;" ><g:message code="usagePool.label.consumption.percentage"/></td>
					<td style="color:#858585;width: 67%;text-align: center;" class="value"><g:message code="usagePool.label.consumption.action"/></td>
				</tr>
	           	<g:each var="action" in="${selected.consumptionActions.sort({it.percentage})}">
	       		 <g:if test="${action.type}">
	                 <tr class="attribute">
		                <td style="text-align: right;padding-right: 30px;">
		                	<g:formatNumber number="${action.percentage}" formatName="decimal.format"/>
		                </td>
		                <td class="value">${action.type}</td>
		            </tr>
		         </g:if>
	    		</g:each>
            </g:if>
            <g:else>
            	<tr>
            	<td colspan="2" style="color:#858585;"><g:message code="usagePool.label.no.consumption.actions.defined"/></td>
            </tr>
            </g:else>
            
          </tbody>
        </table>
      </div>
    </div>

    <div class="btn-box">
        <div class="row">
            <g:link action="edit" id="${selected.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
            <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
        </div>
    </div>

    <g:render template="/confirm"
              model="['message': 'usagePool.delete.confirm',
                      'controller': 'usagePool',
                      'action': 'delete',
                      'id': selected.id,
                      'ajax': false,
                      'update': 'column1',
                     ]"/>
</div>
