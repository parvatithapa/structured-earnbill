%{--
  jBilling - The Enterprise Open Source Billing System
  Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

  This file is part of jbilling.

  jbilling is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  jbilling is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
  --}%

<%@ page import="com.sapienter.jbilling.common.CommonConstants;com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;com.sapienter.jbilling.server.metafields.db.MetaField" contentType="text/html;charset=UTF-8" %>

<g:set var="modelIndex" value="${0}"/>

<div id="paymentMethod">
<g:hiddenField name="modelIndex" value="${0}"/>
<g:hiddenField name="currentIndex" value="${0}"/>
<g:hiddenField name="accountTypeId" value="${accountTypeId}"/>

    <g:if test="${(paymentInstruments && paymentInstruments?.size()>0)}">
    	<g:each in="${paymentInstruments}" var="instrument">
		    <%
			def pmt = PaymentMethodTypeDTO.get(instrument.paymentMethodTypeId)
			def metaFields = null;
			
			if(pmt!=null){
				 metaFields =  pmt.metaFields?.sort { it.displayOrder }
			}
			%>
				
		    <div class="form-columns">
		        <g:if test="${modelIndex != 0}">
		        	<hr/>
		        </g:if>
		        <div class="column">
		        	<g:hiddenField name="paymentMethod_${modelIndex}.id" value="${instrument?.id}"/>
		        	  
		        	<!-- payment method types drop down -->
					<g:applyLayout name="form/select">
		 				<content tag="label"><g:message code="payment.method.type.name"/></content>
		    			<content tag="label.for">paymentMethod_${modelIndex}.paymentMethodTypeId</content>
		    			<content tag="include.script">true</content>

						<g:if test="${modelIndex == 0}">
							<g:select name="paymentMethod_${modelIndex}.paymentMethodTypeId"
									  from="${paymentMethods}"
									  noSelection="['': message(code: 'default.no.selection')]"
									  optionKey="id" optionValue="methodName"
									  value="${instrument?.paymentMethodTypeId}"
									  onChange="refreshPaymentInstrument(${modelIndex}, ${user?.id});"/>
						</g:if>
						<g:else test="${modelIndex != 0}">
							<g:select name="paymentMethod_${modelIndex}.paymentMethodTypeId"
									  from="${paymentMethods}"
									  optionKey="id" optionValue="methodName"
									  value="${instrument?.paymentMethodTypeId}"
									  onChange="refreshPaymentInstrument(${modelIndex}, ${user?.id});"/>
						</g:else>
        			</g:applyLayout>
        
					<g:applyLayout name="form/input">
                        <content tag="label"><g:message code="payment.method.processing.order"/></content>
                        <content tag="label.for">paymentMethod_${modelIndex}.processingOrder</content>
                        <g:textField class="field" name="paymentMethod_${modelIndex}.processingOrder" value="${instrument?.processingOrder}"/>
                    </g:applyLayout>
		        </div>

				<g:if test="${paymentInstruments?.size() > 1}">
					<a onclick="removePaymentInstrument(this, ${modelIndex});" class="plus-icon">&#xe000;</a>
				</g:if>
				
		        <div id="payment-method-fields-${modelIndex}" class="column">   			
					<g:render template="/metaFields/editMetaFields" model="[ availableFields: metaFields, fieldValues: instrument?.metaFields, user: user ]"/>
		        </div>
		        <g:hiddenField name="paymentMethod_${modelIndex}.paymentMethodId" value="${instrument?.paymentMethodId}"/>
		    </div>
		     <g:set var="modelIndex" value="${modelIndex + 1}"/>
	    </g:each>
	    <g:set var="modelIndex" value="${modelIndex - 1}"/>
    </g:if>
    
    <g:else>
    		<!-- If payment instruments are null -->
    		<%
				def pmt = PaymentMethodTypeDTO.get(paymentMethods.iterator().next().id)
				def metaFields = null;

				if(pmt!=null){
					metaFields =  pmt.metaFields?.sort { it.displayOrder }
				}
			%>
			
		    <div class="form-columns">
		        <div class="column">  
		        	<!-- payment method types drop down -->
					<g:applyLayout name="form/select">
		 				<content tag="label"><g:message code="payment.method.type.name"/></content>
		    			<content tag="label.for">paymentMethod_${modelIndex}.paymentMethodTypeId</content>
		    			<g:select name="paymentMethod_${modelIndex}.paymentMethodTypeId"
		              			from="${paymentMethods}"
								noSelection="['': message(code: 'default.no.selection')]"
		              			optionKey="id" optionValue="methodName"
                                onChange="refreshPaymentInstrument(${modelIndex}, ${user?.id});"/>
					</g:applyLayout>
					
					<g:applyLayout name="form/input">
                        <content tag="label"><g:message code="payment.method.processing.order"/></content>
                        <content tag="label.for">paymentMethod_${modelIndex}.processingOrder</content>
                        <g:textField class="field" name="paymentMethod_${modelIndex}.processingOrder"/>
                    </g:applyLayout>
		        </div>
		        
		        <div id="payment-method-fields-${modelIndex}" class="column">   			
					<g:render template="/metaFields/editMetaFields" model="[ availableFields: metaFields, fieldValues: null ]"/>
		        </div>
		    </div>
    </g:else>
    
    <!-- spacer -->
    <div>
        <br/>&nbsp;
    </div>

    <div id="confirm-dialog-delete" class="bg-lightbox" title="<g:message code="popup.confirm.title"/>" style="display:none;">
	    <!-- confirm dialog content body -->
	    <table>
	        <tbody><tr>
	            <td valign="top">
	                <img src="${resource(dir:'images', file:'icon34.gif')}" alt="confirm">
	            </td>
				<td class="col2">
					<p id="confirm-dialog-delete-msg">
						<g:message code="payment.instrument.delete.confirm"/>
					</p>
	            </td>
	        </tr></tbody>
	    </table>
	</div>
    <!-- controls -->
    <div id="payment-method-add" class="btn-row">
        <a class="submit add" onclick="addPaymentInstrument()"><span><g:message code="button.add.payment.instrument"/></span></a>
    </div>
    <script type="text/javascript">
    	$(document).ready(function() {
			modifyMetaFieldName();
			defaultPaymentMethod(${modelIndex})
        });
        
	    function modifyMetaFieldName() {
			$('#modelIndex').val(${modelIndex});
	    	for(var i=0; i<=${modelIndex};i++) {
				var inputEles = $("#payment-method-fields-"+i).find("input,select");
				// add instruction to find select and checkboxes too
				for(var j=0;j<inputEles.length;j++) {
					var elementName = inputEles[j].name
					inputEles[j].setAttribute("name",""+i+"_"+elementName);
					inputEles[j].setAttribute("id",""+i+"_"+elementName);

					if($(inputEles[j]).hasClass('dateobject')) {
						// At first destroy old date picker and then reinitialize it. In case of edit customer cheque.date datepicker was not working properly.
						$(inputEles[j]).datepicker("destroy");
						$(inputEles[j]).datepicker({dateFormat: "${message(code: 'datepicker.jquery.ui.format')}", showOn: "both", buttonImage: "${resource(dir:'images', file:'icon04.gif')}", buttonImageOnly: true});
					}
				}
	        }	
	    }

		function addPaymentInstrument() {
			$.ajax({	   
                type: 'POST',
                url: '${createLink(controller: 'customer', action: 'addPaymentInstrument')}',
                data: $('#paymentMethod *').serialize(),
           		success: function(data) {$("#payment-method-main").html(data)}
         	});
		}

	    function refreshPaymentInstrument(currentIndex, userId) {
	    	$('#currentIndex').val(currentIndex);
			if(!checkEmptySelection(currentIndex)) {
				$.ajax({
					type: 'POST',
					url: '${createLink(controller: 'customer', action: 'refreshPaymentInstrument')}',
					data: $('#paymentMethod *').serialize() + "&id=" + userId,
					success: function(data) {$("#payment-method-main").html(data)}
				});
			}
			defaultPaymentMethod(currentIndex);
		}
		function defaultPaymentMethod(currentIndex) {
			hideNoSelectionIfInstrumentAdded()
			if(checkEmptySelection(currentIndex)) {
				$('#payment-method-fields-'+currentIndex).hide();
			}else{
				$('#payment-method-fields-'+currentIndex).show();
			}
		}
		function checkEmptySelection(currentIndex){
			var currentIndex=${modelIndex};
			var select='paymentMethod_'+currentIndex+'.paymentMethodTypeId';
			if($('select[name="'+select+'"]').val() == ''){
				return true
			}else{
				return false
			}
		}
		function hideNoSelectionIfInstrumentAdded(){
			if($('select[name="paymentMethod_1.paymentMethodTypeId"]').val() != null){
				$('select[name="paymentMethod_0.paymentMethodTypeId"] option:contains(--)').remove();
				return true
			}
		}

	    function removePaymentInstrument(element, modelIndex) {
			$('#currentIndex').val(modelIndex);
			$('#confirm-dialog-delete').dialog('open');
        }

	    $(function() {
	        setTimeout(function() {
	            $('#confirm-dialog-delete.ui-dialog-content').remove();
	            $('#confirm-dialog-delete').dialog({
	                autoOpen: false,
	                height: 200,
	                width: 375,
	                modal: true,
	                buttons: {
	                    '<g:message code="prompt.yes"/>': function() {
	                        <%out.println(onYes)%>;
	                        $.ajax({
	                            type: 'POST',
	                            url: '${createLink(controller: 'customer', action: 'removePaymentInstrument')}',
	                            data: $('#paymentMethod *').serialize(),
	                            success: function(data) { $('#payment-method-main').html(data)}
	                        });
	                        $(this).dialog('close');
	                    },
	                    '<g:message code="prompt.no"/>': function() {
	                        <%out.println(onNo)%>;
	                        $(this).dialog('close');
	                    }
	                }
	            });
	        }, 100);
	    });

    </script>
</div>
