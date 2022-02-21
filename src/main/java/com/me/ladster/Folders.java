package com.me.ladster;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Folders {

    public static List<File> getUnoptimisedMedia() {
        Set<File> inputMedia = getAllFiles(Main.INPUT_MEDIA_FOLDER_LOCATION);
        Set<File> outputMedia = getAllFiles(Main.OUTPUT_MEDIA_FOLDER_LOCATION);

        List<File> unoptimisedMedia = new ArrayList<>();

        // Loop over all media in the input folder
        for (File inputFile : inputMedia) {
            boolean outputContainsInput = false;

            // Loop over all media in the output folder
            for (File outputFile : outputMedia) {
                String inputFilePath = getPathForComparison(inputFile);
                String outputFilePath = getPathForComparison(outputFile);

                // If both input & output directories contain the same file, set outputContainsInput to true and break out of the loop
                if (inputFilePath.equals(outputFilePath)) {
                    outputContainsInput = true;
                    break;
                }
            }

            // If the file does not exist in the output directory, then add to unoptimised media list
            if (!outputContainsInput) {
                unoptimisedMedia.add(inputFile);
            }
        }

        return unoptimisedMedia;
    }

    // Sort all the files to ensure there is only one instance of each video file in a hashset
    private static Set<File> getAllFiles(String folderLocation) {
        File mainFolder = new File(folderLocation);
        Set<File> media = new HashSet<>();

        List<File> files = getFilesInFolder(mainFolder);

        for (File file : files) {
            String fileName = file.getName().toLowerCase();

            // Ensure the file is actually a video file
            if (fileName.endsWith(".mp4")
                    || fileName.endsWith(".wmv")
                    || fileName.endsWith(".avi")
                    || fileName.endsWith(".avchd")
                    || fileName.endsWith(".mov")
                    || fileName.endsWith(".mkv")
                    || fileName.endsWith(".webm")
                    || fileName.endsWith(".mpeg-2")) {
                media.add(file);
            }
        }
        return media;

    }

    private static List<File> getFilesInFolder(File folder) {
        List<File> files = new ArrayList<>();

        if (folder.isDirectory()) {
            // Get all files within the current directory
            File[] dirFiles = folder.listFiles();
            if (dirFiles == null) {
                return files;
            }

            // Loop over the files and call this method recursively to navigate throughout all the directories
            for (File file : dirFiles) {
                files.addAll(getFilesInFolder(file));
            }
        } else {
            // "folder" in this instance is actually a file
            files.add(folder);
        }

        return files;
    }


    static String getOutputPath(File file) {
        String filePath = file.getParent();
        return filePath.replace("/Media/", "/Media-Optimised/");
    }

    static String getOptimisingPath(File file) {
        String optimisingPath = Main.OPTIMISING_FOLDER_LOCATION;

        String fileName = file.getName();

        // Replace the file extension with mkv, so we convert to that format
        fileName = fileName.replace(".wmv", ".mkv")
                .replace(".avi", ".mkv")
                .replace(".avchd", ".mkv")
                .replace(".mov", ".mkv")
                .replace(".mp4", ".mkv")
                .replace(".webm", ".mkv")
                .replace(".mpeg-2", ".mkv");

        return optimisingPath + "/" + fileName;
    }

    private static String getPathForComparison(File file) {
        String fileName = file.getName();

        // Remove all file extensions for path comparison
        fileName = fileName
                .replace(".mp4", "")
                .replace(".wmv", "")
                .replace(".avi", "")
                .replace(".avchd", "")
                .replace(".mov", "")
                .replace(".mkv", "")
                .replace(".webm", "")
                .replace(".mpeg-2", "");

        // Replace the parent optimised directory with the original directory
        String parentPath = file.getParent().replace("/Media-Optimised/", "/Media/");

        return parentPath + "/" + fileName;
    }

}
