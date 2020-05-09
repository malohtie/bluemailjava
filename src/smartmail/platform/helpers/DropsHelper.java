package smartmail.platform.helpers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import smartmail.platform.components.DropComponent;
import smartmail.platform.logging.Logger;
import smartmail.platform.models.admin.Isp;
import smartmail.platform.models.admin.Offer;
import smartmail.platform.models.admin.OfferName;
import smartmail.platform.models.admin.OfferSubject;
import smartmail.platform.models.admin.Server;
import smartmail.platform.models.admin.Sponsor;
import smartmail.platform.models.admin.Vmta;
import smartmail.platform.models.production.Drop;
import smartmail.platform.models.production.DropIp;
import smartmail.platform.parsers.TypesParser;
import smartmail.platform.remote.SSH;
import smartmail.platform.utils.Mapper;
import smartmail.platform.utils.Strings;

public class DropsHelper {
    public static void saveDrop(DropComponent dropComponent, Server server) throws Exception {
        Drop drop = new Drop();
        drop.id = 0;
        drop.pids = dropComponent.pickupsFolder + File.separator + "drop_status_" + server.id;
        drop.userId = dropComponent.mailerId;
        drop.serverId = server.id;
        drop.ispId = dropComponent.ispId;
        drop.status = "in-progress";
        drop.startTime = new Timestamp(System.currentTimeMillis());
        drop.finishTime = null;
        drop.totalEmails = dropComponent.emailsCount;
        drop.sentProgress = 0;
        drop.offerId = dropComponent.offerId;
        drop.offerFromNameId = dropComponent.fromNameId;
        drop.offerSubjectId = dropComponent.subjectId;
        drop.recipientsEmails = String.join(",", (CharSequence[])dropComponent.testEmails);
        drop.header = new String(Base64.encodeBase64(String.join("\n\n", (CharSequence[])dropComponent.headers).getBytes()));
        drop.creativeId = dropComponent.creativeId;
        String[] arrayOfString = new String[0];
        for (Map.Entry<Integer, String> en : (Iterable<Map.Entry<Integer, String>>)dropComponent.lists.entrySet())
            arrayOfString = (String[])ArrayUtils.add((Object[])arrayOfString, String.valueOf(en.getValue()));
        drop.lists = String.join("|", (CharSequence[])arrayOfString);
        drop.postData = new String(Base64.encodeBase64(dropComponent.content.getBytes()));
        dropComponent.id = drop.insert();
        if (dropComponent.id == 0)
            throw new Exception("Error While Saving Drop !");
    }

    public static void saveDropVmta(DropComponent dropComponent, Vmta vmta, int totalSent) throws Exception {
        DropIp dropIp = new DropIp();
        dropIp.id = 0;
        dropIp.serverId = vmta.serverId;
        dropIp.ispId = dropComponent.ispId;
        dropIp.dropId = dropComponent.id;
        dropIp.ipId = vmta.ipId;
        dropIp.dropDate = new Timestamp(System.currentTimeMillis());
        dropIp.totalSent = totalSent;
        dropIp.delivered = 0;
        dropIp.bounced = 0;
        if (dropIp.insert() == 0)
            throw new Exception("Error While Saving Drop Ip !");
    }

