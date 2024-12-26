import argparse
from openai import OpenAI


def get_llm_response(prompt, api_key, organization):
    client = OpenAI(
        organization=organization,
        api_key=api_key
    )

    with open(prompt, "r") as f:
        promptFromFile = f.read()

    stream = client.chat.completions.create(
        model="gpt-4o-mini",
        temperature=0.7,
        max_tokens=16000,
        timeout=400,
        messages=[
            {"role": "user", "content": promptFromFile}],
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

    args = parser.parse_args()
    response = get_llm_response(args.prompt, api_key, organization)
    print(response)