# Etapa 4 — Extração do domínio

Esta etapa introduz serviços de domínio independentes da UI/Android:

- `FinanceManager`: regras de receitas, despesas, patrocínios e upgrades.
- `RosterManager`: lineup, sincronização, free agency e contratação.
- `SeasonManager`: calendário e adversário da rodada.
- `TradeManager`: proposta e execução de trocas.
- `DraftManager`: geração da classe e seleção de rookie.
- `PlayoffManager`: classificação, finais, premiações e mando/receita de playoffs.

O ViewModel continua como orquestrador de estado e efeitos (save, Toast, navegação), mas as regras centrais passam a ser chamadas pelos serviços de domínio.

Próxima etapa: substituir SharedPreferences/Gson por repositórios e Room, mantendo estes serviços sem dependência de Android.
