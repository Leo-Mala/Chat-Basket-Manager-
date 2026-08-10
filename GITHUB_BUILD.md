# GitHub Actions — validação real do Basket Manager

Este projeto está preparado para ser enviado diretamente para um repositório GitHub.

## Compatibilidade fixada

- Android Gradle Plugin: 9.1.1
- Gradle: 9.3.1
- JDK: 17
- compile/target SDK: 36

O AGP 9.1.1 requer Gradle 9.3.1 e JDK 17.

## Como usar

1. Crie um repositório no GitHub.
2. Extraia este ZIP.
3. Envie **todo o conteúdo** para o repositório, mantendo `.github/workflows/`.
4. Abra **Actions** no GitHub.
5. Execute `Android Build & Test` com `Run workflow`, se quiser iniciar manualmente.

O workflow executará:

```text
unit tests
    ↓
lintDebug
    ↓
assembleDebug
    ↓
assembleRelease
    ↓
bundleRelease
    ↓
connectedDebugAndroidTest
```

Ao terminar, os APKs aparecem em **Artifacts**:

- `basket-manager-release-artifacts` (APK debug, APK release e AAB release)

Também haverá um artifact com os relatórios de testes.

## Importante sobre o Release

O APK de release está propositalmente **sem assinatura de produção**. Isso evita colocar uma chave privada no repositório.

Para publicar na Google Play, configure uma assinatura segura no GitHub Actions usando GitHub Secrets/Variables ou um sistema externo de assinatura.

## Se o build falhar

Abra o workflow em:

`GitHub → Actions → Android Build & Test`

O log mostrará a tarefa Gradle e o arquivo/linha que falhou.

## Gradle Wrapper e validação no GitHub

O projeto original não continha Gradle Wrapper e este ambiente de preparação não possui uma instalação local de Gradle nem acesso de rede para materializar o `gradle-wrapper.jar`. Por isso, o ZIP não finge conter um Wrapper completo que não foi validado.

O workflow resolve isso de forma reprodutível no próprio GitHub:

1. `gradle/actions/setup-gradle` instala exatamente o Gradle 9.3.1.
2. O workflow executa `gradle wrapper --gradle-version 9.3.1`.
3. O `gradlew`, `gradle-wrapper.jar` e `gradle-wrapper.properties` são verificados.
4. Todas as tarefas seguintes usam `./gradlew`.

Assim, a validação real não depende de um Gradle pré-instalado no runner além do bootstrap controlado pela versão fixada.

Para gerar e commitar o Wrapper no repositório a partir de uma máquina com Gradle 9.3.1, execute:

```bash
./scripts/bootstrap-gradle-wrapper.sh
```

Depois do comando, faça commit de:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

O workflow já está preparado para usar esses arquivos automaticamente quando estiverem presentes.


## Release signing

See `RELEASE_SIGNING.md`. CI signs the release only when all four signing secrets are present; otherwise the release is intentionally unsigned for technical validation.
