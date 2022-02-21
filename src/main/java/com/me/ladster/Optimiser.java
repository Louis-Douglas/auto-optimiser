package com.me.ladster;

import java.io.File;
import java.io.IOException;

public class Optimiser {

    static boolean process(File file) {
        String inputPath = file.getPath();
        String outputPath = Folders.getOutputPath(file);
        String optimisingPath = Folders.getOptimisingPath(file);
        Runtime runtime = Runtime.getRuntime();

        // Set up the console commands which will run Handbrake CLI and move the file to the appropriate directory
        String[] handbrakeCommand = new String[]{"/Applications/HandBrakeCLI", "HandBrakeCLI", "-i", inputPath, "-o", optimisingPath, "--all-audio", "--all-subtitles", "--two-pass"}; // , "-e", "x264", "-q", "20", "-B", "160"
        String[] moveFileCommand = new String[]{"mv", optimisingPath, outputPath};

        try {
            // Start a new process using the handbrake command, this is akin to typing the command into the terminal
            // although this method returns all output to the process object
            Process handbrakeProcess = runtime.exec(handbrakeCommand);

            Notifications.sendOptimisingMessage(file.getName());

            // This ensures another instance of the program can't run in parallel
            ProcessingStatus.setProcessingStatus(true);

            // At this point the file is being processed, so remove from unoptimised media list
            Main.unoptimisedMedia.remove(file);

            // Send a continuously updating discord message to the webhook
            Notifications.sendDiscordNotification(handbrakeProcess, file.getName());

            // TODO: add check for hanging process here
            handbrakeProcess.waitFor();

            // Create the directories required for the output files path
            new File(outputPath).mkdirs();

            // Moves the optimising file from the optimising directory to its destination directory
            Process moveFileProcess = runtime.exec(moveFileCommand);
            moveFileProcess.waitFor();

        } catch (IOException | InterruptedException e) {
            Notifications.sendErrorNotification(e.getMessage(), e.toString());
            System.out.println("Auto Optimiser: " + e.getMessage());
            return false;
        }

        return true;
    }
}
