// In order to make any changes in the below  raw html please use receipt.html file and convert it's content to a single line
var rawHtmlWrapper = '<!DOCTYPE html> <html> <head> <style> @page { margin: 0; } #cssTable{margin-left: auto; margin-right: auto; width: 100%; height: 100%;}.common{border: 1px solid gray; padding: 10px; border-radius: 10px;} .common-right{border: 1px solid gray; margin-right: 45px; padding: 10px; border-radius: 10px;} th, td{padding: 7px; text-align: right; font-size: 18.3px;}.commonTable{margin-right: 0; margin-left: auto; width: 100%;} td{width: 55%; word-break: break-word; vertical-align: top; font-weight: bold;} th{width: 45%;} .outerDiv{margin-top: 1.6cm; margin-left: 0.6cm; width: 960px; height: 300px; padding-top: 100px;display: inherit;} </style></head> <body> <div id="wrapper"> #TABLE_CONTENT </div> </body></html>';
var rawTableWrapper = '<div class="outerDiv"> <table id="cssTable"> <tr> <td> <div class="common"> <table class="commonTable" > <tr> <td>#VALUE_SUB_NAME</td> <th>#LABEL_SUB_NAME</th> </tr> <tr> <td>#VALUE_ADDRESS</td> <th>#LABEL_ADDRESS</th> </tr> <tr> <td>#VALUE_CONTACT_NUM</td> <th>#LABEL_CONTACT_NUM</th> </tr> <tr> <td>#VALUE_EMAIL</td> <th>#LABEL_EMAIL</th> </tr> <tr> <td> </td> <th> </th> </tr> </table> </div> </td> <td> <div class="common-right"> <table class="commonTable"> <tr> <td>#VALUE_RECEIPT_NUM</td> <th>#LABEL_RECEIPT_NUM</th> </tr> <tr> <td>#VALUE_RECEIPT_DATE</td> <th>#LABEL_RECEIPT_DATE</th> </tr> <tr> <td>#VALUE_CUST_ACCT_NUM</td> <th>#LABEL_CUST_ACCT_NUM</th> </tr> <tr> <td>#VALUE_SUB_DIAL_NUM</td> <th>#LABEL_DIAL_NUM</th> </tr> <tr> <td>#VALUE_TYPE_OF_RECEIPT</td> <th>#LABEL_TYPE_OF_RECEIPT</th> </tr> </table> </div> </td> </tr> <tr> <td> <div class="common"> <table class="commonTable"> <tr> <td>#VALUE_WALLET_AMT</td> <th>#LABEL_WALLET_AMT</th> </tr> <tr> <td>#VALUE_ACTUAL_PLAN_PRICE</td> <th>#LABEL_ACTUAL_PLAN_PRICE</th> </tr> </table> </div> </td> <td> <div class="common-right"> <table class="commonTable"> <tr> <td>#VALUE_PAYMENT_AMT</td> <th>#LABEL_PAYMENT_AMT</th> </tr> <tr> <td>#VALUE_OPERATION_TYPE</td> <th>#LABEL_OPERATION_TYPE</th> </tr> </table> </div> </td> </tr> </table> <table> <tr> <td> <div> <table> <tr> <th>#LABEL_COLLECTOR_NAME</th> </tr> <tr> <td>#VALUE_COLLECTOR_NAME</td> </tr> </table> </div> </td> <td> </td> </tr> </table> </div>';
var pageBreak = '<div style="page-break-after: always;"></div>';

let placeHolderAndElementIds = [
      "#VALUE_SUB_NAME:valueSubName",
      "#LABEL_SUB_NAME:labelSubName",
      "#VALUE_ADDRESS:valueAddress",
      "#LABEL_ADDRESS:labelAddress",
      "#VALUE_CONTACT_NUM:valueContactNumber",
      "#LABEL_CONTACT_NUM:labelContactNumber",
      "#VALUE_EMAIL:valueEmail",
      "#LABEL_EMAIL:labelEmail",
      "#VALUE_RECEIPT_NUM:valueReceiptNumber",
      "#LABEL_RECEIPT_NUM:labelReceiptNumber",
      "#VALUE_RECEIPT_DATE:valueReceiptDate",
      "#LABEL_RECEIPT_DATE:labelReceiptDate",
      "#VALUE_CUST_ACCT_NUM:valueCustAccNum",
      "#LABEL_CUST_ACCT_NUM:labelCustAccNum",
      "#VALUE_SUB_DIAL_NUM:valueSubDialNum",
      "#LABEL_DIAL_NUM:labelSubDialNum",
      "#VALUE_TYPE_OF_RECEIPT:valueReceiptType",
      "#LABEL_TYPE_OF_RECEIPT:labelReceiptType",
      "#VALUE_PAYMENT_AMT:valuePaymentAmount",
      "#LABEL_PAYMENT_AMT:labelPaymentAmount",
      "#VALUE_OPERATION_TYPE:valueOperationType",
      "#LABEL_OPERATION_TYPE:labelOperationType",
      "#VALUE_COLLECTOR_NAME:valueCollectorName",
      "#LABEL_COLLECTOR_NAME:labelCollectorName",
      "#VALUE_WALLET_AMT:valueWalletAmount",
      "#LABEL_WALLET_AMT:labelWalletAmount",
      "#VALUE_ACTUAL_PLAN_PRICE:valueActualPlanPrice",
      "#LABEL_ACTUAL_PLAN_PRICE:labelActualPlanPrice"
      ];

function printReceipt() {

    var tempTableWrapper='';

    $(".receipt").each(function( index ) {
      console.log(`div${index}: ${this.id}`);
      tempTableWrapper = tempTableWrapper + buildRawHtml(this.id, placeHolderAndElementIds) + pageBreak;
    });

    rawHtmlWrapper = rawHtmlWrapper.replace('#TABLE_CONTENT' , tempTableWrapper);

    let printWindow = window.open('', '', 'height=2480,width=1748');
    printWindow.document.write(rawHtmlWrapper);
    printWindow.document.close();
    printWindow.print();
}

function buildRawHtml(outerDivId, placeHolderAndElementIds) {
    let tempTable = rawTableWrapper;
    for(let i=0; i<placeHolderAndElementIds.length; i++){
         let placeHolderAndElementId = placeHolderAndElementIds[i].split(":");
         let placeHolder = placeHolderAndElementId[0];
         let id = placeHolderAndElementId[1];
         let value = $("#" + outerDivId + ' #'+ id).html();
         tempTable = tempTable.replace(placeHolder , value);
    }
    return tempTable;
}

