<g:set var="id_sub1" value="${''+obj.minimum + ':' + (obj.maximum?obj.maximum:'') + ':' + obj.dependentId}"/>
<g:set var="id_sub2" value="${type?'Types':'Items'}"/>
<g:set var="name" value="${id_sub2+':'+id_sub1}"/>
<g:set var="trId" value="${name+'.'+obj.dependentId}"/>
<tr class="dependency-tr small-width" id="${trId}">
    <td>${obj.dependentId}</td>
    <td>${obj.dependentDescription}</td>
    <td>${obj.minimum}</td>
    <td>${obj.maximum?:''}</td>
    <td>
        <a href="#" class="plus-icon" onclick="removeDependency('${trId}', '${obj.dependentId}', '${obj.dependentDescription}'); return false;" style="float: right; padding-right: 5px;">
            &#xe000;
        </a>
    </td>
    <g:hiddenField name="dependency.${name}" value="${obj.dependentDescription}"/>
</tr>
