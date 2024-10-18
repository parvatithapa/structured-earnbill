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

<%@ page import="com.sapienter.jbilling.server.util.Constants; org.quartz.SimpleTrigger" %>
  
<div id="plugin-parameters">
    <div class="form-columns">
		<div class="one_column">

            <g:set var="parameterIndex" value="${0}"/>
            <g:set var="parameters" value="${pluginws ? new HashMap<String, String>(pluginws?.getParameters()) : new HashMap<String, String>()}"/>
            <g:each var="${param}" in="${parametersDesc}">
                <div class="row">
                    <label class = "labelWrap">
                       <g:if test="${param.required==true}">
                        	${param.name}<font color='red'>*</font>
                       </g:if>
                       <g:else>
                        	${param.name}
                       </g:else>
                    </label>
                    <div class="inp-bg">
                        <g:set var="value" value="${pluginws ? (pluginws.getParameters()?.get(param.name)) : param.defaultValue}"/>
                        <g:if test="${param.name == Constants.PARAM_REPEAT}">
                            <g:textField class="field" name="plg-parm-${param.name}" value="${value?:SimpleTrigger.REPEAT_INDEFINITELY}" />
                        </g:if>
                        <g:else>
                            <g:if test="${param.isPassword==true}">
                                <g:passwordField class="field" name="plg-parm-${param.name}" value="${value}" />
                        	</g:if>
                       		<g:else>
                           		<g:textField class="field" name="plg-parm-${param.name}" value="${value}" />
                        	</g:else>
                        </g:else>
                    </div>
                </div>
                %{
                    parameters.remove(param.name)
                }%
            </g:each>

            <g:each var="${parameterEntry}" in="${parameters.entrySet()}">
                <g:set var="parameterIndex" value="${parameterIndex + 1}"/>

                <g:applyLayout name="form/attribute">
                    <content tag="name">
                        <g:textField class="field" name="plgDynamic.${parameterIndex}.name" value="${parameterEntry.key}"/>
                    </content>
                    <content tag="value">
                        <div class="inp-bg inp4">
                        <g:textField class="field" name="plgDynamic.${parameterIndex}.value" value="${parameterEntry.value}"/>
                        </div>
                    </content>

                    <a class="plus-icon" onclick="removePluginParameter(this, ${parameterIndex})">&#xe000;</a>
                </g:applyLayout>
            </g:each>


            <g:set var="parameterIndex" value="${parameterIndex + 1}"/>
            <g:applyLayout name="form/attribute">
                <content tag="name">
                    <g:textField class="field" name="plgDynamic.${parameterIndex}.name"/>
                </content>
                <content tag="value">
                    <div class="inp-bg inp4">
                    <g:textField class="field" name="plgDynamic.${parameterIndex}.value"/>
                    </div>
                </content>

                <a class="plus-icon" onclick="addPluginParameter(this, ${parameterIndex})">&#xe026;</a>
            </g:applyLayout>

            <g:hiddenField name="parameterIndexField" value="${parameterIndex}"/>

        </div>
    </div>

    <script type="text/javascript">

        $(document).ready(function() {
            // Adjust dynamic attributes padding
            $('.dynamicAttrs').removeClass().addClass("plugin-attribute-dynamic")
        });

        function addPluginParameter(element, parameterIndex) {

            $('#parameterIndexField').val(parameterIndex+1);

            $.ajax({
                type: 'POST',
                url: '${createLink(action: 'addPluginParameter')}',
                data: $('#plugin-parameters').parents('form').serialize(),
                success: function(data) {
                    $('#plugin-parameters').replaceWith(data);
                    //alert(data);
                }
            });
        }

        function removePluginParameter(element, parameterIndex) {

            $('#parameterIndexField').val(parameterIndex);

            $.ajax({
                type: 'POST',
                url: '${createLink(action: 'removePluginParameter')}',
                data: $('#plugin-parameters').parents('form').serialize(),
                success: function(data) {
                    $('#plugin-parameters').replaceWith(data);
                    //alert(data);
                }
            });
        }
    </script>
</div>
