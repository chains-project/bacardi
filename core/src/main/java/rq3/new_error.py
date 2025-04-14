import json
import matplotlib.pyplot as plt
import numpy as np

# Load the data from the JSON file
with open('/Users/mam10532/Documents/GitHub/bacardi/result_rq3/total_errors.json') as f:
    data = json.load(f)

# Configure colors for the models
model_colors = {
    "gpt-4o-mini": "#9467BD",
    "qwen-qwen2.5-32b-instruct": "#D62728",
    "gemini-2.0-flash-001": "#FF7F0E",
    "deepseek-deepseek-chat": "#1F77B4",
    "o3-mini-2025-01-31": "#2CA02C"
}
prompt_aliases = {
    "results_baseline_buggy_line": "P2",
    "results_baseline_api_diff_cot": "P7",
    "results_baseline_cot": "P5",
    "results_baseline": "P1",
    "results_baseline_buggy_line_api_diff": "P4",
    "results_baseline_api_diff": "P3",
    "results_baseline_cot_buggy_line": "P6",
    "results_baseline_api_diff_cot_buggy": "P8"
}
llm_mapping = {
    "deepseek-deepseek-chat": "Deepseek V3",
    "gemini-2.0-flash-001": "Gemini-2.0-flash",
    "gpt-4o-mini": "Gpt-4o-mini",
    "o3-mini-2025-01-31": "o3-mini",
    "qwen-qwen2.5-32b-instruct": "Qwen2.5-32b-instruct"
}



# Map prompt names to their aliases
data = {prompt_aliases.get(prompt, prompt): models for prompt, models in data.items()}

# Order prompts from P1 to P8
ordered_prompts = ["P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8"]
data = {prompt: data[prompt] for prompt in ordered_prompts if prompt in data}

# Extract the ordered prompts and models
prompts = list(data.keys())
models = list(next(iter(data.values())).keys())

# Configure the width of the bars and their positions
bar_width = 0.15
x = np.arange(len(prompts))

# Create the figure and axes
plt.figure(figsize=(14, 8))
ax = plt.gca()

# Plot the data
for i, model in enumerate(models):
    llm_changes = []
    breaking_changes = []
    errors=[]
    for prompt in prompts:
        #total_errors = data[prompt][model]["llmChanges"] + data[prompt][model]["breakingChanges"]
        #errors.append(total_errors)
        llm_changes.append(data[prompt][model]["llmChanges"])
        breaking_changes.append(data[prompt][model]["breakingChanges"])

    # Position of the bars for this model
    bar_positions = x + i * bar_width

    # Plot stacked bars
    bars1 = ax.bar(bar_positions, llm_changes, bar_width, label=f"{llm_mapping[model]} - llmChanges", color=model_colors[model], alpha=0.7)
    bars2 = ax.bar(bar_positions, breaking_changes, bar_width, bottom=llm_changes, label=f"{llm_mapping[model]} - breakingChanges", color=model_colors[model], alpha=1.0)

    # Add text inside the bars with vertical rotation
    for j, (llm, breaking) in enumerate(zip(llm_changes, breaking_changes)):
        #set bar height to be that of error 
        llm_percentage = llm / (llm + breaking) * 100
        breaking_percentage = breaking / (llm + breaking) * 100
        ax.text(bar_positions[j], llm / 2, f"{llm_percentage:.1f}%", ha='center', va='center', fontsize=9, color='black', rotation=90)  # Text for llmChanges
        ax.text(bar_positions[j], llm + breaking / 2, f"{breaking_percentage:.1f}%", ha='center', va='center', fontsize=9, color='black', rotation=90)  # Text for breakingChanges

# Configure labels and legend
ax.set_xticks(x + (len(models) - 1) * bar_width / 2)
ax.set_xticklabels(prompts, rotation=45, ha="right", fontsize=10)
ax.set_ylabel("Number of Errors After LLM Changes", fontsize=12)
ax.set_xlabel("Prompts", fontsize=12)
#ax.set_title("Percentage of Errors Caused by LLM Changes VS Breaking Changes for Each Prompt", fontsize=14)
ax.legend(loc="upper left", bbox_to_anchor=(1, 1), fontsize=10)

# Adjust layout and save the plot
plt.tight_layout()
plt.savefig('/Users/mam10532/Documents/GitHub/bacardi/result_rq3/ordered_error_plot.png', dpi=300)
plt.show()