package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.util.db.StringList;
import com.sapienter.jbilling.server.util.hibernate.StringListType;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * The class represents an entry in a list of nested queries.
 *
 * @author Gerhard
 * @since 31/01/14
 */
@Entity
@Table(name="data_table_query_entry")
@TableGenerator(
        name = "data_table_query_entry_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "data_table_query_entry",
        allocationSize = 1
)
@NamedQueries({
        @NamedQuery(name = "DataTableQueryEntryDTO.findAllDataTableEntriesForTable",
                query = "select q from DataTableQueryEntryDTO q " +
                        "where q.route.id = :routeId ")
})
public class DataTableQueryEntryDTO {
    private int id;
    /** Route/Table this query should be joined to */
    private RouteDTO route;
    /** Columns to join on */
    private StringList columns;
    /** Next query in the list of nested queries */
    private DataTableQueryEntryDTO nextQuery;
    /** Previous query in the list of nested queries */
    private DataTableQueryEntryDTO prevQuery;
    /** Query - not null only if this is the root entry */
    private DataTableQueryDTO query;

    private Integer versionNum;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "data_table_query_entry_GEN")
    @Column(name="id",unique = true,nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }

    @Column(name="columns",nullable = false)
    @Type(type = "com.sapienter.jbilling.server.util.hibernate.StringListType")
    public StringList getColumns() {
        return columns;
    }

    public void setColumns(StringList columns) {
        this.columns = columns;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "next_entry_id")
    public DataTableQueryEntryDTO getNextQuery() {
        return nextQuery;
    }

    public void setNextQuery(DataTableQueryEntryDTO nextQuery) {
        this.nextQuery = nextQuery;
    }

    @OneToOne(mappedBy = "nextQuery")
    public DataTableQueryEntryDTO getPrevQuery() {
        return prevQuery;
    }

    public void setPrevQuery(DataTableQueryEntryDTO prevQuery) {
        this.prevQuery = prevQuery;
    }

    @OneToOne(mappedBy = "rootEntry")
    public DataTableQueryDTO getQuery() {
        return query;
    }

    public void setQuery(DataTableQueryDTO query) {
        this.query = query;
    }

    @Version
    @Column(name = "optlock")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Override
    public String toString() {
        return "DataTableQueryEntryDTO{" +
                "versionNum=" + versionNum +
                ", nextQuery=" + nextQuery +
                ", columns=" + columns +
                ", route=" + (route == null ? "null" : route.getId()) +
                ", id=" + id +
                '}';
    }
}
