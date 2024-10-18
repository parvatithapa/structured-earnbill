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
<script>
    function validateDate(element) {
        var dateFormat= "<g:message code="date.format"/>";
        if(!isValidDate(element, dateFormat)) {
            $("#error-messages").css("display","block");
            $("#error-messages ul").css("display","block");
            $("#error-messages ul").html("<li><g:message code="invalid.date.format"/></li>");
            element.focus();
            return false;
        } else {
            return true;
        }
    }
</script>

<g:applyLayout name="form/date">
    <content tag="label"><g:message code="${field.name}"/><g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if></content>
    <content tag="label.for">metaField_${field.id}.value</content>

    <g:textField class="field dateobject ${field.fieldUsage ? 'field_usage':''} {validate:{ date:true, messages: {date: ' ${g.message(code:"metafield.date.valid",args:[field.name]) }' }}}"
                 name="metaField_${field.id}.value" onblur="validateDate(this)"
                 value="${fieldValue ? formatDate(date: fieldValue, formatName: 'datepicker.format') : ""}"/>
</g:applyLayout>
