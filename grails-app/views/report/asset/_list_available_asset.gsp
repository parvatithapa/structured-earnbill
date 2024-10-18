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
  Parameters for the List Asset report.

  @author Leandro Bagur
  @since  27-03-2018
--%>

<div class="form-columns">

    <g:applyLayout name="form/input">
        <content tag="label"><g:message code="threshold.asset" /></content>
        <content tag="label.for">threshold_asset</content>
        <g:textField class="field" name="threshold_asset"/>
    </g:applyLayout>

</div>
