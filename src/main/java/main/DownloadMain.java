package main;

import thread.DownloadThread;
import thread.LogThread;
import util.FileUtils;
import util.HttpUtils;
import util.LogUtils;
import util.ThunderUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.CRC32;

/**
 * <p>
 *     多线程下载
 *     断点续传demo
 *
 * @author quincy
 * @create 2023 - 06 - 11 17:43
 */
public class DownloadMain {

    // 下载线程数量
    public static int DOWNLOAD_THREAD_NUM = 5;
    // 下载线程池
    private static ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_THREAD_NUM + 1);
    // 临时文件后缀
    public static String FILE_TEMP_SUFFIX = ".temp";

    // 支持的URL协议
    private static HashSet<String> PROTOCOL_SET = new HashSet<String>();

    static {
        PROTOCOL_SET.add("thunder://");
        PROTOCOL_SET.add("http://");
        PROTOCOL_SET.add("https://");
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0 || args[0].trim().length() == 0) {
            LogUtils.info("没有传入任何下载链接");
            LogUtils.info("支持http/https/thunder链接");
            return;
        }
        final String url = args[0];
        long count = PROTOCOL_SET.stream().filter(prefix -> url.startsWith(prefix)).count();
        if (count == 0) {
            LogUtils.error("不支持的协议类型");
            return;
        }
        LogUtils.info("要下载的链接:{}", url);
        new DownloadMain().download(ThunderUtils.toHttpUrl(url));

    }

    public void download(String url) throws Exception {
        String fileName = HttpUtils.getHttpFileName(url);
        // 本地文件大小
        long localFileSize = FileUtils.getFileContentLength(fileName);
        // 网络文件大小
        long httpFileSize = HttpUtils.getHttpFileSize(url);
        if (localFileSize >= httpFileSize) {
            LogUtils.info("{}已经下载完毕，无需重新下载", fileName);
            return;
        }
        List<Future<Boolean>> futureList = new ArrayList<>();
        if (localFileSize > 0) {
            LogUtils.info("开始断点续传 {}", fileName);
        } else {
            LogUtils.info("开始下载文件{}", fileName);
        }

        LogUtils.info("开始下载时间 {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
        long startTime = System.currentTimeMillis();
        // 任务划分
        splitDownload(url, futureList);
        LogThread logThread = new LogThread(httpFileSize);
        Future<Boolean> future = executor.submit(logThread);
        futureList.add(future);
        // 等待下载完成
        for (Future<Boolean> booleanFuture : futureList) {
            booleanFuture.get();
        }

        LogUtils.info("文件下载完毕 {}, 本次耗时：{}", fileName, fileName, (System.currentTimeMillis() - startTime) / 1000 + "s");
        LogUtils.info("结束下载时间 {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));

        // 文件合并
        boolean merge = merge(fileName);
        if (merge) {
            clearTemp(fileName);
        }
        LogUtils.info("本次文件下载结束");
        System.exit(0);

    }


    /**
     * 切分下载任务到多个线程
     * @param url
     * @param futureList
     * @throws IOException
     */
    public void splitDownload(String url, List<Future<Boolean>> futureList) throws IOException {
        long httpFileContentLength = HttpUtils.getHttpFileSize(url);
        // 任务切分
        long size = httpFileContentLength / DOWNLOAD_THREAD_NUM;
        long lastSize = httpFileContentLength - (httpFileContentLength / DOWNLOAD_THREAD_NUM * (DOWNLOAD_THREAD_NUM - 1));
        for (int i = 0; i < DOWNLOAD_THREAD_NUM; i++) {
            long start = i * size;
            Long downloadWindow = (i == DOWNLOAD_THREAD_NUM - 1) ? lastSize : size;
            Long end = start + downloadWindow;
            if (start != 0) {
                start++;
            }
            DownloadThread downloadThread = new DownloadThread(url, start, end, i, httpFileContentLength);
            Future<Boolean> future = executor.submit(downloadThread);
            futureList.add(future);
        }
    }


    /**
     * 合并文件
     * @param fileName
     * @return
     * @throws IOException
     */
    public boolean merge(String fileName) throws IOException {
        LogUtils.info("开始合并文件{}", fileName);
        byte[] buffer = new byte[1024 * 10];
        int len = -1;
        try (RandomAccessFile oSaveFile = new RandomAccessFile(fileName, "rw")) {
            for (int i = 0; i < DOWNLOAD_THREAD_NUM; i++) {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName + FILE_TEMP_SUFFIX + i))) {
                    while ((len = bis.read(buffer)) != -1) {
                        oSaveFile.write(buffer, 0, len);
                    }
                }
            }
            LogUtils.info("文件合并完毕{}", fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * 清理暂时文件
     * @param fileName
     * @return
     */
    public boolean clearTemp(String fileName) {
        LogUtils.info("开始清理临时文件 {}{}0-{}", fileName, FILE_TEMP_SUFFIX, (DOWNLOAD_THREAD_NUM - 1));
        for (int i = 0; i < DOWNLOAD_THREAD_NUM; i++) {
            File file = new File(fileName + FILE_TEMP_SUFFIX + i);
            file.delete();
        }
        LogUtils.info("临时文件清理完毕 {}{}0-{}", fileName, FILE_TEMP_SUFFIX, (DOWNLOAD_THREAD_NUM - 1));
        return true;
    }

    public static Long getCRC32(String filepath) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filepath));
        CRC32 crc32 = new CRC32();
        byte[] bytes = new byte[1024];
        int cnt;
        while ((cnt = inputStream.read(bytes)) != -1) {
            crc32.update(bytes, 0, cnt);
        }
        inputStream.close();
        return crc32.getValue();
    }


}
