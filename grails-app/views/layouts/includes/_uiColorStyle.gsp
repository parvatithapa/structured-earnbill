<%@ page import="org.codehaus.groovy.grails.io.support.FileSystemResource; com.sapienter.jbilling.server.util.ColorConverter"%>

${ColorConverter?.setBaseColor()}
<g:set var="COLOR_PRIMARY" value="${ColorConverter?.convert(ColorConverter?.DIFF_BASE)}"/>
<g:set var="COLOR_PRIMARY_DARKER" value="${ColorConverter?.convert(ColorConverter?.DIFF_PRIMARY_DARKER)}"/>
<g:set var="COLOR_PRIMARY_LIGHTER" value="${ColorConverter?.convert(ColorConverter?.DIFF_PRIMARY_LIGHTER)}"/>
<g:set var="COLOR_PRIMARY_HOVER_BACKGROUND" value="${ColorConverter?.convert(ColorConverter?.DIFF_PRIMARY_HOVER_BACKGROUND)}"/>
<g:set var="COLOR_PRIMARY_HOVER_BORDER" value="${ColorConverter?.convert(ColorConverter?.DIFF_PRIMARY_HOVER_BORDER)}"/>
<g:set var="COLOR_PRIMARY_HOVER_BOX_SHADOW" value="${ColorConverter?.convert(ColorConverter?.DIFF_PRIMARY_HOVER_BOX_SHADOW)}"/>
<g:set var="COLOR_PRIMARY_SHADOW" value="${ColorConverter?.toRgb(ColorConverter?.DIFF_PRIMARY_SHADOW)}"/>

<g:set var="COLOR_SECONDARY_BACKGROUND" value="${ColorConverter?.convert(ColorConverter?.DIFF_SECONDARY_BACKGROUND)}"/>
<g:set var="COLOR_SECONDARY_BORDER" value="${ColorConverter?.convert(ColorConverter?.DIFF_SECONDARY_BORDER)}"/>
<g:set var="COLOR_SECONDARY_BOX_SHADOW" value="${ColorConverter?.convert(ColorConverter?.DIFF_SECONDARY_BOX_SHADOW)}"/>
<g:set var="COLOR_SECONDARY_BOX_SHADOW2" value="${ColorConverter?.toRgb(ColorConverter?.DIFF_SECONDARY_BOX_SHADOW2)}"/>


<g:set var="COLOR_SECONDARY_HOVER_BACKGROUND" value="${ColorConverter?.convert(ColorConverter?.DIFF_SECONDARY_HOVER_BACKGROUND)}"/>
<g:set var="COLOR_SECONDARY_HOVER_BORDER" value="${ColorConverter?.convert(ColorConverter?.DIFF_SECONDARY_HOVER_BORDER)}"/>
<g:set var="COLOR_SECONDARY_HOVER_BOX_SHADOW" value="${ColorConverter?.convert(ColorConverter?.DIFF_SECONDARY_HOVER_BOX_SHADOW)}"/>

<g:set var="COLOR_TABLE_HOVER_BACKGROUND" value="${ColorConverter?.convert(ColorConverter?.DIFF_TABLE_HOVER_BACKGROUND)}"/>
<g:set var="COLOR_TABLE_HOVER_BORDER" value="${ColorConverter?.convert(ColorConverter?.DIFF_TABLE_HOVER_BORDER)}"/>
<g:set var="COLOR_HEADER_SEARCH_HOVER" value="${ColorConverter?.convert(ColorConverter?.DIFF_HEADER_SEARCH_HOVER)}"/>
<g:set var="COLOR_BOX_EDIT_HOVER" value="${ColorConverter?.convert(ColorConverter?.DIFF_BOX_EDIT_HOVER)}"/>

