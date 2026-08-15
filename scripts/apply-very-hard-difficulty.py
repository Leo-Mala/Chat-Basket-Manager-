from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/example/ui/SetupScreen.kt",
    '''                            2 to ("Difícil" to "Adversários mais competitivos e menor margem para erros.")\n''',
    '''                            2 to ("Difícil" to "Adversários mais competitivos e menor margem para erros."),\n                            3 to ("Muito Difícil" to "Desafio extremo: sua equipe rende menos e os adversários recebem vantagem máxima nas simulações.")\n''',
)

replace_once(
    "app/src/main/java/com/example/simulator/SimulationRules.kt",
    '''        2 -> 0.94\n        else -> 0.98\n''',
    '''        2 -> 0.94\n        3 -> 0.90\n        else -> 0.98\n''',
)

replace_once(
    "app/src/main/java/com/example/simulator/SimulationRules.kt",
    '''        2 -> 1.06\n        else -> 1.02\n''',
    '''        2 -> 1.06\n        3 -> 1.10\n        else -> 1.02\n''',
)

replace_once(
    "app/src/test/java/com/example/simulation/SimulationRulesTest.kt",
    '''    @Test fun engineProducesNonNegativeStats() {\n''',
    '''    @Test fun difficultyGetsStrictlyHarderAtEachLevel() {\n        val userModifiers = (0..3).map(SimulationRules::difficultyUserModifier)\n        val opponentModifiers = (0..3).map(SimulationRules::difficultyOpponentModifier)\n\n        assertTrue(userModifiers.zipWithNext().all { (easier, harder) -> easier > harder })\n        assertTrue(opponentModifiers.zipWithNext().all { (easier, harder) -> easier < harder })\n        assertTrue(SimulationRules.difficultyUserModifier(3) == 0.90)\n        assertTrue(SimulationRules.difficultyOpponentModifier(3) == 1.10)\n    }\n\n    @Test fun engineProducesNonNegativeStats() {\n''',
)

print("Very hard difficulty patch applied successfully")
