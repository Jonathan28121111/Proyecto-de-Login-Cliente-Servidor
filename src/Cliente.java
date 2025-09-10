import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345)) {
            System.out.println("Conectado al servidor en localhost:12345");
            
            Scanner scanner = new Scanner(System.in);
            System.out.println("=== MENÚ INICIAL ===");
            System.out.println("1. Salir");
            System.out.print("Selecciona una opción: ");
            
            String opcion = scanner.nextLine();
            if ("1".equals(opcion)) {
                System.out.println("Saliendo...");
            }
        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor: " + e.getMessage());
        }
    }
}
