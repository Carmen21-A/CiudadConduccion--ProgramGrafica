package com.graphics; // Comparte el paquete con las cinco lecciones.

import java.text.Normalizer; // Permite quitar tildes antes de buscar cada letra.
import java.util.HashMap; // Guarda la forma de cada carácter.
import java.util.Map; // Define el tipo del diccionario de caracteres.

/**
 * FUENTE DE MAPA DE BITS 5 x 7 PARA CLASE5.
 * Cada carácter es una matriz de 7 filas por 5 columnas; cada fila es un número de 5 bits.
 * El bit 4 (valor 16) es la columna izquierda y el bit 0 (valor 1) la derecha.
 * No usa texturas: clase5 convierte cada tramo de píxeles encendidos en un rectángulo.
 */
final class Fuente {

    static final int COLUMNAS = 5; // Ancho de un carácter en píxeles de la fuente.
    static final int FILAS = 7; // Alto de un carácter en píxeles de la fuente.
    static final int AVANCE = 6; // Ancho del carácter más un píxel de separación.
    private static final Map<Character, int[]> GLIFOS = new HashMap<>(); // Relaciona cada carácter con sus siete filas.

    /** Recibe cada tramo horizontal de píxeles encendidos de un texto. */
    interface Tramo {
        void dibujar(int fila, int columna, int largo); // Fila 0-6, columna global y cantidad de píxeles seguidos.
    }

    static { // Define los caracteres que usa la interfaz.
        agregar('A', 14, 17, 17, 31, 17, 17, 17); // Letra A.
        agregar('B', 30, 17, 17, 30, 17, 17, 30); // Letra B.
        agregar('C', 14, 17, 16, 16, 16, 17, 14); // Letra C.
        agregar('D', 28, 18, 17, 17, 17, 18, 28); // Letra D.
        agregar('E', 31, 16, 16, 30, 16, 16, 31); // Letra E.
        agregar('F', 31, 16, 16, 30, 16, 16, 16); // Letra F.
        agregar('G', 14, 17, 16, 23, 17, 17, 15); // Letra G.
        agregar('H', 17, 17, 17, 31, 17, 17, 17); // Letra H.
        agregar('I', 14, 4, 4, 4, 4, 4, 14); // Letra I.
        agregar('J', 7, 2, 2, 2, 2, 18, 12); // Letra J.
        agregar('K', 17, 18, 20, 24, 20, 18, 17); // Letra K.
        agregar('L', 16, 16, 16, 16, 16, 16, 31); // Letra L.
        agregar('M', 17, 27, 21, 21, 17, 17, 17); // Letra M.
        agregar('N', 17, 17, 25, 21, 19, 17, 17); // Letra N.
        agregar('O', 14, 17, 17, 17, 17, 17, 14); // Letra O.
        agregar('P', 30, 17, 17, 30, 16, 16, 16); // Letra P.
        agregar('Q', 14, 17, 17, 17, 21, 18, 13); // Letra Q.
        agregar('R', 30, 17, 17, 30, 20, 18, 17); // Letra R.
        agregar('S', 15, 16, 16, 14, 1, 1, 30); // Letra S.
        agregar('T', 31, 4, 4, 4, 4, 4, 4); // Letra T.
        agregar('U', 17, 17, 17, 17, 17, 17, 14); // Letra U.
        agregar('V', 17, 17, 17, 17, 17, 10, 4); // Letra V.
        agregar('W', 17, 17, 17, 21, 21, 21, 10); // Letra W.
        agregar('X', 17, 17, 10, 4, 10, 17, 17); // Letra X.
        agregar('Y', 17, 17, 17, 10, 4, 4, 4); // Letra Y.
        agregar('Z', 31, 1, 2, 4, 8, 16, 31); // Letra Z.
        agregar('0', 14, 17, 19, 21, 25, 17, 14); // Dígito 0.
        agregar('1', 4, 12, 4, 4, 4, 4, 14); // Dígito 1.
        agregar('2', 14, 17, 1, 2, 4, 8, 31); // Dígito 2.
        agregar('3', 31, 2, 4, 2, 1, 17, 14); // Dígito 3.
        agregar('4', 2, 6, 10, 18, 31, 2, 2); // Dígito 4.
        agregar('5', 31, 16, 30, 1, 1, 17, 14); // Dígito 5.
        agregar('6', 6, 8, 16, 30, 17, 17, 14); // Dígito 6.
        agregar('7', 31, 1, 2, 4, 8, 8, 8); // Dígito 7.
        agregar('8', 14, 17, 17, 14, 17, 17, 14); // Dígito 8.
        agregar('9', 14, 17, 17, 15, 1, 2, 12); // Dígito 9.
        agregar(' ', 0, 0, 0, 0, 0, 0, 0); // Espacio.
        agregar(':', 0, 12, 12, 0, 12, 12, 0); // Dos puntos.
        agregar('.', 0, 0, 0, 0, 0, 12, 12); // Punto.
        agregar(',', 0, 0, 0, 0, 12, 4, 8); // Coma.
        agregar('-', 0, 0, 0, 31, 0, 0, 0); // Guion.
        agregar('+', 0, 4, 4, 31, 4, 4, 0); // Signo más.
        agregar('=', 0, 0, 31, 0, 31, 0, 0); // Signo igual.
        agregar('/', 0, 1, 2, 4, 8, 16, 0); // Barra.
        agregar('!', 4, 4, 4, 4, 4, 0, 4); // Exclamación.
        agregar('?', 14, 17, 1, 2, 4, 0, 4); // Interrogación.
        agregar('(', 2, 4, 8, 8, 8, 4, 2); // Paréntesis de apertura.
        agregar(')', 8, 4, 2, 2, 2, 4, 8); // Paréntesis de cierre.
        agregar('<', 2, 4, 8, 16, 8, 4, 2); // Flecha izquierda.
        agregar('>', 8, 4, 2, 1, 2, 4, 8); // Flecha derecha.
        agregar('%', 24, 25, 2, 4, 8, 19, 3); // Porcentaje.
    }

