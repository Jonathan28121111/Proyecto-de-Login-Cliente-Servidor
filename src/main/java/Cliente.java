import java.io.*;
import java.net.*;
import java.util.*;

public class Cliente {
    private static String usuarioActivo = null;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
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

                switch (opcion) {
                    case "1":
                        salida.println("REGISTRO");
                        if ("SOLICITAR_DATOS_REGISTRO".equals(entrada.readLine())) {
                            System.out.print("Nombre de usuario: ");
                            String usuario = scanner.nextLine();
                            System.out.print("Contrasena: ");
                            String password = scanner.nextLine();
                            salida.println(usuario);
                            salida.println(password);
                            System.out.println("Resultado: " + entrada.readLine());
                            System.out.println("Mensaje del servidor: " + entrada.readLine());
                        }
                        break;

                    case "2":
                        salida.println("LOGIN");
                        if ("SOLICITAR_DATOS_LOGIN".equals(entrada.readLine())) {
                            System.out.print("Nombre de usuario: ");
                            String usuario = scanner.nextLine();
                            System.out.print("Contrasena: ");
                            String password = scanner.nextLine();
                            salida.println(usuario);
                            salida.println(password);

                            if ("LOGIN_EXITOSO".equals(entrada.readLine())) {
                                usuarioActivo = usuario;
                                System.out.println("Login exitoso: " + usuario);
                                boolean enSesion = true;

                                while (enSesion) {
                                    System.out.println("\n=== MENU PRINCIPAL ===");
                                    System.out.println("1. Ver perfil");
                                    System.out.println("2. Ver usuarios registrados");
                                    System.out.println("3. Ver bandeja de entrada");
                                    System.out.println("4. Cerrar sesion");
                                    System.out.println("5. Desconectar");
                                    System.out.println("6. Jugar Adivina el Numero");
                                    System.out.print("Opcion: ");
                                    String op = scanner.nextLine();

                                    switch (op) {
                                        case "1":
                                            salida.println("VER_PERFIL");
                                            System.out.println(entrada.readLine());
                                            break;
                                        case "2":
                                            salida.println("VER_USUARIOS");
                                            System.out.println(entrada.readLine());
                                            break;
                                        case "3":
                                            salida.println("BANDEJA");
                                            System.out.println(entrada.readLine());
                                            break;
                                        case "4":
                                            salida.println("LOGOUT");
                                            System.out.println(entrada.readLine());
                                            usuarioActivo = null;
                                            enSesion = false;
                                            break;
                                        case "5":
                                            salida.println("DESCONECTAR");
                                            System.out.println(entrada.readLine());
                                            enSesion = false;
                                            ejecutar = false;
                                            break;
                                        case "6":
                                            jugarAdivinaNumero(salida, entrada);
                                            break;
                                        default:
                                            System.out.println("Opcion no valida");
                                    }
                                }
                            } else {
                                System.out.println("Login fallido.");
                            }
                        }
                        break;

                    case "3":
                        ejecutar = false;
                        break;

                    default:
                        System.out.println("Opcion no valida.");
                }
            }

        } catch (IOException e) {
            System.err.println("Error conexion: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    private static void jugarAdivinaNumero(PrintWriter salida, BufferedReader entrada) {
        try {
            System.out.println("\n=== INICIANDO JUEGO ADIVINA EL NUMERO ===");
            salida.println("ADIVINA_NUMERO");
            
            boolean juegoActivo = true;
            while (juegoActivo) {
                String mensajeServidor = entrada.readLine();
                if (mensajeServidor == null) break;
                
                System.out.println(mensajeServidor);

                if (mensajeServidor.contains("Fin del juego")) {
                    juegoActivo = false;
                    break;
                }
                
                if (mensajeServidor.contains("Adivina el numero") || 
                    mensajeServidor.contains("Incorrecto") ||
                    mensajeServidor.contains("numero valido")) {
                    
                    System.out.print("Tu respuesta: ");
                    String intentoUsuario = scanner.nextLine();
                    salida.println(intentoUsuario);
                }
            }
            
            System.out.println("=== JUEGO TERMINADO ===\n");

        } catch (IOException e) {
            System.out.println("Error durante el juego: " + e.getMessage());
        }
    }
}