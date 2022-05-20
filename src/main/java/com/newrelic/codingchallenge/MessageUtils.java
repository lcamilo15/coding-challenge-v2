package com.newrelic.codingchallenge;

import org.apache.commons.lang3.StringUtils;

public class MessageUtils {
    public static boolean isTerminationString(String in) {
        return "terminate".equals(in);
    }

    public static boolean isValidNumber(String in) {
        return in.length() == 9 && StringUtils.isNumeric(in) ;
    }
}
