# Desarrollo de aplicación Android — Planificador de Compras

Quiero desarrollar una aplicación Android nativa en **Java**, orientada a la planificación, registro y análisis de las compras del hogar.

La aplicación debe permitir crear una lista de compras antes de ir al supermercado, registrar los precios reales durante la compra, aplicar descuentos y almacenar todo en una base de datos para posteriormente conocer los hábitos de compra, frecuencia, precios y gasto histórico.

## 1. Objetivo de la aplicación

La aplicación debe resolver el siguiente problema:

> "Quiero planificar mi compra semanal, saber qué productos necesito comprar, estimar cuánto voy a gastar, registrar el precio real cuando estoy en el supermercado y posteriormente poder analizar cuánto compro y cuánto gasto."

El sistema debe funcionar principalmente **offline**, utilizando una base de datos local.

Tecnologías obligatorias:

* Android nativo
* Java
* Android Studio
* SQLite mediante Room
* Material Design
* RecyclerView
* ViewModel
* LiveData o StateFlow equivalente para Java
* Repository Pattern
* Arquitectura MVVM

No utilizar Kotlin.

---

# 2. Concepto principal

La aplicación debe trabajar con tres conceptos principales:

### Productos

Representan los productos que normalmente compro.

Ejemplos:

* Leche
* Pan
* Arroz
* Huevos
* Café
* Detergente
* Papel higiénico

Cada producto debe mantener información histórica.

### Lista de compra

Representa una compra que estoy planificando.

Ejemplo:

> Compra semana 39 — 27/09/2026

Contiene los productos que necesito comprar.

### Compra realizada

Cuando voy al supermercado, puedo convertir una lista planificada en una compra realizada y registrar cuánto costó realmente cada producto.

---

# 3. Flujo principal

El flujo principal de la aplicación debe ser:

```text
Inicio
   ↓
Nueva lista de compra
   ↓
Agregar productos
   ↓
Definir cantidades
   ↓
Estimar precios
   ↓
Ver total estimado
   ↓
Ir al supermercado
   ↓
Registrar precio real
   ↓
Aplicar descuento
   ↓
Confirmar producto
   ↓
Finalizar compra
   ↓
Guardar historial
   ↓
Analizar estadísticas
```

---

# 4. Pantalla principal

Crear un Dashboard moderno.

Debe mostrar:

### Compra actual

* Nombre de la lista
* Fecha
* Cantidad de productos
* Total estimado
* Total real
* Diferencia

Ejemplo:

```text
Compra semanal

27 septiembre 2026

12 productos

Estimado
$85.000

Real
$78.450

Ahorraste
$6.550
```

Botones:

* Continuar compra
* Nueva lista
* Historial
* Productos
* Estadísticas

---

# 5. Crear nueva lista

Permitir crear una nueva lista.

Campos:

* Nombre
* Fecha
* Tipo de compra

Tipos:

* Semanal
* Quincenal
* Mensual
* Otra

Ejemplo:

```text
Nombre:
Compra supermercado

Fecha:
27/09/2026

Periodicidad:
Semanal
```

Al crearla, debe quedar en estado:

```text
PLANIFICADA
```

---

# 6. Agregar productos

La lista debe permitir agregar productos existentes.

Ejemplo:

```text
Buscar producto...

[ Leche ]
[ Pan ]
[ Arroz ]
[ Huevos ]
```

También debe existir:

**+ Crear producto**

Cada producto debe tener:

* ID
* Nombre
* Categoría
* Unidad de medida
* Cantidad habitual
* Precio estimado
* Stock disponible
* Activo

Categorías:

* Alimentos
* Bebidas
* Limpieza
* Higiene
* Congelados
* Frutas y verduras
* Otros

---

# 7. Cantidad

Al agregar un producto se debe poder definir:

```text
Leche

Cantidad:
[-] 2 [+]

Unidad:
Litros

Precio estimado:
$1.200
```

