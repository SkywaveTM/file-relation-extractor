package utils;

import java.text.SimpleDateFormat;

public class DateUtil {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String getDate(long time) {
        return DATE_FORMAT.format(new java.util.Date(time));
    }
}
