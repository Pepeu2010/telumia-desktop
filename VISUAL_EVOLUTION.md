# Evolução visual

Este documento acompanha mudanças de interface que afetam Desktop, celular e TV.
Ele registra o que já entrou no código e o que ainda precisa de validação visual em dispositivo.

## Etapa anterior: capas com foco de TV

- A prévia expandida existente passa a responder ao foco D-pad em aparelhos Android TV e ao hover/foco no Desktop.
- O cartão focado recebe uma elevação curta: 1,07x na TV para a leitura à distância e 1,045x no Desktop para manter a grade estável. O efeito respeita a preferência por movimento reduzido.
- A navegação superior em tablet/TV deixa de renderizar duas barras sobrepostas; permanece a barra flutuante única, com rótulos que se expandem conforme o contexto.
- O hero principal também passa da arte estática para o trailer com uma transição suave quando hover ou foco permanece estável. Desktop, Android TV e as prévias de cartaz compartilham um único slot de vídeo.
- A prévia de trailer só é elegível em Desktop e Android TV. Ela respeita as preferências de prévia e som; o padrão continua sem som, e o foco precisa estabilizar antes de resolver o trailer.
- Android TV é detectada por `PackageManager.FEATURE_LEANBACK`, com o tipo de UI de televisão como fallback.
- As configurações de trailer aparecem no Desktop e na TV, não em celulares de toque.
- Preferências Android antigas são migradas para habilitar a novidade que antes não estava disponível. Preferências explícitas do Desktop permanecem preservadas.

Esta entrega cobre interações com capas e previews; não representa a reformulação completa das telas, nem validação em uma TV física.

## Entrega atual: Home cinematográfica

- O hero de Desktop agora ocupa cerca de 68% da altura disponível, mantendo tela cheia em ultrawide e abrindo espaço para as fileiras mais cedo.
- TVs Android com viewport de pelo menos 1000 dp usam a mesma composição panorâmica: arte em tela ampla, degradês laterais/inferiores, título e metadados à esquerda, ação de detalhes e indicadores do destaque. Celulares, tablets e TVs menores preservam o layout compacto.
- A primeira fileira disponível (continuar assistindo, próximos episódios ou catálogo) sobe sobre o degradê inferior do hero. A medição também é reduzida pelo mesmo valor, então a fileira seguinte começa sem um vão artificial. A regra só vale enquanto o hero estiver habilitado.
- A troca do destaque dá profundidade ao conteúdo com um deslocamento vertical e uma redução de escala discretos, sincronizados ao movimento do pager. O efeito espacial segue a preferência de movimento reduzido/desativado.
- Os cards da Home agora fazem a borda crescer e acender em etapas entre repouso, hover e foco D-pad; as durações usam os tokens globais de movimento.
- O preview expandido fica mais amplo em Desktop (448 dp) e TV (520 dp), limitado pela largura disponível da janela. Sem banner ou pôster, usa um degradê com luz de acento, em vez de uma área lisa.
- Em 31/10/2026, `HomeHeroSectionTest` (11 casos) e `HomePosterPreviewPolicyTest` (8 casos) passaram em Desktop; `:androidApp:assembleFullDebug -Pnuvio.android.distribution=full` compilou o app Android/TV. Ainda falta validar a composição com conta, addons e artes reais em uma tela física; este passo não publicou um release.
- Quando um destaque não traz banner ou pôster, o hero mantém uma base cinematográfica em tons profundos de azul-petróleo em vez de cair num bloco de cor chapado; a arte real continua por cima quando disponível.
- Em telas cinematográficas, as fileiras podem usar todo o catálogo já carregado; cada trilho continua virtualizado, evitando compor todos os cards simultaneamente.
- `HomeHeroSectionTest` verifica a geometria por viewport e `HomeHeroShelfOverlapTest` confirma a sobreposição sem espaço entre fileiras. `TelumiaHomeExperienceTest` valida foco, ação e dimensões em quatro resoluções Desktop.

Os testes automatizados do hero usam metadados sintéticos e não afirmam cobertura visual de banners reais. A instalação Desktop 1.0.6 com conta e addons foi capturada separadamente na auditoria abaixo.

Em 2026-10-31, `HomeHeroSectionTest`, `TelumiaHomeExperienceTest` e `HomeHeroShelfOverlapTest` passaram no Desktop. `:androidApp:assembleFullDebug -Pnuvio.android.distribution=full` também compilou o APK Android TV/Android, usando o SDK já instalado no computador. Isso confirma compilação e layout, mas não substitui uma captura com conta e addons reais.

## Etapa anterior: ações na tela de detalhes

