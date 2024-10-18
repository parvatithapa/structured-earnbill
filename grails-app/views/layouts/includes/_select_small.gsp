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
  Layout for labeled and styled select (drop-down box) elements.

  Usage:

    <g:applyLayout name="include/select_small">
        <select name="name" id="element_id">
            <option value="1">Option 1</option>
            <option value="2">Option 2</option>
        </select>
    </g:applyLayout>

--%>

<div class="select-holder select-holder_small"><span class="select-value"></span>
    <g:select name="${select_name}"
              value="${select_value}"
              from="${select_from}"
              keys="${select_keys}"
              valueMessagePrefix="${select_valueMessagePrefix}"
              optionKey="${select_optionKey}" optionValue="${select_optionValue}"
              noSelection="${select_noSelection}" />
</div>
