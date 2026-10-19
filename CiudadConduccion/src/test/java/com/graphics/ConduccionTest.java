package com.graphics; // Permite probar los métodos protegidos del juego desde el mismo paquete.

import java.util.ArrayDeque; // Cola para recorrer las calles por anchura.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de la ciudad ampliada sin abrir una ventana OpenGL. */
public class ConduccionTest extends TestCase {

    /** Comprueba el tamaño mínimo, la cantidad de manzanas y que el mundo se adapte al mapa. */
    public void testCiudadAmpliada() {
        assertTrue(clase1.MAPA.length >= 11); // Al menos 11 filas.
        int edificios = 0; // Cuenta las manzanas con edificio.
        int parques = 0; // Cuenta los parques.
        for (int[] fila : clase1.MAPA) { // Recorre las filas.
            assertTrue(fila.length >= 11); // Al menos 11 columnas.
            for (int celda : fila) { // Recorre las celdas.
                if (celda == 1) { // Edificio.
                    edificios++; // Lo cuenta.
                }
                if (celda == 2) { // Parque.
                    parques++; // Lo cuenta.
                }
            }
        }
        assertTrue(edificios >= 12); // Exige al menos 12 manzanas con edificios.
        assertTrue(parques >= 4); // Exige al menos 4 parques.
        assertEquals(10f, clase1.CELDA, 0f); // Conserva el tamaño original de cada celda.
        assertEquals(clase1.MAPA.length * clase1.CELDA / 2, clase1.LIMITE, 0f); // Límites calculados con el mapa nuevo.
    }

    /** Comprueba que las calles conectan la salida con todas las entregas (búsqueda en anchura). */
    public void testCallesYDestinosTransitables() {
        clase2 juego = new clase2(); // Crea el estado del juego sin ejecutar run().
        for (float z = -50; z <= 50; z += 0.25f) { // Recorre la calle oeste completa.
            assertTrue(juego.puedeCircular(-50, z)); // Cada punto del tramo debe ser transitable.
        }
        for (float x = -50; x <= 50; x += 0.25f) { // Recorre la avenida del borde norte.
            assertTrue(juego.puedeCircular(x, -50)); // Cada punto del tramo debe ser transitable.
        }
        int n = clase1.MAPA.length; // Tamaño del mapa.
        boolean[][] visitada = new boolean[n][n]; // Celdas de calle alcanzadas.
        ArrayDeque<int[]> cola = new ArrayDeque<>(); // Celdas pendientes de expandir.
        int filaInicio = celda(clase2.INICIO_Z); // Celda de salida.
        int columnaInicio = celda(clase2.INICIO_X); // Celda de salida.
        cola.add(new int[] {filaInicio, columnaInicio}); // Comienza en la salida.
        visitada[filaInicio][columnaInicio] = true; // La marca como alcanzada.
        int[][] pasos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // Vecinos por calle.
        while (!cola.isEmpty()) { // Expande mientras queden celdas.
            int[] actual = cola.poll(); // Celda actual.
            for (int[] paso : pasos) { // Prueba los cuatro vecinos.
                int f = actual[0] + paso[0]; // Fila vecina.
                int c = actual[1] + paso[1]; // Columna vecina.
                if (f >= 0 && c >= 0 && f < n && c < n && !visitada[f][c] && clase1.MAPA[f][c] == 0) { // Calle nueva.
                    visitada[f][c] = true; // La marca.
                    cola.add(new int[] {f, c}); // La expande después.
                }
            }
        }
        for (float[] destino : clase4.DESTINOS) { // Revisa cada entrega.
            assertTrue(juego.puedeCircular(destino[0], destino[1])); // El auto cabe en el destino.
            assertTrue(visitada[celda(destino[1])][celda(destino[0])]); // Y se llega por calles desde la salida.
        }
    }

    /** Convierte una coordenada del mundo al índice de la celda. */
    private static int celda(float coordenada) {
        return (int) Math.floor((coordenada + clase1.LIMITE) / clase1.CELDA); // Inverso de centro().
    }

