<%@ page import="com.sapienter.jbilling.server.ediTransaction.TransactionType; org.apache.commons.lang.StringUtils; com.sapienter.jbilling.client.util.Constants" %>
%{--<div class="heading">
    <strong><g:message code="route.test.results.title"/></strong>
</div>--}%

<div class="box">
    <div class="sub-box">
        <fieldset>
            <div class="form-columns">
                <g:if test="${fileType.equals(com.sapienter.jbilling.server.ediTransaction.TransactionType.OUTBOUND.name())}">
                    <g:if test="${wsList}">
                        <g:each in="${wsList}" var="ediFileRecordWS">
                            <span class="tooltip" title="Key: ${ediFileRecordWS.header}" style="color: #9ec91d">${ediFileRecordWS.header}!</span>

                            <g:set var="prefixOrder" value="${0}"/>
                            <g:each in="${ediFileRecordWS.ediFileFieldWSes}" var="ediFileFieldWS" status="index">
                                <g:set var="separator" value="${ediFileRecordWS.totalFileField==ediFileFieldWS.order+1?'':'!'}"/>
                                <g:set var="order" value="${ediFileFieldWS.order}"/>
                                <g:if test="${order - prefixOrder > 0}">
                                    <g:if test="${order - prefixOrder > 1}">
                                        <g:each in="${prefixOrder..(order -2)}" var="idx">
                                            <span class="tooltip" title="Not Used" style="color: #C96A27">${separator}</span>
                                        </g:each>
                                    </g:if>
                                </g:if>
                                <g:if test="${ediFileFieldWS.getValue()}">
                                    <span class="tooltip" title="${ediFileFieldWS.key} : ${ediFileFieldWS.value}">${ediFileFieldWS.getValue()}${separator}</span>
                                </g:if>
                                <g:else test="${ediFileFieldWS.comment}">
                                    <g:if test="${ediFileFieldWS.comment}">
                                        <span class="tooltip" title="Key: ${ediFileFieldWS.key} \n ${ediFileFieldWS.comment}" style="color: #c91819">${separator}</span>
                                    </g:if>
                                    <g:else>
                                        <span class="tooltip" title="Key: ${ediFileFieldWS.key}" style="color: #2d28e6">${separator}</span>
                                    </g:else>

                                </g:else>
                                <g:set var="prefixOrder" value="${order}"/>
                            </g:each>
                            <span class="tooltip" title="Not Used" style="color: #C96A27">${StringUtils.repeat(" !",ediFileRecordWS.totalFileField-2-prefixOrder)}</span>
                            <br/>
                        </g:each>

                    </g:if>
                </g:if>
                <g:elseif test="${wsList}">
                    <g:each in="${wsList}" var="ediFileRecordWS">
                        ${ediFileRecordWS.header}
                        <g:if test="${ediFileRecordWS.ediFileFieldWSes.length > 0}">
                            <div class="table-area" style="font-size: 10px;padding: 0px 0px;white-space: nowrap;">


                                <table>
                                    <thead>
                                    <tr>
                                        <th class="first" style="width: 20%">Key</th>
                                        <th style="width: 20%">Value</th>
                                        <th style="width: 55%">comment</th>
                                        <th class="last" style="width: 5%">order</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    <g:each in="${ediFileRecordWS.ediFileFieldWSes}" var="fileFieldWS">

                                        <tr>
                                            <td>${fileFieldWS.key}</td>
                                            <td>${fileFieldWS.value}</td>
                                            <td>${fileFieldWS.comment}</td>
                                            <td>${fileFieldWS.order}</td>
                                        </tr>

                                    </g:each>
                                    </tbody>
                                </table>
                            </div>
                        </g:if>
                        <br/><br/>
                    </g:each>
                </g:elseif>

            </div>
        </fieldset>
    </div>
<div class="btn-box buttons">

</div>
</div>
<script type="text/javascript">
    $(function(){
        $(".tooltip").tooltip();
    });
</script>
