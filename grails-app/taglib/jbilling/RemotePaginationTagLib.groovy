package jbilling

import org.springframework.web.servlet.support.RequestContextUtils as RCU

/**
 * Based on RemotePaginationTagLib that is part of the remote-pagination-0.4.8 grails plugin.
 */
class RemotePaginationTagLib {
    static namespace = "jB"
    def grailsApplication

    def isPaginationAvailable = {attrs, body ->
        Integer total = attrs.int('total')?: 0

        if(total > 0) {
            Integer max = params.int('max') ?: (attrs.int('max')  ?: grailsApplication.config.grails.plugins.remotepagination.max as Integer)
            if(total > max) {
                out << body()
            }
        }
    }

    def remotePaginate = {attrs ->
        def writer = out

        if (attrs.total == null)
            throwTagError("Tag [remotePaginate] is missing required attribute [total]")

        if (!attrs.update)
            throwTagError("Tag [remotePaginate] is missing required attribute [update]")

        if (!attrs.action)
            throwTagError("Tag [remotePaginate] is missing required attribute [action]")

        def messageSource = grailsApplication.getMainContext().getBean("messageSource")
        def locale = RCU.getLocale(request)

        Integer total = attrs.int('total')?: 0
        Integer offset = params.int('offset') ?: (attrs.int('offset') ?: 0)
        Integer max = params.int('max') ?: (attrs.int('max')  ?: grailsApplication.config.grails.plugins.remotepagination.max as Integer)
        Integer maxsteps = (params.maxsteps ?: (attrs.maxsteps ?: 10))?.toInteger()
        Boolean alwaysShowPageSizes = new Boolean(attrs.alwaysShowPageSizes?:false)
        def pageSizes = attrs.pageSizes ?: []
        Map linkTagAttrs = attrs
        boolean bootstrapEnabled = grailsApplication.config.grails.plugins.remotepagination.enableBootstrap as boolean

        if(bootstrapEnabled){
            writer << '<ul class="pagination">'
        }

        Map linkParams = [offset: offset - max, max: max]
        Map selectParams = [:]
        if (params.sort) linkParams.sort = params.sort
        if (params.order) linkParams.order = params.order

        def action = params.action?:'actn'
        linkParams.update = 'tmp-pagination-'+action
        linkParams.onSuccess = 'movePaginationResults_'+action+'();'
        if (attrs.params) {
            linkParams.putAll(attrs.params)
            selectParams.putAll(linkParams)
        }

        if (attrs.id != null) {linkTagAttrs.id = attrs.id}
        linkTagAttrs.params = linkParams

        // determine paging variables
        boolean steps = maxsteps > 0
        Integer currentstep = (offset / max) + 1
        Integer firststep = 1
        Integer laststep = Math.round(Math.ceil(total / max))

        boolean firstLink = true;

        // display previous link when not on firststep
        if (currentstep > firststep) {
            linkTagAttrs.class = 'prevLink firstLink'
            firstLink = false

            linkParams.offset = offset - max
            writer << wrapInListItem(bootstrapEnabled,remoteLink(linkTagAttrs.clone()) {
                (attrs.prev ? attrs.prev : '&#10094;')
            })
        }

        // display steps when steps are enabled and laststep is not firststep
        if (steps && laststep > firststep) {
            linkTagAttrs.class = 'step'

            if(firstLink) {
                linkTagAttrs.class += ' firstLink'
                firstLink = false
            }
            // determine begin and endstep paging variables
            Integer beginstep = currentstep - Math.round(maxsteps / 2) + (maxsteps % 2)
            Integer endstep = currentstep + Math.round(maxsteps / 2) - 1

            if (beginstep < firststep) {
                beginstep = firststep
                endstep = maxsteps
            }
            if (endstep > laststep) {
                beginstep = laststep - maxsteps + 1
                if (beginstep < firststep) {
                    beginstep = firststep
                }
                endstep = laststep
            }

            // display firststep link when beginstep is not firststep
            if (beginstep > firststep) {
                linkParams.offset = 0
                linkTagAttrs.class += ' lastLink'
                firstLink = true
                writer << wrapInListItem(bootstrapEnabled,remoteLink(linkTagAttrs.clone()) {
                    firststep.toString()
                })
                writer << wrapInListItem(bootstrapEnabled,'<span class="ellipses">&#xe001;</span>')
            }

            // display paginate steps
            (beginstep..endstep).each {i ->
                linkTagAttrs.class = 'step'
                String linkClasses = ''

                if(firstLink) {
                    linkClasses += ' firstLink'
                    firstLink = false
                }
                if(i == endstep && endstep < laststep) {
                    linkClasses += ' lastLink'
                }
                if (currentstep == i) {
                    String currentStepClass = (bootstrapEnabled ? "active" : "currentStep") + linkClasses
                    writer << wrapInListItem(bootstrapEnabled,"<span class=\"${currentStepClass}\">${i}</span>")
                } else {
                    linkTagAttrs.class += linkClasses
                    linkParams.offset = (i - 1) * max
                    writer << wrapInListItem(bootstrapEnabled,remoteLink(linkTagAttrs.clone()) {i.toString()})
                }
            }

            // display laststep link when endstep is not laststep
            if (endstep < laststep) {
                linkTagAttrs.class = 'step firstLink'
                writer << wrapInListItem(bootstrapEnabled,'<span class="ellipses">&#xe001;</span>')
                linkParams.offset = (laststep - 1) * max
                writer << wrapInListItem(bootstrapEnabled,remoteLink(linkTagAttrs.clone()) { laststep.toString() })
            }
        }
        // display next link when not on laststep
        if (currentstep < laststep) {
            linkTagAttrs.class = 'nextLink lastLink'
            linkParams.offset = offset + max
            writer << wrapInListItem(bootstrapEnabled,remoteLink(linkTagAttrs.clone()) {
                (attrs.next ? attrs.next : '&#10095;')
            })
        }

        if ((alwaysShowPageSizes || total > max) && pageSizes) {
            selectParams.remove("max")
            selectParams.offset=0
            String paramsStr = selectParams.collect {it.key + "=" + it.value}.join("&")
            paramsStr = '\'' + paramsStr + '&max=\' + this.value'
            linkTagAttrs.params = paramsStr
            Boolean isPageSizesMap = pageSizes instanceof Map

            writer << wrapInListItem(bootstrapEnabled,"<span>" + select(from: pageSizes, value: max, name: "max", onchange: "${remoteFunction(linkTagAttrs.clone())}" ,class: 'remotepagesizes',
                    optionKey: isPageSizesMap?'key':'', optionValue: isPageSizesMap?'value':'') + "</span>")
        }

        if(bootstrapEnabled){
            writer << '</ul>'
        }

        writer << """
            <div id="tmp-pagination-${action}" style="display:none;"></div>
            <script type="text/javascript">
                function movePaginationResults_${action} () {
                    var col = '#column2';
                    if(\$('#tmp-reports').closest('#column1').size() > 0) {
                        col = '#column1';
            """

        if(params.update) {
            def updateDiv = params.update
            writer << """
                    } else if(\$('#${updateDiv}').size() > 0) {
                        col = '#${updateDiv}';
                """
        }

        writer << """
                    }
                    var colEl = \$(col);

                    var newEl = \$('#tmp-pagination-${action}').children();
                    newEl.detach();
                    colEl.empty();
                    colEl.append(newEl);
                }

            </script>
        """
    }

    private def wrapInListItem(Boolean bootstrapEnabled, def val){
        bootstrapEnabled ? "<li>$val</li>" : val
    }
}
