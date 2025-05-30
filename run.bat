@echo off
chcp 65001 >nul
rem --- Javaファイルをコンパイル ---
javac C.java

rem --- 入力ファイルから読み込み、出力ファイルに書き出す ---
java C < input.txt > output.txt

rem --- 実行結果のファイル出力を知らせる ---
echo 実行が完了しました。output.txt を確認してください。

rem --- コマンドプロンプトを一時停止（出力を見たいとき） ---
pause
