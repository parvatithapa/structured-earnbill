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

<%@ page import="com.sapienter.jbilling.server.discount.db.DiscountDTO; com.sapienter.jbilling.server.user.db.CompanyDTO;" %>

<%--
  Allows user to define discounts at order or line level.
  
  @author Amol Gadre
  @since  30-Nov-2012
--%>

<g:set var="discountLineIndex" value="${0}"/>
<g:set var="dbDiscountList" value="${
		                			DiscountDTO.createCriteria().list(){
		                				and{
	            							eq('entity', new CompanyDTO(session['company_id']))
	            							or {
	            								ge('endDate', order.activeSince)
												isNull('endDate')
	            							}
            							}
		                			}
		                		}" />
<g:set var="attributes" value="${model?.attributes ? new TreeMap<String, String>(model.attributes) : new TreeMap<String, String>()}"/>

<div id="discount-messages" class="msg-box error" style="display: none;">
    <ul></ul>
</div>

<g:set var="countIndex" value="${1}" />
<g:set var="isNew" value="${!(order?.id && order?.id>0)}" />
<div id="discount-box">
	<g:formRemote name="discount-lines-form" url="[action: 'edit']" update="ui-tabs-discounts" method="GET" onSuccess="removeSelectedDiscount()">
		<g:hiddenField name="_eventId" value="addRemoveDiscountLine" />
		<g:hiddenField name="execution" value="${flowExecutionKey}"/>
		<g:hiddenField name="discountLineWhatToDo" value="" />
	
		<div class="form-columns">
			<br/>
	    	<div>
	        	<div class="tab-column" style="text-align:center;font-weight:bold;">
	                <g:message code="discountable.item.order"/>
	            </div>
	            <div class="tab-column" style="text-align:center;font-weight:bold;">
	                <g:message code="discount.label"/>
				</div>
			</div>
	    	<br/>
    		
	        <g:each var="discountLine" in="${order.discountLines}" status="counter">
				<div id="custom-div">
					<div class="tab-column">
					<g:applyLayout name="form/select">
						<content tag="label.for">discountableItem.${discountLineIndex}.lineLevelDetails</content>
						<content tag="include.script">true</content>
						<content tag="holder.class">select-holder-nofloat</content>
						<g:select 	    style = "width:250px;"
								  		 name = "discountableItem.${discountLineIndex}.lineLevelDetails"
								  		 from = "${discountableItems}"
								  noSelection = "['': message(code: 'discountableItem.option.empty')]"
								    optionKey = "lineLevelDetails" optionValue="description"
								        value = "${discountLine.lineLevelDetails}"
									 disabled = "true"/>
						</g:applyLayout>
					</div>
					<div class="tab-column">
						<g:applyLayout name="form/select">
							<content tag="label.for">discount.${discountLineIndex}.id</content>
							<content tag="include.script">true</content>
							<content tag="holder.class">select-holder-nofloat</content>
							<g:select 		style = "width:250px;"
											 name = "discount.${discountLineIndex}.id"
											 from = "${dbDiscountList}"
									  noSelection = "['': message(code: 'discount.option.empty')]"
									    optionKey = "id"
									  optionValue = "discountCodeAndDescription"
											value = "${discountLine.discountId}"
										 disabled = "true"/>
						</g:applyLayout>
					</div>
					<g:if test="${isNew}">
						<a class="plus-icon order-disc-icon" onclick="removeDiscountLine(${discountLineIndex})">&#xe000;</a>
					</g:if>
					<g:set var="countIndex" value="${countIndex+1}" />
					<g:set var="discountLineIndex" value="${discountLineIndex + 1}"/>

				</div>
				<br/>
	        </g:each>
	       
			<g:if test="${isNew}">
				<div id="custom-div">
				<!-- one empty row -->
				<div class="tab-column">
					<g:applyLayout name="form/select">
						<content tag="label.for">discountableItem.${discountLineIndex}.lineLevelDetails</content>
						<content tag="include.script">true</content>
						<content tag="holder.class">select-holder-nofloat</content>
						<g:select 	    style = "width:250px;"
								  		 name = "discountableItem.${discountLineIndex}.lineLevelDetails"
								  		 from = "${discountableItems}"
								  noSelection = "['': message(code: 'discountableItem.option.empty')]"
								    optionKey = "lineLevelDetails"
								  optionValue = "description"
									    value = ""
									 onchange = "focusNext(${discountLineIndex})"/>
					</g:applyLayout>
					</div>
					<div class="tab-column">
						<g:applyLayout name="form/select">
							<content tag="label.for">discount.${discountLineIndex}.id</content>
							<content tag="include.script">true</content>
							<content tag="holder.class">select-holder-nofloat</content>
							<g:select      style = "width:250px;"
										    name = "discount.${discountLineIndex}.id"
										    from = "${dbDiscountList}"
									 noSelection = "['': message(code: 'discount.option.empty')]"
									   optionKey = "id"
								     optionValue = "discountCodeAndDescription"
									       value = ""
									    onchange = "focusNext(${discountLineIndex})"/>
						</g:applyLayout>
					</div>
					<a class="plus-icon order-disc-icon" onclick="addDiscountLine(${discountLineIndex})">&#xe026;</a>
					<g:hiddenField name="discountLineIndex" value="${discountLineIndex}" />
			  	</div>
			</g:if>
			<br/>
		</div>
	</g:formRemote>
	<script type="text/javascript">
		function focusNext(discountLineIndex) {
			var retVal= addDiscountLine(discountLineIndex);
			return retVal;
		}
		
	    function addDiscountLine(discountLineIndex) {

	    	var discountableItem = $('#discountableItem\\.' + discountLineIndex + '\\.lineLevelDetails').val();
	    	var discount = $('#discount\\.' + discountLineIndex + '\\.id').val();

	    	if(discountableItem == '' || discount == '') {
                $("#discount-messages").css("display","block");
                $("#discount-messages ul").css("display","block");
                $("#discount-messages ul").html("<li><g:message code="validation.error.discountLine.blank"/></li>");

                if( discountableItem == '' ) {
                	document.getElementById('discountableItem.' + discountLineIndex + '.lineLevelDetails').focus();
                } else if( discount == '' ) {
                	document.getElementById('discount.' + discountLineIndex + '.id').focus();
                }
                
                return false;
            }

	        $('#discountLineWhatToDo').val('addDiscountLine');
	        $('#discount-lines-form').submit();
	    }
	    
	    function removeDiscountLine(discountLineIndex) {
	    	$('#discountLineWhatToDo').val('removeDiscountLine');
	    	$('#discountLineIndex').val(discountLineIndex);
	    	$('#discount-lines-form').submit();
	    }

		function removeSelectedDiscount() {
			$('select:disabled[name^="discount."]').each(function() {
				$('select:enabled[name^="discount."]').find('option[value="'+$(this).val()+'"]').remove()
			})
		}
	</script>
</div>
