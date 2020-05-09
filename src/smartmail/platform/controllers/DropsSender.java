package smartmail.platform.controllers;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import smartmail.platform.components.DropComponent;
import smartmail.platform.components.RotatorComponent;
import smartmail.platform.exceptions.ThreadException;
import smartmail.platform.helpers.DropsHelper;
import smartmail.platform.interfaces.Controller;
import smartmail.platform.models.admin.Server;
import smartmail.platform.models.admin.Vmta;
import smartmail.platform.workers.ServerWorker;

public class DropsSender implements Controller {
    public static volatile RotatorComponent PLACEHOLDERS_ROTATOR;

    public static volatile RotatorComponent HEADERS_ROTATOR;

    public static volatile RotatorComponent AUTOREPLY_ROTATOR;

    public DropsSender(String[] path) throws Exception {
        System.out.println("Start Dropping");
        start(path);
    }

    public void start(String[] parameters) throws Exception {
        System.out.println("Start Drop");
        File dropFile = new File(parameters[1]);
        if (dropFile.exists()) {
            DropComponent drop = DropsHelper.parseDropFile(FileUtils.readFileToString(dropFile));
            if (drop != null) {
                PLACEHOLDERS_ROTATOR = drop.hasPlaceholders ? new RotatorComponent(Arrays.asList(drop.placeholders), drop.placeholdersRotation) : null;
                AUTOREPLY_ROTATOR = drop.hasAutoReply ? new RotatorComponent(Arrays.asList(drop.autoReplyEmails), drop.autoResponseRotation) : null;
                HEADERS_ROTATOR = new RotatorComponent(Arrays.asList(drop.headers), drop.headersRotation);
                if (!drop.servers.isEmpty() && !drop.vmtas.isEmpty()) {
                    ExecutorService serversExecutor = Executors.newFixedThreadPool(drop.servers.size());
                    ArrayList<Vmta> serverVmtas = null;
                    int offset = 0;
                    int vmtasLimit = 0;
                    int serverLimit = 0;
                    int limitRest = 0;
                    if (drop.isSend)
                        if ("servers".equalsIgnoreCase(drop.emailsSplitType)) {
                            serverLimit = (int)Math.ceil((drop.dataCount / drop.servers.size()));
                            limitRest = drop.dataCount - serverLimit * drop.servers.size();
                        } else {
                            vmtasLimit = (int)Math.ceil((drop.dataCount / drop.vmtas.size()));
                            limitRest = drop.dataCount - vmtasLimit * drop.vmtas.size();
                        }
                    for (int i = 0; i < drop.servers.size(); i++) {
                        Server server = drop.servers.get(i);
                        if (server != null && server.id > 0) {
                            if (drop.isSend && "vmtas".equalsIgnoreCase(drop.emailsSplitType))
                                serverLimit = 0;
                            serverVmtas = new ArrayList<>();
                            if (!drop.vmtas.isEmpty())
                                for (Vmta vmta : drop.vmtas) {
                                    if (vmta.serverId != server.id)
                                        continue;
                                    serverVmtas.add(vmta);
                                    if (!drop.isSend || !"vmtas".equalsIgnoreCase(drop.emailsSplitType))
                                        continue;
                                    serverLimit += vmtasLimit;
                                }
                            if (i == drop.servers.size() - 1)
                                serverLimit += limitRest;
                            ServerWorker worker = new ServerWorker((DropComponent)SerializationUtils.clone((Serializable)drop), server, serverVmtas, offset, serverLimit);
                            worker.setUncaughtExceptionHandler((Thread.UncaughtExceptionHandler)new ThreadException());
                            serversExecutor.submit((Runnable)worker);
                            offset += serverLimit;
                        }
                    }
                    serversExecutor.shutdown();
                    serversExecutor.awaitTermination(10L, TimeUnit.DAYS);
                }
            } else {
                throw new Exception("No Drop Content Found !");
            }
            if (dropFile.exists())
                dropFile.delete();
        } else {
            throw new Exception("No Drop File Found !");
        }
    }

    public static synchronized String getCurrentPlaceHolder() {
        return (PLACEHOLDERS_ROTATOR != null) ? (String)PLACEHOLDERS_ROTATOR.getCurrentValue() : "";
    }

    public static synchronized void rotatePlaceHolders() {
        if (PLACEHOLDERS_ROTATOR != null)
            PLACEHOLDERS_ROTATOR.rotate();
    }

    public static synchronized String getCurrentHeader() {
        return (HEADERS_ROTATOR != null) ? (String)HEADERS_ROTATOR.getCurrentValue() : "";
    }

    public static synchronized void rotateHeaders() {
        if (HEADERS_ROTATOR != null)
            HEADERS_ROTATOR.rotate();
    }

    public static synchronized String getCurrentAutoReply() {
        return (AUTOREPLY_ROTATOR != null) ? (String)AUTOREPLY_ROTATOR.getCurrentValue() : "";
    }

    public static synchronized void rotateAutoReply() {
        if (AUTOREPLY_ROTATOR != null)
            AUTOREPLY_ROTATOR.rotate();
    }
}
