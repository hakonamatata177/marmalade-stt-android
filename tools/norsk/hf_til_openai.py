#!/usr/bin/env python3
"""Gjør en Whisper-modell i Hugging Face-format (f.eks. NbAiLab/nb-whisper-small)
om til et OpenAI-whisper-sjekkpunkt (.pt), som sherpa-onnx' eksportskript laster.

Henter bare config.json og vektene, festet til repoets nåværende commit, og
skriver commit-en og litt annet til --info (JSON) så manifestet kan oppgi kilden.
Sjekkpunktet lastes inn i whisper.model.Whisper med strict=True, så en nøkkel
som mangler eller er til overs stopper kjøringen.
"""

import argparse
import json
import re
from pathlib import Path

import torch
from huggingface_hub import HfApi, hf_hub_download
from whisper.model import ModelDimensions, Whisper

# HF-navn → OpenAI-navn. Rekkefølgen betyr noe: hele navn først, så delnavn.
FULL = [
    (r"^encoder\.embed_positions\.weight$", "encoder.positional_embedding"),
    (r"^decoder\.embed_positions\.weight$", "decoder.positional_embedding"),
    (r"^decoder\.embed_tokens\.", "decoder.token_embedding."),
    (r"^encoder\.layer_norm\.", "encoder.ln_post."),
    (r"^decoder\.layer_norm\.", "decoder.ln."),
]
PARTS = [
    (".layers.", ".blocks."),
    (".self_attn_layer_norm.", ".attn_ln."),
    (".encoder_attn_layer_norm.", ".cross_attn_ln."),
    (".final_layer_norm.", ".mlp_ln."),
    (".self_attn.q_proj.", ".attn.query."),
    (".self_attn.k_proj.", ".attn.key."),
    (".self_attn.v_proj.", ".attn.value."),
    (".self_attn.out_proj.", ".attn.out."),
    (".encoder_attn.q_proj.", ".cross_attn.query."),
    (".encoder_attn.k_proj.", ".cross_attn.key."),
    (".encoder_attn.v_proj.", ".cross_attn.value."),
    (".encoder_attn.out_proj.", ".cross_attn.out."),
    (".fc1.", ".mlp.0."),
    (".fc2.", ".mlp.2."),
]


def openai_key(hf_key: str):
    k = hf_key.removeprefix("model.")
    if k == "proj_out.weight":  # bundet til token_embedding i OpenAI-formatet
        return None
    for pattern, repl in FULL:
        k = re.sub(pattern, repl, k)
    for old, new in PARTS:
        k = k.replace(old, new)
    return k


def load_hf_weights(repo: str, rev: str, files: set):
    if "model.safetensors" in files:
        from safetensors.torch import load_file

        return load_file(hf_hub_download(repo, "model.safetensors", revision=rev))
    if "pytorch_model.bin" in files:
        return torch.load(hf_hub_download(repo, "pytorch_model.bin", revision=rev), map_location="cpu")
    raise SystemExit(f"{repo}: fant verken model.safetensors eller pytorch_model.bin")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--info", required=True)
    a = ap.parse_args()

    info = HfApi().model_info(a.repo)
    rev = info.sha
    files = {s.rfilename for s in info.siblings}
    cfg = json.loads(Path(hf_hub_download(a.repo, "config.json", revision=rev)).read_text())

    hf = load_hf_weights(a.repo, rev, files)
    sd = {}
    for k, v in hf.items():
        nk = openai_key(k)
        if nk is not None:
            sd[nk] = v.float() if v.is_floating_point() else v

    dims = dict(
        n_mels=cfg["num_mel_bins"],
        n_vocab=cfg["vocab_size"],
        n_audio_ctx=cfg["max_source_positions"],
        n_audio_state=cfg["d_model"],
        n_audio_head=cfg["encoder_attention_heads"],
        n_audio_layer=cfg["encoder_layers"],
        n_text_ctx=cfg["max_target_positions"],
        n_text_state=cfg["d_model"],
        n_text_head=cfg["decoder_attention_heads"],
        n_text_layer=cfg["decoder_layers"],
    )
    model = Whisper(ModelDimensions(**dims))
    model.load_state_dict(sd, strict=True)

    torch.save({"dims": dims, "model_state_dict": sd}, a.out)
    Path(a.info).write_text(
        json.dumps(
            {
                "repo": a.repo,
                "revision": rev,
                "license": getattr(info.card_data, "license", None),
                "dims": dims,
                "parameters": sum(p.numel() for p in model.parameters()),
            },
            indent=2,
        )
    )
    print(f"{a.repo}@{rev}: {len(sd)} tensorer, {dims}")


if __name__ == "__main__":
    main()
