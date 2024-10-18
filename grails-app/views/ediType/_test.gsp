<%@ page import="com.sapienter.jbilling.client.util.Constants" %>
<div class="heading">
    <strong><g:message code="route.test"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <g:formRemote id="tree-route-test" name="tree-route-test"
                      url="[controller: 'ediType', action: 'testEdiResult']"
                      update="column2">

            <g:applyLayout name="form/select">
                <g:applyLayout name="form/select">
                    <content tag="label"><g:message code="edi.conversion.label"/></content>
                    <content tag="label.for">conversionType</content>
                    <g:select from="['Edi to File', 'File to Edi']"
                              name="ediConversion"/>
                </g:applyLayout>
            </g:applyLayout>

            <g:applyLayout name="form/text">
                <content tag="label">Fields</content>
                <content tag="label.for">Fields</content>
                <g:textArea id="fields" name="fields" rows="10" cols="50"/>
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
                <a class="submit save" onclick="$('#tree-route-test').submit();">
                    <span><g:message code="button.test"/></span>
                </a>
            </div>
        </div>
    </div>

</div>