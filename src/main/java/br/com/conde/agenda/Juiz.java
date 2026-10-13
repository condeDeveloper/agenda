package br.com.conde.agenda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * O JUIZ: a lista ordenada.
 *
 * <p>Ela e o juiz certo e nao so o conveniente. Nao ha nela balde a escolher, cascata a
 * fazer nem arvore a consertar: a proxima a disparar e a ultima do vetor.
 *
 * <h2>O que ele confere</h2>
 *
 * <p>A SEQUENCIA INTEIRA de disparos, tique por tique, e nao o conjunto do fim. Uma roda
 * que dispare o temporizador certo no tique errado acerta o conjunto e erra a agenda, e
 * essa e a falha mais provavel de todas aqui: ela aparece so quando o atraso e observado.
 *
 * <p>E confere a ORDEM dentro do mesmo tique. Dois temporizadores marcados para o mesmo
 * instante disparam na ordem em que foram marcados, e sem isso o amontoado e a roda
 * devolveriam ordens diferentes sem nenhum dos dois estar errado sobre o instante.
 */
public final class Juiz {

    private Juiz() {
    }

    public record Veredito(int tiques, int disparos, int divergencias, String falha) {

        public boolean passou() {
            return divergencias == 0;
        }
    }

    /** Um roteiro de operacoes, para o juiz rodar o mesmo em todas as agendas. */
    public record Passo(long instante, int identificador, boolean cancelamento) {

        static Passo agendar(long instante, int identificador) {
            return new Passo(instante, identificador, false);
        }

        static Passo cancelar(int identificador) {
            return new Passo(0, identificador, true);
        }
    }

    /**
     * Roda o roteiro na agenda e devolve a sequencia de disparos, um vetor por tique.
     *
     * <p>Entre cada tique, as operacoes marcadas para aquele tique sao aplicadas. Assim
     * agendar e cancelar acontecem no meio da passagem do tempo, que e o caso dificil: uma
     * roda que reorganize o estado interno enquanto o relogio anda tem que continuar certa.
     */
    public static List<int[]> rodar(Agenda agenda, List<List<Passo>> porTique, Contador contador) {
        List<int[]> disparos = new ArrayList<>();

        for (int tique = 0; tique < porTique.size(); tique++) {
            for (Passo passo : porTique.get(tique)) {
                if (passo.cancelamento()) agenda.cancelar(passo.identificador(), contador);
                else agenda.agendar(passo.instante(), passo.identificador(), contador);
            }

            disparos.add(agenda.andarAte(tique + 1, contador));
        }

        return disparos;
    }

    public static Veredito julgar(Agenda agenda, List<List<Passo>> porTique) {
        List<int[]> certo = rodar(new ListaOrdenada(), porTique, new Contador());
        List<int[]> dele = rodar(agenda, porTique, new Contador());

        int divergencias = 0;
        int quantos = 0;
        String falha = null;

        for (int tique = 0; tique < certo.size(); tique++) {
            quantos += certo.get(tique).length;

            if (Arrays.equals(certo.get(tique), dele.get(tique))) continue;

            divergencias++;

            if (falha != null) continue;

            falha = "no tique " + tique + ": " + Arrays.toString(dele.get(tique))
                    + " contra " + Arrays.toString(certo.get(tique));
        }

        return new Veredito(certo.size(), quantos, divergencias, falha);
    }

    /**
     * Um roteiro sorteado, com agendamentos, cancelamentos e empates no mesmo instante.
     *
     * <p>Os empates sao forcados de proposito: um em cada quatro agendamentos repete o
     * instante do anterior. Sem isso, a regra de desempate por ordem de chegada nunca seria
     * exercitada, e ela e justamente onde o amontoado e a roda podem divergir.
     */
    public static List<List<Passo>> roteiro(int tiques, int porTique, int horizonte, long semente) {
        Agendas.Sorteio sorteio = new Agendas.Sorteio(semente);
        List<List<Passo>> roteiro = new ArrayList<>();
        List<Integer> vivos = new ArrayList<>();

        int proximo = 1;
        long instanteAnterior = 1;

        for (int tique = 0; tique < tiques; tique++) {
            List<Passo> doTique = new ArrayList<>();

            for (int i = 0; i < porTique; i++) {
                long instante;

                if (i % 4 == 3 && instanteAnterior > tique) {
                    instante = instanteAnterior;
                } else {
                    instante = tique + 1 + sorteio.ate(horizonte);
                    instanteAnterior = instante;
                }

                doTique.add(Passo.agendar(instante, proximo));
                vivos.add(proximo);
                proximo++;
            }

            if (!vivos.isEmpty() && sorteio.ate(5) == 0)
                doTique.add(Passo.cancelar(vivos.remove(sorteio.ate(vivos.size()))));

            roteiro.add(doTique);
        }

        return roteiro;
    }
}
