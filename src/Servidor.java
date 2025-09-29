import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

public class Servidor {
    private static Map<String, String> usuarios = new ConcurrentHashMap<>();
    private static Map<String, List<String>> bandejas = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> usuariosBloqueados = new ConcurrentHashMap<>();
    private static Set<String> usuariosBloqueadosGlobal = ConcurrentHashMap.newKeySet();
    private static Map<String, List<ArchivoTransferencia>> archivosCompartidos = new ConcurrentHashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final String ARCHIVO_BLOQUEADOS = "bloqueados.txt";
    private static final String ARCHIVO_BLOQUEADOS_GLOBAL = "bloqueados_global.txt";
    private static final String DIRECTORIO_MENSAJES = "mensajes/";
    private static final String DIRECTORIO_ARCHIVOS = "archivos_compartidos/";
    private static final int PUERTO = 12345;
    private static Map<String, Integer> paginaActual = new ConcurrentHashMap<>();

    public static class ArchivoTransferencia {
        public String nombre;
        public String remitente;
        public byte[] contenido;
        public long tamano;

        public ArchivoTransferencia(String nombre, String remitente, byte[] contenido) {
            this.nombre = nombre;
            this.remitente = remitente;
            this.contenido = contenido;
            this.tamano = contenido.length;
        }
    }

    public static void main(String[] args) {
        cargarUsuarios();
        cargarBloqueados();
        cargarBloqueadosGlobal();
        crearDirectorioMensajes();
        crearDirectorioArchivos();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);

            Thread adminThread = new Thread(() -> {
                Scanner sc = new Scanner(System.in);
                while (true) {
                    System.out.println("\n=== Consola del servidor ===");
                    System.out.println("1. Enviar mensaje a usuario");
                    System.out.println("2. Expulsar usuario");
                    System.out.println("3. Ver usuarios registrados");
                    System.out.println("4. Bloquear usuario globalmente");
                    System.out.println("5. Desbloquear usuario globalmente");
                    System.out.println("6. Ver usuarios bloqueados globalmente");
                    System.out.println("7. Ver archivos compartidos");
                    System.out.println("8. Salir");
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
                            System.out.print("Usuario a bloquear globalmente: ");
                            String usuarioBloquearGlobal = sc.nextLine().trim();
                            if (!usuarios.containsKey(usuarioBloquearGlobal)) {
                                System.out.println("El usuario no existe.");
                                continue;
                            }
                            if (usuariosBloqueadosGlobal.contains(usuarioBloquearGlobal)) {
                                System.out.println("El usuario ya esta bloqueado globalmente.");
                            } else {
                                usuariosBloqueadosGlobal.add(usuarioBloquearGlobal);
                                guardarBloqueadosGlobal();
                                System.out.println("Usuario " + usuarioBloquearGlobal + " bloqueado globalmente.");
                            }
                            break;

                        case "5":
                            System.out.print("Usuario a desbloquear globalmente: ");
                            String usuarioDesbloquearGlobal = sc.nextLine().trim();
                            if (usuariosBloqueadosGlobal.remove(usuarioDesbloquearGlobal)) {
                                guardarBloqueadosGlobal();
                                System.out.println("Usuario " + usuarioDesbloquearGlobal + " desbloqueado globalmente.");
                            } else {
                                System.out.println("El usuario no estaba bloqueado globalmente.");
                            }
                            break;

                        case "6":
                            System.out.println("Usuarios bloqueados globalmente: " + usuariosBloqueadosGlobal);
                            break;

                        case "7":
                            System.out.println("=== Archivos compartidos ===");
                            for (Map.Entry<String, List<ArchivoTransferencia>> entry : archivosCompartidos.entrySet()) {
                                System.out.println("Usuario: " + entry.getKey());
                                for (ArchivoTransferencia archivo : entry.getValue()) {
                                    System.out.println("  - " + archivo.nombre + " (de: " + archivo.remitente +
                                            ", tamaño: " + archivo.tamano + " bytes)");
                                }
                            }
                            break;

                        case "8":
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
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creando archivo usuarios: " + e.getMessage());
            }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                    bandejas.putIfAbsent(partes[0], new ArrayList<>());
                    usuariosBloqueados.putIfAbsent(partes[0], new HashSet<>());
                    archivosCompartidos.putIfAbsent(partes[0], new ArrayList<>());
                    cargarMensajesUsuario(partes[0]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo usuarios: " + e.getMessage());
        }
    }

    private static void cargarBloqueados() {
        File f = new File(ARCHIVO_BLOQUEADOS);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creando archivo bloqueados: " + e.getMessage());
            }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length >= 2) {
                    String usuario = partes[0];
                    usuariosBloqueados.putIfAbsent(usuario, new HashSet<>());
                    for (int i = 1; i < partes.length; i++) {
                        if (!partes[i].isEmpty()) {
                            usuariosBloqueados.get(usuario).add(partes[i]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo bloqueados: " + e.getMessage());
        }
    }

    private static void cargarBloqueadosGlobal() {
        File f = new File(ARCHIVO_BLOQUEADOS_GLOBAL);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.err.println("Error creando archivo bloqueados global: " + e.getMessage());
            }
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    usuariosBloqueadosGlobal.add(linea.trim());
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo archivo bloqueados global: " + e.getMessage());
        }
    }

