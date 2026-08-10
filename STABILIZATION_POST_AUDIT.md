# Stabilização pós-auditoria

## Correções implementadas

1. `NbaDataGenerator` agora devolve cópias profundas dos jogadores-base; uma carreira não contamina outra.
2. Reset de nova carreira limpa o banco antes de criar o novo estado.
3. Contratos passam a ser persistidos na mesma transação do estado principal e são limpos no reset.
4. Jogadores são apagados no reset completo em vez de apenas arquivados, evitando reutilização de IDs entre carreiras.
5. Índices Room foram adicionados para as consultas mais frequentes.
6. Migração Room 3→4 cria os índices novos.
7. Teste de isolamento dos jogadores-base e unicidade dos IDs foi adicionado.
8. Teste instrumentado de migração foi adicionado.
9. GitHub Actions agora executa `assembleDebug`, `assembleRelease`, `bundleRelease` e `connectedDebugAndroidTest`.
10. O workflow publica APK Debug, APK Release e AAB Release.

## Ainda depende do GitHub/Android Studio

- execução real do build;
- execução dos testes instrumentados;
- instalação do APK;
- upgrade de uma instalação anterior e validação das migrações;
- assinatura de produção do AAB.

Esses itens não são declarados como concluídos sem execução real em um ambiente Android.