- O botão principal e as ações secundárias da tela de detalhes agora reagem ao foco do controle remoto e ao hover do Desktop com elevação curta e contorno de destaque.
- A proporção usa a mesma política de 1,045x no Desktop e 1,07x na TV aplicada às capas; movimento reduzido desativa a escala sem remover o contorno de foco.
- Formatos, contornos e transições usam os tokens de raio, borda e movimento do projeto. A ordem de ações e os callbacks existentes foram preservados.
- `DetailActionButtonsTest` confirma que Play recebe foco e pode ser acionado pelo teclado. `CinematicMovieDetailsTest` e `PosterEmphasisScaleTest` também passaram; `assembleFullDebug` compila a implementação compartilhada para Android.

Não havia addons nem dados de catálogo no perfil do emulador, então esta etapa não foi validada em uma tela de detalhes com conteúdo real.

## Validação em emulador Android TV

Em 2026-10-31, a navegação passou a expor a aba Live TV no Desktop e na Android TV; ela permanece fora da navegação de celulares touch. O APK local `fullDebug` compilou e iniciou no emulador Android TV AOSP x86 (1080p). A captura `telumia-tv-live-open.png` confirma a aba selecionada e a tela Live TV aberta. Sem addons no perfil local, a tela mostra o estado vazio de canais.

A navegação superior não mostra mais a barra duplicada: Home e Search renderizam uma única barra, e `DPAD_RIGHT` seguido de `DPAD_CENTER` navega até Search com o foco selecionado. Não houve exceção fatal no `AndroidRuntime` durante essa validação.

O emulador não tinha conta nem addons configurados. Por isso, Home, Search e Live TV exibem estados vazios, e não foi possível validar reprodução de trailer ou canais com catálogos reais. Esta verificação não substitui testes em uma TV física. Capturas temporárias estão em `composeApp/build/reports/telumia-tv-*.png`, incluindo `telumia-tv-search-after-fix.png` e `telumia-tv-live-open.png`.

Em 01/11/2026, o APK de validação atualizado também foi instalado e aberto no mesmo emulador. `telumia-tv-current-after.png` mostra o estado vazio da Home sem conta/addons e o processo Telumia permaneceu aberto. O log do Android registrou uma exceção `PendingIntent` no launcher `com.google.android.tv`; não pertence ao processo `com.nuviodebug.com` do Telumia. A instalação de Desktop 1.0.6 e seus dados continuaram intactos.

## Referências consultadas

Consultadas em 2026-11-01. Os guias de design são recomendações de plataforma e não declaram uma versão específica de biblioteca.

