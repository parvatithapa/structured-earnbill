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

<div class="column-hold">
    <div class="heading">
        <strong>
            ${selected.name}
        </strong>
    </div>

    <div class="box">
        <div class="sub-box">
            <table class="dataTable" cellspacing="0" cellpadding="0">
                <tbody>
                <tr>
                    <td><g:message code="ratingScheme.id"/></td>
                    <td class="value">${selected?.id}</td>
                </tr>
                <tr>
                    <td><g:message code="ratingScheme.name"/></td>
                    <td class="value">${selected?.name}</td>
                </tr>
                <tr>
                    <td><g:message code="ratingScheme.initial.increment"/></td>
                    <td class="value">${selected?.initialIncrement}</td>
                </tr>
                <tr>
                    <td><g:message code="ratingScheme.initial.rounding.mode"/></td>
                    <g:if test="${selected?.initialRoundingMode==1}">
                        <g:set var="initialRoundingMode" value="ROUND DOWN"></g:set>
                    </g:if>
                    <g:if test="${selected?.initialRoundingMode==0}">
                        <g:set var="initialRoundingMode" value="ROUND UP"></g:set>
                    </g:if>
                    <g:if test="${selected?.initialRoundingMode==4}">
                        <g:set var="initialRoundingMode" value="HALF ROUND UP"></g:set>
                    </g:if>
                    <g:if test="${selected?.initialRoundingMode==5}">
                        <g:set var="initialRoundingMode" value="HALF ROUND DOWN"></g:set>
                    </g:if>
                    <td class="value">${initialRoundingMode}</td>
                </tr>
                <tr>
                    <td><g:message code="ratingScheme.main.increment"/></td>
                    <td class="value">${selected?.mainIncrement}</td>
                </tr>
                <tr>
                    <td><g:message code="ratingScheme.main.rounding.mode"/></td>
                    <g:if test="${selected?.mainRoundingMode==1}">
                        <g:set var="mainRoundingMode" value="ROUND DOWN"></g:set>
                    </g:if>
                    <g:if test="${selected?.mainRoundingMode==0}">
                        <g:set var="mainRoundingMode" value="ROUND UP"></g:set>
                    </g:if>
                    <g:if test="${selected?.mainRoundingMode==4}">
                        <g:set var="mainRoundingMode" value="HALF ROUND UP"></g:set>
                    </g:if>
                    <g:if test="${selected?.mainRoundingMode==5}">
                        <g:set var="mainRoundingMode" value="HALF ROUND DOWN"></g:set>
                    </g:if>
                    <td class="value">${mainRoundingMode}</td>
                </tr>
                <g:if test="${selected?.global}">
                    <tr>
                        <td><g:message code="ratingScheme.mediation.assign.global"/></td>
                        <td class="value"><g:checkBox name="global" class="cb checkbox" checked="true" disabled="true" /></td>
                    </tr>
                </g:if>
                </tbody>
            </table>

            <!-- Associated Companies -->
            %{--<g:if test="${selected?.associations.size()>0}">--}%
                <div class="box-cards box-cards-open">
                    <div class="box-cards-title">
                        <span><g:message code="ratingScheme.associations.title"/></span>
                    </div>
                    <div class="box-card-hold">
                        <table class="dataTable text-left" cellspacing="0" cellpadding="0" width="100%">
                            <thead>
                            <tr class="dependency-th">
                                <th><g:message code="ratingScheme.associated.mediation"/></th>
                                <th><g:message code="ratingScheme.associated.joblauncher"/></th>
                                <th><g:message code="ratingScheme.associated.company"/></th>
                            </tr>
                            </thead>
                            <tbody>
                            <%-- items --%>
                            <g:each var="association" in="${selected?.associations}">
                                <tr class="dependency-tr medium-width">
                                    <td class="value">${association.mediation.name}</td>
                                    <td class="value">${association.mediation.mediationJobLauncher}</td>
                                    <td class="value">${association.company.description}</td>
                                </tr>
                            </g:each>
                            </tbody>
                        </table>
                    </div>
                </div>
            %{--</g:if>--}%

        </div>
    </div>

    <g:hiddenField name="ratingSchemeId" value="${selected.id}"/>
    <div id="test">
        <g:render template="test" model="[callDuration: callDuration, resultQuantity: resultQuantity]"/>
    </div>

    <div class="btn-box">
        <div class="row">
            <g:remoteLink class="submit add" id="${selected.id}" action="edit" update="column2">
                <span><g:message code="button.edit"/></span>
            </g:remoteLink>
            <a onclick="showConfirm('delete-${selected.id}');" class="submit delete"><span><g:message code="button.delete"/></span></a>
        </div>
    </div>

    <g:render template="/confirm"
              model="['message': 'config.rating.unit.delete.confirm',
                      'controller': 'mediationRatingSchemeConfig',
                      'action': 'delete',
                      'id': selected.id,
              ]"/>


    <script type="text/javascript">

        function callTestRatingScheme(){
            var callDuration = $('#callDuration').val();
            var ratingSchemeId = $('#ratingSchemeId').val();
            $.ajax({
                url: '${createLink(controller: 'mediationRatingSchemeConfig', action: 'test')}',
                data: {ratingSchemeId: ratingSchemeId, callDuration: callDuration},
                cache: false,
                success: function(html) {
                    $('div[id="test"]').html(html);
                }
            });
        }

    </script>

</div>
