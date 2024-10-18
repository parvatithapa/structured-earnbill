	                        <g:applyLayout name="form/checkbox">
	                            <content tag="label"><g:message code="billing.skip.emails"/></content>
	                            <content tag="label.for">skipEmails</content>
	                            <content tag="style">inp4</content>
	                            <g:checkBox class="cb checkbox" name="skipEmails" id ="skipEmails"
                                        checked="${configuration?.skipEmails > 0}" onclick='toggleDivSE()' />
	                        </g:applyLayout>
                    <div class="row" id="skip_emails_div"  style="display:none">
	                        <g:applyLayout name="form/input">
	                            <content tag="label"><g:message code="billing.skip.emails.days"/></content>
	                            <content tag="label.for">skipEmailsDays</content>
	                            <content tag="style">inp4</content>
	                            <g:field type="string" class="field" id = "skipEmailsDays" name="skipEmailsDays" value="${configuration?.skipEmailsDays}" maxlength="255" size="6"/>
	                        </g:applyLayout>
                    </div>
    
<script>
var togglese = document.getElementById("skipEmails");
if(togglese.checked){
	toggleDivSE();
	}
function toggleDivSE(){		
		$("#skip_emails_div").toggle();
}

</script>