    /** Comprueba edificios, parques, aceras y bordes del mapa ampliado. */
    public void testObstaculosYLimites() {
        clase2 juego = new clase2(); // Crea un vehículo con su radio de colisión inicial.
        assertFalse(juego.puedeCircular(-40, -40)); // Rechaza el centro de un edificio del sector nuevo.
        assertFalse(juego.puedeCircular(0, -40)); // Rechaza el centro de un parque del sector norte.
        assertFalse(juego.puedeCircular(-45.5f, -40)); // Rechaza una posición cuyo círculo invade la acera.
        assertFalse(juego.puedeCircular(54, 0)); // Rechaza una posición demasiado cercana al borde este.
        assertFalse(juego.puedeCircular(0, -56)); // Rechaza una posición exterior al borde norte.
        assertTrue(juego.puedeCircular(-47, -40)); // Acepta una posición con separación suficiente de la acera.
        assertTrue(juego.puedeCircular(50, 50)); // La esquina sureste nueva es transitable.
    }

    /** Comprueba que hay al menos nueve farolas y que todas están sobre aceras de manzanas. */
    public void testFarolas() {
        assertTrue(clase3.LUCES.length >= 9); // Mínimo exigido.
        int fuera = 0; // Farolas en el anillo de sectores nuevos.
        for (float[] luz : clase3.LUCES) { // Revisa cada farola.
            assertTrue(clase1.MAPA[celda(luz[2])][celda(luz[0])] != 0); // Está sobre una manzana (acera).
            if (Math.max(Math.abs(luz[0]), Math.abs(luz[2])) > 25) { // Fuera del centro original.
                fuera++; // Cuenta las de los sectores nuevos.
            }
        }
        assertTrue(fuera >= 9); // Al menos nueve farolas en los sectores nuevos.
    }