    private Fuente() {
        // Clase de utilidades: no se crean objetos.
    }

    /** Guarda las siete filas de un carácter. */
    private static void agregar(char caracter, int... filas) {
        GLIFOS.put(caracter, filas); // Registra el dibujo del carácter.
    }

    /** Convierte a mayúsculas sin tildes, porque la fuente solo define esos caracteres. */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD); // Separa la letra de su tilde.
        sinTildes = sinTildes.replaceAll("\\p{M}", ""); // Elimina las marcas de acento separadas.
        return sinTildes.toUpperCase(); // Usa mayúsculas, como define la fuente.
    }

    /** Devuelve el ancho del texto en píxeles de la fuente. */
    static int ancho(String texto) {
        if (texto.isEmpty()) { // Un texto vacío no ocupa espacio.
            return 0; // Evita un ancho negativo.
        }
        return texto.length() * AVANCE - 1; // Cada letra avanza seis píxeles; la última no deja separación.
    }

    /** Recorre el texto y entrega cada tramo horizontal de píxeles encendidos. */
    static void recorrer(String texto, Tramo tramo) {
        String limpio = normalizar(texto); // Asegura que cada carácter tenga una forma conocida.
        for (int indice = 0; indice < limpio.length(); indice++) { // Recorre los caracteres de izquierda a derecha.
            int[] glifo = GLIFOS.getOrDefault(limpio.charAt(indice), GLIFOS.get('?')); // Usa ? si falta el carácter.
            for (int fila = 0; fila < FILAS; fila++) { // Recorre las siete filas del carácter.
                int columna = 0; // Comienza en la columna izquierda.
                while (columna < COLUMNAS) { // Busca tramos de píxeles consecutivos.
                    if (encendido(glifo[fila], columna)) { // Encuentra el inicio de un tramo.
                        int inicio = columna; // Recuerda dónde empieza.
                        while (columna < COLUMNAS && encendido(glifo[fila], columna)) { // Avanza mientras siga encendido.
                            columna++; // Incluye este píxel en el tramo.
                        }
                        tramo.dibujar(fila, indice * AVANCE + inicio, columna - inicio); // Entrega el tramo completo.
                    } else { // El píxel está apagado.
                        columna++; // Pasa a la siguiente columna.
                    }
                }
            }
        }
    }

    /** Indica si una columna de la fila está encendida; la columna 0 es el bit de valor 16. */
    private static boolean encendido(int fila, int columna) {
        return (fila & (1 << (COLUMNAS - 1 - columna))) != 0; // Prueba el bit correspondiente.
    }
}
