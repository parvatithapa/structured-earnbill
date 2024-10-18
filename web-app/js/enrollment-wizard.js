/**
 * Created by neeraj on 16/8/15.
 */




/*var wizard={};*/

var wizard={

    wizardElement:$("#tabs"),
    nextButtonName:".next-btn",
    saveButtonName:".save-btn",
    reviewButtonName:".review-btn",
    previousButtonName:".prev-btn",
    nextButton:$(".next-btn"),
    saveButton:$(".save-btn"),
    reviewButton:$(".review-btn"),
    completeButton:$(".complete-btn"),
    previousButton:$(".prev-btn"),
    form:$(".customer-enrollment-form"),
    totalTabs:$("#tabs li").size(),


    init:function(){
        wizard.wizardElement.tabs({
            activate: function (event, ui) {
                var selected = wizard.wizardElement.tabs("option", "active");

                if(selected+1 < wizard.totalTabs){
                    wizard.wizardElement.tabs("disable", wizard.totalTabs-1)
                }

                if(selected+1==wizard.totalTabs){

                    wizard.reviewButton.hide();
                    wizard.saveButton.hide();
                    wizard.completeButton.show();
                }
                if(selected+1 < wizard.totalTabs){
                    wizard.reviewButton.show();
                    wizard.saveButton.show();
                    wizard.completeButton.hide();
                }
            }
        });
        wizard.wizardElement.tabs( "disable",  wizard.totalTabs-1);

        $(document).on("click",wizard.nextButtonName, function(){
            var selected = wizard.wizardElement.tabs("option", "active");
            if(selected+1==wizard.totalTabs-1){
                wizard.reviewButton.click();
            }else{
                wizard.next();
            }

        });

        $(document).on("click", wizard.previousButtonName, function(){
            wizard.previous();
        });

        $(document).on("click",wizard.reviewButtonName, function(){
            wizard.validateThenReview();
        });
    },
    next:function(){
        var selected = wizard.wizardElement.tabs("option", "active");
        if(selected+1==wizard.totalTabs-1){
            wizard.wizardElement.tabs("enable", wizard.totalTabs-1)
        }
        wizard.wizardElement.tabs("option", "active", selected + 1);
    },
    previous:function(){
        var selected = wizard.wizardElement.tabs("option", "active");
        wizard.wizardElement.tabs("option", "active", selected -1);
    },
    validateThenReview:function(){
        $.ajax({
            url:"/jbilling/customerEnrollment/validateEnrollment",
            data:wizard.form.serialize(),
            success:function(data){
               if(data.status=="success"){
                   $(".wizard-content").html(data.content).find('.dateobject').datepicker({showOn: "both", buttonImage: calenderIcon, buttonImageOnly: true});
                    wizard.reviewButton.hide();
                    wizard.saveButton.hide();
                    wizard.completeButton.show();
                    wizard.wizardElement.tabs("enable", wizard.totalTabs-1)
                    wizard.wizardElement.tabs("option", "active", $("#tabs li").size()-1);
                }
            }
        });
    }
}



