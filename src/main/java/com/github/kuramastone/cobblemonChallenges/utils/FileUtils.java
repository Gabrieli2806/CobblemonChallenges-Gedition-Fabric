package com.github.kuramastone.cobblemonChallenges.utils;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FileUtils {


    public static void copyDirectoryFromJar(String resourcePath, File destinationFolder, String[] filesToCopy) {
        if (destinationFolder.exists()) {
            throw new RuntimeException("Directory already exists.");
        }

        // Create the directory
        if (!destinationFolder.mkdirs()) {
            throw new RuntimeException("Could not make directory to save config.");
        }

        for (String fileName : filesToCopy) {
            String fullPath = resourcePath + "/" + fileName;
            try (InputStream in = CobbleChallengeMod.class.getResourceAsStream(fullPath)) {

                if (in == null) {
                    System.err.println("File not found in JAR: " + fullPath);
                    continue;
                }

                // Ensure the output path is valid
                File outputFile = new File(destinationFolder, fileName);
                try (OutputStream out = Files.newOutputStream(outputFile.toPath())) {
                    // Manually copy the InputStream to the OutputStream
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    System.out.println("Copied " + fileName + " to " + destinationFolder.getAbsolutePath());
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
