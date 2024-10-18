<div id="subscriptionTemplate">
	<g:applyLayout name="form/select">
		<content tag="label"><g:message code="prompt.main.subscription.value"/></content>
		<content tag="label.for">mainSubscription.nextInvoiceDayOfPeriod</content>
            <g:select id="mainSubscription" from="${mainSubscription?.semiMonthlyDaysMap?.entrySet()}"
               optionKey="key" optionValue="value"
               name="mainSubscription.nextInvoiceDayOfPeriod"
               value="${mainSubscription?.nextInvoiceDayOfPeriod}"/>
	</g:applyLayout>

    <script type="text/javascript" xmlns="http://www.w3.org/1999/html">
        $(document).ready(function() {
            console.log('semiMonthly');
            $('#mainSubscription').each(function () {
                updateSelectLabel(this);
            });

            $('#mainSubscription').change(function () {
                updateSelectLabel(this);
            });
        });
    </script>

</div>
