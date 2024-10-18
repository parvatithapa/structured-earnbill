package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.user.db.UserDTO;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Class represents a sequence of nested queries that may be executed on a data table (Route).
 *
 * @author Gerhard
 * @since 31/01/14
 */
@Entity
@Table(name="data_table_query")
@TableGenerator(
        name = "data_table_query_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "data_table_query",
        allocationSize = 1
)
@NamedQueries({
        @NamedQuery(name = "DataTableQueryDTO.findDataTableQueriesForTable",
                query = "select q from DataTableQueryDTO q " +
                        "where q.route.id = :routeId " +
                        "and (q.user.id = :userId " +
                        "or q.global = 1)"),
        @NamedQuery(name = "DataTableQueryDTO.countDataTableQueriesForTableAndName",
                query = "select count(q.id) from DataTableQueryDTO q " +
                        "where q.route.id = :routeId " +
                        "and q.name = :name " +
                        "and (q.user.id = :userId " +
                        "or q.global = 1)"),
        @NamedQuery(name = "DataTableQueryDTO.findAllDataTableQueriesForTable",
                query = "select q from DataTableQueryDTO q " +
                        "where q.route.id = :routeId ")
})
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class DataTableQueryDTO {
    private int id;
    private String name;
    /** Route this query may be executed on*/
    private RouteDTO route;
    private int global;
    private Integer versionNum;
    /** The first query in the list of nested queries*/
    private DataTableQueryEntryDTO rootEntry;
    /** User that created the query */
    private UserDTO user;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "data_table_query_GEN")
    @Column(name="id",unique = true,nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Column(name = "global")
    public int getGlobal() {
        return global;
    }

    public void setGlobal(int global) {
        this.global = global;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "root_entry_id")
    public DataTableQueryEntryDTO getRootEntry() {
        return rootEntry;
    }

    public void setRootEntry(DataTableQueryEntryDTO rootEntry) {
        this.rootEntry = rootEntry;
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
        return "DataTableQueryDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", route=" + (route == null ? "null" : route.getId()) +
                ", global=" + global +
                ", rootEntry=" + rootEntry +
                ", versionNum=" + versionNum +
                '}';
    }
}
