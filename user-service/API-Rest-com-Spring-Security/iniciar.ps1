# iniciar.ps1 — Inicia os tres servicos em terminais separados

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$userService  = Join-Path $root "user-service"
$msEmail      = Join-Path $root "ms-email"
$frontend     = Join-Path $root "..\..\..\..\..\frontend"
$frontend     = (Resolve-Path $frontend).Path

Write-Host "Iniciando user-service  em $userService"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$userService'; mvn spring-boot:run"

Start-Sleep -Seconds 3

Write-Host "Iniciando ms-email      em $msEmail"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$msEmail'; mvn spring-boot:run"

Start-Sleep -Seconds 3

Write-Host "Iniciando frontend      em $frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontend'; npm install; npm start"

Write-Host ""
Write-Host "Todos os servicos foram iniciados."
Write-Host "Acesse: http://localhost:3000"
