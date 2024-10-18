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
  Show Route for Route Based Configration
  @author Rahul Asthana
--%>

<div class="heading">
    <strong><g:message code="route.detail.selected.title"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <fieldset>
            <div class="form-columns">
                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.name"/></content>
                    ${selected?.name}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.table.name"/></content>
                    ${selected?.tableName}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.output.field.name"/></content>
                    ${selected?.outputFieldName}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.default.next.route"/></content>
                    ${selected?.defaultRoute}
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.root.table"/></content>
                    <g:formatBoolean boolean="${ (selected?.rootTable instanceof Boolean) ? selected?.rootTable : (selected?.rootTable > 0) }"/>
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.route.table"/></content>
                    <g:formatBoolean
                            boolean="${(selected?.routeTable instanceof Boolean) ? selected?.routeTable : (selected?.routeTable > 0)}"/>
                </g:applyLayout>

                <g:applyLayout name="form/text">
                    <content tag="label" class="label2"><g:message code="route.csv.file"/></content>
                    <g:link action="csv" id="${selected.id}">
                        ${selected?.tableName}.csv
                    </g:link>
                </g:applyLayout>
            </div>
        </fieldset>
    </div>

    <div class="btn-box buttons">
        <ul><li>
            <g:remoteLink action="edit" id="${selected.id}" class="submit change" update="column2">
                <span><g:message code="route.edit.button"/></span>
            </g:remoteLink>
        </li><li>
            <a onclick="showConfirm('delete-${selected.id}');" class="submit delete">
                <span><g:message code="route.delete.button"/></span>
            </a>
        </li>
            <li>
                <g:link action="search" id="${selected.id}" class="submit find" >
                    <span><g:message code="route.browse.button"/></span>
                </g:link>
            </li>
        </ul>
    </div>
</div>

<g:if test="${selected.routeTable}">
    <div id="matching-field-holder">
        <g:render template="showMatchingField"
                  model="['route': selected, 'matchingFields': matchingFields]"/>
    </div>

    <div id="matching-field-form-holder">
        <g:if test="${matchFieldWS}">
            <g:render template="editMatchingField"
                      model="[              'route': selected,
                              'availMatchingFields': availMatchingFields,
                                    'matchingField': matchFieldWS]"/>
        </g:if>
    </div>

    <g:if test="${matchingFields}">
        <g:render template="testRoute" model="[
                selected: selected,
                matchingFields: matchingFields
        ]"/>
    </g:if>
</g:if>

<g:render template="/confirm"
          model="['message': 'route.delete.confirm',
                  'controller': 'route',
                  'action': 'delete',
                  'id': selected.id,
                  'ajax': false,
                  'update': 'column-p'
          ]"/>
