package com.mycompany.ut4yut5;

import java.io.IOException;
import java.nio.file.*;
import java.security.Key;
import java.util.concurrent.ExecutorService;

public class MonitorCarpeta {
    private String localFolderPath;
    private ExecutorService executorService;
    private String ftpHost, ftpUser, ftpPassword, remoteFolderPath;
    private Key secretKey;

    public MonitorCarpeta(String localFolderPath, ExecutorService executorService, 
                           String ftpHost, String ftpUser, String ftpPassword, 
                           String remoteFolderPath, Key secretKey) {
        this.localFolderPath = localFolderPath;
        this.executorService = executorService;
        this.ftpHost = ftpHost;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
        this.remoteFolderPath = remoteFolderPath;
        this.secretKey = secretKey;
    }

    public void iniciarMonitoreo() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path directorio = Paths.get(localFolderPath);
            directorio.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            System.out.println("Monitoreo iniciado en: " + localFolderPath);
            new Thread(() -> monitorearCambios(watchService, directorio)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void monitorearCambios(WatchService watchService, Path directorio) {
    try {
        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                Path fileName = ((WatchEvent<Path>) event).context();
                Path fullPath = directorio.resolve(fileName);
                System.out.println(event.kind().name() + ": " + fileName);

                boolean isModification = event.kind() == StandardWatchEventKinds.ENTRY_MODIFY;

                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE || isModification) {
                    executorService.submit(new TareaDeDescarga(fullPath, fileName.toString(), isModification,
                                                               ftpHost, ftpUser, ftpPassword, remoteFolderPath, secretKey));
                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                    executorService.submit(new TareaDeEliminacion(fullPath, fileName.toString(),
                                                                  ftpHost, ftpUser, ftpPassword, remoteFolderPath));
                }
            }
            if (!key.reset()) break;
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    }}
