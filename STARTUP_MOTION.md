# Movimento da tela de abertura

A tela é compartilhada entre o carregamento inicial e o overlay que acompanha a preparação real da sessão e as trocas de perfil. Ela não acrescenta espera artificial nem apresenta porcentagens de progresso que o aplicativo não mede.

## Sequência visual

- O wordmark original da Telumia entra com opacidade, escala e deslocamento vertical em 820 ms.
- Uma fita de luz curva atravessa a tela da esquerda para a direita em 1.360 ms. Ela cruza o centro junto com a entrada do logo e desaparece nas bordas; o halo usa as cores do tema selecionado.
- A fita de tela inteira é uma animação de entrada, não um loop. Só o pequeno brilho da faixa de carregamento real continua em movimento enquanto o aplicativo ainda está ocupado.
- Uma faixa curta e indeterminada comunica que o carregamento continua. O overlay some assim que o estado real do aplicativo fica pronto.
- A faixa indeterminada só se move enquanto a tela está ativa. A entrada de marca toca uma vez, não continua em segundo plano; movimento reduzido encurta a entrada e movimento desligado a remove.
- A camada escura preserva o fundo de perfil por baixo e mantém o contraste do logotipo.
- Os efeitos de abertura são desenhados com Canvas do Compose, sem vídeo, WebGL ou novas dependências. O teste `AppOpeningMotionTest` verifica o percurso da fita e o fade nas bordas.

## Referências

A animação de identidade da Netflix usa fundo escuro e uma passagem breve de cores para preparar uma experiência imersiva; a Telumia adapta apenas esse princípio de ritmo e atmosfera à sua própria paleta, logo e comportamento, sem reutilizar sua marca ou assinatura sonora. Referência: [Netflix — Your new Netflix ident animation](https://about.netflix.com/en/news/your-new-netflix-ident-animation-cue-netflix-sound).

O estudo de identidade global da Netflix descreve uma marca visual em movimento e um sistema dinâmico; aqui a animação também é breve e reconhecível, mas fica restrita ao wordmark e às cores próprias da Telumia. Referência: [Study Hall — Netflix Global 10 Year Identity](https://www.studyhall.design/work/netflix-global-10-year-anniversary-identity).

A sequência usa APIs de animação do Compose: `animate*AsState` conduz a entrada finita; o helper existente baseado em `rememberInfiniteTransition` é reservado à faixa indeterminada enquanto o aplicativo está ativo. Referência: [Android Developers — Animation in Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide).

## Validação local

- `:composeApp:desktopTest --tests com.nuvio.app.AppOpeningMotionTest`: 2 testes aprovados.
- `:composeApp:desktopJar` e `:composeApp:packageMsi`: aprovados no Windows.
- MSI de validação local: `Telumia-Windows-x64-1.0.2-alpha.1.msi`, 210.980.968 bytes, SHA-256 `62A7FE23227D8BF9553A7B4D788177D0697BD5701B6D696044E1F69092716626`. Este artefato não foi publicado.
- A suíte desktop completa terminou com 1.641 testes, 38 falhas e 4 ignorados; nenhuma falha veio de `AppOpeningMotionTest`. A rodada não incluiu comparação completa contra o pai deste branch.
- A compilação Android deste fork desktop e a inspeção visual em aparelho/TV não foram executadas nesta entrega.
