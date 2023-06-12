package util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;

/**
 * 迅雷链接转换工具
 * @author quincy
 * @create 2023 - 06 - 12 14:51
 */
public class ThunderUtils {

    private static String THUNDER = "thunder://";

    /**
     * 判断是否是迅雷链接
     * @param url
     * @return
     */
    public static boolean isThunderLink(String url) {
        return url.startsWith(THUNDER);
    }


    /**
     * thunder://xxxxx，将 xxxxx 去 Base64 解密后会得到 AAyyyyyZZ，去掉 AA 和 ZZ 后的 yyyyy 就是真实下载链接
     * @param url
     * @return
     */
    public static String toHttpUrl(String url) {
        if (!isThunderLink(url)) {
            return url;
        }

        LogUtils.info("当前链接是迅雷链接，开始转换...");
        url = url.replaceFirst(THUNDER, "");
        try {
            // base64转换
            url = new String(Base64.getDecoder().decode(url.getBytes()), "UTF-8");
            // url解码
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (url.startsWith("AA")) {
            url = url.substring(2);
        }
        if (url.endsWith("ZZ")) {
            url = url.substring(0, url.length() - 2);
        }
        LogUtils.info("当前链接是迅雷链接，转换结果:{}", url);
        return url;
    }
}
