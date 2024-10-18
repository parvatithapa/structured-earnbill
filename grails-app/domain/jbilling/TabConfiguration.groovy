/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

/**
 * Links a user to the tabs he has available at the top of his screen
 */
class TabConfiguration {

    static constraints = {
        userId(nullable: false)
    }

    static hasMany = [tabConfigurationTabs: TabConfigurationTab]

    static mapping = {
        id generator: 'org.hibernate.id.enhanced.TableGenerator',
                params: [
                        table_name: 'jbilling_seqs',
                        segment_column_name: 'name',
                        value_column_name: 'next_id',
                        segment_value: 'tabConfiguration'
                ]
        tabConfigurationTabs sort: 'displayOrder', order: 'asc', cascade: 'all-delete-orphan'
    }

    SortedSet tabConfigurationTabs
    Integer userId

    @Override
    public String toString() {
        return "TabConfiguration{" +
                "id=" + id +
                ", userId=" + userId +
                ", tabConfigurationTabs=" + tabConfigurationTabs +
                '}';
    }
}
