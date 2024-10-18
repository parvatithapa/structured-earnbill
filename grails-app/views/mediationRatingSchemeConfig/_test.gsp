<div class="box">
    <div class="box-cards-title">
        <span><g:message code="ratingScheme.test.title"/></span>
    </div>
    <div class="sub-box">
        <table class="dataTable" cellspacing="0" cellpadding="3 ">
            <tbody>
                <tr>
                    <td style="width: 15%"><g:message code="ratingScheme.call.duration"/></td>
                    <td style="width: 10px">=</td>
                    <td style="width: 15%"><g:textField style="width: 80%" name="callDuration" value="${callDuration}"/></td>
                    <td style="width: 20%"><g:message code="ratingScheme.result.quantity"/></td>
                    <td style="width: 10px">=</td>
                    <td style="width: 15%">${resultQuantity}</td>
                    <td style="width: 35%">
                        <a id="column-550-a" class="submit" href="#" onclick="callTestRatingScheme(); return false;">
                            <g:message code="ratingScheme.test.btn"/>
                        </a>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
