package com.mycompany.ut4yut5;


import com.mycompany.ut4yut5.GestorSincronizacion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class SincronizacionAvanzada {
    public static void main(String[] args) throws InterruptedException, IOException {
        String localFolder = "C:\\Users\\usuario\\OneDrive\\Escritorio\\Prueba";
        String ftpHost = "127.0.0.1";
        String ftpUser = "castellon";
        String ftpPassword = "castellon";
        String remoteFolder = "compartia";
        String secretKey = "castellon1234567";

        GestorSincronizacion sync = new GestorSincronizacion(localFolder, ftpHost, ftpUser, ftpPassword, remoteFolder, secretKey);

        // Escuchar comandos del usuario
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Escribe 'descargar_txt' para descargar todos los archivos .txt:");
        String command = reader.readLine();

        if ("descargar_txt".equalsIgnoreCase(command)) {
            DescargadorAvanzado descargador = new DescargadorAvanzado(ftpHost, ftpUser, ftpPassword, remoteFolder, secretKey);
            descargador.descargarTodosLosTxt();
        }

        // Mantener el programa en ejecución
        Thread.sleep(Long.MAX_VALUE);
    }
}
