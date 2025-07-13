@echo off
cd /d %~dp0
cd ai_agent
call venv\Scripts\activate.bat
python app.py 