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
<g:applyLayout name="form/select_holder">
    <content tag="label.for">usagePool.consumptionActions.${actionIndex}.mediumType</content>
    <content tag="include.script">true</content>

    <g:select style="width: 20%; text-align: top;"
          name="usagePool.consumptionActions.${actionIndex}.mediumType"
          from="${mediumTypes}"
          value="${usagePoolAction?.mediumType}"
          noSelection="${['':'--']}"/>
</g:applyLayout>
