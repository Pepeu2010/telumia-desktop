# Navegacao Desktop

## Validacao em 2026-11-05

- DesktopNavigationBarTest: 5 testes aprovados para escolha de layout, hover, popup de perfil e rotulos durante rolagem.
- LiveTvNavigationAvailabilityTest: 2 testes aprovados para disponibilidade do destino TV.
- TelumiaSidebarMotionTest: 2 testes de composicao aprovados. Exercitam clique, teclado, selecao e troca entre todas as abas nos tres modos de movimento.
- O host preservou uma unica montagem por aba e removeu a semantica das abas inativas.
- Captura full.png em composeApp/build/reports/telumia-sidebar inspecionada: rotulos, icones e selecao alinhados, destino TV visivel.

## Limites da evidencia

- O teste do host usa conteudo controlado; nao comprova o retorno de um filme real ao cartaz anterior.
- As capturas da lateral nao comprovam ausencia de sobreposicao com todo conteudo de todas as telas.
- Revisao integrada de Home, Busca, Biblioteca, TV, Configuracoes e destinos secundarios permanece aberta.
