package br.com.conde.agenda;

/**
 * UMA AGENDA DE TEMPORIZADORES: marcar para um instante, andar o relogio, ver o que venceu.
 *
 * <p>O tempo aqui e um contador inteiro de TIQUES, e nao um relogio de verdade. Isso nao e
 * simplificacao: e assim que um escalonador de sistema operacional funciona por dentro. Ele
 * recebe uma interrupcao periodica, soma um, e pergunta o que venceu.
 *
 * <h2>O relogio anda de um em um, e essa e a regra que decide tudo</h2>
 *
 * <p>{@link #andarAte} nao pode pular para o instante pedido: ela tem que passar por cada
 * tique no caminho. Uma roda de tempo que pulasse perderia os baldes do meio, e uma agenda
 * que so dispara quando alguem pergunta nao e uma agenda.
 *
 * <p>E e justamente por isso que a roda tem um custo por tique e o amontoado nao: os dois
 * passam pelos mesmos tiques, e so um deles faz trabalho em cada um.
 *
 * <p>Todas as implementacoes disparam exatamente os mesmos identificadores, no mesmo
 * instante e na mesma ordem.
 */
public interface Agenda {

    /**
     * Marca o identificador para disparar no instante dado.
     *
     * <p>O instante e absoluto e tem que estar no futuro ou no presente. Marcar para o
     * passado seria uma pergunta sem resposta boa: disparar na hora, ignorar e recusar sao
     * todas defensaveis, e escolher uma escondia a decisao dentro da estrutura.
     */
    void agendar(long quando, int identificador, Contador contador);

    /**
     * Anda o relogio ate o instante dado, tique por tique, e devolve o que disparou.
     *
     * <p>Em ordem de instante. Dentro do mesmo instante, em ordem de agendamento: dois
     * temporizadores marcados para o mesmo tique disparam na ordem em que foram marcados, e
     * isso e uma promessa e nao um acaso da estrutura.
     */
    int[] andarAte(long instante, Contador contador);

    /** Desmarca o identificador. Devolve se ele estava marcado. */
    boolean cancelar(int identificador, Contador contador);

    /** Quantos temporizadores estao marcados e ainda nao dispararam. */
    int pendentes();

    long agora();

    String nome();
}
