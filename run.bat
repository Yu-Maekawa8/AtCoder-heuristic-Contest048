@echo off
javac Main.java
if %errorlevel% neq 0 (
    echo コンパイルに失敗しました。
    pause
    exit /b
)
java Main < input.txt > output.txt
echo 実行が完了しました。output.txt を確認してください。
