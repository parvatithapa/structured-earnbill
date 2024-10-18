package com.sapienter.jbilling.server.filter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by marcolin on 19/08/16.
 */
public class PagedResultList<T> extends LinkedList<T> implements Serializable {
    private Long totalCount;

    public PagedResultList() {}

    public PagedResultList(List elements, Long totalCount) {
        this.addAll(elements);
        this.totalCount = totalCount;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }
}
