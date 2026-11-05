# TV ao vivo Desktop

## Estado da implementacao

- A interface atual apresenta canais dos addons instalados, com paginação progressiva.
- Base do EPG portada da TV para o mesmo modulo features/livetv: modelos comuns, parsers M3U/XMLTV no Desktop e segmentos temporais comuns.
- Os parsers preservam limites de entrada/saida, bloqueio de entidades XML externas, horarios em UTC e preferencia por titulos e descricoes em portugues.
- Testes de M3U, XMLTV e recorte de timeline portados junto com o codigo.
- Validacao em 2026-11-05: compilacao Desktop e suites XmlTvParserTest, M3uPlaylistParserTest e GuideTimelineTest aprovadas.
- Corrigida preferencia de descricao pt-BR quando uma traducao inglesa vem depois no XMLTV; mesmo caso de regressao validado na TV.
- O cadastro de fontes usa armazenamento local por perfil. Aceita M3U com XMLTV opcional, ativação e remoção. Edições preservam configurações ilegíveis.
- LiveTvSourceStoreTest (3) e GuideTimelineTest (2) aprovados em 2026-11-05: armazenamento em arquivo temporario reaberto, dois perfis, limite de fontes e preservacao de payload corrompido.

## Uso

Na aba TV, abra **Fontes e programação**, adicione o endereço M3U e, se disponível, o XMLTV. Selecione uma fonte ativa para carregar os canais. A programação usa o `tvg-id` da lista e o identificador do canal no XMLTV da mesma fonte. A ação **Assistir** encaminha a URL ao player configurado.

**Atualizar** recarrega a fonte selecionada. O guia é mantido em memória enquanto a janela está aberta. Se o XMLTV falhar, a lista M3U continua disponível.

Compilação Desktop e 10 testes aprovados em 2026-11-05. Os testes HTTP usam servidor local e cobrem associação por fonte/canal e preservação da lista quando o guia falha.

## Integração ainda necessária

- Grade EPG com horários, cache do guia, favoritos, recentes e lembretes.
- Validação de reprodução e retorno ao catálogo com uma fonte real.
