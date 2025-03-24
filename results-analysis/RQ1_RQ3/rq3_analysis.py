import json
import os

import numpy as np
import csv
import pandas as pd
import matplotlib.pyplot as plt


output_csv = True
output_excel = True
output_latex = True
output_pgfkeys = True

api_diff_path = '/Users/mam10532/Documents/GitHub/bacardi/analysis/api_diff.txt'

root_folder_path = '/Users/mam10532/Documents/GitHub/bacardi/results'
output_folder='rq3-output/'

# Check if the output folder exists, if not, create it
if not os.path.exists(output_folder):
    os.makedirs(output_folder)

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
prompt_latex = {
    "results_baseline_buggy_line": "$P_2$",
    "results_baseline_api_diff_cot": "$P_7$",
    "results_baseline_cot": "$P_5$",
    "results_baseline": "$P_1$",
    "results_baseline_buggy_line_api_diff": "$P_4$",
    "results_baseline_api_diff": "$P_3$",
    "results_baseline_cot_buggy_line": "$P_6$",
    "results_baseline_api_diff_cot_buggy": "$P_8$"
}

# Read the lines from the text file
with open(api_diff_path, 'r') as file:
    lines = [line.strip() for line in file.readlines()]

## print all results
print(f' Number of lines {len(lines)}')

# Dictionary to store the results
results = {}

llm_mapping = {
    "deepseek-deepseek-chat": "\\textbf{Deepseek V3}",
    "gegemini-2.0-flash-001": "\\textbf{Gemini-2.0-flash}",
    "gpt-4o-mini": "\\textbf{Gpt-4o-mini}",
    "o3-mini-2025-01-31": "\\textbf{o3-mini}",
    "qwen-qwen2.5-32b-instruct": "\\textbf{Qwen2.5-32b-instruct}"
}

# Iterate over the prompts in the root folder
for prompt_folder in os.listdir(root_folder_path):
    prompt_path = os.path.join(root_folder_path, prompt_folder)
    if os.path.isdir(prompt_path):
        commit_results = {}
        prompt_results = {}
        # Iterate over the models
        for model_folder in os.listdir(prompt_path):
            model_path = os.path.join(prompt_path, model_folder)
            if os.path.isdir(model_path):
                print(f'Model {prompt_folder}/{model_folder}')
                json_file_path = os.path.join(model_path, f'result_repair_{model_folder}.json')
                if os.path.isfile(json_file_path):
                    with open(json_file_path, 'r') as json_file:
                        data = json.load(json_file)

                        print(f'JSON: {len(data)}')
                        prefix_errors = 0
                        fixed_errors = 0
                        new_errors = 0
                        relative_fixed = 0
        
                        for commit, details in data.items():
                            if commit in lines:
                                for attempt in details['attempts']:
                                    if attempt['failureCategory'] == "ERROR_MODEL_RESPONSE" or attempt['failureCategory'] == "NOT_REPAIRED":
                                        prefix_errors = attempt['prefixErrors']
                                        fixed_errors = 0
                                        new_errors = 0
                                        relative_fixed = 0
                                    else:
                                        prefix_errors = attempt['prefixErrors']
                                        fixed_errors = attempt['fixedErrors']
                                        new_errors = attempt['newErrors']
                                        relative_fixed= (fixed_errors-new_errors)/prefix_errors *100
                                    commit_results[commit] = {
                                        'prefix_errors': prefix_errors,
                                        'fixed_errors': fixed_errors,
                                        'new_errors': new_errors,
                                        'relative_fixed': relative_fixed
                                    }       
                        prompt_results[model_folder] = commit_results
                        commit_results = {}
                        print(f'Processed {model_folder} with {len(commit_results)} commits for prompt {prompt_folder}')
        #prompt = prompt_aliases.get(prompt_folder, prompt_folder)
        prompt=prompt_folder
        results[prompt] = prompt_results
        print(f'Processed prompt {prompt_folder}')        

