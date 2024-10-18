<%@ page import="com.sapienter.jbilling.server.user.partner.db.PartnerDTO; com.sapienter.jbilling.server.user.db.UserDTO" %>
<div class="column-hold">
    <div class="heading">
        <strong>
            <g:message code="customer.enrollment.show.heading" />

        </strong>
    </div>

    <!-- Customer Enrollment Details -->
    <div class="box">

        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="customer.enrollment.id"/></td>
                    <td class="value">${customerEnrollment?.id}</td>
                </tr>
                <tr>
                    <td><g:message code="customer.enrollment.show.company"/></td>
                    <td class="value">${customerEnrollment?.company?.description}</td>
                </tr>
                <tr>
                    <td><g:message code="customer.enrollment.account.type"/></td>
                    <td class="value">
                        ${customerEnrollment?.accountType?.description}
                    </td>
                </tr>
                <tr>
                    <td><g:message code="customer.enrollment.status"/></td>
                    <td class="value">${customerEnrollment?.status}</td>
                </tr>
                <sec:ifAllGranted roles="EDI_922">
                    <g:if test="${customerEnrollment?.user}">
                        <tr>
                            <td><g:message code="customer.enrollment.label.customer.id"/></td>
                            <td class="value">
                                <g:remoteLink controller="customer" action="show" id="${customerEnrollment?.user?.id}"
                                              before="register(this);" onSuccess="render(data, next);">
                                    Customer-${customerEnrollment?.user?.id}
                                </g:remoteLink>
                            </td>
                        </tr>
                    </g:if>
                </sec:ifAllGranted>
                <tr>
                    <td><g:message code="customer.enrollment.edi.files"/></td>
                    <td class="value">
                        <g:link controller="ediFile" action="list" params="[enrollmentId:customerEnrollment?.id]"><g:message code="customer.enrollment.edi.files"/></g:link>
                    </td>
                </tr>

                <g:if test="${customerEnrollment?.parentEnrollment}">
                    <tr>
                        <td><g:message code="customer.enrollment.parent.enrollment.id"/></td>
                        <td class="value">${customerEnrollment?.parentEnrollment?.id}</td>
                    </tr>

                </g:if>

                <g:if test="${customerEnrollment?.parentCustomer?.id}">
                    <tr>
                        <td><g:message code="customer.enrollment.parent.userId.id"/></td>
                        <td class="value"> ${customerEnrollment?.parentCustomer?.id}</td>
                    </tr>
                </g:if>

                </tbody>
            </table>
        </div>
    </div>

    <div class="heading">
        <strong>
            <g:message code="customer.enrollment.show.details" />
        </strong>
    </div>
    <div class="box">

        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <g:each in="${customerEnrollment.accountType.informationTypes?.sort{ it.displayOrder }}" var="metaFieldGroup">
                    <tr><td colspan="2"><br/></td></tr>
                    <tr ><td colspan="2"><b>${metaFieldGroup.name}</b></td></tr>
                    <g:set var="accountMetafields" value="[]"/>
                    <g:each in="${metaFieldGroup?.metaFields?.sort{ it.displayOrder }}" var="metaField">
                        <g:set var="fieldValue" value="${customerEnrollment?.metaFields?.find{ (it.field.name == metaField.name) &&
                                ((it.field.metaFieldGroups && metaFieldGroup.id && it.field.metaFieldGroups.first().id == metaFieldGroup.id) || (!it.field.metaFieldGroups.first().id && !metaFieldGroup.id)) }}"/>
                        <g:if test="${fieldValue == null && metaField.getDefaultValue()}">
                            <g:set var="fieldValue" value="${metaField.getDefaultValue()}"/>
                        </g:if>
                        <g:if test="${fieldValue}">
                            <% accountMetafields.add(fieldValue) %>
                        </g:if>
                    </g:each>
                    <g:render template="/metaFields/metaFields" model="[metaFields:accountMetafields]"/>
                </g:each>

                %{--Displaying enrollment metafield--}%
                <g:set var="aitMetaField" value="[]"/>
                <g:each in="${customerEnrollment.accountType.informationTypes?.sort{ it.displayOrder }}" var="metaFieldGroup">
                    <g:each in="${metaFieldGroup?.metaFields?.sort{ it.displayOrder }}" var="metaField">
                        <% aitMetaField.add(customerEnrollment.metaFields.find{it.field.id==metaField.id}) %>
                    </g:each>
                </g:each>
                <g:if test="${customerEnrollment.metaFields-aitMetaField}">
                    <tr><td colspan="2"><br/></td></tr>
                    <tr ><td colspan="2"><b><g:message code="customer.enrollment.edit.enrollment.info"/> </b></td></tr>

                    <g:render template="/metaFields/metaFields" model="[metaFields: customerEnrollment.metaFields-aitMetaField]"/>
                </g:if>

                </tbody>
            </table>
        </div>
    </div>

    <g:if test="${customerEnrollment?.comments}">
        <div class="heading">
            <strong>
                <g:message code="customer.enrollment.comment" />
            </strong>
        </div>

        <div class="box">
            <div class="sub-box">
                <div class="table-box">
                    <table id="users" cellspacing="0" cellpadding="0">
                        <thead>
                        <tr class="ui-widget-header" >
                            <th class="first" width="50px"><g:message code="customer.detail.note.form.author"/></th>
                            <th width="60px"><g:message code="customer.detail.note.form.createdDate"/></th>
                            <th class="last" width="150px"><g:message code="customer.enrollment.comment"/></th>
                        </thead>
                        <tbody>

                        <g:if test="${customerEnrollment?.comments}">
                            <g:each in="${customerEnrollment?.comments}" var="comment">

                                <tr>
                                    <td>${comment?.user?.userName}</td>
                                    <td><g:formatDate date="${comment?.creationTime}" type="date" style="MEDIUM"/>  </td>
                                    <td>${comment?.comment}</td>
                                </tr>
                            </g:each>
                        </g:if>
                        <g:else>
                            <p><em><g:message code="customer.detail.note.empty.message"/></em></p>
                        </g:else>
                        </tbody>
                    </table>
                </div>

            </div>
        </div>

    </g:if>

