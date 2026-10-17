package com.graphics; // Comparte el paquete con las cinco lecciones.

/**
 * VEHÍCULO AUTÓNOMO DE CLASE5.
 * Guarda posición, orientación y velocidad propias y recorre en bucle una ruta de cruces.
 * Circula por el carril derecho: cada cruce se desplaza CARRIL unidades hacia la derecha del sentido de marcha.
 * No usa OpenGL, por eso las pruebas pueden simular su recorrido sin abrir una ventana.
 */
class VehiculoAutonomo {

    static final float CARRIL = 2.3f; // Distancia del centro de la calle al centro del carril derecho.
    static final float GIRO_MAXIMO = 2.4f; // Radianes por segundo que puede girar el volante.

    float x; // Posición X actual del vehículo.
    float z; // Posición Z actual del vehículo.
    float angulo; // Orientación en radianes; cero apunta hacia -Z, igual que el auto del jugador.
    float velocidad; // Velocidad actual en unidades por segundo.
    final float crucero; // Velocidad deseada en tramos rectos.
    final float rojo, verde, azul; // Color de la carrocería.
    final float[][] puntos; // Puntos de paso ya desplazados al carril derecho.
    int siguiente = 1; // Índice del punto hacia el que se dirige.
    int vueltasCompletas = 0; // Cuenta las veces que cerró la ruta; útil para las pruebas.

    /** Crea un vehículo a partir de cruces (X, Z) recorridos en orden y que forman un circuito cerrado. */
    VehiculoAutonomo(float[][] cruces, float crucero, float rojo, float verde, float azul) {
        this.crucero = crucero; // Guarda la velocidad de crucero.
        this.rojo = rojo; // Guarda la componente roja.
        this.verde = verde; // Guarda la componente verde.
        this.azul = azul; // Guarda la componente azul.
        int cantidad = cruces.length; // Número de esquinas de la ruta.
        puntos = new float[cantidad][]; // Reserva un punto de carril por esquina.
        for (int indice = 0; indice < cantidad; indice++) { // Calcula el punto de carril de cada esquina.
            float[] anterior = cruces[(indice - 1 + cantidad) % cantidad]; // Esquina desde la que se llega.
            float[] actual = cruces[indice]; // Esquina que se está ajustando.
            float[] proxima = cruces[(indice + 1) % cantidad]; // Esquina hacia la que se sale.
            float[] entrada = direccion(anterior, actual); // Sentido de marcha al llegar.
            float[] salida = direccion(actual, proxima); // Sentido de marcha al salir.
            float puntoX = actual[0] - entrada[1] * CARRIL; // La derecha de (dx, dz) es (-dz, dx).
            float puntoZ = actual[1] + entrada[0] * CARRIL; // Desplaza el cruce al carril de llegada.
            boolean recto = entrada[0] * salida[0] + entrada[1] * salida[1] > 0.99f; // Detecta que no hay giro.
            if (!recto) { // En una esquina también se desplaza al carril de salida.
                puntoX += -salida[1] * CARRIL; // Suma la derecha del tramo de salida.
                puntoZ += salida[0] * CARRIL; // Así el punto queda en la esquina interior de ambos carriles.
            }
            puntos[indice] = new float[] {puntoX, puntoZ}; // Guarda el punto de paso definitivo.
        }
        x = puntos[0][0]; // Comienza en la primera esquina.
        z = puntos[0][1]; // Comienza en la primera esquina.
        float[] haciaSiguiente = direccion(puntos[0], puntos[1]); // Dirección del primer tramo.
        angulo = (float) Math.atan2(-haciaSiguiente[0], -haciaSiguiente[1]); // Mira hacia el primer tramo.
    }

    /** Devuelve el vector unitario que va de un punto a otro en el plano XZ. */
    private static float[] direccion(float[] desde, float[] hasta) {
        float dx = hasta[0] - desde[0]; // Diferencia en X.
        float dz = hasta[1] - desde[1]; // Diferencia en Z.
        float largo = (float) Math.hypot(dx, dz); // Longitud del tramo.
        return new float[] {dx / largo, dz / largo}; // Vector de longitud uno.
    }

    /** Componente X del frente del vehículo. */
    float frenteX() {
        return -(float) Math.sin(angulo); // Misma convención que clase2.
    }

    /** Componente Z del frente del vehículo. */
    float frenteZ() {
        return -(float) Math.cos(angulo); // Con ángulo cero apunta hacia -Z.
    }

