package smartmail.platform.controllers;

import java.io.File;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.exceptions.ThreadException;
import smartmail.platform.interfaces.Controller;
import smartmail.platform.logging.Logger;
import smartmail.platform.models.admin.DataList;
import smartmail.platform.orm.Database;
import smartmail.platform.parsers.TypesParser;
import smartmail.platform.utils.Compressor;
import smartmail.platform.utils.Request;
import smartmail.platform.utils.Strings;
import smartmail.platform.workers.SupressionWorker;

public class SuppressionManager implements Controller {
    public static volatile Set<String> MD5_EMAILS = new HashSet<>();

    public static volatile int INDEX = 1;

    public SuppressionManager(String[] args) throws Exception {
        start(args);
    }

    public void start(String[] parameters) throws Exception {
        int proccessId = TypesParser.safeParseInt(parameters[1]);
        String suppressionsFolder = (new File(System.getProperty("base.path"))).getAbsolutePath() + File.separator + "tmp" + File.separator + "suppressions";
        String tempDirectory = Strings.getSaltString(20, true, true, true, false);
        boolean errorOccured = false;
        try {
            int offerId = TypesParser.safeParseInt(parameters[2]);
            String link = new String(Base64.getDecoder().decode(parameters[3]));
            System.out.println("File Link -> " + link);
            if (proccessId == 0)
                throw new Exception("No Proccess Id Found !");
            if ("".equals(link))
                throw new Exception("No Link Found !");
            (new File(suppressionsFolder + File.separator + tempDirectory)).mkdirs();
            String fileName = "suppression_" + Strings.getSaltString(10, true, true, true, false);
            System.out.println("Folder -> " + fileName);
            String[] fileInfo = Request.downloadFile(link.trim(), suppressionsFolder + File.separator + tempDirectory + File.separator + fileName);
            System.out.println("FILE INFO -> " + fileInfo);
            String emailsFile = "";
            if (fileInfo.length == 0)
                throw new Exception("Could not Download the File !");
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'in-progress' , progress = '0%' , emails_found = 0 WHERE Id = ?", new Object[] { Integer.valueOf(proccessId) }, 0);
            if (fileInfo[1].toLowerCase().contains("application/zip") || fileInfo[1].toLowerCase().contains("application/x-zip-compressed")) {
                String zipDirectory = suppressionsFolder + File.separator + tempDirectory + File.separator + Strings.getSaltString(10, true, true, true, false) + File.separator;
                (new File(zipDirectory)).mkdir();
                Compressor.unzip(fileInfo[0], zipDirectory);
                File file = new File(zipDirectory);
                for (File child : file.listFiles()) {
                    if (!child.getName().toLowerCase().contains("domain"))
                        emailsFile = child.getAbsolutePath();
                }
            } else if (fileInfo[1].toLowerCase().contains("text/plain") || fileInfo[1].toLowerCase().contains("application/csv") || fileInfo[1].toLowerCase().contains("text/csv") || fileInfo[1].toLowerCase().contains("application/octet-stream")) {
                emailsFile = fileInfo[0];
            }
            if (!(new File(emailsFile)).exists())
                throw new Exception("Suppression File Not Found !");
            System.out.println("EMAILS -> " + emailsFile);
            List<String> md5lines = FileUtils.readLines(new File(emailsFile));
            boolean isMd5 = (((String)md5lines.get(0)).length() == 32 && !((String)md5lines.get(0)).contains("@"));
            Collections.sort(md5lines);
            MD5_EMAILS = Collections.unmodifiableSet(new HashSet<>(md5lines));
            List<DataList> dataLists = DataList.all(DataList.class);
            if (dataLists == null || dataLists.isEmpty())
                throw new Exception("Data Lists Not Found !");
            int listsSize = dataLists.size();
            ExecutorService executor = Executors.newFixedThreadPool(30);
            SupressionWorker worker = null;
            for (DataList dataList : dataLists) {
                if (dataList.id > 0 && dataList.name != null && !"".equalsIgnoreCase(dataList.name) && !dataList.name.contains("seeds")) {
                    worker = new SupressionWorker(proccessId, offerId, dataList, isMd5, suppressionsFolder + File.separator + tempDirectory, listsSize);
                    worker.setUncaughtExceptionHandler((Thread.UncaughtExceptionHandler)new ThreadException());
                    executor.submit((Runnable)worker);
                }
            }
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.DAYS);
        } catch (Exception e) {
            interruptProccess(proccessId, suppressionsFolder + File.separator + tempDirectory);
            Logger.error(e, SuppressionManager.class);
            errorOccured = true;
        } finally {
            if (!errorOccured)
                finishProccess(proccessId, suppressionsFolder + File.separator + tempDirectory);
        }
    }

    public void interruptProccess(int proccessId, String directory) {
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            long time = cal.getTimeInMillis();
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(time), Integer.valueOf(proccessId) }, 0);
            FileUtils.deleteDirectory(new File(directory));
        } catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public void finishProccess(int proccessId, String directory) {
        try {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            long time = cal.getTimeInMillis();
            Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET status = 'completed' ,  progress = '100%' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(time), Integer.valueOf(proccessId) }, 0);
            FileUtils.deleteDirectory(new File(directory));
        } catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public static synchronized void updateProccess(int proccessId, int size, int emailsFound) throws DatabaseException {
        int progress = (int)((getIndex() / size) * 100.0D);
        Database.get("master").executeUpdate("UPDATE admin.suppression_proccesses SET progress = '" + progress + "%' ,emails_found = emails_found + " + emailsFound + " WHERE Id = ?", new Object[] { Integer.valueOf(proccessId) }, 0);
        updateIndex();
    }

    public static synchronized int getIndex() {
        return INDEX;
    }

    public static synchronized void updateIndex() {
        INDEX++;
    }
}
