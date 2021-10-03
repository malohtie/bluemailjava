package smartmail.platform.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Request {
    public static String[] downloadFile(String link, String fileName) throws Exception {
        
        String location = link;
        HttpURLConnection connection = null;
        URL url = null;
        
        int times = 0;

        for (;;) {
            if(times > 10) {
                throw new Exception("To mush redirects");
            }
            
            url=new URL(location);
            HttpURLConnection.setFollowRedirects(false);
            connection = (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.connect();

            if(connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
            {
                String redirectLocation = connection.getHeaderField("Location");
                if (redirectLocation != null)
                    location = redirectLocation;
                else
                    break;
               
                System.out.println(redirectLocation);
            }
            else {
                break;
            }
            times ++;
        }

        String type = connection.getContentType();
        long totalDataRead = 0L;
        if (type == null)
            throw new Exception("Unsupported File Type !");
        System.out.println("FILE TYPE -> " + type);
        if (type.toLowerCase().contains("application/zip") || type.toLowerCase().contains("application/x-zip") || type.toLowerCase().contains("application/x-zip-compressed") || type.toLowerCase().contains("binary/octet-stream") ) {
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
