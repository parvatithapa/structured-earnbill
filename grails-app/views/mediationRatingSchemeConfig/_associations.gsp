<%-- items --%>
<g:each var="association" in="${associations}">
    <g:set var="trId" value="association.${association.mediation.id}.${association.company.id}"/>
    <tr class="dependency-tr medium-width" id="${trId}">
        <td>${association.mediation.name}</td>
        <td>${association.company.description}</td>
        <td>
            <a href="#" class="plus-icon" onclick="removeDependency('${trId}'); return false;" >&nbsp;&#xe000;</a>
        </td>
        <g:hiddenField name="associations.mediation.id" value="${association.mediation.id}"/>
        <g:hiddenField name="associations.company.id" value="${association.company.id}"/>
        <g:hiddenField name="associations.ratingScheme" value="${association.ratingScheme}"/>
        <g:hiddenField name="associations.id" value="${association.id}"/>
    </tr>
</g:each>
