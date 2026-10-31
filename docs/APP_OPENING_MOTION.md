# Abertura cinematográfica do Telumia

## Direção visual

A abertura parte de uma tela escura e apresenta a marca em uma sequência curta: o símbolo chega primeiro com escala, rotação e acomodação; o nome é revelado em seguida; uma faixa de luz cruza a cena uma única vez. O fundo usa gradientes calculados em cache e permanece estático durante a inicialização real. Isso remove os loops de luz, partículas e caminhos animados que redesenhavam a tela continuamente no PC. O indicador continua representando o carregamento indeterminado, sem inventar uma porcentagem nem introduzir atraso artificial.

A cor vem da paleta selecionada no aplicativo, preservando a identidade própria do Telumia. O movimento reduz para uma composição estática quando o modo de movimento reduzido ou desligado está ativo. Os efeitos restantes usam uma sequência finita de transformações e uma faixa simples em Canvas, sem vídeo, partículas ou nova dependência.

## Avatar por URL

Na edição de perfil, a URL HTTPS informada pelo usuário tem precedência sobre um avatar antigo criado no Profile Studio. A mesma ordem é usada na seleção e nas demais superfícies que resolvem a imagem do perfil, evitando que uma imagem local anterior esconda a URL salva. A URL de exemplo em WebP continua passando pela validação existente.

## Referências consultadas

- A [apresentação oficial do ident da Netflix](https://about.netflix.com/en/news/your-new-netflix-ident-animation-cue-netflix-sound) descreve o fundo escuro como convite para uma experiência imersiva e usa a identidade para antecipar a variedade de histórias. A implementação do Telumia adota esse princípio de atmosfera e ritmo, sem reproduzir o desenho ou a paleta da Netflix.
- O estudo de interação [Hotstar Splash, no 60fps](https://60fps.design/shots/hotstar-splash) registra uma luz colorida que atravessa a tela e revela a marca. No Telumia, esse gesto foi adaptado para os tons configuráveis da própria marca, integrado ao estado real de inicialização.

## Verificação

`AppOpeningMotionTest` e `ProfileAvatarUrlTest` passaram com `:composeApp:desktopTest --tests com.nuvio.app.AppOpeningMotionTest --tests com.nuvio.app.features.profiles.ProfileAvatarUrlTest`.

O MSI Windows da base 1.0.4 foi empacotado em `composeApp/build/compose/release-msis/Telumia-Windows-x64-1.0.4.msi`. A execução completa de `:composeApp:desktopTest` encontrou 39 falhas em 1.690 testes, distribuídas por suites fora dos dois fluxos alterados; por isso, este artefato não foi publicado como novo release estável. QA em aparelho físico não foi executado.
