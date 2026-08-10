# Gradle Wrapper — estado do projeto

O projeto está preparado para usar o Gradle Wrapper em toda a execução de CI.

## Por que o `gradle-wrapper.jar` não está no ZIP?

Este ambiente de preparação não possui Gradle instalado e não possui acesso de rede para baixar o artefato binário do Wrapper. Em vez de incluir um arquivo binário não validado, o workflow do GitHub instala **Gradle 9.3.1** de forma explícita e gera o Wrapper no próprio runner:

```bash
gradle --no-daemon wrapper --gradle-version 9.3.1 --distribution-type bin
```

Em seguida, o restante do pipeline usa exclusivamente:

```bash
./gradlew
```

O workflow também verifica que `gradlew`, `gradle-wrapper.jar` e `gradle-wrapper.properties` foram realmente criados antes de continuar.

## Para materializar o Wrapper no repositório

Em qualquer máquina com Gradle 9.3.1:

```bash
./scripts/bootstrap-gradle-wrapper.sh
```

Depois faça commit de:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Não gere nem comite um keystore ou qualquer chave privada.
