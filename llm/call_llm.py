import argparse
import os
from enum import Enum
from dotenv import load_dotenv
from models.openai import OpenAiModel
from models.anthropic import AnthropicModel


class LLMType(Enum):
    OPENAI = "openai"
    ANTHROPIC = "anthropic"

    def __str__(self):
        return self.value

    def get_model(self, prompt):
        return LLMResolver.get_model(self, prompt)


class LLMResolver:

    @staticmethod
    def get_model(for_type: LLMType, prompt):
        definitions = {
            LLMType.OPENAI: init_gpt4,
            LLMType.ANTHROPIC: init_anthropic
        }

        return definitions[for_type](prompt)


def init_gpt4(prompt):
    model_response = OpenAiModel(
        os.getenv("LLM"),
        prompt
    )
    return model_response._generate_response()


def init_anthropic(prompt):
    model_response = AnthropicModel(
        os.getenv("LLM"),
        prompt
    )
    return model_response._generate_response()


def get_llm_response(prompt, llm_type: LLMType):
    load_dotenv()

    with open(prompt, "r") as f:
        prompt_from_file = f.read()

    return llm_type.get_model(prompt_from_file)


if __name__ == "__main__":
    load_dotenv()
    api_key = os.getenv("API_KEY")
    organization = os.getenv("API_KEY_ORGANIZATION")
    llm_type_str = os.getenv("LLM_TYPE")
    llm_type = LLMType(llm_type_str)

    parser = argparse.ArgumentParser(description="Get LLM response based on a prompt.")
    parser.add_argument("prompt", type=str, help="The prompt to send to the LLM.")

    args = parser.parse_args()
    response = get_llm_response(args.prompt, llm_type)
    print(response)
