# agenda

Temporizadores do zero em Java 21: lista ordenada, amontoado binário, roda de tempo e roda
hierárquica como a do Linux.

O juiz é a **lista ordenada**. Ela é o juiz certo e não só o conveniente: não há nela balde a
escolher, cascata a fazer nem árvore a consertar. A próxima a disparar é a última do vetor.

E ele confere a **sequência inteira** de disparos, tique por tique, não o conjunto do fim.
Uma roda que dispare o temporizador certo no tique errado acerta o conjunto e erra a agenda.
E confere a **ordem dentro do mesmo tique**: dois temporizadores marcados para o mesmo
instante disparam na ordem em que foram marcados.

São **duas** unidades e elas não se misturam. O **trabalho por operação** é o que a notação
O-grande fala, e aí a roda ganha de todo mundo. O **trabalho por tique** é o que ela não
fala, e é onde a roda cobra.

## A conta que a complexidade por operação não mostra

```
$ mvn exec:java -Dexec.args="ociosidade"

     pendentes agenda                 trabalho por tique      comparacoes   baldes abertos
             0 lista ordenada                     0.0000                0                0
             0 amontoado                          0.0000                0                0
             0 roda de 256                        1.0000                0           100000
             0 roda hierarquica                   1.0159                0           101586
         10000 lista ordenada                     1.0000           100000                0
         10000 amontoado                          1.0000           100000                0
         10000 roda de 256                       79.1248          3906240           100000
         10000 roda hierarquica                   1.1371             4829           104016
```

**Nada vence durante esta medida.** Os temporizadores estão todos marcados para depois do fim
dela: a pergunta é quanto cada estrutura trabalha para não fazer nada.

Com a agenda vazia, a lista e o amontoado fazem **zero** e a roda faz **um**: ela abre o balde
daquele tique para descobrir que ele está vazio. Num servidor com relógio de mil tiques por
segundo, isso acontece mil vezes por segundo pelo resto da vida do processo.

E com dez mil marcados para o futuro, a roda simples sobe para **79,12 por tique**, enquanto o
amontoado continua em 1,00. Os dez mil estão espalhados pelos 256 baldes, e cada tique olha os
trinta e nove que calharam de estar naquele.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `ociosidade`, `carga`, `horizonte`, `cancelamento` e
`tudo`.

## O que as medidas me corrigiram

**O custo da roda não cresce com o horizonte: ele satura em pendentes sobre baldes.**

Eu escrevi que, com os temporizadores marcados para longe, cada balde acumula uma marca por
volta e o custo por tique cresce linearmente com o horizonte.

```
$ mvn exec:java -Dexec.args="horizonte"

     horizonte agenda                 trabalho por tique     entradas por tique
           256 roda de 256                         1.500                  0.250
          1024 roda de 256                         2.258                  0.629
         16384 roda de 256                        17.234                  8.117
        262144 roda de 256                        38.488                 18.744
           256 roda hierarquica                    2.022                  0.439
        262144 roda hierarquica                    1.192                  0.055
```

De 16.384 para 262.144 tiques, **dezesseis vezes mais longe**, o custo vai de 17,2 para 38,5:
pouco mais que o dobro.

Com 5.000 temporizadores e 256 baldes, cada balde guarda **5.000/256 = 19,5** marcas, e a
tabela mede 18,744. Quem decide não é o horizonte: é **pendentes dividido por baldes**. Depois
que o horizonte passa do tamanho da roda, as marcas já estão espalhadas por todos os baldes
por igual e a conta para de piorar.

A roda hierárquica não sente nada disso, que é exatamente para o que ela existe: 1,192 com
horizonte de 262 mil, trinta e duas vezes menos que a simples.

**A lista ordenada ganha do amontoado quando muitos vencem de uma vez.**

Na mesma tabela, com horizonte de 256 e 5.000 temporizadores vencendo nesses 256 tiques:

```
           256 lista ordenada                      0.513                  0.250
           256 amontoado                          10.270                  0.250
```

As duas olham **exatamente as mesmas 0,250 entradas por tique**: disparam o mesmo. A diferença
inteira é o conserto da árvore. Cada disparo do amontoado é uma retirada da raiz, que custa o
logaritmo; a lista tira do fim do vetor e não conserta nada.

Vinte vezes mais trabalho, e o amontoado é a estrutura que "todo mundo usa para isso".

**Quem cancela mais barato é o amontoado, e não a roda.**

Eu escrevi que a roda ganharia essa tabela com folga, porque o balde é pequeno.

```
$ mvn exec:java -Dexec.args="cancelamento"

     pendentes agenda                  trabalho por cancelar      entradas vistas
           100 roda de 256                              0.22                  115
           100 amontoado                                0.42                    0
         20000 amontoado                                4.72                    0
         20000 roda de 256                             39.55                38565
         20000 roda hierarquica                        381.44               380464
         20000 lista ordenada                       19507.07             10098688
```

Com cem pendentes a roda ganha. Com vinte mil ela perde por **oito vezes**, e a hierárquica
perde por **oitenta**.

O balde só é pequeno enquanto há poucos temporizadores. Com vinte mil marcas em 256 baldes são
78 por balde, e varrer 78 é pior que os treze passos de um amontoado de vinte mil. O logaritmo
cresce devagar; pendentes dividido por baldes cresce reto.

E a hierárquica é a pior das três, o que parece contraditório e não é: os temporizadores
marcados para longe se acumulam nos níveis grossos, onde um único balde guarda milhares de
marcas que ainda vão descer.

**E a virada da roda está muito mais alta do que a complexidade sugere.**

```
$ mvn exec:java -Dexec.args="carga"

      por mil tiques agenda                 trabalho por agendar   trabalho por tique   trabalho total
                   1 amontoado                             83.15               0.0832             1663
                   1 roda de 256                         1002.90               1.0029            20058
                1000 amontoado                             27.58              27.5821           551642
                1000 roda de 256                            3.99               3.9900            79800
               10000 lista ordenada                       679.76            6797.6103        135952206
               10000 amontoado                             38.94             389.4494          7788988
               10000 roda de 256                            3.09              30.9020           618040
```

Com **um** agendamento a cada mil tiques, a roda custa 1002,90 por agendamento contra 83,15 do
amontoado: doze vezes mais. O tique dela é real e está sendo dividido por pouquíssimos
agendamentos.

Com **dez mil** por mil tiques, ou seja dez por tique, ela custa 3,09 contra 38,94: doze vezes
menos. A virada está entre 10 e 1000 por mil tiques.

"A roda de tempo agenda em tempo constante" é verdade e não basta. A pergunta certa é quantos
agendamentos existem por tique, e num sistema quase ocioso a resposta faz ela perder.

## O que não tem aqui

O relógio anda de um em um, tique por tique, e isso é uma escolha: uma agenda de produção
pula direto para o próximo vencimento quando pode, e aí a conta do tique desaparece para
todo mundo. Esse salto é a resposta moderna ao problema que esta tabela mede, e ele só é
possível porque o amontoado sabe qual é o próximo vencimento sem procurar.

Também não há temporizador periódico nem precisão de relógio real. Nenhuma medida daqui
depende disso.

## Licença

MIT.
