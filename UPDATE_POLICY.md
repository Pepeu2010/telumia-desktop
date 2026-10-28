# Atualizações estáveis

O Telumia Desktop consulta `GET /repos/{owner}/{repo}/releases/latest` ao iniciar e também oferece uma verificação manual nas configurações. Ele não monitora o GitHub em segundo plano com o app fechado.

Somente releases publicadas e estáveis são elegíveis. Além do filtro do GitHub, o app rejeita releases marcadas como draft ou prerelease e versões ou títulos que indiquem alpha, beta, RC ou preview. Preferências antigas de canal beta ou geral são migradas para estável; não existe seletor de pré-lançamentos na interface.

Ao selecionar **Atualizar**, o app abre uma janela modal com o progresso do download do instalador compatível com o sistema. Quando o arquivo termina, o app inicia o instalador. O sistema operacional pode pedir confirmação para instalar ou reiniciar o app.

Para uma atualização aparecer, publique uma release estável com tag sem sufixo de pré-lançamento, por exemplo `v1.0.3`, desative **Set as a pre-release** e inclua o instalador correspondente ao sistema operacional. Uma release alpha não será oferecida, mesmo que apareça no topo da página geral de releases.
