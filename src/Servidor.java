import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    private static Map<String, String> usuarios = new HashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

    public static void main(String[] args) {
        cargarUsuarios();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado en puerto 12345");

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
                if (partes.length == 2) usuarios.put(partes[0], partes[1]);
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
            boolean conexionActiva = true;
            try {
                entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
                salida = new PrintWriter(clienteSocket.getOutputStream(), true);

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
                    guardarUsuario(usuario, password);
                    salida.println("REGISTRO_EXITOSO");
                    salida.println("MENSAJE_SERVIDOR:Usuario registrado correctamente.");
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
                if (usuarios.isEmpty()) {
                    lista = "";
                } else {
                    lista = String.join(",", usuarios.keySet());
                }
            }
            salida.println("USUARIOS_REGISTRADOS:" + lista);
        }

        private void manejarLogout() {
            usuarioActivo = null;
            salida.println("LOGOUT_EXITOSO");
        }
    }
}
