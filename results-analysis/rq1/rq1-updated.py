import pandas as pd
import numpy as np
import os
import matplotlib.pyplot as plt

output_csv= False
output_excel= False
output_latex= False
output_build_success_latex= False
outupt_other_cat_plot=False
output_sep_cat_plot=True

input_file = "rq1_data.csv"
output_dir = "rq1-final-output"

# Check if the output directory exists, if not create it
if not os.path.exists(output_dir):
    os.makedirs(output_dir)
    print(f"Created output directory: {output_dir}")
else:
    print(f"Output directory already exists: {output_dir}")

csv_file = output_dir+"/rq1_by_llm.csv"
excel_file = output_dir+"/rq1_by_llm.xlsx"
latex_file = output_dir+"/rq1_by_llm.tex"
build_success_latex_file = output_dir+"/build_success_rq1.tex"
other_cat_plot = output_dir+"/other_cat_plot.png"

prompts={("$P_1$","results_baseline"),("$P_2$","results_baseline_buggy_line"),("$P_3$","results_baseline_api_diff"), ("$P_4$","results_baseline_buggy_line_api_diff"),("$P_5$","results_baseline_cot"), ("$P_6$","results_baseline_cot_buggy_line"), ("$P_7$","results_baseline_api_diff_cot"), ("$P_8$","results_baseline_api_diff_cot_buggy")}

df = pd.read_csv(input_file)

# --------------------------------------------------
# Count the total number of commits (rows)
total_commits = len(df)
print(f"Total commits: {total_commits}")
# --------------------------------------------------

# 2. Drop the Commit column
df_no_commit = df.drop(columns=["Commit"])

# 3. Prepare a dictionary to collect counts for each (LLM, Prompt) pair
counts_dict = {}

# Iterate over each column (which encodes prompt variant & LLM)
for col in df_no_commit.columns:
    # e.g. "results_baseline_api_diff_cot_buggy - gemini"
    prompt_part, llm_part = col.split(" - ", 1)

    # 4. Get the distribution of status values for this column
    value_counts = df_no_commit[col].value_counts()

    # Find the matching tuple in prompts and replace prompt_part
    for prompt_tuple in prompts:
        if prompt_part == prompt_tuple[1]:
            prompt_part = prompt_tuple[0]
            break

    # Store in a nested dict keyed by (LLM, Prompt)
    counts_dict[(llm_part, prompt_part)] = value_counts

# 5. Create a DataFrame where columns is a MultiIndex: [LLM, Prompt]
counts_df = pd.DataFrame(counts_dict).fillna(0).astype(int)

# (Optional) Sort columns so LLMs are grouped alphabetically
counts_df = counts_df.sort_index(axis=1)

# 6. Save the table to CSV, Excel, LaTeX (conditioned on flags)
if output_csv:
    counts_df.to_csv(csv_file, index=True)
    print(f"CSV file saved to {csv_file}")

if output_excel:
    counts_df.to_excel(excel_file, index=True)
    print(f"Excel file saved to {excel_file}")

if output_latex:
    # 7. Save the entire table to a LaTeX file
    counts_df.to_latex(
        latex_file,
        index=True,  # Include index (status categories)
        multirow=True,  # Combine repeated row labels
        multicolumn=True,  # Combine repeated column labels
        caption="Status category counts broken down by LLM and prompt.",
        label="tab:counts-llm-prompt"
    )
    print(f"LaTeX table saved to {latex_file}")

