# Perfis

## Direcao visual

- O fundo acompanha o perfil em destaque e recebe uma camada escura para manter a leitura.
- Titulo, subtitulo, cartoes, bordas e acao de gerenciamento usam tokens semanticos do tema ativo.
- O avatar e a cor escolhida pelo usuario permanecem como identidade do perfil, sem contaminar o contraste do restante da tela.

## Interacao

- Hover, teclado e controle compartilham o mesmo estado de destaque.
- O foco altera superficie e borda sem reposicionar outros cartoes.
- Movimento reduzido preserva fades e feedback de estado, removendo deslocamentos decorativos.

## Estados

- Carregamento, vazio, PIN, editar e adicionar perfil devem manter a mesma hierarquia visual.
- Imagens que falharem continuam exibindo a inicial e a cor do perfil.