    private static void guardarBloqueados() {
        try (FileWriter writer = new FileWriter(ARCHIVO_BLOQUEADOS, false)) {
            for (Map.Entry<String, Set<String>> entry : usuariosBloqueados.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    writer.write(entry.getKey() + ":" + String.join(":", entry.getValue()) + "\n");
                }
            }
        } catch (IOException e) {
            System.err.println("Error guardando bloqueados: " + e.getMessage());
        }
    }

    private static void guardarBloqueadosGlobal() {
        try (FileWriter writer = new FileWriter(ARCHIVO_BLOQUEADOS_GLOBAL, false)) {
            for (String usuario : usuariosBloqueadosGlobal) {
                writer.write(usuario + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error guardando bloqueados global: " + e.getMessage());
        }
    }

    private static void crearDirectorioMensajes() {
        File dir = new File(DIRECTORIO_MENSAJES);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static void crearDirectorioArchivos() {
        File dir = new File(DIRECTORIO_ARCHIVOS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static void cargarMensajesUsuario(String usuario) {
        File archivoMensajes = new File(DIRECTORIO_MENSAJES + usuario + ".txt");
        if (!archivoMensajes.exists()) {
            return;
        }

        List<String> mensajes = bandejas.get(usuario);
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoMensajes))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                mensajes.add(linea);
            }
        } catch (IOException e) {
            System.err.println("Error cargando mensajes para " + usuario + ": " + e.getMessage());
        }
    }

    private static void guardarMensajesUsuario(String usuario) {
        File archivoMensajes = new File(DIRECTORIO_MENSAJES + usuario + ".txt");
        List<String> mensajes = bandejas.get(usuario);

        if (mensajes == null || mensajes.isEmpty()) {
            if (archivoMensajes.exists()) {
                archivoMensajes.delete();
            }
            return;
        }

        try (FileWriter writer = new FileWriter(archivoMensajes, false)) {
            for (String mensaje : mensajes) {
                writer.write(mensaje + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error guardando mensajes para " + usuario + ": " + e.getMessage());
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
            usuariosBloqueados.remove(usuario);
            usuariosBloqueadosGlobal.remove(usuario);
            archivosCompartidos.remove(usuario);

            File archivoMensajes = new File(DIRECTORIO_MENSAJES + usuario + ".txt");
            if (archivoMensajes.exists()) {
                archivoMensajes.delete();
            }

            reescribirArchivoUsuarios();
            guardarBloqueados();
            guardarBloqueadosGlobal();

            for (Set<String> bloqueados : usuariosBloqueados.values()) {
                bloqueados.remove(usuario);
            }
            guardarBloqueados();

            System.out.println("Usuario " + usuario + " ha sido expulsado del sistema.");
            return true;
        }
        return false;
    }

    private static synchronized void enviarMensaje(String usuario, String mensaje) {
        bandejas.putIfAbsent(usuario, new ArrayList<>());
        bandejas.get(usuario).add(mensaje);
        guardarMensajesUsuario(usuario);
    }

    private static synchronized void enviarArchivo(String usuario, ArchivoTransferencia archivo) {
        archivosCompartidos.putIfAbsent(usuario, new ArrayList<>());
        archivosCompartidos.get(usuario).add(archivo);
    }

    private static class ManejadorCliente implements Runnable {
        private Socket clienteSocket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private DataInputStream dataInput;
        private DataOutputStream dataOutput;
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
                dataInput = new DataInputStream(clienteSocket.getInputStream());
                dataOutput = new DataOutputStream(clienteSocket.getOutputStream());

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
                        case "SIGUIENTE":
                            manejarSiguiente();
                            break;
                        case "ANTERIOR":
                            manejarAnterior();
                            break;
                        case "SALIR_BANDEJA":
                            manejarSalirBandeja();
                            break;
                        case "ELIMINAR_MENSAJES":
                            manejarEliminarMensajes();
                            break;
                        case "ENVIAR_MENSAJE":
                            manejarEnviarMensaje();
                            break;
                        case "BLOQUEAR_USUARIO":
                            manejarBloquearUsuario();
                            break;
                        case "DESBLOQUEAR_USUARIO":
                            manejarDesbloquearUsuario();
                            break;
                        case "VER_BLOQUEADOS":
                            manejarVerBloqueados();
                            break;
                        case "LISTAR_ARCHIVOS":
                            manejarListarArchivos();
                            break;
                        case "ENVIAR_ARCHIVO":
                            manejarEnviarArchivo();
                            break;
                        case "VER_ARCHIVOS_RECIBIDOS":
                            manejarVerArchivosRecibidos();
                            break;
                        case "DESCARGAR_ARCHIVO":
                            manejarDescargarArchivo();
                            break;
                        case "LOGOUT":
                            manejarLogout();
                            break;
                        case "ADIVINA_NUMERO":
                            jugarAdivinaNumero();
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
                    usuariosBloqueados.putIfAbsent(usuario, new HashSet<>());
                    archivosCompartidos.putIfAbsent(usuario, new ArrayList<>());
                    guardarUsuario(usuario, password);
                    cargarMensajesUsuario(usuario);
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
                if (usuariosBloqueadosGlobal.contains(usuario)) {
                    salida.println("USUARIO_BLOQUEADO");
                } else if (usuarios.containsKey(usuario) && usuarios.get(usuario).equals(password)) {
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
            usuariosDisponibles.remove(usuarioActivo);

            Set<String> bloqueadosPorMi = usuariosBloqueados.getOrDefault(usuarioActivo, new HashSet<>());
            usuariosDisponibles.removeAll(bloqueadosPorMi);

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
                return;
            }

            int pagina = paginaActual.getOrDefault(usuarioActivo, 1);
            int mensajesPorPagina = 5;
            int totalMensajes = mensajes.size();
            int totalPaginas = (int) Math.ceil((double) totalMensajes / mensajesPorPagina);

            if (pagina < 1) pagina = 1;
            if (pagina > totalPaginas) pagina = totalPaginas;
            paginaActual.put(usuarioActivo, pagina);

            int inicio = (pagina - 1) * mensajesPorPagina;
            int fin = Math.min(inicio + mensajesPorPagina, totalMensajes);

            salida.println("BANDEJA_PAGINADA");
            salida.println("PAGINA:" + pagina + "/" + totalPaginas);

            for (int i = inicio; i < fin; i++) {
                salida.println((i + 1) + ". " + mensajes.get(i));
            }

            salida.println("FIN_PAGINA");
        }

        private void manejarSiguiente() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }
            int cur = paginaActual.getOrDefault(usuarioActivo, 1);
            paginaActual.put(usuarioActivo, cur + 1);
            manejarBandeja();
        }

        private void manejarAnterior() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }
            int cur = paginaActual.getOrDefault(usuarioActivo, 1);
            paginaActual.put(usuarioActivo, cur - 1);
            manejarBandeja();
        }

        private void manejarSalirBandeja() {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }
            paginaActual.remove(usuarioActivo);
            salida.println("SALISTE_BANDEJA");
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
                guardarMensajesUsuario(usuarioActivo);
                salida.println("TODOS_ELIMINADOS");
            } else {
                try {
                    int indice = Integer.parseInt(respuesta) - 1;
                    if (indice >= 0 && indice < mensajes.size()) {
                        mensajes.remove(indice);
                        guardarMensajesUsuario(usuarioActivo);
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

            Set<String> bloqueadosPorMi = usuariosBloqueados.getOrDefault(usuarioActivo, new HashSet<>());
            if (bloqueadosPorMi.contains(destinatario)) {
                salida.println("USUARIO_BLOQUEADO");
                return;
            }

            Set<String> bloqueadosPorEl = usuariosBloqueados.getOrDefault(destinatario, new HashSet<>());
            if (bloqueadosPorEl.contains(usuarioActivo)) {
                salida.println("USUARIO_TE_BLOCO");
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

        private void manejarBloquearUsuario() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            salida.println("SOLICITAR_USUARIO_BLOQUEAR");
            String usuarioBloquear = entrada.readLine();

            if (usuarioBloquear == null || usuarioBloquear.trim().isEmpty()) {
                salida.println("USUARIO_NO_EXISTE");
                return;
            }

            if (usuarioBloquear.equals(usuarioActivo)) {
                salida.println("NO_PUEDES_BLOQUEARTE");
                return;
            }

            if (!usuarios.containsKey(usuarioBloquear)) {
                salida.println("USUARIO_NO_EXISTE");
                return;
            }

            Set<String> bloqueados = usuariosBloqueados.get(usuarioActivo);
            if (bloqueados == null) {
                bloqueados = new HashSet<>();
                usuariosBloqueados.put(usuarioActivo, bloqueados);
            }

            if (bloqueados.contains(usuarioBloquear)) {
                salida.println("USUARIO_YA_BLOQUEADO");
                return;
            }

            bloqueados.add(usuarioBloquear);
            guardarBloqueados();
            salida.println("USUARIO_BLOQUEADO_EXITOSO");
        }

        private void manejarDesbloquearUsuario() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            salida.println("SOLICITAR_USUARIO_DESBLOQUEAR");
            String usuarioDesbloquear = entrada.readLine();

            if (usuarioDesbloquear == null || usuarioDesbloquear.trim().isEmpty()) {
                salida.println("USUARIO_NO_BLOQUEADO");
                return;
            }

            Set<String> bloqueados = usuariosBloqueados.get(usuarioActivo);
            if (bloqueados != null && bloqueados.remove(usuarioDesbloquear)) {
                guardarBloqueados();
                salida.println("USUARIO_DESBLOQUEADO_EXITOSO");
            } else {
                salida.println("USUARIO_NO_BLOQUEADO");
            }
        }

        private void manejarVerBloqueados() {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            Set<String> bloqueados = usuariosBloqueados.getOrDefault(usuarioActivo, new HashSet<>());
            salida.println("BLOQUEADOS:" + String.join(",", bloqueados));
        }

        private void manejarListarArchivos() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            salida.println("SOLICITAR_DIRECTORIO");
            String directorio = entrada.readLine();

            if (directorio == null || directorio.trim().isEmpty()) {
                directorio = System.getProperty("user.dir");
            }

            try {
                File dir = new File(directorio);
                if (!dir.exists() || !dir.isDirectory()) {
                    salida.println("DIRECTORIO_INVALIDO");
                    return;
                }

                File[] archivos = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));

                if (archivos == null || archivos.length == 0) {
                    salida.println("NO_HAY_ARCHIVOS_TXT");
                } else {
                    salida.println("ARCHIVOS_ENCONTRADOS");
                    for (File archivo : archivos) {
                        salida.println(archivo.getName() + ":" + archivo.length());
                    }
                    salida.println("FIN_LISTA_ARCHIVOS");
                }
            } catch (Exception e) {
                salida.println("ERROR_LISTANDO_ARCHIVOS");
            }
        }

        private void manejarEnviarArchivo() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            salida.println("SOLICITAR_DESTINATARIO_ARCHIVO");
            String destinatario = entrada.readLine();

            if (destinatario == null || destinatario.trim().isEmpty()) {
                salida.println("DESTINATARIO_INVALIDO");
                return;
            }

            if (!usuarios.containsKey(destinatario)) {
                salida.println("DESTINATARIO_NO_EXISTE");
                return;
            }

            if (destinatario.equals(usuarioActivo)) {
                salida.println("NO_PUEDES_ENVIARTE_ARCHIVO");
                return;
            }

            Set<String> bloqueadosPorMi = usuariosBloqueados.getOrDefault(usuarioActivo, new HashSet<>());
            if (bloqueadosPorMi.contains(destinatario)) {
                salida.println("USUARIO_BLOQUEADO");
                return;
            }

            Set<String> bloqueadosPorEl = usuariosBloqueados.getOrDefault(destinatario, new HashSet<>());
            if (bloqueadosPorEl.contains(usuarioActivo)) {
                salida.println("USUARIO_TE_BLOCO");
                return;
            }

            salida.println("SOLICITAR_NOMBRE_ARCHIVO");
            String nombreArchivo = entrada.readLine();

            if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                salida.println("NOMBRE_ARCHIVO_INVALIDO");
                return;
            }

            try {
                int tam = dataInput.readInt();
                if (tam <= 0 || tam > 10 * 1024 * 1024) {
                    salida.println("TAMAÑO_ARCHIVO_INVALIDO");
                    return;
                }

                byte[] contenido = new byte[tam];
                dataInput.readFully(contenido);
                ArchivoTransferencia archivo = new ArchivoTransferencia(nombreArchivo, usuarioActivo, contenido);
                enviarArchivo(destinatario, archivo);
                enviarMensaje(destinatario, "[Archivo recibido de " + usuarioActivo + "]: " + nombreArchivo +
                        " (tamaño: " + tam + " bytes)");

                salida.println("ARCHIVO_ENVIADO");

            } catch (IOException e) {
                salida.println("ERROR_RECIBIENDO_ARCHIVO");
                System.err.println("Error recibiendo archivo: " + e.getMessage());
            }
        }

        private void manejarVerArchivosRecibidos() {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            List<ArchivoTransferencia> archivos = archivosCompartidos.getOrDefault(usuarioActivo, new ArrayList<>());

            if (archivos.isEmpty()) {
                salida.println("NO_HAY_ARCHIVOS_RECIBIDOS");
            } else {
                salida.println("ARCHIVOS_RECIBIDOS");
                for (int i = 0; i < archivos.size(); i++) {
                    ArchivoTransferencia archivo = archivos.get(i);
                    salida.println((i + 1) + ". " + archivo.nombre + " (de: " + archivo.remitente +
                            ", tamaño: " + archivo.tamano + " bytes)");
                }
                salida.println("FIN_ARCHIVOS_RECIBIDOS");
            }
        }

        private void manejarDescargarArchivo() throws IOException {
            if (usuarioActivo == null) {
                salida.println("ERROR:NO_AUTENTICADO");
                return;
            }

            List<ArchivoTransferencia> archivos = archivosCompartidos.getOrDefault(usuarioActivo, new ArrayList<>());

            if (archivos.isEmpty()) {
                salida.println("NO_HAY_ARCHIVOS_PARA_DESCARGAR");
                return;
            }

            salida.println("SOLICITAR_INDICE_ARCHIVO");
            String indiceStr = entrada.readLine();

            try {
                int indice = Integer.parseInt(indiceStr) - 1;

                if (indice < 0 || indice >= archivos.size()) {
                    salida.println("INDICE_ARCHIVO_INVALIDO");
                    return;
                }

                ArchivoTransferencia archivo = archivos.get(indice);

                salida.println("ARCHIVO_DISPONIBLE");
                salida.println(archivo.nombre);
                salida.println(String.valueOf(archivo.tamano));

                dataOutput.writeInt((int) archivo.contenido.length);
                dataOutput.write(archivo.contenido);
                dataOutput.flush();

                System.out.println("Archivo " + archivo.nombre + " enviado a " + usuarioActivo);

            } catch (NumberFormatException e) {
                salida.println("INDICE_FORMATO_INVALIDO");
            } catch (IOException e) {
                salida.println("ERROR_ENVIANDO_ARCHIVO");
                System.err.println("Error enviando archivo: " + e.getMessage());
            }
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
