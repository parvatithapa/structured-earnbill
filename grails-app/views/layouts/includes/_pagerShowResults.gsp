<%@ page import="com.sapienter.jbilling.client.util.SortableCriteria" %>
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

<div style="display: none; visibility: hidden">
    <div id="page-size-tmp-${action?:'actn'}" ></div>
    <g:set var="extraParams" value="${extraParams?extraParams:[:]}"/>
    <g:each var="max" in="${steps}">
        <g:remoteLink    update = "page-size-tmp-${action?:'actn'}"
                      onSuccess = "moveResults_${action?:'actn'}();${success?:''};"
                         action = "${action ?: 'list'}"
                      elementId = "page-size-${action?:'actn'}-${max}"
                         params = "${sortableParams(params: [          partial: true,
                                                                           max: max,
                                                                            id: id,
                                                             contactFieldTypes: contactFieldTypes ?: null] + extraParams)}" >
            ${max}
        </g:remoteLink>
    </g:each>
</div>

<div class="select-holder select-holder_small"><span class="select-value"></span>
    <g:select        name = "page-size-${action?:'actn'}"
                     from = "${steps}"
                    value = "${params.max}"
                 onchange = "pageSizeChange_${action?:'actn'}(this);"
              optionValue = "${{it + " " + message(code:"pager.show.max.results")}}">
    </g:select>
</div>

<script type="text/javascript">
    function pageSizeChange_${action?:'actn'}(obj) {
        $('#page-size-${action?:'actn'}-'+obj.value).click();
    }

    function moveResults_${action?:'actn'} () {
        var targetId = '#column2';
        if(${updateOverride ? 'true' : 'false'}  && $('#${updateOverride}').size() > 0) {
            targetId = '#${updateOverride}';
        } else if($('#page-size-tmp-${action?:'actn'}').closest('#column1').size() > 0) {
            targetId = '#column1';
        } else if(${update ? 'true' : 'false'}  && $('#${update}').size() > 0) {
            targetId = '#${update}';
        }
        var targetEl = $(targetId);
        var newEl = $('#page-size-tmp-${action?:'actn'}').html();

        targetEl.empty();
        targetEl.html(newEl);

        if(targetId == '#column1') {
            $('#column2').html('');
        }
    }

    $(document).ready(function() {
        $("select[name='page-size-${action?:'actn'}']").each(function () {
            updateSelectLabel(this);
        });

        $("select[name='page-size-${action?:'actn'}']").change(function () {
            updateSelectLabel(this);
        });
    });
</script>
