package com.gestion.services;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class VoiceMessageService {

    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;
    private Thread playbackThread;
    private boolean isPlaying = false;

    /**
     * Enregistre un message vocal et le sauvegarde dans le dossier uploads
     */
    public static String recordVoiceMessage(int durationSeconds) throws LineUnavailableException {
        try {
            AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Format audio non supporté");
                return null;
            }

            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            byte[] buffer = new byte[4096];
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Enregistrer pendant la durée spécifiée
            long recordingTime = durationSeconds * 1000; // en millisecondes
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < recordingTime) {
                int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
            }

            targetDataLine.stop();
            targetDataLine.close();

            // Sauvegarder le fichier
            AudioInputStream audioInputStream = new AudioInputStream(
                    new java.io.ByteArrayInputStream(byteArrayOutputStream.toByteArray()),
                    audioFormat,
                    byteArrayOutputStream.toByteArray().length / audioFormat.getFrameSize());

            String fileName = "voice_" + UUID.randomUUID() + ".wav";
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(fileName);

            AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
            AudioSystem.write(audioInputStream, fileType, filePath.toFile());
            audioInputStream.close();

            return filePath.toString();
        } catch (Exception e) {
            System.err.println("Erreur enregistrement vocal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Joue un message vocal
     */
    public void playVoiceMessage(String filePath) {
        if (isPlaying) {
            stopPlayback();
        }

        playbackThread = new Thread(() -> {
            try {
                File audioFile = new File(filePath);
                if (!audioFile.exists()) {
                    System.err.println("Fichier audio non trouvé: " + filePath);
                    return;
                }

                audioInputStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat audioFormat = audioInputStream.getFormat();

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

                isPlaying = true;
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = audioInputStream.read(buffer)) != -1 && isPlaying) {
                    sourceDataLine.write(buffer, 0, bytesRead);
                }

                sourceDataLine.drain();
                sourceDataLine.close();
                audioInputStream.close();
                isPlaying = false;
            } catch (Exception e) {
                System.err.println("Erreur lecture audio: " + e.getMessage());
                e.printStackTrace();
                isPlaying = false;
            }
        });

        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * Arrête la lecture
     */
    public void stopPlayback() {
        isPlaying = false;
        if (sourceDataLine != null) {
            sourceDataLine.close();
        }
    }

    /**
     * Vérifie si un son est en cours de lecture
     */
    public boolean isPlaying() {
        return isPlaying;
    }
}

class ByteArrayOutputStream extends java.io.ByteArrayOutputStream {
    // Utilisé pour stocker les données audio enregistrées
}