    public static DropComponent parseDropFile(String content) throws Exception {
        DropComponent drop = null;
        if (!"".equalsIgnoreCase(content))
            try {
                TreeMap data = (TreeMap)(new ObjectMapper()).readValue(content, TreeMap.class);
                if (data != null && !data.isEmpty()) {
                    drop = new DropComponent();
                    drop.id = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "drop-id", "0")));
                    drop.isNewDrop = (drop.id == 0);
                    drop.isSend = "true".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "drop", "false")));
                    drop.content = content;
                    drop.mailerId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "user-id", "0")));
                    drop.randomTags = getAllRandomTags(drop.content);
                    drop.serversIds = Arrays.<String, Object>copyOf(((List)Mapper.getMapValue(data, "servers", new ArrayList())).toArray(), (((List)Mapper.getMapValue(data, "servers", new ArrayList())).toArray()).length, String[].class);
                    if (drop.serversIds != null && drop.serversIds.length > 0)
                        drop.servers = Server.all(Server.class, "id IN (" + String.join(",", (CharSequence[])drop.serversIds) + ")", null);
                    if (drop.servers == null || drop.servers.isEmpty())
                        throw new Exception("No Servers Found !");
                    drop.vmtasRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "vmtas-rotation", "1")));
                    drop.vmtasIds = Arrays.<String, Object>copyOf(((List)Mapper.getMapValue(data, "selected-vmtas", new ArrayList())).toArray(), (((List)Mapper.getMapValue(data, "selected-vmtas", new ArrayList())).toArray()).length, String[].class);
                    if (drop.vmtasIds != null && drop.vmtasIds.length > 0) {
                        String condition = "id IN (";
                        for (String vmtasId : drop.vmtasIds) {
                            if (vmtasId != null && vmtasId.contains("|"))
                                condition = condition + vmtasId.split("\\|")[1] + ",";
                        }
                        condition = condition.substring(0, condition.length() - 1) + ")";
                        drop.vmtas = Vmta.all(Vmta.class, condition, null);
                    }
                    if (drop.vmtas == null || drop.vmtas.isEmpty())
                        throw new Exception("No Vmtas Found !");
                    drop.vmtasEmailsProcces = String.valueOf(Mapper.getMapValue(data, "vmtas-emails-proccess", "vmtas-rotation"));
                    drop.batch = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "batch", "1")));
                    drop.delay = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "x-delay", "1")));
                    drop.numberOfEmails = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "number-of-emails", "0")));
                    drop.emailsPeriodValue = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "emails-period-value", "0")));
                    drop.emailsPeriodType = String.valueOf(Mapper.getMapValue(data, "emails-period-type", "seconds"));
                    if ("emails-per-period".equalsIgnoreCase(drop.vmtasEmailsProcces)) {
                        if (drop.numberOfEmails == 0)
                            throw new Exception("Number of Emails for Period is 0 !");
                        drop.batch = 1;
                        switch (drop.emailsPeriodType) {
                            case "seconds":
                                drop.emailsPeriodValue *= 1000;
                                break;
                            case "minutes":
                                drop.emailsPeriodValue = drop.emailsPeriodValue * 60 * 1000;
                                break;
                            case "hours":
                                drop.emailsPeriodValue = drop.emailsPeriodValue * 60 * 60 * 1000;
                                break;
                        }
                        drop.delay = (int)Math.ceil((drop.emailsPeriodValue / drop.numberOfEmails));
                    }
                    drop.sponsorId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "sponsor", "0")));
                    drop.sponsor = new Sponsor(Integer.valueOf(drop.sponsorId));
                    drop.offerId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "offer", "0")));
                    drop.offer = new Offer(Integer.valueOf(drop.offerId));
                    drop.creativeId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "creative", "0")));
                    drop.fromNameId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "from-name-id", "0")));
                    drop.fromName = String.valueOf(Mapper.getMapValue(data, "from-name-text", ""));
                    if ("".equals(drop.fromName) && drop.fromNameId > 0) {
                        drop.fromNameObject = new OfferName(Integer.valueOf(drop.fromNameId));
                        if (drop.fromNameObject != null && drop.fromNameObject.value != null)
                            drop.fromName = drop.fromNameObject.value;
                    }
                    drop.subjectId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "subject-id", "0")));
                    drop.subject = String.valueOf(Mapper.getMapValue(data, "subject-text", ""));
                    if ("".equals(drop.subject) && drop.subjectId > 0) {
                        drop.subjectObject = new OfferSubject(Integer.valueOf(drop.subjectId));
                        if (drop.subjectObject != null && drop.subjectObject.value != null)
                            drop.subject = drop.subjectObject.value;
                    }
                    drop.headersRotation = 1;
                    drop.headers = Arrays.<String, Object>copyOf(((List)Mapper.getMapValue(data, "headers", new ArrayList())).toArray(), (((List)Mapper.getMapValue(data, "headers", new ArrayList())).toArray()).length, String[].class);
                    drop.bounceEmail = String.valueOf(Mapper.getMapValue(data, "bounce-email", ""));
                    drop.returnPath = String.valueOf(Mapper.getMapValue(data, "return-path", ""));
                    if (!drop.bounceEmail.contains("@") && !drop.returnPath.contains("@"))
                        drop.bounceEmail = drop.returnPath = (!"".equals(drop.bounceEmail) && !"".equals(drop.returnPath)) ? (drop.bounceEmail + "@" + drop.returnPath) : "";
                    drop.fromEmail = String.valueOf(Mapper.getMapValue(data, "from-email", "from@[domain]"));
                    drop.replyTo = String.valueOf(Mapper.getMapValue(data, "reply-to", "reply@[domain]"));
                    drop.received = String.valueOf(Mapper.getMapValue(data, "received", ""));
                    drop.to = String.valueOf(Mapper.getMapValue(data, "to", "[email]"));
                    drop.placeholdersRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "placeholders-rotation", "1")));
                    drop.placeholders = !"".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "body-placeholders", ""))) ? String.valueOf(Mapper.getMapValue(data, "body-placeholders", "")).split("\r\n") : new String[0];
                    drop.hasPlaceholders = (drop.placeholders.length > 0);
                    drop.uploadImages = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "upload-images", "off")));
                    drop.charset = String.valueOf(Mapper.getMapValue(data, "charset", "utf-8"));
                    drop.contentTransferEncoding = String.valueOf(Mapper.getMapValue(data, "content-transfer-encoding", "7bit"));
                    drop.contentType = String.valueOf(Mapper.getMapValue(data, "content-type", "text/html"));
                    drop.body = String.valueOf(Mapper.getMapValue(data, "body", ""));
                    drop.trackOpens = drop.isSend ? "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "track-opens", "off"))) : false;
                    drop.ispId = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "isp-id", "0")));
                    drop.isp = new Isp(Integer.valueOf(drop.ispId));
                    drop.staticDomain = String.valueOf(Mapper.getMapValue(data, "static-domain", ""));
                    drop.emailsSplitType = String.valueOf(Mapper.getMapValue(data, "emails-split-type", "vmtas"));
                    drop.testFrequency = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "send-test-after", "-1")));
                    drop.testEmails = String.valueOf(Mapper.getMapValue(data, "recipients-emails", "")).split("\\;");
                    drop.rcptfrom = String.valueOf(Mapper.getMapValue(data, "rcpt-from", ""));
                    drop.dataStart = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "data-start", "0")));
                    drop.dataCount = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "data-count", "0")));
                    drop.emailsPerSeeds = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "emails-per-seed", "1")));
                    drop.emailsCount = 0;
                    String[] listsParts;
                    if (drop.isSend && !"".equals(Mapper.getMapValue(data, "lists", "")) && (listsParts = String.valueOf(Mapper.getMapValue(data, "lists", "")).split("\\,")) != null && listsParts.length > 0) {
                        drop.lists = new HashMap<>();
                        for (String tmp : listsParts)
                            drop.lists.put(Integer.valueOf(TypesParser.safeParseInt(String.valueOf(tmp.split("\\|")[0]))), String.valueOf(tmp.split("\\|")[1]));
                    }
                    int n = drop.listsCount = (drop.lists != null) ? drop.lists.size() : 0;
                    if (drop.isSend) {
                        drop.isAutoResponse = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "auto-response", "off")));
                        drop.randomCaseAutoResponse = "on".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "random-case-auto-response", "off")));
                        drop.autoResponseRotation = TypesParser.safeParseInt(String.valueOf(Mapper.getMapValue(data, "auto-response-frequency", "0")));
                        drop.autoReplyEmails = !"".equalsIgnoreCase(String.valueOf(Mapper.getMapValue(data, "auto-reply-emails", ""))) ? String.valueOf(Mapper.getMapValue(data, "auto-reply-emails", "")).split("\r\n") : new String[0];
                        drop.hasAutoReply = (drop.autoReplyEmails.length > 0);
                    }
                    drop.redirectFileName = "r.php";
                    drop.optoutFileName = "optout.php";
                    String dataSourcePath = (new File(System.getProperty("base.path"))).getAbsolutePath() + "/applications/bluemail/configs/application.ini";
                    HashMap<String, String> map;
                    if ((new File((new File(System.getProperty("base.path"))).getAbsolutePath() + "/applications/bluemail/configs/application.ini")).exists() && (map = Mapper.readProperties(dataSourcePath)) != null && !map.isEmpty()) {
                        if (map.containsKey("redirect_file"))
                            drop.redirectFileName = String.valueOf(map.get("redirect_file"));
                        if (map.containsKey("optout_file"))
                            drop.optoutFileName = String.valueOf(map.get("optout_file"));
                    }
                }
            } catch (Exception e) {
                Logger.error(e, DropsHelper.class);
            }
        return drop;
    }

    public static String[] getAllRandomTags(String content) {
        String[] tags = new String[0];
        Pattern p = Pattern.compile("\\[(.*?)\\]");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String match = m.group(1);
            String tag = match.replaceAll("[0-9]", "");
            if (!"a".equalsIgnoreCase(tag) && !"an".equalsIgnoreCase(tag) && !"al".equalsIgnoreCase(tag) && !"au".equalsIgnoreCase(tag) && !"anl".equalsIgnoreCase(tag) && !"anu".equalsIgnoreCase(tag) && !"n".equalsIgnoreCase(tag))
                continue;
            tags = (String[])ArrayUtils.add((Object[])tags, match);
        }
        return tags;
    }

    public static String replaceRandomTags(String value, String[] randomTags) {
        if (value != null && !"".equals(value) && randomTags != null && randomTags.length > 0)
            for (String randomTag : randomTags) {
                if (value.contains(randomTag))
                    value = StringUtils.replace(value, "[" + randomTag + "]", replaceRandomTag(randomTag));
            }
        return value;
    }

    public static String replaceRandomTag(String tag) {
        int size = TypesParser.safeParseInt(tag.replaceAll("[^0-9]", ""));
        String type;
        switch (type = tag.replaceAll("[0-9]", "")) {
            case "a":
                return Strings.getSaltString(size, true, true, false, false);
            case "al":
                return Strings.getSaltString(size, true, true, false, false).toLowerCase();
            case "au":
                return Strings.getSaltString(size, true, true, false, false).toUpperCase();
            case "an":
                return Strings.getSaltString(size, true, true, true, false);
            case "anl":
                return Strings.getSaltString(size, true, true, true, false).toLowerCase();
            case "anu":
                return Strings.getSaltString(size, true, true, true, false).toUpperCase();
            case "n":
                return Strings.getSaltString(size, false, false, true, false);
        }
        return "";
    }

    public static void uploadImage(DropComponent drop, SSH ssh) {
        if (drop != null && !"".equalsIgnoreCase(drop.body) && ssh != null && ssh.isConnected())
            try {
                Document doc = Jsoup.parse(drop.body);
                Elements images = doc.select("img");
                ByteArrayOutputStream out = null;
                for (Element image : images) {
                    String src = image.attr("src");
                    URL url = new URL(src);
                    URLConnection uc = url.openConnection();
                    uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
                    uc.connect();
                    InputStream inStream = uc.getInputStream();
                    if (inStream == null || inStream.available() <= 0)
                        continue;
                    try (BufferedInputStream in = new BufferedInputStream(inStream)) {
                        out = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];
                        int n = 0;
                        while (-1 != (n = in.read(buf)))
                            out.write(buf, 0, n);
                        out.close();
                    }
                    byte[] response;
                    if (out.size() <= 0 || (response = out.toByteArray()) == null || response.length <= 0 || !(new File(System.getProperty("base.path") + "/tmp/")).exists())
                        continue;
                    String extension = src.substring(src.lastIndexOf("."));
                    String imageName = Strings.getSaltString(20, true, true, true, false) + extension;
                    try (FileOutputStream fos = new FileOutputStream(System.getProperty("base.path") + "/tmp/" + imageName)) {
                        fos.write(response);
                    }
                    if (!(new File(System.getProperty("base.path") + "/tmp/" + imageName)).exists())
                        continue;
                    ssh.uploadFile(System.getProperty("base.path") + "/tmp/" + imageName, "/var/www/html/img/" + imageName);
                    drop.body = StringUtils.replace(drop.body, src, "http://[domain]/img/" + imageName);
                    (new File(System.getProperty("base.path") + "/tmp/" + imageName)).delete();
                }
            } catch (Exception exception) {}
    }

    public static void writeThreadStatusFile(int serverId, String folder) {
        try {
            FileUtils.writeStringToFile(new File(folder + File.separator + "drop_status_" + serverId), "0");
        } catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
    }

    public static boolean hasToStopDrop(int serverId, String folder) {
        boolean stop = false;
        try {
            stop = String.valueOf(FileUtils.readFileToString(new File(folder + File.separator + "drop_status_" + serverId))).trim().contains("1");
            if (stop)
                System.out.println("Stoped !");
        } catch (Exception e) {
            Logger.error(e, DropsHelper.class);
        }
        return stop;
    }
}
