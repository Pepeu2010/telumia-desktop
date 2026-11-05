# Entrega integrada de visual e utilidades

Objetivo: concluir as recomendacoes aprovadas no Desktop e TV/APK e publicar os instaladores estaveis no repositorio principal. Alteracoes em branches nao equivalem a release instalado.

## Gates ainda abertos

- Revisao renderizada de Home, detalhes, busca, biblioteca, perfis, configuracoes e telas secundarias nas duas plataformas.
- Navegacao completa, foco/restauracao, retorno e transicoes entre destinos.
- Fluidez da abertura e passagem real ate a Home.
- Player com miniaturas, capitulos, Scene Info, audio/legendas e proximo episodio em fontes reais.
- Busca universal, incluindo ator, emissora, produtora, canal e addon.
- EPG no Desktop equivalente a TV, favoritos, recentes e lembretes.
- Acoes de Continuar assistindo e preferencias de reproducao por perfil.
- Diagnostico compreensivel e recuperacao de falhas de fontes.
- Atualizacao somente estavel com download, progresso e instalacao.
- Sincronizacao integral de perfis, biblioteca, progresso, addons e preferencias.
- Modo economico automatico e privacidade de historico por perfil.
- Geracao assinada de APKs e MSI, verificacao dos artefatos e publicacao no repositorio principal.

## Evidencia atual

- Home, detalhes, busca, biblioteca, player e perfis receberam melhorias visuais nas branches de desenvolvimento.
- Testes focados de perfis, biblioteca e abertura passaram em marcos anteriores; isso nao prova os gates integrados acima.
- TV possui pipeline M3U/XMLTV e guia; Desktop ainda apresenta catalogos de canais dos addons.
- Desktop limitava o parsing da pagina recebida a 60 canais. Limite removido para preservar todos os canais retornados; paginacao de addons permanece uma necessidade separada.

Atualizar este documento com evidencia concreta a cada gate concluido. Nenhum gate acima esta declarado concluido por existencia de arquivos ou testes fora do seu escopo.
