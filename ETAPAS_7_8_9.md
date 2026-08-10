# Basket Manager — Etapas 7, 8 e 9

## Etapa 7 — Repositories e persistência modular

A camada de acesso a dados foi dividida em repositories específicos:

- `TeamRepository`
- `PlayerRepository`
- `SeasonRepository`
- `GameRepository`
- `FinanceRepository`
- `ContractRepository`

Também foi criada a entidade `ContractEntity` e seu DAO. O Room foi atualizado para a versão 3 com migração 2→3.

O `GameStateRepository` continua existindo para compatibilidade dos módulos legados/transitórios, mas o núcleo já possui pontos de acesso independentes.

## Etapa 8 — Regras de manager

Foram centralizadas regras para:

- contratos e valor de mercado;
- salário anual recomendado;
- tamanho mínimo/normal/máximo de elenco;
- elegibilidade para troca;
- faixa de overall para negociação;
- idade e validade de free agents;
- liberação automática do jogador mais fraco;
- Draft e prevenção de IDs duplicados;
- mando de quadra dos playoffs;
- idade máxima de permanência no elenco.

Novos módulos:

- `ContractManager`
- `ContractRules`
- `TradeRules`
- `FreeAgencyRules`
- `SeasonRules`

## Etapa 9 — Motor de simulação e balanceamento

Foi criado `SimulationRules`, removendo do `GameSimulator` a maior parte das fórmulas de força ofensiva/defensiva e dificuldade.

O motor agora considera de forma centralizada:

- qualidade ofensiva do elenco;
- qualidade defensiva;
- banco;
- ritmo (`pace`);
- estilo tático;
- pressão defensiva;
- rebote ofensivo;
- bônus do treinador;
- vantagem de mando;
- dificuldade;
- variação aleatória controlada por faixa.

Também foi corrigida a geração de linha estatística individual para que um jogador com 1 ponto não fique com pontuação sem representação no box score: o ponto passa a ser registrado como lance livre.

Foi adicionada `SimulationValidator` e testes de unidade para o motor.

## Validação

Foi executada compilação Kotlin isolada dos módulos puros novos (`SimulationRules`, `MatchSimulationEngine`, regras de domínio e `ContractManager`). A compilação foi concluída com apenas um warning sobre o parâmetro `isHome` do engine.

O build Android completo não foi executado porque o projeto não possui Gradle Wrapper e o ambiente de auditoria não disponibiliza o Gradle executável.
