/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields;

/**
 * Type of entity that a meta field belongs to.
 *
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
public enum EntityType {

 CUSTOMER, AGENT, PRODUCT, ORDER, INVOICE, PAYMENT,ACCOUNT_TYPE, PRODUCT_CATEGORY, PLAN,
 ASSET, ORDER_CHANGE, ORDER_LINE, PAYMENT_METHOD_TYPE, MEDIATION_CONFIGURATION,
 PAYMENT_METHOD_TEMPLATE, ACCOUNT_INFORMATION_TYPE, ENUMERATION, META_FIELD,
 PLUGGABLE_TASK, COMPANY, ROUTE , EDI_TYPE, ENROLLMENT, COMPANY_INFO, USER

}
