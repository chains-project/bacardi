from dotenv import load_dotenv
from typing import Any, List

import os
import google.generativeai as genai

def extract_text(response) -> str:
    return response['candidates'][0]['content']['parts'][0]['text']


class GoogleModels:
    def __init__(self, model_name: str, prompt: str) -> None:
        self.model_name = model_name
        self.model = genai.GenerativeModel(self.model_name)
        self.prompt = prompt

        load_dotenv()

        self.temperature = float(os.getenv("LLM_TEMP", 0.0))
        self.api_key = os.getenv("GOOGLE_API_KEY")
        genai.configure(api_key=self.api_key)

    def __get_config(self):
        return genai.types.GenerationConfig(
            temperature=self.temperature,
        )

    def _generate_response(self):
        completion = self.model.generate_content(
            self.prompt, generation_config=self.__get_config()
        )
        return completion.to_dict()

    @staticmethod
    def token_cost(response: dict, model_name: str) -> Any:

        __COST_PER_MILLION_TOKENS = {
            "gemini-1.5-pro-001": {
                "prompt": 3.50,
                "completion": 10.50,
            },
            "gemini-1.5-pro-002": {
                "prompt": 1.25,
                "completion": 5.00,
            },
            "gemini-2.0-flash-001": {
                "prompt": 0.1,
                "completion": 0.4,
            },
        }

        __COST_PER_MILLION_TOKENS_OVER_128K = {
            "gemini-1.5-pro-001": {
                "prompt": 7.00,
                "completion": 21.00,
            },
            "gemini-1.5-pro-002": {
                "prompt": 2.50,
                "completion": 10.00,
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

        if "usage_metadata" not in response:
            return usage

        prompt_token_count = response["usage_metadata"][
            "prompt_token_count"
        ]
        completion_token_count = response["usage_metadata"][
            "candidates_token_count"
        ]

        # Update token counts
        usage["prompt_tokens"] += prompt_token_count
        usage["completion_tokens"] += completion_token_count

        # Determine cost rates based on token count
        if (
                prompt_token_count > 128000
                and model_name
                in __COST_PER_MILLION_TOKENS_OVER_128K
        ):
            prompt_cost = (
                __COST_PER_MILLION_TOKENS_OVER_128K[
                    model_name
                ]["prompt"]
            )
            completion_cost = (
                __COST_PER_MILLION_TOKENS_OVER_128K[
                    model_name
                ]["completion"]
            )
        else:
            prompt_cost = __COST_PER_MILLION_TOKENS[
                model_name
            ]["prompt"]
            completion_cost = __COST_PER_MILLION_TOKENS[
                model_name
            ]["completion"]

        # Calculate costs
        usage["prompt_cost"] += prompt_cost * prompt_token_count / 1000000
        usage["completion_cost"] += (
                completion_cost * completion_token_count / 1000000
        )

        usage["total_tokens"] = usage["prompt_tokens"] + usage["completion_tokens"]
        usage["total_cost"] = usage["prompt_cost"] + usage["completion_cost"]
        return usage
