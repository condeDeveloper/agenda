package br.com.conde.agenda;

import java.util.HashMap;
import java.util.Map;

/**
 * A RODA HIERARQUICA: varias rodas, cada uma com a resolucao da anterior vezes o numero de
 * baldes.
 *
 * <p>E o desenho que o Linux usa, e ele existe para consertar exatamente o defeito da roda
 * simples: o horizonte.
 *
 * <p>A roda de baixo guarda os proximos {@code slots} tiques com resolucao de um tique. A
 * segunda guarda os proximos {@code slots*slots} com resolucao de {@code slots} tiques. A
 * terceira, mais grossa ainda. Um temporizador marcado para daqui a um milhao de tiques
 * entra numa roda de cima, onde ele e so um entre poucos.
 *
 * <h2>A cascata</h2>
 *
 * <p>Quando a roda de baixo da uma volta inteira, o balde correspondente da roda de cima e
 * esvaziado e as marcas dele sao REAGENDADAS. Como o relogio andou, elas agora cabem numa
 * roda mais fina e descem.
 *
 * <p>Isso nao e de graca: a cascata move marcas. Mas ela acontece uma vez a cada
 * {@code slots} tiques no segundo nivel, uma vez a cada {@code slots*slots} no terceiro, e
 * cada marca desce no maximo uma vez por nivel durante toda a vida dela. O custo
 * AMORTIZADO por marca e o numero de niveis, e nao o numero de voltas.
 */
public final class RodaHierarquica implements Agenda {

    private final int bits;
    private final int slots;
    private final int mascara;
    private final int niveis;
    private final Marca.Balde[][] rodas;
    private final Map<Integer, long[]> ondeEsta = new HashMap<>();

    private long agora;
    private long proximaOrdem;
    private int quantas;

    public RodaHierarquica(int bits, int niveis) {
        if (bits < 1 || bits > 16) throw new IllegalArgumentException("bits de 1 a 16");
        if (niveis < 1 || niveis * bits > 48)
            throw new IllegalArgumentException("niveis vezes bits ate 48");

        this.bits = bits;
        this.slots = 1 << bits;
        this.mascara = slots - 1;
        this.niveis = niveis;
        this.rodas = new Marca.Balde[niveis][slots];

        for (int nivel = 0; nivel < niveis; nivel++)
            for (int i = 0; i < slots; i++) rodas[nivel][i] = new Marca.Balde();
    }

    public static RodaHierarquica comoNoLinux() {
        return new RodaHierarquica(6, 4);
    }

    /** O instante mais distante que cabe. Alem disto a roda recusa em vez de errar calada. */
    public long horizonte() {
        return 1L << (bits * niveis);
    }

    @Override
    public String nome() {
        return "roda hierarquica " + slots + "x" + niveis;
    }

    @Override
    public void agendar(long quando, int identificador, Contador contador) {
        /*
         * O INSTANTE TEM QUE SER ESTRITAMENTE FUTURO, e isso nao e preciosismo.
         *
         * Marcar para o instante ATUAL nao tem resposta boa. Na roda de tempo o balde de
         * agora acabou de ser aberto, entao a marca so seria vista na volta seguinte e
         * dispararia com um atraso do tamanho da roda inteira. Na lista e no amontoado ela
         * dispararia no proximo tique.
         *
         * Duas estruturas corretas dariam respostas diferentes para a mesma pergunta, e o
         * certo e a pergunta nao existir.
         */
        if (quando <= agora)
            throw new IllegalArgumentException(
                    "instante " + quando + " nao e futuro, agora e " + agora);

        contador.agendamento();

        por(new Marca(quando, proximaOrdem++, identificador), contador);

        quantas++;
    }

    /**
     * Poe a marca na roda certa: a mais fina em que a distancia ainda cabe.
     *
     * <p>A distancia e medida a partir de AGORA, e e por isso que a cascata funciona: a
     * mesma marca, reagendada mais tarde, encontra uma distancia menor e desce de nivel
     * sozinha, sem ninguem decidir nada.
     */
    private void por(Marca marca, Contador contador) {
        long distancia = marca.quando() - agora;

        if (distancia >= horizonte())
            throw new IllegalArgumentException(
                    "instante a " + distancia + " tiques, o horizonte desta roda e " + horizonte());

        for (int nivel = 0; nivel < niveis; nivel++) {
            contador.compara();

            if (distancia >= (1L << (bits * (nivel + 1))) && nivel < niveis - 1) continue;

            int indice = (int) ((marca.quando() >>> (bits * nivel)) & mascara);

            contador.visitaBalde();

            rodas[nivel][indice].por(marca);
            ondeEsta.put(marca.identificador(), new long[] {nivel, indice});

            return;
        }
    }

    @Override
    public int[] andarAte(long instante, Contador contador) {
        Marca.Disparos disparos = new Marca.Disparos();

        while (agora < instante) {
            agora++;
            contador.tique();

            int indice = (int) (agora & mascara);

            /*
             * A CASCATA SO ACONTECE NA VIRADA. Quando o indice do nivel zero volta a zero, a
             * roda de baixo deu uma volta inteira e o balde de cima correspondente precisa
             * descer. E se o indice de cima tambem for zero, aquela roda tambem virou, e a
             * cascata sobe mais um nivel.
             */
            if (indice == 0)
                for (int nivel = 1; nivel < niveis; nivel++) {
                    int deCima = (int) ((agora >>> (bits * nivel)) & mascara);

                    cascatear(nivel, deCima, contador);

                    if (deCima != 0) break;
                }

            contador.visitaBalde();

            Marca.Balde balde = rodas[0][indice];

            for (Marca marca : balde.emOrdem()) {
                contador.veEntrada();

                disparos.disparou(marca.identificador());
                ondeEsta.remove(marca.identificador());
                quantas--;
            }

            balde.esvaziar();
        }

        return disparos.emVetor();
    }

    private void cascatear(int nivel, int indice, Contador contador) {
        Marca.Balde balde = rodas[nivel][indice];

        contador.visitaBalde();

        if (balde.quantas() == 0) return;

        Marca[] descendo = balde.emOrdem();

        balde.esvaziar();

        contador.move(descendo.length);

        for (Marca marca : descendo) {
            contador.veEntrada();
            por(marca, contador);
        }
    }

    @Override
    public boolean cancelar(int identificador, Contador contador) {
        contador.cancelamento();

        long[] onde = ondeEsta.remove(identificador);

        if (onde == null) return false;

        contador.visitaBalde();

        Marca.Balde balde = rodas[(int) onde[0]][(int) onde[1]];

        for (int i = 0; i < balde.quantas(); i++) {
            contador.veEntrada();

            if (balde.em(i).identificador() != identificador) continue;

            balde.tirarEm(i);
            quantas--;

            return true;
        }

        return false;
    }

    @Override
    public int pendentes() {
        return quantas;
    }

    @Override
    public long agora() {
        return agora;
    }
}
