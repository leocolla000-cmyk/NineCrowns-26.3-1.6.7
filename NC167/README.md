# Nine Crowns 1.6.7 — Minecraft Java 26.3 / Fabric

## Server
- 9 partecipanti registrati.
- I primi 9 UUID distinti che entrano vengono salvati nel mondo.
- Ogni partecipante lascia sempre la propria testa alla morte.
- Le teste conservano il profilo/skin del giocatore.

## Ricette leggendarie
Schema per tutti e tre i leggendari:

H H H
H X H
H H H

H = le 8 teste DIVERSE degli altri 8 partecipanti.
La testa del giocatore che sta craftando viene rifiutata.
Otto copie della stessa testa non funzionano.

X:
- Netherite Sword -> OP Sword
- Netherite Spear -> OP Spear
- Empty Crown -> Emperor Crown

Ogni leggendario può essere craftato una sola volta nel mondo.
La mod blocca anche il Crafter automatico vanilla per queste tre ricette,
perché non ha un'identità giocatore con cui validare le 8 teste.

## OP Sword
- Sharpness V forzato
- indistruttibile
- 14 danni raw totali con Sharpness V
- è +3 danni = +1,5 cuori rispetto a Netherite Sword + Sharpness V
- 20% Poison I per 3 secondi
- 10% Slowness I per 3 secondi
- armatura e Protection riducono il danno normalmente

## OP Spear
- Sharpness V forzato
- Lunge III forzato
- indistruttibile
- jab: 10 danni raw totali con Sharpness V = 5 cuori prima di armatura/protezioni
- Shift + tasto destro: attacco speciale
- bersaglio entro 24 blocchi
- 3 fulmini visuali
- un singolo evento da 12 danni = 6 cuori prima di armatura/protezioni
- cooldown: 30 secondi
- una sola carica
- cooldown globale della lancia: passarla/rubarla non resetta i 30 secondi
- fulmini visual-only: non incendiano
- armatura e Protection riducono il danno speciale normalmente

## Emperor Crown
- Protection X
- Speed II
- Strength II
- Fire Resistance I
- indistruttibile
- gli effetti vengono rimossi quando la corona viene tolta

## Empty Crown
Ricetta:
G D G
E Z E
G G G

G = Gold Ingot
D = Diamond
E = Emerald
Z = Dragon Egg

## JEI / ricette
Le ricette JSON sono presenti così JEI e il recipe system possono visualizzare lo schema.
JEI mostra teste generiche; la validazione vera viene fatta server-side e richiede
esattamente le 8 teste degli altri 8 giocatori.

Comandi:
- /ninecrowns
- /ninecrowns recipes
- /ninecrowns status

## Build
- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Fabric Loom 1.17.14
- Java 25
- Gradle 9.6.0 nel workflow GitHub Actions

Il workflow cerca automaticamente la cartella della versione 1.6.7 anche se il progetto
è stato caricato dentro una cartella tipo NC164/.

## Note 1.6.7
- In questa versione NON è presente lo String Duper.
- Il controllo build verifica il `remapJar`, cioè il JAR Fabric giocabile.
- `main.yml` nella root del pacchetto è una copia di comodità del workflow che deve trovarsi in `.github/workflows/main.yml` nella root del repository GitHub.


## Emperor Crown — fit 1.6.7
La geometria e l'estetica restano identiche. Ho cambiato solo la posizione quando è indossata:
- rotazione: [0, 0, 0] invariata
- scala: [0.82, 0.82, 0.82] invariata
- Y: 10.0 -> 4.5

Così la fascia scende attorno alla parte alta della testa invece di appoggiarsi sopra, mentre le punte restano visibili.
