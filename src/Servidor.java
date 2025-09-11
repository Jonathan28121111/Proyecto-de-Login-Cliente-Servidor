import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static Map<String, String> usuarios = new HashMap<>();
    private static Map<String, List<String>> bandejas = new HashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

    public static void main(String[] args) {
        cargarUsuarios();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado en puerto 12345");

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
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.err.println("No se pudo crear archivo usuarios: " + e.getMessage());
            }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
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
                        case "REGISTRO":
                            manejarRegistro();
                            break;
                        case "LOGIN":
                            manejarLogin();
                            break;
                        case "VER_PERFIL":
                            manejarVerPerfil();
                            break;
                        case "VER_USUARIOS":
                            manejarVerUsuarios();
                            break;
                        case "BANDEJA":
                            manejarBandeja();
                            break;
                        case "LOGOUT":
                            manejarLogout();
                            break;
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
                try {
                    if (clienteSocket != null) clienteSocket.close();
                } catch (IOException e) {
                    System.err.println("Error cerrando cliente: " + e.getMessage());
                }
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
            if (usuarioActivo != null) {
                salida.println("PERFIL:" + usuarioActivo);
            } else {
                salida.println("ERROR:NO_AUTENTICADO");
            }
        }

        private void manejarVerUsuarios() {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }
            String lista;
            synchronized (usuarios) {
                lista = String.join(",", usuarios.keySet());
            }
            salida.println("USUARIOS_REGISTRADOS:" + lista);
        }

        private void manejarBandeja() {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }
            List<String> mensajes = bandejas.getOrDefault(usuarioActivo, new ArrayList<>());
            if (mensajes.isEmpty()) {
                salida.println("BANDEJA:No tienes mensajes.");
            } else {
                salida.println("BANDEJA:" + String.join(" | ", mensajes));
                mensajes.clear();
            }
        }

        private void manejarLogout() {
            usuarioActivo = null;
            salida.println("LOGOUT_EXITOSO");
        }
    }
}
