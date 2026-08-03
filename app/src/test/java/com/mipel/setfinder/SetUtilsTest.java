package com.mipel.setfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SetUtilsTest {

    @Test
    public void esSet_tresCartasIdenticas_esSet() {
        Carta c = new Carta(1, 0, 0, 0);
        assertTrue(SetUtils.esSet(c, new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0)));
    }

    @Test
    public void esSet_todosLosAtributosDistintos_esSet() {
        assertTrue(SetUtils.esSet(
                new Carta(1, 0, 0, 0),
                new Carta(2, 1, 1, 1),
                new Carta(3, 2, 2, 2)));
    }

    @Test
    public void esSet_atributoMezclado_noEsSet() {
        assertFalse(SetUtils.esSet(
                new Carta(1, 0, 0, 0),
                new Carta(2, 1, 1, 1),
                new Carta(3, 1, 2, 2)));
    }

    @Test
    public void esSet_mezclaIgualesYDistintosPorAtributo_esSet() {
        assertTrue(SetUtils.esSet(
                new Carta(1, 0, 1, 2),
                new Carta(2, 0, 2, 0),
                new Carta(3, 0, 0, 1)));
    }

    @Test
    public void buscarSets_listaVacia_sinSets() {
        assertTrue(SetUtils.buscarSets(Arrays.<Carta>asList()).isEmpty());
    }

    @Test
    public void buscarSets_menosDeTresCartas_sinSets() {
        List<Carta> cartas = Arrays.asList(new Carta(1, 0, 0, 0), new Carta(2, 1, 1, 1));
        assertTrue(SetUtils.buscarSets(cartas).isEmpty());
    }

    @Test
    public void buscarSets_cuatroCartasConUnSet_encuentraEseUnico() {
        Carta a = new Carta(1, 0, 0, 0);
        Carta b = new Carta(2, 1, 1, 1);
        Carta c = new Carta(3, 2, 2, 2);
        Carta d = new Carta(1, 0, 0, 1);

        List<List<Carta>> sets = SetUtils.buscarSets(Arrays.asList(a, b, c, d));

        assertEquals(1, sets.size());
        assertEquals(Arrays.asList(a, b, c), sets.get(0));
    }

    @Test
    public void buscarSets_doceCartasIdenticas_todasLasCombinaciones() {
        List<Carta> cartas = Arrays.asList(
                new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0),
                new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0),
                new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0),
                new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0), new Carta(1, 0, 0, 0));
        assertEquals(220, SetUtils.buscarSets(cartas).size());
    }

    @Test
    public void buscarSets_doceCartasDistintas_todosLosSetsDevueltosSonValidos() {
        List<Carta> cartas = Arrays.asList(
                new Carta(1, 0, 0, 0), new Carta(1, 0, 1, 1), new Carta(1, 1, 2, 2),
                new Carta(2, 1, 1, 0), new Carta(2, 2, 0, 1), new Carta(2, 0, 2, 2),
                new Carta(3, 2, 2, 0), new Carta(3, 0, 1, 1), new Carta(3, 1, 0, 2),
                new Carta(1, 2, 2, 1), new Carta(2, 1, 2, 2), new Carta(3, 2, 1, 0));

        List<List<Carta>> sets = SetUtils.buscarSets(cartas);

        for (List<Carta> set : sets) {
            assertEquals(3, set.size());
            assertTrue(SetUtils.esSet(set.get(0), set.get(1), set.get(2)));
        }
    }
}