    /** Comprueba que el reinicio restaura la posición, detiene el vehículo y reinicia el recorrido. */
    public void testReinicio() {
        clase5 juego = new clase5(); // Crea la versión final sin inicializar OpenGL.
        juego.autoX = 12; // Simula que el auto se desplazó horizontalmente.
        juego.autoZ = 4; // Simula un desplazamiento en profundidad.
        juego.velocidad = 10; // Simula que el auto está avanzando.
        juego.angulo = 2; // Simula que el vehículo cambió de orientación.
        juego.entregas = 3; // Simula entregas completadas.
        juego.tiempo = 50; // Simula tiempo transcurrido.
        juego.trafico.get(0).x = 0; // Simula que el tráfico se movió.
        juego.reiniciar(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(clase2.INICIO_X, juego.autoX, 0f); // Recupera la coordenada X inicial.
        assertEquals(clase2.INICIO_Z, juego.autoZ, 0f); // Recupera la coordenada Z inicial.
        assertEquals(0f, juego.velocidad, 0f); // El vehículo queda detenido.
        assertEquals(0f, juego.angulo, 0f); // Recupera la dirección frontal inicial.
        assertEquals(0, juego.entregas); // Vuelve a la primera entrega.
        assertEquals(0f, juego.tiempo, 0f); // Reinicia el cronómetro.
        assertTrue(juego.trafico.get(0).x != 0); // El tráfico vuelve a su posición inicial.
    }

    /** Simula dos minutos de tráfico: nunca atraviesa manzanas ni sale del mapa y completa vueltas con giros. */
    public void testTraficoAutonomo() {
        clase5 juego = new clase5(); // Crea la versión final sin ventana.
        clase2 ciudad = new clase2(); // Comprueba solo manzanas y bordes, sin contar el propio tráfico.
        assertTrue(juego.trafico.size() >= 3); // Exige al menos tres vehículos.
        juego.autoX = 1000; // Retira al jugador para que no detenga el tráfico.
        float paso = 1 / 60f; // Simula 60 cuadros por segundo.
        for (int cuadro = 0; cuadro < 60 * 240; cuadro++) { // Cuatro minutos simulados, con esperas en los semáforos.
            juego.reloj += paso; // Avanza el ciclo de los semáforos.
            juego.actualizarTrafico(paso); // Mueve todos los vehículos.
            for (VehiculoAutonomo v : juego.trafico) { // Revisa cada vehículo.
                assertTrue("vehiculo en (" + v.x + ", " + v.z + ")", ciudad.puedeCircular(v.x, v.z)); // Sin invadir aceras ni bordes.
            }
        }
        for (VehiculoAutonomo v : juego.trafico) { // Revisa el progreso de cada vehículo.
            assertTrue(v.vueltasCompletas >= 1); // Recorrió su ruta completa, con sus giros, y siguió circulando.
        }
    }

    /** Comprueba que un vehículo autónomo se detiene en rojo antes del cruce y continúa con verde. */
    public void testTraficoRespetaSemaforo() {
        clase5 juego = new clase5(); // Crea la versión final sin ventana.
        juego.autoX = 1000; // Retira al jugador.
        VehiculoAutonomo taxi = juego.trafico.get(0); // Circula hacia el este por la calle norte.
        float cruceX = clase1.centro(2); // Primer cruce con semáforo en su camino (manzana fila 1, columna 1).
        juego.reloj = 0; // Fase roja para ese semáforo.
        assertEquals(clase4.ROJO, juego.faseSemaforo(1, 1, false)); // Confirma que está en rojo para la calle horizontal.
        for (int cuadro = 0; cuadro < 60 * 10; cuadro++) { // Diez segundos con el reloj detenido en rojo.
            juego.actualizarTrafico(1 / 60f); // Mueve el tráfico.
        }
        assertTrue(taxi.x < cruceX - 8); // Se detuvo antes del paso peatonal.
        assertEquals(0f, taxi.velocidad, 0.01f); // Está quieto.
        juego.reloj = 8; // Cambia a verde.
        assertEquals(clase4.VERDE, juego.faseSemaforo(1, 1, false)); // Confirma el verde.
        for (int cuadro = 0; cuadro < 60 * 5; cuadro++) { // Cinco segundos con verde.
            juego.actualizarTrafico(1 / 60f); // Mueve el tráfico.
        }
        assertTrue(taxi.x > cruceX); // Cruzó la intersección.
    }

    /** Comprueba que los semáforos añadidos están intercalados y los originales del centro sincronizados. */
    public void testSemaforosIntercalados() {
        clase5 juego = new clase5(); // Crea la versión final sin ventana.
        juego.reloj = 0; // Instante inicial del ciclo.
        assertEquals(juego.cicloSemaforo(3, 3, false), juego.cicloSemaforo(5, 5, false), 0f); // Dos originales del centro: mismo ciclo.
        assertEquals(juego.cicloSemaforo(3, 3, false), juego.cicloSemaforo(7, 7, false), 0f); // Otro original: mismo ciclo.
        assertFalse(juego.cicloSemaforo(1, 1, false) == juego.cicloSemaforo(1, 3, false)); // Dos añadidos vecinos: ciclos distintos.
        assertFalse(juego.cicloSemaforo(1, 3, false) == juego.cicloSemaforo(1, 5, false)); // Siguiente par vecino: ciclos distintos.
    }

    /** En cada cruce, la calle horizontal y la vertical nunca tienen paso (verde o amarillo) al mismo tiempo. */
    public void testSemaforosDeFilaYColumnaOpuestos() {
        clase5 juego = new clase5(); // Versión final sin ventana.
        boolean huboVerdeVertical = false; // La calle vertical también recibe su turno.
        for (float t = 0; t < 30; t += 0.05f) { // Más de dos ciclos completos.
            juego.reloj = t; // Avanza el tiempo.
            for (int fila = 1; fila < clase1.MAPA.length; fila += 2) { // Manzanas.
                for (int columna = 1; columna < clase1.MAPA.length; columna += 2) { // Manzanas.
                    boolean pasaFila = juego.faseSemaforo(fila, columna, false) != clase4.ROJO; // Paso este-oeste.
                    boolean pasaColumna = juego.faseSemaforo(fila, columna, true) != clase4.ROJO; // Paso norte-sur.
                    assertFalse(pasaFila && pasaColumna); // Nunca ambos a la vez.
                    huboVerdeVertical |= juego.faseSemaforo(fila, columna, true) == clase4.VERDE; // Registra el verde vertical.
                }
            }
        }
        assertTrue(huboVerdeVertical); // Las calles verticales también avanzan.
    }

    /** Un auto que sube hacia el norte frena con el rojo de la calle vertical, aunque la horizontal tenga verde. */
    public void testTraficoVerticalRespetaSuSemaforo() {
        clase5 juego = new clase5(); // Versión final sin ventana.
        juego.autoX = 1000; // Retira al jugador.
        juego.reloj = 4; // Manzana (9, 1), desfasada 14/3 s: horizontal en verde, vertical en rojo.
        assertEquals(clase4.VERDE, juego.faseSemaforo(9, 1, false)); // Verde para la calle horizontal.
        assertEquals(clase4.ROJO, juego.faseSemaforo(9, 1, true)); // Rojo para la calle vertical.
        float cruceX = clase1.centro(2); // Calle vertical al este de la manzana (9, 1).
        float cruceZ = clase1.centro(8); // Cruce noreste de esa manzana.
        float[][] ruta = {{cruceX, clase1.centro(10)}, {cruceX, clase1.centro(0)}, {clase1.centro(10), clase1.centro(0)}, {clase1.centro(10), clase1.centro(10)}}; // Sube por esa calle.
        VehiculoAutonomo v = new VehiculoAutonomo(ruta, 8, 1, 1, 1); // Vehículo de prueba.
        v.z = cruceZ + 11.5f; // A 11.5 unidades del cruce, dentro de la zona de frenado.
        v.angulo = 0; // Mira al norte.
        assertTrue(juego.debeFrenarPorSemaforo(v)); // Debe detenerse con su rojo.
        juego.reloj = 11; // Ahora la vertical tiene verde.
        assertEquals(clase4.VERDE, juego.faseSemaforo(9, 1, true)); // Verde vertical.
        assertFalse(juego.debeFrenarPorSemaforo(v)); // Puede seguir.
    }

    /** El auto cabe junto a la acera sin que un círculo grande lo detenga antes de tocarla, y la ochava deja doblar más cerca de la esquina. */
    public void testColisionAjustadaALaAcera() {
        clase2 juego = new clase2(); // Auto mirando al norte.
        float bordeAcera = clase1.centro(1) - clase1.CELDA / 2; // Borde oeste de la manzana de la columna 1 (-45).
        assertTrue(juego.puedeCircular(bordeAcera - 1.2f, clase1.centro(7))); // A 1.2 del borde ya cabe (antes se necesitaban 1.65).
        assertFalse(juego.puedeCircular(bordeAcera - 0.9f, clase1.centro(7))); // Más cerca, la rueda tocaría la acera.
        float esquinaX = clase1.centro(1) + clase1.CELDA / 2; // Esquina sureste de la manzana (9, 1): x = -35.
        float esquinaZ = clase1.centro(9) + clase1.CELDA / 2; // z = 45.
        float a = clase2.MEDIO_ANCHO + clase2.HOLGURA; // Medio ancho con holgura.
        float l = clase2.MEDIO_LARGO + clase2.HOLGURA; // Medio largo con holgura.
        assertTrue(juego.puedeCircular(esquinaX - 0.3f + a, esquinaZ - 0.3f + l)); // La esquina del auto entra en el cuadrado, pero no en la ochava.
        assertFalse(juego.puedeCircular(esquinaX - 0.8f + a, esquinaZ - 0.8f + l)); // Más adentro sí toca la acera.
    }

    /** Con el semáforo siempre en verde para los autos, el peatón nunca baja a la calle. */
    public void testPeatonNoCruzaConVerde() {
        Peaton peaton = new Peaton(5, 5, 7, 1); // Peatón en la manzana central.
        for (int cuadro = 0; cuadro < 60 * 120; cuadro++) { // Dos minutos simulados.
            peaton.actualizar(1 / 60f, (fila, columna, vertical) -> 9, (x, z) -> false); // Segundo 9 del ciclo: verde para los autos.
            assertTrue(peaton.sobreAcera()); // Siempre sobre la acera.
        }
        assertEquals(0, peaton.cruces); // No cruzó ninguna calle.
    }

    /** Con el rojo para los autos, el peatón cruza por los pasos peatonales y recorre otras manzanas. */
    public void testPeatonCruzaConRojo() {
        clase5 juego = new clase5(); // Usa los semáforos reales de la ciudad.
        Peaton peaton = juego.peaton; // Peatón del juego.
        clase2 ciudad = new clase2(); // Para comprobar que nunca entra a un edificio.
        for (int cuadro = 0; cuadro < 60 * 300; cuadro++) { // Cinco minutos simulados.
            juego.reloj += 1 / 60f; // Avanza el ciclo de los semáforos.
            peaton.actualizar(1 / 60f, juego::cicloSemaforo, juego::autoEn); // Camina.
            if (peaton.cruzando) { // Mientras está en la calle...
                assertTrue(Math.abs(peaton.x) <= clase1.LIMITE && Math.abs(peaton.z) <= clase1.LIMITE); // ...no sale del mapa.
            }
            float cx = clase1.centro((int) Math.floor((peaton.x + clase1.LIMITE) / clase1.CELDA)); // Centro de su celda.
            float cz = clase1.centro((int) Math.floor((peaton.z + clase1.LIMITE) / clase1.CELDA)); // Centro de su celda.
            boolean dentroEdificio = Math.abs(peaton.x - cx) < 3.5f && Math.abs(peaton.z - cz) < 3.5f && !ciudad.puedeCircular(cx, cz); // Interior de una manzana.
            assertFalse(dentroEdificio); // Camina por la acera, no a través del edificio.
        }
        assertTrue(peaton.cruces >= 3); // Cruzó varias calles con el semáforo en rojo para los autos.
    }

    /** Con el auto del jugador detenido sobre un paso peatonal, el peatón nunca lo atraviesa. */
    public void testPeatonNoAtraviesaAuto() {
        clase5 juego = new clase5(); // Versión final sin ventana.
        juego.autoX = clase1.centro(2); // Calle vertical al este de la manzana inicial del peatón.
        juego.autoZ = clase1.centro(9) - 4; // Justo sobre el paso peatonal de esa calle.
        juego.angulo = 0; // Auto mirando al norte, atravesado en el paso.
        Peaton peaton = juego.peaton; // Peatón del juego.
        for (int cuadro = 0; cuadro < 60 * 300; cuadro++) { // Cinco minutos simulados.
            juego.reloj += 1 / 60f; // Avanza los semáforos.
            peaton.actualizar(1 / 60f, juego::cicloSemaforo, juego::autoEn); // Camina.
            float dx = peaton.x - juego.autoX; // Distancia al auto en X.
            float dz = peaton.z - juego.autoZ; // Distancia al auto en Z.
            assertFalse(Math.abs(dx) < 0.83f && Math.abs(dz) < 1.3f); // Nunca queda dentro de la carrocería.
        }
    }

    /** Si hay autos en todo el ancho de las calles, el peatón se detiene en el borde y no baja al asfalto. */
    public void testPeatonSeDetieneAnteAuto() {
        clase5 juego = new clase5(); // Usa los semáforos reales.
        Peaton peaton = new Peaton(5, 5, 7, 3); // Peatón en la manzana central.
        boolean seDetuvo = false; // Registra si alguna vez esperó a un auto.
        for (int cuadro = 0; cuadro < 60 * 300; cuadro++) { // Cinco minutos simulados.
            juego.reloj += 1 / 60f; // Avanza los semáforos.
            peaton.actualizar(1 / 60f, juego::cicloSemaforo, (x, z) -> !enManzana(x, z)); // Toda la calle está ocupada por autos.
            assertTrue(enManzana(peaton.x, peaton.z)); // Nunca pisa el asfalto.
            seDetuvo |= peaton.detenido; // Recuerda si se detuvo.
        }
        assertTrue(seDetuvo); // Intentó cruzar con rojo y se detuvo ante los autos.
        assertEquals(0, peaton.cruces); // Por eso no cruzó ninguna calle.
    }

    /** Indica si un punto está dentro de una manzana (acera incluida). */
    private static boolean enManzana(float x, float z) {
        int f = celda(z); // Fila de la celda.
        int c = celda(x); // Columna de la celda.
        return f >= 0 && c >= 0 && f < clase1.MAPA.length && c < clase1.MAPA.length && clase1.MAPA[f][c] != 0; // No es calle.
    }

    /** Comprueba que cada negocio está en un edificio de la altura indicada. */
    public void testNegociosSegunAltura() {
        int[] edificiosPorGrupo = new int[4]; // 2-3, 4-5, 6 y 7-8 ventanas.
        for (int fila = 0; fila < clase1.MAPA.length; fila++) { // Recorre el mapa.
            for (int columna = 0; columna < clase1.MAPA.length; columna++) { // Recorre el mapa.
                if (clase1.MAPA[fila][columna] == 1) { // Solo edificios.
                    int ventanas = clase5.filasDeVentanas(fila, columna); // Altura en filas de ventanas.
                    assertEquals(clase1.PISOS[fila][columna], ventanas); // La tabla PISOS coincide con las ventanas dibujadas.
                    edificiosPorGrupo[ventanas <= 3 ? 0 : ventanas <= 5 ? 1 : ventanas == 6 ? 2 : 3]++; // Clasifica.
                }
            }
        }
        assertEquals(8, edificiosPorGrupo[0]); // 8 edificios de 2-3 ventanas.
        assertEquals(4, edificiosPorGrupo[1]); // 4 de 4-5 ventanas.
        assertEquals(4, edificiosPorGrupo[2]); // 4 de 6 ventanas.
        assertEquals(3, edificiosPorGrupo[3]); // 3 de 7-8 ventanas.
        for (clase5.Negocio negocio : clase5.NEGOCIOS) { // Revisa cada letrero.
            int ventanas = clase5.filasDeVentanas(negocio.fila(), negocio.columna()); // Altura del edificio.
            String nombre = negocio.nombre(); // Negocio asignado.
            if (ventanas <= 3) { // Edificios bajos.
                assertTrue(java.util.Arrays.asList(clase5.COMERCIOS).contains(nombre)); // Comercios.
            } else if (ventanas <= 5) { // Edificios medianos.
                assertTrue(java.util.Arrays.asList(clase5.SERVICIOS).contains(nombre)); // Servicios.
            } else if (ventanas == 6) { // Seis pisos.
                assertTrue(java.util.Arrays.asList(clase5.EDIFICIOS_ALTOS).contains(nombre)); // Hospital, oficinas o apartamentos.
            } else { // Siete u ocho pisos.
                assertEquals("HOTEL", nombre); // Hotel.
            }
        }
    }

    /** Comprueba que el avión vuela de este a oeste y vuelve a pasar cada 30 segundos. */
    public void testAvionCada30Segundos() {
        clase5 juego = new clase5(); // Versión final sin ventana.
        juego.actualizarAvion(0); // Posición inicial.
        float inicio = juego.avionX; // Comienza al este, fuera del mapa.
        assertTrue(inicio > clase1.LIMITE); // Fuera del mapa por el este.
        juego.actualizarAvion(10); // Diez segundos después...
        assertTrue(juego.avionX < inicio); // ...avanzó hacia el oeste (-X).
        juego.actualizarAvion(20); // A los 30 segundos...
        assertEquals(inicio, juego.avionX, 0.001f); // ...empieza una nueva pasada desde el este.
    }

    /** Comprueba que el jugador no puede avanzar a través de un vehículo autónomo. */
    public void testColisionConTrafico() {
        clase5 juego = new clase5(); // Crea la versión final sin ventana.
        VehiculoAutonomo v = juego.trafico.get(0); // Toma un vehículo.
        juego.autoX = v.x; // Coloca al jugador detrás del vehículo.
        juego.autoZ = v.z + 4; // A cuatro unidades.
        assertFalse(juego.puedeCircular(v.x, v.z + 2)); // No puede acercarse a menos de 2.8 unidades.
        assertTrue(juego.puedeCircular(v.x, v.z + 5)); // Sí puede alejarse.
    }

    /** Comprueba que la chata sigue al auto, gira con él y se detiene sin traspasarlo ni rebasarlo. */
    public void testChataGiraYSeDetieneSinRebasar() {
        clase2 juego = new clase2();
        juego.reiniciar();
        float distEsperada = clase2.DISTANCIA_ENGANCHE + clase2.DISTANCIA_CHATA;
        assertEquals(juego.autoX, juego.chataX, 0.001f);
        assertEquals(juego.autoZ + distEsperada, juego.chataZ, 0.001f);
        assertEquals(0f, juego.anguloChata, 0.001f);

        // 1. Avanzar en línea recta: la chata se mantiene alineada detrás
        juego.velocidad = 8f;
        for (int i = 0; i < 30; i++) {
            juego.actualizarChata(1 / 60f, 0);
        }
        assertEquals(0f, juego.anguloChata, 0.01f);
        assertTrue(juego.chataZ > juego.autoZ); // Sigue detrás del auto en Z

        // 2. El auto dobla hacia la izquierda (angulo = 0.8) y avanza: la chata gira acompañando la trayectoria
        juego.angulo = 0.8f; // El auto dobló
        float frenteX = -(float) Math.sin(juego.angulo);
        float frenteZ = -(float) Math.cos(juego.angulo);
        for (int i = 0; i < 90; i++) {
            juego.autoX += frenteX * juego.velocidad * (1 / 60f);
            juego.autoZ += frenteZ * juego.velocidad * (1 / 60f);
            juego.actualizarChata(1 / 60f, 1);
        }
        assertTrue("La chata debió girar en la dirección del auto", juego.anguloChata > 0.4f);

        // 3. El auto se detiene (velocidad = 0): la chata también se detiene
        juego.velocidad = 0f;
        float chataXDetenida = juego.chataX;
        float chataZDetenida = juego.chataZ;
        float anguloDetenido = juego.anguloChata;
        for (int i = 0; i < 60; i++) {
            juego.actualizarChata(1 / 60f, 0);
        }
        assertEquals(chataXDetenida, juego.chataX, 0.0001f);
        assertEquals(chataZDetenida, juego.chataZ, 0.0001f);
        assertEquals(anguloDetenido, juego.anguloChata, 0.0001f);

        // 4. Comprobar que nunca rebasa ni traspasa al auto (la distancia al centro del auto es siempre >= DISTANCIA_ENGANCHE)
        float dx = juego.autoX - juego.chataX;
        float dz = juego.autoZ - juego.chataZ;
        float separacion = (float) Math.hypot(dx, dz);
        assertTrue("La chata no traspasa al auto", separacion >= clase2.DISTANCIA_ENGANCHE);
    }

    /** Comprueba que si la chata colisiona con un obstáculo o pared, no avanza. */
    public void testChataColisionBloqueaAvance() {
        Chata chata = new Chata(0, 0);
        float posInicialX = chata.x;
        float posInicialZ = chata.z;

        // Si el validador indica que la posición está bloqueada (choca con acera/edificio)
        chata.actualizar(10, 10, 0, 5, 0, 1 / 60f, (x, z, giro) -> false);

        assertTrue("La chata detecta el choque", chata.chocando);
        assertEquals("La chata no avanza a la posición bloqueada en X", posInicialX, chata.x, 0.001f);
        assertEquals("La chata no avanza a la posición bloqueada en Z", posInicialZ, chata.z, 0.001f);
    }

    /** Comprueba que al retroceder la chata no se desengancha y mantiene estabilidad anti-tijera. */
    public void testChataReversaNoSeDesenganchaNiHaceTijera() {
        clase2 juego = new clase2();
        juego.reiniciar();
        juego.velocidad = -4f; // En reversa
        for (int i = 0; i < 60; i++) {
            juego.actualizarChata(1 / 60f, 0); // Marcha atrás en línea recta
        }
        // Debe permanecer alineada con el auto (sin amplificación descontrolada de tijera)
        assertTrue("La articulación en reversa recta es estable", Math.abs(juego.anguloChata - juego.angulo) < 0.15f);

        // Comprueba la distancia fija a la bola de enganche
        float frenteAutoX = -(float) Math.sin(juego.angulo);
        float frenteAutoZ = -(float) Math.cos(juego.angulo);
        float atrasAutoX = -frenteAutoX;
        float atrasAutoZ = -frenteAutoZ;
        float engancheX = juego.autoX + atrasAutoX * clase2.DISTANCIA_ENGANCHE;
        float engancheZ = juego.autoZ + atrasAutoZ * clase2.DISTANCIA_ENGANCHE;
        float distHitch = (float) Math.hypot(engancheX - juego.chataX, engancheZ - juego.chataZ);
        assertEquals("La lanza permanece unida exactamente a la bola", clase2.DISTANCIA_CHATA, distHitch, 0.01f);
    }

    /** Comprueba que la chata reutiliza el sistema de colisiones para detectar edificios y aceras. */
    public void testColisionChataReutilizaSistemaAuto() {
        clase2 juego = new clase2();
        assertFalse("Rechaza la chata en el centro de un edificio", juego.puedeCircularChata(-40, -40, 0));
        assertFalse("Rechaza la chata fuera de los límites", juego.puedeCircularChata(0, 56, 0));
        assertTrue("Acepta la chata en la calle libre", juego.puedeCircularChata(clase2.INICIO_X, clase2.INICIO_Z + 3.5f, 0));
    }
}
