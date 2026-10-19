package com.graphics;

// =========================================================================================
// CHATA JAVA :: AQUI SE CREA:
// - REMOLQUE DE CARGA ARTICULADO ENGANCHADO AL AUTO
// - FISICA DE ARRASTRE Y LIMITES DE ARTICULACION
// - SISTEMA DE COLISIONES DE LA CHATA CONTRA OBSTACULOS Y VEHICULOS
// - DIBUJO 3D: BOLA DE ENGANCHE Y LANZA DE REMOLQUE EN V
// - DIBUJO 3D: CHASIS, PISO DE MADERA Y BARANDAS ROJAS
// - DIBUJO 3D: RUEDAS, LLANTAS PLATEADAS Y GUARDABARROS
// =========================================================================================
public class Chata {

    // ==================== DIMENSIONES Y PARÁMETROS ====================
    public static final float DISTANCIA_ENGANCHE = 1.45f;
    public static final float DISTANCIA_CHATA = 2.05f;
    public static final float MEDIO_ANCHO = 0.85f;
    public static final float MEDIO_LARGO = 0.95f;
    public static final float TOPE_ARTICULACION = 0.72f;

    // ==================== ESTADO DE LA CHATA ====================
    public float x;
    public float z;
    public float angulo;
    public boolean chocando = false;

    /** Interfaz funcional para consultar las colisiones de la ciudad (reutiliza el sistema de clase2). */
    @FunctionalInterface
    public interface ValidadorColision {
        boolean puedeCircular(float x, float z, float giro);
    }

    /** Crea la chata alineada detrás del auto en la posición inicial. */
    public Chata(float autoX, float autoZ) {
        this.angulo = 0;
        this.x = autoX;
        this.z = autoZ + DISTANCIA_ENGANCHE + DISTANCIA_CHATA;
    }

    /** Reinicia la posición y ángulo de la chata acoplada detrás del auto. */
    public void reiniciar(float autoX, float autoZ, float anguloAuto) {
        this.angulo = anguloAuto;
        float frenteAutoX = -(float) Math.sin(anguloAuto);
        float frenteAutoZ = -(float) Math.cos(anguloAuto);
        float atrasAutoX = -frenteAutoX;
        float atrasAutoZ = -frenteAutoZ;
        float engancheX = autoX + atrasAutoX * DISTANCIA_ENGANCHE;
        float engancheZ = autoZ + atrasAutoZ * DISTANCIA_ENGANCHE;
        this.x = engancheX - frenteAutoX * DISTANCIA_CHATA;
        this.z = engancheZ - frenteAutoZ * DISTANCIA_CHATA;
        this.chocando = false;
    }

    
    public float[] calcularPaso(float autoX, float autoZ, float anguloAuto, float velocidadAuto,
                                float direccionVolante, float deltaTime) {
        if (Math.abs(autoX) > clase1.LIMITE * 1.5f || Math.abs(autoZ) > clase1.LIMITE * 1.5f) {
            return new float[] {autoX, autoZ + DISTANCIA_ENGANCHE + DISTANCIA_CHATA, anguloAuto};
        }

        float frenteAutoX = -(float) Math.sin(anguloAuto);
        float frenteAutoZ = -(float) Math.cos(anguloAuto);
        float atrasAutoX = -frenteAutoX;
        float atrasAutoZ = -frenteAutoZ;
        float engancheX = autoX + atrasAutoX * DISTANCIA_ENGANCHE;
        float engancheZ = autoZ + atrasAutoZ * DISTANCIA_ENGANCHE;

        float dx = engancheX - this.x;
        float dz = engancheZ - this.z;
        float dist = (float) Math.hypot(dx, dz);
        if (dist < 0.001f) {
            dx = frenteAutoX;
            dz = frenteAutoZ;
        }

        float anguloHaciaEnganche = (float) Math.atan2(-dx, -dz);

        float siguienteAngulo = this.angulo;
        if (velocidadAuto > 0.01f) {
            float dif = anguloHaciaEnganche - this.angulo;
            while (dif > Math.PI) dif -= (float) (2 * Math.PI);
            while (dif < -Math.PI) dif += (float) (2 * Math.PI);
            float velocidadGiro = Math.max(2.4f, velocidadAuto * 1.4f);
            float pasoGiro = velocidadGiro * deltaTime;
            siguienteAngulo += Math.max(-pasoGiro, Math.min(pasoGiro, dif));
        } else if (velocidadAuto < -0.01f) {
            float difAuto = anguloAuto - this.angulo;
            while (difAuto > Math.PI) difAuto -= (float) (2 * Math.PI);
            while (difAuto < -Math.PI) difAuto += (float) (2 * Math.PI);

            float giroVolante = -direccionVolante * 1.5f * Math.abs(velocidadAuto) * deltaTime;
            float autoEnderezado = difAuto * 0.8f * Math.abs(velocidadAuto) * deltaTime;
            siguienteAngulo += giroVolante + autoEnderezado;
        }

        float difAuto = anguloAuto - siguienteAngulo;
        while (difAuto > Math.PI) difAuto -= (float) (2 * Math.PI);
        while (difAuto < -Math.PI) difAuto += (float) (2 * Math.PI);
        float tope = TOPE_ARTICULACION;
        if (difAuto > tope) {
            siguienteAngulo = anguloAuto - tope;
        } else if (difAuto < -tope) {
            siguienteAngulo = anguloAuto + tope;
        }

        float frenteChataX = -(float) Math.sin(siguienteAngulo);
        float frenteChataZ = -(float) Math.cos(siguienteAngulo);
        float propuestaX = engancheX - frenteChataX * DISTANCIA_CHATA;
        float propuestaZ = engancheZ - frenteChataZ * DISTANCIA_CHATA;

        return new float[] {propuestaX, propuestaZ, siguienteAngulo};
    }

