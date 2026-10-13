package br.com.conde.agenda;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("ociosidade") || comando.equals("tudo")) ociosidade();
        if (comando.equals("carga") || comando.equals("tudo")) carga();
        if (comando.equals("horizonte") || comando.equals("tudo")) horizonte();
        if (comando.equals("cancelamento") || comando.equals("tudo")) cancelamento();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODA AGENDA DISPARA O MESMO, NO MESMO TIQUE E NA MESMA ORDEM.");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long erradas = linhas.stream().filter(l -> l.divergencias() > 0).count();

        System.out.printf("  %-20s %-14s %10s %12s %14s%n",
                "agenda", "carga", "tiques", "disparos", "divergencias");

        for (var l : linhas)
            System.out.printf("  %-20s %-14s %10d %12d %14d%n",
                    l.agenda(), l.carga(), l.tiques(), l.disparos(), l.divergencias());

        System.out.println();
        System.out.printf("  %d combinacoes, %d com divergencia.%n", linhas.size(), erradas);
        System.out.println();
    }

    private static void ociosidade() {
        System.out.println();
        System.out.println("O CUSTO POR TIQUE COM O SISTEMA OCIOSO, em " + Medidas.TIQUES
                + " tiques e nada vencendo.");
        System.out.println();
        System.out.printf("  %12s %-20s %20s %16s %16s%n",
                "pendentes", "agenda", "trabalho por tique", "comparacoes", "baldes abertos");

        for (var l : Medidas.aOciosidade())
            System.out.printf(Locale.ROOT, "  %12d %-20s %20.4f %16d %16d%n",
                    l.pendentes(), l.agenda(), l.trabalhoPorTique(), l.comparacoes(), l.baldes());

        System.out.println();
    }

    private static void carga() {
        System.out.println();
        System.out.println("O CUSTO POR TEMPORIZADOR AGENDADO, com o relogio andando.");
        System.out.println();
        System.out.printf("  %18s %-20s %22s %20s %16s%n",
                "por mil tiques", "agenda", "trabalho por agendar", "trabalho por tique",
                "trabalho total");

        for (var l : Medidas.aCarga())
            System.out.printf(Locale.ROOT, "  %18d %-20s %22.2f %20.4f %16.0f%n",
                    l.porMilTiques(), l.agenda(), l.porAgendamento(), l.porTique(), l.total());

        System.out.println();
    }

    private static void horizonte() {
        System.out.println();
        System.out.println("O QUE O HORIZONTE FAZ COM A RODA, com 5.000 temporizadores.");
        System.out.println();
        System.out.printf("  %12s %-20s %20s %22s%n",
                "horizonte", "agenda", "trabalho por tique", "entradas por tique");

        for (var l : Medidas.oHorizonte())
            System.out.printf(Locale.ROOT, "  %12d %-20s %20.3f %22.3f%n",
                    l.horizonte(), l.agenda(), l.trabalhoPorTique(), l.entradasPorTique());

        System.out.println();
    }

    private static void cancelamento() {
        System.out.println();
        System.out.println("O CUSTO DE CANCELAR, em mil cancelamentos sorteados.");
        System.out.println();
        System.out.printf("  %12s %-20s %24s %20s%n",
                "pendentes", "agenda", "trabalho por cancelar", "entradas vistas");

        for (var l : Medidas.oCancelamento())
            System.out.printf(Locale.ROOT, "  %12d %-20s %24.2f %20d%n",
                    l.pendentes(), l.agenda(), l.porCancelamento(), l.entradasVistas());

        System.out.println();
    }
}
