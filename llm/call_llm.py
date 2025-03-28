import argparse
import json
import os
from enum import Enum
from dotenv import load_dotenv

from models.gemini import GoogleModels, extract_text
from models.openai import OpenAiModel
from models.anthropic import AnthropicModel
from models.openrouter import OpenRouterModels


class LLMType(Enum):
    OPENAI = "openai"
    ANTHROPIC = "anthropic"
    GEMINI = "google"
    OPENROUTE = "openrouter"

    def __str__(self):
        return self.value

    def get_model(self, prompt, file_path):
        return LLMResolver.get_model(self, prompt, file_path)


def init_gpt4(prompt, file_path):
    model_response = OpenAiModel(
        os.getenv("LLM"),
        prompt
    )
    return model_response._generate_response()


def init_anthropic(prompt, file_path):
    model_response = AnthropicModel(
        os.getenv("LLM"),
        prompt
    )
    return model_response._generate_response()


def init_gemini(prompt, file_path):
    model_response = GoogleModels(os.getenv("LLM"), prompt)
    _result = model_response._generate_response()
    text = extract_text(_result)
    cost = model_response.token_cost(_result, os.getenv("LLM"))
    # Instantiate LLMResolver so that update_costs gets self with cost_data and file_path initialized
    resolver = LLMResolver()
    cost['file_path'] = file_path
    update_costs(resolver, os.getenv("LLM"), cost)

    return text


def init_openroute(prompt, file_path):
    model_response = OpenRouterModels(os.getenv("LLM"), prompt)
    _result = model_response._generate_response()
    text = _result['choices'][0]['message']['content']
    cost = model_response.token_cost(_result, os.getenv("LLM"))
    # Instantiate LLMResolver so that update_costs gets self with cost_data and file_path initialized
    resolver = LLMResolver()
    cost['file_path'] = file_path
    update_costs(resolver, os.getenv("LLM"), cost)
    return text


def get_llm_response(prompt, _llm_type: LLMType):
    file_prompt_path = os.path.relpath(prompt, start=os.getcwd())
    load_dotenv()

    with open(prompt, "r") as f:
        prompt_from_file = f.read()

    return _llm_type.get_model(prompt_from_file, file_prompt_path)


def update_costs(self, model_name, new_cost_data):
    if model_name not in self.cost_data:
        self.cost_data[model_name] = {
            'prompt_tokens': 0,
            'completion_tokens': 0,
            'total_tokens': 0,
            'prompt_cost': 0.0,
            'completion_cost': 0.0,
            'total_cost': 0.0,
            'results': []
        }

    model_costs = self.cost_data[model_name]

    # Update the costs
    model_costs['prompt_tokens'] += new_cost_data['prompt_tokens']
    model_costs['completion_tokens'] += new_cost_data['completion_tokens']
    model_costs['total_tokens'] += new_cost_data['total_tokens']
    model_costs['prompt_cost'] += new_cost_data['prompt_cost']
    model_costs['completion_cost'] += new_cost_data['completion_cost']
    model_costs['total_cost'] += new_cost_data['total_cost']

    # Append the new cost data to the results array, including the file_path
    # new_cost_data['file_path'] = os.path.basename(self.file_path)
    model_costs['results'].append(new_cost_data)

    # Write the updated cost data back to the JSON file
    with open(self.file_path, 'w') as cost_file:
        json.dump(self.cost_data, cost_file, indent=4)


class LLMResolver:
    load_dotenv()
    model = os.getenv("LLM").replace("/", "_")
    prompt = os.getenv("PIPELINE")
    _file_path = f'costs_{model}_{prompt}.json'

    def __init__(self, file_path=f'costs_{model}_{prompt}.json'):
        self.file_path = file_path

        # Check if file exists, if not, create it with an empty JSON object
        if not os.path.exists(self.file_path):
            with open(self.file_path, 'w') as cost_file:
                json.dump({}, cost_file)

        with open(self.file_path, 'r') as cost_file:
            self.cost_data = json.load(cost_file)

    @staticmethod
    def get_model(for_type: LLMType, prompt, file_path):
        definitions = {
            LLMType.OPENAI: init_gpt4,
            LLMType.ANTHROPIC: init_anthropic,
            LLMType.GEMINI: init_gemini,
            LLMType.OPENROUTE: init_openroute
        }

        return definitions[for_type](prompt, file_path)


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
