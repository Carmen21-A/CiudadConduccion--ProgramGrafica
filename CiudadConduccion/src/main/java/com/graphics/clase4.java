package com.graphics; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
 * Hereda clase3, que hereda clase2, que a su vez hereda clase1.
 * Cada cruce tiene luces separadas para la calle horizontal y la vertical; el tráfico autónomo las respeta en clase5.
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    protected boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    protected int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    protected float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    protected float reloj = 0; // Tiempo de animación que nunca se detiene: semáforos y baliza siguen al ganar.
    protected static final int ROJO = 0, AMARILLO = 1, VERDE = 2; // Fases de los semáforos.
    protected static final float CICLO = 14; // Duración del ciclo de cada semáforo en segundos.
    protected static final float FIN_ROJO = 7.5f; // 0-7.5 s: rojo (incluye medio segundo con todo en rojo).
    protected static final float FIN_VERDE = 12; // 7.5-12 s: verde; 12-14 s: amarillo.
    protected int mapaX, mapaY, mapaLado; // Recuadro del minimapa en píxeles (Y desde abajo); clase5 lo usa para rótulos.
    protected static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada; todas son centros de calle.
        {centro(6), centro(0)}, // Primera entrega: calle del borde norte, sector nuevo.
        {centro(10), centro(6)}, // Segunda entrega: calle del borde este, sector nuevo.
        {centro(4), centro(4)}, // Tercera entrega: cruce del centro de la ciudad original.
        {centro(8), centro(10)}, // Cuarta entrega: calle del borde sur, sector nuevo.
        {centro(0), centro(2)} // Quinta entrega: calle del borde oeste, sector nuevo.
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por clase3.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        reloj += deltaTime; // Avanza las animaciones aunque la partida haya terminado.
        if (entregas >= DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de clase3.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | "; // Muestra la tecla que alterna el minimapa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar todas las paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: jugar otra vez"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/" + DESTINOS.length; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en la marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        if (vistaMapa) { // En el minimapa la ciudad ampliada se ve pequeña.
            caja(x, 20, z, 8, 0.1f, 8, 1, 0.72f, 0.12f); // Dibuja una marca mayor y por encima de los edificios.
        } else { // En la vista principal se conserva la marca del proyecto original.
            caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
            float alturaBaliza = 3.5f + (float) Math.sin(reloj * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, reloj); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                if (tipo == 2) { // Detecta una parcela de parque.
                    dibujarParque(x, z); // Añade árboles y un banco.
                }
                if (tipo == 1) { // Detecta una parcela con edificio.
                    float altura = alturaEdificio(fila, columna); // Recupera la misma altura calculada en clase1.
                    dibujarVentanas(x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                }
                if (tipo != 0) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    dibujarPasosPeatonales(fila, columna, x, z); // Pinta los cruces hacia las manzanas vecinas.
                    int faseFila = faseSemaforo(fila, columna, false); // Luz para la calle horizontal (este-oeste) del cruce noreste.
                    int faseColumna = faseSemaforo(fila, columna, true); // Luz para la calle vertical (norte-sur) del mismo cruce.
                    dibujarSemaforo(x + 4, z - 4, faseFila, faseColumna); // Semáforo en la esquina suroeste del cruce.
                    boolean hayOpuesta = fila - 2 >= 0 && columna + 2 < MAPA[fila].length && MAPA[fila - 2][columna + 2] != 0; // Manzana en la esquina noreste del cruce.
                    if (hayOpuesta) { // También se ve desde el otro lado del cruce.
                        dibujarSemaforo(centro(columna + 2) - 4, centro(fila - 2) + 4, faseFila, faseColumna); // Segundo poste en la esquina opuesta.
                    }
                }
            }
        }
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        //asiento
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        float rojo = 0.55f, verde = 0.72f, azul = 0.88f; // De día los vidrios reflejan el cielo celeste.
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
            rojo = 0.95f; // Luz cálida de las habitaciones.
            verde = 0.75f; // Luz cálida de las habitaciones.
            azul = 0.38f; // Luz cálida de las habitaciones.
        }
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, rojo, verde, azul); // Ventana de la fachada norte.
                caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, rojo, verde, azul); // Ventana de la fachada sur.
                caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, rojo, verde, azul); // Ventana de la fachada oeste.
                caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, rojo, verde, azul); // Ventana de la fachada este.
            }
        }
        entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Pinta pasos peatonales que cruzan la calle hacia la manzana vecina del este y la del sur, junto al cruce. */
    private void dibujarPasosPeatonales(int fila, int columna, float x, float z) {
        boolean vecinaEste = columna + 2 < MAPA[fila].length && MAPA[fila][columna + 2] != 0; // Hay manzana al otro lado de la calle este.
        boolean vecinaSur = fila + 2 < MAPA.length && MAPA[fila + 2][columna] != 0; // Hay manzana al otro lado de la calle sur.
        for (int franja = 0; franja < 8; franja++) { // Ocho franjas cubren el ancho de la calle.
            float desplazamiento = -4.025f + franja * 1.15f; // Reparte las franjas de acera a acera.
            if (vecinaEste) { // Cruce sobre la calle vertical, junto a la esquina noreste (donde está el semáforo).
                caja(x + CELDA + desplazamiento, 0.045f, z - 4, 0.55f, 0.04f, 1.6f, 0.85f, 0.87f, 0.83f); // Franja paralela a la marcha.
            }
            if (vecinaSur) { // Cruce sobre la calle horizontal, junto a la esquina sureste.
                caja(x + 4, 0.045f, z + CELDA + desplazamiento, 1.6f, 0.04f, 0.55f, 0.85f, 0.87f, 0.83f); // Franja paralela a la marcha.
            }
        }
    }

    /**
     * Devuelve el estado del semáforo del cruce noreste de una manzana: ROJO, AMARILLO o VERDE.
     * vertical = false: luz de la calle horizontal (fila, este-oeste);
     * vertical = true: luz de la calle vertical (columna, norte-sur). Nunca están en verde a la vez.
     */
    protected int faseSemaforo(int fila, int columna, boolean vertical) {
        float segundo = cicloSemaforo(fila, columna, vertical); // Posición dentro del ciclo de 14 s.
        if (segundo < FIN_ROJO) { // Primeros 7.5 segundos.
            return ROJO; // Rojo.
        }
        if (segundo < FIN_VERDE) { // Siguientes 4.5 segundos.
            return VERDE; // Verde.
        }
        return AMARILLO; // Últimos dos segundos.
    }

    /**
     * Segundos (0 a 14) dentro del ciclo de una de las dos calles del cruce: 0-7.5 rojo, 7.5-12 verde, 12-14 amarillo.
     * La calle vertical va medio ciclo (7 s) desfasada: cuando una tiene verde o amarillo, la otra tiene rojo,
     * y en 0-0.5 s y 7-7.5 s ambas quedan en rojo para que el cruce se vacíe.
     * Los 9 cruces del centro (ciudad original) cambian juntos; los de los sectores nuevos se intercalan.
     */
    protected float cicloSemaforo(int fila, int columna, boolean vertical) {
        boolean original = Math.abs(fila - 5) <= 2 && Math.abs(columna - 5) <= 2; // Manzanas de la ciudad original.
        float desfase = 0; // Los semáforos originales conservan el ciclo sincronizado.
        if (!original) { // Los semáforos añadidos se intercalan.
            desfase = ((fila / 2 + columna / 2) % 3) * CICLO / 3; // Manzanas vecinas reciben desfases distintos.
        }
        if (vertical) { // La calle norte-sur funciona al revés que la este-oeste.
            desfase += CICLO / 2; // Medio ciclo de diferencia.
        }
        return (reloj + desfase) % CICLO; // Posición dentro del ciclo.
    }

    /**
     * Construye un semáforo de cuatro caras: las caras norte y sur muestran la luz de la calle vertical
     * (la ven los autos que van de norte a sur o de sur a norte) y las caras este y oeste la de la calle horizontal.
     */
    private void dibujarSemaforo(float x, float z, int faseFila, int faseColumna) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.55f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las cuatro caras.
        caja(x, 3.75f, z, 0.75f, 0.08f, 0.75f, 0.08f, 0.10f, 0.12f); // Visera superior que sobresale.
        for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            bombilla(indice, faseColumna, x, altura, z - 0.29f, 0.28f, 0.06f); // Cara norte: autos que bajan hacia el sur.
            bombilla(indice, faseColumna, x, altura, z + 0.29f, 0.28f, 0.06f); // Cara sur: autos que suben hacia el norte.
            bombilla(indice, faseFila, x - 0.29f, altura, z, 0.06f, 0.28f); // Cara oeste: autos que van hacia el este.
            bombilla(indice, faseFila, x + 0.29f, altura, z, 0.06f, 0.28f); // Cara este: autos que van hacia el oeste.
   
         }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    /** Dibuja una bombilla (0 rojo, 1 amarillo, 2 verde), encendida si coincide con la fase. */
    private void bombilla(int indice, int fase, float x, float y, float z, float anchoX, float anchoZ) {
        boolean encendida = false; // Parte de una bombilla apagada.
        if (indice == 0) { // Selecciona la bombilla roja.
            encendida = fase == ROJO; // Enciende el rojo en su fase.
        } else if (indice == 1) { // Selecciona la bombilla amarilla.
            encendida = fase == AMARILLO; // Enciende el amarillo en su fase.
        } else { // Selecciona la bombilla verde.
            encendida = fase == VERDE; // Enciende el verde en su fase.
        }
        float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
        entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
        if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
            brillo = 1; // Usa intensidad completa para su color.
            entero("uEmision", 1); // Hace que la bombilla se vea encendida.
        }
        float rojo = 0; // Componente roja inicialmente ausente.
        float verde = 0; // Componente verde inicialmente ausente.
        if (indice < 2) { // Rojo y amarillo necesitan componente roja.
            rojo = brillo; // Añade rojo con la intensidad elegida.
        }
        if (indice > 0) { // Amarillo y verde necesitan componente verde.
            verde = brillo; // Añade verde; rojo más verde produce amarillo.
        }
        caja(x, y, z, anchoX, 0.25f, anchoZ, rojo, verde, 0.02f); // Dibuja la bombilla sobre la cara de la carcasa.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        mapaLado = 0; // Indica que todavía no hay minimapa en este cuadro.
        if (!mostrarMapa) { // Comprueba si el usuario ocultó el minimapa con M.
            return; // Conserva únicamente la imagen principal.
        }
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(280, dimensionMenor / 3); // Limita el mapa a 280 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        mapaX = x; // Guarda el borde izquierdo para los rótulos de la interfaz.
        mapaY = y; // Guarda el borde inferior del recuadro.
        mapaLado = lado; // Guarda el tamaño del recuadro.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja el auto del jugador: rectángulo cian con borde blanco y una flecha blanca hacia su frente. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 24.8f, autoZ, 4.6f, 0.1f, 6.0f, 1, 1, 1, angulo); // Borde blanco que lo distingue del tráfico.
        cajaGirada(autoX, 25, autoZ, 3.2f, 0.1f, 4.6f, 0.1f, 1, 1, angulo); // Cuerpo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        cajaGirada(autoX + frenteX * 4, 26, autoZ + frenteZ * 4, 1.0f, 0.1f, 6, 1, 1, 1, angulo); // Asta de la flecha.
        float puntaX = autoX + frenteX * 7; // Punta de la flecha en X.
        float puntaZ = autoZ + frenteZ * 7; // Punta de la flecha en Z.
        for (float lado = -0.6f; lado <= 0.6f; lado += 1.2f) { // Dos trazos inclinados forman la cabeza.
            float giro = angulo + lado; // Inclina el trazo respecto del frente.
            float trazoX = -(float) Math.sin(giro); // Dirección del trazo en X.
            float trazoZ = -(float) Math.cos(giro); // Dirección del trazo en Z.
            cajaGirada(puntaX - trazoX * 1.6f, 26, puntaZ - trazoZ * 1.6f, 1.0f, 0.1f, 3.2f, 1, 1, 1, giro); // Trazo desde la punta hacia atrás.
        }
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}
