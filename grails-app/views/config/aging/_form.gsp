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

<%@page import="com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO" %>

<div style="width:900px;">
    <div id="tabs">
		  <ul>
		    <li><a href="#regularTab"><g:message code="config.ageing.title.collections.regular"/></a></li>
		    <li><a href="#cancelledTab"><g:message code="config.ageing.title.collections.cancelled.accounts"/></a></li>
		  </ul>
		  <div id="regularTab">
		    <g:form name="save-aging-form" action="saveAging" useToken="true">
        		<g:render template="/config/aging/steps" model="[ageingSteps:ageingSteps]"/>
    		</g:form>
		  </div>
		  <div id="cancelledTab">
		    <g:form name="save-cancel-aging-form" action="saveAging" useToken="true">
        		<g:render template="/config/aging/cancelledUserSteps" model="[cancelledAgeingSteps:cancelledAgeingSteps]"/>
        	</g:form>
		  </div>
	</div>
</div>
<div style="width:900px;">
    <g:settingEnabled property="collections.run.ui">
        <g:form name="run-collections-form" action="runCollectionsForDate" useToken="true">
            <g:render template="/config/aging/run"/>
        </g:form>
    </g:settingEnabled>
     
</div>
