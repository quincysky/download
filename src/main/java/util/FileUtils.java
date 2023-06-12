package util;

import java.io.File;

/**
 * 文件工具类
 * @author quincy
 * @create 2023 - 06 - 11 18:07
 */
public class FileUtils {


    /**
     * 获取文件的长度
     * @param name
     * @return
     */
    public static long getFileContentLength(String name) {
        File file = new File(name);
        return file.exists() && file.isFile() ? file.length() : 0;
    }
}
