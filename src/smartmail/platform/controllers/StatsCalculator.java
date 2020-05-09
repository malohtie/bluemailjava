package smartmail.platform.controllers;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import smartmail.platform.components.AccountingComponent;
import smartmail.platform.interfaces.Controller;
import smartmail.platform.logging.Logger;
import smartmail.platform.models.admin.Server;
import smartmail.platform.models.production.DropIp;
import smartmail.platform.parsers.TypesParser;
import smartmail.platform.remote.SSH;

public class StatsCalculator implements Controller {

    public StatsCalculator() {

    }

    public StatsCalculator(String[] args) throws Exception {
        start(args);
    }

    public void start(String[] parameters) throws Exception {
        try {
            List<Server> servers = new ArrayList<>();
            if (parameters.length > 1) {
                Integer ServerId = Integer.valueOf(TypesParser.safeParseInt(parameters[1]));
                Server serverObj = (Server)Server.first(Server.class, "id = ? AND status_id = ?", new Object[] { ServerId, Integer.valueOf(1) });
                servers.add(serverObj);
            } else {
                servers = Server.all(Server.class, "status_id = ?", new Object[] { Integer.valueOf(1) });
            }
            if (servers != null && !servers.isEmpty()) {
                for (Server server : servers) {
                    if (server != null) {
                        String logsFolder = (new File(System.getProperty("base.path"))).getAbsolutePath() + File.separator + "tmp" + File.separator + "pmta-logs" + File.separator + "server_" + server.id;
                        String today = LocalDate.now().toString();
                        if (!(new File(logsFolder + "/" + today)).exists()) {
                            (new File(logsFolder)).mkdirs();
                            (new File(logsFolder + "/" + today + "/bounces/")).mkdirs();
                            (new File(logsFolder + "/" + today + "/delivered/")).mkdirs();
                        }
                        SSH ssh = SSH.SSHPassword(server.mainIp, String.valueOf(server.sshPort), server.username, server.password);
                        ssh.connect();
                        if (!ssh.isConnected())
                            throw new Exception("Could not connect to the server : " + server.name + " !");
                        String[] types = { "delivered", "bounces" };
                        String result = "";
                        String prefix = "";
                        for (String type : types) {
                            prefix = "delivered".equalsIgnoreCase(type) ? "d" : "b";
                            result = ssh.cmd("awk 'FNR > 1' /etc/pmta/" + type + "/archived/*.csv > /etc/pmta/" + type + "/archived/" + today + "-clean.csv && find /etc/pmta/" + type + "/archived/" + today + "-clean.csv");
                            String[] archiveFiles = new String[0];
                            if (result != null && !"".equals(result)) {
                                result = result.replaceAll("(?m)^[ \t]*\r?\n", "");
                                archiveFiles = result.split("\n");
                                for (String file : archiveFiles) {
                                    try {
                                        String[] tmp = file.split("\\/");
                                        String fileName = tmp[tmp.length - 1];
                                        ssh.downloadFile(file, logsFolder + File.separator + today + File.separator + type + File.separator + fileName);
                                    } catch (Exception e) {
                                        Logger.error(e, StatsCalculator.class);
                                    }
                                }
                            }
                        }
                        ssh.cmd("rm -rf /etc/pmta/bounces/archived/*");
                        ssh.cmd("rm -rf /etc/pmta/delivered/archived/*");
                        ssh.disconnect();
                        HashMap<Integer, HashMap<Integer, AccountingComponent>> stats = new HashMap<>();
                        String[] lineParts = new String[0];
                        int dropId = 0;
                        int ipId = 0;
                        File[] bounceFiles = (new File(logsFolder + File.separator + today + File.separator + "bounces")).listFiles();
                        bounceFiles = (File[])ArrayUtils.addAll((Object[])bounceFiles, (Object[])(new File(logsFolder + File.separator + today + File.separator + "bounces")).listFiles());
                        List<String> lines = new ArrayList<>();
                        for (File bounceFile : bounceFiles) {
                            if (bounceFile.isFile())
                                lines.addAll(FileUtils.readLines(bounceFile));
                        }
                        for (String line : lines) {
                            if (!"".equals(line)) {
                                lineParts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                if (lineParts.length == 12 && ("hardbnc".equalsIgnoreCase(lineParts[1]) || "other".equalsIgnoreCase(lineParts[1])) && !"".equalsIgnoreCase(lineParts[10])) {
                                    dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                                    ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                                    if (!stats.containsKey(Integer.valueOf(dropId)))
                                        stats.put(Integer.valueOf(dropId), new HashMap<>());
                                    if (((HashMap)stats.get(Integer.valueOf(dropId))).containsKey(Integer.valueOf(ipId))) {
                                        ((AccountingComponent)((HashMap)stats.get(Integer.valueOf(dropId))).get(Integer.valueOf(ipId))).bounced++;
                                        continue;
                                    }
                                    ((HashMap<Integer, AccountingComponent>)stats.get(Integer.valueOf(dropId))).put(Integer.valueOf(ipId), new AccountingComponent(dropId, ipId, 0, 1));
                                }
                            }
                        }
                        File[] deliveredFiles = (new File(logsFolder + File.separator + today + File.separator + "delivered")).listFiles();
                        deliveredFiles = (File[])ArrayUtils.addAll((Object[])deliveredFiles, (Object[])(new File(logsFolder + File.separator + today + File.separator + "delivered")).listFiles());
                        lines = new ArrayList<>();
                        for (File deliveredFile : deliveredFiles) {
                            if (deliveredFile.isFile())
                                lines.addAll(FileUtils.readLines(deliveredFile));
                        }
                        for (String line2 : lines) {
                            if (!"".equals(line2)) {
                                lineParts = line2.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                                if (lineParts.length == 12 && "success".equalsIgnoreCase(lineParts[1]) && !"".equalsIgnoreCase(lineParts[10])) {
                                    dropId = TypesParser.safeParseInt(lineParts[10].split("_")[0]);
                                    ipId = TypesParser.safeParseInt(lineParts[10].split("_")[1]);
                                    if (!stats.containsKey(Integer.valueOf(dropId)))
                                        stats.put(Integer.valueOf(dropId), new HashMap<>());
                                    if (((HashMap)stats.get(Integer.valueOf(dropId))).containsKey(Integer.valueOf(ipId))) {
                                        ((AccountingComponent)((HashMap)stats.get(Integer.valueOf(dropId))).get(Integer.valueOf(ipId))).delivered++;
                                        continue;
                                    }
                                    ((HashMap<Integer, AccountingComponent>)stats.get(Integer.valueOf(dropId))).put(Integer.valueOf(ipId), new AccountingComponent(dropId, ipId, 1, 0));
                                }
                            }
                        }
                        for (Map.Entry<Integer, HashMap<Integer, AccountingComponent>> statsEntry : stats.entrySet()) {
                            dropId = ((Integer)statsEntry.getKey()).intValue();
                            HashMap<Integer, AccountingComponent> value = statsEntry.getValue();
                            if (dropId > 0 && value != null && !value.isEmpty())
                                for (Map.Entry<Integer, AccountingComponent> accountingEntry : value.entrySet()) {
                                    ipId = ((Integer)accountingEntry.getKey()).intValue();
                                    AccountingComponent accounting = accountingEntry.getValue();
                                    DropIp dropIp = (DropIp)DropIp.first(DropIp.class, "drop_id = ? AND ip_id = ?", new Object[] { Integer.valueOf(dropId), Integer.valueOf(ipId) });
                                    if (dropIp != null) {
                                        dropIp.delivered = accounting.delivered;
                                        dropIp.bounced = accounting.bounced;
                                        dropIp.update();
                                    }
                                }
                        }
                    }
                }
            } else {
                throw new Exception("No Servers Found To Calculate Pmta Logs !");
            }
        } catch (Exception e) {
            Logger.error(e, StatsCalculator.class);
        }
    }
}
