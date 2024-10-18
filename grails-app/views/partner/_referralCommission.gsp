<tr id = ${hidden ? "referrer_commissions_template" : ''}>
    <td class="medium">
        <g:applyLayout name="form/text">
            <content tag="label.for">referrer.referralId</content>
            <div class="inp-bg medium2">
                <input type="text" name="referrer.referralId" class="field"
                       value="${referrerCommission?.referralId}" ${hidden ? 'id="referrerIdTemplate" disabled="disabled"' : ''}/>
            </div>
        </g:applyLayout>
    </td>

    <td class="medium">
        <g:applyLayout name="form/date">
            <content tag="label.for">referrer.startDate</content>
            <content tag="label.inp.class">medium2</content>
            <input type="text" name="referrer.startDate" class="field"
                   value="${formatDate(date: referrerCommission?.startDate, formatName: 'datepicker.format')}" ${hidden ? 'id="referrerStartDateTemplate" disabled="disabled"' : ''}/>
        </g:applyLayout>
    </td>

    <td class="medium">
        <g:applyLayout name="form/date">
            <content tag="label.for">referrer.endDate</content>
            <content tag="label.inp.class">medium2</content>
            <input type="text" name="referrer.endDate"  class="field"
                   value="${formatDate(date: referrerCommission?.endDate, formatName: 'datepicker.format')}" ${hidden ? 'id="referrerEndDateTemplate" disabled="disabled"' : ''}/>
        </g:applyLayout>
    </td>

    <td class="medium">
        <g:applyLayout name="form/text">
            <content tag="label.for">referrer.percentage</content>
            <div class="inp-bg medium2">
                <input type="text" name="referrer.percentage" class="field"
                   value="${formatNumber(number: referrerCommission?.percentageAsDecimal, formatName: 'money.format')}" ${hidden ? 'id="referrerPercentageDateTemplate" disabled="disabled"' : ''}/>
            </div>
        </g:applyLayout>
    </td>

    <td style="width: 20px;${idx == 0 ? 'display:none;' : ''}" class="addRemoveButton">
        <a class="removeReferralButton plus-icon" href="#" onclick="removeReferralCommission(this);
        return false;">
            &nbsp;&#xe000;
        </a>
    </td>

    <td class="addRemoveButton">
        <a class="addReferralButton plus-icon" href="#" onclick="addReferralCommission(this);
        return false;">
            &nbsp;&#xe026;
        </a>
    </td>
</tr>
