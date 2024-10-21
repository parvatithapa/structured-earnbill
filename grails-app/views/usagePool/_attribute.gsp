<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="jbilling.UsagePoolController" %>
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
  Editor form for usage Pool consumption attributes.
  @author Amol Gadre
  @since  10-Feb-2014
--%>

<div id="custom-div3" align="center">
    <table style="width: 100%;">
        <tr>
            <td style="width: 70%;">
                <div class="row" >
                        <label><div class="inp-bg inp-bg-label"><g:field type="number" step="1" min="0" max="100" class="field" name="usagePool.consumptionActions.${actionIndex}.percentage" value="${usagePoolAction?.percentage}"  maxlength="5"/></div></label>
                        <g:applyLayout name="form/select_holder">
                            <content tag="label.for">usagePool.consumptionActions.${actionIndex}.type</content>
                            <content tag="include.script">true</content>

                            <g:select style="width: 75%; text-align: top;"
                                      name="usagePool.consumptionActions.${actionIndex}.type"
                                      from="${consuptionActions}"
                                      noSelection="${['':'--']}"
                                      value="${usagePoolAction?.type}"
                                      onchange="showMoreInfo(${actionIndex});"/>
                        </g:applyLayout>
                        <g:if test="${usagePoolAction != null}" >
                            <a class="plus-icon" onclick="removeModelAttribute(this, ${actionIndex})"> &nbsp;&#xe000;</a>
                        </g:if>
                        <g:else>
                            <a class="plus-icon" onclick="addModelAttribute(this, ${actionIndex})"> &nbsp;&#xe026; </a>
                        </g:else>
                </div>
                %{--<div class="inp-bg inp4" style="margin-top: 8px;">
                    <g:field type="number" step="1" min="0" max="100" class="field" name="usagePool.consumptionActions.${actionIndex}.percentage" value="${usagePoolAction?.percentage}"  maxlength="5"/>
                </div>
                <g:applyLayout name="form/select">
                        <content tag="label.for">usagePool.consumptionActions.${actionIndex}.type</content>
                        <content tag="include.script">true</content>

                        <div style="vertical-align: top;">
                        <g:select style="width: 75%; text-align: top;"
                                  name="usagePool.consumptionActions.${actionIndex}.type"
                                  from="${consuptionActions}"
                                  noSelection="${['':'--']}"
                                  value="${usagePoolAction?.type}"
                                  onchange="showMoreInfo(${actionIndex});"/>
                    </div>
                    <g:if test="${usagePoolAction != null}" >
                        <a class="plus-icon" onclick="removeModelAttribute(this, ${actionIndex})"> &nbsp;&#xe000;</a>
                    </g:if>
                    <g:else>
                        <a class="plus-icon" onclick="addModelAttribute(this, ${actionIndex})"> &nbsp;&#xe026; </a>
                    </g:else>

                </g:applyLayout>--}%
                <div style="vertical-align: top;  padding-top: 8px; display: none" id="notification-info-${actionIndex}">
                    <g:applyLayout name="form/select_holder">
                        <content tag="label.for">usagePool.consumptionActions.${actionIndex}.notificationId</content>
                        <content tag="include.script">true</content>
                        <g:select style="width: 40%; text-align: top;"
                              name="usagePool.consumptionActions.${actionIndex}.notificationId"
                              optionKey="id"
                              optionValue="description"
                              value="${usagePool != null && StringUtils.isNotBlank(usagePoolAction?.notificationId) ? new Integer(usagePoolAction?.notificationId).intValue() : 0}"
                              from="${notifications}"
                              onChange="${remoteFunction(action: 'retrieveNotificationMediumType',
                                      update: 'usagePool\\\\.consumptionActions\\\\.' + actionIndex + '\\\\.mediumType\\\\.dynamic',
                                      params: '\'notificationId=\' + this.value +  \'&actionIndex=\' + ' + actionIndex)}"
                              noSelection="${['':'--']}"/>
                    </g:applyLayout>
                    %{
                        def mediumTypes = [];
                        if (usagePoolAction?.notificationId ) {
                            mediumTypes = UsagePoolController.findMediumTypesForNotificationMessageTypeDtoId(
                                    usagePoolAction.notificationId.toInteger(), session['language_id'], session['company_id'])
                        }
                    }%
                    <div id="usagePool.consumptionActions.${actionIndex}.mediumType.dynamic">
                        <g:render template="mediumTypeDropDown" model="[mediumTypes: mediumTypes, actionIndex:actionIndex,
                            usagePoolAction:usagePoolAction]" />
                    </div>
                </div>
                <div style="vertical-align: top; display: none" id="fee-info-${actionIndex}" >
                    <g:applyLayout name="form/input">
                        <content tag="label"><g:message code="usagePool.productId"/></content>
                        <content tag="label.for">usagePool.consumptionActions.${actionIndex}.productId</content>
                        <g:field type="number" class="field" name="usagePool.consumptionActions.${actionIndex}.productId"
                                 value="${usagePoolAction?.productId}"
                                 maxlength="10"/>
                    </g:applyLayout>

                    %{--<g:message code="usagePool.productId"/>  <g:field type="number" class="field" name="usagePool.consumptionActions.${actionIndex}.productId"
                                                                        value="${usagePoolAction?.productId}"
                                                                        maxlength="10"/>--}%
                </div>
            </td>
        </tr>
    </table>
</div>
</script>
