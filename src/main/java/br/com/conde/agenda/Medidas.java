package br.com.conde.agenda;

import java.util.ArrayList;
import java.util.List;

/**
 * As MEDIDAS, em trabalho por tique e trabalho por operacao. Nunca segundos.
 *
 * <p>Sao duas contas e elas nao se misturam. O trabalho POR OPERACAO e o que a notacao
 * O-grande fala, e ai a roda de tempo ganha de todo mundo. O trabalho POR TIQUE e o que ela
 * nao fala, e e onde a roda cobra.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros.
 */
public final class Medidas {

    private Medidas() {
    }

    public static final int TIQUES = 100_000;

    public static final long SEMENTE = 20_251_012L;

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String agenda, String carga, int tiques, int disparos,
                                int divergencias) {
    }

    /** TODA AGENDA DISPARA OS MESMOS IDENTIFICADORES NO MESMO TIQUE E NA MESMA ORDEM. */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> linhas = new ArrayList<>();

        var cargas = new java.util.LinkedHashMap<String, List<List<Juiz.Passo>>>();

        cargas.put("perto", Juiz.roteiro(2_000, 2, 50, SEMENTE));
        cargas.put("longe", Juiz.roteiro(2_000, 2, 5_000, SEMENTE));
        cargas.put("tudo junto", Juiz.roteiro(2_000, 3, 3, SEMENTE));

        for (var carga : cargas.entrySet())
            for (var agenda : Agendas.todas().entrySet()) {
                Juiz.Veredito veredito = Juiz.julgar(agenda.getValue().get(), carga.getValue());

                linhas.add(new LinhaCorrecao(agenda.getKey(), carga.getKey(), veredito.tiques(),
                        veredito.disparos(), veredito.divergencias()));
            }

        return linhas;
    }

    // ───────────────────── 2. a ociosidade ─────────────────────

    public record LinhaOciosidade(String agenda, int pendentes, double trabalhoPorTique,
                                  long comparacoes, long baldes) {
    }

    /**
     * O CUSTO POR TIQUE COM O SISTEMA OCIOSO, em {@link #TIQUES} tiques.
     *
     * <p>E a tabela que a notacao de complexidade nao tem como mostrar. Nenhum temporizador
     * vence durante a medida: a pergunta e quanto cada estrutura trabalha para NAO fazer
     * nada.
     *
     * <p>A roda abre um balde por tique, porque e assim que ela sabe se alguem venceu. O
     * amontoado e a lista olham o proximo a vencer, veem que nao e agora e vao embora: uma
     * comparacao, e so.
     *
     * <p>Num servidor com relogio de mil tiques por segundo, essa diferenca acontece mil
     * vezes por segundo pelo resto da vida do processo.
     */
    public static List<LinhaOciosidade> aOciosidade() {
        List<LinhaOciosidade> linhas = new ArrayList<>();

        for (int pendentes : new int[] {0, 10, 10_000})
            for (var fabrica : Agendas.todas().entrySet()) {
                Agenda agenda = fabrica.getValue().get();
                Contador deCarga = new Contador();

                /*
                 * Os temporizadores sao marcados para DEPOIS do fim da medida, de proposito.
                 * Assim eles existem, ocupam a estrutura e nao vencem: a coluna mede so o
                 * custo de o relogio andar.
                 */
                for (int i = 0; i < pendentes; i++)
                    agenda.agendar(TIQUES + 1 + i, i, deCarga);

                Contador contador = new Contador();

                agenda.andarAte(TIQUES, contador);

                linhas.add(new LinhaOciosidade(fabrica.getKey(), pendentes,
                        contador.trabalhoPorTique(), contador.comparacoes(),
                        contador.baldesVisitados()));
            }

        return linhas;
    }

    // ───────────────────── 3. a carga ─────────────────────

    public record LinhaCarga(int porMilTiques, String agenda, double porAgendamento,
                             double porTique, double total) {
    }

    /**
     * O CUSTO POR TEMPORIZADOR AGENDADO, com o relogio andando ao mesmo tempo.
     *
     * <p>Aqui a roda cobra o que prometeu: uma conta e um acrescimo, sem comparacao
     * nenhuma. A lista ordenada paga o deslocamento do vetor e e a unica que cresce com a
     * quantidade de pendentes.
     *
     * <p>A coluna da direita soma as duas contas, e ela e a que decide de verdade: com
     * muitos agendamentos por tique a roda ganha, com poucos ela perde para o amontoado
     * por causa do tique.
     *
     * <p>A carga e dada em agendamentos por MIL tiques, e nao por tique, porque o caso
     * interessante esta embaixo: um servidor com um temporizador novo a cada mil tiques e
     * um sistema praticamente ocioso, e e ali que a conta do tique manda.
     */
    public static List<LinhaCarga> aCarga() {
        List<LinhaCarga> linhas = new ArrayList<>();
        int tiques = 20_000;
        int horizonte = 200;

        for (int porMilTiques : new int[] {0, 1, 10, 1_000, 10_000}) {
            for (var fabrica : Agendas.todas().entrySet()) {
                Agenda agenda = fabrica.getValue().get();
                Contador contador = new Contador();
                Agendas.Sorteio sorteio = new Agendas.Sorteio(SEMENTE);
                int proximo = 1;
                long devendo = 0;

                for (int tique = 0; tique < tiques; tique++) {
                    devendo += porMilTiques;

                    while (devendo >= 1_000) {
                        agenda.agendar(tique + 1 + sorteio.ate(horizonte), proximo++, contador);
                        devendo -= 1_000;
                    }

                    agenda.andarAte(tique + 1, contador);
                }

                linhas.add(new LinhaCarga(porMilTiques, fabrica.getKey(),
                        contador.trabalhoPorAgendamento(), contador.trabalhoPorTique(),
                        (double) contador.trabalho()));
            }
        }

        return linhas;
    }

    // ───────────────────── 4. o horizonte ─────────────────────

    public record LinhaHorizonte(int horizonte, String agenda, double trabalhoPorTique,
                                 double entradasPorTique) {
    }

    /**
     * O QUE O HORIZONTE FAZ COM A RODA.
     *
     * <p>Numa roda de 256 baldes, o balde de indice i guarda os instantes i, i+256, i+512, e
     * assim por diante, que sao de VOLTAS diferentes. No tique i so os da volta atual
     * vencem, e os outros sao olhados e deixados ali.
     *
     * <p>Eu escrevi que o trabalho por tique da roda simples cresce linearmente com o
     * horizonte. Ele cresce ate SATURAR: com 5.000 temporizadores e 256 baldes, cada balde
     * guarda 19,5 marcas, e de 16.384 para 262.144 tiques de horizonte, dezesseis vezes
     * mais longe, o custo vai de 17,2 para 38,5.
     *
     * <p>Quem decide nao e o horizonte, e PENDENTES DIVIDIDO POR BALDES. Passado o ponto em
     * que o horizonte excede a roda, as marcas ja estao espalhadas por todos os baldes por
     * igual e a conta para de piorar.
     *
     * <p>A roda hierarquica nao tem esse defeito: um temporizador para longe entra num nivel
     * grosso, onde ele e um entre poucos, e so desce quando chega a hora. E para isso que os
     * niveis existem.
     */
    public static List<LinhaHorizonte> oHorizonte() {
        List<LinhaHorizonte> linhas = new ArrayList<>();
        int tiques = 20_000;
        int pendentes = 5_000;

        for (int horizonte : new int[] {256, 1_024, 16_384, 262_144}) {
            for (var fabrica : Agendas.todas().entrySet()) {
                Agenda agenda = fabrica.getValue().get();
                Contador deCarga = new Contador();
                Agendas.Sorteio sorteio = new Agendas.Sorteio(SEMENTE);

                for (int i = 0; i < pendentes; i++)
                    agenda.agendar(1 + sorteio.ate(horizonte), i, deCarga);

                Contador contador = new Contador();

                agenda.andarAte(tiques, contador);

                linhas.add(new LinhaHorizonte(horizonte, fabrica.getKey(),
                        contador.trabalhoPorTique(),
                        (double) contador.entradasVistas() / tiques));
            }
        }

        return linhas;
    }

    // ───────────────────── 5. o cancelamento ─────────────────────

    public record LinhaCancelamento(int pendentes, String agenda, double porCancelamento,
                                    long entradasVistas) {
    }

    /**
     * O CUSTO DE CANCELAR, por quantos temporizadores existem.
     *
     * <p>Cancelar pergunta por IDENTIFICADOR, e nenhuma dessas estruturas esta organizada
     * por identificador: todas estao organizadas por INSTANTE. Entao as tres mais espertas
     * guardam um mapa do identificador para o lugar, e a lista ordenada varre.
     *
     * <p>Eu escrevi que a roda ganharia essa tabela com folga, porque o balde e pequeno.
     * Quem ganha e o AMONTOADO, e com folga: 4,72 contra 39,55 da roda simples e 381,44 da
     * hierarquica, com vinte mil pendentes.
     *
     * <p>O balde so e pequeno enquanto ha poucos temporizadores. Com vinte mil marcas em 256
     * baldes sao 78 por balde, e varrer 78 e pior que os treze passos de um amontoado de
     * vinte mil. O logaritmo cresce devagar; pendentes dividido por baldes cresce reto.
     *
     * <p>E a roda hierarquica e a PIOR das tres, o que parece contraditorio e nao e: os
     * temporizadores marcados para longe se acumulam nos niveis grossos, onde um unico
     * balde guarda milhares de marcas que ainda vao descer.
     */
    public static List<LinhaCancelamento> oCancelamento() {
        List<LinhaCancelamento> linhas = new ArrayList<>();

        for (int pendentes : new int[] {100, 2_000, 20_000})
            for (var fabrica : Agendas.todas().entrySet()) {
                Agenda agenda = fabrica.getValue().get();
                Contador deCarga = new Contador();

                Agendas.encher(agenda, pendentes, 100_000, 0, SEMENTE, deCarga);

                Contador contador = new Contador();
                Agendas.Sorteio sorteio = new Agendas.Sorteio(SEMENTE + 1);

                for (int i = 0; i < 1_000; i++) agenda.cancelar(sorteio.ate(pendentes), contador);

                linhas.add(new LinhaCancelamento(pendentes, fabrica.getKey(),
                        contador.trabalhoPorCancelamento(), contador.entradasVistas()));
            }

        return linhas;
    }
}
