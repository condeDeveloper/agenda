package br.com.conde.agenda;

import java.util.HashMap;
import java.util.Map;

/**
 * A RODA DE TEMPO SIMPLES: um vetor de baldes, e o instante escolhe o balde pelo resto.
 *
 * <p>Agendar e uma conta e um acrescimo numa lista: tempo constante, sem comparacao
 * nenhuma. Essa e a propriedade que faz rodas de tempo existirem, e ela e verdadeira.
 *
 * <p>O que ela cobra esta no outro lado. A cada tique ela abre o balde daquele tique,
 * esteja ou nao esteja alguem la dentro. Com o sistema ocioso, o amontoado faz uma
 * comparacao por tique e a roda abre um balde por tique: a roda trabalha mais fazendo
 * nada.
 *
 * <h2>O horizonte e o calcanhar dela</h2>
 *
 * <p>Com {@code slots} baldes, o balde de indice i guarda os instantes i, i+slots,
 * i+2*slots, e assim por diante. Eles sao de VOLTAS diferentes da roda, e no tique i so os
 * da volta atual vencem.
 *
 * <p>Entao, quando os temporizadores sao marcados para longe, cada balde acumula marcas de
 * varias voltas e o tique precisa olhar todas para achar a que venceu.
 *
 * <h2>E esse custo tem teto, que eu tinha escrito que nao tinha</h2>
 *
 * <p>Eu escrevi que o numero cresce linearmente com o horizonte. Ele cresce ate saturar: com
 * 5.000 temporizadores e 256 baldes, cada balde guarda 5.000/256, que e 19,5 marcas, e nao
 * importa se o horizonte e dezesseis mil ou duzentos e sessenta mil tiques.
 *
 * <p>Quem decide o custo por tique nao e o horizonte: e PENDENTES DIVIDIDO POR BALDES.
 * Enquanto o horizonte cabe na roda, cada marca esta no balde dela e so; passado isso, elas
 * se espalham por todos os baldes por igual e a conta para de piorar.
 *
 * <p>A tabela do horizonte mostra isso: de 16.384 para 262.144 tiques, dezesseis vezes mais
 * longe, o custo por tique vai de 17,2 para 38,5, pouco mais que o dobro.
 */
public final class RodaSimples implements Agenda {

    private final int slots;
    private final int mascara;
    private final Marca.Balde[] baldes;
    private final Map<Integer, Integer> baldeDe = new HashMap<>();

    private long agora;
    private long proximaOrdem;
    private int quantas;

    public RodaSimples(int slots) {
        if (slots < 1 || Integer.bitCount(slots) != 1)
            throw new IllegalArgumentException("slots precisa ser potencia de dois, veio " + slots);

        this.slots = slots;
        this.mascara = slots - 1;
        this.baldes = new Marca.Balde[slots];

        for (int i = 0; i < slots; i++) baldes[i] = new Marca.Balde();
    }

    @Override
    public String nome() {
        return "roda de " + slots;
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

        int balde = (int) (quando & mascara);

        contador.visitaBalde();

        baldes[balde].por(new Marca(quando, proximaOrdem++, identificador));
        baldeDe.put(identificador, balde);
        quantas++;
    }

    @Override
    public int[] andarAte(long instante, Contador contador) {
        Marca.Disparos disparos = new Marca.Disparos();

        while (agora < instante) {
            agora++;
            contador.tique();

            /*
             * O BALDE E ABERTO SEMPRE, tenha alguem dentro ou nao. E essa a conta que a
             * complexidade por operacao nao mostra.
             */
            contador.visitaBalde();

            Marca.Balde balde = baldes[(int) (agora & mascara)];
            Marca.Balde venceram = new Marca.Balde();

            for (int i = 0; i < balde.quantas(); ) {
                contador.veEntrada();
                contador.compara();

                if (balde.em(i).quando() != agora) {
                    i++;
                    continue;
                }

                venceram.por(balde.em(i));
                balde.tirarEm(i);
            }

            for (Marca marca : venceram.emOrdem()) {
                disparos.disparou(marca.identificador());
                baldeDe.remove(marca.identificador());
                quantas--;
            }
        }

        return disparos.emVetor();
    }

    @Override
    public boolean cancelar(int identificador, Contador contador) {
        contador.cancelamento();

        Integer onde = baldeDe.remove(identificador);

        if (onde == null) return false;

        contador.visitaBalde();

        Marca.Balde balde = baldes[onde];

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
