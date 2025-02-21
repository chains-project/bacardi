from dotenv import load_dotenv
from typing import Any, List

import os
import anthropic


class AnthropicModel:
    def __init__(self, model_name: str, prompt) -> None:
        self.model_name = model_name
        self.prompt = prompt

        load_dotenv()
        self.max_tokens = min(int(os.getenv("MAX_TOKEN")), 8192)
        self.temperature = float(os.getenv("LLM_TEMP", 0.0))

        self.client = anthropic.Anthropic(api_key=os.getenv("ANTHROPIC_API_KEY"))

    def _generate_response(self) -> Any:
        result = ""

        completion = self.client.messages.create(
            model=self.model_name,
            max_tokens=self.max_tokens,
            messages=[{"role": "user", "content": self.prompt}],
            temperature=self.temperature,
        )
        if completion:
            result = completion.content[0].text
        else:
            result = completion

        return result

    def _token_cost(dict_response: dict) -> Any:


        return dict_response["cost"]
