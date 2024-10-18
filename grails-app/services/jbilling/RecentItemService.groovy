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

import javax.servlet.http.HttpSession
import org.springframework.web.context.request.RequestContextHolder
/**
 * RecentItemService
 
 * @author Brian Cowdery
 * @since  07-12-2010
 */
class RecentItemService implements Serializable {

    public static final String SESSION_RECENT_ITEMS = "recent_items"
    public static final Integer MAX_ITEMS = 5
    public static final Integer MAX_IN_STORE = MAX_ITEMS + 3

    static scope = "singleton"

    List load() {
        if (session['user_id'] && !session[SESSION_RECENT_ITEMS])
            session[SESSION_RECENT_ITEMS] = getRecentItems()
        return session[SESSION_RECENT_ITEMS]
    }

    /**
     * Returns a list of recently viewed items for the currently logged in user.
     *
     * @return list of recently viewed items.
     */
    List getRecentItems(int limit = MAX_ITEMS) {
        def userId = session["user_id"]
        return RecentItem.withSession { session ->
            Query query = session.createQuery("from RecentItem item where item.userId = :userId order by item.id desc")
            query.setInteger("userId", userId)
            query.setReadOnly(true) //do not track entity changes
            query.setMaxResults(limit)
            query.list()
        }
    }

    /**
     * It will delete recent items that belong to some user and have
     * less then or equal id to a given parameter. This method is used
     * to delete extra recent items that are no longer needed in database
     *
     * @param userId - the user id to which the recent items belong
     * @param firstInvalidId - the first invalid id
     * @return the number of deleted recent items
     */
    int deleteExtra(Integer userId, Long firstInvalidId) {
        RecentItem.withNewTransaction {
            return RecentItem.withSession { session ->
                Query query = session.createQuery("delete RecentItem item where item.userId = :userId and item.id <= :id")
                query.setInteger("userId", userId)
                query.setLong("id", firstInvalidId)
                query.executeUpdate();
            }
        }
    }

    /**
     * Add a new item to the recent items list for the currently logged in user and
     * update the session list.
     *
     * This method will not add a recent item if either the ID or recent item type is null.
     *
     * @param objectId object id
     * @param type recent item type
     */
    void addRecentItem(Integer objectId, RecentItemType type) {
        if (objectId && type) {
            addRecentItem(new RecentItem(objectId: objectId, type: type))
        }
    }

    void addRecentItem(UUID uuid, RecentItemType type) {
        if (uuid && type) {
            addRecentItem(new RecentItem(uuid: uuid, type: type))
        }
    }

    /**
     * Add a new item to the recent items list for the currently logged in user and
     * update the session list.
     *
     * @param item recent item
     */
    void addRecentItem(RecentItem item) {
        def items = getRecentItems(MAX_IN_STORE)
        def lastItem = !items.isEmpty() ? items.get(0) : null

        // add item only if it is different from the last item added
        try {
            if (!lastItem || !lastItem.equals(item)) {
                def userId = session['user_id']
                item.userId = userId
                item = item.save(flush: true)

                items.add(0, item)//add as first, newest

                if (items.size() > MAX_ITEMS) {
                    def remove = items.subList(MAX_ITEMS, items.size())
                    if (remove.size() >= (MAX_IN_STORE - MAX_ITEMS)) {
                        //we have reached the max recent items that we want to store
                        //in the database per user so we will delete the extra elements
                        def firstInvalidId = remove.getAt(0).id
                        def deleteCount = deleteExtra(userId, firstInvalidId)
                        log.debug("Deleted: " + deleteCount + " recent items for user id: " + userId);
                    }
                    //removes the elements from original list as well
                    remove.clear()
                }

                session[SESSION_RECENT_ITEMS] = items
            }

        } catch (Throwable t) {
            log.error("Exception caught adding recent item", t)
            session.error = 'recent.item.failed'
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
