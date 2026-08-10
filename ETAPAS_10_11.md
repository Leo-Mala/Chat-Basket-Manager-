# ETAPAS 10 e 11 — Testes, regressão e performance

## Etapa 10 — Testes

- Criado `SimulationValidator` para validar invariantes do box score.
- Adicionados testes para regras de contratos.
- Adicionados testes para regras de temporada/playoffs.
- Adicionados testes para free agency.
- Adicionado teste de validação do motor de simulação.
- Adicionado teste de stress com 1.000 gerações de box score.
- Mantidos os testes Robolectric de identidade do aplicativo.

### Invariantes cobertas

- 240 minutos por equipe.
- Pontos individuais fecham com o placar da equipe.
- FGM >= 3PM.
- FGA >= FGM.
- 3PA >= 3PM.
- FTA >= FTM.
- Nenhuma estatística negativa.
- Identidade matemática da pontuação.
- Mando de quadra NBA 1-2-5-7.
- Limites de elenco.
- Validade de candidatos de free agency.
- Duração recomendada de contratos.

## Etapa 11 — Performance/lifecycle

- `GameViewModel` foi extraído para arquivo próprio e convertido para `AndroidViewModel`, usando `Application` em vez de reter `Activity Context`.
- `StateFlow` intermediários não são mais recriados com `stateIn`; agora expõem diretamente os `MutableStateFlow` como `StateFlow`.
- `GameSimulator` agora recebe `applicationContext` nos pontos de entrada.
- Simulações que executam vários jogos no mesmo dia reutilizam uma única instância do simulador.
- `GameSimulator.release()` passou a ser garantido por `try/finally` nos fluxos principais.
- O avanço rápido da temporada não grava no Room a cada partida/dia: usa checkpoints a cada 10 dias e um save final.
- O atraso visual do fast-forward foi reduzido de 120 ms para 50 ms.
- Removidas buscas repetidas `rotation.indexOf(player)` do motor de estatísticas; distribuição de pesos agora usa `mapIndexed`.
- O ViewModel continua sob `viewModelScope`, portanto coroutines são canceladas automaticamente quando o ViewModel é destruído.

## Validação realizada

- Compilação isolada do núcleo Kotlin de simulação/regras: OK, com apenas warning pré-existente sobre `isHome` não utilizado.
- Smoke test manual de 1.000 gerações: OK.
- Build Android completo não foi executado porque o projeto não possui Gradle Wrapper e o ambiente de análise não possui Gradle instalado.
