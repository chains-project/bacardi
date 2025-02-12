import os
from dotenv import load_dotenv

import openai


class OpenAiModel:
    def __init__(self, model_name, prompt):

        self.model_name = model_name
        self.prompt = prompt

        load_dotenv()

        self.temperature = float(os.getenv("LLM_TEMP", 0.0))
        openai.api_key = os.getenv("API_KEY")
        openai.organization = os.getenv("API_KEY_ORGANIZATION")
        self.reasoning_effort = "high"
        self.client = openai.OpenAI(api_key=openai.api_key)

    def _generate_response(self):
        response_text = ""

        if self.model_name.startswith("o"):
            completion = self.client.chat.completions.create(
                model=self.model_name,
                messages=[{"role": "user", "content": self.prompt}],
                reasoning_effort=self.reasoning_effort,
                timeout=int(os.getenv("TIMEOUT")),
            )
            response_text = completion.choices[0].message.content
        else:
            stream = self.client.chat.completions.create(
                model=self.model_name,
                messages=[{"role": "user", "content": self.prompt}],
                temperature=self.temperature,
                max_tokens=int(os.getenv("MAX_TOKEN")),
                timeout=int(os.getenv("TIMEOUT")),
                stream=True,  # Streaming activado
            )

            for chunk in stream:
                if chunk.choices and chunk.choices[0].delta.content:
                    response_text += chunk.choices[0].delta.content

        return response_text
