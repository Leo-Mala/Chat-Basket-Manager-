from pathlib import Path

p = Path('app/src/main/java/com/example/GameViewModel.kt')
s = p.read_text()

old_regular = '''                if (currentSeason.currentDay >= totalDays) {
                    viewModelScope.launch {
                        val completionSound = SoundManager(appContext)
                        try {
                            completionSound.playWhistle()
                            delay(250)
                        } finally {
                            completionSound.release()
                        }
                    }
                }
'''
new_regular = '''                if (currentSeason.currentDay >= totalDays) {
                    playCompletionWhistle()
                }
'''
assert s.count(old_regular) == 1, 'regular-season whistle block not found exactly once'
s = s.replace(old_regular, new_regular)

marker = '    fun simulationConfig(effectsEnabled: Boolean = true): SimulationConfig = SimulationConfig(\n'
helper = '''    private fun playCompletionWhistle() {
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch {
            val completionSound = SoundManager(appContext)
            try {
                completionSound.playWhistle()
                delay(250)
            } finally {
                completionSound.release()
            }
        }
    }

'''
assert s.count(marker) == 1, 'simulationConfig marker not found exactly once'
s = s.replace(marker, helper + marker)

old_final = '''        gameState = GameState.CHAMPIONSHIP_CELEBRATION
        saveGame()
    }

}'''
new_final = '''        gameState = GameState.CHAMPIONSHIP_CELEBRATION
        saveGame()
        playCompletionWhistle()
    }

}'''
assert s.count(old_final) == 1, 'playoff completion block not found exactly once'
s = s.replace(old_final, new_final)

p.write_text(s)