La aplicación debe calcular automáticamente:

```text
Subtotal = cantidad × precio estimado
```

---

# 8. Inventario

Quiero que el sistema considere el inventario existente.

Ejemplo:

Tengo:

```text
Leche
Stock actual: 1

Consumo habitual:
3 unidades por semana
```

Cuando creo una nueva lista, el sistema puede sugerir:

```text
Leche

Stock actual: 1
Consumo habitual: 3

Cantidad sugerida:
2
```

La lógica debe ser configurable.

No asumir automáticamente que todo producto con stock debe comprarse.

El usuario debe poder modificar la cantidad sugerida.

---

# 9. Lista de compra

Mostrar los productos en una lista.

Ejemplo:

```text
COMPRA SEMANAL

☐ Leche              2 x $1.200
☐ Pan                1 x $2.000
☐ Huevos             12 x $3.500
☐ Arroz              2 x $1.500
☐ Café               1 x $5.990

----------------------------

Total estimado
$16.890
```

Permitir:

* Editar cantidad
* Eliminar producto
* Cambiar precio estimado
* Marcar como comprado
* Agregar producto
* Reordenar productos

---

# 10. Modo supermercado

Esta es una funcionalidad importante.

Cuando el usuario está físicamente en el supermercado debe poder activar:

**"Modo compra"**

La interfaz debe ser extremadamente rápida.

Cada producto debe mostrar:

```text
Leche

Cantidad:
2

Precio estimado:
$1.200

Precio real:
[ $ ______ ]

Descuento:
[ 10 ] %

Precio final:
$ ______

☐ Comprado
```

El usuario debe poder ingresar rápidamente el precio que aparece en el supermercado.

---

# 11. Descuentos

Cada producto debe permitir registrar un descuento.

Tipos:

### Porcentaje

Ejemplo:

```text
Precio:
$5.000

Descuento:
20%

Precio final:
$4.000
```

### Monto fijo

Ejemplo:

```text
Precio:
$5.000

Descuento:
$1.000

Precio final:
$4.000
```

Permitir seleccionar:

```text
Tipo descuento

○ Porcentaje
○ Monto fijo
```

Calcular automáticamente el precio final.

Nunca permitir que el precio final sea negativo.

---

# 12. Precio estimado vs precio real

La aplicación debe diferenciar claramente:

```text
Precio estimado
```

de

```text
Precio real
```

Ejemplo:

```text
Café

Estimado:
$5.990

Real:
$6.490

Diferencia:
+$500
```

También debe calcular:

```text
Total estimado
Total real
Diferencia
Porcentaje de diferencia
```

---

# 13. Finalizar compra

Cuando el usuario termina su compra debe presionar:

**Finalizar compra**

Antes de finalizar mostrar resumen:

```text
RESUMEN DE COMPRA

Productos: 14

Total estimado:
$75.000

Total real:
$69.450

Descuentos:
$8.300

Diferencia:
-$5.550

-------------------

¿Finalizar compra?

[Cancelar]
[Finalizar]
```

Al confirmar:

Cambiar estado:

```text
COMPLETADA
```

Guardar toda la información histórica.

---

# 14. Historial

Crear pantalla:

**Historial de compras**

Ejemplo:

```text
Septiembre 2026

27 Sep
Compra semanal
$69.450

20 Sep
Compra semanal
$73.200

13 Sep
Compra semanal
$71.800
```

Permitir abrir una compra y visualizar todos sus productos.

---

# 15. Historial de productos

Cada producto debe guardar historial de precios.

Ejemplo:

```text
Leche

Historial de precios

27 Sep    $1.290
20 Sep    $1.190
13 Sep    $1.250
06 Sep    $1.290
```

Mostrar:

* Precio promedio
* Precio mínimo
* Precio máximo
* Último precio
* Cantidad de compras
* Frecuencia de compra

---

# 16. Estadísticas

Crear un módulo de estadísticas.

Mostrar:

### Gasto mensual

