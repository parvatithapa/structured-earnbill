<div class="form-columns">

    <h3><g:message
            code="customer.enrollment.edit.notes.agents.title"/></h3>
    <hr/>

    <div class="column" >
        <input type="hidden" id="partner-name-id"  name="partner-name-id" />
        <g:applyLayout name="form/input">
            <content tag="label"><g:message code="customer.bulk.enrollment.edit.partner"/></content>
            <content tag="label.for">partner-name</content>
            <g:textField id="partner-name" class="field" name="partner-name" />
        </g:applyLayout>
        <g:applyLayout name="form/input">
            <content tag="label"><g:message code="customer.bulk.enrollment.edit.rate"/></content>
            <content tag="label.for">partner-rate</content>
            <g:field id="partner-rate" class="field" name="partner-rate" type="number"/>
        </g:applyLayout>
        <div class="row">
            <label>&nbsp;</label>
            <div class="inp-desc">
                <a onclick="addCommission()" class="submit">
                    <span><g:message code="customer.bulk.enrollment.edit.partner.add"/></span>
                </a>
            </div>
        </div>
        <div>&nbsp;</div>
        <div class="table-box">
            <table class="innerTable">
                <thead class="innerHeader">
                <tr class="ui-widget-header">
                    <td/><td><g:message code="customer.bulk.enrollment.edit.partner"/></td>
                    <td><g:message code="customer.bulk.enrollment.edit.rate"/></td>
                    <td/>
                </tr>
                </thead>
                <tbody id="commission-lines">
                    <g:each in="${customerEnrollment?.customerEnrollmentAgents}" var="agent" status="idx">
                        <tr>
                            <td><input type="hidden" name="comm.partner.val${idx}" value="${agent.partnerId}" />
                                <input type="hidden" name="comm.broker.val${idx}" value="${agent.brokerId}" />
                                <input type="hidden" name="comm.partnerName.val${idx}" value="${agent.partnerName}" />
                                <input type="hidden" name="comm.rate.val${idx}" value="${agent.rate}" />
                                <g:link controller="partner" action="show" id="${agent.partnerId}">${agent.partnerId}</g:link></td>
                            <td>${agent.partnerName}</td>
                            <td>${agent.rate ? formatNumber(number: agent.rate, formatName: 'decimal.format') : ''}</td>
                            <td><a class="plus-icon" onclick="removeCommission(this);">&#xe000;</a>
                            </td>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </div>
        <div>&nbsp;</div>
    </div>

    <div class="column">
        <div class="box-text">
            <label class="lb"><g:message code="enrollment.edit.comment"/></label>
            <g:textArea name="comment" rows="5" cols="60" >${comment?:""}</g:textArea>
        </div>
    </div>

    %{-- Template used to add a new commission line --}%
    <div style="display: none;">
        <table>
            <tbody id="partner-comm-template">
            <tr>
                <td><input type="hidden" name="_prefix_comm.partner.val_idx_" value="_id_" />
                    <input type="hidden" name="_prefix_comm.broker.val_idx_" value="_partner_" />
                    <input type="hidden" name="_prefix_comm.partnerName.val_idx_" value="_partner_" />
                    <input type="hidden" name="_prefix_comm.rate.val_idx_" value="_rate_" />
                    <g:link controller="partner" action="show" id="_id_">_id_</g:link></td>
                <td>_partner_</td>
                <td>_rate_</td>
                <td><a class="plus-icon" onclick="removeCommission(this);">&#xe000;</a>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
</div>

<r:script>
var commissionIdx = 1;

$( "#partner-name" ).autocomplete({
  source: "<g:createLink controller="customer" action='findPartners'/>" ,
  minLength: 2,
  select: function( event, ui ) {
    //alert('id='+ui.item.label+', val='+ui.item.value)  ;
    $("#partner-name-id").val(ui.item.value);
    $("#partner-name").val(ui.item.label);
    return false;
  }
});

function addCommission() {
    if($("#partner-name-id").val()) {
        commissionIdx++;
        var template = $("#partner-comm-template").clone().html()
            .replace(/_idx_/g, commissionIdx)
            .replace(/_id_/g, $("#partner-name-id").val())
            .replace(/_partner_/g, $("#partner-name").val())
            .replace(/_prefix_/g, "")
            .replace(/_rate_/g, $("#partner-rate").val());
        $("#commission-lines").append(template);
        $("#partner-name-id").val("");
        $("#partner-name").val("");
        $("#partner-rate").val("");
    }
}

function removeCommission(obj) {
    $(obj).closest("tr").remove();
}

</r:script>





