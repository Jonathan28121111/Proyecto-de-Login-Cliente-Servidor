import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
    private static Map<String, String> usuarios = new HashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";

    public static void main(String[] args) {
        cargarUsuarios();

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Servidor iniciado en puerto 12345");

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(() -> manejarCliente(clienteSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static void manejarCliente(Socket clienteSocket) {
        String usuarioActivo = null;

        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));
             PrintWriter salida = new PrintWriter(clienteSocket.getOutputStream(), true)) {

            String comando = entrada.readLine();

            if ("REGISTRO".equals(comando)) {
                salida.println("SOLICITAR_DATOS_REGISTRO");
                String usuario = entrada.readLine();
                String password = entrada.readLine();

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
            else if ("LOGIN".equals(comando)) {
                salida.println("SOLICITAR_DATOS_LOGIN");
                String usuario = entrada.readLine();
                String password = entrada.readLine();

                synchronized (usuarios) {
                    if (usuarios.containsKey(usuario) && usuarios.get(usuario).equals(password)) {
                        usuarioActivo = usuario;
                        salida.println("LOGIN_EXITOSO");
                        System.out.println("Login exitoso: " + usuario);
                    } else {
                        salida.println("CREDENCIALES_INVALIDAS");
                        System.out.println("Login fallido: " + usuario);
                    }
                }
            } 
            else if ("VER_PERFIL".equals(comando)) {
                if (usuarioActivo != null) {
                    salida.println("PERFIL:" + usuarioActivo);
                } else {
                    salida.println("ERROR:NO_AUTENTICADO");
                }
            } 
            else if ("LOGOUT".equals(comando)) {
                usuarioActivo = null;
                salida.println("LOGOUT_EXITOSO");
            }

            clienteSocket.close();

        } catch (IOException e) {
            System.err.println("Error manejando cliente: " + e.getMessage());
        }
    }

    private static void cargarUsuarios() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) usuarios.put(partes[0], partes[1]);
            }
        } catch (IOException e) {
            System.out.println("Archivo de usuarios no encontrado, se creará uno nuevo al registrar.");
        }
    }

    private static void guardarUsuario(String usuario, String password) {
        try (FileWriter writer = new FileWriter(ARCHIVO_USUARIOS, true)) {
            writer.write(usuario + ":" + password + "\n");
        } catch (IOException e) {
            System.err.println("Error guardando usuario: " + e.getMessage());
        }
    }
}
