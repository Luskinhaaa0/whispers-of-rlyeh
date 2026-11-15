# Whispers of R'lyeh — mod do Mythos de Cthulhu (NeoForge 1.21.1)

## Sanidade (0 a 100)
Barra vertical à direita da hotbar (só aparece no Sobrevivência).

| Perde (a cada 3 s) | Recupera (a cada 6 s, mais devagar) |
|---|---|
| Escuro (luz ≤ 3): −3 · Penumbra (luz ≤ 7): −1 | Céu aberto de dia, sem chuva: +1 |
| Estar no End: −2 | Lugar bem iluminado (luz ≥ 12) com sanidade < 40: +1 |
| Submerso abaixo de Y 30: −2 | Sinal Ancião: +1 extra |
| 3+ monstros por perto: −2 | Dormir a noite toda: +15 |
| **Ver um Deep One: −3** | **Chá Calmante: +15** |
| **Ídolo de Cthulhu a até 6 blocos: −2** | **Ler um livro comum (clique direito): +6, 1x por minuto** |
| Tempestade a céu aberto: −1 | Sinal Ancião (usar): +5 |
| Ler o Necronomicon: −12 · Golpe de Deep One: −3 | Morrer volta para 100 |

O Sinal Ancião no inventário corta todas as perdas pela metade.

## Loucura
- **< 70:** sons que não deveriam estar ali
- **< 50:** tontura
- **< 35:** **alucinações**: uma figura (Deep One ou Enderman) parada ao longe, atrás de você, encarando. Ela some quando você chega perto ou olha fixo para ela.
- **< 30:** escuridão, passos fantasmas, sussurros, fome
- **< 20:** **dor fantasma**: a tela treme e você ouve o som de dano, sem perder vida
- **= 0:** fraqueza, lentidão e meio coração de dano a cada 10 s

## Conteúdo
- **Necronomicon:** feitiço aleatório por −12 de sanidade. Receita: livro + bolsa de tinta + carne podre + olho de aranha
- **Sinal Ancião:** protege e repele monstros. Receita: pedregulho musgoso em cruz + ametista no centro
- **Chá Calmante:** +15 de sanidade, tira tontura e escuridão. Receita: frasco de vidro + bagas doces + açúcar + qualquer flor pequena
- **Ídolo de Cthulhu:** drena sanidade de quem está por perto. Dormir perto dele dá *sonhos de R'lyeh*: −15 de sanidade e +3 níveis de XP
- **Deep Ones:** surgem nos oceanos no escuro. Usam a IA do Afogado (podem vir com tridente), têm 30 de vida e 5 de dano
- **Ruínas de R'lyeh:** geram nos oceanos profundos. Têm pedra ciclópica, pilares tortos, altar com Ídolo e baú (Necronomicon, Sinal Ancião, Chá, Ídolo, ouro, Coração do Mar...)

**Comandos de teste** (precisa de cheats): `/sanity get` · `/sanity set 10`
Para achar uma ruína rápido: `/locate` não funciona (é uma "feature", não uma estrutura). Vá para um oceano profundo e procure no fundo com Visão Noturna.

## Compilar
Pelo GitHub Actions: envie os arquivos para o repositório e baixe o `.jar` em Actions → Artifacts.
