/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ut4yut5;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

/**
 *
 * @author usuario
 */
// Clase para descargar y descifrar archivos y sus versiones históricas
public class DescargadorAvanzado {

    private String ftpHost;
    private String ftpUser;
    private String ftpPassword;
    private String remoteFolderPath;
    private String remoteHistoryPath;
    private Key secretKey;
    private ExecutorService executorService;
    private static final String ALGORITHM = "AES";

    public DescargadorAvanzado(String ftpHost, String ftpUser, String ftpPassword,
            String remoteFolderPath, String secretKeyStr) {
        this.ftpHost = ftpHost;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
        this.remoteFolderPath = remoteFolderPath;
        this.remoteHistoryPath = remoteFolderPath + "/historial";

        // Inicializar la clave de cifrado
        byte[] key = secretKeyStr.getBytes();
        this.secretKey = new SecretKeySpec(key, ALGORITHM);

        // Crear un pool de hilos
        this.executorService = Executors.newFixedThreadPool(5);
    }

    /**
     * Descarga y descifra un archivo del servidor FTP.
     */
    public Future<Boolean> descargarYDescifrar(String fileName, String localDestFolder) {
    return executorService.submit(() -> {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpHost);
            ftpClient.login(ftpUser, ftpPassword);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Cambiar al directorio remoto
            ftpClient.changeWorkingDirectory(remoteFolderPath);

            // Descargar archivo a un ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = ftpClient.retrieveFile(fileName, outputStream);

            if (success) {
                byte[] fileContent = outputStream.toByteArray();

                // Verificar que el contenido no esté vacío
                if (fileContent.length == 0) {
                    System.out.println("El archivo descargado está vacío: " + fileName);
                    return false;
                }

                byte[] decryptedContent;
                try {
                    // Intentar descifrar el contenido
                    decryptedContent = descifrar(fileContent);
                    System.out.println("Archivo descifrado exitosamente: " + fileName);
                } catch (Exception e) {
                    System.out.println("Error al descifrar, usando contenido original: " + fileName);
                    decryptedContent = fileContent;
                }

                // Asegurarse de que la carpeta 'descargas' exista
                Path descargasPath = Paths.get(localDestFolder);
                if (!Files.exists(descargasPath)) {
                    Files.createDirectories(descargasPath);
                }

                // Guardar el contenido en el archivo local dentro de 'descargas'
                String localFilePath = descargasPath.resolve(fileName).toString();
                try (FileOutputStream fos = new FileOutputStream(localFilePath)) {
                    fos.write(decryptedContent);
                }

                System.out.println("Archivo descargado y descifrado exitosamente a: " + localFilePath);
                return true;
            } else {
                System.out.println("Error al descargar el archivo: " + fileName);
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error en descarga: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    });
}



   public void descargarTodosLosTxt() {
    String localDestFolder = "C:\\Users\\usuario\\OneDrive\\Escritorio\\Prueba\\descargas";
    FTPClient ftpClient = new FTPClient();
    try {
        ftpClient.connect(ftpHost);
        ftpClient.login(ftpUser, ftpPassword);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

        // Cambiar al directorio remoto
        ftpClient.changeWorkingDirectory(remoteFolderPath);

        // Listar archivos en el directorio
        FTPFile[] files = ftpClient.listFiles();

        for (FTPFile file : files) {
            if (file.getName().endsWith(".txt")) {
                descargarYDescifrar(file.getName(), localDestFolder).get();
            }
        }

        ftpClient.logout();
        ftpClient.disconnect();
    } catch (Exception e) {
        e.printStackTrace();
    }
}



    /**
     * Descifra el contenido del archivo utilizando el algoritmo AES.
     */
    private byte[] descifrar(byte[] contenidoCifrado) throws Exception {
    try {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(contenidoCifrado);
    } catch (Exception e) {
        // Manejo de excepciones específicas para descifrado
        System.err.println("Error al descifrar el contenido: " + e.getMessage());
        throw new Exception("Descifrado fallido", e);
    }
}


    /**
     * Apaga el pool de hilos.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
