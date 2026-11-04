#!/usr/bin/env python3
"""Sjekker en eksportert modell: transkriber samme lydklipp med OpenAI-whisper
(fp32, sjekkpunktet fra hf_til_openai.py) og med sherpa-onnx (int8-filene appen
laster ned), og stopp hvis sherpa-teksten er tom eller for ulik referansen.
Fanger feil i nøkkeloversettelsen (søppeltekst) og i eksporten/kvantiseringen.
"""

import argparse
import difflib
import re
import sys
import wave

import numpy as np
import sherpa_onnx
import whisper


def read_wav(path):
    with wave.open(path) as f:
        assert f.getframerate() == 16000 and f.getnchannels() == 1 and f.getsampwidth() == 2
        return np.frombuffer(f.readframes(f.getnframes()), dtype=np.int16).astype(np.float32) / 32768


def words(text):
    return re.findall(r"\w+", text.lower())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pt", required=True)
    ap.add_argument("--encoder", required=True)
    ap.add_argument("--decoder", required=True)
    ap.add_argument("--tokens", required=True)
    ap.add_argument("--wav", required=True)
    ap.add_argument("--language", default="no")
    ap.add_argument("--min-likhet", type=float, default=0.6)
    a = ap.parse_args()

    samples = read_wav(a.wav)

    ref = whisper.load_model(a.pt, device="cpu").transcribe(
        a.wav, language=a.language, task="transcribe", fp16=False, temperature=0.0
    )["text"].strip()

    rec = sherpa_onnx.OfflineRecognizer.from_whisper(
        encoder=a.encoder, decoder=a.decoder, tokens=a.tokens,
        language=a.language, task="transcribe", num_threads=4, tail_paddings=1000,
    )
    stream = rec.create_stream()
    stream.accept_waveform(16000, samples)
    rec.decode_stream(stream)
    got = stream.result.text.strip()

    likhet = difflib.SequenceMatcher(None, words(ref), words(got)).ratio()
    print(f"referanse (fp32): {ref}")
    print(f"sherpa (int8):    {got}")
    print(f"ordlikhet: {likhet:.2f}")
    if not got or likhet < a.min_likhet:
        sys.exit(f"FEIL: sherpa-teksten er tom eller for ulik referansen ({likhet:.2f} < {a.min_likhet})")


if __name__ == "__main__":
    main()
