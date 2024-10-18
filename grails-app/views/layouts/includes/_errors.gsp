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
  hidden div for javascript validation errors

  @author Brian Cowdery
  @since  12-Feb-2012
--%>

<div id="error-messages" class="msg-box error" style="display: none;">
    <strong><g:message code="flash.error.title"/></strong>
    <a class="msg-box-close"  onclick="$(this).closest('#error-messages').hide();">&#xe020;</a>
    <ul></ul>
</div>
