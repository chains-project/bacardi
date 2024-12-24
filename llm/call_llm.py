import argparse
from openai import OpenAI


def get_llm_response(prompt, api_key, organization, temperature=0.7):
    client = OpenAI(
        organization=organization,
        api_key=api_key
    )

    stream = client.chat.completions.create(
        model="gpt-4o-mini",
        temperature=temperature,
        max_tokens=16000,
        timeout=400,
        messages=[
            {"role": "user", "content": prompt}],
        stream=True,
    )

    response_text = ""
    for chunk in stream:
        if chunk.choices[0].delta.content is not None:
            response_text += chunk.choices[0].delta.content

    return response_text

if __name__ == "__main__":
    api_key = "sk-None-APHBjbapVsZJI436GiokT3BlbkFJjhWT9WJcegltDZt56m3p"
    organization = "org-8vaaikANoGLw18qMPf7FeuJm"
    parser = argparse.ArgumentParser(description="Get LLM response based on a prompt.")
    parser.add_argument("prompt", type=str, help="The prompt to send to the LLM.")
    parser.add_argument("--temperature", type=float, default=0.7, help="The temperature for the LLM response.")

    args = parser.parse_args()

    response = get_llm_response(args.prompt, api_key, organization, args.temperature)
    print(response)