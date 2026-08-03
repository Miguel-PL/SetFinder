package com.mipel.setfinder;

import java.util.Objects;

public class Carta {
    protected int numero; // 1, 2, 3
    protected int color; // 0: rojo, 1: verde, 2: morado
    protected int forma; // 0: óvalo, 1: rombo, 2: onda
    protected int relleno; // 0: sólido, 1: rayado, 2: vacío

    public Carta() {
    }

    public Carta(int numero, int color, int forma, int relleno) {
        this.numero = numero;
        this.color = color;
        this.forma = forma;
        this.relleno = relleno;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getForma() {
        return forma;
    }

    public void setForma(int forma) {
        this.forma = forma;
    }

    public int getRelleno() {
        return relleno;
    }

    public void setRelleno(int relleno) {
        this.relleno = relleno;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Carta carta = (Carta) o;
        return numero == carta.numero && color == carta.color && forma == carta.forma && relleno == carta.relleno;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, color, forma, relleno);
    }

    @Override
    public String toString() {
        String[] colores = {"Rojo", "Verde", "Morado"};
        String[] formas = {"Óvalo", "Rombo", "Onda"};
        String[] rellenos = {"Sólido", "Rayado", "Vacío"};

        return String.format("%d %s %s y %s",
                numero,
                colores[color],
                formas[forma],
                rellenos[relleno]
        );
    }
}
