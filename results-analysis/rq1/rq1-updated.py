import pandas as pd
import os
import re

output_csv = True
output_excel = True
output_latex = True
output_build_success_latex = True
output_pgfkeys = True

input_file = "rq1_data.csv"
output_dir = "rq1-final-output"

csv_file = os.path.join(output_dir, "rq1_by_llm.csv")
excel_file = os.path.join(output_dir, "rq1_by_llm.xlsx")
latex_file = os.path.join(output_dir, "rq1_by_llm.tex")
build_success_latex_file = os.path.join(output_dir, "build_success_rq1.tex")
pgfkeys_file = os.path.join(output_dir, "rq1_pgfkeys.tex")

llm_mapping = {
    "deepseek": "\\textbf{Deepseek V3}",
    "gemini": "\\textbf{Gemini-2.0-flash}",
    "gpt": "\\textbf{Gpt-4o-mini}",
    "o3": "\\textbf{o3-mini}",
    "qwen": "\\textbf{Qwen2.5-32b-instruct}"
}

prompts = {
    ("$P_1$", "results_baseline"),
    ("$P_2$", "results_baseline_buggy_line"),
    ("$P_3$", "results_baseline_api_diff"),
    ("$P_4$", "results_baseline_buggy_line_api_diff"),
    ("$P_5$", "results_baseline_cot"),
    ("$P_6$", "results_baseline_cot_buggy_line"),
    ("$P_7$", "results_baseline_api_diff_cot"),
    ("$P_8$", "results_baseline_api_diff_cot_buggy")
}

df = pd.read_csv(input_file)

# Ensure output directory exists
os.makedirs(output_dir, exist_ok=True)

# Count the total number of commits (rows)
total_commits = len(df)
print(f"Total commits: {total_commits}")

# Drop the Commit column
df_no_commit = df.drop(columns=["Commit"])

# Prepare a dictionary to collect counts for each (LLM, Prompt) pair
counts_dict = {}

# Iterate over each column (which encodes prompt variant & LLM)
for col in df_no_commit.columns:
    prompt_part, llm_part = col.split(" - ", 1)

    # Get the distribution of status values for this column
    value_counts = df_no_commit[col].value_counts()

    # Replace prompt_part with corresponding LaTeX-friendly notation
    for prompt_tuple in prompts:
        if prompt_part == prompt_tuple[1]:
            prompt_part = prompt_tuple[0]
            break

    # Store in a nested dict keyed by (LLM, Prompt)
    counts_dict[(llm_part, prompt_part)] = value_counts

# Create a DataFrame where columns are a MultiIndex: [LLM, Prompt]
counts_df = pd.DataFrame(counts_dict).fillna(0).astype(int)
counts_df = counts_df.sort_index(axis=1)

# Overwrite all files
if output_csv:
    counts_df.to_csv(csv_file, index=True)
    print(f"CSV file saved to {csv_file}")

if output_excel:
    counts_df.to_excel(excel_file, index=True)
    print(f"Excel file saved to {excel_file}")

# Process PGFKeys Output
highest_build_success_key = None
highest_build_success_value = 0

if output_pgfkeys:
    with open(pgfkeys_file, "w") as f:
        f.write(f"\\pgfkeyssetvalue{{total_commits}}{{{total_commits}}}\n")
        for (llm, prompt), values in counts_dict.items():
            prompt_clean = re.sub(r'\$','', prompt)  # Remove $$ around the prompt name
            key_base = f"{llm}_{prompt_clean}"
            for status, count in values.items():
                percentage = (count / total_commits) * 100
                f.write(f"\\pgfkeyssetvalue{{{key_base}_{status}}}{{{count}}}\n")
                f.write(f"\\pgfkeyssetvalue{{{key_base}_{status}_percent}}{{{percentage:.2f}}}\n")
                
                if status == "BUILD_SUCCESS" and count > highest_build_success_value:
                    highest_build_success_value = count
                    highest_build_success_key = key_base+"_BUILD_SUCCESS"
    print(f"PGFKeys file saved to {pgfkeys_file}")

# LaTeX Output with Updated Headers and Highlighting
if output_build_success_latex:
    counts_build_success = counts_df.loc[["BUILD_SUCCESS"]]
    pivoted = counts_build_success.stack(level=1, future_stack=True)
    pivoted.index = pivoted.index.droplevel(0)
    pivoted = pivoted.astype(object)
    
    for prompt in pivoted.index:
        prompt_clean = re.sub(r'\$','', prompt)  # Remove $$ for LaTeX key usage
        for llm in pivoted.columns:
            key_base = f"{llm}_{prompt_clean}_BUILD_SUCCESS"
            value_str = f"\\pgfkeysvalueof{{{key_base}}}(\\pgfkeysvalueof{{{key_base}_percent}}\\%)"
            if key_base == highest_build_success_key:
                value_str = f"\\textbf{{{value_str}}}"
            pivoted.loc[prompt, llm] = value_str
    
    pivoted.columns = [llm_mapping.get(col, col) for col in pivoted.columns]  # Update column headers
    
    latex_table = pivoted.to_latex(
        index=True,
        caption="Build Success Rate of \\toolname~ on \\finaldata builds.",
        label="tab:build_success_prompt"
    )
    
    # Wrap the table in a table* environment manually
    latex_str = "\\begin{table*}[ht]\n\\centering\n" + latex_table + "\n\\end{table*}"
    
    with open(build_success_latex_file, "w") as f:
        f.write(latex_str)
    print(f"LaTeX table (BUILD_SUCCESS only) with row coloring saved to {build_success_latex_file}")
    print(f"Highest BUILD_SUCCESS key: {highest_build_success_key}")