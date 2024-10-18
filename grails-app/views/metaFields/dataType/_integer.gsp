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
<g:applyLayout name="form/input">
    <content tag="label">
        <g:message code="${field.name}"/>
        <g:if test="${field.mandatory}">
            <span id="mandatory-meta-field">*</span>
        </g:if>
    </content>
    <content tag="label.for">metaField_${field.id}.value</content>

    <g:textField  name = "metaField_${field.id}.value"
                 class = "field text ${field.fieldUsage ? 'field_usage':''}"
                 value = "${fieldValue}"/>

    <g:render template="/metaFields/metaFieldHelp" model="[field:field]" />
</g:applyLayout>
<script>
    $('input[name="metaField_${field.id}.value"]').keyup(function() {
        var valueMF = $(this).val();
        if (/\D/g.test(valueMF)){
            $(this).val(valueMF.replace(/\D/g,''));
            $(this).attr('title', "${g.message(code: 'validation.message.error.invalid.pattern')}");
            $(this).addClass("toolTipElement");
            $(this).tooltip({
                show: {
                      effect: "none",
                       delay: 0,
                    duration: 3000
                }
            });

            $(this).tooltip("enable");
            $(this).trigger('mouseenter');
            $(this).blur();
        }

        if (Number(valueMF) > 2147483647 || Number(valueMF) < -2147483647){
            $(this).val(valueMF.slice(0, -1));
            $(this).attr('title', "${g.message(code: 'validation.error.value', args: [-2147483647, 2147483647])}");
            $(this).addClass("toolTipElement");
            $(this).tooltip({
                show: {
                      effect: "none",
                       delay: 0,
                    duration: 3000
                }
            });
            $(this).tooltip("enable");
            $(this).trigger('mouseenter');
            $(this).blur();
        }
    });

    $('input[name="metaField_${field.id}.value"]').on('click', function(){
        $('.toolTipElement').tooltip("destroy");
    });

    $('input[name="metaField_${field.id}.value"]').hover(function(){
        return false;
    });
</script>

