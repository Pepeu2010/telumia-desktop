# Biblioteca

## Intencao visual

- O cabecalho usa uma transicao vertical entre `surfaceContainerHigh` e o fundo para separar navegacao, origem e conteudo sem criar uma segunda barra pesada.
- A origem ativa permanece visivel junto aos controles da biblioteca; colecoes e listas continuam sendo o foco da tela.
- Divisores, fundos e estados devem usar tokens semanticos do tema. Cores literais ficam restritas a imagens e marcas externas.

## Hierarquia e estados

- Ordem: titulo e origem, troca de fonte, filtros, grade ou lista.
- Carregamento preserva a geometria do conteudo para evitar saltos.
- Estado vazio explica a fonte selecionada e oferece uma acao util.
- Falha de sincronizacao mantem o conteudo local disponivel e apresenta tentativa novamente sem bloquear a tela.

## Interacao e desempenho

- Mouse, teclado e controle exibem foco ou hover sem mudar o tamanho do layout.
- Grades permanecem virtualizadas; imagens usam o pipeline de cache existente.
- Movimento reduzido remove deslocamentos decorativos, mas preserva feedback de estado.
