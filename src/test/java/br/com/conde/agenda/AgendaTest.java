package br.com.conde.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgendaTest {

    // ───────────────────── o juiz contra todas as agendas ─────────────────────

    @Test
    @DisplayName("toda agenda dispara os mesmos identificadores no mesmo tique e na mesma ordem")
    void todasBatemComOJuiz() {
        List<String> falhas = new ArrayList<>();
        int conferidas = 0;

        for (int tiques : new int[] {1, 5, 300, 2_000})
            for (int porTique : new int[] {0, 1, 4})
                for (int horizonte : new int[] {1, 7, 100, 5_000}) {
                    var roteiro = Juiz.roteiro(tiques, porTique, horizonte, 31);

                    for (var agenda : Agendas.todas().entrySet()) {
                        Juiz.Veredito veredito = Juiz.julgar(agenda.getValue().get(), roteiro);

                        conferidas++;

                        if (!veredito.passou())
                            falhas.add(agenda.getKey() + " com " + tiques + " tiques, "
                                    + porTique + " por tique, horizonte " + horizonte
                                    + ": " + veredito.falha());
                    }
                }

        assertThat(conferidas).isGreaterThan(100);
        assertThat(falhas).isEmpty();
    }

    /**
     * O HORIZONTE MAIOR QUE A RODA E O CASO QUE SEPARA AS DUAS RODAS.
     *
     * <p>Com horizonte de cinco mil e uma roda de 256 baldes, cada balde acumula marcas de
     * vinte voltas diferentes, e so as da volta atual vencem. Uma roda que esquecesse de
     * conferir o instante dispararia tudo vinte vezes cedo demais.
     */
    @Test
    @DisplayName("com horizonte muito maior que a roda, ela ainda dispara no tique certo")
    void horizonteMaiorQueARoda() {
        var roteiro = Juiz.roteiro(3_000, 2, 10_000, 7);

        for (var agenda : Agendas.todas().entrySet()) {
            Juiz.Veredito veredito = Juiz.julgar(agenda.getValue().get(), roteiro);

            assertThat(veredito.divergencias()).as(agenda.getKey() + ": " + veredito.falha())
                    .isZero();

            /*
             * SEIS MIL FORAM MARCADOS E 801 DISPARARAM, e essa diferenca e o teste.
             *
             * Com horizonte de dez mil tiques e so tres mil de relogio, a maioria dos
             * temporizadores ainda esta na roda quando o roteiro acaba. Eles sao
             * exatamente os que ficam encalhados nos baldes de voltas futuras, que e o que
             * esta tabela mede: eles nao disparam, e sao olhados em todo tique.
             */
            assertThat(veredito.disparos()).as(agenda.getKey()).isEqualTo(801);
        }
    }

    /**
     * O DESEMPATE POR ORDEM DE CHEGADA, isolado.
     *
     * <p>Sem ele, o amontoado devolveria a ordem da arvore e a roda a ordem do balde, as
     * duas certas sobre o instante e diferentes entre si.
     */
    @Test
    @DisplayName("marcados para o mesmo tique, disparam na ordem em que foram marcados")
    void oDesempatePorOrdemDeChegada() {
        for (var fabrica : Agendas.todas().entrySet()) {
            Agenda agenda = fabrica.getValue().get();
            Contador contador = new Contador();

            for (int i = 9; i >= 1; i--) agenda.agendar(50, i, contador);

            assertThat(agenda.andarAte(50, contador))
                    .as(fabrica.getKey())
                    .containsExactly(9, 8, 7, 6, 5, 4, 3, 2, 1);
        }
    }

    @Test
    @DisplayName("instantes diferentes saem em ordem de instante, e nao de agendamento")
    void aOrdemEDeInstante() {
        for (var fabrica : Agendas.todas().entrySet()) {
            Agenda agenda = fabrica.getValue().get();
            Contador contador = new Contador();

            agenda.agendar(30, 1, contador);
            agenda.agendar(10, 2, contador);
            agenda.agendar(20, 3, contador);

            assertThat(agenda.andarAte(100, contador))
                    .as(fabrica.getKey())
                    .containsExactly(2, 3, 1);
        }
    }

    @Test
    @DisplayName("andar o relogio sem nada marcado nao dispara nada")
    void relogioVazio() {
        for (var fabrica : Agendas.todas().entrySet()) {
            Agenda agenda = fabrica.getValue().get();
            Contador contador = new Contador();

            assertThat(agenda.andarAte(1_000, contador)).as(fabrica.getKey()).isEmpty();
            assertThat(agenda.agora()).as(fabrica.getKey()).isEqualTo(1_000);
            assertThat(agenda.pendentes()).as(fabrica.getKey()).isZero();
        }
    }

    @Test
    @DisplayName("marcar para o instante atual ou para o passado e recusado")
    void instanteNaoFuturo() {
        for (var fabrica : Agendas.todas().entrySet()) {
            Agenda agenda = fabrica.getValue().get();
            Contador contador = new Contador();

            agenda.andarAte(10, contador);

            assertThatThrownBy(() -> agenda.agendar(10, 1, contador))
                    .as(fabrica.getKey() + ", o instante atual")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nao e futuro");

            assertThatThrownBy(() -> agenda.agendar(9, 1, contador))
                    .as(fabrica.getKey() + ", o passado")
                    .isInstanceOf(IllegalArgumentException.class);

            agenda.agendar(11, 1, contador);

            assertThat(agenda.pendentes()).as(fabrica.getKey()).isEqualTo(1);
        }
    }

    // ───────────────────── cancelar ─────────────────────

    @Test
    @DisplayName("cancelar tira o temporizador, e cancelar duas vezes devolve falso")
    void cancelar() {
        for (var fabrica : Agendas.todas().entrySet()) {
            Agenda agenda = fabrica.getValue().get();
            Contador contador = new Contador();

            for (int i = 1; i <= 20; i++) agenda.agendar(i * 3, i, contador);

            assertThat(agenda.cancelar(7, contador)).as(fabrica.getKey()).isTrue();
            assertThat(agenda.cancelar(7, contador)).as(fabrica.getKey()).isFalse();
            assertThat(agenda.cancelar(999, contador)).as(fabrica.getKey()).isFalse();
            assertThat(agenda.pendentes()).as(fabrica.getKey()).isEqualTo(19);

            assertThat(agenda.andarAte(100, contador))
                    .as(fabrica.getKey())
                    .doesNotContain(7)
                    .hasSize(19);
        }
    }

    /**
     * CANCELAR A RAIZ DO AMONTOADO E O CASO QUE QUEBRA SE SO SUBIR OU SO DESCER.
     *
     * <p>A marca que vem do fim do vetor pode ser mais urgente que o pai do buraco ou menos
     * urgente que os filhos. Fazer so um dos dois conserto deixa o amontoado respondendo, e
     * respondendo errado.
     */
    @Test
    @DisplayName("cancelar qualquer posicao, inclusive a raiz e o fim, mantem a ordem certa")
    void cancelarEmQualquerPosicao() {
        for (int alvo = 1; alvo <= 30; alvo++)
            for (var fabrica : Agendas.todas().entrySet()) {
                Agenda agenda = fabrica.getValue().get();
                Contador contador = new Contador();
                Agendas.Sorteio sorteio = new Agendas.Sorteio(alvo);

                for (int i = 1; i <= 30; i++) agenda.agendar(1 + sorteio.ate(200), i, contador);

                assertThat(agenda.cancelar(alvo, contador)).isTrue();

                int[] dispararam = agenda.andarAte(500, contador);

                assertThat(dispararam).as(fabrica.getKey() + ", cancelando " + alvo)
                        .hasSize(29)
                        .doesNotContain(alvo);
            }
    }

    @Test
    @DisplayName("cancelar no meio da passagem do tempo nao atrapalha o resto")
    void cancelarComORelogioAndando() {
        for (var fabrica : Agendas.todas().entrySet()) {
            Agenda agenda = fabrica.getValue().get();
            Contador contador = new Contador();

            for (int i = 1; i <= 50; i++) agenda.agendar(i * 2, i, contador);

            Marca.Disparos saiu = new Marca.Disparos();

            for (int tique = 1; tique <= 100; tique++) {
                if (tique == 37) agenda.cancelar(25, contador);

                for (int id : agenda.andarAte(tique, contador)) saiu.disparou(id);
            }

            assertThat(saiu.quantos()).as(fabrica.getKey()).isEqualTo(49);
            assertThat(saiu.emVetor()).as(fabrica.getKey()).doesNotContain(25);
        }
    }

    // ───────────────────── o que cada estrutura promete sobre si ─────────────────────

    /**
     * A AFIRMACAO CENTRAL DO REPOSITORIO, isolada num teste.
     *
     * <p>Com o sistema ocioso, a roda abre um balde por tique e o amontoado olha a raiz. A
     * roda trabalha mais fazendo nada.
     */
    @Test
    @DisplayName("com o sistema ocioso, a roda trabalha por tique e o amontoado quase nao")
    void aRodaCobraPorTiqueMesmoOciosa() {
        Contador daRoda = new Contador();
        Contador doAmontoado = new Contador();

        new RodaSimples(256).andarAte(100_000, daRoda);
        new Amontoado().andarAte(100_000, doAmontoado);

        assertThat(daRoda.trabalhoPorTique())
                .as("um balde aberto em cada tique")
                .isEqualTo(1.0);

        assertThat(doAmontoado.trabalhoPorTique())
                .as("e com o amontoado vazio nem a raiz existe para olhar")
                .isZero();

        assertThat(new ListaOrdenada().pendentes()).isZero();
    }

    @Test
    @DisplayName("a roda agenda sem comparar nada, e a lista compara o logaritmo")
    void agendarNaRodaNaoCompara() {
        Contador daRoda = new Contador();
        Contador daLista = new Contador();

        Agendas.encher(new RodaSimples(256), 10_000, 200, 1, 5, daRoda);
        Agendas.encher(new ListaOrdenada(), 10_000, 200, 1, 5, daLista);

        assertThat(daRoda.comparacoes())
                .as("escolher o balde e uma conta, nao uma busca")
                .isZero();

        assertThat(daLista.comparacoes()).isGreaterThan(100_000);

        assertThat(daRoda.trabalhoPorAgendamento()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("a roda hierarquica recusa alem do horizonte em vez de errar calada")
    void oHorizonteDaRodaHierarquica() {
        RodaHierarquica roda = new RodaHierarquica(4, 2);

        assertThat(roda.horizonte()).isEqualTo(256);

        Contador contador = new Contador();

        roda.agendar(255, 1, contador);

        assertThatThrownBy(() -> roda.agendar(256, 2, contador))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horizonte");

        assertThat(RodaHierarquica.comoNoLinux().horizonte())
                .as("quatro niveis de 64 cobrem dezesseis milhoes de tiques")
                .isEqualTo(16_777_216L);
    }

    @Test
    @DisplayName("a roda simples exige potencia de dois")
    void aRodaExigePotenciaDeDois() {
        assertThatThrownBy(() -> new RodaSimples(100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("potencia de dois");

        assertThatThrownBy(() -> new RodaHierarquica(0, 4))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new RodaHierarquica(16, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A CASCATA ACONTECE, e ela precisa de um teste que a observe de verdade.
     *
     * <p>Um temporizador marcado para alem da roda de baixo entra num nivel de cima e
     * precisa descer antes de disparar. Se a cascata nao acontecesse, ele simplesmente nao
     * dispararia, e nenhum teste de cancelamento pegaria isso.
     */
    @Test
    @DisplayName("a cascata faz a marca descer de nivel e disparar no tique certo")
    void aCascataAcontece() {
        RodaHierarquica roda = new RodaHierarquica(4, 3);
        Contador contador = new Contador();

        roda.agendar(1_000, 1, contador);

        assertThat(roda.andarAte(999, contador)).isEmpty();

        assertThat(contador.movimentacoes())
                .as("ela desceu de nivel pelo caminho")
                .isGreaterThan(0);

        assertThat(roda.andarAte(1_000, contador)).containsExactly(1);
        assertThat(roda.pendentes()).isZero();
    }

    // ───────────────────── o sorteio da casa ─────────────────────

    /**
     * O TESTE DO DESLOCAMENTO DE 32.
     *
     * <p>Aqui ele seria devastador: o balde da roda e o instante por resto de uma potencia
     * de dois, ou seja, exatamente os bits baixos. Instantes com bits baixos ruins cairiam
     * sempre nos mesmos baldes e a roda pareceria pior do que e.
     */
    @Test
    @DisplayName("o deslocamento de 32 e o que espalha os instantes pelos baldes")
    void oDeslocamentoDeTrintaEDois() {
        Agendas.Sorteio sorteio = new Agendas.Sorteio(1);
        int[] porBalde = new int[64];

        for (int i = 0; i < 64_000; i++) porBalde[sorteio.ate(100_000) & 63]++;

        for (int balde = 0; balde < 64; balde++)
            assertThat(porBalde[balde]).as("balde " + balde).isBetween(850, 1_150);

        long estado = 1;
        int alternancias = 0;
        int anterior = -1;

        for (int i = 0; i < 1_000; i++) {
            estado = estado * 6364136223846793005L + 1442695040888963407L;

            int baixo = (int) (estado & 1);

            if (anterior >= 0 && baixo != anterior) alternancias++;

            anterior = baixo;
        }

        assertThat(alternancias)
                .as("o bit baixo do estado cru alterna em TODAS, e ele e o balde")
                .isEqualTo(999);
    }
}
