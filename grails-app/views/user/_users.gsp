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

<%@ page import="org.apache.commons.lang.StringEscapeUtils; com.sapienter.jbilling.server.user.contact.db.ContactDTO" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.client.util.SortableCriteria;" %>

<%--
  Shows a list of internal users.

  @author Brian Cowdery
  @since  04-Apr-2011
--%>
<style>
.box-cards {
  margin: 0 20px 0;
  padding: 00px 0 0 0;
}

.box-cards .box-card-hold.narrow {
    padding: 2px 10px 10px;
    border: 1px solid #bbbbbb;
    border-top: none;
}
</style>
<g:set var="searchParams" value="${SortableCriteria.extractParameters(params, ['filterBy'])}" />

<div class="box-cards box-cards-no-margin ${searchParams ? "box-cards-open" : ""}">
    <div class="box-cards-title ${searchParams ? "active" : ""}">
        <a class="btn-open" href="#"><span><g:message
                code="asset.heading.filter"/></span></a>
    </div>
    <div class="box-card-hold narrow" >
        <div class="form-columns user-list-filters">
            <g:form name="user-filter-form" id="${params.id}" action="list">
                <g:applyLayout name="form/input">
                    <content tag="label"><g:message code="filters.title"/></content>
                    <content tag="label.for">filterBy</content>
                    <g:textField       name = "filterBy"
                                       class = "field default"
                                       placeholder = "${message(code: 'prompt.login.name')}"
                                       value = "${params.filterBy}"/>
                </g:applyLayout>
            </g:form>
        </div>
    </div>
</div>
<p/>
<div class="table-box">
    <table id="users" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th><g:message code="users.th.login"/></th>
                <th><g:message code="users.th.name"/></th>
                <th><g:message code="users.th.organization"/></th>
                <th class="small"><g:message code="users.th.role"/></th>
            </tr>
        </thead>

        <tbody>
            <g:each var="user" in="${users}">
                <g:set var="_contact" value="${ContactDTO.findByUserId(user.id)}"/>

                <tr id="user-${user.id}" class="${selected?.id == user.id ? 'active' : ''}">
                    <td>
                        <jB:secRemoteLink permissions="USER_1406" class="cell double" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);" params="[partial:true]">
                            <strong>${StringEscapeUtils.escapeHtml(user?.userName)}</strong>
                            <em><g:message code="table.id.format" args="[user.id as String]"/></em>
                        </jB:secRemoteLink>
                    </td>

                    <td>
                        <jB:secRemoteLink permissions="USER_1406" class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);" params="[partial:true]">
                            ${StringEscapeUtils.escapeHtml(_contact?.firstName)} ${StringEscapeUtils.escapeHtml(_contact?.lastName)}
                        </jB:secRemoteLink>
                    </td>

                    <td>
                        <jB:secRemoteLink permissions="USER_1406" class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);" params="[partial:true]">
                            ${StringEscapeUtils.escapeHtml(_contact?.organizationName)}
                        </jB:secRemoteLink>
                    </td>

                    <td class="small" data-cy="clickOnTheUser">
                        <jB:secRemoteLink permissions="USER_1406" class="cell" action="show" id="${user.id}" before="register(this);" onSuccess="render(data, next);" params="[partial:true]">
                            <g:if test="${user.roles}">
                                ${StringEscapeUtils.escapeHtml(user?.roles?.asList()?.first()?.getTitle(session['language_id']))}
                            </g:if>
                            <g:else>
                                -
                            </g:else>
                        </jB:secRemoteLink>
                    </td>
                </tr>

            </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <div class="results">
            <g:render template="/layouts/includes/pagerShowResults"
                       model = "[steps: [10, 20, 50],
                            action: 'list',
                            id: params.id,
                            update: 'column1',
                            extraParams: searchParams,
                            success:'initUserSearchForm()' ]" />
        </div>
    </div>

    <div class="row-center">
        <jB:remotePaginate controller="user"
                           action="list"
                           id = "${params.id}"
                           params = "${sortableParams(params: ([partial: true])) + searchParams}"
                           total="${users?.totalCount ?: 0}"
                           update="column1"/>
    </div>
</div>

<div class="btn-box">
    <sec:ifAnyGranted roles="USER_1405">
        <g:link action="edit" class="submit add button-primary" data-cy="addNewUser">
            <span><g:message code="button.create"/></span>
        </g:link>
    </sec:ifAnyGranted>
</div>

<script type="text/javascript">
    //required by the meta field java script included
    var localLang = '${session.locale.language}';
    var userSearchFormId = 'user-filter-form';
    <%-- function which will search for users --%>
    var userSearchFunction = function() {
        $('#'+userSearchFormId).submit();
    };

</script>
<script type="text/javascript">
    $(document).ready(function() {
        placeholder();
        initUserSearchForm();
    });
function initUserSearchForm() {
        <%-- event listeners to reload results --%>
        $('#user-filter-form :input[name=filterBy]').blur(function () {
            $('#user-filter-form').submit();
        });
    }
</script>
