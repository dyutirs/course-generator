@echo off
REM Remove only the generated content directories
rmdir /s /q generated\A1
rmdir /s /q generated\A2
rmdir /s /q generated\B1
rmdir /s /q generated\B2
rmdir /s /q generated\C1
rmdir /s /q generated\C2
del complete_course_data.json

echo Generated content directories removed.
