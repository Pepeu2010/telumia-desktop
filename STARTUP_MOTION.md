# Movimento da tela de abertura

A tela é compartilhada entre o carregamento inicial e o overlay que acompanha a preparação real da sessão e as trocas de perfil. Ela não acrescenta espera artificial nem apresenta porcentagens de progresso que o aplicativo não mede.

## Sequência visual

- O wordmark original da Telumia entra com opacidade, escala e deslocamento vertical em 820 ms.
- Um halo e uma fita de luz atravessam uma vez o fundo escuro, usando o acento do tema selecionado. Só a faixa de carregamento real permanece em movimento enquanto o aplicativo ainda está ocupado.
- Uma faixa curta e indeterminada comunica que o carregamento continua. O overlay some assim que o estado real do aplicativo fica pronto.
- O movimento ambiente e a faixa só são animados enquanto a tela está ativa. As opções de movimento reduzido e desligado do aplicativo também controlam esta abertura.
- A camada escura preserva o fundo de perfil por baixo e mantém o contraste do logotipo.

## Referências

A animação de identidade da Netflix usa fundo escuro e uma passagem breve de cores para preparar uma experiência imersiva; a Telumia adapta apenas esse princípio de ritmo e atmosfera à sua própria paleta, logo e comportamento, sem reutilizar sua marca ou assinatura sonora. Referência: [Netflix — Your new Netflix ident animation](https://about.netflix.com/en/news/your-new-netflix-ident-animation-cue-netflix-sound).

A sequência usa APIs de animação do Compose: `animate*AsState` conduz a entrada finita; o helper existente baseado em `rememberInfiniteTransition` é reservado à faixa indeterminada enquanto o aplicativo está ativo. Referência: [Android Developers — Animation in Compose](https://developer.android.com/develop/ui/compose/animation/quick-guide).
