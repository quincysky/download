package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志工具类
 * @author quincy
 * @create 2023 - 06 - 11 18:09
 */
public class LogUtils {

    public static boolean DEBUG = false;

    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void info(String msg, Object... args) {
        print(msg, "-INFO-", args);
    }

    public static void error(String msg, Object... args) {
        print(msg, "-ERROR-", args);
    }

    public static void debug(String msg, Object... args) {
        print(msg, "-DEBUG-", args);
    }

    private static void print(String msg, String level, Object... args) {
        if (args != null && args.length > 0) {
            msg = String.format(msg.replace("{}", "%s"), args);
        }
        String threadName = Thread.currentThread().getName();
        System.out.println(LocalDateTime.now().format(dateTimeFormatter)  + " " + threadName + level + msg);
    }
}
