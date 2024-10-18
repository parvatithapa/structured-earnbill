<%--
  Created by IntelliJ IDEA.
  User: vivek
  Date: 1/9/15
  Time: 11:45 AM
--%>

<%@ page import="com.sapienter.jbilling.server.ediTransaction.TransactionType; com.sapienter.jbilling.client.util.Constants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main" />
</head>

<body>

%{--<div style="height: 10%">
    hello
</div>
<content tag="menu.item">routeTest</content>
<content tag="column1">
    s,sdsk
    <g:render template="test"/>
</content>

<content tag="column2">
    <!-- show empty block -->
    <div class="heading"><strong><em><g:message code="route.test.results.title"/></em></strong></div>

    <div class="box"><div class="sub-box"><em><g:message code="route.test.no.results"/></em></div></div>

    <div class="btn-box"></div>
</content>--}%

<div class="heading">
    <strong><g:message code="route.test"/></strong>
</div>

<div class="box">
    <div class="sub-box msg-box">

        <g:formRemote id="edi-test" name="edi-test"
                      url="[controller: 'ediType', action: 'testEdiResult']"
                      update="updateResult">
            <g:hiddenField name="ediTypeId" value="${ediTypeId}"/>
            <g:hiddenField name="fileType"/>

            <g:applyLayout name="form/text">
                <content tag="label">Fields</content>
                <content tag="label.for">Fields</content>
                <g:textArea id="fields" name="fields" rows="10" cols="100"/>
            </g:applyLayout>

            <!-- spacer -->
            <div>
                <br/>&nbsp;
            </div>
        </g:formRemote>
    </div>

    <div class="btn-box buttons">
        <div class="row">
            <div>

                <a class="submit save" id="generateFile" onclick="submitForm(this.id);">
                    <span>Generate</span>
                </a>
                <a class="submit save" id="parseFile" onclick="submitForm(this.id);">
                    <span>Parse</span>
                </a>
            </div>
        </div>
    </div>

</div>
<script type="application/javascript">
    $(function(){
    });
    function submitForm(objId) {
        if(objId == "generateFile") {
            $("#fileType").val("${com.sapienter.jbilling.server.ediTransaction.TransactionType.OUTBOUND}");
        } else {
            $("#fileType").val("${com.sapienter.jbilling.server.ediTransaction.TransactionType.INBOUND}");
        }
        $("#edi-test").submit();
    }
</script>

<br/>
<div class="heading">
    <strong>Result Data</strong>
</div>

<div class="box">
    <div class="sub-box" id="updateResult">
        %{--Data will update here--}%
    </div>
</div>

</body>
</html>