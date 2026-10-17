package com.graphics; // Reúne las mejoras opcionales con las cuatro lecciones anteriores.

import java.util.ArrayList; // Permite guardar la lista de vehículos autónomos.
import java.util.List; // Define el tipo de la lista de tráfico.
import static org.lwjgl.glfw.GLFW.*; // Incluye las teclas, el título de la ventana y el control del cursor.
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL33.*; // Permite activar la mezcla de colores y desactivar la profundidad en la interfaz.

/**
 * CLASE 5: MEJORAS OPCIONALES — TRÁFICO AUTÓNOMO, INTERFAZ Y SECTORES.
 * Orden de lectura: estado, controles, actualización del tráfico, cámara en primera persona, dibujo 3D, interfaz 2D y utilidades.
 * Hereda clase4, que contiene la ciudad ampliada, la conducción, la iluminación, las entregas y el minimapa.
 * La interfaz usa el modo uMapa = 2 del shader: rectángulos en píxeles, sin perspectiva ni iluminación.
 */
public class clase5 extends clase4 {

    // ==================== 1. ESTADO: TRÁFICO, MENÚ Y SECTORES ====================
    protected final List<VehiculoAutonomo> trafico = new ArrayList<>(); // Vehículos que circulan solos.
    protected boolean enMenu = true; // El juego comienza en el menú de inicio.
    protected boolean pausado = false; // La tecla P detiene la partida.
    protected boolean mostrarAyuda = false; // La tecla H muestra la ayuda de controles.

    protected static final int SEGUIMIENTO = 0, PRIMERA_PERSONA = 1, AEREA = 2; // Las tres vistas que alterna la tecla C.
    private static final String[] NOMBRES_CAMARA = {"SEGUIMIENTO", "PRIMERA PERSONA", "AEREA"}; // Texto del panel.
    private static final float SENSIBILIDAD = 0.0025f; // Radianes que gira la cabeza por cada píxel que se mueve el ratón.
    protected int modoCamara = SEGUIMIENTO; // Vista actual.
    protected float mirarGiro = 0; // Giro de la cabeza respecto del frente del auto (primera persona).
    protected float mirarInclinacion = 0; // Cabeza hacia arriba (+) o hacia abajo (-).
    private boolean ratonCapturado = false; // El cursor está oculto y controla la mirada.
    private boolean ratonNuevo = true; // La próxima lectura del ratón solo sirve de referencia.
    private double ratonX, ratonY; // Última posición leída del cursor.
    private float recorrido = 0; // Distancia recorrida: produce el leve vaivén de la cámara en primera persona.

    /** Rutas del tráfico: pares (columna, fila) de cruces del mapa recorridos en circuito cerrado. */
   protected static final int[][] RUTAS = {
        {0, 0, 10, 0},// 10, 10, 0, 10}, // Taxi: vuelta exterior por las calles nuevas del borde.
        {2, 2, 2, 8, 8, 8, 8, 2}, // Segundo vehículo: anillo alrededor del centro.
        {4, 0, 4, 6, 10, 6, 10, 0}, // Tercer vehículo: sectores norte y este.
        {0, 4, 6, 4, 6, 10, 0, 10}, // Cuarto vehículo: sectores oeste y sur.
        {0, 0, 0, 10, 10, 10, 10, 0}, // Quinto vehículo: vuelta exterior en sentido contrario al taxi.
        {8, 2, 8, 8, 2, 8, 2, 2}, // Sexto vehículo: anillo central en sentido contrario.
        {6, 0, 6, 4, 2, 4, 2, 0}, // Séptimo vehículo: circuito del sector norte.
        {4, 4, 4, 10, 10, 10, 10, 4} // Octavo vehículo: sectores sur y este.
    };
    private static final float[][] COLORES_TRAFICO = { // Color de cada vehículo autónomo.
        {1.0f, 0.78f, 0.10f}, // Taxi amarillo.
        {0.20f, 0.70f, 0.35f}, // Verde.
        {0.90f, 0.90f, 0.92f}, // Blanco.
        {0.55f, 0.30f, 0.80f}, // Morado.
        {0.15f, 0.45f, 0.95f}, // Azul.
        {0.95f, 0.50f, 0.10f}, // Naranja.
        {0.10f, 0.75f, 0.80f}, // Turquesa.
        {0.85f, 0.20f, 0.45f} // Rosado.
    };
    private static final float[] CRUCEROS = {9.5f, 7, 9, 7.5f, 8, 7.5f, 6.5f, 8.5f}; // Velocidad de crucero de cada vehículo.

    protected static final String[] SECTORES = {"CENTRO", "NORTE", "SUR", "ESTE", "OESTE"}; // Nombres de los sectores.
    private static final float[][] COLORES_SECTOR = { // Color reconocible de cada sector.
        {0.95f, 0.55f, 0.15f}, // Centro: naranja.
        {0.25f, 0.60f, 0.95f}, // Norte: azul.
        {0.30f, 0.80f, 0.40f}, // Sur: verde.
        {0.90f, 0.35f, 0.55f}, // Este: rosado.
        {0.65f, 0.50f, 0.95f} // Oeste: violeta.
    };
    private static final int[][] LETREROS = {{5, 5}, {1, 5}, {9, 5}, {5, 9}, {5, 1}}; // Manzana (fila, columna) del letrero de cada sector.

    /** Letrero de un negocio: manzana, fachada (sur o este) y nombre. */
    protected record Negocio(int fila, int columna, boolean caraEste, String nombre) {
    }

    /** Negocios por altura del edificio (filas de ventanas, ver clase1.PISOS). */
    protected static final String[] COMERCIOS = {"TIENDA", "PANADERIA", "PIZZERIA", "FARMACIA", "MERCADO", "GIMNASIO", "CAFE", "ESCUELA"}; // 2 o 3 ventanas.
    protected static final String[] SERVICIOS = {"BANCO", "CINE", "POLICIA", "MUSEO", "UNIVERSIDAD"}; // 4 o 5 ventanas.
    protected static final String[] EDIFICIOS_ALTOS = {"HOSPITAL", "OFICINAS", "APARTAMENTOS"}; // 6 ventanas.
    protected static final List<Negocio> NEGOCIOS = crearNegocios(); // Letreros de toda la ciudad.

    /** Cantidad de filas de ventanas de un edificio; usa el mismo ciclo que clase4.dibujarVentanas(). */
    protected static int filasDeVentanas(int fila, int columna) {
        int filas = 0; // Cuenta los pisos con ventanas.
        for (float y = 1.7f; y < alturaEdificio(fila, columna); y += 2) { // Mismo recorrido que las ventanas.
            filas++; // Un piso más.
        }
        return filas; // Total de filas de ventanas.
    }

    /**
     * Reparte los negocios por altura:
     * 8 edificios de 2-3 ventanas: un comercio cada uno;
     * 4 de 4-5: banco, cine, policía y universidad (con su museo en la fachada este);
     * 4 de 6: hospital, oficinas y apartamentos; 3 de 7-8: hoteles.
     */
    private static List<Negocio> crearNegocios() {
        List<Negocio> negocios = new ArrayList<>(); // Lista resultante.
        negocios.add(negocio(1, 1, "TIENDA")); // 3 ventanas.
        negocios.add(negocio(1, 3, "PANADERIA")); // 2 ventanas.
        negocios.add(negocio(1, 9, "PIZZERIA")); // 3 ventanas.
        negocios.add(negocio(7, 1, "FARMACIA")); // 2 ventanas.
        negocios.add(negocio(7, 9, "MERCADO")); // 3 ventanas.
        negocios.add(negocio(9, 1, "GIMNASIO")); // 2 ventanas, junto a la salida del jugador.
        negocios.add(negocio(9, 7, "CAFE")); // 3 ventanas.
        negocios.add(negocio(9, 9, "ESCUELA")); // 2 ventanas.
        negocios.add(negocio(1, 7, "UNIVERSIDAD")); // 5 ventanas.
        negocios.add(new Negocio(1, 7, true, "MUSEO")); // Museo universitario en la fachada este del mismo edificio.
        negocios.add(negocio(5, 1, "POLICIA")); // 4 ventanas.
        negocios.add(negocio(5, 9, "BANCO")); // 5 ventanas.
        negocios.add(negocio(9, 5, "CINE")); // 4 ventanas.
        negocios.add(negocio(3, 3, "HOSPITAL")); // 6 ventanas.
        negocios.add(negocio(3, 7, "OFICINAS")); // 6 ventanas.
        negocios.add(negocio(7, 3, "APARTAMENTOS")); // 6 ventanas.
        negocios.add(negocio(7, 7, "OFICINAS")); // 6 ventanas.
        negocios.add(negocio(3, 5, "HOTEL")); // 8 ventanas.
        negocios.add(negocio(5, 5, "HOTEL")); // 7 ventanas.
        negocios.add(negocio(7, 5, "HOTEL")); // 8 ventanas.
        return negocios; // Todos los letreros de negocios.
    }

