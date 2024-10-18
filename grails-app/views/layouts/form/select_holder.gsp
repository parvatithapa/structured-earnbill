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

    <g:applyLayout name="include/select_holder">
        <select name="name" id="element_id">
            <option value="1">Option 1</option>
            <option value="2">Option 2</option>
        </select>
    </g:applyLayout>

--%>

<div class="select-holder <g:pageProperty name="page.holder.class"/>"><span class="select-value"></span>
    <g:layoutBody/>

    <g:ifPageProperty name="page.include.script">
        <script type="text/javascript" xmlns="http://www.w3.org/1999/html">
            $(document).ready(function() {
                $("select[name='<g:pageProperty name="page.label.for"/>']").each(function () {
                    updateSelectLabel(this);
                });

                $("select[name='<g:pageProperty name="page.label.for"/>']").change(function () {
                    updateSelectLabel(this);
                });
            });
        </script>
    </g:ifPageProperty>
</div>