```text
Septiembre
$285.450
```

### Gasto semanal

Mostrar gráfico.

### Productos más comprados

Ejemplo:

```text
1. Leche
2. Pan
3. Huevos
4. Arroz
5. Café
```

### Productos con mayor gasto

Ejemplo:

```text
Carne
$48.000

Lácteos
$35.000

Limpieza
$28.500
```

### Descuentos obtenidos

```text
Descuentos este mes

$23.450
```

---

# 17. Comparación entre compras

Permitir comparar:

```text
Esta semana vs semana anterior
```

Ejemplo:

```text
Gasto

Semana anterior
$73.500

Esta semana
$69.450

Diferencia
-$4.050
```

También comparar precios de productos.

Ejemplo:

```text
Leche

Semana 1   $1.190
Semana 2   $1.250
Semana 3   $1.290
Semana 4   $1.190
```

---

# 18. Compra recurrente

La aplicación debe aprender de las compras anteriores.

Por ejemplo:

Si el usuario compra:

```text
Leche → todas las semanas
Pan → todas las semanas
Huevos → cada 2 semanas
Café → cada 4 semanas
```

La aplicación debe utilizar el historial para sugerir productos.

Al crear una nueva compra mostrar:

```text
Productos sugeridos

☑ Leche
☑ Pan
☑ Huevos
☑ Café
☐ Detergente
```

El usuario decide cuáles agregar.

No agregar automáticamente productos sin confirmación.

---

# 19. Productos favoritos

Permitir marcar productos como:

**Favoritos**

Los productos favoritos deben aparecer primero al crear una nueva lista.

---

# 20. Base de datos

Utilizar Room.

Crear como mínimo las siguientes entidades:

### Product

```text
id
name
categoryId
unit
estimatedPrice
currentStock
minimumStock
usualQuantity
isFavorite
isActive
createdAt
updatedAt
```

### Category

```text
id
name
```

### ShoppingList

```text
id
name
type
status
plannedDate
completedDate
estimatedTotal
realTotal
totalDiscount
createdAt
updatedAt
```

### ShoppingListItem

```text
id
shoppingListId
productId
quantity
estimatedUnitPrice
realUnitPrice
discountType
discountValue
finalUnitPrice
estimatedSubtotal
realSubtotal
isPurchased
```

### PurchaseHistory

Si es necesario, separar la información histórica de la lista.

Campos:

```text
id
shoppingListId
purchaseDate
total
discount
```

### PriceHistory

```text
id
productId
shoppingListId
price
discount
finalPrice
date
```

---

# 21. Relaciones

Implementar relaciones:

```text
Category
   │
   └── Product
          │
          ├── PriceHistory
          │
          └── ShoppingListItem
                    │
                    └── ShoppingList
```

Utilizar Foreign Keys correctamente.

---

# 22. Estados de la lista

Definir enum:

```text
PLANNED
IN_PROGRESS
COMPLETED
CANCELLED
```

Flujo:

```text
PLANNED
   ↓
IN_PROGRESS
   ↓
COMPLETED
```

También:

```text
PLANNED → CANCELLED
IN_PROGRESS → CANCELLED
```

---

# 23. Interfaz

Diseñar una interfaz Android moderna, limpia y rápida.

Utilizar:

* Material 3
* Cards
* Floating Action Button
* Bottom Navigation
* RecyclerView
* Dialogs / Bottom Sheets
* Material TextInputLayout
* Chips
* Switches
* Checkboxes

Navegación principal:

```text
Inicio
Lista
Productos
Historial
Estadísticas
```

---

# 24. UX del modo supermercado

El modo supermercado debe priorizar velocidad.

Evitar formularios largos.

Para registrar un producto:

```text
Producto
Precio real
Descuento
Confirmar
```

El teclado numérico debe aparecer automáticamente al ingresar el precio.

Después de confirmar un producto, avanzar automáticamente al siguiente producto pendiente.