    /** Crea el letrero en la fachada sur, o en la este si la acera sur ya tiene el letrero del sector. */
    private static Negocio negocio(int fila, int columna, String nombre) {
        boolean tieneSector = false; // Indica si su acera sur tiene el letrero del sector.
        for (int[] letrero : LETREROS) { // Revisa los letreros de sector.
            tieneSector |= letrero[0] == fila && letrero[1] == columna; // Misma manzana.
        }
        return new Negocio(fila, columna, tieneSector, nombre); // Fachada este si la sur está ocupada.
    }

    /** Color del panel según el tipo de negocio. */
    private static float[] colorNegocio(String nombre) {
        switch (nombre) { // Cada negocio tiene un color reconocible.
            case "HOSPITAL": return new float[] {0.80f, 0.08f, 0.10f}; // Rojo.
            case "HOTEL": return new float[] {0.55f, 0.15f, 0.45f}; // Morado.
            case "BANCO": return new float[] {0.10f, 0.25f, 0.55f}; // Azul.
            case "CINE": return new float[] {0.60f, 0.10f, 0.10f}; // Granate.
            case "POLICIA": return new float[] {0.08f, 0.15f, 0.50f}; // Azul oscuro.
            case "FARMACIA": return new float[] {0.05f, 0.50f, 0.30f}; // Verde.
            case "PANADERIA": return new float[] {0.55f, 0.35f, 0.10f}; // Marrón.
            case "PIZZERIA": return new float[] {0.50f, 0.12f, 0.12f}; // Rojo oscuro.
            case "MERCADO": return new float[] {0.60f, 0.35f, 0.05f}; // Naranja.
            case "GIMNASIO": return new float[] {0.30f, 0.30f, 0.35f}; // Gris.
            case "CAFE": return new float[] {0.45f, 0.28f, 0.12f}; // Café.
            case "ESCUELA": return new float[] {0.15f, 0.35f, 0.60f}; // Azul claro.
            case "MUSEO": return new float[] {0.35f, 0.30f, 0.15f}; // Ocre.
            case "UNIVERSIDAD": return new float[] {0.30f, 0.10f, 0.45f}; // Violeta.
            case "OFICINAS": return new float[] {0.20f, 0.30f, 0.40f}; // Gris azulado.
            case "APARTAMENTOS": return new float[] {0.40f, 0.25f, 0.20f}; // Ladrillo.
            default: return new float[] {0.10f, 0.45f, 0.20f}; // Tienda: verde oscuro.
        }
    }

//===============AQUI SE DECLARAN LOS PEATONES (SOPORTA 1, 2, 3 O MAS)==========
    protected Peaton peaton; // Mantiene compatibilidad con el primer peatón y con las pruebas.
    protected List<Peaton> peatones = new ArrayList<>(); // Lista para múltiples personas autónomas.
//======
    protected float avionX; // Posición X del avión; vuela de este (+X) a oeste (-X).
    protected float avionZ; // Calle imaginaria (Z) por la que pasa en esta vuelta.
    private int pasadasAvion = 0; // Cuenta las pasadas para variar la trayectoria.
    private float tiempoAvion = 0; // Segundos desde que empezó la pasada actual.
    private static final float PERIODO_AVION = 50; // Pasa un avión cada 30 segundos.
    private static final float[] CARRILES_AVION = {-20, 15, -45, 40}; // Z de cada pasada sucesiva.
    private static final float ALTURA_AVION = 30; // Altura de vuelo, por encima de las torres más altas.
    private static final float VELOCIDAD_AVION = 14; // Unidades por segundo.

    /** Crea el estado inicial, incluidos el tráfico, el peatón y el avión. */
    public clase5() {
        crearTrafico(); // Coloca los vehículos autónomos en el inicio de sus rutas.
        crearPeatonYAvion(); // Coloca al peatón y al avión en su punto de partida.
    }

//===============AQUI SE CREAN E INICIALIZAN LOS PEATONES==========
    protected void crearPeatonYAvion() {
        peatones.clear(); // Limpia la lista al iniciar o reiniciar.

        // PEATÓN 1 (Activo actualmente): Manzana suroeste, acceso oeste: visible desde la salida.
        peaton = new Peaton(MAPA.length - 2, 1, 7, 7);
        peatones.add(peaton);

        // --- EN EL FUTURO: SOLO DESCOMENTA O AGREGA AQUI LAS PERSONAS QUE QUIERAS ---
        // peatones.add(new Peaton(1, 1, 1, 15));               // Peatón 2: Manzana noroeste
        // peatones.add(new Peaton(1, MAPA.length - 2, 3, 27)); // Peatón 3: Manzana noreste
        // peatones.add(new Peaton(MAPA.length - 2, MAPA.length - 2, 5, 33)); // Peatón 4: Manzana sureste
        // peatones.add(new Peaton(3, 3, 0, 42));               // Peatón 5: Manzana central

        pasadasAvion = 0; // Primera pasada.
        tiempoAvion = 0; // La primera pasada empieza de inmediato.
        avionX = LIMITE + 40; // Empieza fuera del mapa, al este.
        avionZ = CARRILES_AVION[0]; // Primera trayectoria.
    }
//======

    /** Coloca cada vehículo autónomo al inicio de su ruta. */
    protected void crearTrafico() {
        trafico.clear(); // Elimina los vehículos de una partida anterior.
        for (int indice = 0; indice < RUTAS.length; indice++) { // Crea un vehículo por ruta.
            int[] ruta = RUTAS[indice]; // Lee los cruces de esta ruta.
            float[][] cruces = new float[ruta.length / 2][]; // Reserva una posición por cruce.
            for (int punto = 0; punto < cruces.length; punto++) { // Convierte índices del mapa a coordenadas.
                float x = centro(ruta[punto * 2]); // Columna del cruce convertida a X.
                float z = centro(ruta[punto * 2 + 1]); // Fila del cruce convertida a Z.
                cruces[punto] = new float[] {x, z}; // Guarda el cruce.
            }
            float[] color = COLORES_TRAFICO[indice]; // Color de este vehículo.
            trafico.add(new VehiculoAutonomo(cruces, CRUCEROS[indice], color[0], color[1], color[2])); // Lo añade a la ciudad.
        }
    }

    /** Devuelve el índice del sector que contiene un punto: el centro es la ciudad original; el resto, la ampliación. */
    protected static int sector(float x, float z) {
        if (Math.max(Math.abs(x), Math.abs(z)) <= 25) { // Las 3 x 3 manzanas centrales forman el sector original.
            return 0; // Centro.
        }
        if (Math.abs(z) >= Math.abs(x)) { // El punto está más lejos en profundidad que a los lados.
            return z < 0 ? 1 : 2; // Norte si Z es negativa; sur en caso contrario.
        }
        return x > 0 ? 3 : 4; // Este si X es positiva; oeste en caso contrario.
    }

    // ==================== 2. CONTROLES: MENÚ, PAUSA Y AYUDA ====================

