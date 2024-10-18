package jbilling.com.sapienter.jbilling.tools;/*
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

import com.sapienter.jbilling.tools.JArrays;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by marcomanzi on 6/20/14.
 */
public class JArraysTest {

    @Test
    public void toListNullArray() {
        assertEquals(new ArrayList<JArraysTest>(), JArrays.toArrayList(null));
    }

    @Test
    public void toListEmptyArray() {
        assertEquals(new ArrayList<JArraysTest>(), JArrays.toArrayList(new JArraysTest[0]));
    }

    @Test
    public void toListArrayWithElement() {
        String[] array = new String []  {"1", "2", "3"};
        assertEquals(new ArrayList<String>(Arrays.asList(array)), JArrays.toArrayList(array));
    }
}
