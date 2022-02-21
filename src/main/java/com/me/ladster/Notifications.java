package com.me.ladster;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;

public class Notifications {

    // Message ID will be used to keep track of our messages on discord
    // This ensures we edit the correct message when updating values
    private static final AtomicLong messageID = new AtomicLong();

    static void sendOptimisingMessage(String fileName) {
        String notification = "display notification \"Processing: " + fileName + "\" with title \"Auto Optimiser\" sound name \"Ping\"";
        sendNotification(notification);
    }

    // This allows applescript to be run and a notification to be pushed to the Mac
    private static void sendNotification(String notification) {
        Runtime runtime = Runtime.getRuntime();
        String[] code = new String[]{"osascript", "-e", notification};
        try {
            runtime.exec(code);
        } catch (IOException e) {
            sendErrorNotification(e.getMessage(), e.toString());
            System.out.println("Auto Optimiser: " + e.getMessage());
        }
    }

    static void sendSuccessMessage(String fileName, String time) {
        String notification = "display notification \"Finished processing: " + fileName + "\" with title \"Auto Optimiser\" subtitle \"Duration: " + time + "\" sound name \"Ping\"";
        sendNotification(notification);

    }

    static void sendErrorNotification(String errorMessage, String exception) {
        String notification = "display notification \"" + errorMessage + "\" with title \"Auto Optimiser\" subtitle \"Error while attempting to process media!\" sound name \"Basso\"";
        sendNotification(notification);

        WebhookEmbed.EmbedField initialField = new WebhookEmbed.EmbedField(true, "Error while optimising: ", "```" + exception + "```");

        WebhookEmbed initialEmbed = new WebhookEmbedBuilder()
                .setColor(0xFF0000)
                .addField(initialField)
                .build();

        Main.errorClient.send(initialEmbed).thenAccept(System.out::println);
    }

    static void sendDiscordNotification(Process process, String fileName) {

        // If this is the first time its running and message ID hasn't been set yet
        // (atomic long is initialised with value of 0)
        if (messageID.get() == 0) {

            WebhookEmbed.EmbedField initialField = new WebhookEmbed.EmbedField(true, fileName, "Starting...");

            WebhookEmbed initialEmbed = new WebhookEmbedBuilder()
                    .setColor(0x006BFF)
                    .addField(initialField)
                    .build();

            Main.mainClient.send(initialEmbed).thenAccept((message) -> messageID.set(message.getId()));
        }

        try {
            long timeDelay = System.currentTimeMillis();

            // Read the stream from the handbrake process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                // If the time delay hasn't been reached yet, continue looping until it has
                if (timeDelay >= System.currentTimeMillis()) {
                    continue;
                }

                // Ensure discords api rate limits doesn't get hit
                // A channel has 30 msg/60 sec limit for webhooks
                int discordLimitDelay = 5000;
                timeDelay = System.currentTimeMillis() + discordLimitDelay;

                // Update the discord message with the ETA provided by handbrake
                updateDiscordMessage(fileName, line);
            }
        } catch (IOException e) {
            sendErrorNotification(e.getMessage(), e.toString());
            System.out.println(e.getMessage());
        }
    }

    public static void updateDiscordMessage(String fileName, String value) {

        // Create the main field for the webhook embed with the file name and the value passed to this method
        WebhookEmbed.EmbedField mainField = new WebhookEmbed.EmbedField(true, fileName, value);

        // Set the embed builder
        WebhookEmbedBuilder embedBuilder = new WebhookEmbedBuilder()
                .setColor(0x006BFF)
                .addField(mainField);

        // Provided there is unoptimised media in the queue, loop over the list of text-chunks to create a queue list
        if (Main.unoptimisedMedia.size() >= 1) {
            String name = "Queue:";

            // Queue is split into chunks due to the character limit for discord embed fields
            for (String queueEmbed : Main.getQueueEmbeds()) {
                WebhookEmbed.EmbedField queueField = new WebhookEmbed.EmbedField(false, name, queueEmbed);
                embedBuilder.addField(queueField);
                name = "More Queue:";
            }
        }

        // Provided there is elements in the completed list, loop over the list of text-chunks to create the completed list
        if (Main.completedList.size() >= 1) {
            String name = "Completed:";

            // Again, this is split into chunks due to character limit for discord embed fields
            for (String completedEmbed : Main.getCompletedEmbeds()) {
                WebhookEmbed.EmbedField completedField = new WebhookEmbed.EmbedField(false, name, completedEmbed);
                embedBuilder.addField(completedField);
                name = "More Completed:";
            }
        }

        // Edit the message with the id we stored earlier, build the embed builder to create the finished message
        Main.mainClient.edit(messageID.get(), embedBuilder.build());
    }
}
