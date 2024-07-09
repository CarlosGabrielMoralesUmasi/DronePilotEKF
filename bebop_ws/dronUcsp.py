# -*- coding: utf-8 -*-
import os
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import time
import rospy
from std_msgs.msg import Float64

# Configurar la conexión con Firebase
cred = credentials.Certificate("json.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://dronucsp-default-rtdb.firebaseio.com/'
})
ref = db.reference("0001")

# Variable para controlar el estado de vuelo
isAscending = False

# Función para manejar los cambios en el base de datos Firebase
def handle_change(event):
    # Verificar si el cambio es para despegar o aterrizar el dron
    if event.path == "/z":
	control_z(event.data)
    elif event.path == "/x":
	control_x(event.data)
    elif event.path == "/y":
	control_y(event.data)
    elif event.path == "/giro":	
	control_giro(event.data)

# Función para controlar el eje z (ascenso y descenso)
def control_z(z_value):
    global isAscending

    # Obtener la altura actual del dron (por ejemplo, desde un sensor)
    if z_value == 1:
        if not isAscending:  # Solo si no se ha iniciado el ascenso
            os.system("rostopic pub --once /bebop/takeoff std_msgs/Empty")
            isAscending = True
        else:  # Si ya está ascendiendo, incrementar la altura
                os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, 1.0]' '[0.0, 0.0, 0.0]'")
    elif z_value == -1:
        os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, -1.0]' '[0.0, 0.0, 0.0]'")


# Función para controlar el eje x (adelante y atrás)
def control_x(x_value):
    # Leer el valor de velocidad desde Firebase
    velocidad = ref.child("velocidad").get() or 0.5  # Valor por defecto si no se encuentra
    
    # Convertir el valor recibido a velocidad
    speed = abs(x_value) * velocidad  # Velocidad determinada según el valor recibido
    
    if x_value == 0:  # Si el valor es 0, detener el movimiento
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, 0.0]' '[0.0, 0.0, 0.0]'")
    elif x_value > 0:  # Si el valor es mayor a 0, mover hacia la derecha
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[{0}, 0.0, 0.0]' '[0.0, 0.0, 0.0]'".format(speed))
    elif x_value < 0:  # Si el valor es menor a 0, mover hacia la izquierda
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[{0}, 0.0, 0.0]' '[0.0, 0.0, 0.0]'".format(-speed))

# Función para controlar el eje y (izquierda y derecha)
def control_y(y_value):
    # Leer el valor de velocidad desde Firebase
    velocidad = ref.child("velocidad").get() or 0.5  # Valor por defecto si no se encuentra
    
    # Convertir el valor recibido a velocidad
    speed = abs(y_value) * velocidad  # Velocidad determinada según el valor recibido
    
    if y_value == 0:  # Si el valor es 0, detener el movimiento
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, 0.0]' '[0.0, 0.0, 0.0]'")
    elif y_value > 0:  # Si el valor es mayor a 0, mover hacia la derecha
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, {0}, 0.0]' '[0.0, 0.0, 0.0]'".format(speed))
    elif y_value < 0:  # Si el valor es menor a 0, mover hacia la izquierda
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, {0}, 0.0]' '[0.0, 0.0, 0.0]'".format(-speed))

# Función para controlar el giro
def control_giro(giro_value):
    # Leer el valor de velocidad desde Firebase
    velocidad = ref.child("velocidad").get() * 10 or 10  # Valor por defecto si no se encuentra
    
    # Convertir el valor recibido a velocidad
    speed = abs(giro_value) * velocidad  # Velocidad determinada según el valor recibido
    
    if giro_value == 0:  # Si el valor es 0, detener el movimiento
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, 0.0]' '[0.0, 0.0, 0.0]'")
    elif giro_value > 0:  # Si el valor es mayor a 0, girar en sentido horario
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, 0.0]' '[0.0, 0.0, {0}]'".format(-speed))
    elif giro_value < 0:  # Si el valor es menor a 0, girar en sentido antihorario
	os.system("rostopic pub --once /bebop/cmd_vel geometry_msgs/Twist -- '[0.0, 0.0, 0.0]' '[0.0, 0.0, {0}]'".format(speed))


# Almacena los valores iniciales antes de iniciar el bucle
initial_values = ref.get()

# Loop infinito para escuchar constantemente las variables
while True:
    try:
        # Obtener el estado actual de las variables desde Firebase
        current_values = ref.get()

        # Verificar si alguna variable ha cambiado desde su valor inicial
        if any(current_values.get(var) != initial_values.get(var) for var in current_values):
            for variable, value in current_values.items():
                # Verificar si la variable ha cambiado desde su valor inicial
                if value != initial_values.get(variable):
                    # Llamar a la función de control correspondiente a la variable que ha cambiado
                    if variable == "z":
                        control_z(value)
                    elif variable == "x":
                        control_x(value)
                    elif variable == "y":
                        control_y(value)
                    elif variable == "giro":
                        control_giro(value)

                    # Actualizar el valor inicial de la variable que ha cambiado
                    initial_values[variable] = value

    except KeyboardInterrupt:
        print("Saliendo del programa.")
        break

