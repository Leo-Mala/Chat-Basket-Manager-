from pathlib import Path

p = Path('app/src/main/java/com/example/GameViewModel.kt')
text = p.read_text()
old = '''            val rosterRecovery = UserRosterRecovery(contractManager).recover(
                currentSeasonNumber = loadedSeason.seasonNumber,
                currentDay = loadedSeason.currentDay,
                team = canonicalTeam,
                history = loadedHistory,
                freeAgents = loadedFreeAgents,
                contracts = rosterRecovery.contracts
            )
'''
new = '''            val rosterRecovery = UserRosterRecovery(contractManager).recover(
                currentSeasonNumber = loadedSeason.seasonNumber,
                currentDay = loadedSeason.currentDay,
                team = canonicalTeam,
                history = loadedHistory,
                freeAgents = loadedFreeAgents,
                contracts = loadedContracts
            )
'''
if old not in text:
    raise SystemExit('recovery call block not found')
text = text.replace(old, new, 1)
old2 = '''                freeAgents = rosterRecovery.freeAgents
                draftRookies = loadedDraftRookies
                contracts = loadedContracts
'''
new2 = '''                freeAgents = rosterRecovery.freeAgents
                draftRookies = loadedDraftRookies
                contracts = rosterRecovery.contracts
'''
if old2 not in text:
    raise SystemExit('state contracts block not found')
text = text.replace(old2, new2, 1)
text = text.replace('                    canonicalTeam.name\n', '                    effectiveTeam.name\n', 1)
p.write_text(text)
