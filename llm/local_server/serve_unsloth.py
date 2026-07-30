"""
Standalone local inference server for Unsloth-quantized (4-bit) models.

Bacardi's `call_llm.py` shells out to a fresh `python3` process for every prompt it
sends, which is fine for cloud providers (a cheap HTTP call) but far too slow for a
locally-hosted model — reloading multi-GB 4-bit weights onto the GPU on every call is not
practical. This script loads the model once and keeps it resident in memory, exposing a
minimal OpenAI-style `/v1/chat/completions` endpoint that `llm/models/local.py` talks to.

Usage:
    python3 llm/local_server/serve_unsloth.py --model unsloth/gemma-3-12b-it-unsloth-bnb-4bit --port 8001

Then in `.env`:
    LLM_TYPE=local
    LLM=unsloth/gemma-3-12b-it-unsloth-bnb-4bit
    LOCAL_LLM_BASE_URL=http://localhost:8001/v1

Requires: unsloth, torch, transformers, fastapi, uvicorn (only on the machine running
this server — not required by the rest of the Bacardi pipeline).
"""

import argparse

from fastapi import FastAPI
from pydantic import BaseModel
from unsloth import FastModel

app = FastAPI()
model = None
tokenizer = None
max_seq_length = 35000


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatCompletionRequest(BaseModel):
    messages: list[ChatMessage]
    temperature: float = 0.0
    max_tokens: int = 4000


@app.post("/v1/chat/completions")
def chat_completions(request: ChatCompletionRequest):
    text = tokenizer.apply_chat_template(
        [message.model_dump() for message in request.messages],
        add_generation_prompt=True,
        tokenize=False,
    )
    inputs = tokenizer(text, return_tensors="pt").to(model.device)
    input_len = inputs["input_ids"].shape[-1]

    output_ids = model.generate(
        **inputs,
        max_new_tokens=request.max_tokens,
        do_sample=request.temperature > 0.0,
        temperature=request.temperature if request.temperature > 0.0 else None,
    )
    completion = tokenizer.decode(output_ids[0][input_len:], skip_special_tokens=True)

    return {"choices": [{"message": {"role": "assistant", "content": completion}}]}


def main():
    parser = argparse.ArgumentParser(description="Serve an Unsloth 4-bit model over a local OpenAI-style API.")
    parser.add_argument("--model", "-m", required=True, help="Unsloth model name or local checkpoint path")
    parser.add_argument("--port", "-p", type=int, default=8001)
    parser.add_argument("--host", default="127.0.0.1")
    args = parser.parse_args()

    global model, tokenizer
    print(f"Loading {args.model} (4-bit) ...")
    model, tokenizer = FastModel.from_pretrained(
        model_name=args.model,
        max_seq_length=max_seq_length,
        load_in_4bit=True,
        load_in_8bit=False,
        full_finetuning=False,
    )
    print(f"Model loaded, serving on http://{args.host}:{args.port}/v1/chat/completions")

    import uvicorn
    uvicorn.run(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
