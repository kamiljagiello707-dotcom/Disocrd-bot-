# Prefixy rang – Owner i Admin (serwer Minecraft)

Wymagany plugin: **LuckPerms** + plugin od czatu, który pokazuje prefixy
(np. **EssentialsX Chat**, **LPC** albo **ChatControl**). Gradienty (HEX) działają od **1.16+**.

Komendy wpisz w konsoli serwera (bez `/`) albo w grze z `/`.

## 1. Tworzenie rang

```
lp creategroup owner
lp creategroup admin
lp group owner setweight 100
lp group admin setweight 90
lp group owner parent add admin
```

## 2. Prefixy

### Wersja z gradientem (1.16+)

**OWNER** – czerwony → pomarańczowy:
```
lp group owner meta setprefix 100 "&8[&#FF0000&lO&#FF2A00&lW&#FF5500&lN&#FF8000&lE&#FFAA00&lR&8] &c"
```

**ADMIN** – niebieski → turkusowy:
```
lp group admin meta setprefix 90 "&8[&#0055FF&lA&#0080FF&lD&#00AAFF&lM&#00D4FF&lI&#00FFFF&lN&8] &b"
```

### Wersja klasyczna (każda wersja MC)

```
lp group owner meta setprefix 100 "&8[&4&lOWNER&8] &c"
lp group admin meta setprefix 90 "&8[&c&lADMIN&8] &e"
```

Wygląd w czacie:
- `[OWNER] Nick: wiadomość`
- `[ADMIN] Nick: wiadomość`

## 3. Uprawnienia

```
lp group owner permission set * true
lp group admin permission set essentials.* true
lp group admin permission set minecraft.command.* true
lp group admin permission set luckperms.* false
```

## 4. Nadawanie rangi graczowi

```
lp user NICK parent set owner
lp user NICK parent set admin
```

## 5. Format czatu (EssentialsX Chat)

W `plugins/Essentials/config.yml`:
```yaml
chat:
  format: '{PREFIX}{DISPLAYNAME}&7: &f{MESSAGE}'
```

## 6. Prefix w TAB / nad głową (opcjonalnie, plugin TAB)

W `plugins/TAB/groups.yml`:
```yaml
owner:
  tabprefix: "&8[&#FF0000&lOWNER&8] &c"
  tagprefix: "&8[&#FF0000&lOWNER&8] &c"
admin:
  tabprefix: "&8[&#0055FF&lADMIN&8] &b"
  tagprefix: "&8[&#0055FF&lADMIN&8] &b"
```
