# ETAPA 5 — Persistência Room/SQLite

## Objetivo
Substituir a persistência operacional baseada em `SharedPreferences + Gson` por Room/SQLite, mantendo compatibilidade com saves antigos.

## Implementado
- `BasketDatabase` como banco Room.
- `GameStateEntity` como snapshot persistente versionado.
- `GameStateDao` com leitura, upsert e limpeza.
- `GameStateRepository` como única fronteira de persistência.
- `AutoSaveManager` convertido em facade compatível com Room.
- `GameViewModel` carrega e salva através do repository.
- `GameSimulator` deixou de ler `SharedPreferences`; recebe `SimulationConfig`.
- `Season` aceita a configuração de simulação nos playoffs.
- Import/export nativo agora usa o snapshot Room.
- Migração automática do save legado: na primeira leitura, os dados de `BasketPrefs` são transferidos para Room e, após o upsert, o legado é limpo.

## Arquitetura atual

Compose → ViewModel → Domain → Repository → Room → SQLite

## Observação arquitetural
Esta etapa usa inicialmente uma tabela de snapshot para preservar os modelos atuais sem uma migração destrutiva. A próxima evolução recomendada é normalizar o banco em entidades como `Player`, `Team`, `Season`, `Game`, `PlayerGameStat`, `Contract`, `FinanceTransaction`, `Staff` e `Award`, permitindo consultas históricas sem desserializar um JSON gigante.

## Build
O projeto agora requer KSP + Room Compiler. A compilação Android deve ser feita com Gradle/Android Studio para validar geração dos Room implementations.
