import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("=== Sistema de autenticación ===");
            System.out.println("1. Registrarse");
            System.out.println("2. Iniciar sesión");
            System.out.print("Selecciona una opción: ");
            String opcion = scanner.nextLine();
            String usuarioActivo = null;

            if ("1".equals(opcion)) {
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
            } 
            else if ("2".equals(opcion)) {
                salida.println("LOGIN");
                String respuesta = entrada.readLine();
                if ("SOLICITAR_DATOS_LOGIN".equals(respuesta)) {
                    System.out.print("Nombre de usuario: ");
                    String usuario = scanner.nextLine();
                    System.out.print("Contraseña: ");
                    String password = scanner.nextLine();

                    salida.println(usuario);
                    salida.println(password);

                    String resultado = entrada.readLine();
                    if ("LOGIN_EXITOSO".equals(resultado)) {
                        System.out.println("Login exitoso: " + usuario);
                        usuarioActivo = usuario;

                        // Menú principal después de login
                        System.out.println("\n=== Menú Principal ===");
                        System.out.println("1. Ver perfil");
                        System.out.println("2. Cerrar sesión");
                        System.out.print("Selecciona una opción: ");
                        String opcion2 = scanner.nextLine();

                        if ("1".equals(opcion2)) {
                            salida.println("VER_PERFIL");
                            String perfil = entrada.readLine();
                            System.out.println("Perfil: " + perfil);
                        } else if ("2".equals(opcion2)) {
                            salida.println("LOGOUT");
                            String logout = entrada.readLine();
                            System.out.println("Logout: " + logout);
                        }

                    } else {
                        System.out.println("Usuario o contraseña incorrectos.");
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
