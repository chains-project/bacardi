import os
from dotenv import load_dotenv

import requests


class LocalModel:
    def __init__(self, model_name, prompt):
        self.model_name = model_name
        self.prompt = prompt

        load_dotenv()

        self.base_url = os.getenv("LOCAL_LLM_BASE_URL", "http://localhost:8001/v1")
        self.temperature = float(os.getenv("LLM_TEMP", 0.0))
        # Leave MAX_TOKEN unset/blank to not cap generation at all (useful for
        # reasoning models, which can burn the whole budget on hidden "thinking"
        # tokens before ever writing the actual answer).
        max_token_env = os.getenv("MAX_TOKEN", "").strip()
        self.max_tokens = int(max_token_env) if max_token_env else None
        self.timeout = int(os.getenv("TIMEOUT", 1000))

    def _generate_response(self):
        payload = {
            "model": self.model_name,
            "messages": [{"role": "user", "content": self.prompt}],
            "temperature": self.temperature,
        }
        if self.max_tokens is not None:
            payload["max_tokens"] = self.max_tokens

        response = requests.post(
            url=f"{self.base_url}/chat/completions",
            json=payload,
            timeout=self.timeout,
        )
        response.raise_for_status()
        message = response.json()["choices"][0]["message"]
        # Reasoning models (e.g. Qwen3-style "thinking" models served by Ollama) put
        # their output in a separate "reasoning" field and can leave "content" empty
        # if the response is still mid-thought — fall back to it rather than
        # returning nothing.
        return message.get("content") or message.get("reasoning") or ""
