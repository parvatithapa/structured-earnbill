/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

// This function can be used to change active UI tab programmatically without click event fire
// This can be useful during updating inactive tab content by ajax call to activate target tab on ajax request complete
function showTabWithoutClickIfNeeded(tabName) {
    var header = $('li[aria-controls=\'' + tabName + '\'] a');
    $(header).click();

}