<div class="table-box customer-notes-table">
    <table id="notes" cellspacing="0" cellpadding="0">
        <thead>
        <tr class="ui-widget-header" >
        <th class="first" width="150px"><g:message code="customer.detail.note.form.author"/></th>
        <th width="150px"><g:message code="customer.detail.note.form.createdDate"/></th>
        <th width="150px"><g:message code="customer.detail.note.form.title"/></th>
        <th class="last" width="550px"><g:message code="customer.detail.note.form.content"/></th>
        <th width="50px"><g:message code="customer.detail.note.form.include.notes"/></th>
        </thead>
        <tbody>
        <g:if test="${!isNew}">

            <g:if test="${customerNotes}">
                <g:each in="${customerNotes}">
                    <tr>
                        <g:hiddenField name="notes.noteId" value="${it?.noteId}"/>
                        <td>${it?.user.userName}</td>
                        <td><g:formatDate date="${it?.creationTime}" formatName="date.time.format" timeZone="${session['company_timezone']}"/>  </td>
                        <td>   ${it?.noteTitle} <input type='hidden' name ='notes.noteTitle' value='"+noteTitle.val()+"'> </td>
                        <td><pre class="wrap-text">${it?.noteContent}</pre><input type='hidden' name ='notes.noteContent' value='"+noteContent.val()+"'></td>
                        <td><g:checkBox      id = "notesInInvoice"
                              class = "cb checkbox"
                               name = "notesInInvoice"
                            checked = "${it?.notesInInvoice}"/></td>
                    </tr>
                </g:each>

            </g:if>
            <g:else>
                <p><em><g:message code="customer.detail.note.empty.message"/></em></p>
            </g:else>

        </g:if>
        </tbody>
    </table>

    <div class="row">
        <jB:remotePaginate controller="customerInspector" action="subNotes" total="${customerNotesTotal}" update="test" params="[id:user.id]"/>
    </div>
</div>
