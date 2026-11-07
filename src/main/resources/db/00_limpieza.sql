-- =====================================================================
-- QuindíoStay · Bases de Datos I · Grupo 02D · 2026-2
-- Script 00: limpieza y creación de la base de datos
-- Motor: MySQL 8.4 LTS
-- Ejecutar como root (o un usuario con permiso para crear bases).
-- Se puede correr las veces que se quiera: siempre deja la base vacía.
-- =====================================================================

-- 1. Si la base ya existe, se borra completa con todas sus tablas.
--    Así los demás scripts siempre corren sobre una base limpia.
DROP DATABASE IF EXISTS quindiostay;

-- 2. Se crea de nuevo. utf8mb4 permite guardar tildes, la ñ y
--    cualquier carácter (nombres como "Calarcá" o "Génova").
CREATE DATABASE quindiostay
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- 3. A partir de aquí todo se ejecuta dentro de quindiostay.
USE quindiostay;
