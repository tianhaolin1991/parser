package com.huawei.fossbot.dependency.util;

import com.huawei.fossbot.dependency.bean.OS;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 消除process waitFor卡死的风险
 *
 * @author t30002128
 * @since 2020/06/01
 */
public class ProcessUtil {

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 解决waitFor存在的阻塞风险
     */
    public static Integer waitFor(Process process) throws InterruptedException {
        executorService.submit(() -> {
            readInputStream(process.getErrorStream()); });
        executorService.submit(()->
                readInputStream(process.getInputStream()));
        return process.waitFor();
    }

    private static String readInputStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        String type;
        if(DependencyAnalyzeHelper.osType().equals(OS.WINDOWS)){
            type = "GBK";
        }else {
            type = "UTF-8";
        }
        try(InputStreamReader isr = new InputStreamReader(is, Charset.forName(type));
            BufferedReader br = new BufferedReader(isr)) {
            String line = null;
            while ((line = br.readLine()) != null){
                sb.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 从error流中读取字符串
     */
    public static Future<String> getErrorMsg(Process process){
        return executorService.submit(() -> readInputStream(process.getErrorStream()));
    }

    /**
     * 从Input流中读取字符串
     */
    public static Future<String> getInputMsg(Process process){
        return executorService.submit(() -> readInputStream(process.getInputStream()));
    }
}
