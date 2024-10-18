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
  Template for editing asset statuses

 @author Gerhard Maree
 @since  18-Apr-2013
--%>

<div class="form-columns single">
    <table cellpadding="0" cellspacing="0" class="innerTable" width="100%">
        <thead class="innerHeader">
        <tr>
            <th class="left medium"><g:message code="status.edit.th.name"/></th>
            <th class="left tiny2"><g:message code="status.edit.th.isAvailable"/></th>
            <th class="left tiny2"><g:message code="status.edit.th.isDefault"/></th>
            <th class="left tiny2"><g:message code="status.edit.th.isOrderSaved"/></th>
            <th class="left tiny2"><g:message code="status.edit.th.isActive"/></th>
            <th class="left tiny2"><g:message code="status.edit.th.isPending"/></th>
            <th class="left tiny2"><g:message code="status.edit.th.isOrderFinished"/></th>
            <th class="left tiny2"></th>
        </tr>
        </thead>
        <tbody id="statusTBody">

        <g:set var="statusIndex" value="${0}"/>
        <g:set var="readonly" value="${Boolean.toString(!assetMgmtEnabled)}"/>

        <!-- user-defined statuses -->
        <g:each var="status" in="${statuses}">

            <tr id="statusRow${statusIndex}">
                <td class="innerContent">
                    <input type="hidden" name="assetStatus.${statusIndex}.id" value="${status.id}"/>
                    <div class="inp-bg"><g:textField class="field" name="assetStatus.${statusIndex}.description"
                                 value="${status.description}" disabled="${readonly}"/></div>
                    <g:if test="${Boolean.valueOf(readonly)}">
                        <g:hiddenField name="assetStatus.${statusIndex}.description" value="${status.description}" />
                    </g:if>
                </td>
                <td class="innerContent">
                    <g:checkBox class="field" name="assetStatus.${statusIndex}.isAvailable"
                                checked="${status.isAvailable}" onchange="currentAvailable(this);" disabled="${readonly}"/>
                    <g:if test="${Boolean.valueOf(readonly)}">
                        <g:hiddenField name="assetStatus.${statusIndex}.isAvailable" value="${status.isAvailable ? "on" :""}" />
                    </g:if>
                </td>
                <td class="innerContent">
                    <g:checkBox class="field markerDefault" id="assetStatus.${statusIndex}.isDefault"
                                name="assetStatus.${statusIndex}.isDefault" checked="${status.isDefault}"
                                onchange="currentDefault(this);" disabled="${readonly}"/>
                    <g:if test="${Boolean.valueOf(readonly)}">
                        <g:hiddenField name="assetStatus.${statusIndex}.isDefault" value="${status.isDefault ? "on" :""}" />
                    </g:if>
                </td>
                <td class="innerContent">
                    <g:checkBox class="field markerOrderSaved" id="assetStatus.${statusIndex}.isOrderSaved"
                                name="assetStatus.${statusIndex}.isOrderSaved" checked="${status.isOrderSaved}"
                                onchange="currentOrderSaved(this);" disabled="${readonly}"/>
                    <g:if test="${Boolean.valueOf(readonly)}">
                        <g:hiddenField name="assetStatus.${statusIndex}.isOrderSaved" value="${status.isOrderSaved ? "on" :""}" />
                    </g:if>
                </td>
                <td class="innerContent">
                    <g:checkBox class="field markerActive" id="assetStatus.${statusIndex}.isActive"
                                name="assetStatus.${statusIndex}.isActive" checked="${status.isActive}"
                                onchange="currentActive(this);" readonly="${readonly}"/>
                </td>
                <td class="innerContent">
                    <g:checkBox class="field markerPending" id="assetStatus.${statusIndex}.isPending"
                                name="assetStatus.${statusIndex}.isPending" checked="${status.isPending}"
                                onchange="currentPending(this);" readonly="${readonly}"/>
                </td>
                <td class="innerContent">
                    <g:checkBox class="field markerOrderFinished" id="assetStatus.${statusIndex}.isOrderFinished"
                                name="assetStatus.${statusIndex}.isOrderFinished" checked="${status.isOrderFinished}"
                                onchange="currentOrderFinished(this);" readonly="${readonly}"/>
                </td>
                <td class="innerContent">
                    <g:if test="${readonly == 'false'}">
                        <a class="plus-icon" onclick="removeModelStatus(this, ${statusIndex})">
                            &#xe000;
                        </a>
                    </g:if>
                </td>
            </tr>
            <g:set var="statusIndex" value="${statusIndex + 1}"/>
        </g:each>

        <g:if test="${readonly == 'false'}">
            <!-- one empty row -->
            <g:set var="statusIndex" value="${statusIndex + 1}"/>
            <tr id="lastStatus">
                <td class="innerContent">
                    <div class="inp-bg"><input type="hidden" name="assetStatus.${statusIndex}.id" value="null"/>
                    <g:textField id="lastStatusName" class="field" name="assetStatus.${statusIndex}.description"/></div>
                </td>
                <td class="innerContent">
                    <g:checkBox id="lastStatusAvailable" class="field"
                                name="assetStatus.${statusIndex}.isAvailable" onchange="currentAvailable(this);"/>
                </td>
                <td class="innerContent">
                    <g:checkBox id="lastStatusDefault" class="field markerDefault"
                                name="assetStatus.${statusIndex}.isDefault" onchange="currentDefault(this);"/>
                </td>
                <td class="innerContent">
                    <g:checkBox id="lastStatusOrderSaved" class="field markerOrderSaved"
                                name="assetStatus.${statusIndex}.isOrderSaved" onchange="currentOrderSaved(this);"/>
                </td>
                <td class="innerContent">
                    <g:checkBox id="lastStatusActive" class="field markerActive"
                                name="assetStatus.${statusIndex}.isActive" onchange="currentActive(this);"/>
                </td>
                <td class="innerContent">
                    <g:checkBox id="lastStatusPending" class="field markerPending"
                                name="assetStatus.${statusIndex}.isPending" onchange="currentPending(this);"/>
                </td><td class="innerContent">
                <g:checkBox id="lastStatusOrderFinished" class="field markerOrderFinished"
                            name="assetStatus.${statusIndex}.isOrderFinished" onchange="currentOrderFinished(this);"/>
                </td>
                <td class="innerContent">
                    <a class="plus-icon" onclick="addModelStatus()">
                        &#xe026;
                    </a>
                </td>
            </tr>
        </g:if>

        </tbody>

    </table>
