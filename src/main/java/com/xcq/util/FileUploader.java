package com.xcq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public class FileUploader {
    private static final Logger logger = LoggerFactory.getLogger(FileUploader.class);
    private static final String UPLOAD_URL = "http://localhost:8080/upload"; // 替换为实际的上传服务器地址

    public static CompletableFuture<String> uploadFile(File file, Component parent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String boundary = Long.toHexString(System.currentTimeMillis());
                URL url = new URL(UPLOAD_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream output = connection.getOutputStream();
                     PrintWriter writer = new PrintWriter(new OutputStreamWriter(output))) {

                    writer.println("--" + boundary);
                    writer.println("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"");
                    writer.println("Content-Type: " + Files.probeContentType(file.toPath()));
                    writer.println();
                    writer.flush();

                    Files.copy(file.toPath(), output);
                    output.flush();

                    writer.println();
                    writer.println("--" + boundary + "--");
                    writer.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String fileUrl = reader.readLine();
                        return fileUrl;
                    }
                } else {
                    throw new IOException("Server returned code: " + responseCode);
                }
            } catch (Exception e) {
                logger.error("Error uploading file", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parent,
                        "上传文件失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                });
                return null;
            }
        });
    }
} 