import pandas as pd
import matplotlib.pyplot as plt
from supervenn import supervenn

all_prompts = False
p8_only = True

if all_prompts:
    # Define the CSV file path (update if needed)
    csv_file = "o3data.csv"

    # Read the CSV file into a pandas DataFrame.
    # Assumes the first column is the commit id and the remaining columns are prompt outputs (p1, p2, ..., p8)
    df = pd.read_csv(csv_file)
    # Reorder the DataFrame columns so that the prompt columns are sorted descendingly (P8, P7, ..., P1)
    first_col = df.columns[0]
    prompt_cols = sorted(df.columns[1:], key=lambda c: int(c[1:]), reverse=True)
    df = df[[first_col] + prompt_cols]

    # Get the commit id column name and prompt columns
    commit_col = df.columns[0]
    prompt_columns = df.columns[1:]

    # Define outcomes that are considered a "build success"
    success_outcomes = {"BUILD_SUCCESS", "DEPENDENCY_RESOLUTION_FAILURE", "WERROR_FAILURE"}

    # Create a dictionary mapping each prompt to the set of commit ids that resulted in a "success"
    prompt_sets = {
        prompt: set(df.loc[df[prompt].isin(success_outcomes), commit_col])
        for prompt in prompt_columns
    }

    # Create a mapping from each commit id to a unique numerical id
    commit_mapping = {}
    id=1
    for commit in df[commit_col].unique():
        print (commit, id)
        commit_mapping[commit] = id
        id+=1


    # Save the commit_mapping dictionary to a CSV file.
    mapping_df = pd.DataFrame(list(commit_mapping.items()), columns=["commit_id", "numeric_id"])
    mapping_df.to_csv("commit_mapping.csv", index=False)


    # Replace commit ids in prompt_sets with their corresponding numerical ids
    for prompt in prompt_sets:
        prompt_sets[prompt] = {commit_mapping[commit] for commit in prompt_sets[prompt]}

    # # Create custom labels for the prompts
    # prompt_names = {1: 'P1', 2: 'P2', 3: 'P3', 4: 'P4', 5: 'P5', 6: 'P6', 7: 'P7', 8: 'P8'}
    # labels = {prompt: prompt_names[i+1] for i, prompt in enumerate(prompt_sets.keys())}

    labels = {prompt: prompt for prompt in prompt_sets.keys()}
    plt.figure(figsize=(35, 8))
    supervenn(
        list(prompt_sets.values()),
        list(labels.values()),
        fontsize=30 # Increase font size
    )

    plt.title("Visualization of Build Success by o3-mini Across Prompts", fontsize=16)
    # Save the figure to a file (e.g., PNG format) before showing it.
    plt.savefig("o3venn_diagram.png", dpi=300, bbox_inches='tight')
    plt.show()

if p8_only:
    csv_file = "rq1_p8_data.csv"

    # Read the CSV file into a pandas DataFrame
    df = pd.read_csv(csv_file)

    # Define outcomes that are considered a "build success"
    success_outcomes = {"BUILD_SUCCESS", "DEPENDENCY_RESOLUTION_FAILURE", "WERROR_FAILURE"}

    # Get the commit id column name and LLM model columns
    commit_col = df.columns[0]
    llm_columns = df.columns[1:]

    llm_mapping = {
        "deepseek": "Deepseek V3",
        "gemini": "Gemini-2.0-flash",
        "gpt": "Gpt-4o-mini",
        "o3": "o3-mini",
        "qwen": "Qwen2.5-32b-instruct"
    }

    llm_colors = {
        "Deepseek V3": "#1f77b4",  # Blue
        "Gemini-2.0-flash": "#ff7f0e",  # Orange
        "Gpt-4o-mini": "#9467bd",  # Purple
        "o3-mini": "#2ca02c",  # Green
        "Qwen2.5-32b-instruct": "#d62728"  # Red
    }

    # Create a dictionary mapping each LLM model to the set of commit ids that resulted in a "success"
    llm_sets = {
        llm_mapping[llm.strip()]: set(df.loc[df[llm].isin(success_outcomes), commit_col])
        for llm in llm_columns
    }

    # Create custom labels for the LLM models
    labels = {llm: llm for llm in llm_sets.keys()}

    # Generate the Supervenn diagram with custom colors and increased font size
    plt.figure(figsize=(35, 8))
    supervenn(
        list(llm_sets.values()),
        list(labels.values()),
        color_cycle=[llm_colors[llm] for llm in llm_sets.keys()],
        fontsize=30  # Increase font size
    )

    plt.title("Visualization of Build Success by P8 Across LLM Models", fontsize=16)
    # Save the figure to a file (e.g., PNG format) before showing it.
    plt.savefig("p8_llm_venn_diagram.png", dpi=300, bbox_inches='tight')
    plt.show()