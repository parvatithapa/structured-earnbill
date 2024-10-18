%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2013] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<g:each var="metaField" in="${model?.metaFields.sort { it.displayOrder }}" status="idx">

    <tr>
        <td><g:message code="metaField.label.name"/></td>
        <td class="value">${metaField.name}</td>
        <td><g:message code="metaField.label.dataType"/></td>
        <td class="value">${metaField.dataType}</td>
        <td><g:message code="metaField.label.mandatory"/></td>
        <td class="value">${metaField.mandatory?'yes':'no'}</td>
    </tr>
</g:each>