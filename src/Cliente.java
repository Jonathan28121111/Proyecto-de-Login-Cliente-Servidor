import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class Cliente {
    private static String usuarioActivo = null;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             DataInputStream dataInput = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream())) {

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
                                    System.out.println("10. Listar archivos .txt");
                                    System.out.println("11. Crear/Editar archivo");
                                    System.out.println("12. Enviar archivo");
                                    System.out.println("13. Ver archivos recibidos");
                                    System.out.println("14. Descargar archivo");
                                    System.out.println("15. Cerrar sesion");
                                    System.out.println("16. Desconectar");
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
                                            listarArchivos(salida, entrada);
                                            break;
                                        case "11":
                                            crearEditarArchivo();
                                            break;
                                        case "12":
                                            enviarArchivo(salida, entrada, dataOutput);
                                            break;
                                        case "13":
                                            verArchivosRecibidos(salida, entrada);
                                            break;
                                        case "14":
                                            descargarArchivo(salida, entrada, dataInput);
                                            break;
                                        case "15":
                                            salida.println("LOGOUT");
                                            System.out.println(entrada.readLine());
                                            usuarioActivo = null;
                                            enSesion = false;
                                            break;
                                        case "16":
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
            
            if ("BANDEJA:No tienes mensajes.".equals(respuesta)) {
                System.out.println("No tienes mensajes.");
                return;
            }

            if ("BANDEJA_PAGINADA".equals(respuesta)) {
                boolean viendo = true;
                while (viendo) {
                    String linea;
                    while (!(linea = entrada.readLine()).equals("FIN_PAGINA")) {
                        System.out.println(linea);
                    }

                    System.out.print("\nOpciones: [s]iguiente, [a]nterior, [q]uitar: ");
                    String opcion = scanner.nextLine();
                    switch (opcion.toLowerCase()) {
                        case "s":
                            salida.println("SIGUIENTE");
                            break;
                        case "a":
                            salida.println("ANTERIOR");
                            break;
                        case "q":
                            salida.println("SALIR_BANDEJA");
                            viendo = false;
                            break;
                        default:
                            System.out.println("Opcion invalida");
                            salida.println("NINGUNA");
                    }
                    respuesta = entrada.readLine();
                    if ("SALISTE_BANDEJA".equals(respuesta)) {
                        viendo = false;
                    }
                }
            } else {
                System.out.println(respuesta);
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

    private static void crearEditarArchivo() {
        try {
            System.out.println("\n--- CREAR/EDITAR ARCHIVO TXT ---");
            System.out.println("1. Crear archivo nuevo");
            System.out.println("2. Editar archivo existente");
            System.out.print("Opcion: ");
            String opcion = scanner.nextLine();
            
            if (opcion.equals("1")) {
                System.out.print("Nombre del archivo (sin .txt): ");
                String nombre = scanner.nextLine();
                if (!nombre.toLowerCase().endsWith(".txt")) {
                    nombre = nombre + ".txt";
                }
                
                System.out.println("\nEscribe el contenido (escribe 'FIN' en una linea para terminar):");
                StringBuilder contenido = new StringBuilder();
                while (true) {
                    String linea = scanner.nextLine();
                    if (linea.equals("FIN")) {
                        break;
                    }
                    contenido.append(linea).append("\n");
                }
                
                File archivo = new File(nombre);
                try (FileWriter writer = new FileWriter(archivo)) {
                    writer.write(contenido.toString());
                    System.out.println("Archivo creado: " + archivo.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Error al crear archivo: " + e.getMessage());
                }
                
            } else if (opcion.equals("2")) {
                System.out.print("Nombre del archivo a editar: ");
                String nombreArchivo = scanner.nextLine();
                
                File archivo = new File(nombreArchivo);
                if (!archivo.exists()) {
                    System.out.println("Archivo no existe");
                    return;
                }
                
                if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                    System.out.println("Solo archivos txt");
                    return;
                }
                
                // Mostrar contenido actual
                System.out.println("\n--- CONTENIDO ACTUAL ---");
                try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        System.out.println(linea);
                    }
                } catch (IOException e) {
                    System.out.println("Error al leer archivo: " + e.getMessage());
                    return;
                }
                System.out.println("------------------------");
                
                System.out.println("\n1. Sobrescribir archivo");
                System.out.println("2. Agregar al final");
                System.out.print("Opcion: ");
                String modo = scanner.nextLine();
                
                System.out.println("\nEscribe el contenido (escribe 'FIN' en una linea para terminar):");
                StringBuilder contenido = new StringBuilder();
                while (true) {
                    String linea = scanner.nextLine();
                    if (linea.equals("FIN")) {
                        break;
                    }
                    contenido.append(linea).append("\n");
                }
                
                if (modo.equals("1")) {
                    // Sobrescribir
                    try (FileWriter writer = new FileWriter(archivo, false)) {
                        writer.write(contenido.toString());
                        System.out.println("Archivo sobrescrito");
                    } catch (IOException e) {
                        System.out.println("Error al escribir: " + e.getMessage());
                    }
                } else if (modo.equals("2")) {
                    // Agregar
                    try (FileWriter writer = new FileWriter(archivo, true)) {
                        writer.write(contenido.toString());
                        System.out.println("Contenido agregado al archivo");
                    } catch (IOException e) {
                        System.out.println("Error al escribir: " + e.getMessage());
                    }
                } else {
                    System.out.println("Opcion no valida");
                }
            } else {
                System.out.println("Opcion no valida");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void listarArchivos(PrintWriter salida, BufferedReader entrada) {
        try {
            System.out.println("\n--- VER ARCHIVOS TXT ---");
            System.out.print("Carpeta (Enter para carpeta actual): ");
            String directorio = scanner.nextLine();
            
            salida.println("LISTAR_ARCHIVOS");
            
            String respuesta = entrada.readLine();
            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar conectado");
                return;
            }
            
            if ("SOLICITAR_DIRECTORIO".equals(respuesta)) {
                salida.println(directorio);
                
                String resultado = entrada.readLine();
                switch (resultado) {
                    case "DIRECTORIO_INVALIDO":
                        System.out.println("Carpeta no existe");
                        break;
                    case "NO_HAY_ARCHIVOS_TXT":
                        System.out.println("No hay archivos txt");
                        break;
                    case "ARCHIVOS_ENCONTRADOS":
                        System.out.println("\nArchivos txt encontrados:");
                        String linea;
                        while (!(linea = entrada.readLine()).equals("FIN_LISTA_ARCHIVOS")) {
                            String[] partes = linea.split(":");
                            if (partes.length == 2) {
                                System.out.println("- " + partes[0] + " (" + partes[1] + " bytes)");
                            }
                        }
                        break;
                    case "ERROR_LISTANDO_ARCHIVOS":
                        System.out.println("Error al buscar archivos");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void enviarArchivo(PrintWriter salida, BufferedReader entrada, DataOutputStream dataOutput) {
        try {
            System.out.println("\n--- ENVIAR ARCHIVO ---");
            
            salida.println("ENVIAR_ARCHIVO");
            String respuesta = entrada.readLine();
            
            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar conectado");
                return;
            }
            
            if ("SOLICITAR_DESTINATARIO_ARCHIVO".equals(respuesta)) {
                System.out.print("Para quien: ");
                String destinatario = scanner.nextLine();
                salida.println(destinatario);
                
                String estadoDestinatario = entrada.readLine();
                switch (estadoDestinatario) {
                    case "DESTINATARIO_INVALIDO":
                        System.out.println("Usuario no valido");
                        return;
                    case "DESTINATARIO_NO_EXISTE":
                        System.out.println("Usuario no existe");
                        return;
                    case "NO_PUEDES_ENVIARTE_ARCHIVO":
                        System.out.println("No puedes enviarte archivo a ti mismo");
                        return;
                    case "USUARIO_BLOQUEADO":
                        System.out.println("Has bloqueado a este usuario");
                        return;
                    case "USUARIO_TE_BLOQUEO":
                        System.out.println("Este usuario te bloqueo");
                        return;
                    case "SOLICITAR_NOMBRE_ARCHIVO":
                        System.out.print("Archivo a enviar: ");
                        String rutaArchivo = scanner.nextLine();
                        
                        File archivo = new File(rutaArchivo);
                        if (!archivo.exists() || !archivo.isFile()) {
                            System.out.println("Archivo no existe");
                            salida.println("");
                            return;
                        }
                        
                        if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                            System.out.println("Solo archivos txt");
                            salida.println("");
                            return;
                        }
                        
                        if (archivo.length() > 10 * 1024 * 1024) {
                            System.out.println("Archivo muy grande (maximo 10MB)");
                            salida.println("");
                            return;
                        }
                        
                        try {
                            byte[] contenido = Files.readAllBytes(archivo.toPath());
                            
                            salida.println(archivo.getName());
                            
                            dataOutput.writeInt(contenido.length);
                            dataOutput.write(contenido);
                            dataOutput.flush();
                            
                            String resultado = entrada.readLine();
                            switch (resultado) {
                                case "ARCHIVO_ENVIADO":
                                    System.out.println("Archivo enviado a " + destinatario);
                                    break;
                                case "TAMAÑO_ARCHIVO_INVALIDO":
                                    System.out.println("Tamaño de archivo invalido");
                                    break;
                                case "ERROR_RECIBIENDO_ARCHIVO":
                                    System.out.println("Error al enviar archivo");
                                    break;
                                case "NOMBRE_ARCHIVO_INVALIDO":
                                    System.out.println("Nombre de archivo invalido");
                                    break;
                            }
                            
                        } catch (IOException e) {
                            System.out.println("Error al leer archivo: " + e.getMessage());
                            salida.println("");
                        }
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void verArchivosRecibidos(PrintWriter salida, BufferedReader entrada) {
        try {
            System.out.println("\n--- ARCHIVOS RECIBIDOS ---");
            
            salida.println("VER_ARCHIVOS_RECIBIDOS");
            String respuesta = entrada.readLine();
            
            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar conectado");
                return;
            }
            
            switch (respuesta) {
                case "NO_HAY_ARCHIVOS_RECIBIDOS":
                    System.out.println("No tienes archivos recibidos");
                    break;
                case "ARCHIVOS_RECIBIDOS":
                    System.out.println("\nArchivos que recibiste:");
                    String linea;
                    while (!(linea = entrada.readLine()).equals("FIN_ARCHIVOS_RECIBIDOS")) {
                        System.out.println(linea);
                    }
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void descargarArchivo(PrintWriter salida, BufferedReader entrada, DataInputStream dataInput) {
        try {
            System.out.println("\n--- DESCARGAR ARCHIVO ---");
            
            salida.println("DESCARGAR_ARCHIVO");
            String respuesta = entrada.readLine();
            
            if ("ERROR:NO_AUTENTICADO".equals(respuesta)) {
                System.out.println("Debes estar conectado");
                return;
            }
            
            switch (respuesta) {
                case "NO_HAY_ARCHIVOS_PARA_DESCARGAR":
                    System.out.println("No tienes archivos para descargar");
                    break;
                case "SOLICITAR_INDICE_ARCHIVO":
                    System.out.print("Numero del archivo: ");
                    String indice = scanner.nextLine();
                    salida.println(indice);
                    
                    String resultado = entrada.readLine();
                    switch (resultado) {
                        case "ARCHIVO_DISPONIBLE":
                            String nombreArchivo = entrada.readLine();
                            String tamañoStr = entrada.readLine();
                            int tamaño = Integer.parseInt(tamañoStr);
                            
                            System.out.println("Descargando: " + nombreArchivo + " (" + tamaño + " bytes)");
                            System.out.print("Guardar en carpeta (Enter para actual): ");
                            String carpeta = scanner.nextLine();
                            
                            if (carpeta.trim().isEmpty()) {
                                carpeta = System.getProperty("user.dir");
                            }
                            
                            int tamañoArchivo = dataInput.readInt();
                            byte[] contenido = new byte[tamañoArchivo];
                            dataInput.readFully(contenido);
                            
                            File archivoDestino = new File(carpeta, nombreArchivo);
                            
                            int contador = 1;
                            while (archivoDestino.exists()) {
                                String nombreBase = nombreArchivo.substring(0, nombreArchivo.lastIndexOf('.'));
                                String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.'));
                                archivoDestino = new File(carpeta, nombreBase + "_" + contador + extension);
                                contador++;
                            }
                            
                            try {
                                Files.write(archivoDestino.toPath(), contenido);
                                System.out.println("Archivo guardado en: " + archivoDestino.getAbsolutePath());
                            } catch (IOException e) {
                                System.out.println("Error al guardar archivo: " + e.getMessage());
                            }
                            break;
                            
                        case "INDICE_ARCHIVO_INVALIDO":
                            System.out.println("Numero de archivo invalido");
                            break;
                        case "INDICE_FORMATO_INVALIDO":
                            System.out.println("Formato de numero invalido");
                            break;
                        case "ERROR_ENVIANDO_ARCHIVO":
                            System.out.println("Error al descargar archivo");
                            break;
                    }
                    break;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Error en el tamaño del archivo");
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