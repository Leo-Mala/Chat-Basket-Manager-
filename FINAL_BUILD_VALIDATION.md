# Validação final de build

## Resultado

A configuração do projeto foi revisada para o build Android final. A execução real de `assembleDebug`/`assembleRelease` não foi possível neste ambiente porque não há Android SDK nem Gradle instalado e não é possível incorporar o SDK Android no ZIP do aplicativo.

## Preparação feita

- README atualizado para remover instruções obsoletas de Gemini/signingConfig.
- Room configurado com `exportSchema = true`.
- `scripts/verify-build.sh` criado para executar testes, debug e release em uma máquina com Android SDK/Gradle Wrapper.
- Release continua sem chave privada embutida.

## Validação que deve ocorrer no ambiente Android

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

Depois disso, instalar o APK debug e executar uma carreira mínima: criar equipe → salvar → fechar → abrir → simular jogos → avançar temporada → Draft → trocar jogador → carregar save.
