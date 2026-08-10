# Basket Manager — Etapa 1: Correções críticas

Data: 09/08/2026

## Correções aplicadas

1. IDs de rookies agora são derivados do maior ID existente na temporada, evitando duplicação entre equipes.
2. IDs de jogadores gerados no Draft agora são únicos em relação ao elenco e free agents atuais.
3. IDs de free agents agora são únicos em relação ao elenco e Draft atual.
4. Playoffs rápidos agora respeitam mando de quadra 1-2-5-7 para o cabeça de chave.
5. `PlayoffResult.seriesResults` agora preserva quartas, semifinais, finais de conferência e finais da NBA.
6. Identificação da série final foi corrigida para aceitar o nome efetivamente produzido pelo simulador (`Finais da NBA`).
7. Mercado de staff agora é salvo e restaurado, em vez de ser regenerado a cada inicialização.
8. Lista de rookies do Draft agora é salva e restaurada.
9. Draft é limpo após a seleção do jogador.
10. Autosave recebeu `Mutex` para impedir gravações concorrentes fora de ordem.
11. Snapshots de jogadores em elenco, quinteto e free agents passaram a ser cópias reais (`copy()`), reduzindo risco de mutação durante serialização.
12. Snapshot de temporada copia jogadores das equipes e registros da classificação.
13. `MediaPlayer` do simulador agora possui ciclo explícito de liberação ao terminar uma série.
14. Permissão `POST_NOTIFICATIONS` foi declarada no Manifest e solicitada em Android 13+.
15. Teste Robolectric foi corrigido para o nome real do aplicativo (`Basket Manager`).
16. Teste de screenshot/template quebrado que chamava `Greeting()` inexistente foi substituído por um teste funcional do nome do app.
17. Anotações falsas `@Immutable/@Stable` foram removidas dos modelos mutáveis, incluindo `Player`, `Finance`, `Tactics`, `Coach`, `Staff`, `Season` e outros modelos que continham estado mutável.

## Limitação da validação

O projeto original não contém Gradle Wrapper e o ambiente desta auditoria não possui o executável Gradle instalado. Por isso, esta etapa foi validada por inspeção estática e verificações estruturais, mas não por uma compilação Android completa.

Antes de gerar APK/release, executar:

```text
gradle test
gradle assembleDebug
gradle lint
```

ou, preferencialmente, gerar o Gradle Wrapper no ambiente de desenvolvimento e executar `./gradlew test assembleDebug lint`.
