<div id="confirm-dialog-save-permissions" class="bg-lightbox" title="<g:message code="popup.confirm.title"/>" style="display:none;">
    <!-- confirm dialog content body -->
    <table>
        <tbody>
            <tr>
                <td valign="top">
                    <img src="${resource(dir:'images', file:'icon34.gif')}" alt="confirm">
                </td>
                <td class="col2">
                    <p id="confirm-dialog-save-permissions-msg">
                        <g:message code="role.popup.confirmation" />
                    </p>
                </td>
            </tr>
        </tbody>
    </table>
</div>

<script type="text/javascript">
    $(function() {
        setTimeout(function() {
            $('#confirm-dialog-save-permissions.ui-dialog-content').remove();
            $('#confirm-dialog-save-permissions').dialog({
                autoOpen: false,
                height: 200,
                width: 375,
                modal: true,
                buttons: {
                    '<g:message code="prompt.yes"/>': function() {
                        $(this).dialog('close');
                        $('#role-edit-form').submit();
                    },
                    '<g:message code="prompt.no"/>': function() {
                        $(this).dialog('close');
                    }
                }
            });
        }, 100);
    });
</script>

