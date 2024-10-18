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

<%@ page import="com.sapienter.jbilling.common.CommonConstants; com.sapienter.jbilling.server.pricing.PriceModelWS" contentType="text/html;charset=UTF-8" %>
<%@ page import="com.sapienter.jbilling.server.timezone.TimezoneHelper" %>

<div id="timeline-${aitVal}">
    <div id="timeline">
        <g:hiddenField name="currentDate${aitVal}" value="${formatDate(date: startDate)}"/>
        <div class="form-columns">
            <ul>
                <g:if test="${pricingDates.get(aitVal)}">
                    <g:each var="date" status="i" in="${pricingDates.get(aitVal)}">
                        <li class="${startDate.equals(date) ? 'current' : ''}">
                            <g:set var="pricingDate" value="${formatDate(date: date)}"/>
                            <a href="javascript:void(0)" onclick="paintValues(${aitVal},'${pricingDate}');">${pricingDate}</a>
                        </li>
                    </g:each>
                </g:if>
                <g:else>
                    <li class="current">
                        <g:set var="pricingDate" value="${formatDate(date: CommonConstants.EPOCH_DATE)}"/>
                        <g:remoteLink action="edit" params="[_eventId: 'editDate', startDate : pricingDate]"
                                      update="column2" method="GET" onSuccess="timeline.refresh(); details.refresh();">
                            ${pricingDate}
                        </g:remoteLink>
                    </li>
                </g:else>

                <li class="new">
                    <a onclick="openTimeLineDialog('${aitVal}')">
                        <g:message code="button.add.price.date"/>
                    </a>
                </li>
            </ul>
        </div>

        <div id="add-date-dialog-${aitVal}" title="Add Date">
            <div class="form-columns single">
                <div class="msg-box error" id="date-validation-message${aitVal}" style="display: none;">
                    <g:message code="product.invalid.startdate.format"/>
                </div>
            </div>
            <div class="column">
                <div class="columns-holder">
                    <fieldset>
                        <div class="form-columns">
                            <g:applyLayout name="form/date">
                                <content tag="label"><g:message code="plan.item.start.date"/></content>
                                <content tag="label.for">startDate</content>
                                <g:textField    class = "field"
                                                 name = "startDate${aitVal}"
                                             onchange = "isValidStartDate(this, '${aitVal}');"
                                                value = ""/>
                            </g:applyLayout>
                        </div>
                    </fieldset>
                </div>
            </div>
        </div>
    </div>
	<div class="center-align">
		<g:if test="${!startDate.equals(CommonConstants.EPOCH_DATE)}">
    		<a class="submit delete" onclick="removeDate(${aitVal});">
       			<span><g:message code="button.remove"/></span>
   			</a>
   		</g:if>
  	</div>
</div>

<script type="text/javascript">
    var expiryDateFormat= "<g:message code="date.format"/>";
    $(document).ready(function() {
        $('#add-date-dialog-${aitVal}').dialog({
            autoOpen: false,
            height: 400,
            width: 520,
            modal: true,
            buttons: {
                Cancel: function() {
                    $(this).dialog("close");
                },
                Save: function() {
                    if (isValidStartDate($('input[name="startDate${aitVal}"]'))) {
                        addDate(${aitVal});
                        $("#date-validation-message${aitVal}").hide();
                        $(this).dialog("close");
                    }
                }
            },
            close: function() {
                $("#date-validation-message${aitVal}").hide();
                $('input[name="startDate${aitVal}"]').css('border', 'none');
            }
        });
    });

    function openTimeLineDialog(aitVal){
        $('input[name="startDate'+aitVal+'"]').val("${formatDate(date: TimezoneHelper.currentDateForTimezone(session['company_timezone']), formatName: 'datepicker.format')}");
        $('#add-date-dialog-'+aitVal).dialog('open');
    }

    function addDate(value) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'addDate')}',
            async: false,
            data: {
                 date : $("#startDate"+value).val(),
                dates : $("#datesXml").val(),
                aitId : value
            },
            success: function(data) {
                $("#timeline-" + value).replaceWith(data);
                updateXml(value);
                updateEffectiveDateXml(value, $("#startDate"+value).val());
                paintValues(value, $("#startDate"+value).val())
            }
        });
    }

    function updateXml(value) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'updateDatesXml')}',
            async:false,
            data: {
                 date : $("#startDate"+value).val(),
                dates : $("#datesXml").val(),
                aitId : value
            },
            dataType: "text",
            success: function(data) {
                $("#datesXml").val(data);
            }
        });
    }

    function paintValues(val, d) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'editDate')}',
            async: false,
            data: {
                startDate : d,
                    aitId : val,
                   values : $("#infoFieldsMapXml").val(),
                    dates : $("#datesXml").val()
            },
            success: function(data) {
                $("#ait-inner-" + val).replaceWith(data);
            },
            complete: function() {
                refreshTimeLine(val, d);
            }
        });
    };

    function refreshTimeLine(val, date) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'refreshTimeLine')}',
            async: false,
            data: {
                startDate : date,
                    aitId : val,
                   values : $("#datesXml").val()
            },
            success: function(data) {
                $("#timeline-" + val).replaceWith(data);
                updateEffectiveDateXml(val, date);
            }
        });
    }

    function updateEffectiveDateXml(val, date) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'updateEffectiveDateXml')}',
            async: false,
            data: {
                startDate : date,
                    aitId : val,
                   values : $("#effectiveDatesXml").val(),
                    dates : $("#datesXml").val()
            },
            dataType: "text",
            success: function(data) {
                $("#effectiveDatesXml").val(data);
            }
        });
    }

    function removeDate(val) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'updateRemovedDatesXml')}',
            async:  false,
            data: {
                   startDate : $("#currentDate" + val).val(),
                       aitId : val,
                removedDates : $("#removedDatesXml").val()
            },
            dataType: "text",
            success: function(data) {
                $("#removedDatesXml").val(data);
                updateTimeLineDates(val,$("#currentDate" + val).val());
            }
        });
    }

    function updateTimeLineDates(val, date) {
        $.ajax({
            type: 'POST',
            url: '${createLink(action: 'updateTimeLineDatesXml')}',
            async: false,
            data: {
                startDate : date,
                    dates : $("#datesXml").val(),
                    aitId : val
            },
            dataType: "text",
            success: function(data) {
                $("#datesXml").val(data);
                paintValues(val, null);
            }
        });
    }

    function isValidStartDate(dateControl, aitVal) {
        if(!$(dateControl).val() || !isValidDate(dateControl, expiryDateFormat)) {
            $("#error-messages").hide();
            $("#date-validation-message" + aitVal).show();
            dateControl.focus();
            return false;
        } else {
            $("#date-validation-message" + aitVal).hide();
            return true;
        }
    }
</script>