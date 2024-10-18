<div id="subscriptionTemplate">
 <table style="width: 100%; vertical-align: top; margin: 0px; padding: 0px;">
    <tr>
        <td style="width: 100%; vertical-align: top; margin: 0px; padding: 0px;">
            <div class="row">
                <label class="" title="" for="mainSubscription.nextInvoiceDayOfPeriod"><g:message code="prompt.main.subscription.value"/></label>
                <div class="select-holder"><span class="select-value"></span>
                    <g:select id="mainSubscription" from="${mainSubscription?.yearMonthsMap?.entrySet()}"
                              optionKey="key" optionValue="value"
                              name="mainSubscription.nextInvoiceDayOfPeriod"
                              value="${mainSubscription?.nextInvoiceDayOfPeriod}"/>
                </div>

                <div class="select-holder"><span class="select-value"></span>
                    <g:select id="mainSubscription" from="${mainSubscription?.yearMonthDays}"
                              name="mainSubscription.nextInvoiceDayOfPeriodOfYear"
                              value="${mainSubscription?.nextInvoiceDayOfPeriodOfYear}"/>
                </div>
            </div>
    	</td>
    </tr>
</table>

    <script type="text/javascript" xmlns="http://www.w3.org/1999/html">
        $(document).ready(function() {
            console.log('yearly');
            $("select[name='mainSubscription.nextInvoiceDayOfPeriod']").each(function () {
                updateSelectLabel(this);
            });

            $("select[name='mainSubscription.nextInvoiceDayOfPeriod']").change(function () {
                updateSelectLabel(this);
            });

            $("select[name='mainSubscription.nextInvoiceDayOfPeriodOfYear']").each(function () {
                updateSelectLabel(this);
            });

            $("select[name='mainSubscription.nextInvoiceDayOfPeriodOfYear']").change(function () {
                updateSelectLabel(this);
            });
        });
    </script>
</div>