if output_csv:
    # Prepare data for CSV
    csv_data = []
    csv_headers = ["Prompt", "Model", "Commit", "Prefix Errors", "Fixed Errors", "New Errors", "Relative Fixed"]

    for prompt, models in results.items():
        for model, commits in models.items():
            for commit, metrics in commits.items():
                row = [
                    prompt,
                    model,
                    commit,
                    metrics['prefix_errors'],
                    metrics['fixed_errors'],
                    metrics['new_errors'],
                    metrics['relative_fixed']
                ]
                csv_data.append(row)

    # Save the data to a CSV file
    csv_output_file_path = output_folder+'rq3_results.csv'
    with open(csv_output_file_path, 'w', newline='') as csv_output_file:
        writer = csv.writer(csv_output_file)
        writer.writerow(csv_headers)
        writer.writerows(csv_data)

    print(f'CSV results have been saved to {csv_output_file_path}')

if output_excel:
    # Save data to Excel file with separate sheets for each prompt and model
    excel_output_file_path = output_folder+'rq3_results.xlsx'
    with pd.ExcelWriter(excel_output_file_path) as writer:
        for prompt, models in results.items():
            for model, commits in models.items():
                sheet_name = f"{prompt}_{model}"
                df = pd.DataFrame.from_dict(commits, orient='index')
                df.to_excel(writer, sheet_name=sheet_name)

    print(f'Excel results have been saved to {excel_output_file_path}')

if output_pgfkeys:
    pgfkeys_data = []
    # Calculate statistics for each prompt and model
    for prompt, models in results.items():
        for model, commits in models.items():
            relative_fixed_values = [metrics['relative_fixed'] for metrics in commits.values()]
            if relative_fixed_values:
                mean_relative_fixed = np.mean(relative_fixed_values)
                median_relative_fixed = np.median(relative_fixed_values)
                max_relative_fixed = np.max(relative_fixed_values)
                min_relative_fixed = np.min(relative_fixed_values)

                key_prefix = f"{prompt_aliases.get(prompt, prompt)}_{model}"
                pgfkeys_data.append(f"\\pgfkeyssetvalue{{{key_prefix}_mean_relative_fixed}}{{{mean_relative_fixed:.2f}}}")
                pgfkeys_data.append(f"\\pgfkeyssetvalue{{{key_prefix}_median_relative_fixed}}{{{median_relative_fixed:.2f}}}")
                pgfkeys_data.append(f"\\pgfkeyssetvalue{{{key_prefix}_max_relative_fixed}}{{{max_relative_fixed:.2f}}}")
                pgfkeys_data.append(f"\\pgfkeyssetvalue{{{key_prefix}_min_relative_fixed}}{{{min_relative_fixed:.2f}}}")

        # Save the pgfkeysvalue data to a file
    pgfkeys_output_file_path = output_folder+'rq3-values.tex'
    with open(pgfkeys_output_file_path, 'w') as pgfkeys_output_file:
        pgfkeys_output_file.write("\n".join(pgfkeys_data))

    print(f'pgfkeys values have been saved to {pgfkeys_output_file_path}')


if output_latex:
    # Prepare data for LaTeX table using pgfkeys values and only show the median
    table_data = []
    prompts = sorted(results.keys(), key=lambda x: prompt_aliases.get(x, x))
    headers = ["Prompt"] + [llm_mapping.get(model, model) for model in sorted(next(iter(results.values())).keys())]

    for prompt in prompts:
        row = [prompt_latex.get(prompt, prompt)]
        for model in sorted(next(iter(results.values())).keys()):
            key_prefix = f"{prompt_aliases.get(prompt, prompt)}_{model}"
            median_key = f"\\pgfkeysvalueof{{{key_prefix}_median_relative_fixed}}"
            row.append(f"{median_key}\\%")
        table_data.append(row)

    # Generate LaTeX table
    latex_table = "\\begin{tabular}{l" + "r" * len(headers[1:]) + "}\n"
    latex_table += " & ".join(headers) + " \\\\\n"
    latex_table += "\\hline\n"
    for row in table_data:
        latex_table += " & ".join(row) + " \\\\\n"
    latex_table += "\\end{tabular}"

    print(latex_table)

    # Save the LaTeX table to a file
    latex_output_file_path = output_folder+'relative-rq3.tex'
    with open(latex_output_file_path, 'w') as latex_output_file:
        latex_output_file.write(latex_table)

    print(f'LaTeX table has been saved to {latex_output_file_path}')


