/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2017] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CollectionUtil {
    public static <T> T[] nullSafeSort(T[] array, Comparator<T> c) {
        if(array == null) {
            return array;
        }
        Arrays.sort(array, c);
        return array;
    }

    public static <T> List<T> nullSafeSort(List<T> list, Comparator<T> c) {
        if(list == null) {
            return list;
        }
        list.sort(c);
        return list;
    }

    public static <T extends Comparable<T>> T[] nullSafeSort(T[] array) {
        if(array == null) {
            return array;
        }
        Arrays.sort(array);
        return array;
    }

    public static <T extends Comparable<T>> List<T> nullSafeSort(List<T> list) {
        if(list == null) {
            return list;
        }
        Collections.sort(list);
        return list;
    }
}
