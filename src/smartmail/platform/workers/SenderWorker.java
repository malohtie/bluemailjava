package smartmail.platform.workers;

import smartmail.platform.logging.Logger;
import smartmail.platform.parsers.TypesParser;
import smartmail.platform.remote.SSH;
import smartmail.platform.utils.Strings;

import java.io.File;

public class SenderWorker extends Thread {
    public int dropId;

    public SSH ssh;

    public File pickupFile;

    public SenderWorker(int dropId, SSH ssh, File pickupFile) {
        this.dropId = dropId;
        this.ssh = ssh;
        this.pickupFile = pickupFile;
    }

    public void run() {
        try {
            if (this.ssh != null && this.pickupFile != null && this.pickupFile.exists()) {
                int progress = TypesParser.safeParseInt(String.valueOf(this.pickupFile.getName().split("\\_")[2]));
                String file = "/var/spool/bluemail/tmp/pickup_" + Strings.getSaltString(20, true, true, true, false) + ".txt";
                this.ssh.uploadFile(this.pickupFile.getAbsolutePath(), file);
                this.ssh.cmd("mv " + file + " /var/spool/bluemail/pickup/");
                if (this.dropId > 0)
                    ServerWorker.updateDrop(this.dropId, progress);
            }
        } catch (Exception e) {
            Logger.error(e, SenderWorker.class);
        }
    }
}
