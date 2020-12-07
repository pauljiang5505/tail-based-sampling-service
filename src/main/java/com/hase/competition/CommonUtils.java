package com.hase.competition;

import com.hase.competition.beans.Tuple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CommonUtils {
    public static String responseStr = "HTTP/1.1 200\r\n" +
            "Content-Type: text/plain;charset=UTF-8\r\n" +
            "Content-Length: 3\r\n" +
            "Connection: close\r\n" +
            "\r\n" +
            "suc";
    public static char hexDigits[] = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static String getDataPath(int port,int dataSourcePort) {
        return 8000 == port ? "http://localhost:" + dataSourcePort + "/trace1.data" :
                "http://localhost:" + dataSourcePort + "/trace2.data";
    }

    public static long toLong(String str, long defaultValue) {
        if (str == null) {
            return defaultValue;
        } else {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException var4) {
                return defaultValue;
            }
        }
    }

    public static String MD5(String key) {
        try {
            byte[] btInput = key.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isClientProcess() {
        String port = System.getProperty("server.port", "8080");
        if (Constants.CLIENT_PROCESS_PORT1.equals(port) ||
                Constants.CLIENT_PROCESS_PORT2.equals(port)) {
            return true;
        }
        return false;
    }

    public static boolean isBackendProcess() {
        String port = System.getProperty("server.port", "8080");
        if (Constants.BACKEND_PROCESS_PORT1.equals(port)) {
            return true;
        }
        return false;
    }

    public static byte[] intToBytesArray(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = ((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF);
        return value;
    }

    public static void sendIntAndStringFromStream(OutputStream os, int integer, String str) throws IOException {
        byte[] dataBytes = str.getBytes(StandardCharsets.UTF_8);
        byte[] lenBytes = intToBytesArray(dataBytes.length);
        byte[] batchPosBytes = intToBytesArray(integer);
        os.write(lenBytes);
        os.write(batchPosBytes);
        os.write(dataBytes);
        os.flush();
    }

    public static byte[] parseInputStream(InputStream is, byte[] lenAndIntBytes, byte[] dataBytes, Tuple result) throws IOException {
        int readNum = 0;
        while (readNum < 8) {
            int currRead = is.read(lenAndIntBytes, readNum, 8 - readNum);
            if (currRead == -1) {
                if (readNum == 0) { // exit normally
                    return dataBytes;
                }
                return null;
            }
            readNum += currRead;
        }

        int len = bytesToInt(lenAndIntBytes, 0);
        int num = bytesToInt(lenAndIntBytes, 4);

        int dataBytesLen = dataBytes.length;
        while (len > dataBytesLen) {
            dataBytesLen *= 2;
        }
        if (dataBytesLen != dataBytes.length) {
            dataBytes = new byte[dataBytesLen];
        }

        readNum = 0;
        while (readNum < len) {
            int currRead = is.read(dataBytes, readNum, len - readNum);
            if (currRead == -1) {
                return null;
            }
            readNum += currRead;
        }

        String str = new String(dataBytes, 0, len, StandardCharsets.UTF_8);
        result.setNumber(num);
        result.setContent(str);
        return dataBytes;
    }
}
