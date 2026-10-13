package br.com.conde.agenda;

/**
 * O CONTADOR: trabalho por tique e trabalho por operacao. Nunca segundos.
 *
 * <p>Sao duas contas e elas nao se misturam, e essa separacao e o repositorio inteiro.
 *
 * <p>O TRABALHO POR OPERACAO e o que a notacao O-grande fala: quanto custa agendar um
 * temporizador, quanto custa cancelar. E ai a roda de tempo ganha de todo mundo, porque ela
 * agenda em tempo constante.
 *
 * <p>O TRABALHO POR TIQUE e o que ela nao fala, e e onde a roda cobra. Um relogio que anda
 * um tique por milissegundo anda mil vezes por segundo esteja ou nao esteja acontecendo
 * alguma coisa, e a roda visita um balde em cada um deles. O amontoado olha o topo, ve que
 * nao venceu e vai embora.
 *
 * <p>Num servidor com dez temporizadores e um relogio de mil tiques por segundo, essa e a
 * conta que decide, e e invisivel em qualquer tabela de complexidade.
 *
 * <p>Segundos nao serviriam: a integracao continua roda em tres sistemas, e a diferenca
 * entre percorrer um vetor contiguo e seguir ponteiros de uma lista some ou aparece conforme
 * o processador. Aqui a pergunta e quanto trabalho existe, nao quao rapido ele passa.
 */
public final class Contador {

    private long comparacoes;
    private long baldesVisitados;
    private long entradasVistas;
    private long movimentacoes;
    private long tiques;
    private long agendamentos;
    private long cancelamentos;

    /** Dois instantes foram comparados. */
    public void compara() {
        comparacoes++;
    }

    /** Um balde da roda foi aberto. */
    public void visitaBalde() {
        baldesVisitados++;
    }

    /** Uma entrada guardada foi olhada. */
    public void veEntrada() {
        entradasVistas++;
    }

    public void veEntrada(long quantas) {
        entradasVistas += quantas;
    }

    /** Uma entrada mudou de lugar: deslocamento na lista, troca no amontoado, cascata. */
    public void move(long quantas) {
        movimentacoes += quantas;
    }

    public void tique() {
        tiques++;
    }

    public void agendamento() {
        agendamentos++;
    }

    public void cancelamento() {
        cancelamentos++;
    }

    public long comparacoes() {
        return comparacoes;
    }

    public long baldesVisitados() {
        return baldesVisitados;
    }

    public long entradasVistas() {
        return entradasVistas;
    }

    public long movimentacoes() {
        return movimentacoes;
    }

    public long tiques() {
        return tiques;
    }

    public long agendamentos() {
        return agendamentos;
    }

    public long cancelamentos() {
        return cancelamentos;
    }

    /**
     * O trabalho total, somando o que cada estrutura faz do jeito dela.
     *
     * <p>Somar comparacao com balde visitado e com entrada olhada nao e exato: as tres sao
     * trabalho de natureza um pouco diferente. Mas as tres sao "uma coisa que o processador
     * fez porque a estrutura mandou", e uma coluna so e o que permite ordenar as estruturas.
     * As colunas separadas ficam do lado para quem quiser desfazer a soma.
     */
    public long trabalho() {
        return comparacoes + baldesVisitados + entradasVistas + movimentacoes;
    }

    public double trabalhoPorTique() {
        return tiques == 0 ? 0 : (double) trabalho() / tiques;
    }

    public double trabalhoPorAgendamento() {
        return agendamentos == 0 ? 0 : (double) trabalho() / agendamentos;
    }

    public double trabalhoPorCancelamento() {
        return cancelamentos == 0 ? 0 : (double) trabalho() / cancelamentos;
    }

    public void zerar() {
        comparacoes = 0;
        baldesVisitados = 0;
        entradasVistas = 0;
        movimentacoes = 0;
        tiques = 0;
        agendamentos = 0;
        cancelamentos = 0;
    }
}