    /** Indica si un punto está delante, dentro de un pasillo de cierto largo y ancho. */
    boolean tieneDelante(float puntoX, float puntoZ, float largo, float anchoMedio) {
        float dx = puntoX - x; // Vector hacia el punto en X.
        float dz = puntoZ - z; // Vector hacia el punto en Z.
        float adelante = dx * frenteX() + dz * frenteZ(); // Distancia medida a lo largo del frente.
        float lateral = Math.abs(dx * frenteZ() - dz * frenteX()); // Distancia perpendicular al frente.
        return adelante > 0 && adelante < largo && lateral < anchoMedio; // Está en el pasillo frontal.
    }

    /** Avanza el vehículo; detenerse = true cuando hay un obstáculo delante. */
    void actualizar(float deltaTime, boolean detenerse) {
        int cantidad = puntos.length; // Número de puntos de la ruta.
        float[] objetivo = puntos[siguiente]; // Punto hacia el que conduce.
        float[] previo = puntos[(siguiente - 1 + cantidad) % cantidad]; // Inicio del tramo actual.
        float tramoX = objetivo[0] - previo[0]; // Dirección del tramo en X.
        float tramoZ = objetivo[1] - previo[1]; // Dirección del tramo en Z.
        float dx = objetivo[0] - x; // Vector hacia el objetivo en X.
        float dz = objetivo[1] - z; // Vector hacia el objetivo en Z.
        boolean cerca = dx * dx + dz * dz < 1.2f * 1.2f; // Llegó prácticamente al punto.
        boolean pasado = dx * tramoX + dz * tramoZ < 0; // Ya cruzó la línea perpendicular del punto.
        if (cerca || pasado) { // Cambia al siguiente tramo de la ruta.
            siguiente = (siguiente + 1) % cantidad; // Avanza de forma circular.
            if (siguiente == 1) { // Volver al segundo punto significa que completó el circuito.
                vueltasCompletas++; // Registra la vuelta.
            }
            objetivo = puntos[siguiente]; // Lee el nuevo objetivo.
            dx = objetivo[0] - x; // Recalcula el vector en X.
            dz = objetivo[1] - z; // Recalcula el vector en Z.
        }

        float deseado = (float) Math.atan2(-dx, -dz); // Ángulo que apuntaría directo al objetivo.
        float diferencia = deseado - angulo; // Cuánto falta girar.
        while (diferencia > Math.PI) { // Elige el giro más corto.
            diferencia -= (float) (2 * Math.PI); // Resta una vuelta completa.
        }
        while (diferencia < -Math.PI) { // Elige el giro más corto en sentido contrario.
            diferencia += (float) (2 * Math.PI); // Suma una vuelta completa.
        }
        float giroPermitido = GIRO_MAXIMO * deltaTime; // Límite de giro para este cuadro.
        angulo += Math.max(-giroPermitido, Math.min(giroPermitido, diferencia)); // Gira suavemente hacia el objetivo.

        float objetivoVelocidad = crucero * Math.max(0.3f, (float) Math.cos(diferencia)); // Reduce al girar.
        float distancia = (float) Math.hypot(dx, dz); // Distancia al próximo punto.
        if (distancia < 8 && esGiro(siguiente)) { // Se acerca a una esquina.
            objetivoVelocidad = Math.min(objetivoVelocidad, 4.5f); // Frena antes de doblar.
        }
        if (detenerse) { // Hay un vehículo delante.
            objetivoVelocidad = 0; // Se detiene hasta que el paso quede libre.
        }
        float cambio = objetivoVelocidad > velocidad ? 6 * deltaTime : 12 * deltaTime; // Acelera suave, frena más fuerte.
        if (Math.abs(objetivoVelocidad - velocidad) <= cambio) { // Puede alcanzar la velocidad deseada en este cuadro.
            velocidad = objetivoVelocidad; // Ajusta exactamente.
        } else { // Todavía falta para alcanzarla.
            velocidad += Math.signum(objetivoVelocidad - velocidad) * cambio; // Se acerca a la velocidad deseada.
        }

        x += frenteX() * velocidad * deltaTime; // Avanza en X según su orientación.
        z += frenteZ() * velocidad * deltaTime; // Avanza en Z según su orientación.
    }

    /** Indica si en el punto de la ruta hay un cambio de dirección. */
    private boolean esGiro(int indice) {
        int cantidad = puntos.length; // Número de puntos.
        float[] entrada = direccion(puntos[(indice - 1 + cantidad) % cantidad], puntos[indice]); // Tramo que llega.
        float[] salida = direccion(puntos[indice], puntos[(indice + 1) % cantidad]); // Tramo que sale.
        return entrada[0] * salida[0] + entrada[1] * salida[1] < 0.99f; // Si no son paralelos, hay giro.
    }
}
