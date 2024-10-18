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
  Shows the suborders list
--%>

<div id="suborders-box">
    <div class="table-box tab-table">
        <g:if test="${order.childOrders}">
            <div class="table-scroll">
                <table id="suborders" cellspacing="0" cellpadding="0">
                    <tbody>
                        <g:each var="childOrder" in="${order.childOrders}">
                            <g:set var="activeSince" value="${formatDate(date: childOrder.activeSince ?: childOrder.createDate, formatName: 'date.pretty.format')}"/>
                            <g:set var="activeUntil" value="${formatDate(date: childOrder.activeUntil, formatName: 'date.pretty.format')}"/>
                            <tr id="suborder_tr_${childOrder.id}" onclick="$('#showChildButton').attr('href', $('#hidden_link_child_${childOrder.id}').prop('href'));">
                                <td>
                                    <a class="cell double">
                                        <strong>${childOrder.periodStr}</strong>
                                        <em><g:if test="${childOrder.id > 0}">
                                            <g:message code="table.id.format" args="[childOrder.id as String ]"/>
                                        </g:if><g:else>
                                            <g:message code="table.id.format" args="['']"/>&nbsp;<g:message code="default.new.label" args="['']"/>
                                        </g:else></em>
                                    </a>
                                </td>
                                <td><a class="cell"><em>${activeSince}</em></a></td>
                                <td><a class="cell"><em>${activeUntil ? activeUntil : '-'}</em></a></td>
                                <td style="display: none;">
                                    <g:link action="edit" elementId="hidden_link_child_${childOrder.id}" id="${childOrder.id}" params="[_eventId: 'changeOrder']" method="GET"> </g:link>
                                </td>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
            </div>
            <!-- spacer -->
            <div>
                &nbsp;
            </div>
            <div class="btn-box">
                <a class="submit" id="showChildButton" href="#">
                    <span><g:message code="order.button.show"/></span>
                </a>
            </div>
        </g:if>
    </div>

</div>