from pathlib import Path
p = Path('app/src/main/java/com/example/GameViewModel.kt')
text = p.read_text()
old = '''            if (!SavedGameStartupRules.hasRequiredCore(snapshot)) {
                withContext(Dispatchers.Main) {
                    loadErrorMessage = null
                    savedGameLoadState = SavedGameLoadState.EMPTY
                    gameState = GameState.SETUP
                }
                return
            }
'''
new = old + '''            // The helper validates the core payload, but Kotlin cannot smart-cast through it.
            // Re-establish non-nullness explicitly before reading the snapshot fields below.
            snapshot ?: return
'''
if text.count(old) != 1:
    raise SystemExit(f'Expected one startup core block, found {text.count(old)}')
text = text.replace(old, new, 1)
p.write_text(text)
