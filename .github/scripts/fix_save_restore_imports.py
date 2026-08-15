from pathlib import Path

vm_path = Path('app/src/main/java/com/example/GameViewModel.kt')
app_path = Path('app/src/main/java/com/example/ui/GameApp.kt')
vm = vm_path.read_text()
app = app_path.read_text()

anchor_vm = 'import com.example.domain.rules.ContractRules\n'
needed_vm = 'import com.example.domain.rules.SavedGameLoadState\nimport com.example.domain.rules.SavedGameStartupRules\n'
if needed_vm not in vm:
    if anchor_vm not in vm:
        raise SystemExit('GameViewModel import anchor missing')
    vm = vm.replace(anchor_vm, anchor_vm + needed_vm, 1)

anchor_app = 'import com.example.domain.playoff.PlayoffManager\n'
needed_app = 'import com.example.domain.rules.SavedGameLoadState\n'
if needed_app not in app:
    if anchor_app not in app:
        raise SystemExit('GameApp import anchor missing')
    app = app.replace(anchor_app, anchor_app + needed_app, 1)

vm_path.write_text(vm)
app_path.write_text(app)
