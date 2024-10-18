<%@ page import="com.sapienter.jbilling.server.util.Constants; com.sapienter.jbilling.server.item.db.AssetDTO; com.sapienter.jbilling.server.item.db.ItemDTO" %>
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
  Shows the changes list
--%>

<div id="assets-dialog-update">
    <div id="assets-box-update">
    </div>
</div>

<div id="edit-changes-box">
    <script type="text/javascript">
    	
        var updateSuccess = ${message ? true : false};
        if(updateSuccess) {
            showTabWithoutClickIfNeeded('ui-tabs-review');
        } else {
            showTabWithoutClickIfNeeded('ui-tabs-edit-changes');
        }
    </script>

    <!-- error messages -->
    <div id="messages">
        <g:if test="${errorMessages}">
            <div class="msg-box error">
                <ul>
                    <g:each var="message" in="${errorMessages.take(1)}">
                        <li>
                            ${message}
                            <g:if test="${errorMessages.size() > 1}">
                                <g:set var="errorMessagesBody" value="${errorMessages[1..errorMessages.size()-1]}"/>
                                <g:each var="messageBody" in="${errorMessagesBody}">
                                    <br>
                                    <span>${messageBody.decodeHTML()}</span>
                                </g:each>
                            </g:if>
                        </li>
                    </g:each>
                </ul>
            </div>

            <g:set var="errorMessages" value=""/>
            <ul></ul>
        </g:if>
    </div>

    <div class="table-box tab-table">
        <g:if test="${editedOrderChanges?.data}">
            <div class="table-scroll">
            <table id="editOrderChanges" cellspacing="0" cellpadding="0">
                <tbody>
                <g:each var="orderChange" in="${editedOrderChanges.data}">
                    <g:if test="${!orderChange.change.delete || orderChange.change.delete == 0}">
                        <g:render template="orderChange" model="[ orderChange: orderChange , planItemMap: planItemMap]"/>
                    </g:if>
                </g:each>

                </tbody>
            </table>
            <script type="text/javascript">
                %{-- display or hide details for order change clicked--}%
                function showEditChangeDetails(element) {
                    var id = $(element).attr('id').substring('edit_change_header_'.length);
                    if ($('#edit_change_details_' + id).is(':visible')) {
                        $('#edit_change_details_' + id).hide();
                        $(element).removeClass('active');
                    } else {
                        $('#edit_change_details_' + id).show();
                        $(element).addClass('active');
                    }
                }

                <%-- Create a dialog --%>
                $( "#assets-dialog-update" ).dialog({
                    title: "${g.message([code: 'assets.dialog.choose.title'])}" ,
                    autoOpen: false,
                    width: 580,
                    modal: true,
                    dialogClass: "no-close"
                });

                <%-- Close the dialog. Function gets called by the close button in the dialog --%>
                function closeAssetsupdateDialog(event) {
                    event.preventDefault();
                    $( '#assets-dialog-update' ).dialog( "close" );
                    $( '#assets-box-update' ).empty();
                }

                function forceSelectAssets() {
                   <g:remoteFunction controller='orderBuilder'  action= 'edit'
                                        method= 'GET'
                                        update= 'assets-box-update'
                                        params= "['_eventId':'initUpdateAssets','id':forceDisplayAssetsDialogForChangeId]"/>
                }

                <g:if test="${forceDisplayAssetsDialogForChangeId}">
                    // force show assets dialog
                    forceSelectAssets();
                </g:if>

            </script>
        </div>
        </g:if>
    </div>
</div>