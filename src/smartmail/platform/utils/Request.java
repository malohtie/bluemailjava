package smartmail.platform.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Request {
    public static String[] downloadFile(String link, String fileName) throws Exception {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setReadTimeout(120000);
        connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        connection.addRequestProperty("User-Agent", "Mozilla");
        connection.addRequestProperty("Referer", "google.com");
        boolean redirect = false;
        int status = connection.getResponseCode();
        if ((status != 200 && status == 302) || status == 301 || status == 303)
            redirect = true;
        if (redirect) {
            String newUrl = connection.getHeaderField("Location");
            connection = (HttpURLConnection)(new URL(newUrl)).openConnection();
            connection.setReadTimeout(120000);
            connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            connection.addRequestProperty("User-Agent", "Mozilla");
            connection.addRequestProperty("Referer", "google.com");
        }
        String type = connection.getContentType();
        long totalDataRead = 0L;
        if (type == null)
            throw new Exception("Unsupported File Type !");
        System.out.println("FILE TYPE -> " + type);
        if (type.toLowerCase().contains("application/zip") || type.toLowerCase().contains("application/x-zip-compressed")) {
            fileName = fileName + ".zip";
        } else if (type.toLowerCase().contains("text/plain") || type.toLowerCase().contains("application/csv") || type.toLowerCase().contains("text/csv") || type.toLowerCase().contains("application/octet-stream")) {
            fileName = fileName + ".txt";
        } else {
            throw new Exception("Unsupported File Type !");
        }
        try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream())) {
            FileOutputStream fos = new FileOutputStream(fileName);
            try (BufferedOutputStream bout = new BufferedOutputStream(fos, 4096)) {
                byte[] data = new byte[4096];
                int i;
                while ((i = in.read(data, 0, 4096)) >= 0) {
                    totalDataRead += i;
                    bout.write(data, 0, i);
                }
            }
        }
        return new String[] { fileName, type };
    }
}
