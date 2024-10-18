<%--
  View template to render a formatted list of order line fields and meta-fields..

  @author Ashok Kale
--%>

<g:if test="${line.callIdentifier != null}">
	<tr style="width: 100%;">
		<td style="width: 40%;"><g:message code="${product.description}" />
			<g:message code="identifier" /></td>
		<td style="width: 60%;" class="value">
			${line.callIdentifier}
		</td>
	</tr>
</g:if>
<g:if test="${line.callCounter != null}">
	<tr style="width: 100%;">
		<td style="width: 40%;"><g:message
				code="order.label.line.callCounter" /></td>
		<td style="width: 60%;" class="value">
			${line.callCounter}
		</td>
	</tr>
</g:if>
<g:if test="${line?.metaFields}">
	<tr>
		<g:render template="/metaFields/metaFieldsWS" model="[metaFields: line?.metaFields]" />
	</tr>
</g:if>
