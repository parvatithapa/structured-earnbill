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

<%@ page import="org.apache.commons.lang.StringEscapeUtils" contentType="text/html;charset=UTF-8" %>

<%--
  Shows a list of languages.

  @author Neeraj Bhatt
  @since  09-Jun-2014
--%>

<div class="table-box">
    <table id="languages" cellspacing="0" cellpadding="0">
        <thead>
            <tr>
                <th>
                    <g:remoteSort action="list" sort="id" update="column1">
                        <g:message code="language.label.id"/>
                    </g:remoteSort>
                </th>
                <th>
                    <g:remoteSort action="list" sort="code" update="column1">
                        <g:message code="language.label.code"/>
                    </g:remoteSort>
                </th>
                <th>
                    <g:remoteSort action="list" sort="countryCode" update="column1">
                        <g:message code="language.label.countryCode"/>
                    </g:remoteSort>
                </th>
                <th>
                    <g:remoteSort action="list" sort="locale" update="column1">
                        <g:message code="language.label.locale"/>
                    </g:remoteSort>
                </th>
                <th>
                    <g:remoteSort action="list" sort="description" update="column1">
                        <g:message code="language.label.description"/>
                    </g:remoteSort>
                </th>
            </tr>
        </thead>
        <tbody>
            <g:each var="language" in="${languages}">
                <tr id="language-${language.id}" class="${selected?.id == language.id ? 'active' : ''}">
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${language.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${language.getId()}</strong>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${language.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(language.code)}</strong>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${language.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(language.countryCode)}</strong>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${language.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(language.asLocale().toString())}</strong>
                        </g:remoteLink>
                    </td>
                    <td>
                        <g:remoteLink class="cell double" action="show" id="${language.id}" before="register(this);" onSuccess="render(data, next);">
                            <strong>${StringEscapeUtils.escapeHtml(language.description)}</strong>
                        </g:remoteLink>
                    </td>
                </tr>
            </g:each>
        </tbody>
    </table>
</div>

<div class="pager-box">
    <div class="row">
        <jB:remotePaginate controller="language" action="list" params="${sortableParams(params: [partial: true], order: 'desc')}" total="${languages?.totalCount ?: 0}" update="column1"/>
    </div>
</div>

<div class="btn-box">
    <g:remoteLink action='edit' class="submit add button-primary" before="register(this);" onSuccess="render(data, next);">
        <span><g:message code="button.create"/></span>
    </g:remoteLink>
</div>