#if recreating the table for the overleaf table, generate the file and only copy the values part as some of the table format in overleaf where edited manually
if output_build_success_latex:
    # 1. Filter to get only the BUILD_SUCCESS row (still numeric at this point)
    counts_build_success = counts_df.loc[["BUILD_SUCCESS"]]

    # 2. Pivot so that Prompts are rows and LLMs are columns.
    pivoted = counts_build_success.stack(level=1, future_stack=True)  # Moves Prompt into the row index
    pivoted.index = pivoted.index.droplevel(0)     # Drop BUILD_SUCCESS from the row index
    pivoted = pivoted.astype(object)               # Convert to object so we can store strings

    # 3. Format each cell as "count/total_commits (XX.XX%)"
    for prompt in pivoted.index:
        for llm in pivoted.columns:
            count_val = pivoted.loc[prompt, llm]
            percentage = (count_val / total_commits) * 100
            pivoted.loc[prompt, llm] = f"{count_val}\\{total_commits} ({percentage:.2f}\\%)"

    # 4. Generate LaTeX code, then insert \rowcolors
    latex_str = pivoted.to_latex(
        index=True,
        caption="Effectiveness of \\toolname~ on Compilation Failures at the build level",
        label="tab:build_success_prompt"
    )
    # Insert row coloring immediately after \begin{tabular}
    latex_str = latex_str.replace("\\begin{tabular}", "\\rowcolors{2}{gray!10}{white}\n\\begin{tabular}")

    # Write the final LaTeX string to file
    with open(build_success_latex_file, "w") as f:
        f.write(latex_str)

    print(f"LaTeX table (BUILD_SUCCESS only, Prompt rows) with row coloring saved to {build_success_latex_file}")

if outupt_other_cat_plot:
     # Make a copy of counts_df to process for plotting.
    plot_df = counts_df.copy()

    # Combine "NOT_REPAIRED" into "COMPILATION_FAILURE" if present.
    if "COMPILATION_FAILURE" in plot_df.index:
        if "NOT_REPAIRED" in plot_df.index:
            plot_df.loc["COMPILATION_FAILURE"] = plot_df.loc["COMPILATION_FAILURE"] + plot_df.loc["NOT_REPAIRED"]
            plot_df = plot_df.drop("NOT_REPAIRED")
    elif "NOT_REPAIRED" in plot_df.index:
        plot_df = plot_df.rename(index={"NOT_REPAIRED": "COMPILATION_FAILURE"})

    # Exclude BUILD_SUCCESS from the plot.
    if "BUILD_SUCCESS" in plot_df.index:
        plot_df = plot_df.drop("BUILD_SUCCESS")

    # Create new x-axis labels from the column keys (which are tuples: (llm, prompt))
    new_labels = []
    for col in plot_df.columns:
        if isinstance(col, tuple):
            new_labels.append(f"{col[0]} - {col[1]}")
        else:
            new_labels.append(str(col))

    # Create the grouped bar chart.
    x = np.arange(len(plot_df.columns))
    num_categories = len(plot_df.index)
    width = 0.15

    fig, ax = plt.subplots(figsize=(12, 6))

    for i, cat in enumerate(plot_df.index):
        ax.bar(x + i * width, plot_df.loc[cat].values, width, label=cat)

    ax.set_xticks(x + width * (num_categories - 1) / 2)
    ax.set_xticklabels(new_labels, rotation=45, ha='right')
    ax.set_ylabel("Count")
    ax.set_title("Error Categories (excluding BUILD_SUCCESS) by LLM and Prompt")
    ax.legend()

    plt.tight_layout()
    plt.show()
    # Save the plot to a file
    plt.savefig(other_cat_plot)
    print(f"Plot saved to {other_cat_plot}")

  

