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
    IF arg_id_primer_plato IS NOT NULL THEN
        SELECT disponible INTO plato_disponible 
        FROM platos 
        WHERE id_plato = arg_id_primer_plato;
        IF plato_disponible = 0 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Uno de los platos seleccionados no está disponible.');
        END IF;
    END IF;
    
    -- Verificar disponibilidad del segundo plato
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
    SELECT pedidos_activos INTO numeros_pedidos_activos 
    FROM personal_servicio 
    WHERE id_personal = arg_id_personal
    FOR UPDATE;
    IF numeros_pedidos_activos >= 5 THEN
        RAISE_APPLICATION_ERROR(-20003, 'El personal de servicio tiene demasiados pedidos.');
    END IF;
    
    -- Crear el pedido
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
    
    IF arg_id_segundo_plato IS NOT NULL THEN
        INSERT INTO detalle_pedido (id_pedido, id_plato, cantidad) VALUES (id_nuevo_pedido, arg_id_segundo_plato, 1);
        SELECT precio + precio_total_pedido INTO precio_total_pedido 
        FROM platos 
        WHERE id_plato = arg_id_segundo_plato
        FOR UPDATE;
    END IF;
    
    -- Actualizar total del pedido
    UPDATE pedidos SET total = precio_total_pedido WHERE id_pedido = id_nuevo_pedido;

    -- Actualizar contador de pedidos activos del personal
    UPDATE personal_servicio SET pedidos_activos = pedidos_activos + 1 WHERE id_personal = arg_id_personal;

    COMMIT;
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
-- * P4.1
--
-- * P4.2
--
-- * P4.3
--
-- * P4.4
--
-- * P4.5
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
	 
  --caso 1 Pedido correct, se realiza
  BEGIN
    inicializa_test;
    registrar_pedido(1, 1, 1, 2);
        DBMS_OUTPUT.PUT_LINE('Caso 1: Pedido correcto - OK');
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('Caso 1: Pedido correcto - ERROR');
  END;
  
  -- Caso 2: Pedido sin platos (-20002)
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