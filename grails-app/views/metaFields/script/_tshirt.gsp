%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

%{--
    This script can be used in a meta field of type SCRIPT. It will render three
    radio buttons in a group. The label will be 'T-Shirt Size' and the three radio
    buttons will have values 'S', 'M' and 'L'. It is meant to be used against a
    customer and record the customer's t-shirt size.

    Whenever the user select a radio button the java script will update the value
    of the hidden field with name="metaField_${field.id}.value". When the users
    save the changes on screen the value in the hidden field will be used to store
    in database. Afterwards, when the user wants to update the value this script is
    aware of the format the was used to store the value in database and it is able
    to render this value back on screen i.e. select the correct radio button.
--}%

<r:script disposition="head">
    $(document).ready(function() {
        $('.tShirtGroup${field.id}').change(function() {
           var selectedValue = $("input[name='tShirtGroup${field.id}']:checked").val()
           $('#metaField_${field.id}\\.value').val(selectedValue)
        });
    });
</r:script>

<g:applyLayout name="form/radio">
    <content tag="label"><g:message code="${field.name}"/><g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if></content>
    <content tag="label.for">metaField_${field.id}.value</content>

    %{--this is the value is saved in database--}%
    <g:hiddenField name="metaField_${field.id}.value" value="${fieldValue}" />

    <g:radio id="tShirtGroup${field.id}S" class="tShirtGroup${field.id}" name="tShirtGroup${field.id}" value="S"
             checked="${fieldValue && fieldValue.equals("S")}"/>S
    <g:radio id="tShirtGroup${field.id}M" class="tShirtGroup${field.id}" name="tShirtGroup${field.id}" value="M"
             checked="${fieldValue && fieldValue.equals("M")}"/>M
    <g:radio id="tShirtGroup${field.id}L" class="tShirtGroup${field.id}" name="tShirtGroup${field.id}" value="L"
             checked="${fieldValue && fieldValue.equals("L")}"/>L
</g:applyLayout>