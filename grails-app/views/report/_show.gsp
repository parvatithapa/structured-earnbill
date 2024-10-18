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

<%@ page import="com.sapienter.jbilling.server.report.ReportExportFormat;com.sapienter.jbilling.server.user.db.CompanyDTO"%>
<%--
  Report details template.

  @author Brian Cowdery
  @since  07-Mar-2011
--%>

<%
	def company = CompanyDTO.get(session['company_id'])
	def childEntities = CompanyDTO.findAllByParent(company)
%>

<div id="success-message" class="msg-box successfully" style="display: none;">
    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="${message(code:'info.icon.alt',default:'Information')}"/>
     <strong><g:message code="flash.request.sent.title"/></strong>
     <p><g:message code="report.mediation.call.records.file.generate"/></p>
 </div>

<script type="text/javascript" charset="utf-8">
    $("#selectAll").click(function () {
        $('#childs option').prop('selected', this.checked);
        $('#childs').trigger('change');
        $('#childs').prop('readonly', this.checked);
    });
</script>

<div class="column-hold">
    <div class="heading">
        <strong><g:message code="${selected.name}"/></strong>
    </div>
    <g:form name="run-report-form" url="[action: 'run', id: selected.id]" target="_blank" method="GET">
        <div class="box">
            <div class="sub-box">
              <!-- report info -->
              <table class="dataTable" cellspacing="0" cellpadding="0">
                  <tbody>
                  <tr>
                      <td><g:message code="report.label.id"/></td>
                      <td class="value">${selected.id}</td>
                  </tr>
                  <tr>
                      <td><g:message code="report.label.type"/></td>
                      <td class="value">${selected.type.getDescription(session['language_id'])}</td>
                  </tr>
                  <tr>
                      <td><g:message code="report.label.design"/></td>
                      <td class="value">
                          <em>${selected.fileName}</em>
                      </td>
                  </tr>
                  </tbody>
              </table>

              <!-- report description -->
              <p class="description">
                  ${selected.getDescription(session['language_id'])}
              </p>

              <hr/>

              <g:hiddenField id="valid" name="valid" value="" />
              <!-- report parameters -->
              <g:render template="/report/${selected.type.name}/${selected.name}" model="[childEntities:childEntities, company: company]"/>

  			  <g:if test="${childEntities?.size() > 0 && company?.parent == null && (selected?.name != 'monthly_termination_costs' && selected?.name != "platform_net_revenue")}">
  			  <hr/>
  			  <div class="form-columns">
                 <g:applyLayout name="form/select_multiple">
                     <content tag="label"><g:message code="report.label.child.company"/></content>
                     <content tag="label.for">childs</content>
                     <g:select id="childs" multiple="true" name="childs" from="${childEntities}" optionKey="id" optionValue="${{it?.description}}" />
                 </g:applyLayout>
                  <g:applyLayout name="form/checkbox">
                      <content tag="label">Select All</content>
                      <content tag="label.for">selectAll</content>
                      <g:checkBox name="selectAll" class="cb check"/>
                  </g:applyLayout>
              </div>
              </g:if>

              <br/>&nbsp;
            </div>
        </div>
        <g:if test="${selected?.name == 'monthly_termination_costs'}">
        <div class="btn-box" id="generateCsv" >
        <a class="submit edit button-primary" onclick="generateBackgroundReport() ">
                <span><g:message code="button.run.mediation.report"/></span>
            </a>
        </div>
        </g:if>

        <g:else>
        <div class="btn-box">
            <a class="submit edit button-primary" onclick="submitForm()">
                <span><g:message code="button.run.report"/></span>
            </a>
            <g:if test="${selected?.name == 'gstr1_json'}">
            </g:if>
            <g:else>
            <span>
                <g:applyLayout name="form/select_holder">
                    <content tag="label.for">format</content>
                    <content tag="include.script">true</content>
                    <content tag="holder.class">select-holder-nofloat</content>
                    <g:select name="format"
                          from="${ReportExportFormat.values()}"
                          noSelection="['': message(code: 'report.format.HTML')]"
                          valueMessagePrefix="report.format"/>
                </g:applyLayout>
            </span>
            </g:else>
            </div>
            </g:else>

    </g:form>
</div>

<script type="text/javascript">
	
	var selectedReportName = '${selected.name}';
    var message = "${message(code: 'invalid.date.format')}"
	
    $(setTimeout(
        function() {
            var validator = $('#run-report-form').validate();
            validator.init();
            validator.hideErrors();
        }, 500)
    );

    function submitForm(){
    	if (selectedReportName == 'total_invoiced' ||
    		selectedReportName == 'total_invoiced_per_customer' ||
    		selectedReportName == 'top_customers' ||
    		selectedReportName == 'user_signups' ||
    		selectedReportName == 'mediation_records' ||
    		selectedReportName == 'total_payments') {
    		if (!validateDate($("#start_date"))) return false;
    		if (!validateDate($("#end_date"))) return false;  
    	}

        if($('#valid').val()=="false"){
            $("#error-messages ul li").html(message);
            $("#error-messages").show();
        } else{
            checkData();
        }
    };

    function checkData() {
        var valid = true;
        if(selectedReportName != 'gstr1_json'){
        jQuery.ajax({
            type: 'POST',
            async: false,
            url: '${createLink(action: 'checkData')}',
            data: $('#run-report-form').serialize()+"&id="+${selected.id},
            success: function(data) {
                var jsonData = JSON.parse(data);

                if(jsonData.error){
                    $("#error-messages ul").html(jsonData.error);
                    $("#error-messages").show();
                    $("#error-messages ul").show();

                    valid = false;
                }
            }
        });

        if (valid) {
            $('#run-report-form').submit();
        }
       }else {
              jQuery.ajax({
                  type: 'POST',
                  async: false,
                  url: '${createLink(action: 'run')}',
                  data: $('#run-report-form').serialize()+"&id="+${selected.id},
                  success: function(response) {
                    if (String(response).indexOf("error") !== -1){
                     var jsonData = JSON.parse(response);
                       if(jsonData.error){
                          $("#error-messages ul").html(jsonData.error);
                          $("#error-messages").show();
                          $("#error-messages ul").show();

                       }
                     }else{
                        var fileName = "returns_" + $("#start_date").val() + "-" + $("#end_date").val() + "_GST-R1_offline_report.json"
                        var jsonString = JSON.stringify(response, null, 2);
                        var blob = new Blob([jsonString], { type: 'application/json' });

                               // Create a link element
                               var a = document.createElement('a');
                               a.href = window.URL.createObjectURL(blob);
                               a.download = fileName;

                               // Append the link to the body and trigger a click event to start the download
                               document.body.appendChild(a);
                               a.click();

                               // Remove the link element from the body
                               document.body.removeChild(a);

                     }// else close
                  }
           });


       }
    }

	function showMessage() {
		$("#success-message").css("display","block");
	}

	function clearMessage(){
		$("#success-message").css("display","none");
	}

	function generateBackgroundReport() {
		$.ajax({
			type: 'POST',
	        url: '${createLink(action: 'runExports', params:params)}',
	        data: $('#generateCsv').parents('form').serialize(),
	        success: function(response) {
				if(!response.includes("failure")){
					showMessage();
				} else {
					clearMessage();
				}
            },
	        error: function(data) {}
	    });
	}

	function toggleCdrType(cdrvalue){
		clearMessage();
		if(document.getElementById('export_format').value == 'CSV'){
			$("#cdrtype_div").css("display","block");
		} else {
			$("#cdrtype_div").css("display","none");
		}
	}
 
</script>
