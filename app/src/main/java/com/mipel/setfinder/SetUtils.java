package com.mipel.setfinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetUtils {

    // Comprueba si 3 cartas cumplen TODAS las condiciones para ser un set.
    public static boolean esSet(Carta c1, Carta c2, Carta c3) {
        return atributoValido(c1.getNumero(), c2.getNumero(), c3.getNumero()) &&
                atributoValido(c1.getColor(), c2.getColor(), c3.getColor()) &&
                atributoValido(c1.getForma(), c2.getForma(), c3.getForma()) &&
                atributoValido(c1.getRelleno(), c2.getRelleno(), c3.getRelleno());
    }

    // Comprueba si 3 valores de un mismo atributo (por ejemplo color) son todos iguales o todos distintos.
    private static boolean atributoValido(int a, int b, int c) {
        return (a == b && b == c) || (a != b && b != c && a != c);
    }


    // Busca todas las combinaciones de 3 cartas en la lista y devuelve las que forman un set válido.
    public static List<List<Carta>> buscarSets(List<Carta> cartas) {
        List<List<Carta>> setsEncontrados = new ArrayList<>();
        for (int i = 0; i < cartas.size(); i++) {
            for (int j = i + 1; j < cartas.size(); j++) {
                for (int k = j + 1; k < cartas.size(); k++) {
                    Carta c1 = cartas.get(i);
                    Carta c2 = cartas.get(j);
                    Carta c3 = cartas.get(k);
                    if (esSet(c1, c2, c3)) {
                        setsEncontrados.add(Arrays.asList(c1, c2, c3));
                    }
                }
            }
        }
        return setsEncontrados;
    }

}
