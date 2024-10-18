<%@ page import="jbilling.FilterType" %>
%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2016] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<div>
    <g:render template="/layouts/includes/messages"/>
    <g:render template="/layouts/includes/errors"/>
</div>

%{--<div class="heading">
    <strong><g:message code="filters.new.name"/>
        ${raw(FilterType.PARTNER.toString().equalsIgnoreCase(typeName) ? "Agent".replaceAll('_', ' ') : typeName.replaceAll('_', ' '))}
        <g:message code="filters.set.name"/>
    </strong>
</div>--}%

<div align="center">

    <div class="box">
        <div class="sub-box">
            <div class="form-columns">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="filters.save.label.name"/></content>
                    <content tag="label.for">name</content>
                    <g:textField class="field" name="name" maxlength="30" value="${selected?.name}"/>
                </g:applyLayout>
            </div>
        </div>
    </div>

    <div class="box">
        <div class="sub-box">
            <div class="form-columns">
                <g:applyLayout name="form/select_multiple">
                    <content tag="label"><g:message code="filter.values.th.field"/></content>
                    <select id="customFields" name="customFields" class="field" multiple="true">
                        <g:isNotRoot>
                            <% list.removeAll() { it?.field?.trim().equals("u.company.description") } %>
                        </g:isNotRoot>
                        <g:each in="${list}">
                            <option value="${it.field}">
                                <g:message code="filters.${it.field}.title"/>
                            </option>
                        </g:each>
                    </select>
                </g:applyLayout>
            </div>
        </div>
    </div>

    <div>
        <g:if test="${typeName.equals("customer") || typeName.equals("customer_enrollment")}">
            <div class="sub-box ait_fields" style="display: none">
                <div class="form-columns">
                    <g:render template="${typeName}/customAit" model="[excludeList: excludeList]"/>
                </div>
            </div>
        </g:if>

        <div class="sub-box custom_fields" style="display: none">
            <div class="form-columns">

                <g:render template="${typeName}/custom" model="[excludeList: excludeList]"/>
            </div>
        </div>
    </div>

    <div style="clear: both">
    </div>

</div>

<script>
    $(function () {
        $(document).on("change", "#customFields", function () {
            var aitSelected = false;
            var customSelected = false;

            $("#customFields option:selected").each(function () {
                var $this = $(this);
                if ($this.val() == "accountTypeFields") {
                    aitSelected = true;
                    return
                }
                if ($this.val() == "contact.fields") {
                    customSelected = true;
                    return
                }
            });

            if (aitSelected) {
                $(".ait_fields").show();
            } else {
                $(".ait_fields option:selected").prop("selected", false);
                $(".ait_fields").hide();
            }

            if (customSelected) {
                $(".custom_fields").show();
            } else {
                $(".custom_fields option:selected").prop("selected", false);
                $(".custom_fields").hide();
            }
        })
    });
</script>
