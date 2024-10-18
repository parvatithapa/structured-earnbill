package com.sapienter.jbilling.server.mediation.customMediations.movius.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.Assert;

/**
 * 
 * @author Krunal Bhavsar
 *
 * @param <S>
 */
public class CustomPatternMatcher {

    private Map<String, MoviusRecordFormatContainer> map = new HashMap<>();
    private List<String> sorted = new ArrayList<>();

    public CustomPatternMatcher(Map<String, MoviusRecordFormatContainer> map) {
        
        this.map = map;
        
        // Sort keys to start with the most specific
        this.sorted = map.keySet()
                         .stream()
                         .sorted((s1, s2) -> s2.compareTo(s1))
                         .collect(Collectors.toList());
    }

    public MoviusRecordFormatContainer match(String line, String fieldSeparator) {
        
        Assert.notNull(line, "A non-null key must be provided to match against.");
        
        for (String key : sorted) {
            for(String field : line.split(fieldSeparator, -1)) {
                if(field.equals(key)) {
                    return map.get(key);
                }
            }
        }
        
        return null;

    }

}
