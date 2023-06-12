package util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * 网络请求工具类
 * @author quincy
 * @create 2023 - 06 - 11 18:30
 */
public class HttpUtils {


    /**
     * 获取HTTP连接
     * @param url
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        // User-Agent用于标识发送请求的客户端应用程序或浏览器的类型、版本、操作信息等等信息
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");
        return httpURLConnection;
    }


    /**
     * 根据HTTP请求中的range字段自定义下载区间
     * @param url
     * @param start
     * @param end
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpURLConnection(String url, long start, Long end) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        LogUtils.debug("此线程下载内容区间 {}-{}", start, end);
        if (end != null) {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + start + "-" + end);
        } else {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + start + "-");
        }
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        for (String s : headerFields.keySet()) {
            LogUtils.debug("此线程对应头{}:{}", s, headerFields.get(s));
        }
        return httpURLConnection;
    }


    /**
     * 获取网络文件大小bytes
     * @param url
     * @return
     * @throws IOException
     */
    public static long getHttpFileSize(String url) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        int contentLength = httpURLConnection.getContentLength();
        httpURLConnection.disconnect();
        return contentLength;
    }


    /**
     * 获取网路文件Etag
     * @param url
     * @return
     * @throws IOException
     */
    public static String getHttpFileEtag(String url) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        List<String> eTag = headerFields.get("ETag");
        httpURLConnection.disconnect();
        return eTag.get(0);
    }


    /**
     * 获取网络文件名
     * @param url
     * @return
     */
    public static String getHttpFileName(String url) {
        int index = url.lastIndexOf("/");
        return url.substring(index + 1);
    }
}
