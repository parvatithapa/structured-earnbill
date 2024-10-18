package com.sapienter.jbilling.server.report.util;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * ReportUtil class
 * 
 * @author Leandro Bagur
 * @since 18/01/18.
 */
public class ReportUtil {

    /**
     * Function to group a collection and sorted with a Tree Map. 
     * @param function function
     * @param <T> T class
     * @param <K> K class
     * @return <T, K extends Comparable<K>>
     */
    public static <T, K extends Comparable<K>> Collector<T, ?, TreeMap<K, List<T>>> sortedGroupingBy(Function<T, K> function) {
        return Collectors.groupingBy(function, TreeMap::new, Collectors.toList());
    }
}
