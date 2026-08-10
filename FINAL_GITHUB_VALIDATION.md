# Validação final para GitHub

## Objetivo

Este projeto está preparado para que o GitHub Actions faça a validação real do build Android.

## Pipeline

```text
Checkout
  ↓
JDK 17
  ↓
Android SDK 36 + emulator API 35
  ↓
Gradle 9.3.1 bootstrap
  ↓
Gradle Wrapper gerado e verificado
  ↓
./gradlew test
  ↓
./gradlew lintDebug
  ↓
./gradlew assembleDebug
  ↓
./gradlew assembleRelease
  ↓
./gradlew bundleRelease
  ↓
verificação de APK/AAB
  ↓
connectedDebugAndroidTest
  ↓
artifacts + relatórios
```

## Assinatura

Sem os quatro secrets de assinatura, o build release continua tecnicamente válido, porém unsigned.

Com os secrets abaixo, o workflow cria temporariamente o keystore no runner e assina o release:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEY_ALIAS`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_PASSWORD`

Nenhuma chave privada deve ser commitada no repositório.

## Limitação desta preparação

O ZIP foi auditado estaticamente, mas o build Android completo não foi executado neste ambiente porque não há Android SDK/Gradle local disponível. A execução real foi transferida para o GitHub Actions, que instala as versões necessárias e executa as tarefas com `./gradlew`.

## Critério de aprovação

A versão só deve ser considerada Release Candidate validada depois de o workflow terminar com sucesso em:

- `test`
- `lintDebug`
- `assembleDebug`
- `assembleRelease`
- `bundleRelease`
- `connectedDebugAndroidTest`
