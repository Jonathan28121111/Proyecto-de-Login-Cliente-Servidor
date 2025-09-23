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
                        String respuestaLogin = entrada.readLine();
                        
                        if ("SOLICITAR_DATOS_LOGIN".equals(respuestaLogin)) {
                            System.out.print("Nombre de usuario: ");
                            String usuario = scanner.nextLine();
                            System.out.print("Contrasena: ");
                            String password = scanner.nextLine();
                            salida.println(usuario);
                            salida.println(password);

                            String resultadoLogin = entrada.readLine();
                            if ("LOGIN_EXITOSO".equals(resultadoLogin)) {
                                usuarioActivo = usuario;
                                System.out.println("Login exitoso: " + usuario);
                                boolean enSesion = true;

                                while (enSesion) {
                                    System.out.println("\n=== MENU PRINCIPAL ===");
                                    System.out.println("1. Ver perfil");
                                    System.out.println("2. Ver usuarios registrados");
                                    System.out.println("3. Ver bandeja de entrada");
                                    System.out.println("4. Eliminar mensajes");
                                    System.out.println("5. Enviar mensaje a usuario");
                                    System.out.println("6. Jugar Adivina el Numero");
                                    System.out.println("7. Bloquear usuario");
                                    System.out.println("8. Desbloquear usuario");
                                    System.out.println("9. Ver usuarios bloqueados");
                                    System.out.println("10. Cerrar sesion");
                                    System.out.println("11. Desconectar");
                                    System.out.print("Opcion: ");
                                    String op = scanner.nextLine();

                                    switch (op) {
                                        case "1":
                                            salida.println("VER_PERFIL");
                                            System.out.println(entrada.readLine());
                                            break;
                                        case "2":
                                            salida.println("VER_USUARIOS");
                                            String usuarios = entrada.readLine();
                                            if (usuarios.startsWith("USUARIOS:")) {
                                                String listaUsuarios = usuarios.substring(9);
                                                if (listaUsuarios.isEmpty()) {
                                                    System.out.println("No hay otros usuarios registrados.");
                                                } else {
                                                    System.out.println("Usuarios disponibles: " + listaUsuarios);
                                                }
                                            } else {
                                                System.out.println(usuarios);
                                            }
                                            break;
                                        case "3":
                                            manejarBandeja(salida, entrada);
                                            break;
                                        case "4":
                                            eliminarMensajes(salida, entrada);
                                            break;
                                        case "5":
                                            enviarMensajeUsuario(salida, entrada);
                                            break;
                                        case "6":
                                            jugarAdivinaNumero(salida, entrada);
                                            break;
                                        case "7":
                                            bloquearUsuario(salida, entrada);
                                            break;
                                        case "8":
                                            desbloquearUsuario(salida, entrada);
                                            break;
                                        case "9":
                                            verUsuariosBloqueados(salida, entrada);
                                            break;
                                        case "10":
                                            salida.println("LOGOUT");
                                            System.out.println(entrada.readLine());
                                            usuarioActivo = null;
                                            enSesion = false;
                                            break;
                                        case "11":
                                            salida.println("DESCONECTAR");
                                            System.out.println(entrada.readLine());
                                            enSesion = false;
                                            ejecutar = false;
                                            break;
                                        default:
                                            System.out.println("Opcion no valida");
                                    }
                                }
                            } else if ("USUARIO_BLOQUEADO".equals(resultadoLogin)) {
                                System.out.println("Tu cuenta ha sido bloqueada. No puedes iniciar sesion.");
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

    private static void manejarBandeja(PrintWriter salida, BufferedReader entrada) {
        try {
            salida.println("BANDEJA");
            String respuesta = entrada.readLine();
            
            if ("BANDEJA_CON_INDICES".equals(respuesta)) {
                System.out.println("\n=== BANDEJA DE ENTRADA ===");
                String linea;
                while (!(linea = entrada.readLine()).equals("FIN_BANDEJA")) {
                    System.out.println(linea);
                }
                System.out.println("===========================");
            } else {
                System.out.println(respuesta.substring(8)); 
            }
        } catch (IOException e) {
            System.out.println("Error al obtener la bandeja: " + e.getMessage());
        }
    }

    private static void eliminarMensajes(PrintWriter salida, BufferedReader entrada) {
        try {
            salida.println("ELIMINAR_MENSAJES");
            String respuesta = entrada.readLine();

            switch (respuesta) {
                case "ERROR:NO_AUTENTICADO":
                    System.out.println("Debes estar autenticado.");
                    break;
                case "NO_HAY_MENSAJES":
                    System.out.println("No tienes mensajes para eliminar.");
                    break;
                case "SOLICITAR_MENSAJE_ELIMINAR":
                    System.out.println("\n=== ELIMINAR MENSAJES ===");
                    System.out.println("Ingresa el numero del mensaje a eliminar (o 'todos' para eliminar todos):");
                    String opcion = scanner.nextLine();
                    salida.println(opcion);
                    
                    String resultado = entrada.readLine();
                    switch (resultado) {
                        case "TODOS_ELIMINADOS":
                            System.out.println("Todos los mensajes han sido eliminados.");
                            break;
                        case "MENSAJE_ELIMINADO":
                            System.out.println("Mensaje eliminado correctamente.");
                            break;
                        case "INDICE_INVALIDO":
                            System.out.println("Numero de mensaje invalido.");
                            break;
                        case "FORMATO_INVALIDO":
                            System.out.println("Formato invalido. Usa un numero o 'todos'.");
                            break;
                    }
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error al eliminar mensajes: " + e.getMessage());
        }
    }

    private static void enviarMensajeUsuario(PrintWriter salida, BufferedReader entrada) {
        try {
            salida.println("ENVIAR_MENSAJE");
            String respuesta = entrada.readLine();

            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar autenticado.");
                return;
            }

            if ("SOLICITAR_DESTINATARIO".equals(respuesta)) {
                System.out.println("\n=== ENVIAR MENSAJE ===");
                System.out.print("Destinatario: ");
                String destinatario = scanner.nextLine();
                salida.println(destinatario);

                String estadoDestinatario = entrada.readLine();
                switch (estadoDestinatario) {
                    case "DESTINATARIO_INVALIDO":
                        System.out.println("Destinatario invalido.");
                        return;
                    case "NO_PUEDES_ENVIARTE_MENSAJE":
                        System.out.println("No puedes enviarte un mensaje a ti mismo.");
                        return;
                    case "DESTINATARIO_NO_EXISTE":
                        System.out.println("El destinatario no existe.");
                        return;
                    case "USUARIO_BLOQUEADO":
                        System.out.println("Has bloqueado a este usuario. No puedes enviarle mensajes.");
                        return;
                    case "USUARIO_TE_BLOQUEO":
                        System.out.println("Este usuario te ha bloqueado. No puedes enviarle mensajes.");
                        return;
                    case "SOLICITAR_MENSAJE":
                        System.out.print("Mensaje: ");
                        String mensaje = scanner.nextLine();
                        salida.println(mensaje);

                        String estadoMensaje = entrada.readLine();
                        switch (estadoMensaje) {
                            case "MENSAJE_VACIO":
                                System.out.println("No puedes enviar un mensaje vacio.");
                                break;
                            case "MENSAJE_ENVIADO":
                                System.out.println("Mensaje enviado correctamente a " + destinatario);
                                break;
                        }
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al enviar mensaje: " + e.getMessage());
        }
    }

    private static void bloquearUsuario(PrintWriter salida, BufferedReader entrada) {
        try {
            salida.println("BLOQUEAR_USUARIO");
            String respuesta = entrada.readLine();

            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar autenticado.");
                return;
            }

            if ("SOLICITAR_USUARIO_BLOQUEAR".equals(respuesta)) {
                System.out.println("\n=== BLOQUEAR USUARIO ===");
                System.out.print("Usuario a bloquear: ");
                String usuarioBloquear = scanner.nextLine();
                salida.println(usuarioBloquear);

                String resultado = entrada.readLine();
                switch (resultado) {
                    case "USUARIO_BLOQUEADO_EXITOSO":
                        System.out.println("Usuario bloqueado correctamente.");
                        break;
                    case "USUARIO_NO_EXISTE":
                        System.out.println("El usuario no existe.");
                        break;
                    case "NO_PUEDES_BLOQUEARTE":
                        System.out.println("No puedes bloquearte a ti mismo.");
                        break;
                    case "USUARIO_YA_BLOQUEADO":
                        System.out.println("Ya has bloqueado a este usuario.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al bloquear usuario: " + e.getMessage());
        }
    }

    private static void desbloquearUsuario(PrintWriter salida, BufferedReader entrada) {
        try {
            salida.println("DESBLOQUEAR_USUARIO");
            String respuesta = entrada.readLine();

            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar autenticado.");
                return;
            }

            if ("SOLICITAR_USUARIO_DESBLOQUEAR".equals(respuesta)) {
                System.out.println("\n=== DESBLOQUEAR USUARIO ===");
                System.out.print("Usuario a desbloquear: ");
                String usuarioDesbloquear = scanner.nextLine();
                salida.println(usuarioDesbloquear);

                String resultado = entrada.readLine();
                switch (resultado) {
                    case "USUARIO_DESBLOQUEADO_EXITOSO":
                        System.out.println("Usuario desbloqueado correctamente.");
                        break;
                    case "USUARIO_NO_BLOQUEADO":
                        System.out.println("No has bloqueado a este usuario.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al desbloquear usuario: " + e.getMessage());
        }
    }

    private static void verUsuariosBloqueados(PrintWriter salida, BufferedReader entrada) {
        try {
            salida.println("VER_BLOQUEADOS");
            String respuesta = entrada.readLine();

            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar autenticado.");
                return;
            }

            if (respuesta.startsWith("BLOQUEADOS:")) {
                String usuariosBloqueados = respuesta.substring(11);
                if (usuariosBloqueados.isEmpty()) {
                    System.out.println("No has bloqueado a ningun usuario.");
                } else {
                    System.out.println("Usuarios bloqueados: " + usuariosBloqueados);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al obtener usuarios bloqueados: " + e.getMessage());
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