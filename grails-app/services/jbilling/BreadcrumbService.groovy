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

import org.hibernate.Query
import org.springframework.web.context.request.RequestContextHolder

import org.apache.commons.lang.StringUtils

import javax.servlet.http.HttpSession

/**
 * BreadcrumbService
 *
 * @author Brian Cowdery
 * @since  14-12-2010
 */
class BreadcrumbService implements Serializable {

    public static final String SESSION_BREADCRUMBS = "breadcrumbs"
    public static final Integer MAX_ITEMS = 7
    public static final Integer MAX_IN_STORE = MAX_ITEMS + 5

    static scope = "singleton"

    List load() {
        if (session['user_id'] && !session[SESSION_BREADCRUMBS])
            session[SESSION_BREADCRUMBS] = getBreadcrumbs()
        return session[SESSION_BREADCRUMBS]
    }

    /**
     * Returns a list of recorded breadcrumbs for the currently logged in user.
     *
     * @return list of recorded breadcrumbs.
     */
    List getBreadcrumbs(int limit = MAX_ITEMS) {
        def userId = session["user_id"]

        return Breadcrumb.withSession { session ->
            Query query = session.createQuery("from Breadcrumb b where b.userId = :userId order by b.id asc")
            query.setInteger("userId", userId)
            query.setReadOnly(true) //do not track entity changes
            query.setMaxResults(limit)
            query.list()
        }
    }

    /**
     * It will delete breadcrumbs that belong to some user and have
     * less then or equal id to a given parameter. This method is used
     * to delete extra breadcrumbs that are no longer needed in database
     *
     * @param userId - the user id to which the breadcrumb belongs
     * @param firstInvalidId - the first invalid id
     * @return the number of deleted breadcrumbs
     */
    int deleteExtra(Integer userId, Long firstInvalidId) {
        Breadcrumb.withNewTransaction {
            return Breadcrumb.withSession { session ->
                Query query = session.createQuery("delete Breadcrumb b where b.userId = :userId and b.id <= :id")
                query.setInteger("userId", userId)
                query.setLong("id", firstInvalidId)
                query.executeUpdate();
            }
        }
    }

    /**
     * Returns the last recorded breadcrumb for the currently logged in user.
     *
     * @return last recorded breadcrumb.
     */
    Object getLastBreadcrumb() {
        return Breadcrumb.findByUserId(session['user_id'], [sort:'id', order:'desc'])
    }

    /**
     * Add a new breadcrumb to the breadcrumb list for the currently logged in user and
     * update the session list.
     *
     * The resulting breadcrumb link is generated using the 'g:link' grails tag. The same
     * parameter requirements for g:link apply here as well. A breadcrumb MUST have a controller,
     * but action and ID are optional. the name parameter is used to control the translated breadcrumb
     * message and is optional.
     *
     * @param controller breadcrumb controller (required)
     * @param action breadcrumb action, may be null
     * @param name breadcrumb message key name, may be null
     * @param objectId breadcrumb entity id, may be null.
     */
    void addBreadcrumb(String controller, String action, String name, Object id) {
		name= StringUtils.abbreviate(name, 255);
        if (id instanceof UUID) {
            addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, uuid: id))
        } else {
            addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, objectId: id))
        }
    }

    void addBreadcrumb(String controller, String action, String name, Object id, String description) {
        name= StringUtils.abbreviate(name, 255);
        if ("role" == controller && name == "update") {
            description = StringUtils.abbreviate(description, 255);
        } else {
            description = StringUtils.abbreviate(name, 255);
        }
        if ("role" == controller && name != "update" && name != "create") {
            name = null;
        }
        if (id instanceof UUID) {
            addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, uuid: id, description: description))
        } else {
            addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, objectId: id, description: description))
        }
    }

    void addBreadcrumb(String controller, String action, String name, Object id, String description, String parameters) {
        name= StringUtils.abbreviate(name, 255);
        if ("role" == controller && name == "update") {
            description = StringUtils.abbreviate(description, 255);
        } else {
            description = StringUtils.abbreviate(name, 255);
        }
        if ("role" == controller && name != "update" && name != "create") {
            name = null;
        }
        if (id instanceof UUID) {
            addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, uuid: id, description: description, parameters: parameters))
        } else {
            addBreadcrumb(new Breadcrumb(controller: controller, action: action, name: name, objectId: id, description: description, parameters: parameters))
        }
    }

    /**
     * Add a new breadcrumb to the recent breadcrumb list for the currently logged in user and
     * update the session list.
     *
     * @param crumb breadcrumb to add
     */
    void addBreadcrumb(Breadcrumb crumb) {
        def crumbs = getBreadcrumbs(MAX_IN_STORE)
        def lastItem = !crumbs.isEmpty() ? crumbs.last() : null

		// truncate string if its more than 255 words
		crumb.description = StringUtils.left(crumb.description, 255)

        // add breadcrumb only if it is different from the last crumb added
        try {
            if (!lastItem || !lastItem.equals(crumb)) {
                def userId = session['user_id']
                crumb.userId = userId
                crumb = crumb.save()

                crumbs << crumb//add as last, newest

                if (crumbs.size() > MAX_ITEMS) {
                    def remove = crumbs.subList(0, crumbs.size() - MAX_ITEMS)
                    if (remove.size() >= (MAX_IN_STORE - MAX_ITEMS)) {
                        //we have reached the max breadcrumbs that
                        //we want to store in database per user
                        //so we will delete the extra elements
                        def firstInvalidId = remove.last().id
                        def deleteCount = deleteExtra(userId, firstInvalidId)
                        log.debug("Deleted: " + deleteCount + " breadcrumbs for user id: " + userId);
                    }
                    //removes the elements from original list as well
                    remove.clear()
                }

                session[SESSION_BREADCRUMBS] = crumbs
            }

        } catch (Throwable t) {
            log.error("Exception caught adding breadcrumb", t)
            session.error = 'breadcrumb.failed'
        }
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

}
