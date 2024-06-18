package com.github.martinfrank.tcpclientserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorCloser {

    private ExecutorCloser(){
        //NOSONAR
    }

    private static final int TIMEOUT_VALUE = 3;
    private static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    static void close(ExecutorService executor ) {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(TIMEOUT_VALUE, TIMEOUT_UNIT)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(TIMEOUT_VALUE, TIMEOUT_UNIT)){
                    System.err.println("Pool did not terminate");//to prevent further dependencies we write System.err
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
