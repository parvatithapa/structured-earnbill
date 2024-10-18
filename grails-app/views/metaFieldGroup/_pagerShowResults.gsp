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

%{--In metafields group we need to send additional params to action to identify the type of metafields group and
 response was missing when we use generic pagerShowResultTemplate so added showResults function to render response.
--}%

<div style="display: none; visibility: hidden">
    <g:set var="extraParams" value="${extraParams?extraParams:[:]}"/>
    <g:each var="max" in="${steps}">
        <g:remoteLink elementId="page-size-mg-${max}" action="${action ?: 'list'}" id="${entityType}"  params="${sortableParams(params: [template:'list',partial: true, max: max,id:id, contactFieldTypes: contactFieldTypes ?: null])+extraParams}" update="${update ?:'#column2'}">${max}</g:remoteLink>
    </g:each>
</div>

<g:select name="page-size" from="${steps}" value="${params.max}" class="pager-button" onchange="pageSizeChangeMg(this);" optionValue="${{it + " " + message(code:"pager.show.max.results")}}">
</g:select>

<script type="text/javascript">
    function pageSizeChangeMg(obj) {
        $('#page-size-mg-'+obj.value).click();
    }
</script>
