package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected static final float INICIO_X = centro(0); // Salida sobre la calle del borde oeste.
    protected static final float INICIO_Z = centro(MAPA.length - 1); // Salida sobre la calle del borde sur.
    protected float autoX = INICIO_X; // Posición horizontal inicial: centro de una calle.
    protected float autoZ = INICIO_Z; // Posición inicial en la esquina suroeste de la ciudad ampliada.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float MEDIO_ANCHO = 1.0f; // Mitad del ancho del auto, ruedas incluidas.
    protected static final float MEDIO_LARGO = 1.36f; // Mitad del largo del auto, faros incluidos.
    protected static final float HOLGURA = 0.1f; // Pequeño margen para que la carrocería no roce la acera.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = INICIO_X; // Recupera la coordenada X de inicio.
        autoZ = INICIO_Z; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * 9 * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = 0.7f; // Define la pérdida de velocidad normal al rodar.

        if (pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = 7; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-6, Math.min(16, velocidad)); // Limita la reversa a -6 y el avance a 16.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        float anguloAnterior = angulo; // Guarda la orientación por si el giro no es posible.
        angulo += direccion * velocidad * 0.11f * deltaTime; // Gira según la velocidad; en reversa invierte el giro.
        if (!puedeCircular(autoX, autoZ)) { // Al girar, una esquina del auto se metería en la acera.
            angulo = anguloAnterior; // Conserva la orientación anterior.
        }
        float frenteX = -(float) Math.sin(angulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(angulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        float roce = Math.min(1, 8 * deltaTime); // Fracción de velocidad que se pierde por cuadro al rozar la acera.
        if (puedeCircular(siguienteX, siguienteZ)) { // Comprueba la posición antes de mover el auto.
            autoX = siguienteX; // Acepta el desplazamiento horizontal.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else if (puedeCircular(siguienteX, autoZ)) { // Choca de costado: puede deslizarse a lo largo del eje X.
            autoX = siguienteX; // Se desliza junto a la acera.
            velocidad *= 1 - (1 - Math.abs(frenteX)) * roce; // De frente contra la acera casi se detiene; de refilón conserva velocidad.
        } else if (puedeCircular(autoX, siguienteZ)) { // Puede deslizarse a lo largo del eje Z.
            autoZ = siguienteZ; // Se desliza junto a la acera.
            velocidad *= 1 - (1 - Math.abs(frenteZ)) * roce; // Pierde velocidad según lo directo del choque.
        } else { // La posición propuesta invadiría una manzana o saldría del mapa.
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }

        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara | R: reiniciar | "; // Añade los controles de cámara y reinicio.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Comprueba si el rectángulo del auto, con la orientación actual, cabe en una posición sin tocar aceras ni bordes. */
    protected boolean puedeCircular(float x, float z) {
        float[] auto = contornoAuto(x, z, angulo); // Las cuatro esquinas reales de la carrocería.
        for (int i = 0; i < auto.length; i += 2) { // Revisa cada esquina.
            if (Math.abs(auto[i]) > LIMITE || Math.abs(auto[i + 1]) > LIMITE) { // Detecta salida por cualquier borde.
                return false; // Rechaza la posición exterior.
            }
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                if (Math.abs(x - centroX) > CELDA || Math.abs(z - centroZ) > CELDA) { // Manzana lejana: imposible tocarla.
                    continue; // Ahorra el cálculo detallado.
                }

                if (seTocan(auto, contornoManzana(centroX, centroZ))) { // El auto invade la acera ochavada.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /** Esquinas (x0, z0, x1, z1, ...) del rectángulo de un auto con centro, giro y la pequeña holgura. */
    protected static float[] contornoAuto(float x, float z, float giro) {
        float coseno = (float) Math.cos(giro); // Misma rotación que piezaEn().
        float seno = (float) Math.sin(giro); // Misma rotación que piezaEn().
        float a = MEDIO_ANCHO + HOLGURA; // Mitad del ancho con margen.
        float l = MEDIO_LARGO + HOLGURA; // Mitad del largo con margen.
        float[] locales = {-a, -l, a, -l, a, l, -a, l}; // Esquinas en el espacio del auto.
        float[] mundo = new float[8]; // Esquinas en la ciudad.
        for (int i = 0; i < 8; i += 2) { // Transforma cada esquina.
            mundo[i] = x + coseno * locales[i] + seno * locales[i + 1]; // Gira y traslada en X.
            mundo[i + 1] = z - seno * locales[i] + coseno * locales[i + 1]; // Gira y traslada en Z.
        }
        return mundo; // Polígono del auto.
    }

    /** Octógono de la acera de una manzana: un cuadrado de lado CELDA con las esquinas recortadas por DIAGONAL_ACERA. */
    protected static float[] contornoManzana(float cx, float cz) {
        float m = CELDA / 2; // Mitad del lado.
        float c = m - DIAGONAL_ACERA; // Donde empieza el corte diagonal.
        return new float[] {cx + m, cz - c, cx + m, cz + c, cx + c, cz + m, cx - c, cz + m, // Lado este y esquina sureste.
                            cx - m, cz + c, cx - m, cz - c, cx - c, cz - m, cx + c, cz - m}; // Lado oeste y esquina noroeste.
    }

    /** Teorema del eje separador: dos polígonos convexos se tocan si ninguna normal de sus lados los separa. */
    protected static boolean seTocan(float[] a, float[] b) {
        return !hayEjeSeparador(a, b) && !hayEjeSeparador(b, a); // Prueba las normales de ambos polígonos.
    }

    /** Busca un lado de p cuya normal deje a p y q en intervalos separados. */
    private static boolean hayEjeSeparador(float[] p, float[] q) {
        int n = p.length / 2; // Número de vértices de p.
        for (int i = 0; i < n; i++) { // Recorre los lados de p.
            int j = (i + 1) % n; // Vértice siguiente.
            float ejeX = p[2 * i + 1] - p[2 * j + 1]; // Normal del lado (perpendicular a su dirección), en X.
            float ejeZ = p[2 * j] - p[2 * i]; // Normal del lado, en Z.
            float[] rangoP = proyectar(p, ejeX, ejeZ); // Sombra de p sobre la normal.
            float[] rangoQ = proyectar(q, ejeX, ejeZ); // Sombra de q sobre la normal.
            if (rangoP[1] <= rangoQ[0] || rangoQ[1] <= rangoP[0]) { // Las sombras no se superponen.
                return true; // Encontró un eje que los separa.
            }
        }
        return false; // Ningún lado de p los separa.
    }

    /** Mínimo y máximo de los vértices de un polígono proyectados sobre un eje. */
    private static float[] proyectar(float[] poligono, float ejeX, float ejeZ) {
        float minimo = Float.MAX_VALUE; // Valor inicial muy grande.
        float maximo = -Float.MAX_VALUE; // Valor inicial muy pequeño.
        for (int i = 0; i < poligono.length; i += 2) { // Recorre los vértices.
            float valor = poligono[i] * ejeX + poligono[i + 1] * ejeZ; // Producto escalar con el eje.
            minimo = Math.min(minimo, valor); // Actualiza el mínimo.
            maximo = Math.max(maximo, valor); // Actualiza el máximo.
        }
        return new float[] {minimo, maximo}; // Intervalo ocupado sobre el eje.
    }

    // ==================== 5. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca la cámara 12 unidades detrás en Z.
        vector("uOjo", camaraX, 9, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /** Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color RGB. */
    protected void dibujarAuto() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Dibuja la carrocería roja.
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Dibuja la cabina azulada.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.

        for (float x : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float z : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(x, 0.38f, z, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }

        float[] ladosFaros = {-0.55f, 0.55f}; // Define la separación lateral de las luces.
        for (float x : ladosFaros) { // Repite el dibujo para ambos lados.
            pieza(x, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Dibuja un faro delantero claro.
            pieza(x, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Dibuja una luz trasera roja.
        }
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        piezaEn(autoX, autoZ, angulo, x, y, z, sx, sy, sz, r, g, b); // Usa la posición y el giro del jugador.
    }

    /** Igual que pieza(), pero para cualquier vehículo; clase5 lo usa con el tráfico autónomo. */
    protected void piezaEn(float origenX, float origenZ, float giro, float x, float y, float z,
                           float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(giro); // Calcula el coseno de la orientación del vehículo.
        float seno = (float) Math.sin(giro); // Calcula el seno de la misma orientación.
        float mundoX = origenX + coseno * x + seno * z; // Gira la posición local y suma la posición X del vehículo.
        float mundoZ = origenZ - seno * x + coseno * z; // Gira la posición local y suma la posición Z del vehículo.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, giro); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
