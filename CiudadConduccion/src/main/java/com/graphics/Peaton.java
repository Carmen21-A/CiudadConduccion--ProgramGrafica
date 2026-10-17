package com.graphics; // Comparte el paquete con las cinco lecciones.

import java.util.Random; // Elige al azar si cruza la calle o sigue rodeando la manzana.

/**
 * PEATÓN DE CLASE5.
 * Camina por la acera alrededor de una manzana (un anillo de 8 puntos) y, en los pasos peatonales,
 * a veces cruza hacia la manzana vecina. Solo empieza a cruzar cuando el semáforo de ese cruce
 * acaba de ponerse en ROJO para los autos; con verde o amarillo espera en la acera.
 * No usa OpenGL, por eso las pruebas pueden simularlo sin abrir una ventana.
 */
class Peaton {

    /** Consulta los segundos del ciclo del semáforo de una calle del cruce (0-7.5 rojo, 7.5-12 verde, 12-14 amarillo). */
    interface CicloSemaforo {
        float segundos(int fila, int columna, boolean vertical); // Lo implementa clase4.cicloSemaforo().
    }

    /** Indica si un punto del suelo está ocupado por un auto (el del jugador o uno del tráfico). */
    interface Obstaculos {
        boolean ocupado(float x, float z); // Lo implementa clase5.autoEn().
    }

    static final float ANILLO = 4.6f; // Distancia del centro de la manzana al camino por la acera.
    static final float ESQUINA = 4.2f; // Las esquinas están más adentro porque la acera tiene ochava.
    static final float VELOCIDAD = 1.5f; // Paso normal en unidades por segundo.
    static final float VELOCIDAD_CRUCE = 3.0f; // Cruza más rápido para terminar antes de que acabe el rojo.
    static final float INICIO_ROJO = 1.2f; // Solo arranca en el primer 1.2 s del rojo: 11 unidades a 3 u/s caben en los 7.5 s de rojo.

    float x; // Posición X actual.
    float z; // Posición Z actual.
    float angulo; // Orientación; cero mira hacia -Z, igual que los autos.
    float pasos; // Avanza mientras camina; anima las piernas.
    int fila, columna; // Manzana cuya acera recorre.
    int indice; // Punto del anillo al que se dirige (0 a 7).
    boolean esperando; // Está detenido en un paso peatonal esperando el rojo.
    boolean cruzando; // Está sobre la calle.
    boolean detenido; // Un auto le cierra el paso: espera quieto sin atravesarlo.
    int cruces; // Cantidad de calles cruzadas; útil para las pruebas.
    private int filaDestino, columnaDestino, indiceDestino; // Manzana y punto de llegada del cruce.
    private int filaSemaforo, columnaSemaforo; // Manzana dueña del semáforo de ese cruce.
    private boolean calleVertical; // true si cruza una calle norte-sur (debe esperar el rojo de esa calle).
    private final Random azar; // Decide los cruces de forma reproducible.

    /** Crea el peatón en un punto del anillo de una manzana. */
    Peaton(int fila, int columna, int indice, long semilla) {
        this.fila = fila; // Manzana inicial.
        this.columna = columna; // Manzana inicial.
        this.indice = indice; // Punto inicial.
        float[] punto = punto(fila, columna, indice); // Coordenadas del punto inicial.
        x = punto[0]; // Coloca al peatón.
        z = punto[1]; // Coloca al peatón.
        azar = new Random(semilla); // Siempre la misma secuencia de decisiones.
        avanzarIndice(); // Empieza caminando hacia el siguiente punto.
    }

    /**
     * Puntos del anillo en sentido horario (norte arriba). Los impares están junto a los pasos peatonales:
     * 1 = norte, 3 = este, 5 = sur, 7 = oeste.
     */
    static float[] punto(int fila, int columna, int indice) {
        float cx = clase1.centro(columna); // Centro X de la manzana.
        float cz = clase1.centro(fila); // Centro Z de la manzana.
        switch (indice) { // Selecciona la esquina o el acceso al cruce.
            case 0: return new float[] {cx - ESQUINA, cz - ESQUINA}; // Esquina noroeste.
            case 1: return new float[] {cx + 4, cz - ANILLO}; // Acceso al cruce norte (x + 4, como el paso peatonal).
            case 2: return new float[] {cx + ESQUINA, cz - ESQUINA}; // Esquina noreste.
            case 3: return new float[] {cx + ANILLO, cz - 4}; // Acceso al cruce este (z - 4, como el paso peatonal).
            case 4: return new float[] {cx + ESQUINA, cz + ESQUINA}; // Esquina sureste.
            case 5: return new float[] {cx + 4, cz + ANILLO}; // Acceso al cruce sur.
            case 6: return new float[] {cx - ESQUINA, cz + ESQUINA}; // Esquina suroeste.
            default: return new float[] {cx - ANILLO, cz - 4}; // Acceso al cruce oeste.
        }
    }

    /** Pasa al siguiente punto del anillo. */
    private void avanzarIndice() {
        indice = (indice + 1) % 8; // Recorre el anillo en sentido horario.
    }

    /** Indica si existe una manzana en esa fila y columna. */
    private static boolean esManzana(int fila, int columna) {
        int n = clase1.MAPA.length; // Tamaño del mapa.
        return fila >= 0 && columna >= 0 && fila < n && columna < n && clase1.MAPA[fila][columna] != 0; // Dentro y no es calle.
    }

