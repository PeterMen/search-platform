package com.peter.search.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DeleteJsonUtil {

    private static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat dateMinutesFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat dateHourFormatter = new SimpleDateFormat("yyyy-MM-dd HH");
    private static final SimpleDateFormat dateDayFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat dateMonthFormatter = new SimpleDateFormat("yyyy-MM");
    private static final SimpleDateFormat dateYearFormatter = new SimpleDateFormat("yyyy");
    private static final String NOW = "now";
    private static final String CURRENT_MINUTES = "currentMinutes";
    private static final String CURRENT_HOUR = "currentHour";
    private static final String CURRENT_DAY = "currentDay";
    private static final String CURRENT_MONTH = "currentMonth";
    private static final String CURRENT_YEAR = "currentYear";
    private static final String NOW_TMSP = "nowTimestamp";
    private static final String CURRENT_MINUTES_TMSP = "currentMinutesTimestamp";
    private static final String CURRENT_HOUR_TMSP = "currentHourTimestamp";
    private static final String CURRENT_DAY_TMSP = "currentDayTimestamp";
    private static final String CURRENT_MONTH_TMSP = "currentMonthTimestamp";
    private static final String CURRENT_YEAR_TMSP = "currentYearTimestamp";

    private static String regex = String.format("(now|currentMinutes|currentHour|currentDay|currentMonth|currentYear|nowTimestamp|currentMinutesTimestamp|currentHourTimestamp|currentDayTimestamp|currentMonthTimestamp|currentYearTimestamp)((\\+|-)(\\d+)(d|h|m|s))?");

    public static String handleToken(String originContent){
        GenericTokenParser parser = new GenericTokenParser("#{", "}", new SelfTokenHandler());
        return parser.parse(originContent);
    }

    private static class SelfTokenHandler  implements TokenHandler {

        public String handleToken(String content){
            if(!content.matches(regex)){
                throw new RuntimeException(content + "格式不正确");
            }
            return handleToken0(content).replaceAll(":", "&colon;");
        }

        private String handleToken0(String content){
            Date d = new Date();
            if(content.contains("+")) {
                String secondChar = content.substring(content.indexOf("+"));
                content = content.substring(0, content.indexOf("+"));
                if(secondChar.endsWith("d")){
                    d = DateUtils.addDays(d, Integer.valueOf(secondChar.replace("d", "")));
                } else if(secondChar.endsWith("h")){
                    d = DateUtils.addHours(d, Integer.valueOf(secondChar.replace("h", "")));
                } else if(secondChar.endsWith("m")){
                    d = DateUtils.addMinutes(d, Integer.valueOf(secondChar.replace("m", "")));
                } else if(secondChar.endsWith("s")){
                    d = DateUtils.addSeconds(d, Integer.valueOf(secondChar.replace("s", "")));
                }
            } else if( content.contains("-")) {
                String secondChar = content.substring(content.indexOf("-"));
                content = content.substring(0, content.indexOf("-"));
                if(secondChar.endsWith("d")){
                    d = DateUtils.addDays(d, Integer.valueOf(secondChar.replace("d", "")));
                } else if(secondChar.endsWith("h")){
                    d = DateUtils.addHours(d, Integer.valueOf(secondChar.replace("h", "")));
                } else if(secondChar.endsWith("m")){
                    d = DateUtils.addMinutes(d, Integer.valueOf(secondChar.replace("m", "")));
                } else if(secondChar.endsWith("s")){
                    d = DateUtils.addSeconds(d, Integer.valueOf(secondChar.replace("s", "")));
                }
            }
            if(StringUtils.equals(content, NOW)){
                return dateTimeFormatter.format(d);
            } else if(StringUtils.equals(content, CURRENT_DAY)){
                return dateDayFormatter.format(d);
            } else if(StringUtils.equals(content, CURRENT_MONTH)){
                return dateMonthFormatter.format(d);
            } else if(StringUtils.equals(content, CURRENT_YEAR)){
                return dateYearFormatter.format(d);
            } else if(StringUtils.equals(content, CURRENT_HOUR)){
                return dateHourFormatter.format(d);
            } else if(StringUtils.equals(content, CURRENT_MINUTES)){
                return dateMinutesFormatter.format(d);
            } else if(StringUtils.equals(content, NOW_TMSP)){
                return String.valueOf(d.getTime());
            } else if(StringUtils.equals(content, CURRENT_DAY_TMSP)){
                try {
                    return String.valueOf(dateDayFormatter.parse(dateDayFormatter.format(d)).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if(StringUtils.equals(content, CURRENT_MONTH_TMSP)){
                try {
                    return String.valueOf(dateMonthFormatter.parse(dateMonthFormatter.format(d)).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if(StringUtils.equals(content, CURRENT_YEAR_TMSP)){
                try {
                    return String.valueOf(dateYearFormatter.parse(dateYearFormatter.format(d)).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if(StringUtils.equals(content, CURRENT_HOUR_TMSP)){
                try {
                    return String.valueOf(dateHourFormatter.parse(dateHourFormatter.format(d)).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if(StringUtils.equals(content, CURRENT_MINUTES_TMSP)){
                try {
                    return String.valueOf(dateMinutesFormatter.parse(dateMinutesFormatter.format(d)).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return "";
        }
    }
////
//    public static void main(String[] args)throws Exception {
//        String test = "#{currentHourTimestamp-5d}";
//        System.out.println(handleToken(test));
//    }
}
