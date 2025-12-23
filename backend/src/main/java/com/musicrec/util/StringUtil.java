package com.musicrec.util;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;

public class StringUtil {
    
    public static String normalize(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        
        // Normalize Unicode
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);
        
        // Convert to lowercase and trim
        normalized = normalized.toLowerCase().trim();
        
        // Replace multiple spaces with single space
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
    
    public static String sanitize(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        
        // Remove non-printable characters
        String sanitized = input.replaceAll("[^\\p{Print}\\p{Space}]", "");
        
        return sanitized.trim();
    }
    
    public static String truncate(String input, int maxLength) {
        if (StringUtils.isBlank(input) || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }
}