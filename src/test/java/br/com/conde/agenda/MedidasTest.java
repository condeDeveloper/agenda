package br.com.conde.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /* As tabelas sao calculadas uma vez so: cada uma anda o relogio dezenas de milhares de vezes. */
    private static final List<Medidas.LinhaOciosidade> OCIOSIDADE = Medidas.aOciosidade();
    private static final List<Medidas.LinhaCarga> CARGA = Medidas.aCarga();
    private static final List<Medidas.LinhaHorizonte> HORIZONTE = Medidas.oHorizonte();
    private static final List<Medidas.LinhaCancelamento> CANCELAMENTO = Medidas.oCancelamento();

    // ───────────────────── 1. a correcao ─────────────────────

    @Test
    @DisplayName("nenhuma agenda diverge da lista ordenada")
    void aCorrecaoNaoDiverge() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(12);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.divergencias()).as(l.agenda() + " na carga " + l.carga()).isZero();
            assertThat(l.tiques()).isEqualTo(2_000);
        });

        assertThat(disparos(linhas, "perto")).isEqualTo(3_921);
        assertThat(disparos(linhas, "tudo junto"))
                .as("com horizonte de tres tiques, quase tudo dispara dentro da janela")
                .isEqualTo(5_989);
    }

    private static int disparos(List<Medidas.LinhaCorrecao> linhas, String carga) {
        List<Integer> todos = linhas.stream()
                .filter(l -> l.carga().equals(carga))
                .map(Medidas.LinhaCorrecao::disparos)
                .distinct()
                .toList();

        assertThat(todos).as("as quatro agendas concordam").hasSize(1);

        return todos.get(0);
    }

    // ───────────────────── 2. a ociosidade ─────────────────────

    /**
     * A AFIRMACAO CENTRAL DO REPOSITORIO.
     *
     * <p>Com nada marcado, a lista e o amontoado fazem ZERO trabalho por tique e a roda faz
     * um. A roda trabalha mais fazendo nada, e nenhuma tabela de complexidade por operacao
     * tem como mostrar isso.
     */
    @Test
    @DisplayName("com nada marcado, a roda trabalha por tique e as outras duas nao")
    void aRodaCobraPorTiqueSemNada() {
        assertThat(ociosidade(0, "lista ordenada").trabalhoPorTique()).isZero();
        assertThat(ociosidade(0, "amontoado").trabalhoPorTique()).isZero();

        assertThat(ociosidade(0, "roda de 256").trabalhoPorTique())
                .as("um balde aberto em cada tique, para descobrir que ele esta vazio")
                .isEqualTo(1.0);

        assertThat(ociosidade(0, "roda de 256").baldes()).isEqualTo(Medidas.TIQUES);
        assertThat(ociosidade(0, "roda de 256").comparacoes()).isZero();

        assertThat(ociosidade(0, "roda hierarquica").trabalhoPorTique())
                .as("e a hierarquica abre um pouco mais, por causa das viradas")
                .isCloseTo(1.0159, within(0.0001));
    }

    /**
     * E A CONTA DA RODA SIMPLES PIORA COM O NUMERO DE PENDENTES, mesmo sem ninguem vencer.
     *
     * <p>Os temporizadores estao todos marcados para depois do fim da medida, entao nenhum
     * dispara. Eles so estao ali, espalhados pelos baldes, sendo olhados em todo tique.
     */
    @Test
    @DisplayName("com dez mil marcados para o futuro, a roda simples gasta 79 por tique")
    void aRodaSimplesPioraComOsPendentes() {
        assertThat(ociosidade(10_000, "roda de 256").trabalhoPorTique())
                .isCloseTo(79.1248, within(0.0001));

        assertThat(ociosidade(10_000, "amontoado").trabalhoPorTique())
                .as("e o amontoado continua em uma comparacao por tique")
                .isEqualTo(1.0);

        assertThat(ociosidade(10_000, "roda hierarquica").trabalhoPorTique())
                .as("a hierarquica quase nao sente, que e para isso que ela existe")
                .isCloseTo(1.1371, within(0.0001));

        assertThat(ociosidade(10_000, "roda de 256").trabalhoPorTique()
                / ociosidade(10, "roda de 256").trabalhoPorTique())
                .as("mil vezes mais pendentes, 73 vezes mais trabalho por tique")
                .isGreaterThan(70.0);
    }

    private static Medidas.LinhaOciosidade ociosidade(int pendentes, String agenda) {
        return OCIOSIDADE.stream()
                .filter(l -> l.pendentes() == pendentes && l.agenda().equals(agenda))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. a carga ─────────────────────

    @Test
    @DisplayName("com o sistema quase ocioso, a roda custa doze vezes mais por agendamento")
    void comPoucaCargaARodaPerde() {
        assertThat(carga(1, "amontoado").porAgendamento()).isCloseTo(83.15, within(0.01));
        assertThat(carga(1, "roda de 256").porAgendamento()).isCloseTo(1002.90, within(0.01));

        assertThat(carga(1, "roda de 256").porAgendamento()
                / carga(1, "amontoado").porAgendamento())
                .as("o tique dela e dividido entre pouquissimos agendamentos")
                .isGreaterThan(12.0);
    }

    @Test
    @DisplayName("com carga alta a ordem inverte, e a roda ganha de todo mundo")
    void comCargaAltaARodaGanha() {
        assertThat(carga(10_000, "roda de 256").porAgendamento()).isCloseTo(3.09, within(0.01));
        assertThat(carga(10_000, "amontoado").porAgendamento()).isCloseTo(38.94, within(0.01));
        assertThat(carga(10_000, "lista ordenada").porAgendamento())
                .isCloseTo(679.76, within(0.01));

        assertThat(carga(10_000, "amontoado").porAgendamento()
                / carga(10_000, "roda de 256").porAgendamento())
                .as("doze vezes mais barata que o amontoado")
                .isGreaterThan(12.0);

        assertThat(carga(10_000, "lista ordenada").total())
                .as("e a lista ordenada vira quadratica: 136 milhoes contra 618 mil da roda")
                .isGreaterThan(100_000_000.0);
    }

    @Test
    @DisplayName("a virada esta entre 10 e 1000 agendamentos por mil tiques")
    void aViradaEstaNoMeio() {
        assertThat(carga(10, "amontoado").porAgendamento())
                .as("com dez por mil tiques o amontoado ainda ganha")
                .isLessThan(carga(10, "roda de 256").porAgendamento());

        assertThat(carga(1_000, "roda de 256").porAgendamento())
                .as("com mil por mil tiques a roda ja ganha, por sete vezes")
                .isLessThan(carga(1_000, "amontoado").porAgendamento() / 6);
    }

    private static Medidas.LinhaCarga carga(int porMilTiques, String agenda) {
        return CARGA.stream()
                .filter(l -> l.porMilTiques() == porMilTiques && l.agenda().equals(agenda))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. o horizonte ─────────────────────

    /**
     * A CORRECAO DO HORIZONTE.
     *
     * <p>Eu escrevi que o custo por tique da roda simples cresce linearmente com o
     * horizonte. Ele cresce ate saturar em pendentes dividido por baldes.
     */
    @Test
    @DisplayName("o custo da roda satura em pendentes sobre baldes, e nao cresce com o horizonte")
    void oCustoSaturaEmPendentesSobreBaldes() {
        assertThat(horizonte(16_384, "roda de 256").trabalhoPorTique())
                .isCloseTo(17.234, within(0.001));

        assertThat(horizonte(262_144, "roda de 256").trabalhoPorTique())
                .isCloseTo(38.488, within(0.001));

        assertThat(horizonte(262_144, "roda de 256").trabalhoPorTique()
                / horizonte(16_384, "roda de 256").trabalhoPorTique())
                .as("dezesseis vezes mais horizonte, pouco mais que o dobro de trabalho")
                .isLessThan(2.3);

        assertThat(horizonte(262_144, "roda de 256").entradasPorTique())
                .as("e o teto e 5.000 pendentes divididos por 256 baldes, que e 19,5")
                .isCloseTo(18.744, within(0.001));
    }

    @Test
    @DisplayName("a roda hierarquica nao sente o horizonte, que e para isso que ela existe")
    void aHierarquicaNaoSenteOHorizonte() {
        for (int horizonte : new int[] {256, 1_024, 16_384, 262_144})
            assertThat(horizonte(horizonte, "roda hierarquica").trabalhoPorTique())
                    .as("horizonte " + horizonte)
                    .isLessThan(3.3);

        assertThat(horizonte(262_144, "roda hierarquica").trabalhoPorTique())
                .as("com horizonte de 262 mil ela gasta 32 vezes menos que a simples")
                .isLessThan(horizonte(262_144, "roda de 256").trabalhoPorTique() / 30);
    }

    /**
     * A INVERSAO QUE EU NAO ESPERAVA: a lista ordenada ganha do amontoado.
     *
     * <p>Com 5.000 temporizadores vencendo em 256 tiques, cada disparo e uma retirada da
     * raiz do amontoado, que custa o logaritmo. A lista tira do fim do vetor de graca.
     */
    @Test
    @DisplayName("quando muitos vencem de uma vez, a lista ordenada ganha do amontoado")
    void aListaGanhaDoAmontoadoNoDisparo() {
        assertThat(horizonte(256, "lista ordenada").trabalhoPorTique())
                .isCloseTo(0.513, within(0.001));

        assertThat(horizonte(256, "amontoado").trabalhoPorTique())
                .isCloseTo(10.270, within(0.001));

        assertThat(horizonte(256, "amontoado").trabalhoPorTique()
                / horizonte(256, "lista ordenada").trabalhoPorTique())
                .as("vinte vezes mais, e as duas disparam exatamente o mesmo")
                .isGreaterThan(19.0);

        assertThat(horizonte(256, "amontoado").entradasPorTique())
                .as("as entradas vistas sao as mesmas: a diferenca e o conserto da arvore")
                .isEqualTo(horizonte(256, "lista ordenada").entradasPorTique());
    }

    private static Medidas.LinhaHorizonte horizonte(int horizonte, String agenda) {
        return HORIZONTE.stream()
                .filter(l -> l.horizonte() == horizonte && l.agenda().equals(agenda))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. o cancelamento ─────────────────────

    /**
     * A SEGUNDA CORRECAO.
     *
     * <p>Eu escrevi que a roda ganharia a tabela do cancelamento com folga, porque o balde
     * e pequeno. Quem ganha e o amontoado, e a roda hierarquica e a pior das tres.
     */
    @Test
    @DisplayName("quem cancela mais barato e o amontoado, e nao a roda")
    void oAmontoadoGanhaOCancelamento() {
        assertThat(cancelamento(20_000, "amontoado").porCancelamento())
                .isCloseTo(4.72, within(0.01));

        assertThat(cancelamento(20_000, "roda de 256").porCancelamento())
                .isCloseTo(39.55, within(0.01));

        assertThat(cancelamento(20_000, "roda hierarquica").porCancelamento())
                .as("e a hierarquica e a pior: os niveis grossos acumulam milhares por balde")
                .isCloseTo(381.44, within(0.01));

        assertThat(cancelamento(20_000, "amontoado").entradasVistas())
                .as("o amontoado nao olha entrada nenhuma: o mapa leva direto ao lugar")
                .isZero();
    }

    @Test
    @DisplayName("com poucos pendentes a roda ganha, e e so ai que ela ganha")
    void aRodaSoGanhaComPoucosPendentes() {
        assertThat(cancelamento(100, "roda de 256").porCancelamento())
                .isLessThan(cancelamento(100, "amontoado").porCancelamento());

        assertThat(cancelamento(2_000, "roda de 256").porCancelamento())
                .as("com dois mil elas ja empatam")
                .isCloseTo(cancelamento(2_000, "amontoado").porCancelamento(), within(0.5));

        assertThat(cancelamento(20_000, "roda de 256").porCancelamento())
                .as("e com vinte mil a roda ja perde por oito vezes")
                .isGreaterThan(cancelamento(20_000, "amontoado").porCancelamento() * 7);
    }

    @Test
    @DisplayName("a lista ordenada varre, e o custo dela cresce reto com os pendentes")
    void aListaVarre() {
        assertThat(cancelamento(100, "lista ordenada").porCancelamento())
                .isCloseTo(10.03, within(0.01));

        assertThat(cancelamento(20_000, "lista ordenada").porCancelamento())
                .isCloseTo(19507.07, within(0.01));

        assertThat(cancelamento(20_000, "lista ordenada").porCancelamento()
                / cancelamento(2_000, "lista ordenada").porCancelamento())
                .as("dez vezes mais pendentes, doze vezes mais trabalho")
                .isBetween(10.0, 14.0);
    }

    private static Medidas.LinhaCancelamento cancelamento(int pendentes, String agenda) {
        return CANCELAMENTO.stream()
                .filter(l -> l.pendentes() == pendentes && l.agenda().equals(agenda))
                .findFirst()
                .orElseThrow();
    }
}
