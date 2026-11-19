@echo off
title TRD EVOLUTION - Iniciando Stack
color 0A
echo ========================================================
echo   TRD CONSULTORIA DE TI - TRD EVOLUTION DOCKER
echo ========================================================
echo.
echo [1/3] Subindo servicos Docker (API, Frontend, Postgres, Redis, Whisper)...
cd /d "%~dp0trd-evolution"
docker compose up -d

echo.
echo [2/3] Aguardando inicializacao dos servicos...
timeout /t 3 >nul

echo.
echo [3/3] Abrindo front de conexao no navegador...
start http://localhost:3000
start http://localhost:8080/qrcode.html

echo.
echo ========================================================
echo   TRD EVOLUTION Ativo com Sucesso!
echo   - Evolution Manager:  http://localhost:3000
echo   - QR Code / Parear:   http://localhost:8080/qrcode.html
echo   - Simulador Cliente:  http://localhost:8080/test-chat.html
echo   - API:                http://localhost:8085
echo ========================================================
timeout /t 5 >nul