    /** Añade ENTER, P, H, la tercera vista de C y V a los controles anteriores. */
    @Override // Amplía las teclas de clase4.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C, R, N, F y M.
        if ((key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) && enMenu) { // ENTER inicia el recorrido.
            enMenu = false; // Cierra el menú de inicio.
        }
        if (key == GLFW_KEY_P && !enMenu) { // P solo pausa una partida ya iniciada.
            pausado = !pausado; // Alterna entre pausa y juego.
        }
        if (key == GLFW_KEY_H) { // H muestra u oculta la ayuda.
            mostrarAyuda = !mostrarAyuda; // Alterna el panel de controles.
        }
        if (key == GLFW_KEY_C) { // clase2 solo conoce dos vistas; aquí se agrega la primera persona.
            modoCamara = (modoCamara + 1) % 3; // Seguimiento -> primera persona -> aérea -> seguimiento.
            camaraAerea = modoCamara == AEREA; // clase2 dibuja la vista aérea solo en ese modo.
            centrarMirada(); // Cada vista empieza mirando al frente.
        }
        if (key == GLFW_KEY_V) { // V vuelve a mirar al frente del auto.
            centrarMirada(); // Quita el giro de la cabeza.
        }
    }

    /** Coloca la cabeza mirando al frente del auto. */
    private void centrarMirada() {
        mirarGiro = 0; // Sin giro lateral.
        mirarInclinacion = 0; // Sin inclinación.
    }

    /** Reinicia también el tráfico y quita la pausa. */
    @Override // Amplía el reinicio de clase4.
    protected void reiniciar() {
        super.reiniciar(); // Restaura auto, entregas y cronómetro.
        crearTrafico(); // Devuelve el tráfico a sus posiciones iniciales.
        crearPeatonYAvion(); // Devuelve peatón y avión a su inicio.
        pausado = false; // Comienza el nuevo recorrido inmediatamente.
    }

    // ==================== 3. ACTUALIZACIÓN: PAUSA Y TRÁFICO ====================

    /** Detiene todo en el menú o en pausa; en otro caso actualiza jugador, entregas y tráfico. */
    @Override // Amplía la actualización de clase4.
    protected void actualizar(float deltaTime) {
        actualizarRaton(); // Captura o libera el cursor y gira la cabeza en primera persona.
        if (enMenu || pausado) { // En el menú y en pausa no avanza el tiempo.
            String estado = enMenu ? "MENU: ENTER para comenzar" : "PAUSA: P para continuar"; // Texto del estado.
            glfwSetWindowTitle(ventana, "clase5 | " + estado + " | H: ayuda"); // Mantiene informado el título.
            return; // No mueve al jugador ni al tráfico.
        }
        super.actualizar(deltaTime); // Conduce el auto, comprueba entregas y actualiza el título.
        recorrido += Math.abs(velocidad) * deltaTime; // Distancia recorrida para el vaivén de la cámara.
        actualizarTrafico(deltaTime); // Mueve los vehículos autónomos.
//===============AQUI SE ACTUALIZAN TODOS LOS PEATONES AUTONOMOS==========
        for (Peaton p : peatones) {
            p.actualizar(deltaTime, this::cicloSemaforo, this::autoEn); // Cada peatón camina, respeta semáforos y autos.
        }
//======
        actualizarAvion(deltaTime); // Mueve el avión.
    }

    /** En primera persona el ratón mueve la cabeza, como en Minecraft; fuera de ella el cursor queda libre. */
    private void actualizarRaton() {
        boolean capturar = modoCamara == PRIMERA_PERSONA && !enMenu && !pausado; // Solo se mira con el ratón al jugar en primera persona.
        if (capturar != ratonCapturado) { // Cambió la situación desde el cuadro anterior.
            glfwSetInputMode(ventana, GLFW_CURSOR, capturar ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL); // Oculta y fija el cursor, o lo devuelve.
            ratonCapturado = capturar; // Recuerda el estado.
            ratonNuevo = true; // Evita un salto con la primera lectura.
        }
        if (!capturar) { // Sin captura no hay mirada libre.
            return; // Nada más que hacer.
        }
        double[] x = new double[1]; // Recibe la posición X del cursor.
        double[] y = new double[1]; // Recibe la posición Y del cursor.
        glfwGetCursorPos(ventana, x, y); // Lee el cursor virtual (sin límites mientras está capturado).
        if (!ratonNuevo) { // Ya hay una lectura anterior con la cual comparar.
            mirarGiro -= (float) (x[0] - ratonX) * SENSIBILIDAD; // Ratón a la derecha: la cabeza gira a la derecha.
            mirarInclinacion -= (float) (y[0] - ratonY) * SENSIBILIDAD; // Ratón hacia abajo: mira hacia abajo.
            mirarGiro = Math.max(-1.9f, Math.min(1.9f, mirarGiro)); // No gira la cabeza más de unos 110 grados.
            mirarInclinacion = Math.max(-1.1f, Math.min(1.1f, mirarInclinacion)); // Ni la levanta o baja más de 63 grados.
        }
        ratonX = x[0]; // Guarda la lectura para el próximo cuadro.
        ratonY = y[0]; // Guarda la lectura para el próximo cuadro.
        ratonNuevo = false; // Las próximas lecturas ya producen giro.
    }

    /** Cada 30 s un avión cruza la ciudad de este a oeste (tarda unos 14 s) y la siguiente pasada usa otra trayectoria. */
    protected void actualizarAvion(float deltaTime) {
        tiempoAvion += deltaTime; // Avanza el tiempo de la pasada.
        if (tiempoAvion >= PERIODO_AVION) { // Ya pasaron 30 s desde que empezó la pasada anterior.
            tiempoAvion -= PERIODO_AVION; // Comienza una pasada nueva.
            pasadasAvion++; // Cuenta la pasada.
            avionZ = CARRILES_AVION[pasadasAvion % CARRILES_AVION.length]; // Cambia de trayectoria.
        }
        avionX = LIMITE + 40 - VELOCIDAD_AVION * tiempoAvion; // Parte fuera del mapa por el este y avanza hacia el oeste (-X).
    }

    /** Decide qué vehículos deben detenerse y luego los mueve a todos. */
    protected void actualizarTrafico(float deltaTime) {
        int cantidad = trafico.size(); // Número de vehículos.
        boolean[] detener = new boolean[cantidad]; // Decisión de cada vehículo para este cuadro.
        for (int i = 0; i < cantidad; i++) { // Revisa cada vehículo.
            VehiculoAutonomo vehiculo = trafico.get(i); // Vehículo que decide.
            if (vehiculo.tieneDelante(autoX, autoZ, 7, 2.2f)) { // El jugador está delante en su carril.
                detener[i] = true; // Cede el paso al jugador.
            }
            if (debeFrenarPorSemaforo(vehiculo)) { // El próximo cruce tiene el semáforo en rojo (o amarillo con tiempo para frenar).
                detener[i] = true; // Se detiene antes del paso peatonal.
            }
//===============AQUI EL TRAFICO DETECTA SI ALGUN PEATON CRUZA DELANTE==========
            if (peatonDelante(vehiculo)) { // Cede el paso si cualquiera de los peatones está cruzando delante.
                detener[i] = true; // Le cede el paso.
            }
//======
            for (int j = 0; j < cantidad; j++) { // Compara con los demás vehículos.
                VehiculoAutonomo otro = trafico.get(j); // Posible obstáculo.
                if (i == j || !vehiculo.tieneDelante(otro.x, otro.z, 6, 2.2f)) { // Ignora a sí mismo y a los que no tiene delante.
                    continue; // Pasa al siguiente.
                }
                boolean mutuo = otro.tieneDelante(vehiculo.x, vehiculo.z, 6, 2.2f); // Ambos se ven de frente en un cruce.
                if (!mutuo || j < i) { // Si se bloquean mutuamente, pasa primero el de menor índice.
                    detener[i] = true; // Espera a que el otro avance.
                }
            }
        }
        for (int i = 0; i < cantidad; i++) { // Aplica las decisiones.
            trafico.get(i).actualizar(deltaTime, detener[i]); // Avanza o frena cada vehículo.
        }
    }

    /**
     * Cada manzana controla el cruce de su esquina noreste, con una luz para la calle horizontal y otra para la vertical.
     * El vehículo mira la luz de la calle por la que circula y frena con rojo, o con amarilla si todavía está lejos.
     */
    protected boolean debeFrenarPorSemaforo(VehiculoAutonomo vehiculo) {
        for (int fila = 1; fila < MAPA.length; fila += 2) { // Recorre las filas de manzanas.
            for (int columna = 1; columna < MAPA[fila].length; columna += 2) { // Recorre las columnas de manzanas.
                if (MAPA[fila][columna] == 0) { // Solo las manzanas tienen semáforo.
                    continue; // Pasa a la siguiente.
                }
                boolean vertical = Math.abs(vehiculo.frenteZ()) > Math.abs(vehiculo.frenteX()); // Circula de norte a sur o de sur a norte.
                int fase = faseSemaforo(fila, columna, vertical); // Estado de la luz que corresponde a su calle.
                if (fase == VERDE) { // Con verde se puede pasar.
                    continue; // No hace falta medir la distancia.
                }
                float cruceX = centro(columna + 1) - vehiculo.x; // Vector hacia el centro del cruce en X.
                float cruceZ = centro(fila - 1) - vehiculo.z; // Vector hacia el centro del cruce en Z.
                float adelante = cruceX * vehiculo.frenteX() + cruceZ * vehiculo.frenteZ(); // Distancia a lo largo de la marcha.
                float lateral = Math.abs(cruceX * vehiculo.frenteZ() - cruceZ * vehiculo.frenteX()); // Separación lateral.
                if (lateral > 4) { // El cruce no está sobre la calle por la que circula.
                    continue; // Lo ignora.
                }
                if (fase == ROJO && adelante > 8.2f && adelante < 13) { // Rojo antes de la línea de detención.
                    return true; // Debe detenerse.
                }
                if (fase == AMARILLO && adelante > 11 && adelante < 13) { // Amarillo con distancia suficiente.
                    return true; // Prefiere detenerse.
                }
            }
        }
        return false; // Ningún semáforo le obliga a frenar.
    }

