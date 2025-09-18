import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Servidor {
    private static Map<String, String> usuarios = new ConcurrentHashMap<>();
    private static Map<String, List<String>> bandejas = new ConcurrentHashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final int PUERTO = 12345;

    public static void main(String[] args) {
        cargarUsuarios();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);

            Thread adminThread = new Thread(() -> {
                Scanner sc = new Scanner(System.in);
                while (true) {
                    System.out.println("\n=== Consola del servidor ===");
                    System.out.println("1. Enviar mensaje a usuario");
                    System.out.println("2. Expulsar usuario");
                    System.out.println("3. Ver usuarios registrados");
                    System.out.println("4. Salir");
                    System.out.print("Selecciona una opcion: ");
                    String opcion = sc.nextLine().trim();

                    switch (opcion) {
                        case "1":
                            System.out.print("Escribe destinatario (usuario): ");
                            String usuario = sc.nextLine().trim();
                            if (!usuarios.containsKey(usuario)) {
                                System.out.println("Ese usuario no existe.");
                                continue;
                            }
                            System.out.print("Escribe el mensaje: ");
                            String mensaje = sc.nextLine();
                            enviarMensaje(usuario, "[Servidor]: " + mensaje);
                            System.out.println("Mensaje enviado a " + usuario);
                            break;
                        
                        case "2":
                            System.out.print("Usuario a expulsar: ");
                            String usuarioExpulsar = sc.nextLine().trim();
                            if (expulsarUsuario(usuarioExpulsar)) {
                                System.out.println("Usuario " + usuarioExpulsar + " expulsado exitosamente.");
                            } else {
                                System.out.println("Usuario no encontrado o error al expulsar.");
                            }
                            break;
                        
                        case "3":
                            System.out.println("Usuarios registrados: " + usuarios.keySet());
                            break;
                        
                        case "4":
                            System.out.println("Cerrando servidor...");
                            System.exit(0);
                            break;
                        
                        default:
                            System.out.println("Opcion no valida.");
                    }
                }
            });
            adminThread.setDaemon(true);
            adminThread.start();

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(new ManejadorCliente(clienteSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static void cargarUsuarios() {
        File f = new File(ARCHIVO_USUARIOS);
        if (!f.exists()) {
            try { f.createNewFile(); } catch (IOException e) { System.err.println("Error creando archivo usuarios: " + e.getMessage()); }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                    bandejas.putIfAbsent(partes[0], new ArrayList<>());
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo usuarios: " + e.getMessage());
        }
    }

    private static void guardarUsuario(String usuario, String password) {
        try (FileWriter writer = new FileWriter(ARCHIVO_USUARIOS, true)) {
            writer.write(usuario + ":" + password + "\n");
        } catch (IOException e) {
            System.err.println("Error guardando usuario: " + e.getMessage());
        }
    }

    private static void reescribirArchivoUsuarios() {
        try (FileWriter writer = new FileWriter(ARCHIVO_USUARIOS, false)) {
            for (Map.Entry<String, String> entry : usuarios.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error reescribiendo archivo usuarios: " + e.getMessage());
        }
    }

    private static synchronized boolean expulsarUsuario(String usuario) {
        if (usuarios.containsKey(usuario)) {
            usuarios.remove(usuario);
            bandejas.remove(usuario);
            reescribirArchivoUsuarios();
            System.out.println("Usuario " + usuario + " ha sido expulsado del sistema.");
            return true;
        }
        return false;
    }

    private static synchronized void enviarMensaje(String usuario, String mensaje) {
        bandejas.putIfAbsent(usuario, new ArrayList<>());
        bandejas.get(usuario).add(mensaje);
    }

    private static class ManejadorCliente implements Runnable {
        private Socket clienteSocket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private String usuarioActivo = null;
        private Random random = new Random();

        public ManejadorCliente(Socket socket) {
            this.clienteSocket = socket;
        }

        @Override
        public void run() {
            try {
                entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                salida = new PrintWriter(clienteSocket.getOutputStream(), true);

                boolean conexionActiva = true;
                while (conexionActiva) {
                    String comando = entrada.readLine();
                    if (comando == null) break;
                    comando = comando.trim().toUpperCase();

                    switch (comando) {
                        case "REGISTRO": manejarRegistro(); break;
                        case "LOGIN": manejarLogin(); break;
                        case "VER_PERFIL": manejarVerPerfil(); break;
                        case "VER_USUARIOS": manejarVerUsuarios(); break;
                        case "BANDEJA": manejarBandeja(); break;
                        case "ELIMINAR_MENSAJES": manejarEliminarMensajes(); break;
                        case "ENVIAR_MENSAJE": manejarEnviarMensaje(); break;
                        case "LOGOUT": manejarLogout(); break;
                        case "ADIVINA_NUMERO": jugarAdivinaNumero(); break;
                        case "DESCONECTAR":
                            salida.println("DESCONEXION_EXITOSA");
                            conexionActiva = false;
                            break;
                        default:
                            salida.println("COMANDO_DESCONOCIDO");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error manejando cliente: " + e.getMessage());
            } finally {
                try { if (clienteSocket != null) clienteSocket.close(); } catch (IOException e) { System.err.println("Error cerrando cliente: " + e.getMessage()); }
            }
        }

        private void manejarRegistro() throws IOException {
            salida.println("SOLICITAR_DATOS_REGISTRO");
            String usuario = entrada.readLine();
            String password = entrada.readLine();

            if (usuario == null || password == null) {
                salida.println("ERROR:DATOS_INVALIDOS");
                return;
            }

            synchronized (usuarios) {
                if (usuarios.containsKey(usuario)) {
                    salida.println("USUARIO_EXISTE");
                    salida.println("MENSAJE_SERVIDOR:El usuario ya existe.");
                } else {
                    usuarios.put(usuario, password);
                    bandejas.putIfAbsent(usuario, new ArrayList<>());
                    guardarUsuario(usuario, password);
                    salida.println("REGISTRO_EXITOSO");
                    salida.println("MENSAJE_SERVIDOR:Usuario registrado correctamente.");
                    enviarMensaje(usuario, "Hola " + usuario + ", tu cuenta fue creada.");
                }
            }
        }

        private void manejarLogin() throws IOException {
            salida.println("SOLICITAR_DATOS_LOGIN");
            String usuario = entrada.readLine();
            String password = entrada.readLine();

            if (usuario == null || password == null) {
                salida.println("ERROR:DATOS_INVALIDOS");
                return;
            }

            synchronized (usuarios) {
                if (usuarios.containsKey(usuario) && usuarios.get(usuario).equals(password)) {
                    usuarioActivo = usuario;
                    salida.println("LOGIN_EXITOSO");
                } else {
                    salida.println("CREDENCIALES_INVALIDAS");
                }
            }
        }

        private void manejarVerPerfil() { 
            salida.println(usuarioActivo != null ? "PERFIL:" + usuarioActivo : "ERROR:NO_AUTENTICADO"); 
        }
        
        private void manejarVerUsuarios() { 
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }
            
            Set<String> usuariosDisponibles = new HashSet<>(usuarios.keySet());
            usuariosDisponibles.remove(usuarioActivo); // Remover el usuario actual de la lista
            
            salida.println("USUARIOS:" + String.join(",", usuariosDisponibles)); 
        }

        private void manejarBandeja() throws IOException {
            if (usuarioActivo == null) { 
                salida.println("ERROR:NO_AUTENTICADO"); 
                return; 
            }
            
            List<String> mensajes = bandejas.getOrDefault(usuarioActivo, new ArrayList<>());
            if (mensajes.isEmpty()) {
                salida.println("BANDEJA:No tienes mensajes.");
            } else {
                salida.println("BANDEJA_CON_INDICES");
                for (int i = 0; i < mensajes.size(); i++) {
                    salida.println((i + 1) + ". " + mensajes.get(i));
                }
                salida.println("FIN_BANDEJA");
            }
        }

        private void manejarEliminarMensajes() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            List<String> mensajes = bandejas.getOrDefault(usuarioActivo, new ArrayList<>());
            if (mensajes.isEmpty()) {
                salida.println("NO_HAY_MENSAJES");
                return;
            }

            salida.println("SOLICITAR_MENSAJE_ELIMINAR");
            String respuesta = entrada.readLine();

            if ("TODOS".equalsIgnoreCase(respuesta)) {
                mensajes.clear();
                salida.println("TODOS_ELIMINADOS");
            } else {
                try {
                    int indice = Integer.parseInt(respuesta) - 1;
                    if (indice >= 0 && indice < mensajes.size()) {
                        mensajes.remove(indice);
                        salida.println("MENSAJE_ELIMINADO");
                    } else {
                        salida.println("INDICE_INVALIDO");
                    }
                } catch (NumberFormatException e) {
                    salida.println("FORMATO_INVALIDO");
                }
            }
        }

        private void manejarEnviarMensaje() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            salida.println("SOLICITAR_DESTINATARIO");
            String destinatario = entrada.readLine();

            if (destinatario == null || destinatario.trim().isEmpty()) {
                salida.println("DESTINATARIO_INVALIDO");
                return;
            }

            if (destinatario.equals(usuarioActivo)) {
                salida.println("NO_PUEDES_ENVIARTE_MENSAJE");
                return;
            }

            if (!usuarios.containsKey(destinatario)) {
                salida.println("DESTINATARIO_NO_EXISTE");
                return;
            }

            salida.println("SOLICITAR_MENSAJE");
            String mensaje = entrada.readLine();

            if (mensaje == null || mensaje.trim().isEmpty()) {
                salida.println("MENSAJE_VACIO");
                return;
            }

            enviarMensaje(destinatario, "[De: " + usuarioActivo + "]: " + mensaje);
            salida.println("MENSAJE_ENVIADO");
        }

        private void manejarLogout() { 
            usuarioActivo = null; 
            salida.println("LOGOUT_EXITOSO"); 
        }

        private void jugarAdivinaNumero() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            int numeroSecreto = random.nextInt(10) + 1;
            int intentos = 0;
            boolean acertado = false;
            
            salida.println("=== JUEGO: ADIVINA EL NUMERO ===");
            salida.println("Adivina el numero del 1 al 10. Tienes 3 intentos.");

            while (intentos < 3 && !acertado) {
                String entradaCliente = this.entrada.readLine();
                if (entradaCliente == null) break;

                int intento;
                try { 
                    intento = Integer.parseInt(entradaCliente.trim()); 
                } catch (NumberFormatException e) { 
                    salida.println("Eso no es un numero valido. Intenta de nuevo (no cuenta como intento).");
                    continue; 
                }

                if (intento < 1 || intento > 10) {
                    salida.println("El numero debe estar entre 1 y 10. Intenta de nuevo (no cuenta como intento).");
                    continue;
                }

                intentos++;
                
                if (intento == numeroSecreto) { 
                    salida.println("¡Correcto! Adivinaste el numero en " + intentos + " intento(s)."); 
                    acertado = true; 
                } else {
                    if (intentos < 3) {
                        if (intento < numeroSecreto) {
                            salida.println("Incorrecto. El numero secreto es mayor. Te quedan " + (3 - intentos) + " intento(s).");
                        } else {
                            salida.println("Incorrecto. El numero secreto es menor. Te quedan " + (3 - intentos) + " intento(s).");
                        }
                    }
                }
            }

            if (!acertado) {
                salida.println("No lograste adivinar el numero. Era: " + numeroSecreto);
            }
            
            salida.println("Fin del juego.");
        }
    }
}