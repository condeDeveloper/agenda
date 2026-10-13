package br.com.conde.agenda;

import java.util.Arrays;

/**
 * A LISTA ORDENADA: todas as marcas num vetor, na ordem em que vao disparar.
 *
 * <p>Ela e o juiz deste repositorio e tambem uma das estruturas medidas, e as duas coisas
 * pelo mesmo motivo: nao ha o que errar nela. A proxima a disparar e a ultima do vetor, e
 * andar o relogio e tirar do fim enquanto o instante tiver chegado. Nao ha balde a escolher,
 * cascata a fazer nem arvore a consertar.
 *
 * <p>Guardada ao CONTRARIO, da maior para a menor, de proposito: assim tirar a proxima e
 * mexer no fim do vetor, que nao desloca nada. O preco fica todo na insercao, que e onde a
 * tabela do agendamento o encontra.
 *
 * <p>E o preco e grande: inserir no meio de um vetor de dez mil marcas move cinco mil. E a
 * estrutura certa quando ha poucos temporizadores e e a errada quando ha muitos, e o ponto
 * em que isso vira esta medido.
 */
public final class ListaOrdenada implements Agenda {

    private Marca[] marcas = new Marca[16];
    private int quantas;
    private long agora;
    private long proximaOrdem;

    @Override
    public String nome() {
        return "lista ordenada";
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

        if (quantas == marcas.length) marcas = Arrays.copyOf(marcas, marcas.length * 2);

        Marca nova = new Marca(quando, proximaOrdem++, identificador);

        /*
         * BUSCA BINARIA NUM VETOR DECRESCENTE: procura o primeiro lugar, da esquerda para a
         * direita, em que a marca de la ja dispara DEPOIS da nova. A nova entra ali.
         */
        int baixo = 0;
        int alto = quantas;

        while (baixo < alto) {
            int meio = (baixo + alto) >>> 1;

            contador.compara();

            if (nova.antesDe(marcas[meio])) baixo = meio + 1;
            else alto = meio;
        }

        System.arraycopy(marcas, baixo, marcas, baixo + 1, quantas - baixo);

        contador.move(quantas - baixo);

        marcas[baixo] = nova;
        quantas++;
    }

    @Override
    public int[] andarAte(long instante, Contador contador) {
        Marca.Disparos disparos = new Marca.Disparos();

        while (agora < instante) {
            agora++;
            contador.tique();

            /*
             * UMA COMPARACAO POR TIQUE, e so. Esta linha e o contraste com a roda: a lista
             * olha a proxima marca, ve que nao venceu e vai embora. Com o sistema ocioso,
             * o vetor nem e tocado.
             */
            while (quantas > 0) {
                contador.compara();

                if (marcas[quantas - 1].quando() > agora) break;

                contador.veEntrada();

                disparos.disparou(marcas[--quantas].identificador());
                marcas[quantas] = null;
            }
        }

        return disparos.emVetor();
    }

    @Override
    public boolean cancelar(int identificador, Contador contador) {
        contador.cancelamento();

        /*
         * CANCELAR E A VARREDURA QUE ESTA ESTRUTURA NAO TEM COMO EVITAR. O vetor esta em
         * ordem de INSTANTE, e cancelar pergunta por IDENTIFICADOR: nao ha busca binaria
         * possivel sobre um criterio que nao e o da ordenacao.
         */
        for (int i = 0; i < quantas; i++) {
            contador.veEntrada();

            if (marcas[i].identificador() != identificador) continue;

            System.arraycopy(marcas, i + 1, marcas, i, quantas - i - 1);

            contador.move(quantas - i - 1);

            marcas[--quantas] = null;

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