//===============AQUI SE REVISA SI CUALQUIER PEATON ESTA DELANTE DE UN AUTO==========
    /** Indica si algún peatón de la ciudad está delante del morro del vehículo (no a su costado). */
    private boolean peatonDelante(VehiculoAutonomo vehiculo) {
        for (Peaton p : peatones) {
            float dx = p.x - vehiculo.x; // Vector hacia el peatón en X.
            float dz = p.z - vehiculo.z; // Vector hacia el peatón en Z.
            float adelante = dx * vehiculo.frenteX() + dz * vehiculo.frenteZ(); // Distancia a lo largo de la marcha.
            float lateral = Math.abs(dx * vehiculo.frenteZ() - dz * vehiculo.frenteX()); // Separación lateral.
            if (adelante > 1.8f && adelante < 7 && lateral < 2.5f) { // Delante del morro.
                return true; // Encontró al menos un peatón cruzando delante.
            }
        }
        return false;
    }
//======

    /** Indica si un punto cae sobre algún auto (jugador o tráfico), con medio peatón de margen. */
    protected boolean autoEn(float x, float z) {
        if (dentroDeAuto(autoX, autoZ, angulo, x, z)) { // Auto del jugador.
            return true; // Ocupado.
        }
        for (VehiculoAutonomo vehiculo : trafico) { // Autos del tráfico.
            if (dentroDeAuto(vehiculo.x, vehiculo.z, vehiculo.angulo, x, z)) { // El punto cae sobre este auto.
                return true; // Ocupado.
            }
        }
        return false; // El paso está libre.
    }

    /** Comprueba un punto contra el rectángulo girado de un auto (1.65 x 2.6) más 0.35 de margen. */
    private static boolean dentroDeAuto(float autoX, float autoZ, float giro, float x, float z) {
        float dx = x - autoX; // Vector desde el centro del auto en X.
        float dz = z - autoZ; // Vector desde el centro del auto en Z.
        float frenteX = -(float) Math.sin(giro); // Frente del auto en X.
        float frenteZ = -(float) Math.cos(giro); // Frente del auto en Z.
        float largo = Math.abs(dx * frenteX + dz * frenteZ); // Distancia a lo largo del auto.
        float ancho = Math.abs(dx * frenteZ - dz * frenteX); // Distancia a lo ancho.
        return largo < 1.3f + 0.35f && ancho < 0.83f + 0.35f; // Dentro de la carrocería ampliada.
    }

    /** El jugador tampoco puede atravesar los vehículos autónomos. */
    @Override // Amplía las colisiones de clase2 con el tráfico.
    protected boolean puedeCircular(float x, float z) {
        if (!super.puedeCircular(x, z)) { // Primero comprueba manzanas y bordes.
            return false; // Rechaza la posición.
        }
        float[] auto = contornoAuto(x, z, angulo); // Rectángulo real del jugador en la posición propuesta.
        for (VehiculoAutonomo vehiculo : trafico) { // Revisa cada vehículo autónomo.
            if (!seTocan(auto, contornoAuto(vehiculo.x, vehiculo.z, vehiculo.angulo))) { // Las carrocerías no se tocan.
                continue; // Este vehículo no estorba.
            }
            float nueva = (float) Math.hypot(x - vehiculo.x, z - vehiculo.z); // Distancia en la posición propuesta.
            float actual = (float) Math.hypot(autoX - vehiculo.x, autoZ - vehiculo.z); // Distancia en la posición actual.
            if (nueva < actual) { // Solo bloquea si el movimiento acerca ambos autos; separarse siempre se permite.
                return false; // El choque detiene al jugador, como con las manzanas.
            }
        }
//===============AQUI SE VERIFICA LA COLISION DEL JUGADOR CON CUALQUIER PEATON==========
        for (Peaton p : peatones) {
            if (!p.sobreAcera()) { // Sobre la acera el auto nunca lo alcanza: solo importa si está en la calle.
                float m = 0.35f; // Medio ancho del peatón.
                float[] persona = {p.x - m, p.z - m, p.x + m, p.z - m, p.x + m, p.z + m, p.x - m, p.z + m}; // Cuadrado del peatón.
                float nueva = (float) Math.hypot(x - p.x, z - p.z); // Distancia en la posición propuesta.
                float actual = (float) Math.hypot(autoX - p.x, autoZ - p.z); // Distancia actual.
                if (seTocan(auto, persona) && nueva < actual) { // El movimiento lo atropellaría.
                    return false; // El auto se detiene.
                }
            }
        }
//======
        return true; // No hay obstáculos.
    }

    // ==================== 4. CÁMARA EN PRIMERA PERSONA Y CABINA ====================

    /** Seguimiento y aérea vienen de clase2; la primera persona coloca el ojo en el asiento del conductor. */
    @Override // Amplía la cámara de clase2.
    protected void configurarCamara() {
        if (modoCamara != PRIMERA_PERSONA) { // Las otras dos vistas ya existen.
            super.configurarCamara(); // Seguimiento o aérea.
            return; // No hace falta más.
        }
        float seno = (float) Math.sin(angulo); // Orientación del auto.
        float coseno = (float) Math.cos(angulo); // Orientación del auto.
        float vaiven = (float) Math.sin(recorrido * 1.3f) * 0.03f * Math.min(1, Math.abs(velocidad) / 6); // Leve balanceo al avanzar.
        float ojoX = autoX + seno * 0.2f; // Asiento: 0.2 detrás del centro del auto.
        float ojoY = 1.4f + vaiven; // Altura de los ojos, por encima del capó.
        float ojoZ = autoZ + coseno * 0.2f; // Asiento: 0.2 detrás del centro del auto.
        float giro = angulo + mirarGiro; // Hacia dónde mira la cabeza.
        float inclinacion = mirarInclinacion - 0.08f; // Ligeramente hacia abajo para ver el capó.
        float horizontal = (float) Math.cos(inclinacion); // Parte horizontal de la mirada.
        float frenteX = -(float) Math.sin(giro) * horizontal; // Dirección de la mirada en X.
        float frenteY = (float) Math.sin(inclinacion); // Dirección de la mirada en Y.
        float frenteZ = -(float) Math.cos(giro) * horizontal; // Dirección de la mirada en Z.
        vector("uOjo", ojoX, ojoY, ojoZ); // Posición de la cámara.
        vector("uObjetivo", ojoX + frenteX, ojoY + frenteY, ojoZ + frenteZ); // Punto un paso delante de los ojos.
        decimal("uAspecto", (float) ancho / alto); // Proporción de la ventana.
        decimal("uCampo", 70); // Campo visual más amplio, como el de Minecraft.
    }

    /** En primera persona se dibuja la cabina vista desde dentro; en las otras vistas, el auto completo. */
    @Override // Amplía el dibujo del auto de clase2.
    protected void dibujarAuto() {
        if (modoCamara != PRIMERA_PERSONA || vistaMapa) { // Vista exterior o minimapa.
            super.dibujarAuto(); // Auto completo.
            return; // No dibuja la cabina interior.
        }
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Carrocería roja: desde el asiento se ve el capó.
        for (float lado = -0.55f; lado <= 0.55f; lado += 1.1f) { // Dos faros.
            pieza(lado, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Faro delantero.
        }
        pieza(0, 0.95f, -0.8f, 1.55f, 0.1f, 0.3f, 0.10f, 0.11f, 0.13f); // Tablero oscuro.
        for (float lado = -0.78f; lado <= 0.78f; lado += 1.56f) { // Pilares izquierdo y derecho del parabrisas.
            pieza(lado, 1.38f, -0.9f, 0.07f, 0.76f, 0.07f, 0.22f, 0.65f, 0.78f); // Pilar del color de la cabina.
        }
        pieza(0, 1.8f, 0.1f, 1.62f, 0.08f, 2.0f, 0.22f, 0.65f, 0.78f); // Techo: borde superior de la vista.
        float volante = 0; // Giro del volante según las teclas.
        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Gira a la izquierda.
            volante += 1; // Volante a la izquierda.
        }
        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Gira a la derecha.
            volante -= 1; // Volante a la derecha.
        }
        float oscuro = 0.07f; // Color del volante.
        pieza(0, 1.13f, -0.6f, 0.36f, 0.05f, 0.05f, oscuro, oscuro, oscuro); // Volante cuadrado: lado superior.
        pieza(0, 0.83f, -0.6f, 0.36f, 0.05f, 0.05f, oscuro, oscuro, oscuro); // Lado inferior.
        pieza(-0.155f, 0.98f, -0.6f, 0.05f, 0.3f, 0.05f, oscuro, oscuro, oscuro); // Lado izquierdo.
        pieza(0.155f, 0.98f, -0.6f, 0.05f, 0.3f, 0.05f, oscuro, oscuro, oscuro); // Lado derecho.
        for (int brazo = -1; brazo <= 1; brazo += 2) { // Brazo izquierdo (-1) y derecho (+1), cuadrados como en Minecraft.
            float alturaMano = 0.98f + brazo * volante * 0.06f; // Al girar, una mano sube y la otra baja.
            pieza(brazo * 0.17f, alturaMano, -0.4f, 0.1f, 0.1f, 0.42f, 0.80f, 0.60f, 0.45f); // Antebrazo y mano color piel.
            pieza(brazo * 0.19f, alturaMano - 0.015f, -0.1f, 0.12f, 0.12f, 0.2f, 0.0f, 0.66f, 0.66f); // Manga turquesa.
        }
    }

    // ==================== 5. DIBUJO 3D: TRÁFICO Y LETREROS ====================

    /** Añade el tráfico y los letreros de los sectores a la escena. */
    @Override // Amplía el dibujo de clase4.
    protected void escena() {
        super.escena(); // Dibuja ciudad, auto, farolas, decoración y destino.
        for (VehiculoAutonomo vehiculo : trafico) { // Dibuja cada vehículo autónomo.
            dibujarVehiculo(vehiculo); // Modelo 3D o marca del minimapa.
        }
        dibujarPeaton(); // Peatón en la acera o cruzando.
        dibujarAvion(); // Avión blanco en el cielo.
        if (!vistaMapa) { // Los letreros son pequeños para el minimapa.
            for (int indice = 0; indice < SECTORES.length; indice++) { // Un letrero por sector.
                float x = centro(LETREROS[indice][1]); // Centro X de la manzana elegida.
                float z = centro(LETREROS[indice][0]); // Centro Z de la manzana elegida.
                dibujarLetrero(SECTORES[indice], x, z + 4.2f, COLORES_SECTOR[indice]); // Lo coloca en la acera sur.
            }
            for (Negocio negocio : NEGOCIOS) { // Letreros de negocios en las fachadas.
                dibujarNegocio(negocio); // Panel con el nombre del negocio.
            }
        }
    }

    /** Coloca el letrero de un negocio entre la primera y la segunda fila de ventanas de la fachada sur o este. */
    private void dibujarNegocio(Negocio negocio) {
        float cx = centro(negocio.columna()); // Centro X del edificio.
        float cz = centro(negocio.fila()); // Centro Z del edificio.
        float nx = negocio.caraEste() ? 1 : 0; // Normal de la fachada en X (hacia la calle).
        float nz = negocio.caraEste() ? 0 : 1; // Normal de la fachada en Z.
        float rx = nz; // Derecha de quien mira la fachada desde la calle, en X.
        float rz = -nx; // Derecha de quien mira la fachada, en Z (en la fachada este apunta al norte).
        String nombre = negocio.nombre(); // Texto del letrero.
        float[] color = colorNegocio(nombre); // Color del panel.
        float pixel = Math.min(0.11f, 6.4f / Fuente.ancho(nombre)); // Letras más pequeñas si el nombre es largo (fachada de 7).
        float anchoTexto = Fuente.ancho(nombre) * pixel; // Ancho del texto en el mundo.
        float anchoPanel = anchoTexto + 0.5f; // Ancho del panel con margen.
        if (noche) { // De noche los letreros se ven encendidos.
            entero("uEmision", 1); // Conservan su color.
        }
        float panelX = cx + nx * 3.57f; // Panel pegado a la fachada (el edificio mide 7 x 7).
        float panelZ = cz + nz * 3.57f; // Panel pegado a la fachada.
        caja(panelX, 2.7f, panelZ, Math.abs(rx) * anchoPanel + nx * 0.06f, 1.0f, Math.abs(rz) * anchoPanel + nz * 0.06f, color[0], color[1], color[2]); // Panel.
        float textoX = cx + nx * 3.62f; // Texto delante del panel.
        float textoZ = cz + nz * 3.62f; // Texto delante del panel.
        float arriba = 2.7f + Fuente.FILAS * pixel / 2; // Borde superior del texto.
        Fuente.recorrer(nombre, (fila, columna, largo) -> { // Cada tramo de píxeles es una caja blanca.
            float d = -anchoTexto / 2 + (columna + largo / 2f) * pixel; // Distancia del tramo al centro, hacia la derecha.
            float largoMundo = largo * pixel; // Longitud del tramo.
            caja(textoX + rx * d, arriba - (fila + 0.5f) * pixel, textoZ + rz * d,
                 Math.abs(rx) * largoMundo + nx * 0.04f, pixel, Math.abs(rz) * largoMundo + nz * 0.04f, 1, 1, 1); // Tramo de letra.
        });
        if (nombre.equals("HOSPITAL")) { // El hospital lleva además una cruz roja en la azotea.
            float techo = alturaEdificio(negocio.fila(), negocio.columna()) + 0.65f; // Encima de la cubierta.
            caja(cx, techo, cz, 3.2f, 0.12f, 1.0f, 0.9f, 0.1f, 0.1f); // Brazo horizontal de la cruz.
            caja(cx, techo, cz, 1.0f, 0.12f, 3.2f, 0.9f, 0.1f, 0.1f); // Brazo vertical de la cruz.
        }
        entero("uEmision", 0); // Restablece la iluminación normal.
    }