</div>

<r:script>

var statusIdx = ${statusIndex}+1;

<%-- Removes a line from the list of statuses --%>
    function removeModelStatus(thisRef, idx) {
      $(thisRef).closest("tr").remove();
    }

<%-- Make sure only the currently selected default status is checked--%>
    function currentDefault(thisRef) {
       if(thisRef.checked == true) {
           $('input[name="'+thisRef.name.replace("isDefault","isOrderSaved")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isDefault","isActive")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isDefault","isPending")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isDefault","isOrderFinished")+'"]').prop("checked", false);
           $(".markerDefault").prop("checked", false);
           thisRef.checked = true;
        }
    }

<%-- Make sure only the currently selected order saved status is checked--%>
    function currentOrderSaved(thisRef) {
        if(thisRef.checked == true) {
           $('input[name="'+thisRef.name.replace("isOrderSaved","isAvailable")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isOrderSaved","isDefault")+'"]').prop("checked", false);
           thisRef.checked = true;
        }
    }

    function currentAvailable(thisRef) {
        if(thisRef.checked == true) {
           $('input[name="'+thisRef.name.replace("isAvailable","isOrderSaved")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isAvailable","isActive")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isAvailable","isPending")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isAvailable","isOrderFinished")+'"]').prop("checked", false);
           thisRef.checked = true;
        }
    }

<%-- Make sure only the currently selected order saved status and active status is checked--%>
    function currentActive(thisRef) {
        if(thisRef.checked == true) {
           $('input[name="'+thisRef.name.replace("isActive","isAvailable")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isActive","isDefault")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isActive","isPending")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isActive","isOrderSaved")+'"]').prop("checked", true);
           $(".markerActive").prop("checked", false);
           thisRef.checked = true;
        } if(thisRef.checked == false){
           $('input[name="'+thisRef.name.replace("isActive","isOrderSaved")+'"]').prop("checked", false);
           $(".markerActive").prop("checked", false);
           thisRef.checked = false;
        }
    }

