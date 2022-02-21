package com.me.ladster;

import java.io.File;
import java.io.IOException;

public class ProcessingStatus {

    // Define control file locations
    private static final String RUNNING_FILE = System.getProperty("user.dir") + "/" + "currently-processing.txt";
    private static final String PAUSE_FILE = System.getProperty("user.dir") + "/" + "pause.txt";
    private static final String STOP_FILE = System.getProperty("user.dir") + "/" + "stop.txt";

    static boolean shouldExit() {
        File stopFile = new File(STOP_FILE);

        if (stopFile.exists()) {
            return true;
        }

        return false;
    }

    static boolean isPaused() {
        File pauseFile = new File(PAUSE_FILE);

        if (pauseFile.exists()) {
            return true;
        }

        return false;
    }

    static boolean isProcessing() {
        File runningFile = new File(RUNNING_FILE);

        if (runningFile.exists()) {
            return true;
        }

        return false;
    }

    static void setProcessingStatus(boolean isProcessing) {
        File runningFile = new File(RUNNING_FILE);

        try {
            if (isProcessing) {
                runningFile.createNewFile();
            } else {
                runningFile.delete();
            }
        } catch (IOException e) {
            Notifications.sendErrorNotification(e.getMessage(), e.toString());
            System.out.println(e.getMessage());
        }
    }

}
