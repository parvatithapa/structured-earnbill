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

<%@ page contentType="text/html;charset=UTF-8" %>

<div id="schemeModel">
    <div class="column-hold">
        <g:set var="isNew" value="${!ratingScheme || !ratingScheme?.id || ratingScheme?.id == 0}"/>
        <div class="heading">
            <strong>
                <g:if test="${isNew}">
                    <g:message code="ratingScheme.add.title"/>
                </g:if>
                <g:else>
                    <g:message code="ratingScheme.edit.title"/>
                </g:else>
            </strong>
        </div>
        <g:form id="save-unit-form" name="unit-form" url="[action: 'save']" >
        <g:hiddenField name="ratingSchemeId" value="${ratingScheme?.id}"/>
        <input type="hidden" name="isNew" value="${isNew}">
        <div class="box">
            <div class="sub-box">
                <fieldset>
                    <div class="form-columns">
                        <g:hiddenField name="id" value="${ratingScheme?.id}"/>
                        <g:applyLayout name="form/input">
                            <content tag="label"><g:message code="usageRatingScheme.scheme.code"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">ratingSchemeCode</content>
                            <g:textField class="field" name="ratingSchemeCode" value="${ratingScheme?.ratingSchemeCode}"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/select">
                            <content tag="label"><g:message code="usageRatingScheme.scheme.type"/><span id="mandatory-meta-field">*</span></content>
                            <content tag="label.for">ratingSchemeType</content>
                            <content tag="include.script">true</content>
                                <g:select name="ratingSchemeType" id="ratingSchemeType"
                                          from="${ratingSchemeStrategies}"
                                          optionKey="name" optionValue="name"
                                          value="${ratingScheme?.ratingSchemeType}"
                                          noSelection="['':'-Select Rating Scheme Type']"
                                          />
                        </g:applyLayout>
                        <div id="schemeHolder" >
                            <g:if test="${ratingScheme?.ratingSchemeType}">
                                <g:render template="/usageRatingScheme/attributes" model="[ratingScheme:ratingScheme, ratingSchemeType: ratingSchemeType]"/>
                            </g:if>
                        </div>
                    </div>
                </fieldset>
            </div>
        </div>
        </g:form>

        <div class="btn-box">
            <a class="submit save button-primary" onclick="$('#save-unit-form').submit();"><span><g:message code="button.save"/></span></a>
            <a class="submit cancel" onclick="closePanel(this);"><span><g:message code="button.cancel"/></span></a>
        </div>

    </div>
</div>

<script type="text/javascript">

    $(function() {
        $('#ratingSchemeType').change(function() {
            if($(this).val()=='') {
                $('#schemeHolder').html('');
            }
            else {
                updateStrategy();
            }
        });
    });

    function updateStrategy() {
        var selectedTemplate=$('#ratingSchemeType').val();
        <g:remoteFunction  action="updateStrategy" update="schemeHolder" params="'templateName='+selectedTemplate"/>
    }
</script>
