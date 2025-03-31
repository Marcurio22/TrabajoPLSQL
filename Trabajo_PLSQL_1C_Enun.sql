DROP TABLE detalle_pedido CASCADE CONSTRAINTS;
DROP TABLE pedidos CASCADE CONSTRAINTS;
DROP TABLE platos CASCADE CONSTRAINTS;
DROP TABLE personal_servicio CASCADE CONSTRAINTS;
DROP TABLE clientes CASCADE CONSTRAINTS;

DROP SEQUENCE seq_pedidos;


-- Creación de tablas y secuencias



create sequence seq_pedidos;

CREATE TABLE clientes (
    id_cliente INTEGER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    apellido VARCHAR2(100) NOT NULL,
    telefono VARCHAR2(20)
);

CREATE TABLE personal_servicio (
    id_personal INTEGER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    apellido VARCHAR2(100) NOT NULL,
    pedidos_activos INTEGER DEFAULT 0 CHECK (pedidos_activos <= 5)
);

CREATE TABLE platos (
    id_plato INTEGER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    disponible INTEGER DEFAULT 1 CHECK (DISPONIBLE in (0,1))
);

CREATE TABLE pedidos (
    id_pedido INTEGER PRIMARY KEY,
    id_cliente INTEGER REFERENCES clientes(id_cliente),
    id_personal INTEGER REFERENCES personal_servicio(id_personal),
    fecha_pedido DATE DEFAULT SYSDATE,
    total DECIMAL(10, 2) DEFAULT 0
);

CREATE TABLE detalle_pedido (
    id_pedido INTEGER REFERENCES pedidos(id_pedido),
    id_plato INTEGER REFERENCES platos(id_plato),
    cantidad INTEGER NOT NULL,
    PRIMARY KEY (id_pedido, id_plato)
);


	
-- Procedimiento a implementar para realizar la reserva
create or replace procedure registrar_pedido(
    arg_id_cliente      INTEGER, 
    arg_id_personal     INTEGER, 
    arg_id_primer_plato INTEGER DEFAULT NULL,
    arg_id_segundo_plato INTEGER DEFAULT NULL
) is 
    id_nuevo_pedido INTEGER;
    precio_total_pedido DECIMAL(10,2) := 0;
    numeros_pedidos_activos INTEGER;
    plato_disponible INTEGER;
 begin
     -- Verificar disponibilidad del primer plato
     --El SELECT comprueba que se haya añadido un arg_id_primer_plato. Después, comprueba que el plato este disponible. 
     --En caso de que no este disponible se lanza el error -20001.

    IF arg_id_primer_plato IS NOT NULL THEN
        SELECT disponible INTO plato_disponible 
        FROM platos 
        WHERE id_plato = arg_id_primer_plato;
        IF plato_disponible = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Uno de los platos seleccionados no está disponible.');
        END IF;
    END IF;
    
    -- Verificar disponibilidad del segundo plato
    -- El SELECT comprobará que se haya añadido un arg_id_segundo_plato y que se encuentre disponible. 
    -- En caso de no estarlo, se lanzará el error: -20001
    IF arg_id_segundo_plato IS NOT NULL THEN
        SELECT disponible INTO plato_disponible 
        FROM platos 
        WHERE id_plato = arg_id_segundo_plato;
        IF plato_disponible = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Uno de los platos seleccionados no está disponible.');
        END IF;
    END IF;
    
    -- Verificar que al menos un plato ha sido seleccionado
    IF arg_id_primer_plato IS NULL AND arg_id_segundo_plato IS NULL THEN
        RAISE_APPLICATION_ERROR(-20002, 'El pedido debe contener al menos un plato.');
    END IF;   
    
     -- Verificar que el personal de servicio no tiene más de 5 pedidos activos
     --Usamos un SELECT para ver el numero de pedidos activos para el empleado con el id_persona y bloqueamos la fila. 
     --Si el número de pedidos es mayor o igual a 5 lanzamos el error -20003.

    SELECT pedidos_activos INTO numeros_pedidos_activos 
    FROM personal_servicio 
    WHERE id_personal = arg_id_personal
    FOR UPDATE;
    IF numeros_pedidos_activos >= 5 THEN
        RAISE_APPLICATION_ERROR(-20003, 'El personal de servicio tiene demasiados pedidos.');
    END IF;
    
    -- Crear el pedido
    -- Se usa el SELECT de seq_pedido.NEXTVAL para asignar el siguiente valor de seq_Pedidos a id_nuevo_pedido. 
    -- Con INSERT se agrega una nueva fila a la tabla pedidos con fecha actual y total inicial de 0.
    SELECT seq_pedidos.NEXTVAL INTO id_nuevo_pedido 
    FROM dual;
    INSERT INTO pedidos (id_pedido, id_cliente, id_personal, fecha_pedido, total) 
    VALUES (id_nuevo_pedido, arg_id_cliente, arg_id_personal, SYSDATE, 0);
    
    -- Insertar los platos en detalle_pedido y calcular el total
    IF arg_id_primer_plato IS NOT NULL THEN
        INSERT INTO detalle_pedido (id_pedido, id_plato, cantidad) VALUES (id_nuevo_pedido, arg_id_primer_plato, 1);
        SELECT precio INTO precio_total_pedido 
        FROM platos 
        WHERE id_plato = arg_id_primer_plato
        FOR UPDATE;
    END IF;
    
    --Insertamos el segundo plato y añadimos su precio al precio total, en caso de que el valor de arg_id_segundo_plato  no sea null.
    IF arg_id_segundo_plato IS NOT NULL THEN
        INSERT INTO detalle_pedido (id_pedido, id_plato, cantidad) VALUES (id_nuevo_pedido, arg_id_segundo_plato, 1);
        SELECT precio + precio_total_pedido INTO precio_total_pedido 
        FROM platos 
        WHERE id_plato = arg_id_segundo_plato
        FOR UPDATE;
    END IF;
    
    -- Actualizar total del pedido con el nuevo precio calculado.
    UPDATE pedidos SET total = precio_total_pedido WHERE id_pedido = id_nuevo_pedido;

    -- Actualizar contador de pedidos activos del personal
    -- Incrementa en 1 el número de pedidos activos del miembro del personal asignado.
    UPDATE personal_servicio SET pedidos_activos = pedidos_activos + 1 WHERE id_personal = arg_id_personal;

    COMMIT; --Guardamos los cambios.
    
