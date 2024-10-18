	                        <g:applyLayout name="form/checkbox">
	                            <content tag="label"><g:message code="billing.auto.payment"/></content>
	                            <content tag="label.for">autoPayment</content>
	                            <g:checkBox class="cb checkbox" name="autoPayment" id="autoPayment"
									checked="${configuration?.autoPayment > 0}" onclick='toggleDiv()' value="${configuration?.autoPayment}"/>
	                        </g:applyLayout>
	                     <div class="row"  id="auto_payment_div"  style="display:none">  
	                        <g:applyLayout name="form/input">
	                            <content tag="label"><g:message code="billing.auto.retrycount"/></content>
	                            <content tag="label.for">retryCount</content>
	                            <content tag="style">inp4</content>
	                            <g:field type="number" class="field" name="retryCount" value="${configuration?.retryCount}"/>
	                        </g:applyLayout>
	                     </div> 
    
<script>
var toggle = document.getElementById("autoPayment");
if(toggle.checked){
	toggleDiv();
	}
function toggleDiv(){
	
		$("#auto_payment_div").toggle();
}

</script>