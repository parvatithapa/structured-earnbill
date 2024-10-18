package com.sapienter.jbilling.tools;/*
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by marcomanzi on 6/20/14.
 */
public class JArrays {

    public static <T> List<T> toArrayList(T[] array) {
        return array != null ?
                new ArrayList<T>(Arrays.asList(array)) :
                new ArrayList<T>();
    }
}
