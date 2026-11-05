# Player

## Objetivo

O player deve parecer parte do Telumia, não uma ferramenta técnica sobre o vídeo. Controles,
prévia da timeline, informações de cena e momentos salvos compartilham as mesmas superfícies,
raios e hierarquia do restante do aplicativo.

## Timeline e prévia

- A prévia usa somente frames decodificados do vídeo atual. Poster, fanart ou imagem inventada
  nunca substituem uma cena.
- O quadro solicitado é o elemento principal; frames vizinhos formam um filmstrip secundário,
  em ordem temporal, com o frame atual claramente destacado.
- A prévia fica centralizada no espaço disponível e permanece dentro do viewport em 16:9 e
  layouts compactos.
- Marcadores de abertura, créditos, capítulos e momentos salvos não capturam foco nem impedem
  o seek.
- Animações usam apenas opacidade e transformação. O modo de movimento reduzido preserva
  geometria e função; o modo desligado remove transições.

## Momentos salvos

- A superfície usa fundo cinematográfico, título com acento da marca e cartões para separar
  cada momento.
- Salvar, renomear, remover e pular mantêm suporte completo a teclado, mouse e D-pad.
- Um timestamp salvo em outra fonte exige confirmação explícita porque cortes podem divergir.
- Estados de carregamento ou erro nunca descartam dados existentes nem habilitam mutações.

## Informações da cena

- A interface pode mostrar capítulo, pessoas em cena, música e curiosidades somente quando
  provedores reais e confiáveis entregarem esses dados.
- Na ausência de dados verificados, mostrar um estado vazio claro; não simular um produto
  “X-Ray”.
- Cada entrada usa um cartão e um rótulo de categoria, mantendo leitura rápida sobre o vídeo.

## Desempenho

- Prévia e filmstrip compartilham uma única fila de extração e cache limitado.
- Superfícies usam gradientes estáticos e evitam blur em tempo real, especialmente em TV Box.
- Extração é suspensa quando o player perde primeiro plano, entra em PiP ou abre outro modal.

## Validacao em 2026-11-05

- 34 testes aprovados: NativeTimelineFramesTest (6, sem skips), DesktopTimelineFramesTest (5), PlayerSceneInfoDesktopTest (2) e PlayerTrackSelectionTest (21).
- Integracao Windows usa JNI/libmpv empacotado, videos locais e servidor HTTP de teste: capitulos UTF-8, extracao de frames e preservacao da posicao do player principal.
- Isto nao comprova todos os formatos de addons, fluidez em hardware variado nem a navegacao completa de um filme da conta do usuario.

## Preferencias e proximo episodio

- Corrigida persistencia de subtitleIsForced no Desktop, alinhada aos armazenamentos Android/iOS: true, false e ausencia sao valores distintos.
- Teste de gravacao/leitura confirma round-trip de pt-BR, audio e indicador forced, alem da limpeza de preferencia.
- PlayerNextEpisodeStreamSelectionTest cobre espera pela fonte correspondente e selecao manual quando nao ha correspondencia.
- Isolamento entre perfis e sincronizacao destas preferencias ainda exigem verificacao integrada.

