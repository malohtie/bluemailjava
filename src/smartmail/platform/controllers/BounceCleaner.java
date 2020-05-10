package smartmail.platform.controllers;

import smartmail.platform.exceptions.DatabaseException;
import smartmail.platform.exceptions.ThreadException;
import smartmail.platform.interfaces.Controller;
import smartmail.platform.logging.Logger;
import smartmail.platform.models.admin.Server;
import smartmail.platform.orm.Database;
import smartmail.platform.parsers.TypesParser;
import smartmail.platform.workers.BounceWorker;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BounceCleaner implements Controller {
    public static volatile int COUNT = 0;

    public static volatile int INDEX = 1;

    public BounceCleaner(String[] args) throws Exception {
        start(args);
    }

    public void start(String[] parameters) throws Exception {
        int proccessId = TypesParser.safeParseInt(parameters[1]);
        boolean errorOccured = false;
        try {
            String listName = parameters[2];
            int ispId = TypesParser.safeParseInt(parameters[3]);
            int userId = TypesParser.safeParseInt(parameters[4]);
            System.out.println("CL");
            List<Server> servers = new ArrayList<>();
            if (proccessId == 0)
                throw new Exception("No Proccess Id Found !");
            if (listName == null || "".equals(listName))
                throw new Exception("No List Name Found !");
            if (parameters.length > 4) {
                int serverId = TypesParser.safeParseInt(parameters[5]);
                Controller controllerCalcul = new StatsCalculator();
                String[] args = { "send_stats", parameters[5] };
                controllerCalcul.start(args);
                Server serverObj = (Server)Server.first(Server.class,
                        "id = ? AND status_id = ?",
                        new Object[] { Integer.valueOf(serverId), Integer.valueOf(1) });
                servers.add(serverObj);
            } else {
                Controller controllerCalcul2 = new StatsCalculator();
                String[] args2 = { "send_stats" };
                if (controllerCalcul2 != null)
                    controllerCalcul2.start(parameters);
                servers = Server.all(Server.class, "status_id = ?", new Object[] { Integer.valueOf(1) });
            }
            if (servers != null && !servers.isEmpty()) {
                ExecutorService serversExecutor = Executors.newFixedThreadPool((servers.size() > 15) ? 15 : servers.size());
                BounceWorker worker = null;
                for (Server server : servers) {
                    if (server != null) {
                        System.out.println("Start Cleaninf for server -> " + server.name);
                        worker = new BounceWorker(proccessId, listName, server, userId, ispId);
                        worker.setUncaughtExceptionHandler((Thread.UncaughtExceptionHandler)new ThreadException());
                        serversExecutor.submit((Runnable)worker);
                    }
                }
                serversExecutor.shutdown();
                serversExecutor.awaitTermination(10L, TimeUnit.DAYS);
            } else {
                throw new Exception("No Servers Found To Clean Bounce From !");
            }
        } catch (Exception e) {
            interruptProccess(proccessId);
            Logger.error(e, BounceCleaner.class);
            errorOccured = true;
        } finally {
            if (!errorOccured)
                finishProccess(proccessId);
        }
    }

    public static synchronized void updateProccess(int proccessId, String type) throws DatabaseException {
        int progress = (int)((getIndex() / getCount()) * 100.0D);
        String update = "bounce".equalsIgnoreCase(type) ? " , hard_bounce = hard_bounce + 1 " : " , clean = clean + 1 ";
        Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET progress = '" + progress + "%' " + update + " WHERE Id = ?", new Object[] { Integer.valueOf(proccessId) }, 0);
        updateIndex();
    }

    public static synchronized int getIndex() {
        return INDEX;
    }

    public static synchronized void updateIndex() {
        INDEX++;
    }

    public static synchronized void updateCount(int size) {
        COUNT += size;
    }

    public static synchronized int getCount() {
        return COUNT;
    }

    public void interruptProccess(int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'error' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), Integer.valueOf(proccessId) }, 0);
        } catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }

    public void finishProccess(int proccessId) {
        try {
            Database.get("master").executeUpdate("UPDATE admin.bounce_clean_proccesses SET status = 'completed' , progress = '100%' , finish_time = ?  WHERE id = ?", new Object[] { new Timestamp(System.currentTimeMillis()), Integer.valueOf(proccessId) }, 0);
        } catch (Exception e) {
            Logger.error(e, SuppressionManager.class);
        }
    }
}
