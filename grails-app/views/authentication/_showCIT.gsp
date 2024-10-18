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

<%--

  @author Aamir Ali
  @since  02/21/2017
--%>


<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.name}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <!-- cit info -->
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="company.information.type.id.label"/></td>
                    <td class="value" colspan="3">
                        ${selected.id}
                    </td>
                </tr>
                <tr>
                    <td><g:message code="company.information.type.name.label"/></td>
                    <td class="value" colspan="3">
                        ${selected.name}
                    </td>
                </tr>
                <tr>
                    <td><g:message code="company.information.type.display.label"/></td>
                    <td class="value">${selected.displayOrder}</td>
                </tr>
                </tbody>
            </table>

            <!-- metafields -->
            <g:if test="${selected.metaFields}">
                <table class="dataTable" cellspacing="0" cellpadding="0" width="100%">
                    <tbody>
                    <g:render template="/metaFieldGroup/metafields" model="[model: selected]"/>
                    </tbody>
                </table>
            </g:if>
        </div>
    </div>

    <div class="btn-box">
        <g:link action="editCIT" id="${selected.id}" params="[companyId: selected.company.id]" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
        <a href="${createLink(controller:'authentication', action:'editCIT', id:selected.id ,params:[companyId: selected.company.id, clone:'true']) }" class="submit add">
            <span><g:message code="button.clone"/></span></a>
        <a onclick="showConfirm('deleteCIT-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
    </div>

    <g:render template="/confirm"
              model="['message': 'company.information.type.delete.confirm',
                      'controller': 'authentication',
                      'action': 'deleteCIT',
                      'id': selected.id,
                      'formParams': ['companyId': selected.company.id],
                      'ajax': false,
                      'update': 'column1',
              ]"/>
</div>