Debe ser posible marcar un producto como:

```text
No encontré
```

y permitir posteriormente indicar el motivo:

* Sin stock
* No estaba disponible
* Cambié de opinión
* Otro

---

# 25. Productos no encontrados

La aplicación debe diferenciar:

```text
COMPRADO
NO ENCONTRADO
NO COMPRADO
```

Esto permitirá posteriormente analizar qué productos suelen faltar.

---

# 26. Productos sustitutos

Permitir registrar eventualmente un producto sustituto.

Ejemplo:

```text
Producto planificado:
Leche marca A

Producto comprado:
Leche marca B
```

Registrar ambos.

---

# 27. Inteligencia futura

Diseñar el código pensando en incorporar posteriormente inteligencia artificial.

Ejemplos futuros:

* Predecir productos que probablemente compraré.
* Detectar aumentos de precio.
* Sugerir cantidades.
* Detectar productos que compro frecuentemente.
* Estimar cuánto gastaré la próxima semana.
* Generar automáticamente una lista de compras.
* Analizar mis hábitos de consumo.

**No implementar IA inicialmente.**

Primero desarrollar una versión completamente funcional basada en reglas y datos históricos.

---

# 28. Arquitectura del proyecto

Utilizar:

```text
MVVM

UI
 ↓
ViewModel
 ↓
Repository
 ↓
Room DAO
 ↓
SQLite
```

Separar claramente:

```text
data/
domain/
ui/
utils/
```

Utilizar buenas prácticas de Java.

Evitar colocar lógica de negocio dentro de Activities.

---

# 29. Validaciones

Implementar:

* No permitir cantidades menores o iguales a 0.
* No permitir precios negativos.
* No permitir descuentos negativos.
* No permitir descuentos porcentuales superiores al 100%.
* Evitar productos duplicados dentro de una misma lista.
* Confirmar antes de eliminar productos.
* Confirmar antes de finalizar una compra.
* Validar todos los campos.

---

# 30. Entregables

Quiero que desarrolles el proyecto completo.

Entregar:

1. Proyecto Android Studio.
2. Código Java.
3. Arquitectura MVVM.
4. Base de datos Room.
5. DAOs.
6. Repositories.
7. ViewModels.
8. Activities/Fragments.
9. Adapters.
10. Layouts XML.
11. Navegación.
12. Material Design.
13. Datos iniciales de ejemplo.
14. Validaciones.
15. Estadísticas.
16. Historial.
17. Modo supermercado.

El proyecto debe poder abrirse directamente en Android Studio y ejecutarse en un dispositivo Android o emulador.

---

# 31. Prioridad de desarrollo

Desarrollar por fases.

## FASE 1 — MVP

Implementar:

* Productos
* Categorías
* Crear lista
* Agregar productos
* Cantidades
* Precio estimado
* Total estimado
* Modo supermercado
* Precio real
* Descuentos
* Total real
* Finalizar compra
* Historial

## FASE 2

Implementar:

* Inventario
* Productos favoritos
* Sugerencias basadas en historial
* Historial de precios
* Estadísticas
* Comparaciones

## FASE 3

Implementar:

* Compras recurrentes
* Predicción de productos
* Análisis avanzado
* IA
* Recomendaciones inteligentes

No intentar desarrollar todas las fases simultáneamente.

Primero dejar completamente funcional la FASE 1.

---

# 32. Requisito importante

La aplicación debe ser realmente utilizable por una persona mientras está haciendo sus compras.

Por lo tanto:

* pocos clics
* botones grandes
* lectura clara
* teclado numérico para precios
* cálculos automáticos
* navegación rápida
* funcionamiento offline
* no depender de Internet
* no exigir demasiados datos al usuario

El objetivo no es crear solamente un CRUD de productos.

El objetivo es crear una herramienta personal para **planificar, ejecutar, registrar y analizar las compras semanales**, utilizando el historial de datos para mejorar progresivamente las próximas listas de compra.
