# Whispers of R'lyeh — mod do Mythos de Cthulhu (NeoForge 1.21.1)

## O que já tem (v0.1.0)

**Sistema de sanidade (0 a 100)**: uma barra vertical à direita da hotbar.
- Verde-água: mente estável · Roxo: inquieto · Vermelho: à beira da loucura

| Perde sanidade (a cada 3 s) | Recupera sanidade |
|---|---|
| Escuro (luz ≤ 3): −2 | Céu aberto de dia: +1 |
| Estar no End: −1 | Lugar bem iluminado (sanidade < 50): +1 |
| Submerso abaixo de Y 30: −1 | Dormir a noite inteira: +30 |
| 3+ monstros por perto: −1 | Usar o Sinal Ancião: +5 |
| Ler o Necronomicon: −12 | Morrer volta para 100 |

**Efeitos da loucura**
- < 60: sons estranhos (batimentos do Warden, o olhar do Enderman…)
- < 40: tontura aleatória e vinheta roxa pulsando na tela
- < 25: escuridão, passos fantasmas, sussurros ("Ph'nglui mglw'nafh…")
- = 0: fraqueza e lentidão constantes

**Itens** (aba criativa "Sussurros de R'lyeh"):
- **Necronomicon**: clique direito lança um feitiço aleatório (visão além do véu, força dos Antigos, bênção de Dagon ou XP). Custa 12 de sanidade, tem 10 s de recarga e machuca se você ler com sanidade < 15.
  Receita (sem forma): livro + bolsa de tinta + carne podre + olho de aranha
- **Sinal Ancião (Elder Sign)**: no inventário, corta pela metade a perda de sanidade. Com clique direito, repele monstros num raio de 8 blocos (64 usos, recarga de 30 s).
  Receita: pedregulho musgoso em cruz com fragmento de ametista no centro

**Comandos de teste** (precisa de cheats): `/sanity get` e `/sanity set 10`

## Como rodar

1. Instale o **JDK 21** (ex.: Eclipse Temurin 21). O Gradle também tenta baixar sozinho.
2. Abra a pasta do projeto no **IntelliJ IDEA** (File → Open). Ele importa o Gradle sozinho; a primeira vez demora porque baixa o Minecraft.
3. Para abrir o jogo com o mod:
   - Windows: `gradlew.bat runClient`
   - Linux/Mac: `./gradlew runClient`
   - Ou, no IntelliJ: aba Gradle → Tasks → mod development → runClient
4. Para gerar o `.jar` para colocar na pasta `mods`: `gradlew build` → arquivo em `build/libs/`.

## Estrutura

```
src/main/java/com/whispersofrlyeh/
├── WhispersOfRlyeh.java      classe principal
├── registry/                 itens, aba criativa, dado de sanidade
├── sanity/                   regras de sanidade (servidor) e cópia no cliente
├── item/                     Necronomicon e Sinal Ancião
├── network/                  pacote que sincroniza a sanidade com o cliente
├── command/                  /sanity
└── client/                   HUD (barra + vinheta)
src/main/resources/
├── assets/whispersofrlyeh/   texturas, modelos, traduções (pt_br e en_us)
└── data/whispersofrlyeh/     receitas
```

Para ajustar o equilíbrio, os números estão em `sanity/SanityEvents.java`.
