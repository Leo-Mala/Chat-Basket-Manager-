# ETAPA 2 — Motor de Simulação e Consistência Estatística

## Alterações

### 1. Novo `MatchSimulationEngine`
O motor de estatísticas foi separado de Android, SharedPreferences, notificações e áudio.

### 2. Pontuação dos jogadores
A distribuição usa pesos de uso ofensivo e garante que a soma das pontuações individuais seja exatamente igual ao placar da equipe.

### 3. Minutos
A rotação utiliza até 10 jogadores e distribui exatamente 240 minutos por equipe, respeitando o limite de 48 minutos por jogador.

### 4. Box Score matematicamente consistente
Para cada jogador é garantido:

- FGM >= 3PM
- FGA >= FGM
- 3PA >= 3PM
- FTA >= FTM
- `2*(FGM-3PM) + 3*3PM + FTM == pontos`

### 5. Estatísticas coletivas
Rebotes, assistências, roubos, tocos, turnovers e faltas são distribuídos a partir de pesos por jogador e fecham nos totais gerados para a equipe.

### 6. Plus/Minus
O plus/minus individual é distribuído pelos minutos e fecha exatamente no diferencial de pontos da equipe.

### 7. Quartos
O placar agora é dividido em quatro períodos cuja soma é exatamente igual ao placar final.

### 8. ID da partida
O ID combina timestamp e abreviações das equipes para reduzir colisões.

### 9. Testes
Foi adicionado `MatchSimulationEngineTest`, executando 100 partidas sintéticas e verificando fechamento de pontos, minutos, plus/minus e identidade do box score.

## Limitação de validação
O projeto original não possui Gradle Wrapper e o ambiente de execução desta auditoria não disponibiliza o executável Gradle. Portanto, o build Android completo não pôde ser executado nesta etapa.
