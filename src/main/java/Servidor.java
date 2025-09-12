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
                    System.out.print("Escribe destinatario (usuario) o 'salir': ");
                    String usuario = sc.nextLine().trim();
                    if (usuario.equalsIgnoreCase("salir")) break;

                    if (!usuarios.containsKey(usuario)) {
                        System.out.println("Ese usuario no existe.");
                        continue;
                    }

                    System.out.print("Escribe el mensaje: ");
                    String mensaje = sc.nextLine();
                    enviarMensaje(usuario, "[Servidor]: " + mensaje);
                    System.out.println("Mensaje enviado a " + usuario);
                }
                sc.close();
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

        private void manejarVerPerfil() { salida.println(usuarioActivo != null ? "PERFIL:" + usuarioActivo : "ERROR:NO_AUTENTICADO"); }
        private void manejarVerUsuarios() { salida.println(usuarioActivo != null ? "USUARIOS:" + String.join(",", usuarios.keySet()) : "ERROR:NO_AUTENTICADO"); }
        private void manejarBandeja() {
            if (usuarioActivo == null) { salida.println("ERROR:NO_AUTENTICADO"); return; }
            List<String> mensajes = bandejas.getOrDefault(usuarioActivo, new ArrayList<>());
            if (mensajes.isEmpty()) salida.println("BANDEJA:No tienes mensajes.");
            else { salida.println("BANDEJA:" + String.join(" | ", mensajes)); mensajes.clear(); }
        }
        private void manejarLogout() { usuarioActivo = null; salida.println("LOGOUT_EXITOSO"); }

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