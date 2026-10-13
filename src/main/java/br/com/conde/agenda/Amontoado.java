package br.com.conde.agenda;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * O AMONTOADO: uma arvore binaria num vetor, com a proxima marca na raiz.
 *
 * <p>E a escolha padrao de quase toda biblioteca de temporizadores, e com razao: logaritmo
 * para agendar, logaritmo para disparar, e NADA por tique quando nao ha o que disparar.
 *
 * <p>Essa ultima parte e a que as tabelas de complexidade nao contam e que esta medida aqui.
 * Com o sistema ocioso ele faz UMA comparacao por tique: olha a raiz, ve que nao venceu e
 * vai embora. A roda de tempo visita um balde.
 *
 * <h2>O mapa de posicoes</h2>
 *
 * <p>Cancelar por identificador num amontoado exige saber ONDE o identificador esta, e a
 * posicao muda a cada troca. O mapa daqui e atualizado em toda troca, e e o preco de
 * cancelar em logaritmo em vez de varrer.
 *
 * <p>Sem ele, cancelar custaria uma varredura igual a da lista ordenada, e o amontoado
 * deixaria de ter vantagem nenhuma naquela coluna. Com ele, ele GANHA a coluna de todo
 * mundo, inclusive das rodas: 4,72 contra 39,55 com vinte mil pendentes.
 *
 * <h2>Onde ele perde, e perde para a lista ordenada</h2>
 *
 * <p>Quando muitos temporizadores vencem no mesmo intervalo, cada disparo e uma retirada da
 * raiz, e consertar o amontoado custa o logaritmo. A lista ordenada tira do fim do vetor e
 * nao conserta nada.
 *
 * <p>Com 5.000 temporizadores vencendo em 256 tiques, sao 10,27 unidades de trabalho por
 * tique contra 0,51 da lista: vinte vezes mais. A estrutura que ganha no agendamento perde
 * no disparo, e a tabela do horizonte mostra as duas pontas.
 */
public final class Amontoado implements Agenda {

    private Marca[] marcas = new Marca[16];
    private final Map<Integer, Integer> posicaoDe = new HashMap<>();
    private int quantas;
    private long agora;
    private long proximaOrdem;

    @Override
    public String nome() {
        return "amontoado";
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

        porEm(quantas, new Marca(quando, proximaOrdem++, identificador));

        subir(quantas++, contador);
    }

    @Override
    public int[] andarAte(long instante, Contador contador) {
        Marca.Disparos disparos = new Marca.Disparos();

        while (agora < instante) {
            agora++;
            contador.tique();

            while (quantas > 0) {
                contador.compara();

                if (marcas[0].quando() > agora) break;

                contador.veEntrada();

                disparos.disparou(marcas[0].identificador());

                tirarARaiz(contador);
            }
        }

        return disparos.emVetor();
    }

    @Override
    public boolean cancelar(int identificador, Contador contador) {
        contador.cancelamento();

        Integer onde = posicaoDe.get(identificador);

        if (onde == null) return false;

        int i = onde;

        posicaoDe.remove(identificador);

        quantas--;

        if (i == quantas) {
            marcas[quantas] = null;
            return true;
        }

        porEm(i, marcas[quantas]);

        marcas[quantas] = null;

        /*
         * A MARCA QUE VEIO DO FIM PODE PRECISAR SUBIR OU DESCER. Subir quando ela e mais
         * urgente que o pai do buraco, descer quando e menos urgente que os filhos. Fazer
         * so uma das duas quebra o amontoado em silencio: ele continua respondendo, e
         * responde errado.
         */
        subir(i, contador);
        descer(i, contador);

        return true;
    }

    private void tirarARaiz(Contador contador) {
        posicaoDe.remove(marcas[0].identificador());

        quantas--;

        if (quantas == 0) {
            marcas[0] = null;
            return;
        }

        porEm(0, marcas[quantas]);

        marcas[quantas] = null;

        descer(0, contador);
    }

    private void subir(int i, Contador contador) {
        while (i > 0) {
            int pai = (i - 1) / 2;

            contador.compara();

            if (!marcas[i].antesDe(marcas[pai])) break;

            trocar(i, pai, contador);

            i = pai;
        }
    }

    private void descer(int i, Contador contador) {
        while (true) {
            int menor = i;
            int esquerda = 2 * i + 1;
            int direita = 2 * i + 2;

            if (esquerda < quantas) {
                contador.compara();
                if (marcas[esquerda].antesDe(marcas[menor])) menor = esquerda;
            }

            if (direita < quantas) {
                contador.compara();
                if (marcas[direita].antesDe(marcas[menor])) menor = direita;
            }

            if (menor == i) return;

            trocar(i, menor, contador);

            i = menor;
        }
    }

    private void trocar(int a, int b, Contador contador) {
        Marca guarda = marcas[a];

        porEm(a, marcas[b]);
        porEm(b, guarda);

        contador.move(2);
    }

    private void porEm(int i, Marca marca) {
        marcas[i] = marca;
        posicaoDe.put(marca.identificador(), i);
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
