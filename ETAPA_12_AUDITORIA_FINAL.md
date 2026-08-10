# Etapa 12 — Auditoria final de produção

## Resultado

Auditoria final aplicada sobre a versão das etapas 10 e 11.

## Alterações

- Release passou a usar R8/minificação e `shrinkResources`.
- Release deixou de depender de um keystore local inexistente; o artefato é gerado sem assinatura e deve ser assinado no CI/Android Studio.
- Java/Kotlin Android target atualizado para Java 17.
- Plugins Google Services e Secrets removidos porque não havia uso efetivo no código analisado.
- Dependências sem referências de produção removidas: Firebase AI/App Check, Retrofit, Moshi, OkHttp e logging-interceptor.
- Room passou a exportar schemas via KSP.
- Estrutura `app/schemas/` adicionada para versionamento das migrações.
- NotificationHelper agora usa `applicationContext`, verifica `POST_NOTIFICATIONS` e não engole exceções silenciosamente.
- Revisados Manifest, permissões, release configuration, ProGuard, persistência, lifecycle e testes.

## Pontos ainda dependentes do ambiente de build

O ZIP original não contém Gradle Wrapper e o ambiente de auditoria não possui Gradle instalado. Portanto não foi possível executar `assembleDebug`, `assembleRelease`, R8 ou testes Android instrumentados neste ambiente.

A validação final de release deve ser executada em Android Studio/CI com JDK 17 e Gradle configurado.

## Checklist de release

- [x] Persistência Room
- [x] Migrações Room 1→2→3
- [x] Repositories modulares
- [x] Motor de simulação validado por testes de núcleo
- [x] Testes de regras e invariantes
- [x] Lifecycle do ViewModel revisado
- [x] Notificações Android 13+
- [x] R8/minificação habilitados no release
- [x] Recursos não utilizados encolhidos no release
- [x] Dependências sem uso removidas
- [x] Java 17 configurado
- [x] Room schema export configurado
- [ ] assembleDebug real
- [ ] assembleRelease real
- [ ] testes instrumentados em dispositivo/emulador
- [ ] assinatura de release
- [ ] teste de instalação/upgrade do APK/AAB

## Veredito

A base está tecnicamente preparada para a validação de release, mas não deve ser declarada como APK de produção compilado até que o build Android completo seja executado em um ambiente Gradle/Android SDK real.
