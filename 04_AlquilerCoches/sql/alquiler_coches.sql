drop sequence seq_modelos;
drop sequence seq_num_fact;
drop sequence seq_reservas;

drop table precio_combustible cascade constraints;
drop table modelos cascade constraints;
drop table vehiculos cascade constraints;
drop table clientes cascade constraints;
drop table facturas cascade constraints;
drop table lineas_factura cascade constraints;
drop table reservas cascade constraints;

create table clientes(
	NIF	varchar(9) primary key,
	nombre	varchar(20) not null,
	ape1	varchar(20) not null,
	ape2	varchar(20) not null,
	direccion varchar(40) 
);

create table precio_combustible(
	tipo_combustible	varchar(10) primary key,
	precio_por_litro	numeric(4,2) not null
);

create sequence seq_modelos;

create table modelos(
	id_modelo 		integer primary key,
	nombre			varchar(30) not null,
	precio_cada_dia 	numeric(6,2) not null check (precio_cada_dia>=0),
	capacidad_deposito	integer not null check (capacidad_deposito>0),
	tipo_combustible	varchar(10) not null references precio_combustible);


create table vehiculos(
	matricula	varchar(8)  primary key,
	id_modelo	integer  not null references modelos,
	color		varchar(10)
);

create sequence seq_reservas;
create table reservas(
	idReserva	integer primary key,
	cliente  	varchar(9) references clientes,
	matricula	varchar(8) references vehiculos,
	fecha_ini	date not null,
	fecha_fin	date,
	check (fecha_fin >= fecha_ini)
);

create sequence seq_num_fact;
create table facturas(
	nroFactura	integer primary key,
	importe		numeric( 8, 2),
	cliente		varchar(9) not null references clientes
);

create table lineas_factura(
	nroFactura	integer references facturas,
	concepto	char(40),
	importe		numeric( 7, 2),
	primary key ( nroFactura, concepto)
);
	

create or replace procedure reset_seq( p_seq_name varchar )
--From https://stackoverflow.com/questions/51470/how-do-i-reset-a-sequence-in-oracle
is
    l_val number;
begin
    --Averiguo cual es el siguiente valor y lo guardo en l_val
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    --Utilizo ese valor en negativo para poner la secuencia cero, pimero cambiando el incremento de la secuencia
    execute immediate
    'alter sequence ' || p_seq_name || ' increment by -' || l_val || 
                                                          ' minvalue 0';
   --segundo pidiendo el siguiente valor
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    --restauro el incremento de la secuencia a 1
    execute immediate
    'alter sequence ' || p_seq_name || ' increment by 1 minvalue 0';
end;
/

create or replace procedure inicializa_test is
begin
  	reset_seq( 'seq_modelos' );
  	reset_seq( 'seq_num_fact' );
  	reset_seq( 'seq_reservas' );
        
  
    delete from lineas_factura;
    delete from facturas;
    delete from reservas;
    delete from vehiculos;
    delete from modelos;
    delete from precio_combustible;
    delete from clientes;
   
		
    insert into clientes values ('12345678A', 'Pepe', 'Perez', 'Porras', 'C/Perezoso n1');
    insert into clientes values ('11111111B', 'Beatriz', 'Barbosa', 'Bernardez', 'C/Barriocanal n1');
    
    insert into precio_combustible values ('Gasolina', 1.5);
    insert into precio_combustible values ('Gasoil',   1.4);
    
    insert into modelos values ( seq_modelos.nextval, 'Renault Clio Gasolina', 15, 50, 'Gasolina');
    insert into vehiculos values ( '1234-ABC', seq_modelos.currval, 'VERDE');

    insert into modelos values ( seq_modelos.nextval, 'Renault Clio Gasoil', 16,   50, 'Gasoil');
    insert into vehiculos values ( '1111-ABC', seq_modelos.currval, 'VERDE');
    insert into vehiculos values ( '2222-ABC', seq_modelos.currval, 'GRIS');
	
    commit;
end;
/

--set serveroutput on
exec inicializa_test;

exit
