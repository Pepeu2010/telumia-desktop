# Telumia Desktop

![Telumia](https://raw.githubusercontent.com/Pepeu2010/telumia/main/assets/brand/telumia-banner.png)

**Seu cinema, suas séries e novelas — com foco no Brasil.** Cliente nativo Telumia para PC, desenvolvido sobre a infraestrutura do Nuvio Desktop com Kotlin Multiplatform, Compose e libmpv. Possui marca e identidade próprias, preservando o ecossistema de contas e addons compatíveis.

## Versão 1.0.0

A versão atual do código é **1.0.0**, sem sufixo alpha; os testes e a preparação dos novos pacotes estão em andamento. [Downloads e registro da publicação](https://github.com/Pepeu2010/telumia/releases). O instalador Windows x64 usa a numeração interna MSI 2.0.0 para permitir atualização sobre as alphas, mantendo a identidade do aplicativo. Linux/macOS permanecem no código multiplataforma, sem novos instaladores ou execução comprovada nesta entrega.

As melhorias incluem Home e detalhes cinematográficos, seleção de perfis, menu lateral com motion configurável, cache Auto/Manual, momentos salvos, miniaturas e filmstrip reais no player Windows e capítulos nomeados do arquivo. Coleções e addons têm mesclagem durável nos contratos de conta existentes. [Alcance da versão e pendências](https://github.com/Pepeu2010/telumia/blob/main/docs/VERSION_100.md).

Buscar **Iludida** também consulta seus nomes alternativos. A edição brasileira com 78 capítulos exige uma fonte instalada que a ofereça: seus IDs são preservados. Uma fonte com 31 episódios conserva essa edição, sem capítulos inventados. Português do Brasil é o idioma inicial dos metadados de perfis novos.

[Downloads e status](https://github.com/Pepeu2010/telumia/releases) · [Especificação e limites](https://github.com/Pepeu2010/telumia/blob/main/docs/TELUMIA.md) · [Roadmap](https://github.com/Pepeu2010/telumia/blob/main/docs/ROADMAP.md).

## Desenvolvimento

O cliente em desenvolvimento inclui avatar pessoal com arquivo local, clipboard, drag-and-drop, recorte, zoom e biblioteca integrada de 64 ilustrações licenciadas. As fotos ficam no aparelho, separadas do cache e dos payloads da conta. O editor permanece aberto quando o salvamento remoto do perfil falha; perfis locais recriados recebem uma nova identidade para evitar herdar fotos de outro perfil.

O Profile Studio oferece imagens pessoais, biblioteca licenciada, seleção adaptativa e foco por teclado, preservando as rotas existentes de troca, edição e PIN. [Evidências e pendências](https://github.com/Pepeu2010/telumia/blob/main/docs/PROFILE_STUDIO.md).

```sh
./gradlew :composeApp:run
```

Use as instruções de configuração local do [projeto central](https://github.com/Pepeu2010/telumia). Não publique arquivos de credenciais ou keystores. A meta integral continua em execução: faltam Scene Info completo, Source Intelligence, Live TV/EPG, Phone Remote, Smart Collections, downloads avançados e o redesign de todas as superfícies. Sincronização com conta oficial, WebView2 completo e desempenho/HDR em hardware físico exigem validação própria. Os recursos exclusivos locais não são anunciados como sincronizados quando o backend não oferece esse contrato.

## Licença

[GPL-3.0](LICENSE). Copyrights, autoria e avisos de terceiros preservados. [Créditos do código](FORK_NOTICE.md).
