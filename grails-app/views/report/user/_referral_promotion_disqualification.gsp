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
  Parameters for the Referral Promotion Disqualification report.

  @author Matias Cabezas
  @since  27-Sep-2017
--%>

<div class="form-columns">

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="required_referrals" /></content>
        <content tag="label.for">required_referrals</content>
        <g:textField class="field" name="required_referrals"/>
    </g:applyLayout>

</div>
