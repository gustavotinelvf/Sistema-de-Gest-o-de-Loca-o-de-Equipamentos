# Configuração de permissões do PostgreSQL

O arquivo `permissoes-equipe.sql` contém as instruções
para configuração da role `ziploc_equipe` e suas permissões
de acesso ao banco de dados.

Sua execução manual deve ser realizada por um administrador
do banco de dados com as permissões necessárias.

## Compatibilidade com o Flyway

A migration `V3__permissoes_equipe.sql` permanece
inalterada para preservar a integridade do histórico
de migrations dos ambientes existentes.

A retirada definitiva do V3 do fluxo de migrations
deverá ser coordenada entre os integrantes da equipe.

Enquanto isso, o V3 continuará sendo executado
automaticamente pelo Flyway em novos bancos.
