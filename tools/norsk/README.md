# Norske modeller i Marmalade STT (fork)

Forken (`hakonamatata177/marmalade-stt-android`, gren `norsk`) legger til motorer fra en
modelliste på GitHub i tillegg til de innebygde. Listen er `norske-modeller.json` i releasen
[`modeller`](https://github.com/hakonamatata177/marmalade-stt-android/releases/tag/modeller).
Appen henter den hver gang fanen Motorer åpnes. Nye modeller dukker dermed opp uten at appen må bygges på nytt.

## Slik henger det sammen

| Del | Hva |
|---|---|
| `.github/workflows/norske-modeller.yml` | Henter Hugging Face-modeller, konverterer til sherpa-onnx int8, tester og laster opp |
| `hf_til_openai.py` | HF-format → OpenAI-whisper-sjekkpunkt (streng innlasting, festet til commit) |
| `export-onnx.py` | sherpa-onnx v1.13.8 sitt eksportskript, med `--checkpoint` og `n_mels` fra modellen |
| `test_modell.py` | Kongens tale (25 s): sherpa int8 mot OpenAI fp32, stopper under 0,6 ordlikhet |
| `manifest.py` | Bygger og fletter `norske-modeller.json` |
| `app/.../install/ExtraEngines.kt` | Leser listen, sjekker den og legger motorene til katalogen |
| `.github/workflows/apk-norsk.yml` | Bygger `app.marmalade.stt.norsk` (arm64, usignert) ved push |
| `signer.sh` | På Rafiki: henter siste APK, signerer med `.auth/norsk.keystore` og installerer på Pixelen |

Hugging Face er blokkert i AdGuard. Derfor skjer all nedlasting derfra i Actions, mens telefonen og
Rafiki bare henter fra GitHub.

## Legge til en modell

Kjør workflowen «Norske modeller» med repoet lagt til i listen, for eksempel
`NbAiLab/nb-whisper-medium`. Modeller med samme commit som før hoppes over. Bare Whisper-arkitektur
støttes av konverteringen. Appen godtar også NeMo-transducer og -CTC hvis noen lager slike filer.

```bash
gh workflow run norske-modeller.yml -R hakonamatata177/marmalade-stt-android \
  -f modeller="NbAiLab/nb-whisper-tiny NbAiLab/nb-whisper-base NbAiLab/nb-whisper-small NbAiLab/nb-whisper-medium"
```

## Oppdatere appen

```bash
git fetch upstream && git merge upstream/main   # nye versjoner fra Max
git push origin norsk                            # Actions bygger APK
tools/norsk/signer.sh                            # signer + installer på Pixelen
```

Nøkkelen i `.auth/` må ikke mistes. Med en ny nøkkel må appen avinstalleres, og modellene må lastes ned på nytt.
