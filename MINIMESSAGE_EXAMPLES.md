# MiniMessage Examples for Cobblemon Challenges

This mod now supports **MiniMessage** formatting! You can use advanced text formatting in your config.yml messages.

## 🎨 **Basic Usage**

Instead of using old `&` color codes, you can now use MiniMessage:

### **Old way:**
```yaml
message: "&aGreen text &l&nBold underlined"
```

### **New way:**
```yaml
message: "<green>Green text <bold><underlined>Bold underlined</underlined></bold></green>"
```

## 🌈 **Advanced Features**

### **1. Gradients**
```yaml
prefix: "<gradient:yellow:gold><bold>Cobblemon</bold></gradient> <white><bold>Challenges</bold></white>"
title: "<gradient:red:dark_red><bold>⚠ WARNING ⚠</bold></gradient>"
```

### **2. Rainbow Text**
```yaml
celebration: "<rainbow><bold>¡Felicitaciones por completar el desafío!</bold></rainbow>"
```

### **3. Hover Text**
```yaml
challenge_name: "<hover:show_text:'Click para ver detalles'><yellow>{challenge_name}</yellow></hover>"
```

### **4. Clickable Text**
```yaml
help_message: "<click:run_command:'/challenges help'><green>[Click for Help]</green></click>"
url_link: "<click:open_url:'https://discord.gg/yourserver'><blue><underlined>Join Discord</underlined></blue></click>"
```

### **5. Combined Effects**
```yaml
awesome_message: "<gradient:gold:yellow><bold><hover:show_text:'This is amazing!'><click:run_command:'/challenges'>[⭐ CHALLENGES ⭐]</click></hover></bold></gradient>"
```

## 🎯 **Practical Examples**

### **Mission Replacement System:**
```yaml
replacement:
  separator: "<gradient:yellow:gold>═════════════════════════════════════════</gradient>"
  title: "<gradient:red:dark_red><bold>⚠  REEMPLAZO DE MISIÓN REQUERIDO  ⚠</bold></gradient>"
  current-mission-name: "<gray>  Actual: <hover:show_text:'Misión actual en progreso'><yellow>{challenge_name}</yellow></hover></gray>"
  confirm-button: "<hover:show_text:'Click para confirmar'><click:run_command:'/challenges confirm'><green><bold>[✓ CONFIRMAR]</bold></green></click></hover>"
```

### **Challenge Completion:**
```yaml
completion:
  success: "<rainbow><bold>¡DESAFÍO COMPLETADO!</bold></rainbow>"
  reward: "Recompensa: <gradient:gold:yellow><hover:show_text:'Click para reclamar'><bold>{reward}</bold></hover></gradient>"
```

## 🛠 **How to Use**

1. **Update your config.yml** with MiniMessage formatting
2. **Use the new methods** in code:
   - `api.getMiniMessage(key)` - Returns Adventure Component
   - `profile.sendAdventureMessage(adventureComponent)` - Send Adventure Component to player
   - `api.getMiniMessageComponent(key)` - Returns Minecraft Component (for internal use)

## 🔗 **Resources**

- **MiniMessage Web Editor:** https://webui.advntr.dev/
- **Documentation:** https://docs.advntr.dev/minimessage/
- **Color Names:** https://docs.advntr.dev/minimessage/format.html#color

## ⚡ **Performance**

MiniMessage is:
- ✅ **Backward compatible** - Old `&` codes still work
- ✅ **More powerful** - Gradients, hover, click events
- ✅ **Better performance** - Efficient parsing
- ✅ **Modern standard** - Used by major plugins

Enjoy creating beautiful, interactive messages! 🎉