# Jarvis Local Models

This directory holds quantized `.gguf` models for on-device inference on Android.

## Recommended Models for Mobile:

1. **SmolLM2 360M Instruct (Q4_K_M)**
   - File: `smollm2-360m-instruct-q4_k_m.gguf`
   - Size: ~229 MB
   - RAM required: ~450 MB
   - Strengths: Extremely fast response times, fits within low-end device constraints.
   - Download: `https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf`

2. **Qwen 2.5 0.5B Instruct (Q4_K_M)**
   - File: `qwen2.5-0.5b-instruct-q4_k_m.gguf`
   - Size: ~390 MB
   - RAM required: ~800 MB
   - Strengths: High tool calling precision and JSON schema adherence.
   - Download: `https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf`

3. **Llama 3.2 1B Instruct (Q4_K_M)**
   - File: `llama-3.2-1b-instruct-q4_k_m.gguf`
   - Size: ~800 MB
   - RAM required: ~1.5 GB
   - Strengths: High general intelligence and multi-step reasoning.
   - Download: `https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf`

## Deployment:
You can either:
- Download models directly inside the Jarvis app UI via the built-in `ModelManager`.
- Push a `.gguf` file to the device storage via ADB:
  ```bash
  adb push model/smollm2-360m-instruct-q4_k_m.gguf /sdcard/Download/
  # or into app data
  adb push model/smollm2-360m-instruct-q4_k_m.gguf /data/data/com.personalai.jarvis/files/models/
  ```
