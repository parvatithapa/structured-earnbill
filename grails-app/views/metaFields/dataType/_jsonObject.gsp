<%@ page import="com.fasterxml.jackson.databind.ObjectMapper; grails.converters.JSON" %>


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


<style>

    .jsonData .row label{
        font-size: 10px;
    }

    .row > .inp-bg > .remove {
        position: absolute; z-index: 99; margin-left: -19px; margin-top: -4px;
    }

    html[dir="rtl"] .row > .inp-bg > .remove {
        margin-right: -19px;
        margin-left: auto;
    }

</style>

<g:if test="${fieldValue}">
    <div class="row">
        <label ><b><g:message code="${field.name}"/></b></label><g:if test="${field.mandatory}"><span id="mandatory-meta-field">*</span></g:if></label>
    </div>

    <%
        /* converting json string to map */
        com.fasterxml.jackson.databind.ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> mapObject = mapper.readValue(fieldValue, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>(){});
    %>

    <div class="jsonData">
        <g:each in="${mapObject}" var="object">
            <g:applyLayout name="form/input">

                <content tag="label" ><g:message code="${object.key}"/></content>
                <g:hiddenField name="JSON_metaField_${field.id}_name" value="${object.key}"/>
                <g:textField name="JSON_metaField_${field.id}_value"
                             class="field text"
                             value="${object?.value}"/>
                <a class="remove">
                    <img src="${resource(dir: 'images', file: 'remove.png')}" alt="remove">
                </a>
            </g:applyLayout>
        </g:each>

        <div class="row">
            <div>
                <label class="" title="" for="">&nbsp</label>
                <table>
                    <tbody>
                    <tr>
                        <td>
                            <div class="inp-bg inp4">
                                <input type="text" class="field" name="JSON_metaField_${field.id}_name">
                            </div>
                        </td>
                        <td>
                            <div class="inp-bg inp4">
                                <input type="text" class="field" name="JSON_metaField_${field.id}_value">
                            </div>
                        </td>
                        <td>
                            <a class="add">
                                <img src="${resource(dir: 'images', file: 'add.png')}" alt="add">
                            </a>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</g:if>

<script>
    $(function(){

        $(".jsonData").on('click', '.add' , function(){
            $(".jsonData table tbody").append('<tr><td><div class="inp-bg inp4"><input type="text" class="field" name="JSON_metaField_${field.id}_name"></div></td><td><div class="inp-bg inp4"><input type="text" class="field" name="JSON_metaField_${field.id}_value"></div></td><td><a class="add"><img src="${resource(dir: 'images', file: 'add.png')}" alt="add"></a></td></tr>');
            $(this).parent().html('<a class="remove"><img src="${resource(dir: 'images', file: 'remove.png')}" alt="remove"></a>')
        });

        $(".jsonData").on('click', '.remove' , function(){
            $(this).closest("tr").remove();
            $(this).closest(".row").remove();
        });
    });
</script>


