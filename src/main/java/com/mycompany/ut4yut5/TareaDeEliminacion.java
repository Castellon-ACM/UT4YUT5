/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ut4yut5;

/**
 *
 * @author usuario
 */
import java.io.ByteArrayInputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;

public class TareaDeEliminacion implements Runnable {
    private Path localPath;
    private String fileName;
    private String ftpHost, ftpUser, ftpPassword, remoteFolderPath;

    public TareaDeEliminacion(Path localPath, String fileName, String ftpHost, String ftpUser,
                              String ftpPassword, String remoteFolderPath) {
        this.localPath = localPath;
        this.fileName = fileName;
        this.ftpHost = ftpHost;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
        this.remoteFolderPath = remoteFolderPath;
    }

    @Override
    public void run() {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.enterLocalPassiveMode();

            // Cambiar al directorio remoto
            ftpClient.changeWorkingDirectory(remoteFolderPath);

            // Verificar si es un archivo o directorio y mover al historial
            FTPFile[] files = ftpClient.listFiles(fileName);
            if (files.length > 0) {
                if (files[0].isDirectory()) {
                    moverDirectorioAlHistorial(ftpClient, fileName);
                } else {
                    moverArchivoAlHistorial(ftpClient, fileName);
                }
            }

            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moverArchivoAlHistorial(FTPClient ftpClient, String fileName) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean downloaded = ftpClient.retrieveFile(fileName, baos);

        if (!downloaded) return;

        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        ftpClient.changeWorkingDirectory(remoteFolderPath + "/historial");
        boolean uploaded = ftpClient.storeFile(fileName, is);
        is.close();

        if (uploaded) {
            ftpClient.changeWorkingDirectory(remoteFolderPath);
            ftpClient.deleteFile(fileName);
            System.out.println("Archivo movido al historial: " + fileName);
        }
    }

    private void moverDirectorioAlHistorial(FTPClient ftpClient, String dirName) throws Exception {
        ftpClient.changeWorkingDirectory(remoteFolderPath + "/" + dirName);
        FTPFile[] files = ftpClient.listFiles();

        for (FTPFile file : files) {
            if (file.isDirectory()) {
                moverDirectorioAlHistorial(ftpClient, dirName + "/" + file.getName());
            } else {
                moverArchivoAlHistorial(ftpClient, dirName + "/" + file.getName());
            }
        }

        ftpClient.changeWorkingDirectory(remoteFolderPath);
        ftpClient.removeDirectory(dirName);
        System.out.println("Directorio movido al historial: " + dirName);
    }
}