- [Android TV navigation](https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv): navegação direcional previsível e organização em linhas e categorias.
- [Android TV focus system](https://developer.android.com/design/ui/tv/guides/styles/focus-system): estados de foco distinguíveis por escala, contorno, brilho e cor.
- [Build adaptive apps for TV](https://developer.android.com/develop/adaptive-apps/guides/tv/build-adaptive-apps-for-tv): interface legível a distância, foco visível e navegação D-pad.
- [Detect TV devices](https://developer.android.com/training/tv/get-started/hardware): detecção de Leanback por `PackageManager.hasSystemFeature`.
- [Netflix TV previews](https://about.netflix.com/en/news/new-netflix-tv-experience-includes-video-previews-that-speed-your-next-selection): referência de intenção para dar contexto antes da seleção; a interface não reutiliza identidade ou assets da Netflix.
- [Netflix TV experience update](https://about.netflix.com/en/news/unveiling-our-innovative-new-tv-experience): referências de produto para colocar dados úteis de cada título em evidência, deixar atalhos de busca/lista fáceis de achar e manter recomendações responsivas ao contexto. Usamos esses princípios como referência; arte, marca e composição continuam próprias do Telumia.
- [Netflix TV layout help](https://help.netflix.com/en/node/321880164349028): referência de navegação por destinos no topo e agrupamento de itens salvos, progresso e perfis.
- [Android TV focus system](https://developer.android.com/design/ui/tv/guides/styles/focus-system): estados default/focused/pressed e uso combinado de escala, borda, glow e cor para comunicar foco D-pad.
- [Build adaptive apps for TV](https://developer.android.com/develop/adaptive-apps/guides/tv/build-adaptive-apps-for-tv): recomenda legibilidade a distância, foco evidente, margens de segurança e organização em trilhos e grades. A página foi atualizada em 2026-09-17.
- [Apple focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection/): foco deve confirmar visualmente o alvo; em TV, a expansão sutil comunica profundidade sem mover o cursor.
- [Apple designing for tvOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-tvos): referência de arte em tela cheia e animação discreta para leitura a distância.
- [Android adaptive apps for TV](https://developer.android.com/develop/adaptive-apps/guides/tv/build-adaptive-apps-for-tv): guia atualizado em 2026-09-17; orienta D-pad e adaptação a telas grandes.

No projeto, o lockfile registra Compose Multiplatform 1.12.0 e Kotlin 2.4.10. Os guias Android acima são documentação online oficial; não representam uma snapshot local do Grounded Docs.

## Auditoria visual da instalação 1.0.6 — 01/11/2026

A instalação existente em `C:\Program Files\Telumia` mostra `Versão 1.0.6 (1006)` na tela Sobre. O checkout usado nesta evolução também parte do commit `a9f1b2c1` (`chore(release): prepare Telumia 1.0.6`). As telas foram abertas com a conta e os addons já configurados no perfil local; as capturas temporárias ficam em `composeApp/build/reports/telumia-installed-*.png`.

Pontos observados no aplicativo instalado e mudanças aplicadas:

- Home: o destaque e os catálogos carregam arte real; a data parcial `2026-` aparecia sob o título. O formatador agora remove o separador final de anos parciais.
- Busca: filtros e trilhos carregam os títulos reais e a área está dimensionada para a janela Desktop.
- Biblioteca: o estado vazio ficava preso a um cartão baixo no topo. Agora tem ícone, hierarquia centralizada e usa uma parcela maior do espaço disponível.
- Live TV: os pôsteres com logo eram ampliados como imagem de fundo e ficavam cortados. Logos agora são contidos em um painel; apenas banners ocupam o fundo do card.
- Configurações: o título “Configurações” excedia a largura da barra lateral. O tamanho tipográfico foi reduzido para caber sem truncamento; itens da navegação lateral agora animam hover e mostram contorno de foco para teclado/D-pad.
- Detalhes: o fundo colorido amostrado da arte reduzia o contraste do texto. O tom lateral agora é misturado com o fundo do tema antes de construir o degradê cinematográfico.
- Player: Play/Pause, ações e voltar compartilham botões com feedback de hover, pressão e foco; a timeline evidencia o ponto de seek ao focar ou arrastar. A reprodução e os callbacks existentes permanecem no runtime do player.
- Perfis: cartões de seleção e criação respondem a hover/foco com escala e transição de cor, e o foco remoto também atualiza a prévia do fundo; opções de avatar/fundo deixam o grupo de seleção mais explícito.
- Addons: a tela diferencia carregamento inicial, estado vazio e erros; detalhes não desaparecem durante atualização e os controles recebem hover/foco responsivo.

Verificação em 01/11/2026: `:composeApp:compileKotlinDesktop` passou; 6 suítes Desktop direcionadas passaram (53 testes: seleção de perfil, timeline/player, Home e policy de preview); `ReleaseDateDisplayTest` passou 2 testes; `:androidApp:assembleFullDebug -Pnuvio.android.distribution=full` compilou o APK full. O APK debug foi produzido apenas como validação, não foi instalado por cima da versão 1.0.6 nem publicado.

Esta passagem cobre superfícies do Desktop instalado e acrescenta mudanças em Player, Perfis e Addons. Falta inspeção visual em runtime das telas editadas e validação D-pad em Android TV físico/emulador; a compilação não substitui essa validação.

## Release estável 1.0.7 — Desktop, Android e Android TV

- A mesma Home e os componentes comuns seguem adaptados por plataforma; Android continua usando layout compacto por toque, enquanto a TV usa composição panorâmica e foco D-pad nas áreas compatíveis.
- A tela de entrada em ecrãs grandes agora dimensiona o formulário pela altura disponível e rola o painel quando necessário. Na captura 1080p do AVD, todos os campos e ações aparecem inteiros, incluindo “Continuar sem conta”.
- Versão de app: `1.0.7`, código Android `140`, código Desktop `1007`; a versão interna MSI foi elevada para `2.0.7` para continuar acima do pacote `2.0.6`.
- `:androidApp:assembleFullRelease -Pnuvio.android.distribution=full` e `:composeApp:packageReleaseMsi` passaram. A APK universal e variantes arm64-v8a, armeabi-v7a, x86_64 e x86 foram geradas; o MSI x64 foi gerado.
- 10 suítes Desktop direcionadas passaram com 63 testes e zero falhas/erros; `git diff --check` passou, com apenas o aviso existente de conversão LF/CRLF em `LiveTvScreen.kt`.
- O certificado SHA-256 da APK final coincide com o release 1.0.6 (`82dd58d84ff6dccdbe1ddf71c2d00063a307f7bbc50f80d4164962a2a41d4540`), permitindo atualizar a 1.0.6 sem desinstalação.
- A APK release foi instalada e aberta no AVD Android TV API 33; a tela de entrada foi conferida visualmente e o processo Telumia permaneceu ativo. O AVD não tem conta nem addons, então não valida Home com o catálogo do usuário nem a reprodução de canais.
- `release-manifest.json` e `SHA256SUMS.txt` foram gerados para os seis ativos do release. A distribuição Windows instalada no computador continua em 1.0.6 até que o MSI 1.0.7 seja instalado.