    /** Aplica un paso calculado aceptado por la comprobación de colisiones. */
    public void aplicarPaso(float[] paso) {
        this.x = paso[0];
        this.z = paso[1];
        this.angulo = paso[2];
        this.chocando = false;
    }

    /** Mantiene la chata firmemente acoplada al enganche sin desengancharse, aun con colisión o auto detenido. */
    public void mantenerEnganchada(float autoX, float autoZ, float anguloAuto) {
        float frenteAutoX = -(float) Math.sin(anguloAuto);
        float frenteAutoZ = -(float) Math.cos(anguloAuto);
        float atrasAutoX = -frenteAutoX;
        float atrasAutoZ = -frenteAutoZ;
        float engancheX = autoX + atrasAutoX * DISTANCIA_ENGANCHE;
        float engancheZ = autoZ + atrasAutoZ * DISTANCIA_ENGANCHE;

        float difAuto = anguloAuto - this.angulo;
        while (difAuto > Math.PI) difAuto -= (float) (2 * Math.PI);
        while (difAuto < -Math.PI) difAuto += (float) (2 * Math.PI);
        if (difAuto > TOPE_ARTICULACION) {
            this.angulo = anguloAuto - TOPE_ARTICULACION;
        } else if (difAuto < -TOPE_ARTICULACION) {
            this.angulo = anguloAuto + TOPE_ARTICULACION;
        }

        float frenteChataX = -(float) Math.sin(this.angulo);
        float frenteChataZ = -(float) Math.cos(this.angulo);
        this.x = engancheX - frenteChataX * DISTANCIA_CHATA;
        this.z = engancheZ - frenteChataZ * DISTANCIA_CHATA;
    }

    
    public void actualizar(float autoX, float autoZ, float anguloAuto, float velocidadAuto,
                           float direccionVolante, float deltaTime, ValidadorColision validador) {
        float[] paso = calcularPaso(autoX, autoZ, anguloAuto, velocidadAuto, direccionVolante, deltaTime);
        if (validador != null && !validador.puedeCircular(paso[0], paso[1], paso[2])) {
            this.chocando = true; // Chocó con un obstáculo: no avanza a la posición bloqueada
        } else {
            aplicarPaso(paso);
        }
    }

