%{-- JBILLING CONFIDENTIAL _____________________ [2003] - [2012]
Enterprise jBilling Software Ltd. All Rights Reserved. NOTICE: All
information contained herein is, and remains the property of Enterprise
jBilling Software. The intellectual and technical concepts contained
herein are proprietary to Enterprise jBilling Software and are protected
by trade secret or copyright law. Dissemination of this information or
reproduction of this material is strictly forbidden. --}%
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper"%>

<%--
  @author Amol Saware
  @since  22-Aug-2019
--%>

<div class="form-columns">
	<g:applyLayout name="form/select">
		<content tag="label">
		<g:message code="Instrument Type" /></content>
		<content tag="label.for">instrument_type</content>
		<content tag="include.script">true</content>
		<g:select name="instrument_type" from="['Both', 'Credit Cards', 'Bank Debits']"
			valueMessagePrefix="instrument_type" />
	</g:applyLayout>
</div>