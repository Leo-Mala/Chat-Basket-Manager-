# Etapa 6 — Normalização completa da persistência Room

## Objetivo
Substituir o snapshot JSON como armazenamento principal por tabelas relacionais para o núcleo da carreira.

## Tabelas normalizadas
- `teams`
- `players`
- `coaches`
- `finances`
- `sponsors`
- `finance_expenses`
- `tactics`
- `seasons`
- `standings`
- `games`
- `player_game_stats`
- `game_injuries`
- `awards`
- `season_history`
- `season_history_team_wins`
- `season_history_players`

## O que deixou de ser armazenado como JSON no núcleo
- equipes e elencos;
- jogadores e pool de free agents/draft;
- treinador;
- finanças e despesas/patrocínios;
- táticas;
- temporada e classificação;
- partidas;
- estatísticas por jogador/partida;
- lesões associadas às partidas;
- premiações;
- histórico de temporadas e seus jogadores.

## JSON mantido deliberadamente
O snapshot `game_state` continua existindo apenas como compatibilidade para módulos secundários e transitórios, como:
- comissão técnica polimórfica;
- facilities;
- notícias;
- notificações;
- mercado de staff;
- finance advanced;
- último box score.

Isso evita uma migração destrutiva e prepara a Etapa 7 para normalizar esses módulos restantes.

## Migração
- Room sobe de versão 1 para 2.
- `MIGRATION_1_2` cria as novas tabelas.
- Saves antigos em `game_state` ou `SharedPreferences` são hidratados e convertidos para as tabelas relacionais na primeira leitura.
- Novos saves gravam o núcleo somente nas tabelas normalizadas.
- A gravação é transacional.

## Observação de performance
A próxima melhoria deve substituir o `upsertAll` amplo por operações incrementais por entidade e paginação/limpeza de históricos muito antigos quando necessário.
