
/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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


function moveLeftUp(msId) {
    var $sel = $("#"+msId+"-group-left :selected");
    if($sel.length == 1) $sel.prev().before($sel);
}

function moveLeftDown(msId) {
    var $sel = $("#"+msId+"-group-left :selected");
    if($sel.length == 1) $sel.next().after($sel);
}

function moveSelectedLeft(msId) {
    var $sel = $("#"+msId+"-group-right :selected");
    if($sel.length) $sel.appendTo("#"+msId+"-group-left");
}

function moveSelectedRight(msId) {
    var $sel = $("#"+msId+"-group-left :selected");
    if($sel.length) $sel.appendTo("#"+msId+"-group-right");
}

function moveAllLeft(msId) {
    var $sel = $("#"+msId+"-group-right option");
    if($sel.length) $sel.appendTo("#"+msId+"-group-left");
}

function moveAllRight(msId) {
    var $sel = $("#"+msId+"-group-left option");
    if($sel.length) $sel.appendTo("#"+msId+"-group-right");
}

function updateDLValues(msId) {
    var $ord = $("#"+msId+"-left-order");
    $ord.val("")
    $("#"+msId+"-group-left option").each( function(i, elm) {
        $ord.val( $ord.val() +","+elm.value);
    });

    $ord = $("#"+msId+"-right-order");
    $ord.val("")
    $("#"+msId+"-group-right option").each( function(i, elm) {
        $ord.val($ord.val()+","+elm.value);
    });
}

function registerDisjointListbox() {
    $(".disjoint-listbox").each( function(i, elm) {
        var msId = elm.id;
        if($("#"+msId+"-left-up").length) $("#"+msId+"-left-up").click(function(evtObj) { moveLeftUp(msId); });
        if($("#"+msId+"-left-down").length) $("#"+msId+"-left-down").click(function(evtObj) { moveLeftDown(msId); });
        $("#"+msId+"-to-left").click(function(evtObj) { moveSelectedLeft(msId); });
        $("#"+msId+"-to-left-all").click(function(evtObj) { moveAllLeft(msId); });
        $("#"+msId+"-to-right").click(function(evtObj) { moveSelectedRight(msId); });
        $("#"+msId+"-to-right-all").click(function(evtObj) { moveAllRight(msId); });
        if($("#"+msId+"-form").length) $("#"+msId+"-form").submit(function(evtObj) { updateDLValues(msId); });
    });
}

$(function(){
    registerDisjointListbox();
});