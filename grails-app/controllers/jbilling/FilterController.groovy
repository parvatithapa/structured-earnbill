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

package jbilling

import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional

/**
 * FilterController
 *
 * @author Brian Cowdery
 * @since  03-12-2010
 */
@Secured(["isAuthenticated()"])
class FilterController {
	static scope = "prototype"
    def filterService

    /**
     * Add a hidden filter to the filter pane.
     */
    def add () {
        def filters = filterService.showFilter(params.name)
        render template: "/layouts/includes/filters", model: [        filters : filters,
                                                               isCustomFilter : filterService.isCustomFilter()]
    }

    /**
     * Remove a filter from the filter pane.
     */
    def remove () {
        def filters = filterService.removeFilter(params.name)
        render template: "/layouts/includes/filters", model: [        filters : filters,
                                                               isCustomFilter : filterService.isCustomFilter()]
    }

    /**
     * Load a saved filter set and replace the current filters in the filter pane.
     */
    def load () {
        def filters = params.int("id") ? filterService.loadFilters(params.int("id")) :
                                         filterService.getFilters(filterService.getCurrentFilterType(), params)

        render template: "/layouts/includes/filters", model: [        filters : filters,
                                                               isCustomFilter : filterService.isCustomFilter()]
    }



    /**
     * Render the filter pane
     */
    def filters () {
        def filters = filterService.getCurrentFilters()
        render template: "/layouts/includes/filters", model: [        filters : filters,
                                                               isCustomFilter : filterService.isCustomFilter()]
    }

    /**
     * Remove all value from the current filters in the filter pane.
     */
    def clearFilters(){
        def filters = filterService.clearCurrentFilter()
        render template: "/layouts/includes/filters", model: [        filters : filters,
                                                               isCustomFilter : filterService.isCustomFilter()]
    }

    /**
     * Render a list of filter sets to be edited (from the save dialog)
     */
    def filtersets () {
        def filters = filterService.getCurrentFilters()

        def filtersets = filterTypeFiltersetList(filters?.get(0)?.filterSet?.id)

        render template: "filtersets", model: [ filtersets: filtersets, filters: filters ]
    }

    /**
     * Render a list of filter sets of specific filterType to be edited (from the save dialog)
     */
    def filterTypeFiltersetList (def filterSetId) {
        FilterType filterType = filterService.getCurrentFilterType()

        def filterset = FilterSet.createCriteria().listDistinct {
            eq('userId', session['user_id'])

            if (filterSetId) {
                ne('id', filterSetId)
            }

            filters{
                or{
                    eq('type', filterType)
                    eq('type', FilterType.ALL)
                }
            }
        }
        return filterset
    }

    @Transactional(readOnly = false)
    def edit () {
        def filters = filterService.getCurrentFilters()
        def filterset = FilterSet.get(params.int('id'))

        render template: "edit", model: [ selected: filterset, filters: filters ]
    }

    @Transactional(readOnly = false)
    def editCustomFilter() {

        def filters = filterService.getCurrentFilters()
        def filterSet = FilterSet.get(params.int('id'))

        def list = []
        def excludeList = []
        if (filters != null) {
            if (filterSet && filterSet?.filters?.size() > 0) {
                excludeList = filterSet.filters
                list = (filters - excludeList)
            } else {
                list = filters
            }
        }

        def type = filterService.getCurrentFilterType()
        String typeName = type.toString().toLowerCase()

        render template: "custom", model: [selected: filterSet, typeName: typeName, list: list, excludeList: excludeList]
    }

    @Transactional(readOnly = false)
    def saveCustomizeFilter() {
        def filterset = params.id ? FilterSet.get(params.int('id')) : new FilterSet()
        filterset.userId = session['user_id']
        def existingFilters = filterService.getCurrentFilters()

        if(!params.name) {
            def type = filterService.getCurrentFilterType()
            String typeName = type.toString().toLowerCase()
            flash.error = 'filterSet.name.blank'
            render template: 'custom', model: [typeName: typeName, list: existingFilters]
            return
        }

        filterset.setName(params.name)

        // Get all the filters defined for the type

        // Need to check which filter customer wants to see.
        def mainFilters = params.list("customFields")
        def filters = []
        mainFilters.each { filterName ->
            def filter = existingFilters.find { it?.field?.equals(filterName) }
            if (filter == null) return;

            // If that filter has fieldKeyData value means either meta field or AIT type filter then
            // we need to save the meta field id as fieldKeyData in filter. There will be a different multiselect for
            // those fields which user wants. Name of the params should be "${filter.field}.fieldKeyData"

            if (params."${filterName}.fieldKeyData") {
                def subFilters = params.list("${filterName}.fieldKeyData")
                subFilters.each {
                    def filterObject = new Filter(filter)
                    filterObject.setFieldKeyData("" + it)
                    filters.add(filterObject)
                }
            } else {
                def filterObject = new Filter(filter)
                filters.add(filterObject)
            }
        }
        filters.each {
            it.clear()
            it.setVisible(true)
            filterset.addToFilters(it)
        }

        try {
            filterset.save(flush: true)
            flash.message = 'filter.created'
        } catch (Exception e) {
            viewUtils.resolveException(flash, session.locale, e);
        }

        render template: '/layouts/includes/messages'
    }


    @Transactional(readOnly = false)
    def save () {
        def filterset = params.id ? FilterSet.get(params.int('id')) : new FilterSet(params)
        filterset.userId = session['user_id']

        def filters = filterService.getCurrentFilters()
        filterset.filters?.removeAll(filters);

        filters.each {
            filterset.addToFilters(new Filter(it))
        }

        filterset.save(flush: true)

        def filtersets = filterTypeFiltersetList(filters?.get(0)?.filterSet?.id)

        render template: "filtersets", model: [ filtersets: filtersets, selected: filterset  ]
    }

    @Transactional(readOnly = false)
    def delete () {
        FilterSet.get(params.int('id'))?.delete(flush: true)

        log.debug("deleted filter set ${params.id}")

        def filters = filterService.getCurrentFilters()
        def filtersets = filterTypeFiltersetList(filters?.get(0)?.filterSet?.id)

        render template: "filtersets", model: [ filtersets: filtersets ]
    }
}
