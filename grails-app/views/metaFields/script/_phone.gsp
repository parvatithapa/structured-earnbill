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
    This script can be used in a meta field of type SCRIPT. It can be used to record
    a person's phone number. A phone number consist of a country code, area code and a
    number. The country code and area code are rendered as drop downs and the phone
    number is rendered as input field. When a user changes a value in the country
    code drop down then the values in the area code drop down are automatically changed.
    This allows the user to only select an area code that is valid for the selected
    country.

    Whenever a user makes a change in any of the field a java script code will try to
    update the value of the hidden field with name="metaField_${field.id}.value". The
    value that this hidden field holds will be stored into the database. The hidden
    field will hold a value only if the three fields are defined. The logic will not
    save partial phone numbers. The value of the hidden field is also formatted with
    in following format <country_code>-<area_code>-<phone_number>. When the user
    wants to edit already persisted phone number that the java script in this file
    knows how to parse the phone numbers and set all the input fields.
--}%

<r:script disposition="head">
    $(document).ready(function() {
        //ensure only digits can be input in the number field
        $(".field.phone-text").keydown(function(event) {
            // Allow: backspace, delete, tab, escape, and enter
            if ( event.keyCode == 46 || event.keyCode == 8 || event.keyCode == 9 || event.keyCode == 27 || event.keyCode == 13 ||
                 // Allow: Ctrl+A
                (event.keyCode == 65 && event.ctrlKey === true) ||
                 // Allow: home, end, left, right
                (event.keyCode >= 35 && event.keyCode <= 39)) {
                 // let it happen, don't do anything
                     return;
            }
            else {
                // Ensure that it is a number and stop the keypress
                if (event.shiftKey || (event.keyCode < 48 || event.keyCode > 57) && (event.keyCode < 96 || event.keyCode > 105 )) {
                    event.preventDefault();
                }
            }
        });

        //when key is release on this field update the selected value
        $(".field.phone-text").keyup(function() {
            updatePhoneValue();
        });

        //update the values in the components from database values
        var number = $('#metaField_${field.id}\\.value').val();
        if(!isEmpty(number)){
            var numberParts = number.split("-");
            if(numberParts.length === 3){
                $("#phone${field.id}\\.countryCode").val(numberParts[0]);
                $("#phone${field.id}\\.areaCode").val(numberParts[1]);
                $("#phone${field.id}\\.number").val(numberParts[2]);
            }
        }
    });

    function updatePhoneValue(){
        var country = $("#phone${field.id}\\.countryCode").val()
        var area = $("#phone${field.id}\\.areaCode").val()
        var number = $("#phone${field.id}\\.number").val()

        if(isEmpty(country) || isEmpty(area) || isEmpty(number)){
            $('#metaField_${field.id}\\.value').val('')
        } else {
           $('#metaField_${field.id}\\.value').val(country + '-' + area + '-' + number)
        }
    }

    function isEmpty(str) {
        return (!str || 0 === str.length);
    }

</r:script>

%{--this is the value is saved in database--}%
<g:hiddenField name="metaField_${field.id}.value" value="${fieldValue}" />

<g:applyLayout name="form/text">
    <content tag="label">
        <g:message code="prompt.phone.number"/>
        <g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if>
    </content>
    <content tag="label.for">contact.phoneCountryCode</content>
    <span>
        <g:textField id="phone${field.id}.countryCode" name="phone${field.id}.countryCode" class="field phone-text" maxlength="3" size="2"/>
        -
        <g:textField id="phone${field.id}.areaCode" name="phone${field.id}.areaCode" class="field phone-text" maxlength="5" size="2"/>
        -
        <g:textField id="phone${field.id}.number" name="phone${field.id}.number" class="field phone-text" maxlength="10" size="7"/>
    </span>
</g:applyLayout>