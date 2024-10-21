%{--
 SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 _____________________

 [2024] Sarathi Softech Pvt. Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is and remains
 the property of Sarathi Softech.
 The intellectual and technical concepts contained
 herein are proprietary to Sarathi Softech
 and are protected by IP copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
--}%

<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_NATIONAL_ID;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_PASSPORT;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_COMPANY_LETTER;" %>
<%@ page import="static com.sapienter.jbilling.server.adennet.AdennetConstants.IDENTIFICATION_TYPE_OFFICIAL_LETTER;" %>

<style>
    #displayIdentificationImage{
        vertical-align: inherit;
        border-color: inherit;
    }

    .modal {
        display: none;
        position: fixed;
        z-index: 1;
        padding-top: 100px;
        padding-bottom: 100px;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        overflow: auto;
        background-color: rgb(0,0,0);
        background-color: rgba(0,0,0,0.9);
    }

    /* Modal Content (image) */
    .modelContent {
        margin-left: 250px;
        margin-right: 250px;
        margin-bottom: 200px;
        display: block;
        width: 60%;
        height: 70%;
    }

    /* The Close Button */
    .close {
        position: absolute;
          top: 50px;
          right: 50px;
          color: #f1f1f1;
          font-size: 40px;
          font-weight: bold;
          transition: 0.3s;
    }

    .close:hover, .close:focus {
        color: #bbb;
        text-decoration: none;
        cursor: pointer;
    }

    .modelContent {
      -webkit-animation-name: zoom;
      -webkit-animation-duration: 0.6s;
      animation-name: zoom;
      animation-duration: 0.6s;
    }

    @-webkit-keyframes zoom {
      from {-webkit-transform:scale(0)}
      to {-webkit-transform:scale(1)}
    }

    @keyframes zoom {
      from {transform:scale(0)}
      to {transform:scale(1)}
    }

</style>

<div id="imageModal" class="modal" oncontextmenu="return false">
    <span class="close" onclick="closeImage()">&times;</span>
</div>

<tr id="displayIdentificationImage">
    <g:if test="${caller!='showOnEdit'}">
        <td>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_NATIONAL_ID}">
                <g:message code="label.customer.national.id"/>
            </g:if>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_PASSPORT}">
                <g:message code="label.customer.passport"/>
            </g:if>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_COMPANY_LETTER}">
                <g:message code="label.company.letter"/>
            </g:if>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_OFFICIAL_LETTER}">
                <g:message code="label.customer.official.letter.document"/>
            </g:if>
        </td>
    </g:if>
    <td>
        <a id="show-identification-image" onclick="displayImage('identificationImage')">
            <g:if test="${identificationType==IDENTIFICATION_TYPE_NATIONAL_ID}">
                <g:message code="label.show.national.id"/>
            </g:if>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_PASSPORT}">
                <g:message code="label.show.passport"/>
            </g:if>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_COMPANY_LETTER}">
                <g:message code="label.show.company.letter"/>
            </g:if>
            <g:if test="${identificationType==IDENTIFICATION_TYPE_OFFICIAL_LETTER}">
                <g:message code="label.show.Official.letter"/>
            </g:if>
        </a>
    </td>
</tr>

<script>

    function closeImage() {
        document.getElementById("imageModal").style.display = "none";
    }

    function displayLink(imageElementId, sectionId){
        var imageText = document.getElementById(imageElementId).contentWindow.document.body.innerHTML;
        if(imageText!=''){
            var showButton = document.getElementById(sectionId);
            showButton.style.display = "table-row";
        }
    }

    function displayImage(imageElementId){
        $('#imageModal').append('<iframe class="modelContent" id="imgModelContent" src="<g:createLink controller="customer" action="viewImage" params="[userId: userId]"/>" onload="applyCssToImage()"></iframe>');
        document.getElementById("imageModal").style.display = "block";
    }

    function applyCssToImage(){
        $('#imgModelContent').contents().find("head").append(
            $("<style type='text/css'>"+"img {"+"width: -webkit-fill-available;"+"} "+"</style>")
        );
        $('#imgModelContent').contents().find('html').attr("oncontextmenu", "return false");
        displayLink('imgModelContent', 'displayIdentificationImage')
    }

</script>
