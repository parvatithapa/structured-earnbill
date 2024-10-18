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
  messages

  @author Brian Cowdery
  @since  23-11-2010
--%>

<%-- show flash messages if available --%>
<%-- either 'flash.message', 'flash.info', 'flash.warn' or 'flash.error' --%>
<%-- will also print all messages from 'flash.errorMessages' as an unordered list --%>
<div id="messages">

    <g:if test='${session.message}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>
        <div id="session-scs" class="msg-box successfully">
            <strong><g:message code="flash.success.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#session-scs').hide();">&#xe020;</a>
            <p><g:message code="${session.message}" args="${session.args}"/></p>

            <g:set var="message" value="" scope="session"/>
        </div>
    </g:if>

    <g:if test='${session.error}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>

        <div id="session-error" class="msg-box error">
            <strong><g:message code="flash.error.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#session-error').hide();">&#xe020;</a>
            <ul>
                <li><g:message code="${session.error}" args="${session.args}"/></li>
            </ul>
            <g:set var="error" value="" scope="session"/>
        </div>
    </g:if>

    <g:if test='${flash.message}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>

        <div id="flash-msg" class="msg-box successfully">
            <strong><g:message code="flash.success.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#flash-msg').hide();">&#xe020;</a>
            <p><g:message code="${flash.message}" args="${flash.args}"/></p>
        </div>
    </g:if>

    <g:if test='${flash.info}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>

        <div id="flash-info" class="msg-box info">
            <strong><g:message code="flash.info.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#flash-info').hide();">&#xe020;</a>
            <p><g:message code="${flash.info}" args="${flash.args}"/></p>
        </div>
    </g:if>

    <g:if test="${flash.infoMessages}">
        <div id="flash-info" class="msg-box successfully">
            <strong><g:message code="flash.success.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#flash-info').hide();">&#xe020;</a>
            <ul>
                <g:each var="message" in="${flash.infoMessages}">
                    <script type="text/javascript">
                        canReloadMessages = false;
                    </script>

                    <li>${message}</li>
                </g:each>
            </ul>
        </div>
    </g:if>

    <g:if test='${flash.warn}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>

        <div id="flash-warn" class="msg-box warn">
            <strong><g:message code="flash.warn.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#flash-warn').hide();">&#xe020;</a>
            <p><g:message code="${flash.warn}" args="${flash.args}"/></p>
        </div>
    </g:if>

    <g:if test='${flash.error}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>

        <div id="flash-error" class="msg-box error">
            <strong><g:message code="flash.error.title"/></strong>
            <a class="msg-box-close"  onclick="$(this).closest('#flash-error').hide();">&#xe020;</a>
            <g:if test='${flash.errorDetails}'>
                <p><g:message code="${flash.error}" args="${flash.args}"/></p>
                <ul>
                    <g:each var="message" in="${flash.errorDetails}">
                        <li>${message}</li>
                    </g:each>
                </ul>
            </g:if>
            <g:else>
                <ul>
                    <li><g:message code="${flash.error}" args="${flash.args}"/></li>
                </ul>
            </g:else>
        </div>
    </g:if>

    <g:if test="${flash.errorMessages}">
        <div id="flash-errormsg" class="msg-box error">
            <strong><g:message code="flash.error.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#flash-errormsg').hide();">&#xe020;</a>
            <ul>
                <g:each var="message" in="${flash.errorMessages}">
                    <script type="text/javascript">
                        canReloadMessages = false;
                    </script>

                    <li>${message}</li>
                </g:each>
            </ul>
        </div>
    </g:if>

    <g:if test='${flash.invalidToken}'>
        <script type="text/javascript">
            canReloadMessages = false;
        </script>

        <div id="flash-token" class="msg-box error">
            <strong><g:message code="flash.error.title"/></strong>
            <a class="msg-box-close" onclick="$(this).closest('#flash-token').hide();">&#xe020;</a>
            <p><g:message code="${flash.invalidToken}"/></p>
        </div>
    </g:if>

    <%-- clear message once displayed --%>
    <g:set var="message" value="" scope="flash"/>
    <g:set var="info" value="" scope="flash"/>
    <g:set var="infoMessages" value="" scope="flash"/>
    <g:set var="warn" value="" scope="flash"/>
    <g:set var="error" value="" scope="flash"/>
    <g:set var="errorMessages" value="" scope="flash"/>
</div>