//===============AQUI SE DIBUJAN TODOS LOS PEATONES DE LA CIUDAD==========
    /** Dibuja a todos los peatones: piernas que se alternan al caminar, cuerpo, brazos y cabeza. */
    private void dibujarPeaton() {
        // Paleta de colores para las camisetas (así cada persona luce diferente al agregar más)
        float[][] coloresCamiseta = {
            {0.90f, 0.35f, 0.25f}, // Persona 1: Rojo / naranja original
            {0.20f, 0.60f, 0.90f}, // Persona 2: Celeste
            {0.30f, 0.80f, 0.35f}, // Persona 3: Verde
            {0.95f, 0.80f, 0.20f}, // Persona 4: Amarillo
            {0.70f, 0.35f, 0.85f}  // Persona 5: Morado
        };

        for (int i = 0; i < peatones.size(); i++) {
            Peaton p = peatones.get(i);
            if (p == null) continue;

            if (vistaMapa) { // En el minimapa basta un punto rosado.
                caja(p.x, 24.5f, p.z, 1.8f, 0.1f, 1.8f, 1, 0.55f, 0.75f); // Marca del peatón.
                continue; // Pasa al siguiente peatón.
            }

            float base = p.sobreAcera() ? 0.3f : 0; // Pisa la acera elevada o el asfalto.
            boolean quieto = p.esperando || p.detenido; // Esperando el semáforo o un auto no mueve las piernas.
            float balanceo = quieto ? 0 : (float) Math.sin(p.pasos * 4) * 0.22f; // Avance alterno de las piernas.
            float[] color = coloresCamiseta[i % coloresCamiseta.length];

            piezaEn(p.x, p.z, p.angulo, -0.13f, base + 0.4f, balanceo, 0.18f, 0.8f, 0.2f, 0.15f, 0.17f, 0.30f); // Pierna izquierda.
            piezaEn(p.x, p.z, p.angulo, 0.13f, base + 0.4f, -balanceo, 0.18f, 0.8f, 0.2f, 0.15f, 0.17f, 0.30f); // Pierna derecha.
            piezaEn(p.x, p.z, p.angulo, 0, base + 1.1f, 0, 0.5f, 0.65f, 0.3f, color[0], color[1], color[2]); // Camiseta distintiva.
            piezaEn(p.x, p.z, p.angulo, -0.32f, base + 1.05f, -balanceo, 0.13f, 0.6f, 0.15f, color[0], color[1], color[2]); // Brazo izquierdo.
            piezaEn(p.x, p.z, p.angulo, 0.32f, base + 1.05f, balanceo, 0.13f, 0.6f, 0.15f, color[0], color[1], color[2]); // Brazo derecho.
            piezaEn(p.x, p.z, p.angulo, 0, base + 1.62f, 0, 0.32f, 0.34f, 0.32f, 0.92f, 0.75f, 0.60f); // Cabeza.
            piezaEn(p.x, p.z, p.angulo, 0, base + 1.82f, 0.02f, 0.34f, 0.08f, 0.34f, 0.25f, 0.15f, 0.08f); // Cabello.
        }
    }
