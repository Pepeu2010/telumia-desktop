# TV ao vivo Desktop

## Estado da implementacao

- A interface atual apresenta canais dos addons instalados, com paginação progressiva.
- Base do EPG portada da TV para o mesmo modulo features/livetv: modelos comuns, parsers M3U/XMLTV no Desktop e segmentos temporais comuns.
- Os parsers preservam limites de entrada/saida, bloqueio de entidades XML externas, horarios em UTC e preferencia por titulos e descricoes em portugues.
- Testes de M3U, XMLTV e recorte de timeline portados junto com o codigo.
- Validacao em 2026-11-05: compilacao Desktop e suites XmlTvParserTest, M3uPlaylistParserTest e GuideTimelineTest aprovadas.
- Corrigida preferencia de descricao pt-BR quando uma traducao inglesa vem depois no XMLTV; mesmo caso de regressao validado na TV.
- Armazenamento local de fontes implementado no Desktop: isolamento por perfil, URL M3U e XMLTV, ativacao/remocao, limites de configuracao e preservacao de dados ilegíveis durante edicao. Ainda sem formulario conectado na interface.
- LiveTvSourceStoreTest (3) e GuideTimelineTest (2) aprovados em 2026-11-05: armazenamento em arquivo temporario reaberto, dois perfis, limite de fontes e preservacao de payload corrompido.

## Integracao ainda necessaria

- Formulario de configuracao conectado ao armazenamento por perfil.
- Importacao/atualizacao de fontes e cache do guia.
- Associacao explicita de tvg-id entre canais e programas.
- Grade EPG na interface, agora/depois, favoritos, recentes e lembretes.
- Encaminhamento de canais importados ao player e validacao de fluxo completo.

Esta base ainda nao e um guia funcional exposto ao usuario. Nao anunciar EPG Desktop concluido antes destas integracoes.
