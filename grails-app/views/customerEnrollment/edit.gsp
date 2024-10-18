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


<html>
<head>
    <meta name="layout" content="main"/>
    <style>

    #tabs li, #tabs li, #tabs li:active, #tabs li:hover {
        width: ${ 95/(accountInformationTypes.size()+3) }%;
    }

    #ui-datepicker-div {
        z-index: 1001 !important;
    }

    #tabs li a {
        width: 90%;
        padding: 8px 5%;
    }
        h3 {
            font-size: 16px;
            padding-left:30px;
        }

    .column .row{
        width: 400px;
    }
    .column .row label{
        width: 40%;
    }

    </style>
    <r:require module="wizard"/>
</head>
<body>

<div class="column panel">
    <div class="form-edit">
        <div class="heading">
            <strong><g:message code="customer.enrollment.edit.title" default="Customer Enrollment"/></strong>
        </div>
        <div class="form-hold">
            <g:form class="customer-enrollment-form" action="save">
                <div class="form-columns" style="width: 90%;">
                    <g:hiddenField name="user.accountTypeId" value="${accountTypeId}"/>
                    <g:hiddenField name="parentUserId" value="${userId}"/>
                    <g:hiddenField name="parentEnrollmentId" value="${parentEnrollmentId}"/>
                    <g:hiddenField name="entityId" value="${customerEnrollment?.entityId}"/>
                    <g:hiddenField name="id" value="${customerEnrollment?.id ?: 0}"/>
                    <div id="tabs">
                        <ul>
                            <g:each in="${accountInformationTypes}" var="ait" status="i">
                                <li><a href="#tabs-${i}">${ait.name}</a></li>
                            </g:each>
                            <li><a href="#tabs-${accountInformationTypes.size() }"><g:message
                                    code="customer.enrollment.edit.enrollment.info"/></a></li>
                            <li><a href="#tabs-${accountInformationTypes.size() + 1}"><g:message
                                    code="customer.enrollment.edit.notes.agents"/></a></li>
                            <li><a href="#tabs-${accountInformationTypes.size() + 2}"><g:message
                                    code="customer.enrollment.edit.review"/></a></li>
                        </ul>
                        <div class="wizard-content" style="background-color: #ffffff">
                            <g:render template="edit" model="[accountInformationTypes:accountInformationTypes, customerEnrollment:customerEnrollment, enrollmentMetaFields:enrollmentMetaFields]"/>
                        </div>

                    </div>


                    <div id="searchData">

                    </div>

                </div>

                <div class="buttons">
                    <ul>

                        <li>
                            <g:if test="${i < accountInformationTypes.size()+1}">
                                <g:actionSubmit value="${g.message(code: "customer.enrollment.button.save.and.exit")}"
                                                action="save" name="submit" class="submit save save-btn"/>
                            </g:if>
                        </li>

                        <li>
                            <g:submitButton name="submit" class="submit save complete-btn" style="display: none;"
                                            value="${g.message(code: 'wizard.complete')}"/>
                        </li>

                        <li>
                            <a href="javascript:void(0)" class="submit edit review-btn" style="width: 180px">
                                <span><g:message code="customer.enrollment.validate.and.review"/></span>
                            </a>
                        </li>

                        <li>
                            <g:link action="list" class="submit cancel cancel-btn"><span><g:message
                                    code="button.cancel"/></span></g:link>
                        </li>

                    </ul>
                </div>
            </g:form>

        </div>
    </div>

</div>

</body>

<r:script>
var calenderIcon = "${resource(dir:'images', file:'icon04.gif')}";
    $(function(){
        wizard.init();

        var validator=$(".customer-enrollment-form").validate({
            rules: {
    <g:each in="${validationMessage.keySet()}" var="metaFieldId" status="i">
    'metaField_${metaFieldId}.value' : {
    <g:each in="${validationMessage[metaFieldId].rules.keySet()}" var="rule" status="j">
        '${rule}': "${validationMessage[metaFieldId].rules[rule]}"
        <g:if test="${validationMessage[metaFieldId].rules.keySet().size() - 1 > j}">
            ,
        </g:if>

    </g:each>
    }
    <g:if test="${validationMessage.keySet().size() - 1 > i}">
        ,
    </g:if>

</g:each>
    },
        messages:{
    <g:each in="${validationMessage.keySet()}" var="metaFieldId" status="i">

        'metaField_${metaFieldId}.value':{
        <g:if test="${validationMessage[metaFieldId]?.message}">
            <g:each in="${validationMessage[metaFieldId].message.keySet()}" var="message" status="j">
                '${message}':  '${validationMessage[metaFieldId].message[message]}'
                <g:if test="${validationMessage[metaFieldId].rules.keySet().size() - 1 > j}">
                    ,
                </g:if>
            </g:each>
        </g:if>
        }
        <g:if test="${validationMessage.keySet().size() - 1 > i}">
            ,
        </g:if>

    </g:each>
    },
    highlight: function() {
        $("#messages").html("");
    }
})

$.validator.addMethod(
"regex", function(value, element, regexp) {
var re = new RegExp(regexp);
return this.optional(element) || re.test(value);
},
""
);

    $(".field_usage").on("change", function(){
        if(validator.form()){

             $.ajax({
                 url: '${createLink(action: 'searchCustomerOrEnrollment')}',
                 data: $('.customer-enrollment-form').serialize(),
                 success:function(data){
                  $("#searchData").html(data.content)
                 }

             });
        }
    });

    $("#copyContent").on("change", function(e){
        if($(this).is(":checked")) {
            $.ajax({
                url: '${createLink(action: 'copyBusinessInfoToContactInfo')}',
                data: $('.customer-enrollment-form').serialize(),
                success:function(data){
                    $("#ait-inner-"+$("#ciAitId").val()).html(data);
                    validator.form();
                }
            });
        } else {
            var selectBox="#ait-inner-"+$("#ciAitId").val().concat(' option');
            $("#ait-inner-"+$("#ciAitId").val()).find('input:text').val('');
            $.each($(selectBox), function (key, value) {
                if ($(this).prop('selected')) {
                    $(this).removeAttr('selected');
                }
             });
        }
    });

    $(document).on("click", ".btn-open", function(){
    toggleSlide(this)
    })

});

</r:script>

</html>
