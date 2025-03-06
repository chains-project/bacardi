import os
from typing import Any

import requests
import json

from dotenv import load_dotenv


class OpenRouterModels:
    def __init__(self, model_name, prompt):
        self.model_name = model_name
        self.prompt = prompt
        self.include_reasoning = True

        load_dotenv()
        self.provider = os.getenv("OPENROUTER_PROVIDER", None)
        self.provider_args = {
            "allow_fallbacks": False,
        }
        if self.provider:
            self.provider_args["order"] = [self.provider]
        self.temperature = float(os.getenv("LLM_TEMP", 0.0))
        self.api_key = os.getenv("OPENROUTER_API_KEY")
        self.openrouter_api_key = os.getenv("OPENROUTER_API_KEY")

    def _generate_response(self):
        model_client = {
            "model": self.model_name,
            "messages": [{"role": "user", "content": self.prompt}],
            "temperature": self.temperature,
            "include_reasoning": self.include_reasoning,
            "provider": self.provider_args,
        }

        response = requests.post(
            url="https://openrouter.ai/api/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {self.openrouter_api_key}",
                # Shows in rankings on openrouter.ai.
                "X-Title": f"bacardi",
            },
            timeout=600,
            data=json.dumps(model_client),
        )

        response = response.json()

        if "error" in response:
            raise Exception(response["error"])

        return response

    @staticmethod
    def token_cost(response: Any, model_name: str):
        # This is a placeholder for the actual cost calculation

        __COST_PER_MILLION_TOKENS = {
            "deepseek-v2.5": {
                "prompt": 2,
                "completion": 2,
            },
            "deepseek-v3": {
                "prompt": 0.14,
                "completion": 0.28,
            },
            "deepseek/deepseek-chat:free": {
                "prompt": 0.14,
                "completion": 0.28,
            },
            "deepseek/deepseek-chat": {
                "prompt": 0.14,
                "completion": 0.28,
            },
            "qwen/qwen2.5-32b-instruct": {
                "prompt": 0.79,
                "completion": 0.79,
            },
        }

        usage = {
            "prompt_tokens": 0,
            "completion_tokens": 0,
            "total_tokens": 0,
            "prompt_cost": 0.0,
            "completion_cost": 0.0,
            "total_cost": 0.0,
        }

        prompt_token_count = response["usage"]["prompt_tokens"]
        completion_token_count = response["usage"]["completion_tokens"]

        # Update token counts
        usage["prompt_tokens"] += prompt_token_count
        usage["completion_tokens"] += completion_token_count

        # Calculate costs
        prompt_cost = __COST_PER_MILLION_TOKENS[
            model_name
        ]["prompt"]
        completion_cost = __COST_PER_MILLION_TOKENS[
            model_name
        ]["completion"]

        usage["prompt_cost"] += prompt_cost * prompt_token_count / 1000000
        usage["completion_cost"] += (
                completion_cost * completion_token_count / 1000000
        )

        usage["completion_cost"] += (
                completion_cost * completion_token_count / 1000000
        )

        usage["total_tokens"] = usage["prompt_tokens"] + usage["completion_tokens"]
        usage["total_cost"] = usage["prompt_cost"] + usage["completion_cost"]
        return usage
