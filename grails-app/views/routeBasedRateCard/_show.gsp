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

<%@ page contentType="text/html;charset=UTF-8" %>
<%--
  Shows a route rate card.

  @author Rahul Asthana
--%>

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected?.name}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
          <fieldset>
            <div class="form-columns">
                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.name"/></content>
                    ${selected?.name}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.table.name"/></content>
                    ${selected?.tableName}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="route.based.rate.card.unit"/></content>
                    ${selected?.ratingUnit?.name}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label"><g:message code="rate.card.csv.file"/></content>
                    <g:link action="csv" id="${selected.id}">
                        ${selected?.tableName}.csv
                    </g:link>
                </g:applyLayout>
            </div>
        </fieldset>
      </div>
        <div class="btn-box buttons">
            <ul>
                <li>
                    <g:remoteLink action="edit" id="${selected.id}" class="submit change" update="column2" params="[partial: true]">
                        <span><g:message code="button.edit"/></span>
                    </g:remoteLink>
                </li>
                <li>
                    <g:remoteLink action="test" update="update" class="submit play" params="[routeRateCardId:selected?.id]">
                        <g:message code="button.test"/>
                    </g:remoteLink>
                </li>
                <li>
                    <g:link action="search" id="${selected.id}" class="submit find" >
                        <span><g:message code="route.browse.button"/></span>
                    </g:link>
                </li>
                <li>
                    <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
                </li>
            </ul>
        </div>
    </div>
    <g:render template="/confirm"
              model="['message': 'route.rate.card.delete.confirm',
                      'controller': 'routeBasedRateCard',
                      'action': 'delete',
                      'id': selected.id,
                      'ajax': false,
                      'update': 'column1',
                     ]"/>


    <div id="update">

        <g:render  template="showMatchingField" model="[selected:selected]"/>

