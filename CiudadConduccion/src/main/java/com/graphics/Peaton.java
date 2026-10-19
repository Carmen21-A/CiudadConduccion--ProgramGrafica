package com.graphics; // Comparte el paquete con las cinco lecciones.

import java.util.Random; // Elige al azar si cruza la calle o sigue rodeando la manzana.

// =========================================================================================
// PEATON JAVA :: AQUI SE CREA:
// - LOGICA DE MOVIMIENTO DE PEATONES (CAMINAR POR ACERAS EN ANILLO)
// - CRUCE SEGURO POR PASOS PEATONALES CON SEMAFORO EN ROJO
// - ANIMACION DE PASOS (BALANCEO ALTERNO DE PIERNAS Y BRAZOS)
// - DETECCION DE AUTOS Y OBSTACULOS PARA NO SER ATROPELLADO
// =========================================================================================

class Peaton {

    /** Consulta los segundos del ciclo del semáforo de una calle del cruce (0-7.5 rojo, 7.5-12 verde, 12-14 amarillo). */
    interface CicloSemaforo {
        float segundos(int fila, int columna, boolean vertical); // Lo implementa clase4.cicloSemaforo().
    }

    /** Indica si un punto del suelo está ocupado por un auto (el del jugador o uno del tráfico). */
    interface Obstaculos {
        boolean ocupado(float x, float z); // Lo implementa clase5.autoEn().
    }

    static final float ANILLO = 4.6f;
    static final float ESQUINA = 4.2f;
    static final float VELOCIDAD = 1.5f;
    static final float VELOCIDAD_CRUCE = 3.0f;
    static final float INICIO_ROJO = 1.2f;

    float x;
    float z;
    float angulo;
    float pasos;
    int fila, columna;
    int indice;
    boolean esperando;
    boolean cruzando;
    boolean detenido;
    int cruces;
    private int filaDestino, columnaDestino, indiceDestino;
    private int filaSemaforo, columnaSemaforo;
    private boolean calleVertical;
    private final Random azar;

    /** Crea el peatón en un punto del anillo de una manzana. */
    Peaton(int fila, int columna, int indice, long semilla) {
        this.fila = fila;
        this.columna = columna;
        this.indice = indice;
        float[] punto = punto(fila, columna, indice);
        x = punto[0];
        z = punto[1];
        azar = new Random(semilla);
        avanzarIndice();
    }

    /**
     * Puntos del anillo en sentido horario (norte arriba). Los impares están junto a los pasos peatonales:
     * 1 = norte, 3 = este, 5 = sur, 7 = oeste.
     */
    static float[] punto(int fila, int columna, int indice) {
        float cx = clase1.centro(columna);
        float cz = clase1.centro(fila);
        switch (indice) {
            case 0: return new float[] {cx - ESQUINA, cz - ESQUINA};
            case 1: return new float[] {cx + 4, cz - ANILLO};
            case 2: return new float[] {cx + ESQUINA, cz - ESQUINA};
            case 3: return new float[] {cx + ANILLO, cz - 4};
            case 4: return new float[] {cx + ESQUINA, cz + ESQUINA};
            case 5: return new float[] {cx + 4, cz + ANILLO};
            case 6: return new float[] {cx - ESQUINA, cz + ESQUINA};
            default: return new float[] {cx - ANILLO, cz - 4};
        }
    }

    /** Pasa al siguiente punto del anillo. */
    private void avanzarIndice() {
        indice = (indice + 1) % 8;
    }

    /** Indica si existe una manzana en esa fila y columna. */
    private static boolean esManzana(int fila, int columna) {
        int n = clase1.MAPA.length;
        return fila >= 0 && columna >= 0 && fila < n && columna < n && clase1.MAPA[fila][columna] != 0;
    }

    /** Prepara el cruce desde el acceso actual; devuelve false si al otro lado no hay manzana. */
    private boolean prepararCruce() {
        int vf = fila;
        int vc = columna;
        int llegada;
        int sf = fila;
        int sc = columna;
        if (indice == 1) {
            vf = fila - 2;
            llegada = 5;
        } else if (indice == 3) {
            vc = columna + 2;
            llegada = 7;
        } else if (indice == 5) {
            vf = fila + 2;
            llegada = 1;
            sf = fila + 2;
        } else {
            vc = columna - 2;
            llegada = 3;
            sc = columna - 2;
        }
        if (!esManzana(vf, vc)) {
            return false;
        }
        filaDestino = vf;
        columnaDestino = vc;
        indiceDestino = llegada;
        filaSemaforo = sf;
        columnaSemaforo = sc;
        calleVertical = indice == 3 || indice == 7;
        return true;
    }

    /** Indica si el peatón está pisando la acera de alguna manzana (para dibujarlo a la altura correcta). */
    boolean sobreAcera() {
        int f = (int) Math.floor((z + clase1.LIMITE) / clase1.CELDA);
        int c = (int) Math.floor((x + clase1.LIMITE) / clase1.CELDA);
        return esManzana(f, c);
    }

    /** Mueve al peatón; con verde o amarillo para los autos espera en el paso peatonal. */
    void actualizar(float deltaTime, CicloSemaforo semaforos, Obstaculos autos) {
        detenido = false;
        if (esperando) {
            if (semaforos.segundos(filaSemaforo, columnaSemaforo, calleVertical) >= INICIO_ROJO) {
                return;
            }
            esperando = false;
            cruzando = true;
        }
        float[] objetivo = cruzando ? punto(filaDestino, columnaDestino, indiceDestino) : punto(fila, columna, indice);
        float dx = objetivo[0] - x;
        float dz = objetivo[1] - z;
        float distancia = (float) Math.hypot(dx, dz);
        float paso = (cruzando ? VELOCIDAD_CRUCE : VELOCIDAD) * deltaTime;
        if (distancia > 0.0001f) {
            angulo = (float) Math.atan2(-dx, -dz);
        }
        if (distancia > 0.0001f) {
            float delanteX = x + dx / distancia * 0.5f;
            float delanteZ = z + dz / distancia * 0.5f;
            if (autos.ocupado(delanteX, delanteZ)) {
                detenido = true;
                return;
            }
        }
        if (distancia > paso) {
            x += dx / distancia * paso;
            z += dz / distancia * paso;
            pasos += paso;
            return;
        }
        x = objetivo[0];
        z = objetivo[1];
        if (cruzando) {
            cruzando = false;
            fila = filaDestino;
            columna = columnaDestino;
            indice = indiceDestino;
            cruces++;
            avanzarIndice();
            return;
        }
        boolean acceso = indice % 2 == 1;
        if (acceso && azar.nextFloat() < 0.6f && prepararCruce()) {
            esperando = true;
            float[] otraAcera = punto(filaDestino, columnaDestino, indiceDestino);
            angulo = (float) Math.atan2(-(otraAcera[0] - x), -(otraAcera[1] - z));
            return;
        }
        avanzarIndice();
    }
}