<g:if test="${customerEnrollment?.agents}">
    <div class="heading">
        <strong>
            <g:message code="customer.enrollment.agents" />
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <div class="table-box">
                <table id="agents" cellspacing="0" cellpadding="0">
                    <thead>
                    <tr class="ui-widget-header" >
                    <th class="first" width="120px"><g:message code="customer.enrollment.head.agent"/></th>
                    <th class="last" width="50px"><g:message code="customer.enrollment.head.rate"/></th>
                    </thead>
                    <tbody>

                    <g:if test="${customerEnrollment?.agents}">
                        <g:each in="${customerEnrollment?.agents}" var="agent">

                            <tr>
                                <td>
                                    <%
                                        String partnerName = "";
                                        if(agent.getPartner() != null) {
                                            PartnerDTO partnerDTO = agent.getPartner();
                                            partnerName = (partnerDTO.getBaseUser().getContact().getFirstName() != null && partnerDTO.getBaseUser().getContact().getFirstName().trim().length() > 0) ?
                                                (partnerDTO.getBaseUser().getContact().getFirstName() + ' ' + partnerDTO.getBaseUser().getContact().getLastName()) : partnerDTO.getBaseUser().getUserName();
                                        } else if(agent.getBrokerId() != null) {
                                            partnerName = agent.getBrokerId();
                                        }
                                    %>
                                    ${partnerName}</td>
                                <td>${ agent?.rate ? formatNumber(number: agent.rate, formatName: 'decimal.format') : ''}</td>
                            </tr>
                        </g:each>
                    </g:if>
                    <g:else>
                        <p><em><g:message code="customer.detail.agents.empty.message"/></em></p>
                    </g:else>
                    </tbody>
                </table>
            </div>

        </div>
    </div>

</g:if>


    <div class="btn-box">
        <sec:ifAllGranted roles="CUSTOMER_ENROLLMENT_911">
            <g:if test="${customerEnrollment.status==com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus.PENDING}">
                <div class="row">
                    <g:link action="edit" id="${customerEnrollment.id}" class="submit edit"><span><g:message code="button.edit"/></span></g:link>
                </div>
            </g:if>

        </sec:ifAllGranted>
        <div >
            <sec:ifAllGranted roles="CUSTOMER_ENROLLMENT_912">
                <a onclick="showConfirm('delete-${customerEnrollment.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
            </sec:ifAllGranted>
        </div>
    </div>
    <g:render template="/confirm"
              model="['message': 'customer.enrollment.delete.confirm',
                      'controller': 'customerEnrollment',
                      'action': 'delete',
                      'id': customerEnrollment?.id,
              ]"/>

</div>

