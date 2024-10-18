<tr id = ${hidden ? "commission_exceptions_template" : ''}>
    <td class="medium">
        <g:applyLayout name="form/text">
            <content tag="label.for">exception.itemId</content>
            <div class="inp-bg medium2">
                <input type="text" name="exception.itemId" class="field"
                    value="${commissionException?.itemId}" ${hidden ? 'id="exceptionIdTemplate" disabled="disabled"' : ''}/>
            </div>
        </g:applyLayout>
    </td>

    <td class="medium">
        <g:applyLayout name="form/date">
            <content tag="label.for">exception.startDate</content>
            <content tag="label.inp.class">medium2</content>
            <input type="text" name="exception.startDate" class="field"
                   value="${formatDate(date: commissionException?.startDate, formatName: 'datepicker.format')}" ${hidden ? 'id="exceptionStartDateTemplate" disabled="disabled"' : ''}/>
        </g:applyLayout>
    </td>

    <td class="medium">
        <g:applyLayout name="form/date">
            <content tag="label.for">exception.endDate</content>
            <content tag="label.inp.class">medium2</content>
            <input type="text" name="exception.endDate" class="field"
                   value="${formatDate(date: commissionException?.endDate, formatName: 'datepicker.format')}" ${hidden ? 'id="exceptionEndDateTemplate" disabled="disabled"' : ''}/>
        </g:applyLayout>
    </td>

    <td class="medium">
        <g:applyLayout name="form/text">
            <content tag="label.for">exception.percentage</content>
            <div class="inp-bg medium2">
                <input type="text" name="exception.percentage" class="field"
                   value="${formatNumber(number: commissionException?.percentageAsDecimal, formatName: 'money.format')}"
                        ${hidden ? 'id="exceptionPercentageTemplate" disabled="disabled"' : ''}/>
            </div>
        </g:applyLayout>
    </td>

    <td style="width: 20px;${idx == 0 ? 'display:none;' : ''}" class="addRemoveButton">
        <a class="removeButton plus-icon" href="#" onclick="removeCommissionException(this);
        return false;">
            &nbsp;&#xe000;
        </a>
    </td>

    <td class="addRemoveButton">
        <a class="addButton plus-icon" href="#" onclick="addCommissionException(this);
        return false;">
            &nbsp;&#xe026;
        </a>
    </td>
</tr>