    /** Prepara el cruce desde el acceso actual; devuelve false si al otro lado no hay manzana. */
    private boolean prepararCruce() {
        int vf = fila; // Fila de la manzana vecina.
        int vc = columna; // Columna de la manzana vecina.
        int llegada; // Punto de llegada en la vecina.
        int sf = fila; // Fila de la manzana dueña del semáforo.
        int sc = columna; // Columna de la manzana dueña del semáforo.
        if (indice == 1) { // Cruce hacia el norte.
            vf = fila - 2; // La vecina está dos filas arriba.
            llegada = 5; // Llega a su acceso sur.
        } else if (indice == 3) { // Cruce hacia el este.
            vc = columna + 2; // La vecina está dos columnas a la derecha.
            llegada = 7; // Llega a su acceso oeste.
        } else if (indice == 5) { // Cruce hacia el sur.
            vf = fila + 2; // La vecina está dos filas abajo.
            llegada = 1; // Llega a su acceso norte.
            sf = fila + 2; // El semáforo de ese cruce pertenece a la vecina.
        } else { // Cruce hacia el oeste.
            vc = columna - 2; // La vecina está dos columnas a la izquierda.
            llegada = 3; // Llega a su acceso este.
            sc = columna - 2; // El semáforo de ese cruce pertenece a la vecina.
        }
        if (!esManzana(vf, vc)) { // Al otro lado está el borde de la ciudad.
            return false; // No hay cruce.
        }
        filaDestino = vf; // Guarda la manzana de llegada.
        columnaDestino = vc; // Guarda la manzana de llegada.
        indiceDestino = llegada; // Guarda el punto de llegada.
        filaSemaforo = sf; // Guarda el semáforo que debe respetar.
        columnaSemaforo = sc; // Guarda el semáforo que debe respetar.
        calleVertical = indice == 3 || indice == 7; // Hacia el este u oeste atraviesa una calle norte-sur.
        return true; // El cruce es posible.
    }

    /** Indica si el peatón está pisando la acera de alguna manzana (para dibujarlo a la altura correcta). */
    boolean sobreAcera() {
        int f = (int) Math.floor((z + clase1.LIMITE) / clase1.CELDA); // Fila de la celda actual.
        int c = (int) Math.floor((x + clase1.LIMITE) / clase1.CELDA); // Columna de la celda actual.
        return esManzana(f, c); // Las celdas de manzana tienen la acera elevada.
    }

    /** Mueve al peatón; con verde o amarillo para los autos espera en el paso peatonal. */
    void actualizar(float deltaTime, CicloSemaforo semaforos, Obstaculos autos) {
        detenido = false; // Se vuelve a comprobar en cada cuadro.
        if (esperando) { // Está en el borde de la acera.
            if (semaforos.segundos(filaSemaforo, columnaSemaforo, calleVertical) >= INICIO_ROJO) { // No es el inicio del rojo de esa calle.
                return; // Sigue esperando quieto.
            }
            esperando = false; // El rojo acaba de empezar: los autos se detienen.
            cruzando = true; // Comienza a cruzar.
        }
        float[] objetivo = cruzando ? punto(filaDestino, columnaDestino, indiceDestino) : punto(fila, columna, indice); // Hacia dónde camina.
        float dx = objetivo[0] - x; // Distancia pendiente en X.
        float dz = objetivo[1] - z; // Distancia pendiente en Z.
        float distancia = (float) Math.hypot(dx, dz); // Distancia pendiente.
        float paso = (cruzando ? VELOCIDAD_CRUCE : VELOCIDAD) * deltaTime; // Lo que avanza en este cuadro.
        if (distancia > 0.0001f) { // Gira hacia el objetivo si no está encima.
            angulo = (float) Math.atan2(-dx, -dz); // Misma convención de ángulo que los autos.
        }
        if (distancia > 0.0001f) { // Mira medio paso por delante antes de moverse.
            float delanteX = x + dx / distancia * 0.5f; // Punto inmediato delante del peatón.
            float delanteZ = z + dz / distancia * 0.5f; // Punto inmediato delante del peatón.
            if (autos.ocupado(delanteX, delanteZ)) { // Un auto se atravesó en su camino.
                detenido = true; // No puede pasar sobre el auto.
                return; // Espera a que el auto se retire.
            }
        }
        if (distancia > paso) { // Todavía no llega.
            x += dx / distancia * paso; // Avanza en X.
            z += dz / distancia * paso; // Avanza en Z.
            pasos += paso; // Anima las piernas.
            return; // Sigue caminando el próximo cuadro.
        }
        x = objetivo[0]; // Llega exactamente al punto.
        z = objetivo[1]; // Llega exactamente al punto.
        if (cruzando) { // Terminó de cruzar la calle.
            cruzando = false; // Vuelve a la acera.
            fila = filaDestino; // Ahora rodea la manzana vecina.
            columna = columnaDestino; // Ahora rodea la manzana vecina.
            indice = indiceDestino; // Está en el acceso de llegada.
            cruces++; // Cuenta el cruce.
            avanzarIndice(); // Sigue por la acera.
            return; // Continúa el próximo cuadro.
        }
        boolean acceso = indice % 2 == 1; // Los puntos impares están junto a un paso peatonal.
        if (acceso && azar.nextFloat() < 0.6f && prepararCruce()) { // A veces decide cruzar.
            esperando = true; // Espera el rojo para los autos.
            float[] otraAcera = punto(filaDestino, columnaDestino, indiceDestino); // Lado opuesto de la calle.
            angulo = (float) Math.atan2(-(otraAcera[0] - x), -(otraAcera[1] - z)); // Mira hacia donde va a cruzar.
            return; // No avanza más en este cuadro.
        }
        avanzarIndice(); // Sigue rodeando la manzana.
    }
}
