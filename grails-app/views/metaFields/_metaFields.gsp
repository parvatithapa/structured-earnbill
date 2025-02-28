<%@ page import="com.sapienter.jbilling.server.metafields.DataType; com.sapienter.jbilling.common.Constants; com.sapienter.jbilling.server.metafields.MetaFieldType" %>
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

<%--
  View template to render a formatted list of meta-fields to be printed as part of a data table..
  
<g:render template="/metaFields/metaFields" model="[ metaFields: object.metaFields ]"/>
  @author Brian Cowdery
  @since 25-Oct-2011
--%>

<g:each var="metaField" in="${metaFields?.sort{ it.field.displayOrder }}">
    <g:if test="${!metaField.field.disabled}">
        <g:set var="fieldValue" value="${metaField.getValue()}"/>

        <tr>
            <td>
                <g:message code="${metaField.field.name}"/>
            </td>
            <td class="value wide-width">
               <g:render template="/metaFields/formatMetaFieldValue" model="[fieldName: metaField.field.name, fieldType:metaField?.field?.fieldUsage, dataType: metaField?.field?.dataType, fieldValue:fieldValue, instrument:instrument]"/>
            </td>
        </tr>
    </g:if>

</g:each>
