# OwnTelecom

Plugin Minecraft (Paper **1.20.4**) symulujący sieci komórkowe: operatorzy, stacje bazowe, połączenia, SMS, internet i roaming z integracją **Vault**.

## Wymagania

- Paper 1.20.4 (kompatybilne z 1.20.x)
- Java 17+
- Vault + plugin ekonomii (np. EssentialsX)

## Budowanie

```bash
mvn clean package
```

Plik JAR: `target/OwnTelecom-1.0.0-SNAPSHOT.jar`

## Szybki start

1. Wgraj plugin + Vault na serwer Paper.
2. `/operator utworz Phonify US phonifyus` — tworzy sieć (koszt w `config.yml`).
3. `/stacja utworz phonifyus` — GUI wyboru technologii (EDGE/LTE/5G).
4. `/operator dolacz` — inni gracze wybierają operatora.
5. `/telefon info` — zasięg, technologia, prędkość.
6. `/call Gracz`, `/sms Gracz wiadomość`, `/112 pomoc`.
7. `/internet` — przeglądarka stron.
8. `/owntelecom globalchat on` — tymczasowy chat globalny (eventy).

## Moduły

| Moduł | Status | Opis |
|-------|--------|------|
| Chat lokalny | ✅ | Promień 10 bloków, konfigurowalny; override admina |
| Operatorzy | ✅ | ID + nazwa, właściciel, transfer, cooldown 7 dni |
| Stacje | ✅ | GUI, technologie, poziomy, awarie |
| Połączenia/SMS | ✅ | Umowy między operatorami, jakość sygnału |
| Internet | ✅ | Strony, MineTweet, koszt MB, opóźnienie od prędkości |
| Płatności | ✅ | Prepaid (Vault) + pakiety subskrypcyjne |
| Umowy | 🔶 | Podstawowe tworzenie; strefy roamingowe — do rozbudowy |
| Serwerownie | 🔶 | Schema DB + config; GUI/komendy — następna iteracja |

## Personalizacja stron (propozycja)

System **linii tekstowych w bazie** + komendy (bez zewnętrznych plików dla graczy):

```
/strona utworz mojblog "Moj Blog"
/strona linia dodaj mojblog &aWitaj na moim blogu!
/strona linia dodaj mojblog &7Tu opisuje moje przygody...
/strona podglad mojblog
```

- Treść w SQLite (`website_lines`), kolory przez `&` kody.
- Limity w `internet.yml` (`max-lines`, `max-line-length`).
- Admin/właściciel: `/strona usun`, wyłączenie w DB.
- Rozszerzenia: szablony (`page-templates`), przyciski akcji (klik = komenda), import z YAML dla adminów.

## Optymalizacja

- **Indeks przestrzenny stacji** (chunk hash) — O(stacje w okolicy), nie O(wszystkie).
- **Cache zasięgu gracza** (500 ms, próg ruchu 2 bloki).
- **SQLite + HikariCP** — lekkie, async zapis.
- **Async sprawdzanie awarii** — co 30 min (config).

## Konfiguracja

- `config.yml` — chat, ekonomia, wydajność
- `technologies.yml` — EDGE/LTE/5G, prędkości, zasięgi, awarie
- `stations.yml` — ustawienia stacji
- `internet.yml` — hosting, social media, limity stron
- `messages.yml` — wszystkie komunikaty

## Komendy admina

- `/owntelecom reload`
- `/owntelecom globalchat on|off`
- Uprawnienia: `owntelecom.admin`, `owntelecom.bypass.cooldown`, `owntelecom.bypass.chat`

## Roadmap

1. Strefy roamingowe/połączeń (ID + nazwa, GUI pakietów roamingowych)
2. Serwerownie (poziomy 1–3, wynajem slotów)
3. Przełączanie kosztów umów na klienta (B2B settlement)
4. Komendy admina na stacje (`/owntelecom stacja ...`)
5. Testy obciążeniowe (100+ graczy online)
