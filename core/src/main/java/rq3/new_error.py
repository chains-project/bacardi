import json
import matplotlib.pyplot as plt
import numpy as np

# Load the data from the JSON file
with open('/Users/frank/Documents/Work/PHD/bacardi/bacardi/result_rq3/total_errors.json') as f:
    data = json.load(f)

# Extract the prompts and models
prompts = list(data.keys())
models = list(next(iter(data.values())).keys())

# Configure colors for the models
model_colors = {
    "gpt-4o-mini": "#9467BD",
    "qwen-qwen2.5-32b-instruct": "#D62728",
    "gemini-2.0-flash-001": "#FF7F0E",
    "deepseek-deepseek-chat": "#1F77B4",
    "o3-mini-2025-01-31": "#2CA02C"
}

# Configure the width of the bars and their positions
bar_width = 0.15
x = np.arange(len(prompts))

# Create the figure and axes
plt.figure(figsize=(14, 8))
ax = plt.gca()

# Plot the data
for i, model in enumerate(models):
    llm_changes = [data[prompt][model]["llmChanges"] for prompt in prompts]
    breaking_changes = [data[prompt][model]["breakingChanges"] for prompt in prompts]

    # Position of the bars for this model
    bar_positions = x + i * bar_width

    # Plot stacked bars
    bars1 = ax.bar(bar_positions, llm_changes, bar_width, label=f"{model} - llmChanges", color=model_colors[model], alpha=0.7)
    bars2 = ax.bar(bar_positions, breaking_changes, bar_width, bottom=llm_changes, label=f"{model} - breakingChanges", color=model_colors[model], alpha=1.0)

    # Add text inside the bars
    for j, (llm, breaking) in enumerate(zip(llm_changes, breaking_changes)):
        ax.text(bar_positions[j], llm / 2, str(llm), ha='center', va='center', fontsize=9, color='black')  # Text for llmChanges
        ax.text(bar_positions[j], llm + breaking / 2, str(breaking), ha='center', va='center', fontsize=9, color='black')  # Text for breakingChanges

# Configure labels and legend
ax.set_xticks(x + (len(models) - 1) * bar_width / 2)
ax.set_xticklabels(prompts, rotation=45, ha="right", fontsize=10)
ax.set_ylabel("Number of errors", fontsize=12)
ax.set_xlabel("Prompts", fontsize=12)
ax.set_title("Errors by model and type for each prompt", fontsize=14)
ax.legend(loc="upper left", bbox_to_anchor=(1, 1), fontsize=10)

# Adjust layout and display the plot
plt.tight_layout()
plt.show()