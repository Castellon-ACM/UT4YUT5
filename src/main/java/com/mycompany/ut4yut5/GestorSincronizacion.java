package com.mycompany.ut4yut5;

import java.security.Key;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GestorSincronizacion {
    private ExecutorService executorService;
    private MonitorCarpeta monitor;
    private GestorFTP gestorFTP;

    public GestorSincronizacion(String localFolder, String ftpHost, String ftpUser,
                                String ftpPassword, String remoteFolder, String secretKey) {
        this.executorService = Executors.newFixedThreadPool(5);

        Key key = generarClave(secretKey);

        this.monitor = new MonitorCarpeta(localFolder, executorService, ftpHost, ftpUser,
                                          ftpPassword, remoteFolder, key);

        this.gestorFTP = new GestorFTP(ftpHost, ftpUser, ftpPassword);
        String remoteHistoryPath = remoteFolder + "/historial";
        gestorFTP.crearDirectorioHistorialRemoto(remoteHistoryPath);

        monitor.iniciarMonitoreo();
    }

    public void shutdown() {
        executorService.shutdown();
        System.out.println("Sincronización finalizada.");
    }

    private Key generarClave(String secretKey) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey key = keyGen.generateKey();
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Error generando clave", e);
        }
    }
}
