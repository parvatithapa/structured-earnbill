<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
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

<div class="table-box">
<table cellpadding="0" cellspacing="0">
    <thead>
        <tr>
            <th class="small"><g:message code="plugins.plugin.id"/></th>
    	    <th class="medium"><g:message code="plugins.plugin.type"/></th>
    	    <th class="tiny"><g:message code="plugins.plugin.order"/></th>
        </tr>
    </thead>

    <tbody>
		<g:each in="${plugins}" status="idx" var="dto">
		   <tr class="${selected == dto?.id ? 'active' : ''}">
		     <td>
		     <g:remoteLink action="show" id="${dto.id}" before="register(this);" 
                           onSuccess="render(data, next);" params="[template:'show']">
                    ${dto.getId()}
			 </g:remoteLink>
             </td>
		     <td>
		     <g:remoteLink action="show" id="${dto.id}" before="register(this);" 
                           onSuccess="render(data, next);" params="[template:'show']">
                 <strong>
		            ${StringEscapeUtils.escapeHtml(dto?.type?.getDescription(session['language_id'], "title"))}
		         </strong>
		         <em class="tiny">
		            ${StringEscapeUtils.escapeHtml(dto?.type?.getClassName())}
		         </em>
			 </g:remoteLink>
             </td>
		     <td>
		     <g:remoteLink action="show" id="${dto.id}" before="register(this);" 
                           onSuccess="render(data, next);" params="[template:'show']">
                  ${dto?.getProcessingOrder()}
			 </g:remoteLink>
             </td>
		   </tr>
        </g:each>
    </tbody>
</table>
</div>

<sec:ifAllGranted roles="CONFIGURATION_1902">
    <div class="btn-hold btn-box">
        <div class="row">
            <a href="${createLink(action: 'showForm')}" class="submit add button-primary"><span><g:message code="button.create"/></span></a>
        </div>
    </div>
</sec:ifAllGranted>
