# Movimento da tela de abertura

A tela é compartilhada entre o carregamento inicial e o overlay que acompanha a preparação real da sessão e as trocas de perfil. Ela não acrescenta espera artificial nem apresenta porcentagens de progresso que o aplicativo não mede.

## Sequência visual

- O símbolo surge primeiro, inclinado e menor; ele cresce com um overshoot curto e assenta sem rotação.
- O nome TELUMIA entra logo depois, revelado da esquerda para a direita por uma máscara.
- Um feixe curvo cruza o fundo escuro, anéis de luz se expandem ao redor do centro e partículas discretas aparecem durante a entrada. As luzes usam as cores do tema Telumia.
- A entrada cinematográfica dura cerca de 2.200 ms na intensidade padrão, ajustada pela preferência de movimento. É finita e não reinicia em loop.
- O brilho estreito abaixo da marca indica que a sessão ainda está carregando, sem inventar uma porcentagem. Ele só se move enquanto a tela está ativa, e o overlay some quando o estado real do aplicativo fica pronto.
- A faixa indeterminada só se move enquanto a tela está ativa. A entrada de marca toca uma vez, não continua em segundo plano; movimento reduzido encurta a entrada e movimento desligado a remove.
- A camada escura preserva o fundo de perfil por baixo e mantém o contraste do logotipo.
- Os efeitos de fundo são desenhados em um único Canvas do Compose, sem vídeo, WebGL ou novas dependências. O teste `AppOpeningMotionTest` verifica o percurso do feixe, o estágio do símbolo/wordmark, o overshoot e o assentamento final.

## Referências

A identidade da Netflix usa um fundo escuro e um breve rompimento de cor para criar expectativa antes do conteúdo. A Telumia usa somente esse princípio de ritmo e atmosfera, com símbolo, paleta e movimento próprios, sem copiar o logotipo nem a assinatura sonora. Referência: [Netflix — Your new Netflix ident animation](https://about.netflix.com/en/news/your-new-netflix-ident-animation-cue-netflix-sound).

O motion design da Sony descreve uma marca que atravessa uma fronteira visual, responde e então assenta no centro. Inspirou a sequência de aproximação, impacto e repouso do símbolo Telumia. Referência: [Sony Design — Sony’s Motion Logo](https://www.sony.com/en/SonyInfo/design/stories/motionlogo/).

O Compose mantém a entrada finita separada do indicador indeterminado e suporta a preferência de movimento já adotada pelo aplicativo. Referência: [Android Developers — Animation in Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide).

## Validação local

- `:composeApp:desktopTest --tests com.nuvio.app.AppOpeningMotionTest`: 4 testes aprovados, incluindo ordem de entrada, overshoot e estado final.
- `:composeApp:desktopJar` e `:composeApp:packageMsi`: aprovados no Windows após a mudança.
- MSI local de validação: `Telumia-Windows-x64-1.0.2-alpha.1.msi`, 210.993.256 bytes, SHA-256 `31335FCB11D85FBB385762AAAEDDF8D7DAD74D0AB7BBA9F411C861F35918091E`. Não foi publicado como release.
- A suíte desktop completa da revisão inicial terminou com 1.641 testes, 38 falhas e 4 ignorados. Ela ocorreu antes desta atualização e não foi repetida neste ajuste.
- Não foi feita validação visual da janela em execução nem build Android; a captura de referência desta tarefa é do Desktop.