//======

    /** Dibuja un avión blanco pequeño con la nariz hacia el oeste (-X). */
    private void dibujarAvion() {
        float x = avionX; // Posición X actual.
        float y = ALTURA_AVION; // Altura de vuelo.
        float z = avionZ; // Trayectoria actual.
        if (x < -LIMITE - 40) { // Ya salió por el oeste: espera la próxima pasada.
            return; // No se dibuja.
        }
        if (vistaMapa) { // En el minimapa: silueta blanca encima de todo.
            caja(x, 27, z, 5, 0.1f, 1.2f, 1, 1, 1); // Fuselaje.
            caja(x + 0.3f, 27, z, 1.4f, 0.1f, 6, 1, 1, 1); // Alas.
            caja(x + 2.2f, 27, z, 0.8f, 0.1f, 2.4f, 1, 1, 1); // Cola.
            return; // No dibuja detalles.
        }
        caja(x, y, z, 5, 0.8f, 0.8f, 0.95f, 0.95f, 0.97f); // Fuselaje blanco.
        caja(x - 2.8f, y - 0.05f, z, 0.7f, 0.6f, 0.6f, 0.95f, 0.95f, 0.97f); // Nariz.
        caja(x - 2.2f, y + 0.25f, z, 0.6f, 0.25f, 0.82f, 0.15f, 0.25f, 0.40f); // Cabina oscura.
        caja(x + 0.3f, y - 0.1f, z, 1.4f, 0.15f, 7, 0.90f, 0.90f, 0.93f); // Alas.
        caja(x + 2.2f, y + 0.75f, z, 0.9f, 1.2f, 0.15f, 0.90f, 0.90f, 0.93f); // Timón vertical.
        caja(x + 2.3f, y + 0.1f, z, 0.7f, 0.12f, 2.6f, 0.90f, 0.90f, 0.93f); // Estabilizador horizontal.
        if (!noche) { // De día proyecta una sombra sobre el asfalto.
            caja(x, 0.075f, z, 5, 0.01f, 0.8f, 0.09f, 0.10f, 0.12f); // Sombra del fuselaje.
            caja(x + 0.3f, 0.075f, z, 1.4f, 0.01f, 7, 0.09f, 0.10f, 0.12f); // Sombra de las alas.
        }
    }

    /** Dibuja un vehículo autónomo con las mismas proporciones que el auto del jugador. */
    private void dibujarVehiculo(VehiculoAutonomo v) {
        if (vistaMapa) { // En el minimapa basta una marca orientada.
            cajaGirada(v.x, 24, v.z, 2.8f, 0.1f, 4.0f, v.rojo, v.verde, v.azul, v.angulo); // Rectángulo del color del auto.
            return; // No dibuja las piezas pequeñas.
        }
        piezaEn(v.x, v.z, v.angulo, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, v.rojo, v.verde, v.azul); // Carrocería.
        piezaEn(v.x, v.z, v.angulo, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.20f, 0.28f, 0.35f); // Cabina oscura.
        for (float lado = -0.88f; lado <= 0.88f; lado += 1.76f) { // Lados izquierdo y derecho.
            for (float eje = -0.82f; eje <= 0.82f; eje += 1.64f) { // Ejes delantero y trasero.
                piezaEn(v.x, v.z, v.angulo, lado, 0.38f, eje, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Rueda.
            }
        }
        if (noche) { // De noche los faros del tráfico se ven encendidos.
            entero("uEmision", 1); // Las luces no se oscurecen.
        }
        for (float lado = -0.55f; lado <= 0.55f; lado += 1.1f) { // Dos luces por extremo.
            piezaEn(v.x, v.z, v.angulo, lado, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Faro delantero.
            piezaEn(v.x, v.z, v.angulo, lado, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Luz trasera.
        }
        entero("uEmision", 0); // Restablece la iluminación normal.
    }

    /** Dibuja un letrero con el nombre del sector, legible desde ambos lados de la calle. */
    private void dibujarLetrero(String nombre, float x, float z, float[] color) {
        float pixel = 0.13f; // Tamaño en el mundo de cada píxel de la fuente.
        float anchoPanel = Fuente.ancho(nombre) * pixel + 0.8f; // Ancho del texto más un margen.
        float altoPanel = Fuente.FILAS * pixel + 0.5f; // Alto del texto más un margen.
        float yPanel = 3.2f; // Altura del centro del panel.
        float postes = anchoPanel / 2 - 0.2f; // Distancia de cada poste al centro.
        caja(x - postes, 1.6f, z, 0.12f, 2.6f, 0.12f, 0.25f, 0.27f, 0.30f); // Poste izquierdo sobre la acera.
        caja(x + postes, 1.6f, z, 0.12f, 2.6f, 0.12f, 0.25f, 0.27f, 0.30f); // Poste derecho.
        if (noche) { // De noche el letrero se ve iluminado.
            entero("uEmision", 1); // El panel conserva su color.
        }
        caja(x, yPanel, z, anchoPanel, altoPanel, 0.1f, color[0], color[1], color[2]); // Panel de color del sector.
        texto3D(nombre, x, yPanel, z + 0.08f, pixel, 1); // Texto de la cara sur.
        texto3D(nombre, x, yPanel, z - 0.08f, pixel, -1); // Texto de la cara norte, escrito en sentido contrario.
        entero("uEmision", 0); // Restablece la iluminación normal.
    }

    /** Escribe texto blanco en el mundo; sentido = 1 avanza hacia +X y -1 hacia -X. */
    private void texto3D(String texto, float centroX, float centroY, float z, float pixel, int sentido) {
        float inicio = centroX - sentido * Fuente.ancho(texto) * pixel / 2; // Borde donde empieza la primera letra.
        float arriba = centroY + Fuente.FILAS * pixel / 2; // Borde superior del texto.
        Fuente.recorrer(texto, (fila, columna, largo) -> { // Recibe cada tramo de píxeles encendidos.
            float x = inicio + sentido * (columna + largo / 2f) * pixel; // Centro horizontal del tramo.
            float y = arriba - (fila + 0.5f) * pixel; // Centro vertical de la fila.
            caja(x, y, z, largo * pixel, pixel, 0.04f, 1, 1, 1); // Dibuja el tramo como una caja delgada.
        });
    }

    // ==================== 6. INTERFAZ 2D DENTRO DE LA VENTANA ====================

    /** Dibuja escena y minimapa y, al final, la interfaz encima de todo. */
    @Override // Amplía el cuadro de clase4.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Escena principal y minimapa.
        glDisable(GL_DEPTH_TEST); // La interfaz siempre queda delante.
        glEnable(GL_BLEND); // Permite paneles semitransparentes.
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Mezcla según la opacidad del panel.
        entero("uMapa", 2); // Selecciona el modo de interfaz del shader.
        vector("uPantalla", ancho, alto, 0); // Envía el tamaño actual de la ventana.
        try { // Restaura el estado aunque falle el dibujo.
            int escala = Math.max(2, Math.round(Math.min(ancho, alto) / 320f)); // Tamaño del texto según la ventana.
            dibujarPanelEstado(escala); // Velocidad, luces, entregas y sector.
            dibujarRotulosMapa(); // Nombres de sectores y norte sobre el minimapa.
            if (mostrarAyuda) { // La ayuda se muestra solo si se pidió.
                dibujarAyuda(escala); // Panel de controles.
            }
            if (enMenu) { // El menú tiene prioridad.
                dibujarMenu(escala); // Pantalla de inicio.
            } else if (pausado) { // Después la pausa.
                dibujarPausa(escala); // Aviso de pausa.
            } else if (entregas == DESTINOS.length) { // Finalmente el recorrido completado.
                dibujarVictoria(escala); // Mensaje de victoria.
            }
        } finally { // La próxima imagen debe empezar con el estado 3D normal.
            entero("uMapa", 0); // Vuelve a la proyección en perspectiva.
            glDisable(GL_BLEND); // Desactiva la mezcla.
            glEnable(GL_DEPTH_TEST); // Reactiva la profundidad.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba errores de la interfaz.
            throw new IllegalStateException("Error OpenGL en la interfaz"); // Los muestra en la consola.
        }
    }

    /** Panel inferior izquierdo con la información de la partida. */
    private void dibujarPanelEstado(int e) {
        int kmh = Math.round(Math.abs(velocidad) * 3.6f); // Velocidad en km/h, como en el título.
        int indiceSector = sector(autoX, autoZ); // Sector donde está el auto.
        float[] colorSector = COLORES_SECTOR[indiceSector]; // Color de ese sector.
        String[] etiquetas = {"VELOCIDAD: ", "AMBIENTE: ", "FAROS: ", "CAMARA: ", "ENTREGAS: ", "SECTOR: "}; // Primera parte de cada línea.
        String[] valores = { // Segunda parte de cada línea.
            kmh + " KM/H", // Velocidad.
            noche ? "NOCHE (N)" : "DIA (N)", // Ambiente.
            faros ? "ENCENDIDOS (F)" : "APAGADOS (F)", // Faros.
            NOMBRES_CAMARA[modoCamara] + " (C)", // Cámara.
            entregas + "/" + DESTINOS.length + "   " + (int) tiempo + " S", // Entregas y tiempo.
            SECTORES[indiceSector] // Sector actual.
        };
        float[][] colores = { // Color de cada valor.
            {1, 1, 1}, // Velocidad en blanco.
            noche ? new float[] {0.55f, 0.70f, 1} : new float[] {1, 0.85f, 0.30f}, // Azul de noche, amarillo de día.
            faros ? new float[] {1, 0.95f, 0.60f} : new float[] {0.6f, 0.6f, 0.6f}, // Claro si están encendidos.
            {0.75f, 0.90f, 1}, // Cámara.
            {1, 0.75f, 0.15f}, // Entregas en dorado, como la marca.
            colorSector // Sector con su color propio.
        };
        int linea = 11 * e; // Separación vertical entre líneas.
        int margen = 6 * e; // Espacio interior del panel.
        int anchoMaximo = Fuente.ancho("H: AYUDA   P: PAUSA"); // Ancho mínimo: la última línea.
        for (int i = 0; i < etiquetas.length; i++) { // Busca la línea más ancha.
            anchoMaximo = Math.max(anchoMaximo, Fuente.ancho(etiquetas[i] + valores[i])); // Conserva el máximo.
        }
        float anchoPanel = anchoMaximo * e + 2 * margen; // Ancho total del panel.
        float altoPanel = (etiquetas.length + 1) * linea + 4 * e + 2 * margen; // Líneas, barra y márgenes.
        float x = 12; // Separación del borde izquierdo.
        float y = alto - altoPanel - 12; // Separación del borde inferior.
        rectangulo(x, y, anchoPanel, altoPanel, 0.03f, 0.05f, 0.09f, 0.72f); // Fondo oscuro semitransparente.
        float cursorY = y + margen; // Posición de la primera línea.
        for (int i = 0; i < etiquetas.length; i++) { // Escribe cada línea.
            float finEtiqueta = texto(etiquetas[i], x + margen, cursorY, e, 0.70f, 0.75f, 0.80f); // Etiqueta gris.
            texto(valores[i], finEtiqueta, cursorY, e, colores[i][0], colores[i][1], colores[i][2]); // Valor en color.
            cursorY += linea; // Baja a la siguiente línea.
            if (i == 0) { // Después de la velocidad dibuja la barra.
                float anchoBarra = anchoPanel - 2 * margen; // Ancho disponible.
                float fraccion = Math.min(1, Math.abs(velocidad) / 16); // Proporción de la velocidad máxima.
                rectangulo(x + margen, cursorY - 3 * e, anchoBarra, 2 * e, 0.2f, 0.22f, 0.26f, 1); // Fondo de la barra.
                rectangulo(x + margen, cursorY - 3 * e, anchoBarra * fraccion, 2 * e, 0.3f + fraccion * 0.7f, 0.9f - fraccion * 0.6f, 0.3f, 1); // Verde a rojo.
                cursorY += 4 * e; // Deja espacio debajo de la barra.
            }
        }
        rectangulo(x + anchoPanel - margen - 7 * e, cursorY - linea, 7 * e, 7 * e, colorSector[0], colorSector[1], colorSector[2], 1); // Cuadro con el color del sector.
        texto("H: AYUDA   P: PAUSA", x + margen, cursorY, e, 0.55f, 0.60f, 0.65f); // Recordatorio de teclas.
    }

    /** Escribe el norte y los nombres de los sectores encima del minimapa. */
    private void dibujarRotulosMapa() {
        if (mapaLado <= 0) { // El minimapa está oculto.
            return; // No hay dónde escribir.
        }
        int e = Math.max(1, mapaLado / 250); // Texto pequeño, proporcional al minimapa.
        float[][] posiciones = {{0, -12}, {0, -46}, {0, 46}, {46, 0}, {-46, 0}}; // Punto del mundo donde va cada nombre.
        for (int i = 0; i < SECTORES.length; i++) { // Escribe cada sector.
            float px = mapaX + (posiciones[i][0] / (LIMITE + 2) * 0.5f + 0.5f) * mapaLado; // X del mundo al píxel.
            float pyAbajo = mapaY + (-posiciones[i][1] / (LIMITE + 2) * 0.5f + 0.5f) * mapaLado; // Z del mundo al píxel (desde abajo).
            float py = alto - pyAbajo; // Convierte a píxeles medidos desde arriba.
            float izquierda = px - Fuente.ancho(SECTORES[i]) * e / 2f; // Centra el texto.
            float arriba = py - Fuente.FILAS * e / 2f; // Centra verticalmente.
            texto(SECTORES[i], izquierda + 1, arriba + 1, e, 0, 0, 0); // Sombra negra para que se lea sobre los edificios.
            float[] color = COLORES_SECTOR[i]; // Color del sector.
            texto(SECTORES[i], izquierda, arriba, e, color[0], color[1], color[2]); // Nombre del sector.
        }
        float autoPx = mapaX + (autoX / (LIMITE + 2) * 0.5f + 0.5f) * mapaLado; // Posición del jugador en píxeles (X).
        float autoPy = alto - (mapaY + (-autoZ / (LIMITE + 2) * 0.5f + 0.5f) * mapaLado); // Posición del jugador (Y desde arriba).
        int eYo = e + 1; // La etiqueta del jugador es algo mayor que los rótulos.
        float anchoYo = Fuente.ancho("YO") * eYo + 4; // Ancho de la etiqueta con margen.
        float altoYo = Fuente.FILAS * eYo + 4; // Alto de la etiqueta con margen.
        float yoX = Math.min(autoPx + 8, mapaX + mapaLado - anchoYo); // A la derecha del auto, sin salir del mapa.
        float yoY = Math.max(autoPy - altoYo - 8, alto - (mapaY + mapaLado)); // Encima del auto, sin salir del mapa.
        rectangulo(yoX, yoY, anchoYo, altoYo, 0, 0, 0, 0.8f); // Fondo oscuro para que se lea.
        texto("YO", yoX + 2, yoY + 2, eYo, 0.1f, 1, 1); // Etiqueta cian, del mismo color que el auto en el mapa.
        float norteX = mapaX + mapaLado / 2f - 2.5f * e; // Centro superior del minimapa.
        float norteY = alto - (mapaY + mapaLado) + 3; // Justo debajo del borde superior.
        texto("N", norteX, norteY, e + 1, 1, 1, 1); // Indica que el norte está arriba.
    }

    /** Panel superior izquierdo con todos los controles. */
    private void dibujarAyuda(int e) {
        String[] lineas = { // Controles disponibles.
            "CONTROLES", // Título.
            "W/S O FLECHAS: ACELERAR / RETROCEDER", // Avance.
            "A/D O FLECHAS: GIRAR", // Giro.
            "ESPACIO: FRENO", // Freno.
            "C: CAMARA SEGUIMIENTO / PRIMERA PERSONA / AEREA", // Cámara.
            "RATON: MIRAR EN PRIMERA PERSONA   V: MIRAR AL FRENTE", // Mirada libre.
            "N: DIA / NOCHE    F: FAROS", // Luces.
            "M: MINIMAPA       R: REINICIAR", // Mapa y reinicio.
            "P: PAUSA          H: OCULTAR AYUDA", // Pausa y ayuda.
            "ESC: SALIR", // Salida.
            "FRENA SOBRE LA MARCA DORADA PARA ENTREGAR" // Objetivo.
        };
        panelLineas(lineas, 12, 12, e); // Lo coloca en la esquina superior izquierda.
    }

    /** Pantalla de inicio. */
    private void dibujarMenu(int e) {
        rectangulo(0, 0, ancho, alto, 0, 0, 0, 0.55f); // Oscurece la ciudad detrás del menú.
        String titulo = "CIUDAD DE ENTREGAS"; // Nombre del juego.
        float y = alto * 0.28f; // Altura del título.
        textoCentrado(titulo, ancho / 2f, y, e * 3, 1, 0.75f, 0.15f); // Título grande y dorado.
        y += 12 * e * 3; // Baja debajo del título.
        String[] lineas = { // Instrucciones del menú.
            "ENTREGA " + DESTINOS.length + " PAQUETES EN LAS MARCAS DORADAS", // Objetivo.
            "FRENA SOBRE CADA MARCA PARA COMPLETAR LA ENTREGA", // Condición.
            "CUIDADO CON EL TRAFICO DE LA CIUDAD", // Tráfico.
            "", // Separación.
            "ENTER: COMENZAR      H: VER CONTROLES      ESC: SALIR" // Teclas.
        };
        for (String linea : lineas) { // Escribe cada línea centrada.
            textoCentrado(linea, ancho / 2f, y, e, 0.92f, 0.94f, 0.97f); // Texto blanco.
            y += 12 * e; // Siguiente línea.
        }
    }

    /** Aviso de pausa. */
    private void dibujarPausa(int e) {
        rectangulo(0, 0, ancho, alto, 0, 0, 0, 0.45f); // Oscurece la escena detenida.
        textoCentrado("PAUSA", ancho / 2f, alto * 0.38f, e * 4, 1, 1, 1); // Palabra grande.
        textoCentrado("P: CONTINUAR    R: REINICIAR    H: AYUDA", ancho / 2f, alto * 0.38f + 40 * e, e, 0.85f, 0.88f, 0.92f); // Opciones.
    }

    /** Mensaje al completar todas las entregas. */
    private void dibujarVictoria(int e) {
        String[] lineas = { // Texto del mensaje.
            "GANASTE! " + DESTINOS.length + " ENTREGAS EN " + (int) tiempo + " S", // Resultado.
            "R: JUGAR OTRA VEZ" // Cómo reiniciar.
        };
        int anchoTexto = Math.max(Fuente.ancho(lineas[0]) * e * 2, Fuente.ancho(lineas[1]) * e); // Ancho del bloque.
        float x = ancho / 2f - anchoTexto / 2f - 8 * e; // Centra el panel.
        float y = alto * 0.12f; // Lo coloca en la parte superior.
        if (mapaLado > 0) { // Si el minimapa está visible, el mensaje no debe taparlo.
            y = Math.max(y, alto - mapaY + 12); // Lo baja hasta debajo del minimapa.
        }
        rectangulo(x, y, anchoTexto + 16 * e, 34 * e, 0.05f, 0.18f, 0.08f, 0.82f); // Fondo verde oscuro.
        textoCentrado(lineas[0], ancho / 2f, y + 6 * e, e * 2, 0.5f, 1, 0.55f); // Resultado grande.
        textoCentrado(lineas[1], ancho / 2f, y + 24 * e, e, 1, 1, 1); // Instrucción.
    }

    /** Dibuja un panel con varias líneas; la primera se resalta como título. */
    private void panelLineas(String[] lineas, float x, float y, int e) {
        int anchoMaximo = 0; // Ancho de la línea más larga.
        for (String linea : lineas) { // Recorre las líneas.
            anchoMaximo = Math.max(anchoMaximo, Fuente.ancho(linea)); // Conserva el máximo.
        }
        int margen = 6 * e; // Espacio interior.
        int linea = 11 * e; // Separación entre líneas.
        rectangulo(x, y, anchoMaximo * e + 2 * margen, lineas.length * linea + 2 * margen - 4 * e, 0.03f, 0.05f, 0.09f, 0.78f); // Fondo.
        float cursorY = y + margen; // Primera línea.
        for (int i = 0; i < lineas.length; i++) { // Escribe cada línea.
            float r = i == 0 ? 1 : 0.88f; // El título es dorado; el resto, claro.
            float g = i == 0 ? 0.75f : 0.90f; // Componente verde.
            float b = i == 0 ? 0.15f : 0.95f; // Componente azul.
            texto(lineas[i], x + margen, cursorY, e, r, g, b); // Escribe la línea.
            cursorY += linea; // Siguiente línea.
        }
    }

    // ==================== 7. UTILIDADES DE DIBUJO 2D ====================

    /** Dibuja un rectángulo en píxeles (origen arriba a la izquierda) con opacidad alfa. */
    protected void rectangulo(float x, float y, float w, float h, float r, float g, float b, float alfa) {
        vector("uPos", x + w / 2, y + h / 2, 0); // Centro del rectángulo.
        vector("uEscala", w, h, 1); // Tamaño en píxeles.
        vector("uColor", r, g, b); // Color.
        decimal("uGiro", 0); // Sin rotación.
        decimal("uAlfa", alfa); // Opacidad.
        glDrawArrays(GL_TRIANGLES, 0, 6); // Solo la cara frontal del cubo: dos triángulos bastan.
    }

    /** Escribe texto con la fuente 5 x 7; e es el tamaño de cada píxel. Devuelve la X donde termina. */
    protected float texto(String texto, float x, float y, int e, float r, float g, float b) {
        Fuente.recorrer(texto, (fila, columna, largo) -> // Cada tramo se convierte en un rectángulo.
            rectangulo(x + columna * e, y + fila * e, largo * e, e, r, g, b, 1)); // Tramo opaco.
        return x + (Fuente.ancho(texto) + 1) * e; // Permite continuar escribiendo a la derecha.
    }

    /** Escribe texto centrado horizontalmente en centroX. */
    protected void textoCentrado(String texto, float centroX, float y, int e, float r, float g, float b) {
        texto(texto, centroX - Fuente.ancho(texto) * e / 2f, y, e, r, g, b); // Desplaza la mitad del ancho.
    }

    /** Punto de entrada del proyecto final con las mejoras opcionales. */
    public static void main(String[] args) {
        clase5 aplicacion = new clase5(); // Crea la versión completa.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