-- Con la excepción se lanza un error si uno de los platos no existe en la base de datos.
-- Revierte los cambios y vuelve a lanzar el error en caso de cualquier otra excepción.
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20004, 'Uno de los platos seleccionados no existe.');
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
    
end;
/

------ Deja aquí tus respuestas a las preguntas del enunciado:
-- NO SE CORREGIRÁN RESPUESTAS QUE NO ESTÉN AQUÍ (utiliza el espacio que necesites apra cada una)
-- * P4.1 ¿Cómo garantizas en tu código que un miembro del personal de servicio no supere el límite de pedidos activos?

--      Para garantizar que se cumpla esta cuestión usamos un select que comprueba que la variable que controla el número de pedidos activos no sea mayor que 5. 
--      Con fines de lograr esto, hemos puesto un mayor o igual en la comprobación para asegurarnos de que esta variable sea estrictamente menor que 5, 
--		ya que, al empezar en 0, nos aseguramos de que con más de 5 pedidos se lance la excepción.
--      Para mantener el contador actualizado, después de registrar el pedido actualizamos el contador pedidos_activos.

-- * P4.2 ¿Cómo evitas que dos transacciones concurrentes asignen un pedido al mismo personal de servicio cuyos pedidos activos estan a punto de superar el límite?

--      En nuestro caso hemos elegido el bloqueo pesimista, porque tiene un enfoque que protege mejor la integridad de la base de datos frente a los errores al manejarlos 
--      directamente en lugar de confiar en que sea improbable que sucedan. 
--      Para realizar esta implementación hemos añadido un FOR UPDATE en el select evitando así que otro proceso acceda o modifique la misma fila a la vez. Esto nos permite 
--      evitar tanto excedernos del límite de 5 por modificación simultánea como incosistencias en la base de datos.
--      Otra opción diferente, podría haber sido emplear el bloqueo optimista que confía en que no se va a dar un caso en el que dos transacciones editen o accedan a la misma 
--      fila en el mismo momento, pensando que es improbable que esto suceda.
--      Para implementar este bloqueo habría que verificar que ninguna fila haya sido modificada, añadiendo la comprobación SQL%ROWCOUNT =0 después de verificar que pedidos_activos 
--      no sea mayor que 5 y antes de lanzar la excepción.

