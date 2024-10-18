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

<div class="heading">
    <strong><g:message code="route.test"/></strong>
</div>

<div class="box">
    <div class="sub-box">
        <g:formRemote id="tree-route-test" name="tree-route-test"
                      url="[controller: 'routeTest', action: 'testTreeRoute']"
                      update="column2">

            <g:applyLayout name="form/select">
                <content tag="label"><g:message code="mediation.config.rootRoute"/></content>
                <content tag="label.for">rootRoute</content>
                <g:select from="${routes}"
                          optionKey="id"
                          optionValue="${{it?.name}}"
                          name="rootRoute"/>
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
        <ul>
            <li>
                <a class="submit save button-primary" onclick="$('#tree-route-test').submit();">
                    <span><g:message code="button.test"/></span>
                </a>
            </li>
        </ul>
    </div>

</div>
