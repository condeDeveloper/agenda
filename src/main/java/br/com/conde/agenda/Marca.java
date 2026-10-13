package br.com.conde.agenda;

import java.util.Arrays;

/**
 * Uma marca: o instante, a ordem de chegada e o identificador.
 *
 * <p>A ORDEM DE CHEGADA NAO E ENFEITE. Dois temporizadores marcados para o mesmo tique
 * precisam disparar na ordem em que foram marcados, e sem um criterio de desempate cada
 * estrutura devolveria uma ordem diferente: o amontoado pela forma da arvore, a roda pela
 * ordem do balde. As duas estariam certas sobre o instante e o juiz acusaria divergencia.
 */
public record Marca(long quando, long ordem, int identificador) {

    /** Antes na fila de disparo: instante menor, e no empate ordem de chegada menor. */
    public boolean antesDe(Marca outra) {
        return quando != outra.quando ? quando < outra.quando : ordem < outra.ordem;
    }

    /** Uma lista de identificadores que cresce sozinha. */
    public static final class Disparos {

        private int[] identificadores = new int[16];
        private int quantos;

        public void disparou(int identificador) {
            if (quantos == identificadores.length)
                identificadores = Arrays.copyOf(identificadores, identificadores.length * 2);

            identificadores[quantos++] = identificador;
        }

        public int quantos() {
            return quantos;
        }

        public int[] emVetor() {
            return Arrays.copyOf(identificadores, quantos);
        }
    }

    /** Um balde: marcas numa ordem qualquer, que cresce sozinho. */
    public static final class Balde {

        private Marca[] marcas = new Marca[4];
        private int quantas;

        public void por(Marca marca) {
            if (quantas == marcas.length) marcas = Arrays.copyOf(marcas, marcas.length * 2);

            marcas[quantas++] = marca;
        }

        public int quantas() {
            return quantas;
        }

        public Marca em(int i) {
            return marcas[i];
        }

        public void tirarEm(int i) {
            marcas[i] = marcas[--quantas];
            marcas[quantas] = null;
        }

        public void esvaziar() {
            Arrays.fill(marcas, 0, quantas, null);
            quantas = 0;
        }

        /** As marcas em ordem de disparo. Usado quando o balde inteiro vence de uma vez. */
        public Marca[] emOrdem() {
            Marca[] copia = Arrays.copyOf(marcas, quantas);

            Arrays.sort(copia, (a, b) -> a.antesDe(b) ? -1 : b.antesDe(a) ? 1 : 0);

            return copia;
        }
    }
}
