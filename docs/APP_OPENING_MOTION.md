# Abertura cinematográfica do Telumia

## Direção visual

A abertura parte de uma tela escura e apresenta a marca em uma sequência curta: o símbolo chega primeiro com escala, rotação e acomodação; o nome é revelado em seguida; uma faixa de luz cruza a cena. Durante a inicialização real, uma luz ambiente percorre lentamente o fundo e um reflexo sutil atravessa a marca. O indicador continua representando o carregamento indeterminado, sem inventar uma porcentagem nem introduzir atraso artificial.

A cor vem da paleta selecionada no aplicativo, preservando a identidade própria do Telumia. O movimento reduz para uma composição estática quando o modo de movimento reduzido ou desligado está ativo. O loop ambiente é suspenso quando a tela deixa de estar ativa. Os efeitos são desenhados em Canvas e usam transformações, gradientes e opacidade, sem vídeo, partículas pesadas ou nova dependência.

## Referências consultadas

- A [apresentação oficial do ident da Netflix](https://about.netflix.com/en/news/your-new-netflix-ident-animation-cue-netflix-sound) descreve o fundo escuro como convite para uma experiência imersiva e usa a identidade para antecipar a variedade de histórias. A implementação do Telumia adota esse princípio de atmosfera e ritmo, sem reproduzir o desenho ou a paleta da Netflix.
- O estudo de interação [Hotstar Splash, no 60fps](https://60fps.design/shots/hotstar-splash) registra uma luz colorida que atravessa a tela e revela a marca. No Telumia, esse gesto foi adaptado para os tons configuráveis da própria marca, integrado ao estado real de inicialização.

## Verificação

`AppOpeningMotionTest` passou com 5 testes, zero falhas e zero testes ignorados, executado por `:composeApp:desktopTest --tests com.nuvio.app.AppOpeningMotionTest`. O mesmo comando compilou o módulo Desktop.

O MSI Windows foi empacotado com sucesso em `composeApp/build/compose/release-msis/Telumia-Windows-x64-1.0.2-alpha.1.msi` (SHA-256 `25889c145b31784cea3a99a2456dc2ba64dc8b7427c59b39d57a5943792279a6`). O MSI foi gerado localmente para verificação; não foi publicado em um release estável. QA em aparelho físico não foi executado.