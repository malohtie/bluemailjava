package smartmail.platform.workers;

import org.apache.commons.io.FileUtils;
import smartmail.platform.controllers.SuppressionManager;
import smartmail.platform.logging.Logger;
import smartmail.platform.models.admin.DataList;
import smartmail.platform.orm.Database;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class SupressionWorker extends Thread {
    public int proccessId;

    public int offerId;

    public DataList dataList;

    public boolean isMd5;

    public String directory;

    public int listsSize;

    public SupressionWorker(int proccessId, int offerId, DataList dataList, boolean isMd5, String directory, int listsSize) {
        this.proccessId = proccessId;
        this.offerId = offerId;
        this.dataList = dataList;
        this.isMd5 = isMd5;
        this.directory = directory;
        this.listsSize = listsSize;
    }

    public void run() {
        try {
            if (this.dataList != null && this.proccessId > 0 && this.offerId > 0) {
                List<String> suppressionEmails = new ArrayList<>();
                String[] columns = null;
                String schema = this.dataList.name.split("\\.")[0];
                String table = this.dataList.name.split("\\.")[1];
                if (table.startsWith("fresh_") || table.startsWith("clean_")) {
                    columns = new String[] { "id", "email", "fname", "lname", "offers_excluded" };
                } else if (table.startsWith("unsubscribers_")) {
                    columns = new String[] {
                            "id", "email", "fname", "lname", "drop_id", "action_date", "message", "offers_excluded", "verticals", "agent",
                            "ip", "country", "region", "city", "language", "device_type", "device_name", "os", "browser_name", "browser_version" };
                } else {
                    columns = new String[] {
                            "id", "email", "fname", "lname", "action_date", "offers_excluded", "verticals", "agent", "ip", "country",
                            "region", "city", "language", "device_type", "device_name", "os", "browser_name", "browser_version" };
                }
                List<LinkedHashMap<String, Object>> totalEmails = getsuppressionEmails(suppressionEmails, columns);
                if (!suppressionEmails.isEmpty() && !totalEmails.isEmpty() && columns != null) {
                    Collections.sort(suppressionEmails);
                    suppressionEmails.retainAll(SuppressionManager.MD5_EMAILS);
                    HashSet<String> hashset = new HashSet();
                    hashset.addAll(suppressionEmails);
                    suppressionEmails.clear();
                    suppressionEmails.addAll(hashset);
                    String csv = convertEmailsToCsv(totalEmails, suppressionEmails, columns);
                    if (!"".equalsIgnoreCase(csv)) {
                        FileUtils.writeStringToFile(new File(this.directory + File.separator + this.dataList.name + ".csv"), csv);
                        String exists = "false";
                        List<LinkedHashMap<String, Object>> res = Database.get("lists").executeQuery("SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace WHERE  n.nspname = '" + schema + "' AND c.relname = '" + table + "_suppression_copy' AND c.relkind = 'r');", null, 1);
                        if (res != null && !res.isEmpty())
                            exists = String.valueOf(((LinkedHashMap)res.get(0)).get("exists"));
                        if ("true".equals(exists))
                            Database.get("lists").executeUpdate("DROP TABLE " + this.dataList.name + "_suppression_copy", null, 0);
                        Runtime.getRuntime().exec(new String[] { "bash", "-c", "chmod a+rw " + this.directory + File.separator + this.dataList.name + ".csv" });
                        Database.get("lists").executeUpdate("CREATE TABLE " + this.dataList.name + "_suppression_copy ( like " + this.dataList.name + " including defaults including constraints including indexes )", null, 0);
                        Database.get("lists").executeUpdate("COPY " + this.dataList.name + "_suppression_copy FROM '" + this.directory + File.separator + this.dataList.name + ".csv' WITH CSV HEADER DELIMITER AS ',' NULL AS '';", null, 0);
                        List<LinkedHashMap<String, Object>> result = Database.get("lists").executeQuery("SELECT (SELECT COUNT(id) AS count1 FROM " + this.dataList.name + ") - (SELECT COUNT(id) AS count2 FROM " + this.dataList.name + "_suppression_copy) AS difference", null, 0);
                        boolean identical = false;
                        if (result != null && !result.isEmpty() && ((LinkedHashMap)result.get(0)).containsKey("difference"))
                            identical = "0".equalsIgnoreCase(String.valueOf(((LinkedHashMap)result.get(0)).get("difference")));
                        if (identical == true) {
                            Database.get("lists").executeUpdate("DROP TABLE " + this.dataList.name, null, 0);
                            Database.get("lists").executeUpdate("ALTER TABLE " + this.dataList.name + "_suppression_copy RENAME TO " + this.dataList.name.split("\\.")[1], null, 0);
                        }
                        SuppressionManager.updateProccess(this.proccessId, this.listsSize, suppressionEmails.size());
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(e, SupressionWorker.class);
        }
    }

    public List<LinkedHashMap<String, Object>> getsuppressionEmails(List<String> suppressionEmails, String[] columns) {
        List<LinkedHashMap<String, Object>> emails = null;
        try {
            emails = Database.get("lists").executeQuery("SELECT " + String.join(",", (CharSequence[])columns) + ",md5(email) as md5_email FROM " + this.dataList.name, null, 1);
            for (LinkedHashMap<String, Object> row : emails) {
                if (row != null)
                    suppressionEmails.add(String.valueOf(row.get("md5_email")).trim());
            }
        } catch (Exception e) {
            Logger.error(e, SupressionWorker.class);
        }
        return emails;
    }

    public String convertEmailsToCsv(List<LinkedHashMap<String, Object>> totalEmails, List<String> suppressionEmails, String[] columns) throws SQLException {
        StringBuilder csv = new StringBuilder();
        boolean insertOfferId = false;
        List<String> offerIds = null;
        for (int i = 0; i < columns.length; i++) {
            csv.append("\"").append(columns[i]);
            if (i < columns.length - 1) {
                csv.append("\"").append(",");
            } else {
                csv.append("\"");
            }
        }
        csv.append("\n");
        for (LinkedHashMap<String, Object> row : totalEmails) {
            insertOfferId = false;
            if (suppressionEmails.contains(String.valueOf(row.get("md5_email")).trim()))
                insertOfferId = true;
            for (int j = 0; j < columns.length; j++) {
                csv.append("\"");
                if ("offers_excluded".equalsIgnoreCase(columns[j])) {
                    if (row.get(columns[j]) == null || "null".equalsIgnoreCase(String.valueOf(row.get(columns[j]))) || "".equalsIgnoreCase(String.valueOf(row.get(columns[j])))) {
                        if (insertOfferId == true) {
                            csv.append(this.offerId);
                        } else {
                            csv.append("");
                        }
                    } else {
                        if (insertOfferId == true) {
                            offerIds = new ArrayList<>(new HashSet<>(Arrays.asList((this.offerId + "," + String.valueOf(row.get(columns[j]))).split(","))));
                        } else {
                            offerIds = new ArrayList<>(new HashSet<>(Arrays.asList(String.valueOf(row.get(columns[j])).split(","))));
                        }
                        for (int k = 0; k < offerIds.size(); k++) {
                            if (!"".equalsIgnoreCase(((String)offerIds.get(k)).trim())) {
                                csv.append(offerIds.get(k));
                                if (k < offerIds.size() - 1)
                                    csv.append(",");
                            }
                        }
                    }
                } else if (row.get(columns[j]) == null || "null".equalsIgnoreCase(String.valueOf(row.get(columns[j])))) {
                    csv.append("");
                } else {
                    csv.append(String.valueOf(row.get(columns[j])).replaceAll("\"", "\\\""));
                }
                if (j < columns.length - 1) {
                    csv.append("\"").append(",");
                } else {
                    csv.append("\"");
                }
            }
            csv.append("\n");
        }
        return csv.toString();
    }
}
