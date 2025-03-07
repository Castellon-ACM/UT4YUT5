package com.mycompany.ut4yut5;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.nio.file.*;
import java.security.Key;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;

public class TareaDeDescarga implements Runnable {
    private Path localPath;
    private String fileName;
    private boolean isModification;
    private String ftpHost, ftpUser, ftpPassword, remoteFolderPath;
    private Key secretKey;
    private static final Map<String, Integer> fileVersions = new ConcurrentHashMap<>();

    public TareaDeDescarga(Path localPath, String fileName, boolean isModification,
                           String ftpHost, String ftpUser, String ftpPassword,
                           String remoteFolderPath, Key secretKey) {
        this.localPath = localPath;
        this.fileName = fileName;
        this.isModification = isModification;
        this.ftpHost = ftpHost;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
        this.remoteFolderPath = remoteFolderPath;
        this.secretKey = secretKey;
    }

    @Override
    public void run() {
        if (!Files.exists(localPath)) {
            System.out.println("El archivo ya no existe: " + localPath);
            return;
        }

        // Verificar si la carpeta local se llama "descargas"
        if (localPath.getFileName().toString().equalsIgnoreCase("descargas")) {
            System.out.println("La carpeta local es 'descargas'. No se realizará ninguna operación en FTP.");
            return;
        }

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            if (Files.isDirectory(localPath)) {
                // Si es un directorio, crearlo en el servidor FTP
                crearDirectorioEnFTP(ftpClient, localPath, remoteFolderPath);
            } else {
                // Si es un archivo, mover la versión anterior al historial y luego subirlo
                if (isModification) {
                    moverAlHistorial(ftpClient, fileName);
                }

                byte[] fileContent = Files.readAllBytes(localPath);
                byte[] encryptedContent = cifrar(fileContent);
                InputStream inputStream = new ByteArrayInputStream(encryptedContent);

                ftpClient.changeWorkingDirectory(remoteFolderPath);
                boolean success = ftpClient.storeFile(fileName, inputStream);
                inputStream.close();

                if (success) {
                    System.out.println("Archivo subido exitosamente: " + fileName);
                } else {
                    System.out.println("Error al subir el archivo: " + fileName);
                }
            }

            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crearDirectorioEnFTP(FTPClient ftpClient, Path localDir, String remoteDir) throws IOException, Exception {
        String dirName = localDir.getFileName().toString();
        if (!ftpClient.changeWorkingDirectory(remoteDir + "/" + dirName)) {
            ftpClient.makeDirectory(remoteDir + "/" + dirName);
            System.out.println("Directorio creado en FTP: " + dirName);
        }

        // Recursivamente subir archivos y subdirectorios
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(localDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    crearDirectorioEnFTP(ftpClient, entry, remoteDir + "/" + dirName);
                } else {
                    // Subir archivos dentro del directorio
                    byte[] fileContent = Files.readAllBytes(entry);
                    byte[] encryptedContent = cifrar(fileContent);
                    InputStream inputStream = new ByteArrayInputStream(encryptedContent);
                    ftpClient.storeFile(remoteDir + "/" + dirName + "/" + entry.getFileName(), inputStream);
                    inputStream.close();
                }
            }
        }
    }

    private void moverAlHistorial(FTPClient ftpClient, String fileName) throws IOException {
        String remoteHistoryPath = "C:\\Users\\usuario\\OneDrive\\Documentos\\NetBeansProjects\\UT4YUT5\\compartida\\compartia\\historial";

        ftpClient.changeWorkingDirectory(remoteFolderPath);
        FTPFile[] files = ftpClient.listFiles(fileName);

        if (files.length == 0) {
            System.out.println("No se encontró el archivo para mover al historial: " + fileName);
            return;
        }

        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        String baseFileName = fileName;

        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
            baseFileName = fileName.substring(0, dotIndex);
        }

        int nextVersion = fileVersions.compute(baseFileName, (k, v) -> (v == null) ? 1 : v + 1);
        String historyFileName = baseFileName + "_v" + nextVersion + extension;

        // Mover el archivo actual al historial
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean downloaded = ftpClient.retrieveFile(fileName, baos);

        if (!downloaded) {
            System.out.println("Error al descargar el archivo para mover al historial: " + fileName);
            return;
        }

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ftpClient.changeWorkingDirectory(remoteHistoryPath);
        System.out.println("Cambiado al directorio de historial.");

        boolean uploaded = ftpClient.storeFile(historyFileName, is);
        is.close();

        if (uploaded) {
            // Eliminar el archivo de la carpeta principal después de moverlo al historial
            ftpClient.changeWorkingDirectory(remoteFolderPath);
            ftpClient.deleteFile(fileName);
            System.out.println("Archivo movido al historial: " + historyFileName);
        } else {
            System.out.println("Error al subir el archivo al historial: " + historyFileName);
        }
    }

    private byte[] cifrar(byte[] content) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(content);
    }
}
