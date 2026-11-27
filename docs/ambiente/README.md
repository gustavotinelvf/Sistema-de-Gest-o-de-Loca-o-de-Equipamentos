# Configuração de permissões do PostgreSQL

O arquivo `permissoes-equipe.sql` contém as instruções
para configuração da role `ziploc_equipe` e suas permissões
de acesso ao banco de dados.

O script identifica automaticamente o banco conectado
por meio da função `current_database()`.

Sua execução manual deve ser realizada por um administrador
do banco de dados com as permissões necessárias.

## Compatibilidade com o Flyway

A migration `V3__permissoes_equipe.sql` foi corrigida para
eliminar a dependência de um nome fixo de banco de dados.

### Bancos novos

Nenhum procedimento adicional é necessário.
O Flyway aplicará normalmente a migration corrigida.

### Bancos existentes

Ambientes que executaram a versão original do V3
podem apresentar divergência de checksum.

Checksums da migration V3:

- Original: 233365448
- Corrigido: -1848433977

Antes de executar o repair:

1. Realizar um backup do banco de dados.
2. Confirmar que a única divergência no histórico é a do V3.
3. Verificar se a role ziploc_equipe possui a permissão
   CONNECT concedida diretamente no banco atual.
4. Configurar as variáveis FLYWAY_URL, FLYWAY_USER,
   FLYWAY_PASSWORD e FLYWAY_LOCATIONS para o banco correto.

Na pasta backend, executar:

```powershell
.\mvnw.cmd "org.flywaydb:flyway-maven-plugin:12.4.0:repair"
```

O repair atualiza o histórico do Flyway, mas não
executa novamente o SQL da migration.

Caso existam outras divergências ou permissões incorretas,
interromper o procedimento e comunicar a equipe.

Após o repair, executar a suíte completa de testes
e remover as variáveis temporárias do Flyway.

## Validação

A correção foi validada com PostgreSQL 16 e Flyway 12.4.0
em dois ambientes:

- Banco novo denominado ziploc_db.
- Banco existente denominado ziploc, após repair controlado.

A suíte completa de testes passou nos dois ambientes.
