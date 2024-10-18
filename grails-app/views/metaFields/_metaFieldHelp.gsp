<g:if test="${field?.helpContentURL || field.helpDescription}">
    <style>.form-columns .inp-bg input.field {
        width: 90%;
    }
    .form-columns a img{
        top: 0;
    }
    </style>
</g:if>

<g:if test="${field?.helpContentURL || field.helpDescription}">
    <g:if test="${field?.helpContentURL}">
        <div id="metafield-dialog-${field.id}">
            <iframe src="${field?.helpContentURL}" width="650" height="500"></iframe>
        </div>
        <script>
            $(function(){
                $( "#metafield-dialog-${field.id}" ).dialog({
                    title: "${g.message([code: 'metafield.help.popup.heading'])}" ,
                    autoOpen: false,
                    width: 650,
                    modal: true,
                    dialogClass: "no-close"
                });
            });
        </script>
    </g:if>
    <a href="javascript:void(0)" onclick="$('#metafield-dialog-${field.id}').dialog('open')"><img src="${resource(dir: 'images', file: 'help.gif')}" width="17" height="17" title="${field?.helpDescription}"/></a>
</g:if>