<%-- Make sure only the currently selected order saved status and pending status is checked--%>
    function currentPending(thisRef) {
        if(thisRef.checked == true) {
           $('input[name="'+thisRef.name.replace("isPending","isAvailable")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isPending","isDefault")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isPending","isActive")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isPending","isOrderSaved")+'"]').prop("checked", true);
           $(".markerPending").prop("checked", false);
           thisRef.checked = true;
        } if(thisRef.checked == false){
           $('input[name="'+thisRef.name.replace("isPending","isOrderSaved")+'"]').prop("checked", false);
           $(".markerPending").prop("checked", false);
           thisRef.checked = false;
        }
    }
<%-- Make sure only the currently selected order finished status is checked--%>
    function currentOrderFinished(thisRef) {
        if(thisRef.checked == true) {
           $('input[name="'+thisRef.name.replace("isOrderFinished","isAvailable")+'"]').prop("checked", false);
           $('input[name="'+thisRef.name.replace("isOrderFinished","isDefault")+'"]').prop("checked", false);
           thisRef.checked = true;
        }
    }
<%-- Add a new empty status line to the table --%>
    function addModelStatus() {
        statusIdx ++;
        var lastStatusName = $("#lastStatusName").val();
        $("#lastStatusName").val("");
        var lastStatusAvailable = $("#lastStatusAvailable").prop("checked");
        $("#lastStatusAvailable").prop("checked", false);
        var lastStatusDefault = $("#lastStatusDefault").prop("checked");
        $("#lastStatusDefault").prop("checked", false);
        var lastStatusOrderSaved = $("#lastStatusOrderSaved").prop("checked");
        $("#lastStatusOrderSaved").prop("checked", false);
        var lastStatusActive = $("#lastStatusActive").prop("checked");
        $("#lastStatusActive").prop("checked", false);
        var lastStatusPending = $("#lastStatusPending").prop("checked");
        $("#lastStatusPending").prop("checked", false);
        var lastStatusOrderFinished = $("#lastStatusOrderFinished").prop("checked");
        $("#lastStatusOrderFinished").prop("checked", false);

        $("#lastStatus").before('<tr id="statusRow'+statusIdx+'">' +
            '<td class="innerContent">' +
            '<input type="hidden" name="assetStatus.'+statusIdx+'.id" value="null"/>' +
            '<div class="inp-bg"><input type="text" class="field" name="assetStatus.'+statusIdx+'.description" value="'+lastStatusName+'"/></div>' +
            '</td>' +
            '<td class="innerContent">' +
            '<input type="checkbox" class="field" name="assetStatus.'+statusIdx+'.isAvailable" '+(lastStatusAvailable?'checked':'')+' onchange="currentAvailable(this);" />' +
            '</td>' +
            '<td class="innerContent">' +
            '<input type="checkbox" class="field markerDefault" id="assetStatus.'+statusIdx+'.isDefault" name="assetStatus.'+statusIdx+'.isDefault" '+(lastStatusDefault?'checked':'')+' onchange="currentDefault(this);" />' +
            '</td>' +
            '<td class="innerContent">' +
            '<input type="checkbox" class="field markerOrderSaved" id="assetStatus.'+statusIdx+'.isOrderSaved" name="assetStatus.'+statusIdx+'.isOrderSaved" '+(lastStatusOrderSaved?'checked':'')+' onchange="currentOrderSaved(this);"/>' +
            '</td>' +
            '<td class="innerContent">' +
            '<input type="checkbox" class="field markerActive" id="assetStatus.'+statusIdx+'.isActive" name="assetStatus.'+statusIdx+'.isActive" '+(lastStatusActive?'checked':'')+' onchange="currentActive(this);"/>' +
            '</td>' +
            '<td class="innerContent">' +
            '<input type="checkbox" class="field markerPending" id="assetStatus.'+statusIdx+'.isPending" name="assetStatus.'+statusIdx+'.isPending" '+(lastStatusPending?'checked':'')+' onchange="currentPending(this);"/>' +
            '</td>' +
            '<td class="innerContent">' +
            '<input type="checkbox" class="field markerOrderFinished" id="assetStatus.'+statusIdx+'.isOrderFinished" name="assetStatus.'+statusIdx+'.isOrderFinished" '+(lastStatusOrderFinished?'checked':'')+' onchange="currentOrderFinished(this);"/>' +
            '</td>' +
            '<td class="innerContent">' +
            '<a class="plus-icon" onclick="removeModelStatus(this, '+statusIdx+')">' +
            '&#xe000;' +
            '</a>' +
            '</td>' +
            '</tr>');
    }
</r:script>
