package br.com.conde.agenda;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AS AGENDAS QUE AS TABELAS PERCORREM e as cargas de trabalho que as separam.
 *
 * <p>Uma carga aqui e um par: quantos temporizadores existem e a que DISTANCIA eles sao
 * marcados. As duas coisas mexem em coisas diferentes, e e por isso que elas tem tabelas
 * separadas.
 */
public final class Agendas {

    private Agendas() {
    }

    /**
     * O SORTEIO DA CASA: um gerador congruente linear escrito aqui, com constantes fixas.
     *
     * <p>Nao e {@link java.util.Random} de proposito. Uma medida que depende de uma
     * biblioteca nao e reproduzivel: a implementacao pode mudar entre versoes da maquina
     * virtual e as tabelas do README deixariam de bater sem que nada aqui tivesse mudado.
     */
    public static final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente;
        }

        /**
         * O proximo inteiro.
         *
         * <p>O DESLOCAMENTO DE 32 CARREGA O METODO. Os bits BAIXOS de um gerador congruente
         * linear de modulo potencia de dois sao pessimos: o ultimo bit alterna zero e um
         * para sempre, o penultimo tem periodo quatro.
         *
         * <p>Num repositorio sobre rodas de tempo isso seria devastador e silencioso. O
         * balde e o instante por resto de uma potencia de dois, ou seja, EXATAMENTE os bits
         * baixos: instantes sorteados com bits baixos ruins cairiam sempre nos mesmos
         * baldes, e a roda pareceria pior do que e por um motivo que nao e dela. Ha um
         * teste so para isto.
         */
        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            return (int) (estado >>> 32);
        }

        public int ate(int limite) {
            return Math.floorMod(proximo(), limite);
        }
    }

    public static Map<String, Supplier<Agenda>> todas() {
        Map<String, Supplier<Agenda>> agendas = new LinkedHashMap<>();

        agendas.put("lista ordenada", ListaOrdenada::new);
        agendas.put("amontoado", Amontoado::new);
        agendas.put("roda de 256", () -> new RodaSimples(256));
        agendas.put("roda hierarquica", RodaHierarquica::comoNoLinux);

        return agendas;
    }

    /**
     * Agenda temporizadores com distancias sorteadas entre um e o horizonte.
     *
     * <p>Os identificadores sao distintos e crescentes. Distintos porque cancelar e por
     * identificador e um repetido tornaria a pergunta ambigua; crescentes porque assim o
     * identificador tambem diz a ordem de chegada e um teste pode ler a saida.
     */
    public static int encher(Agenda agenda, int quantos, int horizonte, int primeiroIdentificador,
                             long semente, Contador contador) {

        Sorteio sorteio = new Sorteio(semente);

        for (int i = 0; i < quantos; i++)
            agenda.agendar(agenda.agora() + 1 + sorteio.ate(horizonte),
                    primeiroIdentificador + i, contador);

        return primeiroIdentificador + quantos;
    }
}
