package smartmail.platform.bootstrap;

import org.apache.commons.codec.binary.Base64;
import smartmail.platform.controllers.BounceCleaner;
import smartmail.platform.controllers.DropsSender;
import smartmail.platform.controllers.StatsCalculator;
import smartmail.platform.controllers.SuppressionManager;
import smartmail.platform.logging.Logger;
import smartmail.platform.orm.Database;

import java.io.File;

public class Start {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        try {
            System.setProperty("base.path", (new File(Start.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getParentFile().getParentFile().getParentFile().getParentFile().getAbsolutePath().replaceAll("%20", " "));
            Logger.initlog4Java();
            Database.init();
            if (args.length == 0)
                throw new Exception("No Parameters Passed !");
            String s = new String(Base64.encodeBase64(args[0].getBytes()));
            switch (s) {
                case "c2VuZF9wcm9jY2Vzcw==":
                    new DropsSender(args);
                    break;
                case "c2VuZF9zdGF0cw==":
                    new StatsCalculator(args);
                    break;
                case "Ym91bmNlX2NsZWFu":
                    new BounceCleaner(args);
                    break;
                case "c3VwcHJlc3Npb25fcHJvY2Nlc3M=":
                    new SuppressionManager(args);
                    break;
                default:
                    throw new Exception("Unsupported Action !");
            }
        } catch (Exception e) {
            Logger.error(e, Start.class);
            System.out.println("Error :" + e.getMessage());
        } finally {
            long end = System.currentTimeMillis();
            System.out.println("Job Completed in : " + (end - startTime) + " miliseconds");
            System.exit(0);
        }
    }
}
