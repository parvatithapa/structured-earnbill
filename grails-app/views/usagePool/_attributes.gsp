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
<g:set var="actionIndex" value="${0}"/>
<!-- remaining user-defined attributes -->
<g:each var="action" in="${usagePool?.consumptionActions?.sort({it.percentage})}">
    <g:set var="actionIndex" value="${actionIndex + 1}"/>
    <g:render template="/usagePool/attribute" model="[usagePool: usagePool, consuptionActions: consuptionActions,
            actionIndex:actionIndex, usagePoolAction:action, notifications:notifications]" />
</g:each>
<g:set var="actionIndex" value="${actionIndex + 1}"/>
<g:render template="/usagePool/attribute" model="[usagePool: usagePool, consuptionActions: consuptionActions,
        actionIndex:actionIndex, usagePoolAction:null, notifications:notifications]" />

</script>