    /** Comprueba si un punto cae dentro del volumen de la chata (para peatones). */
    public boolean dentroDe(float puntoX, float puntoZ) {
        float dx = puntoX - this.x;
        float dz = puntoZ - this.z;
        float frenteX = -(float) Math.sin(this.angulo);
        float frenteZ = -(float) Math.cos(this.angulo);
        float largo = Math.abs(dx * frenteX + dz * frenteZ);
        float ancho = Math.abs(dx * frenteZ - dz * frenteX);
        return largo < MEDIO_LARGO + 0.35f && ancho < MEDIO_ANCHO + 0.35f;
    }

 
    public void dibujar(clase2 juego) {
        // ==================== AQUI SE CREA BOLA DE ENGANCHE EN EL AUTO ====================
        // 1. Bola de enganche en el paragolpes trasero del auto
        juego.pieza(0, 0.40f, 1.38f, 0.16f, 0.12f, 0.18f, 0.15f, 0.16f, 0.18f); // Base soporte en el auto
        juego.pieza(0, 0.45f, 1.45f, 0.10f, 0.12f, 0.10f, 0.85f, 0.85f, 0.88f); // Bola de enganche cromada

        // ==================== AQUI SE CREA LANZA DE REMOLQUE EN V ====================
        // 2. Lanza de remolque conectada desde la chata hacia la bola
        float largoLanza = DISTANCIA_CHATA - 0.90f;
        float centroLanzaZ = -(0.90f + DISTANCIA_CHATA) / 2.0f;
        piezaChata(juego, 0, 0.45f, centroLanzaZ, 0.10f, 0.09f, largoLanza, 0.25f, 0.27f, 0.30f); // Barra central de tiro
        piezaChata(juego, 0, 0.47f, -DISTANCIA_CHATA, 0.15f, 0.14f, 0.15f, 0.18f, 0.20f, 0.22f); // Cabezal acoplador
        piezaChata(juego, -0.25f, 0.45f, centroLanzaZ + 0.10f, 0.07f, 0.07f, largoLanza * 0.88f, 0.25f, 0.27f, 0.30f); // Brazo V izquierdo
        piezaChata(juego, 0.25f, 0.45f, centroLanzaZ + 0.10f, 0.07f, 0.07f, largoLanza * 0.88f, 0.25f, 0.27f, 0.30f); // Brazo V derecho

        // ==================== AQUI SE CREA CHASIS Y PISO DE MADERA ====================
        // 3. Chasis y piso del carrito de carga
        piezaChata(juego, 0, 0.33f, 0, 1.30f, 0.08f, 1.80f, 0.18f, 0.19f, 0.22f); // Bastidor inferior metálico
        piezaChata(juego, 0, 0.38f, 0, 1.25f, 0.04f, 1.76f, 0.62f, 0.45f, 0.28f); // Piso de madera tratada

        // ==================== AQUI SE CREAN BARANDAS ROJAS DEL CAJON ====================
        // 4. Barandas del cajón de la chata (rojo a juego con la carrocería del auto)
        piezaChata(juego, -0.65f, 0.60f, 0, 0.08f, 0.40f, 1.80f, 0.85f, 0.22f, 0.14f); // Baranda lateral izquierda
        piezaChata(juego, 0.65f, 0.60f, 0, 0.08f, 0.40f, 1.80f, 0.85f, 0.22f, 0.14f); // Baranda lateral derecha
        piezaChata(juego, 0, 0.60f, -0.86f, 1.38f, 0.40f, 0.08f, 0.85f, 0.22f, 0.14f); // Panel frontal
        piezaChata(juego, 0, 0.60f, 0.86f, 1.38f, 0.40f, 0.08f, 0.80f, 0.18f, 0.12f); // Compuerta trasera

        // Molduras superiores protectoras de acero
        piezaChata(juego, -0.65f, 0.81f, 0, 0.09f, 0.04f, 1.82f, 0.22f, 0.23f, 0.26f);
        piezaChata(juego, 0.65f, 0.81f, 0, 0.09f, 0.04f, 1.82f, 0.22f, 0.23f, 0.26f);
        piezaChata(juego, 0, 0.81f, -0.86f, 1.39f, 0.04f, 0.09f, 0.22f, 0.23f, 0.26f);
        piezaChata(juego, 0, 0.81f, 0.86f, 1.39f, 0.04f, 0.09f, 0.22f, 0.23f, 0.26f);

        // ==================== AQUI SE CREAN RUEDAS, LLANTAS Y GUARDABARROS DE LA CHATA ====================
        // 5. Ruedas, llantas y guardabarros
        piezaChata(juego, 0, 0.35f, 0, 1.55f, 0.08f, 0.08f, 0.20f, 0.20f, 0.22f); // Eje transversal
        piezaChata(juego, -0.78f, 0.35f, 0, 0.20f, 0.54f, 0.54f, 0.055f, 0.065f, 0.08f); // Neumático izquierdo
        piezaChata(juego, -0.81f, 0.35f, 0, 0.08f, 0.30f, 0.30f, 0.75f, 0.75f, 0.80f); // Llanta plateada izquierda
        piezaChata(juego, 0.78f, 0.35f, 0, 0.20f, 0.54f, 0.54f, 0.055f, 0.065f, 0.08f); // Neumático derecho
        piezaChata(juego, 0.81f, 0.35f, 0, 0.08f, 0.30f, 0.30f, 0.75f, 0.75f, 0.80f); // Llanta plateada derecha

        // Guardabarros sobre las ruedas
        piezaChata(juego, -0.78f, 0.65f, 0, 0.22f, 0.06f, 0.64f, 0.20f, 0.22f, 0.25f);
        piezaChata(juego, 0.78f, 0.65f, 0, 0.22f, 0.06f, 0.64f, 0.20f, 0.22f, 0.25f);
    }

    /** Dibuja una pieza en las coordenadas locales de la chata. */
    private void piezaChata(clase2 juego, float lx, float ly, float lz, float sx, float sy, float sz,
                            float r, float g, float b) {
        juego.piezaEn(this.x, this.z, this.angulo, lx, ly, lz, sx, sy, sz, r, g, b);
    }
}
