<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO"%>

<%-- 
    Orders list template. 
    
    @author Neeraj Bhatt
    @since 14-Aug-2015
 --%>

<div class="table-box">
    <div class="table-scroll">
        <sec:ifAllGranted roles="CUSTOMER_ENROLLMENT_910">
            <table id="orders" cellspacing="0" cellpadding="0">
                <thead>
                <tr>
                    <th class="small first">
                        <g:remoteSort action="list" sort="id" update="column1">
                            <g:message code="customer.enrollment.label.id"/>
                        </g:remoteSort>
                    </th>
                    <th class="small">
                        <g:remoteSort action="list" sort="accountType.id" update="column1">
                            <g:message code="customer.enrollment.label.account.type"/>
                        </g:remoteSort>
                    </th>

                    <th class="small">
                        <g:remoteSort action="list" sort="status" update="column1">
                            <g:message code="customer.enrollment.label.status"/>
                        </g:remoteSort>
                    </th>

                    <th class="small last">
                        <g:remoteSort action="list" sort="company" update="column1">
                            <g:message code="customer.enrollment.label.company"/>
                        </g:remoteSort>
                    </th>

                </tr>
                </thead>
                <tbody>
                <g:each var="customerEnrollment" in="${customerEnrollments}">

                    <tr id="order-${customerEnrollment.id}"
                        class="${(selected?.id == customerEnrollment?.id) ? 'active' : ''}">
                        <td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${customerEnrollment.id}"
                                          params="['template': 'show']" before="register(this);"
                                          onSuccess="render(data, next);">
                                ${customerEnrollment.id}
                            </g:remoteLink>
                        </td>

                        <td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${customerEnrollment.id}"
                                          params="['template': 'show']" before="register(this);"
                                          onSuccess="render(data, next);">
                                ${customerEnrollment?.accountType?.description}
                            </g:remoteLink>
                        </td>

                        <td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${customerEnrollment.id}"
                                          params="['template': 'show']" before="register(this);"
                                          onSuccess="render(data, next);">
                                ${customerEnrollment?.status}
                            </g:remoteLink>
                        </td>

                        <td>
                            <g:remoteLink breadcrumb="id" class="cell" action="show" id="${customerEnrollment.id}"
                                          params="['template': 'show']" before="register(this);"
                                          onSuccess="render(data, next);">
                                ${customerEnrollment?.company.getDescription()}
                            </g:remoteLink>
                        </td>

                    </tr>
                </g:each>
                </tbody>
            </table>
        </sec:ifAllGranted>
     </div>
</div>

<div class="pager-box">

    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults" model="[steps: [10, 20, 50], update: 'column1']" />
        </div>

    </div>

    <div class="row">
        <jB:remotePaginate controller="customerEnrollment" action="list" total="${customerEnrollments?.totalCount ?: 0}" update="column1" params="[partial:true]"/>
    </div>
</div>

<sec:ifAllGranted roles="CUSTOMER_ENROLLMENT_911">
<div class="btn-box">
    <g:link action='edit' class="submit add button-primary"><span><g:message code="customer.enrollment.button.create"/></span></g:link>
    <g:link action='editBulk' class="submit"><span><g:message code="customer.bulk.enrollment.button"/></span></g:link>
</div>
</sec:ifAllGranted>
