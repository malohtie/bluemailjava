package smartmail.platform.exceptions;

import smartmail.platform.logging.Logger;

public class ThreadException implements Thread.UncaughtExceptionHandler {
    public void uncaughtException(Thread t, Throwable e) {
        Logger.error(e, t.getClass());
    }
}
