# Sistema de Rotación de Misiones - CobblemonChallenges

## 📋 Resumen

Se ha implementado un sistema completo de rotación de misiones que permite mostrar solo un número limitado de desafíos en el GUI, rotando automáticamente según intervalos configurables (diario, semanal, mensual) de forma aleatoria para que cada jugador tenga experiencias diferentes.

## ✨ Características Principales

- **Rotación Automática**: Las misiones rotan según intervalos configurables
- **Selección Aleatoria**: Las misiones se seleccionan aleatoriamente para cada rotación
- **Preservación de Misiones Activas**: Las misiones en progreso NO rotan hasta completarse
- **Configuración Flexible**: Cada categoría puede tener su propio intervalo y cantidad de misiones
- **Persistencia**: El estado de rotación se guarda y carga automáticamente
- **Comandos de Administrador**: Comandos para forzar rotaciones manuales

## 🛠️ Configuración

### Parámetros en challenge-list:

```yaml
challenge-list:
  max-challenges-per-player: 1
  
  # Configuración de rotación de misiones
  visible-missions: 9          # Cantidad de misiones a mostrar (máximo)
  rotation-interval: daily     # Intervalo de rotación: daily, weekly, monthly, disabled
  
  challenges:
    # ... tus misiones aquí ...
```

### Ejemplos por Categoría:

#### Desafíos Diarios (daily.yml)
```yaml
challenge-list:
  max-challenges-per-player: 1
  visible-missions: 9
  rotation-interval: daily     # Rota cada 24 horas
```

#### Desafíos Semanales (weekly.yml)
```yaml
challenge-list:
  max-challenges-per-player: 1
  visible-missions: 9
  rotation-interval: weekly    # Rota cada 7 días
```

#### Desafíos Mensuales (monthly.yml)
```yaml
challenge-list:
  max-challenges-per-player: 999
  visible-missions: 9
  rotation-interval: monthly   # Rota cada 30 días
```

## 📊 Funcionamiento

### 1. Rotación Automática
- **Verificación**: El sistema verifica cada 10 minutos si es tiempo de rotar
- **Selección Aleatoria**: Selecciona aleatoriamente las misiones visibles
- **Preservación**: Las misiones activas se mantienen hasta completarse

### 2. Lógica de Preservación
- Si un jugador tiene una misión activa, esa misión permanece visible
- Se seleccionan misiones adicionales aleatoriamente para llenar los slots restantes
- Ejemplo: Si `visible-missions: 9` y hay 2 misiones activas, se seleccionan 7 misiones adicionales

### 3. Persistencia de Datos
- **Archivo**: `rotation-data.yml` (generado automáticamente)
- **Contenido**: Última rotación y misiones visibles actuales
- **Carga**: Se restaura automáticamente al iniciar el servidor

## 🎮 Comandos de Administrador

### Rotar Todas las Categorías
```
/challenges rotate
```

### Rotar Categoría Específica
```
/challenges rotate daily
/challenges rotate weekly
/challenges rotate monthly
```

### Permisos Requeridos
- `challenges.commands.admin.rotate` - Para usar comandos de rotación

## 🔧 Ejemplos de Configuración Completa

### Configuración Típica para Servidor
```yaml
# daily.yml
challenge-list:
  max-challenges-per-player: 1
  visible-missions: 9
  rotation-interval: daily
  challenges:
    # 15+ desafíos aquí, pero solo 9 serán visibles a la vez
```

### Configuración sin Rotación
```yaml
# Para desactivar rotación en una categoría
challenge-list:
  max-challenges-per-player: 1
  visible-missions: 50         # Número alto para mostrar todas
  rotation-interval: disabled  # Sin rotación
```

## 🐛 Logs y Depuración

El sistema registra información importante en los logs:
- `Rotated challenges for daily: 2 active preserved, 7 new challenges selected`
- `Loading daily config with X challenges...`
- `Rotated visible challenges for list: daily`

## 📈 Beneficios

1. **Variedad**: Los jugadores ven diferentes misiones cada día/semana/mes
2. **Rendimiento**: Solo se cargan las misiones visibles en el GUI
3. **Engagement**: Los jugadores regresan para ver las nuevas misiones
4. **Flexibilidad**: Cada categoría puede tener configuración diferente
5. **Estabilidad**: Las misiones activas nunca se pierden por rotación

## 🚀 Notas Técnicas

- **Archivo de Rotación**: `rotation-data.yml` se crea automáticamente
- **Scheduler**: Verifica rotaciones cada 10 minutos en el servidor
- **Thread Safety**: El sistema es thread-safe para servidores con múltiples jugadores
- **Compatibilidad**: Funciona con todas las configuraciones existentes

¡El sistema está completamente implementado y listo para usar! 🎉