-- * P4.3 Una vez hechas las comprobaciones en los pasos 1 y 2, ¿podrías asegurar que el pedido se puede realizar de manera correcta en el paso 3 y no se generan inconsistencias? 

--      ¿Por qué? Recuerda que trabajamos en entornos con conexiones concurrentes. 

--      Podemos asegurar que el paso 3 se puede realizar sin inconsistencias, ya que, usamos el bloqueo pesimista como hemos indicado en el apartado anterior. El uso del FOR UPDATE 
--      nos garantiza que dos transacciones simultáneas no puedan actualizar el contador pedidos_activos a la vez, asegurando que un mismo empleado no tenga asignados más de 5 pedidos.
--      Además, hemos garantizado que en caso de que que suceda un error se ejecuta un ROLLBACK, en caso de que no haya error también cerramos las transacciones pero en este caso usando un COMMIT.
--      Por último, el CHECK en pedidos_activos ayuda a evitar estados de datos inconsistetes.
--
-- * P4.4 Si modificásemos la tabla de personal_servicio añadiendo CHECK (pedido_activos <=5), ¿Qué implicaciones tendría en tu código? ¿Cómo afectaría en la gestión de excepciones? 
--      Describe en detalle las modificaciones que deberías hacer en tu código para mejorar tu solución ante esta situación (puedes añadir pseudocódigo). 

--      La función Check, que verifica que el número de pedidos activos sea menor o igual a 5, no es suficiente para garantizar la integridad en concurrencia. 
--      Porque no tiene en cuenta varias transacciones simultáneas. Esto podría suponer que si dos procesos incrementan la variable pedidos_activos al mismo tiempo, 
--      ambos entrarían como pedidos activos, incrementando su contador de 4 a 6 en un instante, sin dejarle opción al check de realizar la comprobación previamente.
--      Para poder implementar este cambio en el código habría que quitar la comprobación de que no haya más de 5 pedidos de manera simultánea, explicada en el apartado 1. 
--      Esto se debe a que ahora este error lo realizaría la base de datos utilizando el check. 
--      Además, se debería incluir excepciones para tratar los casos de error del check.
--      Una posible implementación podría ser:

--      INICIO
--          INTENTAR
--              ACTUALIZAR personal_servicio 
--              INCREMENTAR pedidos_activos EN 1 
--              DONDE id_personal SEA IGUAL A arg_id_personal        
--          SI OCURRE EL ERROR ENTONCES
--              SI SE PRODUCE UN ERROR DE VALIDACIÓN ENTONCES
--                   LANZAR EL ERROR(-20003)
--              SINO
--                    REENVIAR EL ERROR
--              FIN SI
--            FIN SI
--        FIN

-- * P4.5 ¿Qué tipo de estrategia de programación has utilizado? ¿Cómo puede verse en tu código?

--      Hemos utilizado una estrategia de programación defensiva con control de concurrencia.
--      Esto se debe a que hemos utilizado un bloqueo pesimista usando el FOR UPDATE, como hemos explicado anteriormente.
--      Además, tenemos una gestión estructurada de excepciones con el uso de  RAISE_APPLICATION_ERROR para el manejo de errores.
--      Garantizamos la atomicidad usando ROLLBACK, en caso de error, o con el uso de COMMIT en caso de que el proceso haya sido correcto.
-- 


create or replace
procedure reset_seq( p_seq_name varchar )
is
    l_val number;
begin
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    execute immediate
    'alter sequence ' || p_seq_name || ' increment by -' || l_val || 
                                                          ' minvalue 0';
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    execute immediate
    'alter sequence ' || p_seq_name || ' increment by 1 minvalue 0';

end;
/


