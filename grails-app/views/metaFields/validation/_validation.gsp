<%@ page import="com.sapienter.jbilling.server.metafields.validation.ValidationRuleType; org.apache.commons.lang.WordUtils; com.sapienter.jbilling.server.util.db.EnumerationDTO; com.sapienter.jbilling.server.user.db.CompanyDTO; com.sapienter.jbilling.server.metafields.DataType;com.sapienter.jbilling.server.metafields.MetaFieldType" %>

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
  Validation rule model

  Parameters to be passed to this template:

  1. validationRule - Validation Rule
  2. parentId - Id of the parent submit form
  3. metaFieldIdx - (optional) Used when rendering multiple instances of the template in order to provide an unique
        name for input fields. The metaFieldIdx will be appended to all names

  @author Panche Isajeski, Shweta Gupta
--%>

<g:set var="types" value="${com.sapienter.jbilling.server.metafields.validation.ValidationRuleType.values() as java.util.List}"/>
<g:set var="validationType" value="${validationRule?.ruleType ? ValidationRuleType.valueOf(validationRule?.ruleType) : types?.first()}"/>

<div id="metaField${metaFieldIdx}.validationModel" class="form-columns">
    <g:hiddenField name="metaFieldIdx" value="${metaFieldIdx}"/>
    <g:hiddenField name="metaField${metaFieldIdx}.validationRule.enabled" value="${enabled}"/>
    <g:hiddenField name="metaField${metaFieldIdx}.validationRule.id" value="${validationRule?.id}"/>
    <g:render template="/metaFields/validation/validationRule" model="[validationRule: validationRule, metaFieldIdx: metaFieldIdx, validationType : validationType, types: types, parentId: parentId]"/>
</div>


<script type="text/javascript">

    $(function() {
        $('[name="metaField${metaFieldIdx}.validationRule.ruleType"]').change(function() {
            var val  = $("[name='metaField${metaFieldIdx}.validationRule.ruleType']").attr('value');
            if(val==null || val==""){
                $("[name='metaField${metaFieldIdx}.validationRule.enabled']").attr('value', false);
            } else {
                $("[name='metaField${metaFieldIdx}.validationRule.enabled']").attr('value', true);
            }
            updateValidationModel${metaFieldIdx}();
        });
    });

    function updateValidationModel${metaFieldIdx}() {
        var $parentForm = $('div[id="metaField${metaFieldIdx}.validationModel"] :input');
        $.ajax({
            type: 'POST',
            url: '${createLink(controller:'metaFields', action: 'updateValidationModel')}',
            data: $parentForm.serialize(),
            success: function(data) { $('div[id="metaField${metaFieldIdx}.validationModel"]').replaceWith(data); }
        });
    }

</script>