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

  include.script must only be set if select rendered as part of a partial page render like an AJAX call. When the entire
  page is rendered the correct handlers will be attached.

  Usage:

    <g:applyLayout name="form/select">
        <content tag="label">Field Label</content>
        <content tag="label.for">element_id</content>
        <content tag="label.title">Label Title</content>
        <content tag="label.class">Label Class</content>
        <select name="name" id="element_id">
            <option value="1">Option 1</option>
            <option value="2">Option 2</option>
        </select>
    </g:applyLayout>


  @author Brian Cowdery
  @since  25-11-2010
--%>

<div class="row<%=pageProperty(name: 'page.label.row.class') ? ' '+pageProperty(name: 'page.label.row.class') : ''%>">
    <label class="<g:pageProperty name="page.label.class"/>" title="<g:pageProperty name="page.label.title"/>" for="<g:pageProperty name="page.label.for"/>" data-cy="<g:pageProperty name="page.label.for"/>"><g:pageProperty name="page.label"/></label>
    <div class="select-holder <g:pageProperty name="page.holder.class"/>"><span class="select-value" data-cy="currentOption"></span>
    <g:layoutBody/>
    </div>
    <g:pageProperty name="page.icon"/>

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
