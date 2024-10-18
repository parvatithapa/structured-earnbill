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

<%@ page import="com.sapienter.jbilling.server.util.Constants" %>

<div class="form-edit">
    <div class="heading">
        <strong><g:message code="entity.logo.config.title"/></strong>
    </div>
    <div class="form-hold">
        <g:uploadForm name="save-ui-entity-logo-form" url="[action: 'saveEntityLogo']" useToken="true">
            <g:hiddenField name="logoByDefault" value="false"/>
            <g:hiddenField name="faviconByDefault" value="false"/>

            <fieldset>
                <div class="form-columns">
                    <div class="column-550 single-81">
                        <!-- Navigation bar logo upload -->
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="entity.logo.config.label.logo"/>
                                <a onclick="defaultLogo('logoImg', '${createLink(controller: 'config', action: 'defaultNavBarLogo')}', 'logo')" style="float: right" class="submit">
                                    <g:message code="entity.logo.default"/>
                                </a>
                            </content>
                            <img   src = "${createLink(controller: 'config', action: 'navigationBarLogo')}"
                                   alt = "logo"
                                    id = "logoImg"
                                height = "68"
                                 width = "62"
                                 style = "background-color: #cccccc; border: 1px solid #cacaca"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label">&nbsp;</content>
                            <g:applyLayout name="form/fileupload">
                                <content tag="reset.default">logoByDefault</content>
                                <content tag="input.name">logo</content>
                                <content tag="img.id">logoImg</content>
                                <content tag="img.src">
                                    "${createLink(controller: 'config', action: 'navigationBarLogo')}"
                                </content>
                                <content tag="update.image">
                                    onchange="updateImage(this, 'logoImg');"
                                </content>
                            </g:applyLayout>
                        </g:applyLayout>
                        <br/>
                        <br/>
                        
                        <!-- favicon logo upload -->
                        <g:applyLayout name="form/text">
                            <content tag="label"><g:message code="entity.logo.favicon.config.label.logo"/>
                                <a onclick="defaultLogo('favImg', '${createLink(controller: 'config', action: 'defaultFaviconLogo')}', 'favicon')" style="float: right" class="submit">
                                    <g:message code="entity.logo.default"/>
                                </a>
                            </content>
                            <img   src = "${createLink(controller: 'config', action: 'faviconLogo')}"
                                   alt = "favicon"
                                    id = "favImg"
                                height = "68"
                                 width = "62"
                                 style = "background-color: whitesmoke; border: 1px solid #cacaca"/>
                        </g:applyLayout>

                        <g:applyLayout name="form/text">
                            <content tag="label">&nbsp;</content>
                            <g:applyLayout name="form/fileupload">
                                <content tag="reset.default">faviconByDefault</content>
                                <content tag="input.name">favicon</content>
                                <content tag="img.id">favImg</content>
                                <content tag="img.src">
                                    "${createLink(controller: 'config', action: 'faviconLogo')}"
                                </content>
                                <content tag="update.image">
                                    onchange="updateImage(this, 'favImg');"
                                </content>
                            </g:applyLayout>
                        </g:applyLayout>
                        <br/>
                        
                        <span><g:message code="entity.logo.config.display.logo.text"/></span>
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
                <li><a onclick="$('#save-ui-entity-logo-form').submit();" class="submit save button-primary"><span><g:message code="button.save"/></span></a></li>
                <li><g:link controller="config" action="index" class="submit cancel"><span><g:message code="button.cancel"/></span></g:link></li>
            </ul>
        </div>
    </div>
</div>
<script>
    function defaultLogo(imgId, actionName, logo){
        $('#file-name-'+logo).text("${message(code: 'file.upload.nofile')}");
        $('#'+imgId).attr('src', actionName + "?" + Math.random());
        $('input[name="'+logo+'ByDefault"]').val(true);
        $('#file-remove-'+logo).hide();
        $('#'+logo).val('');
    }
</script>