/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ut4yut5;

import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 *
 * @author usuario
 */

class GestorFTP {
    private String ftpHost, ftpUser, ftpPassword;

    public GestorFTP(String ftpHost, String ftpUser, String ftpPassword) {
        this.ftpHost = ftpHost;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
    }

    public void crearDirectorioHistorialRemoto(String remoteHistoryPath) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.enterLocalPassiveMode();

            if (!ftpClient.changeWorkingDirectory(remoteHistoryPath)) {
                ftpClient.makeDirectory(remoteHistoryPath);
                System.out.println("Directorio de historial creado: " + remoteHistoryPath);
            }
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cargarVersionesExistentes(String remoteHistoryPath, ConcurrentHashMap<String, Integer> fileVersions) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.enterLocalPassiveMode();
            ftpClient.changeWorkingDirectory(remoteHistoryPath);
            FTPFile[] files = ftpClient.listFiles();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(.+)_v(\\d+)\\.[^.]*$");

            for (FTPFile file : files) {
                java.util.regex.Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    String baseFileName = matcher.group(1);
                    int version = Integer.parseInt(matcher.group(2));
                    fileVersions.compute(baseFileName, (k, v) -> (v == null || v < version) ? version : v);
                }
            }
            ftpClient.logout();
            ftpClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
