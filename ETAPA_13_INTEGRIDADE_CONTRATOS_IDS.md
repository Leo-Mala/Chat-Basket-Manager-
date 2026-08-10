# Etapa 13 — Integridade de Dados, IDs e Contratos

## Objetivo

Eliminar a reutilização de IDs de jogadores, tornar contratos parte do estado durável da carreira e validar as migrações Room reais.

## Alterações

- `Season.nextPlayerId` é um contador monotônico persistido.
- Draft, free agency e rookies usam `Season.allocatePlayerIds()` em vez de calcular `max(activeRoster)+1`.
- Room evoluiu da versão 4 para 5 com `MIGRATION_4_5`.
- Contratos ganharam modelo de domínio `PlayerContract`.
- `ContractManager` agora cobre criação, avaliação, transferência e expiração.
- Save/load passa os contratos pelo `GameStateSnapshot` e Room continua sendo a fonte durável.
- Contratos órfãos são removidos; apenas jogadores atualmente vinculados a equipes possuem contrato.
- Termos negociados (salário, anos, player option e no-trade) são preservados.
- `noTrade` é respeitado pelo fluxo de trade.
- Contratos são reduzidos em uma temporada e jogadores expirados passam ao mercado de free agents.
- Jogadores draftados recebem contrato ao entrarem no novo elenco.
- Migrações 2→3, 3→4 e 4→5 reais passaram a ser usadas pelo teste instrumentado.
- Adicionados testes de ciclo de vida de contrato, allocator de IDs e persistência Room.

## Limite conhecido

O build Android completo ainda precisa ser executado no GitHub Actions/Android Studio. Esta etapa não declara `assembleRelease` como executado localmente.
