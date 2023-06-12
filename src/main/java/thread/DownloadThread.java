package thread;

import main.DownloadMain;
import util.FileUtils;
import util.HttpUtils;
import util.LogUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

/**
 * 多线程下载工具类
 * <p></p>
 * Callable是一个类似Runnable的接口，与其不同的是它可以返回一个结果，而且也可以抛出一个异常
 * @author quincy
 * @create 2023 - 06 - 11 18:50
 */
public class DownloadThread implements Callable<Boolean> {

    // 每次读取的数据块大小
    private static int BYTE_SIZE = 1024 * 100;

    // 下载链接
    private String url;

    // 下载开始位置
    private long startPos;

    // 下载结束位置
    private Long endPos;

    // 标识多线程下载切分的第几部分
    private Integer part;

    // 文件总大小
    private long contentSize;

    public DownloadThread(String url, long startPos, Long endPos, Integer part, Long contentSize) {
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.contentSize = contentSize;
    }

    @Override
    public Boolean call() throws Exception {
        if (url == null || url.trim() == "") {
            throw new RuntimeException("下载路径不正确！");
        }

        // 获取请求文件名
        String httpFileName = HttpUtils.getHttpFileName(url);
        if (part != null)
            httpFileName += DownloadMain.FILE_TEMP_SUFFIX + part;

        // 本地文件大小
        Long localFileSize = FileUtils.getFileContentLength(httpFileName);
        LogThread.LOCAL_FINISH_SIZE.addAndGet(localFileSize);
        if (localFileSize >= endPos - startPos) {
            LogUtils.info("{} 已经完成下载，无需重复下载", httpFileName);
            LogThread.DOWNLOAD_FINISH_THREAD.addAndGet(1);
            return true;
        }
        if (endPos.equals(contentSize)) {
            endPos = null;
        }

        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos + localFileSize, endPos);
        // 获得输入流
        try (InputStream input = httpURLConnection.getInputStream(); BufferedInputStream bis = new BufferedInputStream(input);
             RandomAccessFile oSaveFile = new RandomAccessFile(httpFileName, "rw")) {
            oSaveFile.seek(localFileSize);
            byte[] buffer = new byte[BYTE_SIZE];
            int len = -1;
            while ((len = bis.read(buffer)) != -1) {
                oSaveFile.write(buffer, 0, len);
                LogThread.LOCAL_FINISH_SIZE.addAndGet(len);
            }
        } catch (FileNotFoundException e) {
            LogUtils.error("ERROR! 要下载的文件路径不存在 {}", url);
            return false;
        } catch (Exception e) {
            LogUtils.error("下载出现异常！");
            e.printStackTrace();
            return false;
        } finally {
            httpURLConnection.disconnect();
            LogThread.DOWNLOAD_FINISH_THREAD.addAndGet(1);
        }
        return true;
    }
}
