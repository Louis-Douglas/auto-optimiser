package com.me.ladster;

import club.minnced.discord.webhook.WebhookClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    // Start of Configuration
    static final String INPUT_MEDIA_FOLDER_LOCATION = "/Users/louisdouglas/Movies/Optimising/Media";
    static final String OUTPUT_MEDIA_FOLDER_LOCATION = "/Users/louisdouglas/Movies/Optimising/Media-Optimised";
    static final String OPTIMISING_FOLDER_LOCATION = "/Users/louisdouglas/Movies/Optimising/Processing";

    static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/<api-key-here>";
    static final String DISCORD_ERROR_BOT_WEBHOOK_URL = "https://discord.com/api/<api-key-here>";
    // End of Configuration

    // Discord webhook clients
    static WebhookClient mainClient;
    static WebhookClient errorClient;

    static List<File> unoptimisedMedia;
    static List<String> completedList = new ArrayList<>();

    // Amount of media files that were optimised in this session
    private static int count = 0;


    public static void main(String[] args) {

        // Wrap the entire program in a try/catch and output any error messages to the error discord channel for diagnosis
        try {

            // Track start time for this optimising session
            long startTime = System.currentTimeMillis();

            // Check if the program is already running elsewhere by checking if the running file exists
            if (ProcessingStatus.isProcessing()) {
                System.out.println("Currently processing elsewhere!");
                return;
            }

            // If user has left stop.txt in the main directory, stop the program here
            if (ProcessingStatus.shouldExit()) {
                System.out.println("Not running due to stop.txt file being detected!");
                return;
            }

            // Initialise discord webhook clients
            mainClient = WebhookClient.withUrl(DISCORD_WEBHOOK_URL);
            errorClient = WebhookClient.withUrl(DISCORD_ERROR_BOT_WEBHOOK_URL);

            // Do the optimising
            optimiseMedia();

            // If count is 0, then nothing was optimised this session
            if (count <= 0) {
                System.out.println("Nothing to optimise");
            } else {
                System.out.println("Optimised " + count + " videos!");

                // Track end time for this optimising session
                long endTime = System.currentTimeMillis();

                // Ensure the program wasn't forcefully exited before displaying a finished message
                if (!ProcessingStatus.shouldExit()) {
                    Notifications.updateDiscordMessage("Finished!", "Total Time: " + getDuration(startTime, endTime));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to optimise: " + e);
            Notifications.sendErrorNotification(e.getMessage(), e.toString());
        } finally {

            // Make sure the running file is deleted and the processing status set to false through this method
            ProcessingStatus.setProcessingStatus(false);

            // Close the webhook clients
            if (mainClient != null && !mainClient.isShutdown())
                mainClient.close();
            if (errorClient != null && !errorClient.isShutdown())
                errorClient.close();
        }
    }

    private static void optimiseMedia() {

        // Check if program should exit at start as this method is called recursively
        if (ProcessingStatus.shouldExit()) {
            Notifications.updateDiscordMessage("Stopped!", "Exited due to user input!");
            return;
        }

        if (ProcessingStatus.isPaused()) {
            Notifications.updateDiscordMessage("Paused...", "Currently paused due to user input!");

            // Check every second if the paused file is still there
            while (ProcessingStatus.isPaused()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Failed to optimise: " + e);
                    Notifications.sendErrorNotification(e.getMessage(), e.toString());
                }
            }
        }

        // Check for new files everytime this method is called
        unoptimisedMedia = Folders.getUnoptimisedMedia();

        // If the unoptimised media list has a size over or equal to 1
        if (unoptimisedMedia.size() >= 1) {
            File fileToProcess = unoptimisedMedia.get(0);
            long startTime = System.currentTimeMillis();

            // Throw the file over to the process method which implements the handbrake CLI to do the actual optimisation
            // If this method returns true there were no errors
            if (Optimiser.process(fileToProcess)) {
                long endTime = System.currentTimeMillis();

                // Send success message as notification to my Mac with file name and duration
                Notifications.sendSuccessMessage(fileToProcess.getName(), getDuration(startTime, endTime));

                // Add the files name and duration to a completed list of strings, this will be used for the discord message
                completedList.add(fileToProcess.getName() + " - " + getDuration(startTime, endTime));
                count++;

            } else {
                // TODO: Move this error message to the catch block in the process method and remove this whole if statement
                System.out.println("Failed to optimise: " + fileToProcess.getName());
            }

            // Call this method recursively so any new media is automatically added to the queue
            optimiseMedia();
        }
    }

    // Get a list of the file names appropriately formatted from the media queue
    public static List<String> getQueueEmbeds() {
        List<String> queueEmbeds = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (File file : unoptimisedMedia) {

            // If over max embed character limit, add string builder content to queueEmbeds list, and start new string builder
            if ((sb + file.getName() + "\n").length() > 1024) {
                queueEmbeds.add(sb.toString().trim());
                sb = new StringBuilder();
            }
            sb.append(file.getName()).append("\n");
        }

        // Add any leftover string builder content to the list
        if (sb.length() > 0) {
            queueEmbeds.add(sb.toString().trim());
        }

        return queueEmbeds;
    }

    // Get a list of the file names appropriately formatted from the completed list
    public static List<String> getCompletedEmbeds() {
        List<String> completedEmbeds = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (String completed : completedList) {

            // If over max embed character limit, add string builder content to completedEmbeds list, and start new string builder
            if ((sb + completed + "\n").length() > 1024) {
                completedEmbeds.add(sb.toString().trim());
                sb = new StringBuilder();
            }
            sb.append(completed).append("\n");
        }

        // Add any leftover string builder content to the list
        if (sb.length() > 0) {
            completedEmbeds.add(sb.toString().trim());
        }

        return completedEmbeds;
    }

    // Get the duration between two times and return an appropriately formatted string
    // TODO: Could be improved to remove the hours if they're 0
    public static String getDuration(long startTime, long endTime) {
        long millis = endTime - startTime;
        return String.format("%d Hours(s) & %d Minutes(s)",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))
        );
    }
}
