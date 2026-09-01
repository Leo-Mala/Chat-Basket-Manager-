# Etapa 12 — Auditoria final de produção

## Resultado

Auditoria final aplicada sobre a versão das etapas 10 e 11.

## Alterações

- Release passou a usar R8/minificação e `shrinkResources`.
- Release deixou de depender de um keystore local inexistente; a assinatura de produção permanece dependente de credenciais de release fornecidas ao CI/Android Studio.
- Java/Kotlin Android target atualizado para Java 17.
- Plugins Google Services e Secrets removidos porque não havia uso efetivo no código analisado.
- Dependências sem referências de produção removidas: Firebase AI/App Check, Retrofit, Moshi, OkHttp e logging-interceptor.
- Room passou a exportar schemas via KSP.
- Estrutura `app/schemas/` adicionada para versionamento das migrações.
- NotificationHelper agora usa `applicationContext`, verifica `POST_NOTIFICATIONS` e não engole exceções silenciosamente.
- Revisados Manifest, permissões, release configuration, ProGuard, persistência, lifecycle e testes.

## Validação executável atual

A limitação descrita na auditoria original foi superada pela evolução posterior do repositório: o projeto atual contém Gradle Wrapper versionado e possui GitHub Actions executando o toolchain Android real.

No HEAD `9ffe7f7bcb5205f03bae6d6d78e86ef755562c55` da `main`, em 1 de setembro de 2026, a certificação pós-merge concluiu com sucesso:

- `Android Build & Test` — run #205;
- `Android Lint` — run #196;
- `Security Audit` — run #188.

O gate `Android Build & Test` executou testes unitários, `assembleDebug`, `assembleRelease`, `bundleRelease`, verificação do APK debug, testes instrumentados em emulador Android API 35, verificação dos artefatos e upload dos resultados.

Os artefatos dessa certificação foram preservados pelo GitHub Actions:

- `basket-manager-release-artifacts` — APK debug, APK release e AAB release;
- `basket-manager-test-reports` — resultados e relatórios dos testes.

Isso certifica o build automatizado atual, mas não substitui assinatura de produção nem teste manual de instalação/upgrade de uma versão destinada a publicação.

## Checklist de release

- [x] Persistência Room
- [x] Migrações Room versionadas e testadas
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
- [x] `assembleDebug` real no GitHub Actions
- [x] `assembleRelease` real no GitHub Actions
- [x] `bundleRelease` real no GitHub Actions
- [x] testes instrumentados em emulador Android API 35
- [x] APK/AAB e relatórios preservados como artefatos do CI
- [ ] assinatura de produção com credenciais de release
- [ ] teste manual de instalação/upgrade do artefato destinado à publicação

## Veredito

A base atual possui build Android completo e testes automatizados certificados no GitHub Actions. A etapa técnica de compilação que estava pendente na auditoria original não está mais pendente.

Uma publicação oficial ainda deve exigir, separadamente, assinatura de produção quando aplicável e validação manual de instalação/upgrade do artefato que será efetivamente distribuído. Nenhuma tag ou GitHub Release é criada por esta atualização documental.
