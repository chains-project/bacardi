from enum import Enum
import os
from langchain_core.language_models import BaseChatModel
from langchain_google_vertexai import ChatVertexAI, VertexAI
from langchain_groq import ChatGroq
from langchain_openai import ChatOpenAI, OpenAI

import vertexai


class LLMType(Enum):
    GEMINI = "gemini"
    GPT4 = "gpt4"
    LLAMA = "llama"
    MIXTRAL = "mixtral"

    def __str__(self):
        return self.value

    def get_model(self) -> BaseChatModel:
        return LLMResolver.get_model(self)


class LLMResolver:
    @staticmethod
    def get_model(for_type: LLMType) -> BaseChatModel:
        definitions = {
            LLMType.GEMINI: LLMResolver.init_gemini,
            LLMType.GPT4: LLMResolver.init_gpt4,
            LLMType.LLAMA: LLMResolver.init_llama,
            LLMType.MIXTRAL: LLMResolver.init_mixtral
        }

        return definitions[for_type]()

    @staticmethod
    def init_gemini() -> BaseChatModel:
        vertexai.init(location=os.getenv("GOOGLE_CLOUD_REGION"))
        return ChatVertexAI(
            model_name="gemini-pro",
            temperature=os.getenv("LLM_TEMPERATURE", 0.5),
            top_p=os.getenv("LLM_TOP_P", 1.0),
        )

    @staticmethod
    def init_gpt4() -> BaseChatModel:
        return ChatOpenAI(
            model_name="gpt-4",
            temperature=os.getenv("LLM_TEMPERATURE", 0.5),
            model_kwargs={"top_p": os.getenv("LLM_TOP_P", 1.0)},
        )

    @staticmethod
    def init_llama() -> BaseChatModel:
        return ChatGroq(
            model_name="llama3-70b-8192",
            temperature=os.getenv("LLM_TEMPERATURE", 0.5),
            model_kwargs={"top_p": os.getenv("LLM_TOP_P", 1.0)}
        )

    @staticmethod
    def init_mixtral() -> BaseChatModel:
        return ChatGroq(
            model_name="mixtral-8x7b-32768",
            temperature=os.getenv("LLM_TEMPERATURE", 0.5),
            model_kwargs={"top_p": os.getenv("LLM_TOP_P", 1.0)}
        )
