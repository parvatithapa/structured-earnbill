import com.sapienter.jbilling.client.util.SortableCriteria

import java.util.regex.Pattern

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

/**
 * SortableTagLib 
 *
 * @author Brian Cowdery
 * @since 08/06/11
 */
class SortableTagLib {

    static returnObjectForTags = ['sortableParams']

    def remoteSort = { attrs, body ->
        def sort = assertAttribute('sort', attrs, 'remoteSort') as String
        def order = params.sort == sort ? params.order == 'desc' ? 'asc' : 'desc' : null
        def alias = attrs.containsKey('alias') ? attrs.remove('alias') : null
        def fetch = attrs.containsKey('fetch') ? attrs.remove('fetch') : null

        def action = assertAttribute('action', attrs, 'remoteSort') as String
        def controller = params.controller ?: controllerName
        def id = attrs.id ?: params.id
        def eventId = attrs.containsKey('eventId') ? attrs.remove('eventId') : null
        def method = attrs.containsKey('method') ? attrs.remove('method') : null

        def searchParams = attrs.searchParams ?: [:]

        def update = assertAttribute('update', attrs, 'remoteSort') as String

        out << render(template: "/sortable",
                      params: params,
                      model: [
                              sort: sort,
                              order: order,
                              alias: alias,
                              fetch: fetch,
                              action: action,
                              controller: controller,
                              id: id,
                              update: update,
                              eventId: eventId,
                              method: method,
                              searchParams: searchParams,
                              body: body()
                      ]
        )
    }

    def sortableParams = { attrs, body ->
        def urlParameters = assertAttribute('params', attrs, 'sortableParameters') as Map
        def id = attrs.containsKey('id') ? attrs.remove('id') : params.id
        def sort = attrs.containsKey('sort') ? attrs.remove('sort') : params.sort
        def order = attrs.containsKey('order') ? attrs.remove('order') : params.order
        def alias = attrs.containsKey('alias') ? attrs.remove('alias') : params.alias
        def fetch = attrs.containsKey('fetch') ? attrs.remove('fetch') : params.fetch

        if (!urlParameters.containsKey('id')) {
            urlParameters.put('id', id)
        }

        if (!urlParameters.containsKey('sort')) {
            urlParameters.put('sort', sort)
        }

        if (!urlParameters.containsKey('order')) {
            urlParameters.put('order', order)
        }

        if (alias == SortableCriteria.NO_ALIAS) {
            urlParameters.put("alias", SortableCriteria.NO_ALIAS)
        } else {
            alias?.each { k, v ->
                urlParameters.put("alias.${k}", v)
            }
        }

        if (!urlParameters.containsKey('fetch')) {
            urlParameters.put('fetch', fetch)
        }

        return urlParameters
    }

    protected assertAttribute(String name, attrs, String tag) {
        if (!attrs.containsKey(name)) {
            throwTagError "Tag [$tag] is missing required attribute [$name]"
        }
        attrs.remove name
    }
}
