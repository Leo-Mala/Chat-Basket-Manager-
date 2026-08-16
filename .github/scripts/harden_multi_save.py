from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:180]!r}")
    p.write_text(text.replace(old, new, 1))

# Persist the current slot before opening new-career setup.
replace_once(
    'app/src/main/java/com/example/ui/GameApp.kt',
    '''                        onNewCareer = {
                            val targetSlot = saveSlots.firstOrNull { !it.occupied }?.slotId ?: activeSlotId
                            SaveSlotManager.setPendingNewSlot(context, targetSlot)
                            viewModel.gameState = CareerResumeRules.newCareerState()
                            showMainMenu = false
                        },
''',
    '''                        onNewCareer = {
                            val targetSlot = saveSlots.firstOrNull { !it.occupied }?.slotId ?: activeSlotId
                            menuScope.launch {
                                if (viewModel.savedGameLoadState == SavedGameLoadState.READY && viewModel.managedTeam != null) {
                                    viewModel.saveGame()?.join()
                                }
                                SaveSlotManager.setPendingNewSlot(context, targetSlot)
                                viewModel.gameState = CareerResumeRules.newCareerState()
                                showMainMenu = false
                            }
                        },
'''
)
replace_once(
    'app/src/main/java/com/example/ui/GameApp.kt',
    '''                        onNewCareerSlot = { slotId ->
                            SaveSlotManager.setPendingNewSlot(context, slotId)
                            viewModel.gameState = CareerResumeRules.newCareerState()
                            showMainMenu = false
                        },
''',
    '''                        onNewCareerSlot = { slotId ->
                            menuScope.launch {
                                if (viewModel.savedGameLoadState == SavedGameLoadState.READY && viewModel.managedTeam != null) {
                                    viewModel.saveGame()?.join()
                                }
                                SaveSlotManager.setPendingNewSlot(context, slotId)
                                viewModel.gameState = CareerResumeRules.newCareerState()
                                showMainMenu = false
                            }
                        },
'''
)

# A bad slot must not trap the player away from other careers.
replace_once(
    'app/src/main/java/com/example/ui/GameApp.kt',
    '''                            Button(onClick = { viewModel.retryLoadSavedGame() }) {
                                Text("TENTAR CARREGAR NOVAMENTE")
                            }
                            Text("O save não será apagado por esta tela.", color = TextGray, textAlign = TextAlign.Center)
''',
    '''                            Button(onClick = { viewModel.retryLoadSavedGame() }) {
                                Text("TENTAR CARREGAR NOVAMENTE")
                            }
                            saveSlots
                                .filter { it.occupied && it.slotId != activeSlotId }
                                .forEach { slot ->
                                    OutlinedButton(
                                        onClick = {
                                            SaveSlotManager.clearPendingNewSlot(context)
                                            SaveSlotManager.setActiveSlot(context, slot.slotId)
                                            activeSlotId = slot.slotId
                                            pendingContinueSlot = slot.slotId
                                            viewModel.retryLoadSavedGame()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("ABRIR SLOT ${slot.slotId} — ${slot.teamName ?: "CARREIRA SALVA"}")
                                    }
                                }
                            Text("O save com erro não será apagado por esta tela.", color = TextGray, textAlign = TextAlign.Center)
'''
)

# Loading an actually empty slot must clear stale in-memory data from the prior career.
replace_once(
    'app/src/main/java/com/example/GameViewModel.kt',
    '''            if (!SavedGameStartupRules.hasRequiredCore(snapshot)) {
                withContext(Dispatchers.Main) {
                    loadErrorMessage = null
                    savedGameLoadState = SavedGameLoadState.EMPTY
                    gameState = GameState.SETUP
                }
                return
            }
''',
    '''            if (!SavedGameStartupRules.hasRequiredCore(snapshot)) {
                val appContext = getApplication<Application>().applicationContext
                SaveSlotManager.clearSlotMetadata(appContext, SaveSlotManager.getActiveSlot(appContext))
                withContext(Dispatchers.Main) {
                    loadErrorMessage = null
                    managedTeam = null
                    coach = null
                    finances = null
                    tactics = null
                    season = null
                    historyManager = HistoryManager()
                    currentAwards = null
                    latestResult = null
                    playoffResult = null
                    startingFive = emptyList()
                    freeAgents = emptyList()
                    draftRookies = emptyList()
                    contracts = emptyMap()
                    availableStaffMarket = emptyList()
                    assistantNotifications.clear()
                    teamStaff = TeamStaff()
                    teamFacilities = TeamFacilities()
                    financeAdvanced = FinanceAdvanced()
                    newsFeed.clear()
                    latestBoxScore = null
                    savedGameLoadState = SavedGameLoadState.EMPTY
                    gameState = GameState.SETUP
                }
                return
            }
'''
)

# Guard text to ensure the hardening landed.
for file, needles in {
    'app/src/main/java/com/example/ui/GameApp.kt': ['viewModel.saveGame()?.join()', 'ABRIR SLOT ${slot.slotId}'],
    'app/src/main/java/com/example/GameViewModel.kt': ['SaveSlotManager.clearSlotMetadata', 'managedTeam = null'],
}.items():
    text = Path(file).read_text()
    for needle in needles:
        if needle not in text:
            raise SystemExit(f'Missing {needle!r} in {file}')