<g:set var="COLOR_HEADER_BACKGROUND" value="${ColorConverter?.convertIfNotBase(ColorConverter?.DIFF_HEADER_BACKGROUND, '#f5f5f5')}"/>
<g:set var="COLOR_HEADER_BORDER" value="${ColorConverter?.convertIfNotBase(ColorConverter?.DIFF_HEADER_BORDER, '#cbcbcb')}"/>
<g:set var="COLOR_HEADER_COLOR" value="${ColorConverter?.ifBase('#9c9c9c', '#343434')}"/>

<style type="text/css">

.button-primary, a.button-primary {
    text-shadow: 0px -1px 0px ${COLOR_PRIMARY_DARKER};
    border: 1px solid ${COLOR_PRIMARY_DARKER};
    background: ${COLOR_PRIMARY};
    box-shadow: inset 0 1px 0 0 ${COLOR_PRIMARY_LIGHTER}, 0 2px 2px 0 rgba(${COLOR_PRIMARY_SHADOW}, 0.1);
}

.button-primary:hover, .button-primary:focus {
    border-color: ${COLOR_PRIMARY_HOVER_BORDER};
    background-color: ${COLOR_PRIMARY_HOVER_BACKGROUND};
    box-shadow: inset 0 1px 0 0 ${COLOR_PRIMARY_HOVER_BOX_SHADOW}  , 0 2px 3px 0 rgba(${COLOR_PRIMARY_SHADOW}, 0.3);
}
/*006080*/
.button-secondary {
    text-shadow: 0px -1px 0px ${COLOR_SECONDARY_BORDER};
    border: 1px solid ${COLOR_SECONDARY_BORDER};
    background: ${COLOR_SECONDARY_BACKGROUND};
    box-shadow: inset 0 1px 0 0 ${COLOR_SECONDARY_BOX_SHADOW}, 0 2px 2px 0 rgba(${COLOR_SECONDARY_BOX_SHADOW2}, 0.1);
}

.button-secondary:focus, .button-secondary:hover {
    border-color: ${COLOR_SECONDARY_HOVER_BORDER};
    background-color: ${COLOR_SECONDARY_HOVER_BACKGROUND};
    box-shadow: inset 0 1px 0 0 ${COLOR_SECONDARY_HOVER_BOX_SHADOW}, 0 2px 3px 0 rgba(${COLOR_SECONDARY_BOX_SHADOW2}, 0.3);
}

.heading, .heading, .ui-widget-header {
    background-color: ${COLOR_HEADER_BACKGROUND};
    border-color: ${COLOR_HEADER_BORDER};
    color: ${COLOR_HEADER_COLOR};
}

a:focus, a:hover {
    color: ${COLOR_PRIMARY};
}

a {
    color: ${COLOR_SECONDARY_BACKGROUND};
}

.list li a:hover {
    color: ${COLOR_PRIMARY};
}

.column .box.edit.hover, .column .box.edit:hover {
    background: ${COLOR_BOX_EDIT_HOVER};
}


#navigation ul ul li.not-active:hover span {
    background-color: ${COLOR_SECONDARY_BACKGROUND};
}

#navigation ul ul li:hover, #navigation ul ul li a:hover span {
    background-color: ${COLOR_SECONDARY_BACKGROUND};
}

#header .search .popup .input-row label:hover, #header .search .popup .input-row label.hover {
    color: ${COLOR_HEADER_SEARCH_HOVER};
}

#home-logo a {
    background: url(${logoLink(favicon:false)}) no-repeat;
    background-position: 50%;
    background-size: auto 40px;
}

input.autoFill {
    background-color: ${COLOR_TABLE_HOVER_BACKGROUND} !important;
    border: 1px solid ${COLOR_PRIMARY_HOVER_BORDER} !important;
}
.table-box tbody tr:hover td, .table-box tbody tr:hover:before, .table-box tbody tr:hover:after, .table-box tbody tr.active td, .table-box tbody tr.active:before, .table-box tbody tr.active:after {
    border-color: ${COLOR_TABLE_HOVER_BORDER};
    background-color: ${COLOR_TABLE_HOVER_BACKGROUND};
}
</style>
