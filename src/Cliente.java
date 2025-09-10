import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("=== Registro de usuario ===");
            salida.println("REGISTRO");

            String respuesta = entrada.readLine(); 
            if ("SOLICITAR_DATOS_REGISTRO".equals(respuesta)) {
                System.out.print("Nombre de usuario: ");
                String usuario = scanner.nextLine();
                System.out.print("Contraseña: ");
                String password = scanner.nextLine();

                salida.println(usuario);
                salida.println(password);

                String resultado = entrada.readLine(); 
                String mensaje = entrada.readLine();

                System.out.println("Resultado: " + resultado);
                System.out.println("Mensaje del servidor: " + mensaje);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
