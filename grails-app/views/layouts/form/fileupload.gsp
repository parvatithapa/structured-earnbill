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

<%--

  Usage:

    <g:applyLayout name="form/fileupload">
        <content tag="input.name">input attr name</content>
    </g:applyLayout>

--%>
<%@page import="com.sapienter.jbilling.server.util.LogoType" defaultCodec="none" %>
<div id="file-upload-cont-${pageProperty(name: 'page.input.name')}" style="padding-right: 10px;">
    <input  type = "file"
            name = "${pageProperty(name: 'page.input.name')}"
              id = "${pageProperty(name: 'page.input.name')}"
           class = "inputfile"
           ${pageProperty(name: 'page.update.image')}>
    <label id="fileupload" for="${pageProperty(name: 'page.input.name')}" class="fileupload" style="">
        <span class="placeholder">
            <span class="placeholder-text" id="file-name-${pageProperty(name: 'page.input.name')}">
                <g:message code="file.upload.nofile"/>
            </span>
            <a id="file-remove-${pageProperty(name: 'page.input.name')}" class="file-cancel" style="font-family: AppDirectIcons">
                &nbsp;&#xe000;
            </a>
        </span>
        <span class="filebutton">
            <g:message code="file.upload.choose.file"/>
        </span>
    </label>
</div>

<script>
    $(document).ready(function() {
        $('#file-remove-${pageProperty(name: 'page.input.name')}').click(function(event){
            removeFile_${pageProperty(name: 'page.input.name')}();
            event.preventDefault();
        });

        $('#file-upload-cont-${pageProperty(name: 'page.input.name')} input[type="file"]').change(function() {
            var fileName = this.value.split( '\\' ).pop();
            var label = $('#file-name-${pageProperty(name: 'page.input.name')}');

            if( fileName ) {
                label.text( fileName );
                $('#file-remove-${pageProperty(name: 'page.input.name')}').show();
                <g:ifPageProperty name="page.reset.default">
                    $('input[name="${pageProperty(name: 'page.reset.default')}"]').val('false');
                </g:ifPageProperty>
            }
        });

        $('#file-remove-${pageProperty(name: 'page.input.name')}').hide();
    });

    function removeFile_${pageProperty(name: 'page.input.name')}() {
        $('#file-upload-cont-${pageProperty(name: 'page.input.name')} input[type="file"]').val('');
        $('#file-name-${pageProperty(name: 'page.input.name')}').text('No file chosen');
        $('#file-remove-${pageProperty(name: 'page.input.name')}').hide();

        <g:ifPageProperty name="page.img.src">
            $('#${pageProperty(name: 'page.img.id')}').attr('src', ${pageProperty(name: 'page.img.src')});
        </g:ifPageProperty>
    }

    <g:ifPageProperty name="page.update.image">
        function updateImage(input, imgId) {
            if (input.files && input.files[0]) {
                if ("${LogoType.getExtensionsAsString()}".indexOf(input.files[0].type) !== -1) {
                    var reader = new FileReader();
                    reader.onload = function (e) {
                        $('#' + imgId).attr('src', e.target.result);
                    }

                    reader.readAsDataURL(input.files[0]);
                } else {
                    $('#' + imgId).attr('src', "${createLink(controller: 'config', action: 'notFoundLogo')}");
                }
            }
        };
    </g:ifPageProperty>
</script>
