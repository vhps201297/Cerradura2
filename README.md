# Cerradura
En este proyecto se pretende usar los temas que corresponden a la materia de estructuras discretas.
Se propuso como problema, el tener acceso a un lugar de forma segura, para ello, la propuesta de solución fue crear una aplicacion en android studio para, que las personas que 
quieran tener acceso a la puerta, deberán ingresar una contresañe desde la aplicación correspondiente y si esta es correcta, la puerta (magnética) se abrirá.
El proyecto implica una comunicación a un módulo bluetooth en Arduino, mediante un cifrado de curva eliptica y una puerta magnética, el cual se creará apartir de una bobina, una placa de metal y una fuente de voltaje.
La aplicación consta de iniciar una comunicación segura, entre la aplicación y el módulo bluetooth, por lo que inicialmente
se debe de vincular, posteriormente se ingresará la contraseña dentro de la aplicación y si la contraseña ingresada es correcta, se hará un corte en la corriente eléctrica para así impedir que se siga generando el campo magnético en la placa metálica.
