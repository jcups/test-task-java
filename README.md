Если у вас ОС Windows, то просто запустите bat файл:

>startJersey.bat - реализация Jersey Client 

>startOpenfeign.bat - реализация OpenFeign

В противном случае выполните команты последовательно:
>gradle build - сборка проекта

>gradle run --args="0"; реализация Jersey Client

>gradle run --args="1"; реализация OpenFeign