if output_sep_cat_plot:
    # Create a copy of counts_df for per-category plots.
    sep_plot_df = counts_df.copy()
    
    # Lump "NOT_REPAIRED" and "ERROR_MODEL_RESPONSE" into "COMPILATION_FAILURE"
    if "COMPILATION_FAILURE" in sep_plot_df.index:
        if "NOT_REPAIRED" in sep_plot_df.index:
            sep_plot_df.loc["COMPILATION_FAILURE"] = sep_plot_df.loc["COMPILATION_FAILURE"] + sep_plot_df.loc["NOT_REPAIRED"]
            sep_plot_df = sep_plot_df.drop("NOT_REPAIRED")
        if "ERROR_MODEL_RESPONSE" in sep_plot_df.index:
            sep_plot_df.loc["COMPILATION_FAILURE"] = sep_plot_df.loc["COMPILATION_FAILURE"] + sep_plot_df.loc["ERROR_MODEL_RESPONSE"]
            sep_plot_df = sep_plot_df.drop("ERROR_MODEL_RESPONSE")
    elif "NOT_REPAIRED" in sep_plot_df.index:
        sep_plot_df = sep_plot_df.rename(index={"NOT_REPAIRED": "COMPILATION_FAILURE"})
    elif "ERROR_MODEL_RESPONSE" in sep_plot_df.index:
        sep_plot_df = sep_plot_df.rename(index={"ERROR_MODEL_RESPONSE": "COMPILATION_FAILURE"})

    # # Exclude BUILD_SUCCESS from the plots.
    # if "BUILD_SUCCESS" in sep_plot_df.index:
    #     sep_plot_df = sep_plot_df.drop("BUILD_SUCCESS")
        
    # Convert columns to a MultiIndex from tuples so we can easily re-pivot the data.
    sep_plot_df.columns = pd.MultiIndex.from_tuples(sep_plot_df.columns, names=["llm", "prompt"])

    # Define the desired order of prompts for the x-axis.
    prompt_order = ["$P_1$", "$P_2$", "$P_3$", "$P_4$", "$P_5$", "$P_6$", "$P_7$", "$P_8$"]

    # Get the list of unique LLM names and assign each a fixed color.
    unique_llms = sorted(sep_plot_df.columns.get_level_values("llm").unique())
    color_cycle = plt.rcParams['axes.prop_cycle'].by_key()['color']
    colors = {llm: color_cycle[i % len(color_cycle)] for i, llm in enumerate(unique_llms)}
    
    # For each error category, create a separate grouped bar chart.
    for cat in sep_plot_df.index:
        # Extract the series for the current category, then unstack so that rows = prompts and columns = llm.
        cat_series = sep_plot_df.loc[cat]
        cat_df = cat_series.unstack(level=0)  # Now: index = prompt, columns = llm
        # Reindex the DataFrame to enforce the desired prompt order, filling missing entries with 0.
        cat_df = cat_df.reindex(prompt_order, fill_value=0)
        
        fig, ax = plt.subplots(figsize=(8, 6))
        x = np.arange(len(cat_df.index))
        bar_width = 0.1  # width for each individual bar
        added_labels = set()  # to avoid duplicate legend entries

        # For each prompt (row), sort the LLM values in descending order and plot them.
        for i, prompt in enumerate(cat_df.index):
            # Sort the values for this prompt in descending order
            sorted_series = cat_df.loc[prompt].sort_values(ascending=False)
            n_bars = len(sorted_series)
            # Calculate offsets to center the group of bars for this prompt
            offsets = np.linspace(-bar_width * n_bars / 2, bar_width * n_bars / 2, n_bars)
            for offset, (llm, value) in zip(offsets, sorted_series.items()):
                if llm not in added_labels:
                    ax.bar(i + offset, value, bar_width, color=colors[llm], label=llm)
                    added_labels.add(llm)
                else:
                    ax.bar(i + offset, value, bar_width, color=colors[llm])
        
        ax.set_xticks(x)
        ax.set_xticklabels(cat_df.index)
        ax.set_xlabel("Prompt")
        ax.set_ylabel("Number of Builds")
        ax.set_title(f"Number of builds resulting in {cat.lower().replace('_', ' ')} after Byam updates")
        ax.set_ylim(0, total_commits)  # Set y-axis limit to total commits
        ax.legend()
        plt.tight_layout()
        
        # Save the plot with a sanitized filename (spaces replaced with underscores)
        cat_filename = os.path.join(output_dir, f"{cat.replace(' ', '_')}_plot.png")
        plt.savefig(cat_filename)
        print(f"Plot for category {cat} saved to {cat_filename}")
        plt.show()