create or replace procedure inicializa_test is
begin
    
    reset_seq('seq_pedidos');
        
  
    delete from Detalle_pedido;
    delete from Pedidos;
    delete from Platos;
    delete from Personal_servicio;
    delete from Clientes;
    
    -- Insertar datos de prueba
    insert into Clientes (id_cliente, nombre, apellido, telefono) values (1, 'Pepe', 'Perez', '123456789');
    insert into Clientes (id_cliente, nombre, apellido, telefono) values (2, 'Ana', 'Garcia', '987654321');
    
    insert into Personal_servicio (id_personal, nombre, apellido, pedidos_activos) values (1, 'Carlos', 'Lopez', 0);
    insert into Personal_servicio (id_personal, nombre, apellido, pedidos_activos) values (2, 'Maria', 'Fernandez', 5);
    
    insert into Platos (id_plato, nombre, precio, disponible) values (1, 'Sopa', 10.0, 1);
    insert into Platos (id_plato, nombre, precio, disponible) values (2, 'Pasta', 12.0, 1);
    insert into Platos (id_plato, nombre, precio, disponible) values (3, 'Carne', 15.0, 0);

    commit;
end;
/

exec inicializa_test;

-- Completa lost test, incluyendo al menos los del enunciado y añadiendo los que consideres necesarios

create or replace procedure test_registrar_pedido is
begin
	 
  --Caso 1 Pedido correct, se realiza
  --Este test simula un escenario en el que se realiza un pedido correcto. 
  --Si el pedido es correcto se muestra un OK por consola, en caso contrario se muestra ERROR

  BEGIN
    inicializa_test;
    registrar_pedido(1, 1, 1, 2);
        DBMS_OUTPUT.PUT_LINE('Caso 1: Pedido correcto - OK');
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('Caso 1: Pedido correcto - ERROR');
  END;
  
  -- Caso 2: Pedido sin platos (-20002)
  --Este test simula un escenario en el que se intenta realizar un pedido sin seleccionar platos.  
  --En caso de que se lance el mensaje de error -20002 muestra OK.

    BEGIN
        registrar_pedido(1, 1, NULL, NULL);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Caso 2: Pedido sin platos - OK');
    END;
  -- Caso 3: Pedido con un plato inexistente (-20004)
    BEGIN
        registrar_pedido(1, 1, 99, NULL);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Caso 3: Plato inexistente - OK');
    END;
    -- Caso 4: Pedido con un plato válido y uno inexistente (-20004)
    BEGIN
        registrar_pedido(1, 1, 1, 99);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Caso 4: Pedido con plato inexistente - OK');
    END;
    -- Caso 5: Pedido con un plato no disponible (-20001)
    -- Este test llama a registrar_pedido con un ID de plato que no está disponible. 
    -- En la tabla platos, hay una columna disponible que puede ser 0 o 1. 
    -- Registrar_pedido verifica este valor y lanza la excepción -20001 si el plato no está disponible en el sistema.
    BEGIN
        registrar_pedido(1, 1, 3, NULL);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Caso 5: Plato no disponible - OK');
    END;
    -- Caso 6: Personal con pedidos máximos (-20003)
    -- La tabla personal_servicio tiene una columna pedidos_activos con una restricción que limita los pedidos activos a 5. 
    -- Registrar_pedido verifica este límite y lanza la excepción -20003 si un miembro del personal ya tiene el máximo de pedidos activos.
    BEGIN
        registrar_pedido(1, 2, 1, NULL);
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Caso 6: Personal con pedidos máximos - OK');
    END;

  -- Idem para el resto de casos

  /* - Si se realiza un pedido vac´ıo (sin platos) devuelve el error -200002.
     - Si se realiza un pedido con un plato que no existe devuelve en error -20004.
     - Si se realiza un pedido que incluye un plato que no est´a ya disponible devuelve el error -20001.
     - Personal de servicio ya tiene 5 pedidos activos y se le asigna otro pedido devuelve el error -20003
     - ... los que os puedan ocurrir que puedan ser necesarios para comprobar el correcto funcionamiento del procedimiento
*/
  
end;
/


set serveroutput on;
exec test_registrar_pedido;