%{--
  JBILLING CONFIDENTIAL
  _____________________

  [2003] - [2016] Enterprise jBilling Software Ltd.
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Enterprise jBilling Software.
  The intellectual and technical concepts contained
  herein are proprietary to Enterprise jBilling Software
  and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden.
  --}%

<%@ page import="com.sapienter.jbilling.server.util.Constants" %>

    <div class="form-edit">
        <div class="heading">
            <strong><g:message code="config.tools.file.upload.title"/></strong>
        </div>
        <div class="form-hold">
            <g:uploadForm name="file-upload-form" url="[action: 'toolsUploadFile']" useToken="true">
                <fieldset>
                    <div class="form-columns">
                        <div class="column single">

                            <g:applyLayout name="form/input">
                                <content tag="label"><g:message code="config.tools.file.upload.folder"/></content>
                                <content tag="label.for">prefix</content>
                                <g:textField name="folder" class="field" />
                            </g:applyLayout>

                            <g:applyLayout name="form/radio">
                                <content tag="label"><g:message code="config.tools.file.upload.location"/></content>

                                <input type="radio" class="rb" name="file_location" id="relative" value="relative" checked="true"/>
                                <label for="relative" class="rb"><g:message code="config.tools.file.upload.relative"/></label>

                                <input type="radio" class="rb" name="file_location" id="absolute" value="absolute" />
                                <label for="absolute" class="rb"><g:message code="config.tools.file.upload.absolute"/></label>
                            </g:applyLayout>

                            <g:applyLayout name="form/text">
                                <content tag="label">&nbsp;</content>
                                <g:applyLayout name="form/fileupload">
                                    <content tag="input.name">f</content>
                                </g:applyLayout>
                            </g:applyLayout>

                        <!-- spacer -->
                            <div>
                                <br/>&nbsp;
                            </div>
                        </div>
                    </div>
                </fieldset>
            </g:uploadForm>

            <div class="btn-box buttons">
                <ul>
                    <li><a onclick="$('#file-upload-form').submit();" class="submit save button-primary"><span><g:message code="button.upload"/></span></a></li>
                    <li><g:link controller="config" action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link></li>
                </ul>
            </div>
        </div>

    </div>

