import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {
    private static String usuarioActivo = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

            boolean ejecutar = true;
            while (ejecutar) {
                System.out.println("=== Sistema de autenticacion ===");
                System.out.println("1. Registrarse");
                System.out.println("2. Iniciar sesion");
                System.out.println("3. Salir");
                System.out.print("Selecciona una opcion: ");
                String opcion = scanner.nextLine();

                if ("1".equals(opcion)) {
                    salida.println("REGISTRO");
                    String respuesta = entrada.readLine(); 
                    if ("SOLICITAR_DATOS_REGISTRO".equals(respuesta)) {
                        System.out.print("Nombre de usuario: ");
                        String usuario = scanner.nextLine();
                        System.out.print("Contrasena: ");
                        String password = scanner.nextLine();

                        salida.println(usuario);
                        salida.println(password);

                        String resultado = entrada.readLine();
                        String mensaje = entrada.readLine();
                        System.out.println("Resultado: " + resultado);
                        System.out.println("Mensaje del servidor: " + mensaje);
                    } else {
                        System.out.println("Respuesta inesperada del servidor: " + respuesta);
                    }

                } else if ("2".equals(opcion)) {
                    salida.println("LOGIN");
                    String respuesta = entrada.readLine(); 
                    if ("SOLICITAR_DATOS_LOGIN".equals(respuesta)) {
                        System.out.print("Nombre de usuario: ");
                        String usuario = scanner.nextLine();
                        System.out.print("Contrasena: ");
                        String password = scanner.nextLine();

                        salida.println(usuario);
                        salida.println(password);

                        String resultado = entrada.readLine();
                        if ("LOGIN_EXITOSO".equals(resultado)) {
                            usuarioActivo = usuario;
                            System.out.println("Login exitoso: " + usuario);
                            boolean enSesion = true;
                            while (enSesion) {
                                System.out.println("\n=== MENU PRINCIPAL ===");
                                System.out.println("1. Ver perfil");
                                System.out.println("2. Ver usuarios registrados");
                                System.out.println("3. Cerrar sesion");
                                System.out.println("4. Desconectar");
                                System.out.print("Opcion: ");
                                String op = scanner.nextLine();

                                switch (op) {
                                    case "1":
                                        salida.println("VER_PERFIL");
                                        String resp = entrada.readLine();
                                        if (resp != null && resp.startsWith("PERFIL:")) {
                                            System.out.println("Perfil: " + resp.substring("PERFIL:".length()));
                                        } else {
                                            System.out.println("Respuesta: " + resp);
                                        }
                                        break;
                                    case "2":
                                        salida.println("VER_USUARIOS");
                                        String resp2 = entrada.readLine();
                                        if (resp2 != null && resp2.startsWith("USUARIOS_REGISTRADOS:")) {
                                            String lista = resp2.substring("USUARIOS_REGISTRADOS:".length());
                                            if (lista.isEmpty()) {
                                                System.out.println("No hay usuarios registrados.");
                                            } else {
                                                String[] arr = lista.split(",");
                                                System.out.println("Usuarios registrados:");
                                                for (String u : arr) {
                                                    if (!u.trim().isEmpty()) System.out.println(" - " + u);
                                                }
                                            }
                                        } else {
                                            System.out.println("Respuesta: " + resp2);
                                        }
                                        break;
                                    case "3":
                                        salida.println("LOGOUT");
                                        String logout = entrada.readLine();
                                        System.out.println("Respuesta: " + logout);
                                        usuarioActivo = null;
                                        enSesion = false;
                                        break;
                                    case "4":
                                        salida.println("DESCONECTAR");
                                        String descon = entrada.readLine();
                                        System.out.println("Respuesta: " + descon);
                                        enSesion = false;
                                        ejecutar = false;
                                        break;
                                    default:
                                        System.out.println("Opcion no valida");
                                }
                            }
                        } else {
                            System.out.println("Login fallido: usuario o contrasena incorrectos.");
                        }
                    } else {
                        System.out.println("Respuesta inesperada del servidor: " + respuesta);
                    }

                } else if ("3".equals(opcion)) {
                    System.out.println("Saliendo del cliente.");
                    ejecutar = false;
                } else {
                    System.out.println("Opcion no valida.");
                }
            }

        } catch (IOException e) {
            System.err.println("Error conexion: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
