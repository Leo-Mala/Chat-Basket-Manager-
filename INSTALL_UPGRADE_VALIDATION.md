# Instalação e upgrade — validação final

## Fluxo recomendado no GitHub/Android Studio

1. Instalar o APK Debug/Release gerado pelo Actions.
2. Criar uma carreira e avançar pelo menos alguns dias.
3. Fechar e reabrir o aplicativo.
4. Confirmar que a carreira, elenco, finanças e histórico continuam intactos.
5. Instalar o APK da versão seguinte sobre a instalação anterior.
6. Abrir a carreira existente.
7. Confirmar a migração Room e verificar contratos, jogadores e estatísticas.
8. Criar uma nova carreira e confirmar que nenhum estado da carreira anterior permanece.

## Critérios de aprovação

- APK instala sem erro.
- Abertura após upgrade preserva o save.
- IDs de jogadores continuam únicos.
- Contratos continuam presentes.
- Estatísticas não são duplicadas.
- Nova carreira começa limpa.
- `assembleDebug`, `assembleRelease`, `bundleRelease`, unit tests e instrumented tests passam no GitHub Actions.
