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
 * Links to one of the tabs a user has access to.
 * Tabs will be displayed according to displayOrder.
 * Those not visible will be available under a (+) menu option
 *
 */
class TabConfigurationTab implements Comparable {

    static constraints = {
        tabConfiguration(nullable: false)
        displayOrder(nullable: false)
        visible(nullable: false)
    }

    static mapping = {
        id generator: 'org.hibernate.id.enhanced.TableGenerator',
                params: [
                        table_name: 'jbilling_seqs',
                        segment_column_name: 'name',
                        value_column_name: 'next_id',
                        segment_value: 'tabConfigurationTab'
                ]
    }

    static belongsTo = [tabConfiguration: TabConfiguration]

    Integer displayOrder
    Boolean visible
    Tab tab

    int compareTo(obj) {
        displayOrder.compareTo(obj.displayOrder)
    }

    @Override
    public String toString() {
        return "TabConfigurationTab{" +
                "id=" + id +
                ", displayOrder=" + displayOrder +
                ", visible=" + visible +
                ", tab=" + tab +
                '}';
    }
}
