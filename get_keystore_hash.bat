@echo off
echo ========================================
echo   BearOwner Keystore Certificate Info
echo ========================================
echo.
echo Getting certificate details from: C:\Users\BearOwner\BearOwner.jks
echo.

keytool -list -v -keystore "C:\Users\BearOwner\BearOwner.jks" -alias BearOwner

echo.
echo ========================================
echo   IMPORTANT: Copy the SHA1 fingerprint
echo   Remove the colons (:) for KeyAuth hash
echo ========================================
echo.
echo Example: If SHA1 is AB:CD:EF:12:34:56...
echo KeyAuth hash should be: ABCDEF123456...
echo.
pause
